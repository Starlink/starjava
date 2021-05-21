package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.AdqlSyntax;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage which checks all actual results (SELECT *) against
 * declared metadata.
 *
 * @author   Mark Taylor
 * @since    7 Jul 2011
 */
public class ColumnMetadataStage implements Stage {

    private final TapRunner tapRunner_;
    private final MetadataHolder metaHolder_;
    private int maxTables_;
    private static final AdqlSyntax syntax_ = AdqlSyntax.getInstance();

    /**
     * Constructor.
     *
     * @param  tapRunner    object that can run TAP queries
     * @param  metaHolder   provides table metadata at run time
     * @param  maxTables  limit on the number of tables to test,
     *                    or &lt;=0 for no limit
     */
    public ColumnMetadataStage( TapRunner tapRunner,
                                MetadataHolder metaHolder,
                                int maxTables ) {
        tapRunner_ = tapRunner;
        metaHolder_ = metaHolder;
        maxTables_ = maxTables;
    }

    /**
     * Resets the limit on the number of tables to test.
     *
     * @param  maxTables  limit on the number of tables to test,
     *                    or &lt;=0 for no limit
     */
    public void setMaxTestTables( int maxTables ) {
        maxTables_ = maxTables;
    }

    public String getDescription() {
        return "Check table query result columns against declared metadata";
    }

    public void run( Reporter reporter, TapService tapService ) {
        SchemaMeta[] smetas = metaHolder_.getTableMetadata();
        List<TableMeta> tlist = new ArrayList<TableMeta>();
        if ( smetas != null ) {
            for ( SchemaMeta smeta : smetas ) {
                for ( TableMeta tmeta : smeta.getTables() ) {
                    tlist.add( tmeta );
                }
            }
        }
        TableMeta[] tmetas = tlist.toArray( new TableMeta[ 0 ] );
        if ( tmetas.length == 0 ) {
            reporter.report( FixedCode.F_NOTM,
                             "No table metadata available"
                           + " (earlier stages failed/skipped?)"
                           + " - will not run test queries" );
            return;
        }
        if ( maxTables_ > 0 && tmetas.length > maxTables_ ) {
            TableMeta[] tms = new TableMeta[ maxTables_ ];
            System.arraycopy( tmetas, 0, tms, 0, maxTables_ );
            reporter.report( FixedCode.I_TMAX,
                             "Testing only " + maxTables_ + "/" + tmetas.length
                           + " tables" );
            tmetas = tms;
        }
        new Checker( reporter, tapService, tapRunner_, tmetas ).run();
        tapRunner_.reportSummary( reporter );
    }

    /**
     * Returns the essence of a column name.
     * The result is the string that ought to be equal to the name of the
     * same column when it appears somewhere else.
     *
     * <p>Currently, this does two things: flattens case and unquotes
     * quoted names (delimited identifiers).
     * This is a bit tricky or debatable or controversial.
     * In general unquoting column names is not supposed to be something
     * the client can do for itself, because there may be strange
     * server-specific rules.  But it's pretty clear how it would be done
     * if it could be done, and pragmatically it doesn't make sense to
     * flag an error if a column declared in the metadata with name '"size"'
     * comes back in a result table with name 'size'.  So this is probably
     * the right thing to do for comparisons.
     *
     * @param   colName  column name
     * @return   normalised column name
     */
    public static String normaliseColumnName( String colName ) {
        return syntax_.unquote( colName ).toLowerCase();
    }

    /**
     * Does the work for this stage.
     */
    private static class Checker implements Runnable {
        private final Reporter reporter_;
        private final TapService tapService_;
        private final TapRunner tRunner_;
        private final TableMeta[] tmetas_;

        /**
         * Constructor.
         *
         * @param   reporter  validation message destination
         * @param   tapService  TAP service description
         * @param   tRunner  tap query executer
         * @param   tmetas  declared table metadata to check against
         */
        Checker( Reporter reporter, TapService tapService, TapRunner tRunner,
                 TableMeta[] tmetas ) {
            reporter_ = reporter;
            tapService_ = tapService;
            tRunner_ = tRunner;
            tmetas_ = tmetas;
        }

