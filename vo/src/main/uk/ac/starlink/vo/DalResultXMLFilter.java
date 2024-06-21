package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.votable.Namespacing;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;

/**
 * SAX filter which ignores any tables in a VOTable document, except those
 * in a RESOURCE which has type="results".
 * This is suitable for getting the basic table from the result of an
 * SIA or SSA service.
 * Under some circumstances the results can come with a large amount
 * of non-result information (for instance massive numbers of small footprint
 * tables from http://www.stecf.org/hst-vo/hst_ssa?), which can have a 
 * very serious impact on performance when trying to build a DOM.
 * So if all you are interested in is the results table which the SIA/SSA
 * protocol says has to be there, using this filter can save a lot of
 * unnecessary processing.
 *
 * <p>In many cases, you can just use the {@link #getDalResultTable}
 * utility method.
 *
 * @see <a href="http://www.ivoa.net/Documents/latest/SSA.html"
 *         >Simple Spectral Access Protocol</a>
 * @see <a href="http://www.ivoa.net/Documents/SIA/"
 *         >Simple Image Access Protocol</a>
 */
public class DalResultXMLFilter extends XMLFilterImpl {

    private final Namespacing namespacing_;
    private final StringBuffer path_;
    private String resultsPath_;
    private String ignorePath_;

    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   parent  parent SAX reader
     * @param   namespacing  VOTable namespacing policy to employ
     */
    public DalResultXMLFilter( XMLReader parent, Namespacing namespacing ) {
        super( parent );
        namespacing_ = namespacing;
        path_ = new StringBuffer();
    }

    /**
     * Returns true if we are ignoring table data at this point in the
     * document.
     *
     * @return   true iff this is not part of the results
     */
    private boolean ignore() {
        return ignorePath_ != null;
    }

    public void startElement( String namespaceURI, String localName,
                              String qName, Attributes atts )
            throws SAXException {
        String voTagName =
            namespacing_.getVOTagName( namespaceURI, localName, qName );
        path_.append( '/' ).append( voTagName );

        if ( "RESOURCE".equals( voTagName ) &&
             "results".equals( atts.getValue( "type" ) ) ) {
            resultsPath_ = path_.toString();
        }

        if ( "TABLE".equals( voTagName ) && resultsPath_ == null ) {
            ignorePath_ = path_.toString();
        }

        if ( ! ignore() ) {
            super.startElement( namespaceURI, localName, qName, atts );
        }
    }

    public void endElement( String namespaceURI, String localName,
                            String qName ) throws SAXException {
        if ( ! ignore() ) {
            super.endElement( namespaceURI, localName, qName );
        }

        String path = path_.toString();
        if ( path.equals( resultsPath_ ) ) {
            resultsPath_ = null;
        }
        else if ( path.equals( ignorePath_ ) ) {
            ignorePath_ = null;
        }

        String voTagName =
            namespacing_.getVOTagName( namespaceURI, localName, qName );
        assert path_.toString().endsWith( "/" + voTagName );
        path_.setLength( path_.length() - voTagName.length() - 1 );
    }

    public void startPrefixMapping( String prefix, String uri )
            throws SAXException {
        if ( ! ignore() ) {
            super.startPrefixMapping( prefix, uri );
        }
    }

    public void endPrefixMapping( String prefix ) throws SAXException {
        if ( ! ignore() ) {
            super.endPrefixMapping( prefix );
        }
    }

    public void characters( char[] ch, int start, int length )
            throws SAXException {
        if ( ! ignore() ) {
            super.characters( ch, start, length );
        }
    }

    public void ignorableWhitespace( char[] ch, int start, int length )
            throws SAXException {
        if ( ! ignore() ) {
            super.ignorableWhitespace( ch, start, length );
        }
    }

    public void processingInstruction( String target, String data )
            throws SAXException {
        if ( ! ignore() ) {
            super.processingInstruction( target, data );
        }
    }

    public void skippedEntity( String name ) throws SAXException {
        if ( ! ignore() ) {
            super.skippedEntity( name );
        }
    }

