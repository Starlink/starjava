package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
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
    private final int maxTables_;

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

    public String getDescription() {
        return "Check table query result columns against declared metadata";
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        TableMeta[] tmetas = metaHolder_.getTableMetadata();
        if ( tmetas == null || tmetas.length == 0 ) {
            reporter.report( ReportType.FAILURE, "NOTM",
                             "No table metadata available"
                           + " (earlier stages failed/skipped?)"
                           + " - will not run test queries" );
            return;
        }
        if ( maxTables_ > 0 && tmetas.length > maxTables_ ) {
            TableMeta[] tms = new TableMeta[ maxTables_ ];
            System.arraycopy( tmetas, 0, tms, 0, maxTables_ );
            reporter.report( ReportType.INFO, "TMAX",
                             "Testing only " + maxTables_ + "/" + tmetas.length
                           + " tables" );
            tmetas = tms;
        }
        new Checker( reporter, serviceUrl, tapRunner_, tmetas ).run();
        tapRunner_.reportSummary( reporter );
    }

    /**
     * Does the work for this stage.
     */
    private static class Checker implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final TapRunner tRunner_;
        private final TableMeta[] tmetas_;

        /**
         * Constructor.
         *
         * @param   reporter  validation message destination
         * @param   serviceUrl  TAP service URL
         * @param   tRunner  tap query executer
         * @param   tmetas  declared table metadata to check against
         */
        Checker( Reporter reporter, URL serviceUrl, TapRunner tRunner,
                 TableMeta[] tmetas ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
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
            TapQuery tq;
            try {
                tq = new TapQuery( serviceUrl_, adql, null, null, 0 );
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "TSER",
                                  "TAP job creation failed for " + adql, e );
                return;
            }
            StarTable result;
            try {
                result = tRunner_.attemptGetResultTable( reporter_, tq );
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "QERR",
                                  "Failed TAP query " + adql, e );
                return;
            }
            catch ( SAXException e ) {
                reporter_.report( ReportType.ERROR, "QERX",
                                  "Failed to parse result for TAP query "
                                + adql, e );
                return;
            }

            /* Check row count. */
            try {
                result = Tables.randomTable( result );
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "VTIO",
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
                reporter_.report( ReportType.ERROR, "RRTO", msg );
            }

            /* Prepare name->column-metadata maps for both declared and
             * result column sets. */
            ColumnMeta[] colMetas = tmeta.getColumns();
            Map<String,ColumnMeta> declaredMap =
                new LinkedHashMap<String,ColumnMeta>();
            for ( int ic = 0; ic < colMetas.length; ic++ ) {
                ColumnMeta colMeta = colMetas[ ic ];
                declaredMap.put( normalize( colMeta.getName() ), colMeta );
            }
            Map<String,ColumnInfo> resultMap =
                new LinkedHashMap<String,ColumnInfo>();
            for ( int ic = 0; ic < result.getColumnCount(); ic++ ) {
                ColumnInfo colInfo = result.getColumnInfo( ic );
                resultMap.put( normalize( colInfo.getName() ), colInfo );
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
                    reporter_.report( ReportType.ERROR, "CLDR",
                                      sbuf.toString() );
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
                    reporter_.report( ReportType.ERROR, "CLRD",
                                      sbuf.toString() );
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
                if ( ! dName.equals( rName ) ) {
                    String msg = new StringBuilder()
                        .append( "Declared/result column " )
                        .append( "capitalization mismatch in table " )
                        .append( tname )
                        .append( " " )
                        .append( dName )
                        .append( " != " )
                        .append( rName )
                        .toString();
                    reporter_.report( ReportType.WARNING, "CCAS", msg );
                }
            }

            /* Report on type discrepancies. */
            for ( String cname : bothList ) {
                String dType = declaredMap.get( cname ).getDataType();
                String rType = (String)
                               resultMap.get( cname )
                              .getAuxDatumValue( VOStarTable.DATATYPE_INFO,
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
                    reporter_.report( ReportType.ERROR, "CTYP", msg );
                }
            }
        }

        /**
         * Flattens case for column names.
         *
         * @param   name  column name
         * @param   name with case folded
         */
        private String normalize( String name ) {
            return name.toLowerCase();
        }
    }
}