        public void run() {
            for ( int it = 0; it < tmetas_.length; it++ ) {
                checkTable( tmetas_[ it ] );
            }
        }

        /**
         * Performs a service query on a single table whose declared metadata
         * is given.
         *
         * @param  tmeta  declared table metadata
         */
        private void checkTable( TableMeta tmeta ) {

            /* Make query. */
            int ntop = 1;
            String tname = tmeta.getName();
            String adql = "SELECT TOP " + ntop + " * FROM " + tname;
            TapQuery tq = new TapQuery( tapService_, adql, null );
            StarTable result;
            try {
                result = tRunner_.attemptGetResultTable( reporter_, tq );
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_QERR,
                                  "Failed TAP query " + adql, e );
                return;
            }
            catch ( SAXException e ) {
                reporter_.report( FixedCode.E_QERX,
                                  "Failed to parse result for TAP query "
                                + adql, e );
                return;
            }

            /* Check row count. */
            try {
                result = Tables.randomTable( result );
            }
            catch ( IOException e ) {
                reporter_.report( FixedCode.E_VTIO,
                                  "Error reading table result for " + adql, e );
                return;
            }
            long nrow = result.getRowCount();
            if ( nrow > ntop ) {
                String msg = new StringBuilder()
                   .append( "Too many rows returned (" )
                   .append( nrow )
                   .append( " > " )
                   .append( ntop )
                   .append( " for " )
                   .append( adql )
                   .toString();
                reporter_.report( FixedCode.E_RRTO, msg );
            }

            /* Prepare name->column-metadata maps for both declared and
             * result column sets. */
            ColumnMeta[] colMetas = tmeta.getColumns();
            Map<String,ColumnMeta> declaredMap =
                new LinkedHashMap<String,ColumnMeta>();
            for ( int ic = 0; ic < colMetas.length; ic++ ) {
                ColumnMeta colMeta = colMetas[ ic ];
                declaredMap.put( normaliseColumnName( colMeta.getName() ),
                                 colMeta );
            }
            Map<String,ColumnInfo> resultMap =
                new LinkedHashMap<String,ColumnInfo>();
            for ( int ic = 0; ic < result.getColumnCount(); ic++ ) {
                ColumnInfo colInfo = result.getColumnInfo( ic );
                resultMap.put( normaliseColumnName( colInfo.getName() ),
                               colInfo );
            }

            /* Check for columns declared but not in result. */
            {
                List<String> dOnlyList =
                    new ArrayList<String>( declaredMap.keySet() );
                dOnlyList.removeAll( resultMap.keySet() );
                int ndo = dOnlyList.size();
                if ( ndo > 0 ) {
                    StringBuilder sbuf = new StringBuilder()
                        .append( "SELECT * for table " )
                        .append( tname )
                        .append( " lacks " )
                        .append( ndo )
                        .append( " declared " )
                        .append( ndo == 1 ? "column" : "columns" )
                        .append( ": " );
                    for ( Iterator<String> cit = dOnlyList.iterator();
                          cit.hasNext(); ) {
                        String cname = cit.next();
                        sbuf.append( declaredMap.get( cname ).getName() );
                        if ( cit.hasNext() ) {
                            sbuf.append( ", " );
                        }
                    }
                    reporter_.report( FixedCode.E_CLDR, sbuf.toString() );
                }
            }

