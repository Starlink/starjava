package uk.ac.starlink.votable;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Content handler which translates SAX events into table events.
 * It holds a {@link TableHandler} object which, if non-null,
 * is messaged for every suitable DATA element in the
 * input stream.  Inline tables (TABLEDATA ones and FITS/BINARY/BINARY2 ones
 * in which STREAM element contains the byte data as text children)
 * are always messaged to the handler.  Externally-referenced ones
 * (FITS/BINARY/BINARY2 ones in which the data is located from an
 * <code>href</code> attribute on the STREAM element) may either be ignored
 * or streamed to the table handler according to the setting of
 * the <code>setReadHrefTables</code> member.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Apr 2005
 */
public class TableContentHandler implements ContentHandler {

    private final VOTableParser votParser_;

    /**
     * Constructor.
     *
     * @param  strict  whether to effect a strict reading of the
     *                 VOTable standard
     */
    public TableContentHandler( boolean strict ) {
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
    }

    public void startDocument() throws SAXException {
        votParser_.startDocument();
    }

    public void endDocument() throws SAXException {
        votParser_.endDocument();
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        votParser_.startElement( namespaceURI, localName, qName, atts );
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        votParser_.endElement( namespaceURI, localName, qName );
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        votParser_.characters( ch, start, length );
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        votParser_.ignorableWhitespace( ch, start, length );
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        votParser_.startPrefixMapping( prefix, uri );
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        votParser_.endPrefixMapping( prefix );
    }

    public void skippedEntity( String name ) throws SAXException {
        votParser_.skippedEntity( name );
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        votParser_.processingInstruction( target, data );
    }
}
