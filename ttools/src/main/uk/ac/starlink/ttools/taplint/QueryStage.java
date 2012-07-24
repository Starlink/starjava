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
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage which performs some ADQL queries on data tables.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2011
 */
public class QueryStage implements Stage {

    private final VotLintTapRunner tapRunner_;
    private final MetadataHolder metaHolder_;
    private final CapabilityHolder capHolder_;

    /**
     * Constructor.
     *
     * @param  tapRunner    object that can run TAP queries
     * @param  metaHolder   provides table metadata at run time
     * @param  capHolder    provides capability info at run time;
     *                      may be null if capability-related queries
     *                      are not required
     */
    public QueryStage( VotLintTapRunner tapRunner, MetadataHolder metaHolder,
                       CapabilityHolder capHolder ) {
        tapRunner_ = tapRunner;
        metaHolder_ = metaHolder;
        capHolder_ = capHolder;
    }

    public String getDescription() {
        return "Make ADQL queries in " + tapRunner_.getDescription()
             + " mode";
    }

    public void run( Reporter reporter, URL serviceUrl ) {
        TableMeta[] tmetas = metaHolder_.getTableMetadata();
        String[] adqlLangs = capHolder_ == null
                           ? null
                           : getAdqlLanguages( capHolder_ );
        if ( tmetas == null || tmetas.length == 0 ) {
            reporter.report( ReportType.FAILURE, "NOTM",
                             "No table metadata available "
                           + "(earlier stages failed/skipped?) "
                           + "- will not run test queries" );
            return;
        }
        List<TableMeta> tmList = new ArrayList<TableMeta>();
        for ( int i = 0; i < tmetas.length; i++ ) {
            TableMeta tmeta = tmetas[ i ];
            if ( ! tmeta.getName().toUpperCase().startsWith( "TAP_SCHEMA." ) ) {
                tmList.add( tmeta );
            }
        }
        if ( ! tmList.isEmpty() ) {
            tmetas = tmList.toArray( new TableMeta[ 0 ] );
        }
        new Querier( reporter, serviceUrl, tmetas, adqlLangs ).run();
        tapRunner_.reportSummary( reporter );
    }

    /**
     * Returns all the known ADQL variants for a given capabilities element.
     *
     * @param  capHolder   capabilities metadata info supplier
     * @return  array of ADQL* language specifiers
     */
    private static String[] getAdqlLanguages( CapabilityHolder capHolder ) {
        TapCapability tcap = capHolder.getCapability();
        if ( tcap == null ) {
            return new String[] { "ADQL", "ADQL-2.0" };  // questionable?
        }
        String[] langs = tcap.getLanguages();
        List<String> adqlLangList = new ArrayList<String>();
        for ( int il = 0; il < langs.length; il++ ) {
            String lang = langs[ il ];
            if ( lang.startsWith( "ADQL" ) ) {
                adqlLangList.add( lang );
            }
        }
        if ( ! adqlLangList.contains( "ADQL" ) ) {
            adqlLangList.add( "ADQL" );
        }
        return adqlLangList.toArray( new String[ 0 ] );
    }

    /**
     * Utility class to perform and check TAP queries.
     */
    private class Querier implements Runnable {
        private final Reporter reporter_;
        private final URL serviceUrl_;
        private final String[] adqlLangs_;
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
        Querier( Reporter reporter, URL serviceUrl, TableMeta[] tmetas,
                 String[] adqlLangs ) {
            reporter_ = reporter;
            serviceUrl_ = serviceUrl;
            adqlLangs_ = adqlLangs;

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
            runOneColumn( tmeta1_ );
            runSomeColumns( tmeta2_ );
            runJustMeta( tmeta3_ );
        }