            /* Check for columns in result but not declared. */
            {
                List<String> rOnlyList =
                    new ArrayList<String>( resultMap.keySet() );
                rOnlyList.removeAll( declaredMap.keySet() );
                int nro = rOnlyList.size();
                if ( nro > 0 ) {
                    StringBuilder sbuf = new StringBuilder()
                        .append( "SELECT * for table " )
                        .append( tname )
                        .append( " returns " )
                        .append( nro )
                        .append( " undeclared " )
                        .append( nro == 1 ? "column" : "columns" )
                        .append( ": " );
                    for ( Iterator<String> cit = rOnlyList.iterator();
                          cit.hasNext(); ) {
                        String cname = cit.next();
                        sbuf.append( resultMap.get( cname ).getName() );
                        if ( cit.hasNext() ) {
                            sbuf.append( ", " );
                        }
                    }
                    reporter_.report( FixedCode.E_CLRD, sbuf.toString() );
                }
            }

            /* Work out the intersection. */
            List<String> bothList =
                new ArrayList<String>( declaredMap.keySet() );
            bothList.retainAll( resultMap.keySet() );

            /* Report on capitalisation discrepancies. */
            for ( String cname : bothList ) {
                String dName = declaredMap.get( cname ).getName();
                String rName = resultMap.get( cname ).getName();
                if ( ! syntax_.unquote( dName )
                      .equals( syntax_.unquote( rName ) ) ) {
                    String msg = new StringBuilder()
                        .append( "Declared/result column " )
                        .append( "capitalization mismatch in table " )
                        .append( tname )
                        .append( " " )
                        .append( dName )
                        .append( " != " )
                        .append( rName )
                        .toString();
                    reporter_.report( FixedCode.W_CCAS, msg );
                }
            }

            /* Report on datatype and metadata discrepancies. */
            for ( String cname : bothList ) {
                ColumnMeta dMeta = declaredMap.get( cname );
                ColumnInfo rInfo = resultMap.get( cname );
                String dType = dMeta.getDataType();
                String rType =
                    rInfo.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                            String.class );
                if ( ! CompareMetadataStage
                      .compatibleDataTypes( dType, rType ) ) {
                    String msg = new StringBuilder()
                        .append( "Declared/result type mismatch " )
                        .append( "for column " )
                        .append( resultMap.get( cname ).getName() )
                        .append( " in table " )
                        .append( tname )
                        .append( " (" )
                        .append( dType )
                        .append( " != " )
                        .append( rType )
                        .append( ")" )
                        .toString();
                    reporter_.report( FixedCode.E_CTYP, msg );
                }
                compareColumnMetadata( dMeta.getUnit(), rInfo.getUnitString(),
                                       "Unit", FixedCode.W_DRUN, cname, tname );
                compareColumnMetadata( dMeta.getXtype(), rInfo.getXtype(),
                                       "XType", FixedCode.W_DRXT, cname, tname);
                compareColumnMetadata( dMeta.getUcd(), rInfo.getUCD(),
                                       "UCD", FixedCode.W_DRUC, cname, tname );
                compareColumnMetadata( dMeta.getUtype(), rInfo.getUtype(),
                                       "UType", FixedCode.W_DRUT, cname, tname);
            }
        }

        /**
         * Compares declared and query result metadata and reports
         * discrepancies.
         *
         * @param   declTxt   declared metadata item value
         * @param   resultTxt   query result metadata item value
         * @param   metaName   metadata item name
         * @param   mismatchCode   report code in case of mismatch
         * @param   colName   name of column
         * @Param   tableName   name of table
         */
        private void compareColumnMetadata( String declTxt, String resultTxt,
                                            String metaName,
                                            ReportCode mismatchCode,
                                            String colName, String tableName ) {
            if ( declTxt == null ? resultTxt != null
                                 : ! declTxt.equals( resultTxt ) ) {
                String msg = new StringBuilder()
                    .append( "Declared/result " )
                    .append( metaName )
                    .append( " mismatch for column " )
                    .append( colName )
                    .append( " in table " )
                    .append( tableName )
                    .append( " (")
                    .append( declTxt )
                    .append( " != " )
                    .append( resultTxt )
                    .append( ")" )
                    .toString();
                reporter_.report( mismatchCode, msg );
            }
        }
    }
}
