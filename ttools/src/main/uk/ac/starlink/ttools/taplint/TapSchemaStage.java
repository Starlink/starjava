package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
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
        TapSchemaInterrogator tsi =
            new LintTapSchemaInterrogator( reporter, serviceUrl, tapRunner_ );
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
     * Check that a map is empty, and report on any entries that are present.
     * The maps that this checks should be empty since their entries are
     * removed as their content is used by the metadata reading routines
     * to populate higher classes in the metadata hierarchy.
     *
     * @param   reporter   destination for validation messages
     * @param   map   map to check
     * @param   code  reporting code for unused entries
     * @parma   stName   unqualified TAP_SCHEMA table name
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
         * @param  tapRunner  object to perform TAP queries
         */
        public LintTapSchemaInterrogator( Reporter reporter, URL serviceUrl,
                                          TapRunner tapRunner ) {
            super( serviceUrl );
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
