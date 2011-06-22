package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.vo.AdqlSyntax;
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
        private final TableMeta tmeta1_;
        private final TableMeta tmeta2_;
        private final TableMeta tmeta3_;

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

            /* Pick some representative tables for later testing.
             * Do it by choosing tables with column counts near the median. */
            tmetas = (TableMeta[]) tmetas.clone();
            Arrays.sort( tmetas, new Comparator<TableMeta>() {
                public int compare( TableMeta tm1, TableMeta tm2 ) {
                    return tm1.getColumns().length - tm2.getColumns().length;
                }
            } );
            int imed = tmetas.length / 2;
            tmeta1_ = tmetas[ imed ];
            tmeta2_ = tmetas[ Math.min( imed + 1, tmetas.length - 1 ) ];
            tmeta3_ = tmetas[ Math.min( imed + 2, tmetas.length - 1 ) ];
        }

        /**
         * Invoked to perform queries.
         */
        public void run() {
            runAllColumns( tmeta1_ );
            runSomeColumns( tmeta2_ );
            runJustMeta( tmeta3_ );
            // make sure GET and POST work
        }

        /**
         * Runs some tests using all the columns in an example table.
         *
         * @param  tmeta  table to test
         */
        private void runAllColumns( TableMeta tmeta ) {
            int nr0 = 10;
            String tname = tmeta.getName();

            /* Prepare an array of column specifiers corresponding to all
             * the columns in the table. */
            ColumnMeta[] cmetas = tmeta.getColumns();
            int ncol = cmetas.length;
            ColSpec[] cspecs = new ColSpec[ ncol ];
            for ( int ic = 0; ic < ncol; ic++ ) {
                cspecs[ ic ] = new ColSpec( cmetas[ ic ] );
            }

            /* Check that MAXREC limit works. */
            StarTable t1 = runCheckedQuery( "SELECT * FROM " + tname, nr0,
                                            cspecs, -1 );

            /* Get a lower bound for the number of rows in the result. */
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

            /* Check that limiting using TOP works. */
            StarTable t2 =
                runCheckedQuery( "SELECT TOP " + nr2 + " * FROM " + tname, -1,
                                 cspecs, (int) nr2 );
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
         * Runs some tests using a selection of the columns in an example
         * table.
         *
         * @param  tmeta  table to run tests on
         */
        private void runSomeColumns( TableMeta tmeta ) {

            /* Assemble column specifiers for the query. */
            String talias = tmeta.getName().substring( 0, 1 );
            List<ColSpec> clist = new ArrayList<ColSpec>();
            int ncol = tmeta.getColumns().length;
            int step = Math.max( 1, ncol / 11 );
            int ix = 0;
            for ( int icol = ncol - 1; icol >= 0; icol -= step ) {
                ColSpec cspec = new ColSpec( tmeta.getColumns()[ icol ] );
                if ( ix % 2 == 1 ) {
                    cspec.setRename( "c_" + ( ix + 1 ) );
                }
                if ( ix % 3 == 2 ) {
                    cspec.setQuoted( true );
                }
                if ( ix % 5 == 3 ) {
                    cspec.setTableAlias( talias );
                }
                clist.add( cspec );
                ix++;
            }
            ColSpec[] cspecs = clist.toArray( new ColSpec[ 0 ] );

            /* Assemble the ADQL text. */
            int nr = 8;
            StringBuffer abuf = new StringBuffer();
            abuf.append( "SELECT " )
                .append( "TOP " )
                .append( nr )
                .append( " " );
            for ( Iterator<ColSpec> colIt = clist.iterator();
                  colIt.hasNext(); ) {
                ColSpec col = colIt.next();
                abuf.append( col.getQueryText() );
                if ( colIt.hasNext() ) {
                    abuf.append( "," );
                }
                abuf.append( " " );
            }
            abuf.append( " FROM " )
                .append( tmeta.getName() )
                .append( " AS " )
                .append( talias );
            String adql = abuf.toString();

            /* Run the query. */
            runCheckedQuery( adql, -1, cspecs, nr );
        }

        /**
         * Test that a query with MAXREC=0 works (it should return metadata).
         *
         * @param  tmeta  table to run tests on
         */
        private void runJustMeta( TableMeta tmeta ) {
            ColSpec cspec = new ColSpec( tmeta.getColumns()[ 0 ] );
            String adql = new StringBuffer()
                .append( "SELECT " )
                .append( cspec.getQueryText() )
                .append( " FROM " )
                .append( tmeta.getName() )
                .toString();
            runCheckedQuery( adql, 0, new ColSpec[] { cspec }, -1 );
        }

        /**
         * Executes a TAP query and checks the results.
         *
         * @param  adql  query string
         * @param  maxrec  value of MAXREC limit; if negative none applied
         * @param  colSpecs  expected column metadata of result
         * @param  maxrow  expected maximum row count, different from MAXREC
         */
        private StarTable runCheckedQuery( String adql, int maxrec,
                                           ColSpec[] colSpecs, int maxrow ) {
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
                checkMeta( adql, colSpecs, table );
            }
            return table;
        }

        /**
         * Checks metadata from a TAP result matches what's expected.
         *
         * @param  adql  text of query
         * @param  colSpecs   metadata of expected columns in result
         * @param  table  actual result table
         */
        private void checkMeta( String adql, ColSpec[] colSpecs,
                                StarTable table ) {

            /* Check column counts match. */
            int qCount = colSpecs.length;
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
                ColSpec cspec = colSpecs[ ic ];
                ColumnInfo cinfo = table.getColumnInfo( ic );
                String qName = cspec.getResultName();
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
                String qType = cspec.getColumnMeta().getDataType();
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

    /**
     * Specifies a column for an ADQL query.
     */
    private static class ColSpec {
        private final ColumnMeta cmeta_;
        private String talias_;
        private String rename_;
        private boolean quote_;

        /**
         * Constructor.
         *
         * @param   cmeta  column metadata on which this queried column is based
         */
        ColSpec( ColumnMeta cmeta ) {
            cmeta_ = cmeta;
        }

        /**
         * Returns the column metadata object this column is based on.
         *
         * @return  col meta
         */
        ColumnMeta getColumnMeta() {
            return cmeta_;
        }

        /**
         * Sets whether the column name should be quoted in ADQL.
         *
         * @param  quote   true for quoting, false for not
         */
        public void setQuoted( boolean quote ) {
            quote_ = quote;
        }

        /**
         * Sets an optional table alias by which the table this column
         * comes from is known.
         *
         * @param  talias  table alias, may be null
         */
        public void setTableAlias( String talias ) {
            talias_ = talias;
        }

        /**
         * Sets an optional changed name by which this column will be queried
         * as.  If null, the name will be taken from the column metadata.
         *
         * @param  rename  column alias, or null
         */
        public void setRename( String rename ) {
            rename_ = rename;
        }

        /**
         * Returns the ADQL text for the column list in the SELECT statement
         * corresponding to this column.
         *
         * @return  query column specifier
         */
        String getQueryText() {
            AdqlSyntax syntax = AdqlSyntax.getInstance();
            StringBuffer sbuf = new StringBuffer();
            if ( talias_ != null ) {
                sbuf.append( syntax.quoteIfNecessary( talias_ ) )
                    .append( "." );
            }
            String cname = cmeta_.getName();
            sbuf.append( quote_ ? syntax.quote( cname )
                                : syntax.quoteIfNecessary( cname ) );
            if ( rename_ != null ) {
                sbuf.append( " AS " )
                    .append( syntax.quoteIfNecessary( rename_ ) );
            }
            return sbuf.toString();
        }

        /**
         * Returns the name of the column that should be returned from the
         * query for this column.
         *
         * @return   result column name
         */
        String getResultName() {
            return rename_ == null ? cmeta_.getName() : rename_;
        }
    }
}
