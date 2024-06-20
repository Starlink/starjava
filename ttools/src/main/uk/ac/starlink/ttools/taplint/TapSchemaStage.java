package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import uk.ac.starlink.vo.TapVersion;
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

    protected MetadataHolder readTableMetadata( Reporter reporter,
                                                TapService tapService ) {
        TapVersion tapVersion = tapService.getTapVersion();
        reporter.report( FixedCode.I_TAPV,
                         "Validating for TAP version " + tapVersion );

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
        TableMap tmap = new TableMap( sList );

        /* Check column constraints in TAP_SCHEMA metadata -
         * see TAP 1.0 sec 2.6, TAP 1.1 sec 4.
         * This effectively duplicates some work done by the superclass,
         * but it includes some specifics laid out in the TAP_SCHEMA
         * requirements for TAP. */
        ColumnChecker colchecker =
            new ColumnChecker( reporter, tapRunner_, tapService, tmap );
        ColType tStr = ColType.STR;
        ColType tInt = ColType.INT;
        ColType tBool = ColType.BOOL;
        colchecker.checkColumns( "TAP_SCHEMA.schemas", new ColReq[] {
            new ColReq( "schema_name", tStr, true ),
            new ColReq( "utype", tStr, false ),
            new ColReq( "description", tStr, false ),
            new ColReq( "schema_index", tInt, false, true ),
        } );
        colchecker.checkColumns( "TAP_SCHEMA.tables", new ColReq[] {
            new ColReq( "schema_name", tStr, true ),
            new ColReq( "table_name", tStr, true ),
            new ColReq( "table_type", tStr, true ),
            new ColReq( "utype", tStr, false ),
            new ColReq( "description", tStr, false ),
            new ColReq( "table_index", tInt, false, true ),
        } );
        colchecker.checkColumns( "TAP_SCHEMA.columns", new ColReq[] {
            new ColReq( "table_name", tStr, true ),
            new ColReq( "column_name", tStr, true ),
            new ColReq( "datatype", tStr, true ),
            new ColReq( "arraysize", tStr, false, true ),
            new ColReq( "xtype", tStr, false, true ),
            new ColReq( "\"size\"", tInt, false ),
            new ColReq( "description", tStr, false ),
            new ColReq( "utype", tStr, false ),
            new ColReq( "unit", tStr, false ),
            new ColReq( "ucd", tStr, false ),
            new ColReq( "indexed", tBool, true ),
            new ColReq( "principal", tBool, true ),
            new ColReq( "std", tBool, true ),
            new ColReq( "column_index", tInt, false, true ),
        } );
        colchecker.checkColumns( "TAP_SCHEMA.keys", new ColReq[] {
            new ColReq( "key_id", tStr, true ),
            new ColReq( "from_table", tStr, true ),
            new ColReq( "target_table", tStr, true ),
            new ColReq( "description", tStr, false ),
            new ColReq( "utype", tStr, false ),
        } );
        colchecker.checkColumns( "TAP_SCHEMA.key_columns", new ColReq[] {
            new ColReq( "key_id", tStr, true ),
            new ColReq( "from_column", tStr, true ),
            new ColReq( "target_column", tStr, true ),
        } );

        /* Check foreign keys - see TAP 1.1 sec 4.4. */
        if ( tapVersion.is11() ) {
            ForeignKeyChecker fkchecker =
                new ForeignKeyChecker( reporter, "TAP_SCHEMA.", tmap );
            fkchecker.checkLink( "tables", "schema_name",
                                 "schemas", "schema_name" );
            fkchecker.checkLink( "columns", "table_name",
                                 "tables", "table_name" );
            fkchecker.checkLink( "keys", "from_table",
                                 "tables", "table_name" );
            fkchecker.checkLink( "keys", "target_table",
                                 "tables", "table_name" );
            fkchecker.checkLink( "key_columns", "key_id",
                                 "keys", "key_id" );
        }

        /* Check consistency of size and arraysize columns. */
        if ( tapVersion.is11() ) {
            String adql = "SELECT table_name, column_name, datatype, "
                               + "arraysize, \"size\" "
                        + "FROM TAP_SCHEMA.columns";
            TapQuery tq = tsi.createTapQuery( adql );
            StarTable table = tapRunner_.getResultTable( reporter, tq );
            if ( table != null ) {
                try {
                    RowSequence rseq = table.getRowSequence();
                    while ( rseq.next() ) {
                        Object[] row = rseq.getRow();
                        String tname = (String) row[ 0 ];
                        String cname = (String) row[ 1 ];
                        String dtype = (String) row[ 2 ];
                        String arraysize = (String) row[ 3 ];
                        Number size = (Number) row[ 4 ];
                        boolean isCharacter = "char".equals( dtype )
                                           || "unicodeChar".equals( dtype );
                        checkArraysize( reporter, tname, cname,
                                        arraysize, size, isCharacter );
                    }
                }
                catch ( Throwable e ) {
                    reporter.report( FixedCode.F_DTIO,
                                     "Trouble checking size/arraysize", e );
                }
            }
        }

        /* Return the schemas, if we managed to read any. */
        if ( sList == null ) {
            return null;
        }
        else {
            final SchemaMeta[] smetas = sList.toArray( new SchemaMeta[ 0 ] );
            return new MetadataHolder() {
                public SchemaMeta[] getTableMetadata() {
                    return smetas;
                }
                public boolean hasDetail() {
                    return true;
                }
            };
        }
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
     * Checks that the TAP_SCHEMA.columns columns arraysize and size
     * are consistent, in accordance with the text in TAP 1.1 sec 4.3.
     *
     * @param  reporter   message destination
     * @param  tname      table name, used for reporting
     * @param  cname      column name, used for reporting
     * @param  arraysize  value of arraysize column
     * @param  size       value of size column
     * @param  isCharacter  true for apparently character/string-like columns
     */
    private void checkArraysize( Reporter reporter, String tname, String cname,
                                 String arraysize, Number size,
                                 boolean isCharacter ) {
        String context = new StringBuffer()
            .append( tname )
            .append( "." )
            .append( cname )
            .append( ": arraysize=" )
            .append( arraysize )
            .append( "; size=" )
            .append( size )
            .toString();
        if ( arraysize == null ) {
            if ( size != null ) {
                reporter.report( FixedCode.E_TSSZ,
                                 "Non-null size for null arraysize: "
                               + context );
            }
        }
        else if ( "*".equals( arraysize ) ) {
            if ( size != null ) {
                reporter.report( FixedCode.E_TSSZ,
                                 "Arraysize/size mismatch: " + context );
            }
        }
        else if ( arraysize.matches( "[0-9]+[*]?" ) ) {
            String astxt = arraysize.endsWith( "*" )
                         ? arraysize.substring( 0, arraysize.length() - 1 )
                         : arraysize;
            long asize = Long.parseLong( astxt );
            if ( size == null || size.longValue() != asize ) {
                reporter.report( FixedCode.E_TSSZ,
                                 "Size does not match arraysize for vector: "
                               + context );
            }

            /* TAP 1.1 sec 4.3 says 'both arraysize and "size" must be null
             * for scalar numeric columns', so we exclude character-like
             * columns here.  That also means that people who want to use
             * arraysize=1 to get round problems with binary-encoded
             * VOTables and STIL votable.strict issues don't get warnings. */
            else if ( "1".equals( arraysize ) && ! isCharacter ) {
                reporter.report( FixedCode.W_TSZ1,
                                 "Questionable use of single-element array: "
                               + context );
            }
        }
        else if ( arraysize.matches( "([0-9]+x)+[0-9]*[0-9*]" ) ) {
            if ( size != null ) {
                reporter.report( FixedCode.E_TSSZ,
                                 "Non-null size does not match arraysize: "
                               + context );
            }
        }
        else {
            reporter.report( FixedCode.E_TSAZ,
                             "Bad arraysize syntax: " + context );
        }
    }

    /**
     * Performs some checking of the standard columns in a TAP_SCHEMA table.
     */
    private static class ColumnChecker {
        private final Reporter reporter_;
        private final TapRunner tapRunner_;
        private final TapService tapService_;
        private final TableMap tmap_;

        /**
         * Constructor.
         *
         * @param  reporter   message sink
         * @param  tapRunner   TAP runner
         * @param  tapService  TAP service
         * @param  tapVersion  version of TAP protocol
         * @param  tmap   map of TAP_SCHEMA-declared tables by name
         */
        ColumnChecker( Reporter reporter, TapRunner tapRunner,
                       TapService tapService, TableMap tmap ) {
            reporter_ = reporter;
            tapRunner_ = tapRunner;
            tapService_ = tapService;
            tmap_ = tmap;
        }

        /**
         * Checks a set of standard columns in a table.
         *
         * @param  tableName  TAP_SCHEMA table name to check
         * @param  colReqs    list of required standard columns in table
         */
        void checkColumns( String tableName, ColReq[] colReqs ) {
            boolean is11 = tapService_.getTapVersion().is11();

            /* Check table exists and prepare data structures. */
            TableMeta tmeta = tmap_.getTable( tableName );
            if ( tmeta == null ) {
                reporter_.report( FixedCode.E_TST0,
                                  "Missing required table " + tableName );
                return;
            }
            Map<String,ColumnMeta> colMap =
                new LinkedHashMap<String,ColumnMeta>();
            for ( ColumnMeta cmeta : tmeta.getColumns() ) {
                colMap.put( cmeta.getName().toLowerCase(), cmeta );
            }

            /* Iterate over each column suitable for the specified
             * TAP version. */
            for ( ColReq colReq : colReqs ) {
                if ( ! colReq.is11_ || is11 ) {
                    String ctxt = "column " + tmeta.getName()
                                + "." + colReq.name_;
                    ColumnMeta cmeta =
                        colMap.remove( colReq.name_.toLowerCase() );
                    if ( cmeta == null ) {
                        reporter_.report( FixedCode.E_TSC0,
                                          "Missing required " + ctxt );
                    }
                    else {

                        /* Check STD status declaration - all columns being
                         * checked here are standard ones. */
                        if ( ! cmeta.hasFlag( "std" ) ) { 
                            reporter_.report( FixedCode.E_TSTD,
                                              "Not declared STD " + ctxt );
                        }

                        /* Check declared datatype. */
                        String datatype = cmeta.getDataType();
                        String arraysize = cmeta.getArraysize();
                        ColType reqType = colReq.type_;
                        if ( is11 ) {
                            if ( ! reqType.isCompatibleTap11( datatype,
                                                              arraysize ) ) {
                               String msg = new StringBuffer()
                                  .append( "Type mismatch for " )
                                  .append( ctxt )
                                  .append( " datatype=" )
                                  .append( datatype )
                                  .append( " arraysize=" )
                                  .append( arraysize )
                                  .append( " is not " )
                                  .append( reqType.name11_ )
                                  .append( "-like" )
                                  .append( " (TAP 1.1)" )
                                  .toString();
                               reporter_.report( FixedCode.E_TSCT, msg );
                            }
                        }
                        else {

                            /* Given that TAP 1.0 has no real type system,
                             * (and for that reason, this checking is ad hoc)
                             * this can only be a warning, not an error;
                             * see TAP 1.0 Erratum #3. */
                            if ( ! reqType.isCompatibleTap10( datatype ) ) {
                               String msg = new StringBuffer()
                                  .append( "Possible type mismatch for " )
                                  .append( ctxt )
                                  .append( ": datatype=" )
                                  .append( datatype )
                                  .append( " does not look " )
                                  .append( reqType.name10_ )
                                  .append( "-like" )
                                  .append( " (TAP 1.0)" )
                                  .toString();
                               reporter_.report( FixedCode.W_TSCT, msg );
                            }
                        }
                    }

                    /* Check non-nullable columns have no nulls values */
                    if ( colReq.notNull_ ) {
                        String adql = new StringBuffer()
                           .append( "SELECT TOP 1 " )
                           .append( colReq.name_ )
                           .append( " FROM " )
                           .append( tableName )
                           .append( " WHERE " )
                           .append( colReq.name_ )
                           .append( " IS NULL" )
                           .toString();
                        TapQuery tq = new TapQuery( tapService_, adql, null );
                        StarTable table =
                            tapRunner_.getResultTable( reporter_, tq );
                        TableData tdata =
                            TableData.createTableData( reporter_, table );
                        if ( tdata != null && tdata.getRowCount() > 0 ) {
                            reporter_.report( FixedCode.E_TSNL,
                                              "Null values in non-nullable "
                                            + ctxt );
                        }
                    }
                }
            }

            /* Report non-standard columns for information. */
            if ( ! colMap.isEmpty() ) {
                String msg = new StringBuffer()
                   .append( colMap.size() )
                   .append( " non-standard columns in " )
                   .append( tableName )
                   .append( ": " )
                   .append( colMap.keySet() )
                   .toString();
                reporter_.report( FixedCode.I_TSNS, msg );
            }
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
         */
        void checkLink( String table1, String col1,
                        String table2, String col2 ) {
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
                reporter_.report( FixedCode.E_TSLN, msg );
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
     * Defines requirements on a standard column.
     */
    private static class ColReq {
        final String name_;
        final ColType type_;
        final boolean notNull_;
        final boolean is11_;


        /**
         * Constructs a column that is standard for TAP v1.0 and v1.1.
         *
         * @param  name  column name
         * @param  type  column type
         * @param  notNull   true iff column is forbidden to contain null values
         */
        ColReq( String name, ColType type, boolean notNull ) {
            this( name, type, notNull, false );
        }

        /**
         * Constructs a column that is standard for TAP v1.0 and maybe v1.1.
         *
         * @param  name  column name
         * @param  type  column type
         * @param  notNull   true iff column is forbidden to contain null values
         * @param  is11  true iff column is standard in TAP v1.1 but not v1.0
         */
        ColReq( String name, ColType type, boolean notNull, boolean is11 ) {
            name_ = name;
            type_ = type;
            notNull_ = notNull;
            is11_ = is11;
        }
    }

    /**
     * Enumeration of standard column data types.
     */
    private static enum ColType {
        STR( "VARCHAR", "string" ) {
            boolean isCompatibleTap11( String datatype, String arraysize ) {
                return ( "char".equals( datatype ) ||
                         "unicodeChar".equals( datatype ) )
                     && ( arraysize != null &&
                          arraysize.matches( "[0-9]*[0-9*]" ) );
            }
            boolean isCompatibleTap10( String datatype ) {
                return CompareMetadataStage
                      .compatibleDataTypes( "varchar", datatype );
            }
        },
        INT( "INTEGER", "integer" ) {
            boolean isCompatibleTap11( String datatype, String arraysize ) {
                return ( "unsignedByte".equals( datatype ) ||
                         "int".equals( datatype ) ||
                         "short".equals( datatype ) ||
                         "long".equals( datatype ) )
                    && ( arraysize == null || "1".equals( arraysize ) );
            }
            boolean isCompatibleTap10( String datatype ) {
                return CompareMetadataStage
                      .compatibleDataTypes( "integer", datatype );
            }
        },
        BOOL( "INTEGER", "integer" ) {
            boolean isCompatibleTap11( String datatype, String arraysize ) {
                return INT.isCompatibleTap11( datatype, arraysize );
            }
            boolean isCompatibleTap10( String datatype ) {
                return INT.isCompatibleTap10( datatype );
            }
        };
        final String name10_;
        final String name11_;

        /**
         * Constructor.
         *
         * @param  name10  type name for TAP v1.0
         * @param  name11  type name for TAP v1.1
         */
        ColType( String name10, String name11 ) {
            name10_ = name10;
            name11_ = name11;
        }

        /**
         * Indicates whether this type is compatible with given values
         * of the TAP v1.1 datatype and arraysize values.
         *
         * @param  datatype   value of datatype column in TAP_SCHEMA.columns
         * @param  arraysize  value of arraysize column in TAP_SCHEMA.columns
         * @return  true iff this type is compatible with the supplied metadta
         */
        abstract boolean isCompatibleTap11( String datatype, String arraysize );

        /**
         * Indicates whether this type is compatible with given values
         * of the TAP v1.0 datatype value.
         * Since TAP 1.0 has no type system (see TAP 1.0 Erratum #3)
         * this test is only advisory, it cannot report a true error.
         *
         * @param  datatype   value of datatype column in TAP_SCHEMA.columns
         * @return  true iff this type looks compatible
         *          with the supplied metadta
         */
        abstract boolean isCompatibleTap10( String datatype );
    }

    /**
     * TapSchemaInterrogator implementation which augments the default
     * implementation a bit for more checking.
     */
    private static class LintTapSchemaInterrogator
           extends TapSchemaInterrogator {
        private final Reporter reporter_;
        private final TapRunner tapRunner_;
        private static Integer BOOL_TRUE = Integer.valueOf( 1 );
        private static Integer BOOL_FALSE = Integer.valueOf( 0 );

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
                    cinfo.getAuxDatumValue( VOStarTable.DATATYPE_INFO,
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

        @Override
        public TapQuery createTapQuery( String adql ) {
            return super.createTapQuery( adql );
        }
    }
}
