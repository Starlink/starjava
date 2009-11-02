package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from VOTable documents.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableBuilder implements TableBuilder, MultiTableBuilder {

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
        strict_ = strict;
    }

    /**
     * Returns the string "votable".
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
     * elements in the document pointed to by <tt>datsrc</tt>,
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
     * @return  a StarTable made out of <tt>datsrc</tt>, or <tt>null</tt>
     *          if this handler can't handle it
     */
    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws TableFormatException, IOException {

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
                    "Expecting integer for position in " +
                    datsrc + "(" +  e + ")" );
            }
            if ( itab < 0 ) {
                throw new TableFormatException(
                    "Expecting integer >= 0 for position in " + 
                    datsrc + "(got " + itab + ")" );
            }
        }

        /* Read the data. */
        TableElement[] tEls = readTableElements( datsrc, storagePolicy );

        /* Identify the requested TABLE element from the list that has been
         * read. */
        int ntab = tEls.length;
        if ( ntab == 0 ) {
            throw new TableFormatException( "Document contains "
                                          + "no TABLE elements" );
        }
        else if ( itab >= ntab ) {
            throw new IOException( "VOTable document contained only " + ntab
                                 + " tables; item " + itab + " requested" );
        }
        TableElement tEl = tEls[ itab ];

        /* Adapt the TABLE element to a StarTable. */
        StarTable st = new VOStarTable( tEl );

        /* Check it looks vaguely sensible. */
        if ( st.getColumnCount() == 0 ) {
            throw new TableFormatException( "TABLE contains no columns" );
        }

        /* Return the StarTable. */
        return st;
    }

    public StarTable[] makeStarTables( DataSource datsrc,
                                       StoragePolicy storagePolicy )
            throws TableFormatException, IOException {
        String frag = datsrc.getPosition();
        if ( frag != null && frag.trim().length() > 0 ) {
            return new StarTable[] { makeStarTable( datsrc, false,
                                                    storagePolicy ) };
        }
        TableElement[] tEls = readTableElements( datsrc, storagePolicy );
        List tableList = new ArrayList();
        for ( int i = 0; i < tEls.length; i++ ) {
            VOStarTable table = new VOStarTable( tEls[ i ] );
            if ( table.getColumnCount() > 0 ) {
                tableList.add( table );
            }
        }
        return (StarTable[]) tableList.toArray( new StarTable[ 0 ] );
    }

    /**
     * Reads a DataSource and returns an array of all the TableElements
     * in it, in breadth-first order.
     *
     * @param  datsrc  the location of the VOTable document to use
     * @param  storagePolicy  a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return  array of TableElements
     */
    private TableElement[] readTableElements( DataSource datsrc,
                                              StoragePolicy storagePolicy )
            throws TableFormatException, IOException {

        /* Check if the source looks like HTML.  If it does it is almost
         * certainly not going to represent a valid VOTable, and trying
         * to process it will be slow, since the parser may take some
         * time to work out that it's not XML we are seeing.
         * In this case bail out. */
        String sintro = new String( datsrc.getIntro() );
        if ( htmlPattern.matcher( sintro ).lookingAt() ) {
            throw new TableFormatException( "Data looks like HTML" );
        }

        /* Try to get a VOTable object from this source. */
        VOElement voEl;
        try {
            voEl = new VOElementFactory( storagePolicy )
                  .makeVOElement( datsrc );
        }

        /* If we have got a parse exception it's probably because
         * it wasn't XML.  Rethrow as a TableFormatException 
         * to indicate it wasn't our kind of input. */
        catch ( SAXException e ) {
            throw new TableFormatException( "XML parse error ("
                                          + e.getMessage() + ")", e );
        }
        catch ( IOException e ) {
            throw e;
        }

        /* Identify the TABLE elements within the VOTable document. */
        NodeList tables = voEl.getElementsByVOTagName( "TABLE" );

        /* Return them as an array of TableElements. */
        int ntab = tables.getLength();
        TableElement[] els = new TableElement[ ntab ];
        for ( int itab = 0; itab < ntab; itab++ ) {
            els[ itab ] = (TableElement) tables.item( itab );
        }
        return els;
    }

    /**
     * Returns <tt>true</tt> for flavors which have MIME types starting
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
     * <p>For more flexible streamed access to VOTable data, use a 
     * {@link TableContentHandler}.
     *
     * @param  istrm  stream from which the VOTable document will be supplied
     * @param  sink   callback interface into which the table metadata and
     *                data will be dumped
     * @param  index  if present, a string representation of the index of
     *                the table in the document to be read - "0" means the
     *                first one encountered, "1" means the second, etc.
     *                If it's <tt>null</tt> or not of numeric form the 
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
}
