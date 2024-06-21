package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.DataSource;

/**
 * Implementation of the <code>TableBuilder</code> interface which 
 * gets <code>StarTable</code>s from VOTable documents.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableBuilder extends DocumentedTableBuilder
                            implements MultiTableBuilder {

    private boolean strict_;
    private static Pattern htmlPattern = 
        Pattern.compile( "<x?html", Pattern.CASE_INSENSITIVE );

    /**
     * Default constructor.
     * Strictness of VOTable standard enforcement is determined by 
     * {@link VOElementFactory#isStrictByDefault}.
     */
    public VOTableBuilder() {
        this( VOElementFactory.isStrictByDefault() );
    }

    /**
     * Constructs a builder with explicit setting of whether VOTable
     * standard interpreation is strict or not. 
     *
     * @param  strict  true iff you want strict enforcement of VOTable standard
     * @see    VOElementFactory#setStrict
     */
    public VOTableBuilder( boolean strict ) {
        super( new String[] { "vot", "votable", "xml" } );
        strict_ = strict;
    }

    /**
     * Returns the string "VOTable".
     * 
     * @return  format name
     */
    public String getFormatName() {
        return "VOTable";
    }

    /**
     * Makes a StarTable out of a DataSource which points to a VOTable.
     * If the source has a position attribute, it is currently 
     * interpreted as an index into a breadth-first list of the TABLE
     * elements in the document pointed to by <code>datsrc</code>,
     * thus it must be a non-negative integer less than the number of
     * TABLE elements.  If it has no position attribute, the first
     * TABLE element is used.  The interpretation of the position
     * should probably change or be extended in the future to 
     * allow XPath expressions.
     *
     * @param  datsrc  the location of the VOTable document to use
     * @param  wantRandom  whether, preferentially, a random access table
     *         should be returned (doesn't guarantee that it will be random)
     * @param  storagePolicy  a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return  a StarTable made out of <code>datsrc</code>,
     *          or <code>null</code> if this handler can't handle it
     */
    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws TableFormatException, IOException {
        checkForHtml( datsrc );

        /* If the datasource has a position, try to use this to identify
         * the actual table we're after in the document. 
         * For now, this is just an integer indicating which table in 
         * breadth-order we want.  In due course it ought to be an XPath
         * probably. */
        int itab = 0;
        String pos = datsrc.getPosition();
        if ( pos != null ) {
            try {
                itab = Integer.parseInt( pos );
            }
            catch ( NumberFormatException e ) {
                throw new TableFormatException( 
                    "Expecting integer for position in " + datsrc +
                    " (got \"" + pos + "\")", e );
            }
            if ( itab < 0 ) {
                throw new TableFormatException(
                    "Expecting integer >= 0 for position in" + datsrc +
                    " (got " + itab + ")" );
            }
        }

        /* Stream the result to a new table store. */
        InputSource saxSrc = new InputSource( datsrc.getInputStream() );
        saxSrc.setSystemId( datsrc.getSystemId() );
        try {
            return SingleTableReader
                  .readStarTable( saxSrc, itab, storagePolicy, strict_ );
        }
        catch ( SAXException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
    }

    public TableSequence makeStarTables( DataSource datsrc,
                                         StoragePolicy storagePolicy )
            throws TableFormatException, IOException {
        checkForHtml( datsrc );
        String frag = datsrc.getPosition();
        if ( frag != null && frag.trim().length() > 0 ) {
            return Tables
                  .singleTableSequence( makeStarTable( datsrc, false,
                                                       storagePolicy ) );
        }
        final InputSource saxSrc = new InputSource( datsrc.getInputStream() );
        saxSrc.setSystemId( datsrc.getSystemId() );
        try {
            return MultiTableStreamer
                  .streamStarTables( saxSrc, storagePolicy, strict_ );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Returns <code>true</code> for flavors which have MIME types starting
     * <ul>
     * <li>text/xml
     * <li>application/xml
     * <li>application/x-votable+xml
     * </ul>
     */
    public boolean canImport( DataFlavor flavor ) {
        String pri = flavor.getPrimaryType();
        String sub = flavor.getSubType();
        if ( pri.equals( "text" ) && sub.equals( "xml" ) ||
             pri.equals( "application" ) && sub.equals( "xml" ) ||
             pri.equals( "application" ) && sub.equals( "x-votable+xml" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Acquires the data from a single TABLE element in a VOTable document,
     * writing the result to a sink.  This can be used if only one-shot
     * access to the data is required.  
     * Invocation of this method should be cheap on memory even
     * for large XML documents and/or tables.
     * Invocation is synchronous, so the method only returns when the
     * streaming has been done (successfully or otherwise).
     *
     * <p>Note that only table metadata that precedes the TABLE element
     * in the XML stream can be picked up when using this method.
     * If there are any XML elements following the end of the TABLE
     * whose content would normally show up in table metadata,
     * it will be ignored when using this method.
     *
     * <p>For more flexible streamed access to VOTable data, use a 
     * {@link TableContentHandler}.
     *
     * @param  istrm  stream from which the VOTable document will be supplied
     * @param  sink   callback interface into which the table metadata and
     *                data will be dumped
     * @param  index  if present, a string representation of the index of
     *                the table in the document to be read - "0" means the
     *                first one encountered, "1" means the second, etc.
     *                If it's <code>null</code> or not of numeric form the 
     *                first table will be used
     */
    public void streamStarTable( InputStream istrm, TableSink sink, 
                                 String index ) throws IOException {
        int itable = index != null && index.matches( "[0-9]+" ) 
                   ? Integer.parseInt( index ) 
                   : 0;
        try {
            TableStreamer.streamStarTable( new InputSource( istrm ),
                                           sink, itable, strict_ );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public boolean canStream() {
        return true;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "VOTableBuilder.xml" );
    }

    /**
     * Performs a simple check to see whether the data in a data source
     * appears to be HTML.  If it does, it is almost certainly not going
     * to represent a valid VOTable, and trying to process it may be slow.
     * In this case, throw a TableFormatException.
     *
     * @param  datsrc  data source to check
     */
    private static void checkForHtml( DataSource datsrc ) throws IOException {
        String sintro = new String( datsrc.getIntro() );
        if ( htmlPattern.matcher( new String( datsrc.getIntro() ) )
                        .lookingAt() ) {
            throw new TableFormatException( "Looks like HTML"
                                          + " - bet it's not a VOTable" );
        }
    }
}
