package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;

/**
 * TapMetaReader implementation that works with the proposed VOSI-1.1
 * two-stage /tables endpoint proposed by Pat Dowler (grid list,
 * 4 May 2015) and implemented at CADC.
 * Schemas may be retrieved with or without table lists,
 * and tables or schemas may be retrieved individually (by name)
 * or collectively.
 *
 * @author   Mark Taylor
 * @since    15 May 2015
 */
public class CadcTapMetaReader implements TapMetaReader {

    private final URL url_;
    private final Config config_;
    private final ContentCoding coding_;
    private final Map<String,String> schemaMap_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructs a default instance.
     *
     * @param    tablesetUrl  URL of TAPVizieR service followed by /tables
     */
    public CadcTapMetaReader( String tablesetUrl ) {
        this( tablesetUrl, Config.POPULATE_SCHEMAS, ContentCoding.GZIP );
    }

    /**
     * Constructs an instance with a configurable metadata object population
     * policy.
     *
     * @param    tablesetUrl  URL of TAPVizieR service followed by /tables
     * @param   config  population configuration
     * @param    coding   configures HTTP content-coding
     */
    public CadcTapMetaReader( String tablesetUrl, Config config,
                              ContentCoding coding ) {
        try {
            url_ = new URL( tablesetUrl );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalArgumentException( "Not a URL: " + tablesetUrl );
        }
        config_ = config;
        coding_ = coding;
        schemaMap_ = new HashMap<String,String>();
    }

    public SchemaMeta[] readSchemas() throws IOException {
        SchemaMeta[] schemas = populateHandler( "", config_.schemaDetail_ )
                              .getSchemas();
        TapSchemaTapMetaReader.sortSchemas( schemas );
        int nTable = 0;
        int nHasCols = 0;
        int nHasFkeys = 0;
        boolean popTables = config_.populateTables_;
        for ( SchemaMeta schema : schemas ) {
            String sname = schema.getName();
            TableMeta[] tables = schema.getTables();
            if ( tables != null ) {
                TapSchemaTapMetaReader.sortTables( tables );
                for ( TableMeta table : tables ) {
                    nTable++;
                    schemaMap_.put( table.getName(), sname );
                    if ( ! popTables ) {
                        ColumnMeta[] cols = table.getColumns();
                        if ( cols != null ) {
                            if ( cols.length == 0 ) {
                                table.setColumns( null );
                            }
                            else {
                                nHasCols++;
                            }
                        }
                        ForeignMeta[] fkeys = table.getForeignKeys();
                        if ( fkeys != null ) {
                            if ( fkeys.length == 0 ) {
                                table.setForeignKeys( null );
                            }
                            else {
                                nHasFkeys++;
                            }
                        }
                    }
                }
            }
        }
        if ( ! popTables && ( nHasCols > 0 || nHasFkeys > 0 ) ) {
            logger_.warning( "Got unexpected table content metadata "
                           + "(" + nTable + " tables: "
                           + nHasCols + " with cols, "
                           + nHasFkeys + " with fkeys) "
                           + "- might as well keep it" );
        }
        return schemas;
    }

    public TableMeta[] readTables( SchemaMeta schema ) throws IOException {
        String sname = schema.getName();
        SchemaMeta[] schemas = 
             populateHandler( "/" + sname, config_.tableDetail_ )
            .getSchemas();
        int ns = schemas.length;
        if ( ns == 1 ) {
            TableMeta[] tables = schemas[ 0 ].getTables();
            TapSchemaTapMetaReader.sortTables( tables );
            for ( TableMeta table : tables ) {
                schemaMap_.put( table.getName(), sname );
            }
            return tables;
        }
        else if ( ns == 0 ) {
            throw new IOException( "No schemas" );
        }
        else {
            throw new IOException( "Non-unique schema" );
        }
    }

    public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
        return readSingleTable( table ).getColumns();
    }

    public ForeignMeta[] readForeignKeys( TableMeta table ) throws IOException {
        return readSingleTable( table ).getForeignKeys();
    }

    public String getSource() {
        return url_.toString();
    }

    public String getMeans() {
        return "CADC-variant VOSI TableSet, " + config_;
    }

    /**
     * Reads a tableset document from the base URL of this reader,
     * as modified by a given subpath and detail query string.
     *
     * @param  subPath  url subpath
     * @param  detail   value of "detail" query parameter
     * @return   handler that has performed a parse on the URL corresponding
     *           to the given arguments
     */
    private TableSetSaxHandler populateHandler( String subPath, String detail )
            throws IOException {
        String surl = url_.toString();
        if ( subPath != null && subPath.length() > 0 ) {
            surl += "/" + subPath;
        }
        if ( detail != null && detail.length() > 0 ) {
            surl += "?detail=" + detail;
        }
        URL url = new URL( surl );
        logger_.info( "Reading table metadata from " + url );
        try {
            return TableSetSaxHandler.populateHandler( url, coding_ );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Invalid TableSet XML document" )
                 .initCause( e );
        }
    }

    /**
     * Reads a table metadata object fully populated with column and
     * foreign key information.
     *
     * @param   table   object indicating what table is required;
     *                  presumably not populated with columns and keys
     * @return   table metadata object populated with columns and keys
     */
    private TableMeta readSingleTable( TableMeta table ) throws IOException {
        String tname = table.getName();
        String sname = schemaMap_.get( tname );
        if ( sname != null ) {
            String subPath = sname + "/" + tname;
            TableSetSaxHandler tsHandler = populateHandler( subPath, "" );
            List<TableMeta> tlist = new ArrayList<TableMeta>();
            tlist.addAll( Arrays.asList( tsHandler.getNakedTables() ) );
            for ( SchemaMeta schema : tsHandler.getSchemas() ) {
                TableMeta[] tables = schema.getTables();
                if ( tables != null ) {
                    tlist.addAll( Arrays.asList( tables ) );
                }
            }
            TableMeta[] tables = tlist.toArray( new TableMeta[ 0 ] );
            int nt = tables.length;
            if ( nt == 1 ) {
                return tables[ 0 ];
            }
            else {
                throw new IOException( ( nt == 0 ? "No table element"
                                                 : "Multiple table elements" )
                                     + " at " + url_ + subPath );
            }
        }
        else {
            throw new IOException( "Table " + tname + " in unknown schema" );
        }
    }

    /**
     * Defines what parts of the tableset tree are acquired at what
     * types of read query on this object.
     */
    public enum Config {

        /** Schema query doesn't get tables, table query doesn't get columns. */
        POPULATE_NONE( false, "schema", "table" ),

        /** Schema query gets tables but not columns. */
        POPULATE_SCHEMAS( false, "table", "table" ),

        /** Schema query gets fully populated schemas and tables. */
        POPULATE_SCHEMAS_AND_TABLES( true, "", "" );

        private boolean populateTables_;
        private final String schemaDetail_;
        private final String tableDetail_;

        /**
         * Constructor.
         *
         * @param  populateTables  whether tables are populated with columns
         *                         during a schema query
         * @param  schemaDetail   detail parameter for readSchemas query
         * @param  tableDetail    detail parametr for readTables query
         */
        Config( boolean populateTables, String schemaDetail,
                String tableDetail ) {
            populateTables_ = populateTables;
            schemaDetail_ = schemaDetail;
            tableDetail_ = tableDetail;
        }
    }
}