    /**
     * Utility method which uses an instance of this class to turn a SAX
     * InputSource into a DOM.  The DOM will lack non-results tables.
     *
     * @param  vofact  factory which can generate VOTable DOMs
     * @param  inSrc   source of the SAX stream
     */
    public static VOElement parseDalResult( VOElementFactory vofact,
                                            InputSource inSrc )
            throws IOException {
        try {
            Namespacing namespacing = Namespacing.LAX;
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            namespacing.configureSAXParserFactory( spfact );
            XMLReader streamReader = spfact.newSAXParser().getXMLReader();
            XMLReader resultReader =
                new DalResultXMLFilter( streamReader, namespacing );
            Source xsrc = new SAXSource( resultReader, inSrc );
            return vofact.makeVOElement( xsrc );
        }
        catch ( SAXException e ) {
            throw (IOException)
                  new IOException( "VOTable document parse error" )
                 .initCause( e );
        }
        catch ( ParserConfigurationException e ) {
            throw (IOException)
                  new IOException( "XML configuration error" )
                 .initCause( e );
        }
    }

    /**
     * Utility method which can return the single results table from a
     * DAL-type response.  This is the single table within the RESOURCE
     * element marked with type="results", as described by the SIA and SSA
     * standards.
     * The QUERY_STATUS INFO element is checked; in case of ERROR, 
     * an exception is thrown.
     *
     * @param   vofact  factory which can generate VOTable DOMs
     * @param   inSrc   source of the SAX stream
     * @return  result table, never null
     * @throws  IOException  in case of error, including an ERROR-valued
     *          QUERY_STATUS in the response, or no suitable table found
     */
    public static StarTable getDalResultTable( VOElementFactory vofact,
                                               InputSource inSrc )
            throws IOException {

        /* Do the parse. */
        VOElement voEl = parseDalResult( vofact, inSrc );

        /* Locate the results resource. */
        VOElement resultEl =
            locateElement( voEl, "RESOURCE", "type", "results" );
        if ( resultEl == null ) {
            throw new IOException( "No <RESOURCE type='results'> "
                                 + "element found" );
        }

        /* Locate and process QUERY_STATUS info element. */
        VOElement statusEl =
            locateElement( resultEl, "INFO", "name", "QUERY_STATUS" );
        if ( statusEl == null ) {
            logger_.warning( "No <INFO name='QUERY_STATUS'> element found"
                           + " - try assuming OK" );
        }
        if ( statusEl != null ) {
            String statusValue = statusEl.getAttribute( "value" );
            String statusText = DOMUtils.getTextContent( statusEl );
            String msg =
                ( statusText == null || statusText.trim().length() == 0 )
                     ? ( "QUERY_STATUS is " + statusValue )
                     : statusText;
            if ( "OK".equals( statusValue ) ) {
                // no action
            }
            else if ( "ERROR".equals( statusValue ) ) {
                throw new IOException( msg );
            }
            else {
                logger_.warning( msg );
            }
        }

        /* Locate result TABLE element. */
        NodeList tableNodes = resultEl.getElementsByVOTagName( "TABLE" );
        int nTable = tableNodes.getLength();
        if ( nTable == 0 ) {
            throw new IOException( "No table found"
                                 + " in <RESOURCE type='results'>" );
        }
        if ( nTable > 1 ) {
            logger_.warning( "Found " + nTable + " tables"
                           + " in <RESOURCE type='results'>"
                           + " - just returning first" );
        }

        /* Return it as a StarTable. */
        return new VOStarTable( (TableElement) tableNodes.item( 0 ) );
    }

    /**
     * Returns an element distinguished by a tag name, and the value of
     * a given attribute.
     * The immediate children are searched first, followed by all descendants.
     * Null is returned if none is found.
     *
     * @param   parentEl  element within which element is sought
     * @param   voTagName  unqualified tag name of sought element
     * @param   attName  attribute name
     * @param   attValue attribute value
     * @return   element within parentEl with element name voTagName and
     *           attribute attName=attValue
     */
    private static VOElement locateElement( VOElement parentEl,
                                            String voTagName, String attName,
                                            String attValue ) {

        /* Search immediate children. */
        VOElement[] children = parentEl.getChildrenByName( voTagName );
        for ( int ii = 0; ii < children.length; ii++ ) {
            VOElement el = children[ ii ];
            if ( attValue.equals( el.getAttribute( attName ) ) ) {
                return el;
            }
        }

        /* Failing that, search all descendants. */
        NodeList nodes = parentEl.getElementsByVOTagName( voTagName );
        int nNode = nodes.getLength();
        for ( int in = 0; in < nNode; in++ ) {
            VOElement el = (VOElement) nodes.item( in );
            if ( attValue.equals( el.getAttribute( attName ) ) ) {
                return el;
            }
        }
        return null;
    }
}
