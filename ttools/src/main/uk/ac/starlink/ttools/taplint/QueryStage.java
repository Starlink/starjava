package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage which performs some ADQL queries on data tables.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2011
 */
public abstract class QueryStage implements Stage {

    private final VotLintTapRunner tapRunner_;

    /**
     * Constructor.
     *
     * @param  tapRunner  object that can run TAP queries
     */
    protected QueryStage( VotLintTapRunner tapRunner ) {
        tapRunner_ = tapRunner;
    }

    public String getDescription() {
        return "Make ADQL queries in " + tapRunner_.getDescription()
             + " mode";
    }

    /**
     * Returns the table metadata which will be used to frame example
     * ADQL queries.
     *
     * @return   table metadata array
     */
    protected abstract TableMeta[] getTableMetadata();

    public void run( URL serviceUrl, Reporter reporter ) {
        TableMeta[] tmetas = getTableMetadata();
        if ( tmetas == null || tmetas.length == 0 ) {
            reporter.report( Reporter.Type.WARNING, "NOTM",
                             "No table metadata available"
                           + " - will not run test queries" );
            return;
        }
        new Querier( reporter, serviceUrl, tmetas ).run();
        tapRunner_.reportSummary( reporter );
    }

    /**
     * Chooses one of a given set of tables to use for examples.
     * Some attempt is made to use a representative table.
     *
     * @param  tmetas  table list
     * @return  representative element of <code>tmetas</code>
     */
    private static TableMeta getExampleTable( TableMeta[] tmetas ) {
        TableMeta[] sorted = (TableMeta[]) tmetas.clone();
        Arrays.sort( sorted, new Comparator<TableMeta>() {
            public int compare( TableMeta tm1, TableMeta tm2 ) {
                return tm1.getColumns().length - tm2.getColumns().length;
            }
        } );
        return sorted[ sorted.length / 2 ];
    }

    /**
     * Service method to create an instance of this class given some
     * previously-run stages which might have acquired table metadata.
     *
     * @param  metaStages  metadata generating stages
     */
    public static QueryStage createStage( VotLintTapRunner tapRunner,
                                          final TableMetadataStage[]
                                          metaStages ) {
        return new QueryStage( tapRunner ) {
            protected TableMeta[] getTableMetadata() {
                for ( int is = 0; is < metaStages.length; is++ ) {
                    TableMeta[] tmetas = metaStages[ is ].getTableMetadata();
                    if ( tmetas != null ) {
                        return tmetas;
                    }
                }
                return null;
            }
        };
    }

    /**
     * Utility class to perform and check TAP queries.
     */
    private class Querier implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final TableMeta[] tmetas_;
        private final TableMeta tmeta1_;

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  serviceUrl  TAP service URL
         * @param  tmetas  metadata for known tables
         */
        Querier( Reporter reporter, URL serviceUrl, TableMeta[] tmetas ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            tmetas_ = tmetas;
            tmeta1_ = getExampleTable( tmetas );
        }

        /**
         * Invoked to perform queries.
         */
        public void run() {
            runAllColumns();
        }

        /**
         * Runs some tests using all the columns in an example table.
         */
        private void runAllColumns() {
            int nr0 = 10;
            String name1 = tmeta1_.getName();
            StarTable t1 = runCheckedQuery( "SELECT * FROM " + name1, nr0,
                                            tmeta1_.getColumns(), -1 );
            long nr1 = t1 != null ? t1.getRowCount() : 0;
            assert nr1 >= 0;
            final boolean over;
            long nr2;
            if ( nr1 > 0 ) {
                over = true;
                nr2 = Math.min( nr1 - 1, nr0 );
            }
            else {
                over = false;
                nr2 = 3;
            }
            StarTable t2 =
                runCheckedQuery( "SELECT TOP " + nr2 + " * FROM " + name1, -1,
                                 tmeta1_.getColumns(), (int) nr2 );
            if ( t2 != null && over && ! tapRunner_.isOverflow( t2 ) ) {
                String msg = new StringBuffer()
                   .append( "Overflow not marked - " )
                   .append( "no <INFO name='QUERY_STATUS' value='OVERFLOW'/> " )
                   .append( "after TABLE" )
                   .toString();
                reporter_.report( Reporter.Type.ERROR, "OVNO", msg );
            }
        }

