package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;

/**
 * TapMetaReader that works with the VOSI-1.1 scalable /tables endpoint.
 * It should work correctly with VOSI 1.1 services and also with VOSI 1.0
 * services and services (like, at time of writing, TAPVizieR)
 * that declare themselves as VOSI 1.0 but refuse to emit table details
 * for the full tables list.
 *
 * <p>Services may return tables without column and foreign key metadata
 * (table elements have no column children).  If such a table is retrieved,
 * the detailed metadata may be obtained from a child URL /tables/(table-name).
 * The service may accept a <code>detail</code> parameter for the /tables URL,
 * with possible values <code>min</code> or <code>max</code>
 * (that is <code>/tables?detail=min</code> or <code>/tables?detail=max</code>)
 * to give it a non-binding hint about whether detail is returned
 * in child tables.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2016
 */
public class Vosi11TapMetaReader implements TapMetaReader {

    private final URL url_;
    private final MetaNameFixer fixer_;
    private final ContentCoding coding_;
    private final DetailMode detailMode_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   tablesetUrl   URL of VOSI1.1-like TAP tableset service
     * @param   fixer  object that fixes up syntactically incorrect
     *                 table/column names; if null no fixing is done;
     *                 has no effect for compliant VODataService documents
     * @param   coding   configures HTTP content-coding
     * @param   detailMode  detail mode
     */
    public Vosi11TapMetaReader( URL tablesetUrl, MetaNameFixer fixer,
                                ContentCoding coding, DetailMode detailMode ) {
        url_ = tablesetUrl;
        fixer_ = fixer == null ? MetaNameFixer.NONE : fixer;
        coding_ = coding;
        detailMode_ = detailMode;
    }

    public String getMeans() {
        String note = detailMode_.note_;
        return "VOSI-1.1" + ( note == null ? "" : ( ", " + note ) );
    }

    public String getSource() {
        return url_.toString();
    }

    public SchemaMeta[] readSchemas() throws IOException {

        /* Read the tableset document.  Either do or don't ask for restricted
         * metadata.  We treat the result in almost exactly the same way
         * in both cases. */
        SchemaMeta[] schemas = populateHandler( null, detailMode_ )
                              .getSchemas( true );
        if ( fixer_ != null ) {
            fixer_.fixSchemas( schemas );
        }

        /* If there are no columns for a given table, assume it's because
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
        int nCol = 0;
        int nKey = 0;
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
                    if ( cols != null ) {
                        nCol += cols.length;
                    }
                    if ( fkeys != null ) {
                        nKey += fkeys.length;
                    }
                }
            }
        }
        logger_.info( "Metadata loaded for "
                    + nTable + " tables, "
                    + nCol + " columns, "
                    + nKey + " foreign keys" );

        /* If the service apparently rejected our request for max or min
         * detail, log it.  The service is perfectly within its rights
         * to do that. */
        if ( detailMode_ == DetailMode.MAX && nDetail == 0 ) {
            logger_.info( "Table column/fkey metadata absent"
                        + " despite " + detailMode_.query_
                        + " - will acquire per-table as required" );
        }
        else if ( detailMode_ == DetailMode.MIN && nDetail > 0 ) { 
            logger_.info( "Table column/fkey metadata present"
                        + " despite " + detailMode_.query_
                        + " - no further metadata requests required" );
        }
        return schemas;
    }

    /**
     * @throws  UnsupportedOperationException  always
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
        String tname = fixer_.getOriginalTableName( table );
        String subPath = "/" + tname;
        TableSetSaxHandler tsHandler = populateHandler( subPath, null );
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
     * as modified by a given subpath and detail mode.
     *
     * @param  subPath  url subpath, normally including a leading '/',
     *                  or null for the base URL
     * @param  detailMode  detail mode, or null if not applicable
     * @return   handler that has performed a parse on the URL corresponding
     *           to the given arguments
     */
    private TableSetSaxHandler populateHandler( String subPath,
                                                DetailMode detailMode )
            throws IOException {
        String surl = url_.toString();

        /* The table name may need some doctoring.  In particular,
         * it must not contain any characters which are illegal in a URI,
         * for instance double quote (") which is ubiquitous in TAPVizieR
         * table names.  Characters which RFC3986 marks as "reserved"
         * are a moot point; VOSI is not written carefully enough to
         * make clear whether these should be encoded or literal,
         * for now keep them literal. */
        if ( subPath != null && subPath.length() > 0 ) {
            surl += URLUtils.percentEncodeIllegalCharacters( subPath );
        }
        String query = detailMode == null ? null : detailMode.query_;
        if ( query != null ) {
            surl += "?" + query;
        }
        URL url = URLUtils.newURL( surl );
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
     * Enumeration for detail-preference mode of table metadata queries.
     */
    public enum DetailMode {

        /**
         * Detail=min.  Hints that a full tableset query should return
         * only tables no columns or fkeys.
         */
        MIN( "detail=min", "minimal detail requested",
             "if supported, " +
             "column and foreign-key metadata is only read as required" ),

        /**
         * Detail=max.  Hints that a full tableset query should return
         * columns and fkeys along with tables.
         */
        MAX( "detail=max", "full detail requested",
             "if supported, all metadata is read at once" ),

        /**
         * No detail preference.  No hint to service whether column and fkey
         * metadata is supplied along with tables.
         */
        NULL( null, null,
              "service decides whether column and foreign key metadata " +
              "is read all at once or on demand" );

        private final String query_;
        private final String note_;
        private final String descrip_;

        /**
         * Constructor.
         *
         * @query   param=value part of URL query string
         * @param   note   short note of function
         * @param   descrip   longer description of function
         */
        private DetailMode( String query, String note, String descrip ) {
            query_ = query;
            note_ = note;
            descrip_ = descrip;
        }

        /**
         * Returns a description of the function described by this mode
         * (one sentence, not capitalised).
         *
         * @return  description
         */
        public String getDescription() {
            return descrip_;
        }
    }
}