        /**
         * Runs some tests using all the columns in an example table.
         *
         * @param  tmeta  table to test
         */
        private void runOneColumn( TableMeta tmeta ) {
            final int nr0 = 10;
            String tname = tmeta.getName();

            /* Prepare an array of column specifiers corresponding to
             * the first single column in the table. */
            ColumnMeta cmeta1 = tmeta.getColumns()[ 0 ];
            ColSpec cspec1 = new ColSpec( cmeta1 );

            /* Check that limiting using TOP works. */
            String tAdql = new StringBuilder()
               .append( "SELECT TOP " )
               .append( nr0 )
               .append( " " )
               .append( cspec1.getQueryText() )
               .append( " FROM " )
               .append( tname )
               .toString();
            StarTable t1 =
                runCheckedQuery( tAdql, -1, new ColSpec[] { cspec1 }, nr0 );
            long nr1 = t1 != null ? t1.getRowCount() : 0;
            assert nr1 >= 0;

            /* Check that limiting using MAXREC works. */
            if ( nr1 > 0 ) {
                int nr2 = Math.min( (int) nr1 - 1, nr0 - 1 );
                String mAdql = new StringBuilder()
                   .append( "SELECT " )
                   .append( cspec1.getQueryText() )
                   .append( " FROM " )
                   .append( tname )
                   .toString();
                StarTable t2 =
                    runCheckedQuery( mAdql, nr2, new ColSpec[] { cspec1 }, -1 );
                if ( t2 != null && ! tapRunner_.isOverflow( t2 ) ) {
                    String msg = "Overflow not marked - no "
                               + "<INFO name='QUERY_STATUS' value='OVERFLOW'/> "
                               + "after TABLE";
                    reporter_.report( ReportType.ERROR, "OVNO", msg );
                }
            }

            /* Check that all known variants (typically "ADQL" and "ADQL-2.0")
             * work the same. */
            if ( adqlLangs_ != null && adqlLangs_.length > 1 ) {
                List<String> okLangList = new ArrayList<String>();
                List<String> failLangList = new ArrayList<String>();
                for ( int il = 0; il < adqlLangs_.length; il++ ) {
                    String lang = adqlLangs_[ il ];
                    String vAdql = new StringBuilder()
                       .append( "SELECT TOP 1 " )
                       .append( cspec1.getQueryText() )
                       .append( " FROM " )
                       .append( tname )
                       .toString();
                    Map<String,String> extraParams =
                        new HashMap<String,String>();
                    extraParams.put( "LANG", lang );
                    StarTable result;
                    try {
                        TapQuery tq = new TapQuery( serviceUrl_, vAdql,
                                                    extraParams, null, 0 );
                        result = tapRunner_.getResultTable( reporter_, tq );
                    }
                    catch ( IOException e ) {
                        result = null;
                    }
                    ( result == null ? failLangList
                                     : okLangList ).add( lang );
                }
                if ( ! failLangList.isEmpty() && ! okLangList.isEmpty() ) {
                    String msg = new StringBuilder()
                       .append( "Some ADQL language variants fail: " )
                       .append( okLangList )
                       .append( " works, but " )
                       .append( failLangList )
                       .append( " doesn't" )
                       .toString();
                    reporter_.report( ReportType.ERROR, "LVER", msg );
                }
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
                    cspec.setRename( "taplint_c_" + ( ix + 1 ) );
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
         * @param  maxrow  expected maximum row count, different from MAXREC;
         *                 if negative ignored
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
                reporter_.report( ReportType.ERROR, "TSER",
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
                    reporter_.report( ReportType.ERROR, "NREC", msg );
                }
                if ( maxrow >= 0 && nrow > maxrow ) {
                    String msg = new StringBuffer()
                       .append( "More rows than expected (" )
                       .append( nrow )
                       .append( ") returned" )
                       .append( " for " )
                       .append( adql )
                       .toString();
                    reporter_.report( ReportType.ERROR, "NROW", msg );
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
                reporter_.report( ReportType.ERROR, "NCOL", msg );
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
                    reporter_.report( ReportType.ERROR, "CNAM", msg );
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
                    reporter_.report( ReportType.ERROR, "CTYP", msg );
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