        /**
         * Executes a TAP query and checks the results.
         *
         * @param  adql  query string
         * @param  maxrec  value of MAXREC limit
         * @param  colMetas  expected column metadata of result
         * @param  maxrow  expected maximum row count, different from MAXREC
         */
        private StarTable runCheckedQuery( String adql, int maxrec,
                                           ColumnMeta[] colMetas, int maxrow ) {
            Map<String,String> extraParams = new HashMap<String,String>();
            if ( maxrec >= 0 ) {
                extraParams.put( "MAXREC", Integer.toString( maxrec ) );
            }
            TapQuery tq;
            try {
                tq = new TapQuery( serviceUrl_, adql, extraParams, null, 0 );
            }
            catch ( IOException e ) {
                reporter_.report( Reporter.Type.ERROR, "TSER",
                                  "TAP job creation failed for " + adql, e );
                return null;
            }
            StarTable table = tapRunner_.getResultTable( reporter_, tq );
            if ( table != null ) {
                int nrow = (int) table.getRowCount();
                if ( maxrec >=0 && nrow > maxrec ) {
                    String msg = new StringBuffer()
                       .append( "More than MAXREC rows returned (" )
                       .append( nrow )
                       .append( " > " )
                       .append( maxrec )
                       .append( ")" )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( Reporter.Type.ERROR, "NREC", msg );
                }
                if ( maxrow >= 0 && nrow > maxrow ) {
                    String msg = new StringBuffer()
                       .append( "More rows than expected (" )
                       .append( nrow )
                       .append( ") returned" )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( Reporter.Type.ERROR, "NROW", msg );
                }
                checkMeta( adql, colMetas, table );
            }
            return table;
        }

        /**
         * Checks metadata from a TAP result matches what's expected.
         *
         * @param  adql  text of query
         * @param  colMetas   metadata of expected columns in result
         * @param  table  actual result table
         */
        private void checkMeta( String adql, ColumnMeta[] colMetas,
                                StarTable table ) {

            /* Check column counts match. */
            int qCount = colMetas.length;
            int rCount = table.getColumnCount();
            if ( qCount != rCount ) {
                String msg = new StringBuffer()
                   .append( "Query/result column count mismatch; " )
                   .append( qCount )
                   .append( " != " )
                   .append( rCount )
                   .append( " for " )
                   .append( adql )
                   .toString();
                reporter_.report( Reporter.Type.ERROR, "NCOL", msg );
                return;
            }
            int ncol = qCount;
            assert ncol == rCount;

            /* Check column names match. */
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnMeta cmeta = colMetas[ ic ];
                ColumnInfo cinfo = table.getColumnInfo( ic );
                String qName = cmeta.getName();
                String rName = cinfo.getName();
                if ( ! qName.equalsIgnoreCase( rName ) ) {
                    String msg = new StringBuffer()
                       .append( "Query/result column name mismatch " )
                       .append( "for column #" )
                       .append( ic )
                       .append( "; " )
                       .append( qName )
                       .append( " != " )
                       .append( rName )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( Reporter.Type.ERROR, "CNAM", msg );
                }
                String columnId = rName.equalsIgnoreCase( qName )
                                ? qName
                                : "#" + ic;

                /* Check column types match. */
                String qType = cmeta.getDataType();
                String rType =
                    (String) cinfo.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                                     String.class );
                if ( ! CompareMetadataStage
                      .compatibleDataTypes( qType, rType ) ) {
                    String msg = new StringBuffer()
                       .append( "Query/result column type mismatch " )
                       .append( " for column " )
                       .append( columnId )
                       .append( "; " )
                       .append( qType )
                       .append( " vs. " )
                       .append( rType )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( Reporter.Type.ERROR, "CTYP", msg );
                }
            }
        }
    }
}
