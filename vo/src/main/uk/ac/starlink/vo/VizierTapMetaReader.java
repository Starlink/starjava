package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;

/**
 * TapMetaReader implementation that works with VizieR's non-standard
 * tableset endpoint.  The /tables endpoint gives you table-less schemas,
 * and you can get the table documents by appending the table name to
 * the tables URL.  Details of what's quoted when seem to be in flux
 * at time of writing, so the implementation may need adjustment.
 *
 * @author   Mark Taylor
 * @since    14 May 2015
 */
public class VizierTapMetaReader implements TapMetaReader {

    private final URL url_;
    private final MetaNameFixer fixer_;
    private final ContentCoding coding_;
    private final AdqlSyntax syntax_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param    tablesetUrl  URL of TAPVizieR service followed by /tables
     * @param    fixer  object that fixes up syntactically incorrect
     *                  table/column names; if null no fixing is done;
     *                  has no effect for compliant VODataService documents
     * @param    coding  configures HTTP content-coding
     */
    public VizierTapMetaReader( URL tablesetUrl, MetaNameFixer fixer,
                                ContentCoding coding ) {
        url_ = tablesetUrl;
        fixer_ = fixer == null ? MetaNameFixer.NONE : fixer;
        coding_ = coding;
        syntax_ = AdqlSyntax.getInstance();
    }

    public SchemaMeta[] readSchemas() throws IOException {
        SchemaMeta[] schemas = readSchemas( url_ );
        for ( SchemaMeta schema : schemas ) {
            for ( TableMeta table : schema.getTables() ) {
                ColumnMeta[] cols = table.getColumns();
                if ( cols != null && cols.length == 0 ) {
                    table.setColumns( null );
                }
            }
        }
        fixer_.fixSchemas( schemas );
        TapMetaPolicy.sortSchemas( schemas );
        for ( SchemaMeta schema : schemas ) {
            TapMetaPolicy.sortTables( schema.getTables() );
        }
        return schemas;
    }

    public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
        ColumnMeta[] columns = readSingleTable( table ).getColumns();
        fixer_.fixColumns( columns );
        return columns;
    }

    public ForeignMeta[] readForeignKeys( TableMeta table ) throws IOException {
        ForeignMeta[] fkeys = readSingleTable( table ).getForeignKeys();
        return fkeys;
    }

    /** @throws UnsupportedOperationException  always */
    public TableMeta[] readTables( SchemaMeta schema ) {
        throw new UnsupportedOperationException( "Schemas contain tables; "
                                               + "shouldn't need this method" );
    }

    /**
     * Reads a single populated TableMeta object given its unpopulated form.
     *
     * @param  unpopulated table object
     * @param  table containing columns and foreign keys as available
     *         from service
     */
    private TableMeta readSingleTable( TableMeta table ) throws IOException {
        String[] cstNames = syntax_.getCatalogSchemaTable( table.getName() );
        String tName = cstNames != null
                     ? cstNames[ 2 ]
                     : fixer_.getOriginalTableName( table );
        URL turl = URLUtils.newURL( url_ + "/" + syntax_.unquote( tName ) );
        SchemaMeta[] schemas = readSchemas( turl );
        if ( schemas.length == 1 ) {
            TableMeta[] tables = schemas[ 0 ].getTables();
            if ( tables != null && tables.length == 1 ) {
                return tables[ 0 ];
            }
        }
        throw new IOException( "Table metadata not found at " + turl );
    }

    /**
     * Returns a tableset document from a given URL to give an array of
     * schemas.
     *
     * @param  url  location of tableset document
     * @return   array of schemas
     */
    private SchemaMeta[] readSchemas( URL url ) throws IOException {
        logger_.info( "Reading table metadata from " + url );
        final SchemaMeta[] schemas;
        try {
            schemas = TableSetSaxHandler.readTableSet( url, coding_ );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "Invalid TableSet XML document" )
                 .initCause( e );
        }
        return schemas;
    }

    public String getSource() {
        return url_.toString();
    }

    public String getMeans() {
        return "VizieR-variant 2-stage VOSI TableSet";
    }
}
