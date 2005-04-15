package uk.ac.starlink.votable;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Filtering SAX parser which additionally sends table events.
 * This behaves in most respects like a normal XMLFilterImpl, 
 * passing all its events on to its parent parser, unless you 
 * set its various handlers to do something else.
 *
 * <p>However, in addition it holds a {@link TableHandler} object
 * which is messaged for every suitable DATA element in the 
 * input stream.  Inline tables (TABLEDATA ones and FITS or BINARY ones
 * in which STREAM element contains the byte data as text children)
 * are always messaged to the handler.  Externally-referenced ones
 * (FITS or BINARY ones in which the data is located from an 
 * <tt>href</tt> attribute on the STREAM element) may either be ignored
 * or streamed to the table handler according to the setting of 
 * the <tt>setReadHrefTables</tt> member.
 *
 * <p>The entity resolver is also set by default to one which knows
 * where to look for the VOTable DTD etc.
 *
 * @author   Mark Taylor (Starlink)
 * @since    14 Apr 2005
 */
public class TableXMLReader extends XMLFilterImpl {

    private final VOTableParser votParser_;

    /**
     * Constructor.
     *
     * @param  parent  parent parser 
     * @param  strict  whether to effect a strict reading of the 
     *                 VOTable standard
     */
    public TableXMLReader( XMLReader parent, boolean strict ) {
        super( parent );
        setEntityResolver( StarEntityResolver.getInstance() );
        votParser_ = new VOTableParser( strict );
    }

    /**
     * Sets the TableHandler object for this parser.  If set to a non-null
     * value, the handler will be messaged with the table metadata and data
     * for each table (each DATA element) encountered in parsing 
     * the VOTable document.
     *
     * @param  handler   table handler
     */
    public void setTableHandler( TableHandler handler ) {
        votParser_.setTableHandler( handler );
    }

    /**
     * Returns the TableHandler object for this parser.
     *
     * @return  table handler
     */
    public TableHandler getTableHandler() {
        return votParser_.getTableHandler();
    }

    /**
     * Sets whether href-referenced tables should be streamed to the table
     * handler.
     *
     * @param  readHrefs  if true, externally-referenced tables will be
     *         messaged to the handler, if false they will be ignored
     */
    public void setReadHrefTables( boolean readHrefs ) {
        votParser_.setReadHrefTables( readHrefs );
    }

    /**
     * Indicates whether href-referenced tables should be streamed to the
     * table handler.
     *
     * @return  true if externally-referenced tables will be messaged to
     *          the handler, false if they will be ignored
     */
    public boolean getReadHrefTables() {
        return votParser_.getReadHrefTables();
    }

    /**
     * Returns the document locator.
     *
     * @return  locator
     */
    public Locator getLocator() {
        return votParser_.getLocator();
    }

    public void setDocumentLocator( Locator locator ) {
        votParser_.setDocumentLocator( locator );
        super.setDocumentLocator( locator );
    }

    public void startDocument() throws SAXException {
        votParser_.startDocument();
        super.startDocument();
    }

    public void endDocument() throws SAXException {
        votParser_.endDocument();
        super.endDocument();
    }

    public void startElement( String namespaceURI, String localName, 
                              String qName, Attributes atts )
            throws SAXException {
        votParser_.startElement( namespaceURI, localName, qName, atts );
        super.startElement( namespaceURI, localName, qName, 
                                            atts );
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        votParser_.endElement( namespaceURI, localName, qName );
        super.endElement( namespaceURI, localName, qName );
    }
    
    public void characters( char[] ch, int start, int length )
            throws SAXException {
        votParser_.characters( ch, start, length );
        super.characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        votParser_.ignorableWhitespace( ch, start, length );
        super.ignorableWhitespace( ch, start, length );
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        votParser_.startPrefixMapping( prefix, uri );
        super.startPrefixMapping( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        votParser_.endPrefixMapping( prefix );
        super.endPrefixMapping( prefix );
    }

    public void skippedEntity( String name ) throws SAXException {
        votParser_.skippedEntity( name );
        super.skippedEntity( name );
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        votParser_.processingInstruction( target, data );
        super.processingInstruction( target, data );
    }
}
