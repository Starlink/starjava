package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;

/**
 * TapMetaReader that works with the proposed VOSI-1.1 scalable /tables
 * endpoint.  This is currently as defined in VOSI-1.1-WD20160129.
 * The tables endpoint may be accessed with an optional query part
 * <code>?detail=min</code>; if it is, the column and foreign key
 * metadata is omitted, and if not, that metadata might be omitted.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2016
 */
public class Vosi11TapMetaReader implements TapMetaReader {

    private final URL url_;
    private final ContentCoding coding_;
    private final boolean preferPopulateTables_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   tablesetUrl   URL of VOSI1.1-like TAP tableset service
     * @param    coding   configures HTTP content-coding
     * @param   preferPopulateTables  if true, column/fkey metadata will
     *                                not be solicited in initial query
     *                                (<code>detail=min</code> will be used)
     */
    public Vosi11TapMetaReader( URL tablesetUrl, ContentCoding coding,
                                boolean preferPopulateTables ) {
        url_ = tablesetUrl;
        coding_ = coding;
        preferPopulateTables_ = preferPopulateTables;
    }

    public String getMeans() {
        return "VOSI-1.1-WD20160129"
             + ( preferPopulateTables_ ? ", full detail requested"
                                       : ", minimal detail requested" );
    }

    public String getSource() {
        return url_.toString();
    }

    public SchemaMeta[] readSchemas() throws IOException {

        /* Read the tableset document.  Either do or don't ask for restricted
         * metadata.  We treat the result in almost exactly the same way
         * in both cases. */
        SchemaMeta[] schemas =
            populateHandler( null, preferPopulateTables_ ? null : "min" )
           .getSchemas( true );

        /* In principle, it is possible to work out whether the request for
         * complete metadata has been rejected.  But it's fiddly (involves
         * grubbing through HTTP response codes).  So be lazy; just conclude
         * that if there are no columns for a given table, it's because
         * the service decided not to give detailed table metadata to us,
         * rather than because the table has zero columns.
         * This will nearly always be a sound conclusion
         * (since column-less tables are rare) but if it's not, the worst
         * that will happen is that we make an unnecessary extra query for
         * columns at some point in the future.
         * This means we have to take care to fill in column arrays as null
         * rather than empty arrays, to flag to downstream code to make
         * separate requests for this metadata if required. */
        TapMetaPolicy.sortSchemas( schemas );
        int nTable = 0;
        int nDetail = 0;
        for ( SchemaMeta schema : schemas ) {
            TableMeta[] tables = schema.getTables();
            if ( tables != null ) {
                TapMetaPolicy.sortTables( tables );
                for ( TableMeta table : tables ) {
                    nTable++;
                    ColumnMeta[] cols = table.getColumns();
                    ForeignMeta[] fkeys = table.getForeignKeys();
                    boolean hasDetail = ( cols != null && cols.length > 0 )
                                     || ( fkeys != null && fkeys.length > 0 );
                    if ( ! hasDetail ) {
                        table.setColumns( null );
                        table.setForeignKeys( null );
                    }
                    else {
                        nDetail++;
                    }
                }
            }
        }

        /* Log the service's (perfectly legal) refusal for full metadata
         * if it looks like that's what happened. */
        if ( preferPopulateTables_ && nDetail == 0 ) {
            logger_.info( "Requested column/fkey metadata was absent"
                        + " - will acquire per-table as required" );
        }
        return schemas;
    }

    /**
     * @throws  UnsupportedOperationException
     */
    public TableMeta[] readTables( SchemaMeta schema ) {
        String msg = "You shouldn't need to call this method"
                   + " (if you've got the schemas you've got the tables)";
        throw new UnsupportedOperationException( msg );
    }

    public ColumnMeta[] readColumns( TableMeta table ) throws IOException {
        return readSingleTable( table ).getColumns();
    }

    public ForeignMeta[] readForeignKeys( TableMeta table ) throws IOException {
        return readSingleTable( table ).getForeignKeys();
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
        String subPath = "/" + tname;
        TableSetSaxHandler tsHandler = populateHandler( subPath, "" );
        List<TableMeta> tlist = new ArrayList<TableMeta>();
        tlist.addAll( Arrays.asList( tsHandler.getNakedTables() ) );
        for ( SchemaMeta schema : tsHandler.getSchemas( false ) ) {
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
}
