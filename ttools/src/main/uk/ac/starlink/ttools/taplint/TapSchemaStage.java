package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.SchemaMeta;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapSchemaInterrogator;
import uk.ac.starlink.vo.TapService;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Validation stage for checking table metadata from the TAP_SCHEMA tables.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2011
 */
public class TapSchemaStage extends TableMetadataStage {

    private final TapRunner tapRunner_;

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
    public void run( Reporter reporter, TapService tapService ) {
        super.run( reporter, tapService );
        tapRunner_.reportSummary( reporter );
    }

    protected SchemaMeta[] readTableMetadata( Reporter reporter,
                                              TapService tapService ) {
        boolean isTap11 = tapService.getTapVersion().is11();

        /* Work out the MAXREC value to use for metadata queries.
         * If this is not set, and the service default value is used,
         * then metadata will be truncated when read from services
         * with large column counts (or conceivably table counts etc).
         * So we work out the row count of the largest metadata table
         * and use that for the maxrec value for all metadata queries.
         * There are other possibilities for doing this more carefully,
         * for instance checking after each query that the table has
         * not been truncated when read. */
        int maxrec = getMetaMaxrec( reporter, tapService, tapRunner_ ) + 10;
        LintTapSchemaInterrogator tsi =
            new LintTapSchemaInterrogator( reporter, tapService, maxrec,
                                           tapRunner_ );

        /* Perform some column-specific checks. */
        try {
            tsi.checkColumnTypes();
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_CERR,
                             "Error reading TAP_SCHEMA.columns data", e );
        }

