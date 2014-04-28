package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapSchemaInterrogator;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage for checking table metadata from the TAP_SCHEMA tables.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapSchemaStage extends TableMetadataStage {

    private final TapRunner tapRunner_;
    private TableMeta[] tmetas_;

    /**
     * Constructor.
     *
     * @param  tapRunner  object to perform TAP queries
     */
    public TapSchemaStage( TapRunner tapRunner ) {
        super( "TAP_SCHEMA",
               new String[] { "indexed", "principal", "std", }, false );
        tapRunner_ = tapRunner;
    }

    @Override
    public void run( Reporter reporter, URL serviceUrl ) {
        super.run( reporter, serviceUrl );
        tapRunner_.reportSummary( reporter );
    }

    protected TableMeta[] readTableMetadata( Reporter reporter,
                                             URL serviceUrl ) {

        /* Work out the MAXREC value to use for metadata queries.
         * If this is not set, and the service default value is used,
         * then metadata will be truncated when read from services
         * with large column counts (or conceivably table counts etc).
         * So we work out the row count of the largest metadata table
         * and use that for the maxrec value for all metadata queries.
         * There are other possibilities for doing this more carefully,
         * for instance checking after each query that the table has
         * not been truncated when read. */
        int maxrec = getMetaMaxrec( reporter, serviceUrl, tapRunner_ ) + 10;
        TapSchemaInterrogator tsi =
            new LintTapSchemaInterrogator( reporter, serviceUrl, maxrec,
                                           tapRunner_ );
        Map<String,List<ColumnMeta>> cMap;
        try {
            cMap = tsi.readColumns();
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "CLIO",
                             "Error reading TAP_SCHEMA.columns table", e );
            cMap = new HashMap<String,List<ColumnMeta>>();
        }

        Map<String,List<ForeignMeta.Link>> lMap;
        try {
            lMap = tsi.readForeignLinks();
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "FLIO",
                             "Error reading TAP_SCHEMA.key_columns table", e );
            lMap = new HashMap<String,List<ForeignMeta.Link>>();
        }

        Map<String,List<ForeignMeta>> fMap;
        try {
            fMap = tsi.readForeignKeys( lMap );
            checkEmpty( reporter, lMap, "FLUN", "key_columns" );
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "FKIO",
                             "Error reading TAP_SCHEMA.keys table", e );
            fMap = new HashMap<String,List<ForeignMeta>>();
        }

        List<TableMeta> tList;
        try {
            tList = tsi.readTables( cMap, fMap );
            checkEmpty( reporter, cMap, "CLUN", "columns" );
            checkEmpty( reporter, fMap, "FKUN", "keys" );
        }
        catch ( IOException e ) {
            tList = null;
            reporter.report( ReportType.ERROR, "TBIO",
                             "Error reading TAP_SCHEMA.tables table", e );
        }

        return tList == null ? null
                             : tList.toArray( new TableMeta[ 0 ] );
    }

    /**
     * Returns the maximum record count that will be required
     * to retrieve all the TAP_SCHEMA metadata items.
     *
     * @param  reporter    destination for validation messages
     * @param  serviceUrl  TAP service URL
     * @param  tapRunner   object to perform TAP queries
     * @return   maximum record count required for metadata queries,
     *           or 0 if it could not be determined
     */
    private int getMetaMaxrec( Reporter reporter, URL serviceUrl,
                               TapRunner tapRunner ) {
        String[] tnames = new String[] {
            "TAP_SCHEMA.tables",
            "TAP_SCHEMA.columns",
            "TAP_SCHEMA.keys",
            "TAP_SCHEMA.key_columns",
        };
        int maxrec = 0;
        for ( String tname : Arrays.asList( tnames ) ) {
            int nr = Math.max( maxrec, getRowCount( reporter, serviceUrl,
                                                    tapRunner, tname ) );
            if ( nr < 0 ) {
                return 0;
            }
            else {
                maxrec = Math.max( maxrec, nr );
            }
        }
        return maxrec;
    }

    /**
     * Returns the number of rows in a named TAP table.
     *
     * @param  reporter    destination for validation messages
     * @param  serviceUrl  TAP service URL
     * @param  tapRunner   object to perform TAP queries
     * @param  tname       name of table in TAP db
     * @return   number of rows counted, or -1 if some error
     */
    private int getRowCount( Reporter reporter, URL serviceUrl,
                             TapRunner tapRunner, String tname ) {
        String adql = "SELECT COUNT(*) AS nr FROM " + tname;
        TapQuery tq = new TapQuery( serviceUrl, adql, null );
        StarTable result = tapRunner.getResultTable( reporter, tq );
        if ( result != null ) {
            try {
                result = Tables.randomTable( result );
                if ( result.getColumnCount() == 1 &&
                     result.getRowCount() == 1 ) {
                    Object cell = result.getCell( 0, 0 );
                    if ( cell instanceof Number ) {
                        return ((Number) cell).intValue();
                    }
                    else {
                        reporter.report( ReportType.ERROR, "NONM",
                                         "Non-numeric return cell from "
                                       + adql );
                        return -1;
                    }
                }
                else {
                    reporter.report( ReportType.ERROR, "NO11",
                                     "Expecting nrow=1, ncol=1, got"
                                   + " nrow=" + result.getRowCount()
                                   + " ncol=" + result.getColumnCount()
                                   + " from " + adql );
                    return -1;
                }
            }
            catch ( IOException e ) {
                reporter.report( ReportType.ERROR, "NRER",
                                 "Error counting rows with " + adql, e );
                return -1;
            }
        }
        else {
            return -1;
        }
    }

    /**
     * Check that a map is empty, and report on any entries that are present.
     * The maps that this checks should be empty since their entries are
     * removed as their content is used by the metadata reading routines
     * to populate higher classes in the metadata hierarchy.
     *
     * @param   reporter   destination for validation messages
     * @param   map   map to check
     * @param   code  reporting code for unused entries
     * @param   stName   unqualified TAP_SCHEMA table name
     */
    private void checkEmpty( Reporter reporter, Map<?,?> map,
                             String code, String stName ) {
        for ( Object key : map.keySet() ) {
            reporter.report( ReportType.WARNING, code,
                             "Unused entry in TAP_SCHEMA." + stName
                           + " table: " + key );
        }
    }

    /**
     * TapSchemaInterrogator implementation which augments the default
     * implementation a bit for more checking.
     */
    private static class LintTapSchemaInterrogator
           extends TapSchemaInterrogator {
        private final Reporter reporter_;
        private final TapRunner tapRunner_;
        private static Integer BOOL_TRUE = new Integer( 1 );
        private static Integer BOOL_FALSE = new Integer( 0 );

        /**
         * Constructor.
         *
         * @param  reporter  validation message destination
         * @param  serviceUrl  TAP service URL
         * @param  maxrec     maximum record count (0 for default limit)
         * @param  tapRunner  object to perform TAP queries
         */
        public LintTapSchemaInterrogator( Reporter reporter, URL serviceUrl,
                                          int maxrec, TapRunner tapRunner ) {
            super( serviceUrl, maxrec );
            reporter_ = reporter;
            tapRunner_ = tapRunner;
        }

        @Override
        public Map<String,List<ColumnMeta>> readColumns() throws IOException {
            Map<String,List<ColumnMeta>> cMap = super.readColumns();
            try {
                checkColumnTypes();
            }
            catch ( IOException e ) {
                reporter_.report( ReportType.ERROR, "CERR",
                                  "Error reading TAP_SCHEMA.columns data", e );
            }
            return cMap;
        }

        @Override
        protected StarTable executeQuery( TapQuery tq )
                throws IOException {
            try {
                return tapRunner_.attemptGetResultTable( reporter_, tq );
            }
            catch ( SAXException e ) {
                throw (IOException)
                      new IOException( "Result parse error: " + e.getMessage() )
                     .initCause( e );
            }
        }

        /**
         * Performs checking and reporting on known column types for 
         * TAP_SCHEMA.columns table.
         */
        private void checkColumnTypes() throws IOException {

            /* Note names and characteristics of numeric columns.
             * All others are strings. */
            String[] colNames = { "principal", "indexed", "std", "size" };
            String[] colTypes = { "int", "int", "int", "int" };
            boolean[] colBools = { true, true, true, false };

            /* Perform the query.  Note size has to be quoted since it is
             * an ADQL reserved word. */
            String adql = "SELECT principal, indexed, std, \"size\" "
                        + "FROM TAP_SCHEMA.columns";
            StarTable table = executeQuery( createTapQuery( adql ) );
            int ncol = Math.min( colNames.length, table.getColumnCount() );

            /* Check column types - they should all be integers. */
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnInfo cinfo = table.getColumnInfo( ic );
                String type =
                    (String) cinfo.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
                                                     String.class );
                if ( ! colTypes[ ic ].equals( type ) ) {
                    String msg = new StringBuffer()
                       .append( "Column " )
                       .append( colNames[ ic ] )
                       .append( " in " )
                       .append( "TAP_SCHEMA.columns" )
                       .append( " has wrong type " )
                       .append( type )
                       .append( " not " )
                       .append( colTypes[ ic ] )
                       .toString();
                    reporter_.report( ReportType.ERROR, "CINT", msg );
                    colBools[ ic ] = false;
                }
            }

            /* Check the column values for booleans.  They should be either
             * zero or one. */
            RowSequence rseq = table.getRowSequence();
            try {
                while ( rseq.next() ) {
                    Object[] row = rseq.getRow();
                    for ( int ic = 0; ic < ncol; ic++ ) {
                        if ( colBools[ ic ] ) {
                            Object cell = row[ ic ];
                            if ( ! BOOL_TRUE.equals( cell ) &&
                                 ! BOOL_FALSE.equals( cell ) ) {
                                String msg = new StringBuffer()
                                   .append( "Non-boolean value " )
                                   .append( cell )
                                   .append( " in TAP_SCHEMA.columns column " )
                                   .append( colNames[ ic ] )
                                   .toString();
                                reporter_.report( ReportType.ERROR, "CLOG",
                                                  msg );
                            }
                        }
                    }
                }
            }
            finally {
                rseq.close();
            }
        }
    }
}
