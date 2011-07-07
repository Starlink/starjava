package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
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
            StarTable result = tRunner_.getResultTable( reporter_, tq );
            if ( result != null ) {

                /* Check row count. */
                try {
                    result = Tables.randomTable( result );
                }
                catch ( IOException e ) {
                    reporter_.report( ReportType.ERROR, "VTIO",
                                      "Error reading table result for " + adql,
                                      e );
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

                /* Check column count. */
                ColumnMeta[] colMetas = tmeta.getColumns();
                ColumnInfo[] colInfos = Tables.getColumnInfos( result );
                int mCount = colMetas.length;
                int rCount = colInfos.length;
                if ( mCount != rCount ) {
                    String msg = new StringBuilder()
                       .append( "Result/metadata column count mismatch: " )
                       .append( rCount )
                       .append( " != " )
                       .append( mCount )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( ReportType.ERROR, "NCOL", msg );
                    return;
                }
                int ncol = mCount;

                /* Check column names. */
                boolean[] matches = new boolean[ ncol ];
                for ( int ic = 0; ic < ncol; ic++ ) {
                    String mName = colMetas[ ic ].getName();
                    String rName = colInfos[ ic ].getName();
                    final boolean match;
                    if ( ! mName.equalsIgnoreCase( rName ) ) {
                        match = false;
                        String msg = new StringBuilder()
                           .append( "Result/metadata column name mismatch: " )
                           .append( rName )
                           .append( " != " )
                           .append( mName )
                           .append( " for column #" )
                           .append( ic + 1 )
                           .append( " in table " )
                           .append( tname )
                           .toString();
                        reporter_.report( ReportType.ERROR, "CNAM", msg );
                    }
                    else if ( ! mName.equals( rName ) ) {
                        match = true;
                        String msg = new StringBuilder()
                           .append( "Result/metadata column name " )
                           .append( "case mismatch: " )
                           .append( rName )
                           .append( " != " )
                           .append( mName )
                           .append( " for column #" )
                           .append( ic + 1 )
                           .append( " in table " ) 
                           .append( tname )
                           .toString();
                        reporter_.report( ReportType.WARNING, "CCAS", msg );
                    }
                    else {
                        match = true;
                    }
                    matches[ ic ] = match;
                }

                /* Check column types. */
                for ( int ic = 0; ic < ncol; ic++ ) {
                    if ( matches[ ic ] ) {
                        String cname = colMetas[ ic ].getName();
                        String mType = colMetas[ ic ].getDataType();
                        String rType =
                            (String)
                            colInfos[ ic ]
                           .getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                              String.class );
                        if ( ! CompareMetadataStage
                              .compatibleDataTypes( mType, rType ) ) {
                            String msg = new StringBuilder()
                               .append( "Result/metadata column type " )
                               .append( "mismatch for column " )
                               .append( cname )
                               .append( " in table " )
                               .append( tname )
                               .append( ": " )
                               .append( rType )
                               .append( " != " )
                               .append( mType )
                               .toString();
                            reporter_.report( ReportType.WARNING, "CTYP", msg );
                        }
                    }
                }
            }
        }
    }
}