        /* Read all the metadata items, not filled in with structure. */
        Map<String,List<ColumnMeta>> cMap;
        try {
            cMap = tsi.readMap( TapSchemaInterrogator.COLUMN_QUERIER, null );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_CLIO,
                             "Error reading TAP_SCHEMA.columns table", e );
            cMap = new HashMap<String,List<ColumnMeta>>();
        }
        Map<String,List<ForeignMeta.Link>> lMap;
        try {
            lMap = tsi.readMap( TapSchemaInterrogator.LINK_QUERIER, null );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_KCIO,
                             "Error reading TAP_SCHEMA.key_columns table", e );
            lMap = new HashMap<String,List<ForeignMeta.Link>>();
        }
        Map<String,List<ForeignMeta>> fMap;
        try {
            fMap = tsi.readMap( TapSchemaInterrogator.FKEY_QUERIER, null );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_FKIO,
                             "Error reading TAP_SCHEMA.keys table", e );
            fMap = new HashMap<String,List<ForeignMeta>>();
        }
        Map<String,List<TableMeta>> tMap;
        try {
            tMap = tsi.readMap( TapSchemaInterrogator.TABLE_QUERIER, null );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_TBIO,
                             "Error reading TAP_SCHEMA.tables table", e );
            tMap = new HashMap<String,List<TableMeta>>();
        }
        List<SchemaMeta> sList;
        try {
            sList = tsi.readList( TapSchemaInterrogator.SCHEMA_QUERIER,
                                  null );
        }
        catch ( IOException e ) {
            reporter.report( FixedCode.E_SCIO,
                             "Error reading TAP_SCHEMA.schemas table", e );
            sList = null;
        }

        /* Put these together to form the hierarchical structure
         * described by the TAP_SCHEMA tables. */
        for ( List<ForeignMeta> flist : fMap.values() ) {
            for ( ForeignMeta fmeta : flist ) {
                tsi.populateForeignKey( fmeta, lMap );
            }
        }
        checkEmpty( reporter, lMap, FixedCode.W_FLUN, "key_columns" );
        for ( List<TableMeta> tlist : tMap.values() ) {
            for ( TableMeta tmeta : tlist ) {
                tsi.populateTable( tmeta, fMap, cMap );
            }
        }
        checkEmpty( reporter, fMap, FixedCode.W_FKUN, "keys" );
        checkEmpty( reporter, cMap, FixedCode.W_CLUN, "columns" );
        if ( sList != null ) {
            for ( SchemaMeta smeta : sList ) {
                tsi.populateSchema( smeta, tMap );
            }
            checkEmpty( reporter, tMap, FixedCode.W_TBUN, "tables" );
        }

        /* Check foreign keys - see TAP 1.1 sec 4.4. */
        if ( isTap11 ) {
            ForeignKeyChecker fkchecker =
                new ForeignKeyChecker( reporter, "TAP_SCHEMA.",
                                       new TableMap( sList ) );
            fkchecker.checkLink( "tables", "schema_name",
                                "schemas", "schema_name", true );
            fkchecker.checkLink( "columns", "table_name",
                                "tables", "table_name", true );
            fkchecker.checkLink( "keys", "from_table",
                                "tables", "table_name", true );
            fkchecker.checkLink( "keys", "target_table",
                                "tables", "table_name", true );
            fkchecker.checkLink( "key_columns", "key_id",
                                "keys", "key_id", true );
            fkchecker.checkLink( "key_columns", "from_column",
                                "columns", "column_name", false );
            fkchecker.checkLink( "key_columns", "target_column",
                                "columns", "column_name", false );
        }

        /* Return the schemas, if we managed to read any. */
        return sList == null ? null : sList.toArray( new SchemaMeta[ 0 ] );
    }

    /**
     * Returns the maximum record count that will be required
     * to retrieve all the TAP_SCHEMA metadata items.
     *
     * @param  reporter    destination for validation messages
     * @param  tapService   TAP service description
     * @param  tapRunner   object to perform TAP queries
     * @return   maximum record count required for metadata queries,
     *           or 0 if it could not be determined
     */
    private int getMetaMaxrec( Reporter reporter, TapService tapService,
                               TapRunner tapRunner ) {
        String[] tnames = new String[] {
            "TAP_SCHEMA.schemas",
            "TAP_SCHEMA.tables",
            "TAP_SCHEMA.columns",
            "TAP_SCHEMA.keys",
            "TAP_SCHEMA.key_columns",
        };
        int maxrec = 0;
        for ( String tname : tnames ) {
            int nr = Math.max( maxrec, getRowCount( reporter, tapService,
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
     * @param  tapService  TAP service description
     * @param  tapRunner   object to perform TAP queries
     * @param  tname       name of table in TAP db
     * @return   number of rows counted, or -1 if some error
     */
    private int getRowCount( Reporter reporter, TapService tapService,
                             TapRunner tapRunner, String tname ) {
        String adql = "SELECT COUNT(*) AS nr FROM " + tname;
        TapQuery tq = new TapQuery( tapService, adql, null );
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
                        reporter.report( FixedCode.E_NONM,
                                         "Non-numeric return cell from "
                                       + adql );
                        return -1;
                    }
                }
                else {
                    reporter.report( FixedCode.E_NO11,
                                     "Expecting nrow=1, ncol=1, got"
                                   + " nrow=" + result.getRowCount()
                                   + " ncol=" + result.getColumnCount()
                                   + " from " + adql );
                    return -1;
                }
            }
            catch ( IOException e ) {
                reporter.report( FixedCode.E_NRER,
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
                             ReportCode code, String stName ) {
        for ( Object key : map.keySet() ) {
            reporter.report( code,
                             "Unused entry in TAP_SCHEMA." + stName
                           + " table: " + key );
        }
    }

    /**
     * Checks the presence of given foreign keys.
     */
    private static class ForeignKeyChecker {
        private final Reporter reporter_;
        private final String tablePrefix_;
        private final TableMap tmap_;

        /**
         * Constructor.
         *
         * @param  reporter  message sink
         * @param  tablePrefix  string to be prefixed to all table names
         *                      being checked
         * @param  tmap  map  of tables by name
         */
        ForeignKeyChecker( Reporter reporter, String tablePrefix,
                           TableMap tmap  ) {
            reporter_ = reporter;
            tablePrefix_ = tablePrefix;
            tmap_ = tmap;
        }

        /**
         * Checks for presence of a known link, and emits a report if
         * it is not present.  The parameters are treated case-insensitively.
         *
         * @param  table1  source table, missing prefix
         * @param  col1    column in source table
         * @param  table   target table, missing prefix
         * @param  col2    column in target table
         * @param  isMust  true for MUST (error), false for SHOULD (warning)
         */
        void checkLink( String table1, String col1, String table2, String col2,
                        boolean isMust ) {
            String fqTable1 = tablePrefix_ + table1;
            String fqTable2 = tablePrefix_ + table2;
            TableMeta tmeta = tmap_.getTable( fqTable1.toLowerCase() );
            if ( tmeta != null ) {
                for ( ForeignMeta fmeta : tmeta.getForeignKeys() ) {
                    if ( fqTable2.equalsIgnoreCase( fmeta.getTargetTable() ) ) {
                        ForeignMeta.Link[] links = fmeta.getLinks();
                        if ( links.length == 1 &&
                             col1.equalsIgnoreCase( links[ 0 ].getFrom() ) &&
                             col2.equalsIgnoreCase( links[ 0 ].getTarget() ) ) {
                            return;
                        }
                    }
                }
                String msg = new StringBuffer() 
                   .append( "Missing foreign key " )
                   .append( fqTable1 )
                   .append( "." )
                   .append( col1 )
                   .append( " -> " )
                   .append( fqTable2 )
                   .append( "." )
                   .append( col2 )
                   .toString();
                reporter_.report( isMust ? FixedCode.E_TSLN
                                         : FixedCode.W_TSLN,
                                  msg );
            }
        }
    }

    /**
     * Map from case-insensitive table name to TableMeta object.
     */
    private static class TableMap {
        private final Map<String,TableMeta> tmap_;

        /**
         * Constructor.
         *
         * @param  schemaList  list of populated schema metadata objects
         */
        TableMap( List<SchemaMeta> schemaList ) {
            tmap_ = new HashMap<String,TableMeta>();
            if ( schemaList != null ) {
                for ( SchemaMeta smeta : schemaList ) {
                    for ( TableMeta tmeta : smeta.getTables() ) {
                        tmap_.put( tmeta.getName().toLowerCase(), tmeta );
                    }
                }
            }
        }

        /**
         * Table metadata for given name.
         *
         * @param  name  case-insensitive table name
         * @return  table metadata object, or null
         */
        TableMeta getTable( String name ) {
            return tmap_.get( name.toLowerCase() );
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
         * @param  tapService  TAP service description
         * @param  maxrec     maximum record count (0 for default limit)
         * @param  tapRunner  object to perform TAP queries
         */
        public LintTapSchemaInterrogator( Reporter reporter,
                                          TapService tapService,
                                          int maxrec, TapRunner tapRunner ) {
            super( tapService, maxrec, ContentCoding.NONE );
            reporter_ = reporter;
            tapRunner_ = tapRunner;
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
        void checkColumnTypes() throws IOException {

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
                    reporter_.report( FixedCode.E_CINT, msg );
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
                                reporter_.report( FixedCode.E_CLOG, msg );
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
