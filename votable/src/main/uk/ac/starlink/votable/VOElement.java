package uk.ac.starlink.votable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.StarEntityResolver;

/**
 * Generic element in a VOTable document.  This class is extended to
 * provide the classes associated with specific VOTable elements.
 * It provides handling for some of the characteristics common to
 * various VOTable elements, for instance DESCRIPTION elements.
 *
 * <p>This element and its subclasses provide some methods for returning
 * the values of specific attributes from the element in question, such as
 * <tt>getName</tt>, <tt>getID</tt>.  This is either for convenience
 * or in order to return some non-String object from these attributes.
 * For attribute values which are not covered by specific methods in
 * this way, the {@link #getAttribute} method can be used.
 *
 * <p>The implementation of VOElement and its subclasses is currently
 * in terms of the {@link org.w3c.dom} classes, but this may possibly change
 * in the future.  The {@link #getChildren} and {@link #getAttribute} methods
 * allow implementation-neutral navigation through the VOTable tree.
 * For more fine control of the underlying document you can maintain
 * your own XML document representation and create VOElement objects
 * from selected nodes of it using the {@link #makeVOElement} method.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOElement {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    private final Element el;
    private final String systemId;
    private URL context;
    private String id;
    private String name;
    private String description;

    /**
     * Constructs a VOElement from a DOMSource.
     *
     * @param  xsrc the DOM source; its node must be a Document or Element
     */
    protected VOElement( DOMSource xsrc ) {

        /* Get the element associated with this source. */
        Node node = xsrc.getNode();
        if ( node instanceof Element ) {
            el = (Element) node;
        }
        else if ( node instanceof Document ) {
            el = ((Document) node).getDocumentElement();
        }
        else {
            throw new IllegalArgumentException( 
                "DOM node " + node + " is not a Document or an Element" );
        }

        /* Get the System ID associated with this source and work out a
         * URL version of it. */
        systemId = xsrc.getSystemId();
        if ( systemId != null ) {
            try {
                context = new URL( new File( "." ).toURI().toURL(), systemId );
            }
            catch ( MalformedURLException e ) {
                context = null;
            }
            catch ( SecurityException e ) {
                context = null;
            }
        }

        /* Store items which are generic to most/all VOTable elements. */
        if ( el.hasAttribute( "ID" ) ) {
            id = el.getAttribute( "ID" );
        }
        if ( el.hasAttribute( "name" ) ) {
            name = el.getAttribute( "name" );
        }
        Element descEl = DOMUtils.getChildElementByName( el, "DESCRIPTION" );
        if ( descEl != null ) {
            description = DOMUtils.getTextContent( descEl );
        }
    }

    /**
     * Constructs a VOElement from an XML source.
     * <p>
     * Obviously, it's no good submitting a spent source here 
     * (e.g. a SAXSource or StreamSource which have already been read).
     * A used DOMSource is OK of course.
     *
     * @param  xsrc  source
     * @throws  TransformerException  if <tt>xsrc</tt> is not a DOMSource
     *          and there is an error transforming it into a DOM
     */
    protected VOElement( Source xsrc ) throws TransformerException {
        this( transformToDOM( xsrc ) );
    }

    /**
     * Constructs a VOElement from an XML source which is required to
     * contain an Element with a given tag name.
     *
     * @param  the XML source
     * @param  tagname  the name that the element contained in <tt>xsrc</tt>
     *         is required to have
     * @throws  TransformerException  if <tt>xsrc</tt> is not a DOMSource
     *          and there is an error transforming it into a DOM
     * @throws  IllegalArgumentException  if the element in <tt>xsrc</tt>
     *          has a name other than <tt>tagname</tt>
     */
    protected VOElement( Source xsrc, String tagname )
            throws TransformerException {
        this( xsrc );
        if ( ! getTagName().equals( tagname ) ) {
            throw new IllegalArgumentException(
                "Unsuitable source: " + getTagName() + " != " + tagname );
        }
    }

    /**
     * Constructs a VOElement from a DOM source which is required to 
     * contain an Element with a given tag name.
     *
     * @param  the XML source
     * @param  tagname  the name that the element contained in <tt>xsrc</tt>
     *         is required to have
     * @throws  IllegalArgumentException  if the element in <tt>xsrc</tt>
     *          has a name other than <tt>tagname</tt>
     */
    protected VOElement( DOMSource dsrc, String tagname ) {
        this( dsrc );
        if ( ! getTagName().equals( tagname ) ) {
            throw new IllegalArgumentException(
                "Unsuitable source: " + getTagName() + " != " + tagname );
        }
    }

    /**
     * Returns the text of a DESCRIPTION element associated with this object,
     * or <tt>null</tt> if none exists.
     *
     * @return  the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the <tt>ID</tt> attribute value for this element,
     * or <tt>null</tt> if none exists.
     *
     * @return  the ID
     */
    public String getID() {
        return id;
    }

    /**
     * Returns the <tt>name</tt> attribute value for this element,
     * or <tt>null</tt> if none exists.
     *
     * @return  the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of a named attribute for this element,
     * or <tt>null</tt> if there is no such attribute.
     *
     * @param   attname  the name of the attribute to retrieve
     * @return  the value of attribute <tt>attname</tt>
     */
    public String getAttribute( String attname ) {
        return el.hasAttribute( attname ) ? el.getAttribute( attname ) : null;
    }

    /**
     * Returns the value of a named attribute for this element,
     * or a default ('#IMPLIED') value if the attribute is not present.
     *
     * @param  attname  the name of the attribute to retrieve
     * @param  implied  the default value
     * @return the value of the attribute <tt>attname</tt> if present,
     *         or <tt>implied</tt> if not
     */
    public String getAttribute( String attname, String implied ) {
        return hasAttribute( attname ) ? getAttribute( attname ) : implied;
    }

    /**
     * Indicates whether this element has a value for a given attribute.
     *
     * @param   attname the name of an attribute
     * @return  true iff this element has an attribute called <tt>attname</tt>
     */
    public boolean hasAttribute( String attname ) {
        return el.hasAttribute( attname );
    }

    /**
     * Returns the parent element of this element as a VOElement.
     * Note that the returned object is not guaranteed to be one of
     * the elements in the VOTable DTD.  If this element is at the
     * root of the document, <tt>null</tt> will be returned.
     */
    public VOElement getParent() {
        Node pnode = el.getParentNode();
        if ( pnode != null && pnode instanceof Element ) {
            DOMSource psrc = new DOMSource( (Element) pnode, systemId );
            return VOElement.makeVOElement( psrc );
        }
        else {
            return null;
        }
    }

    /**
     * Returns the child elements of this element.  Each is returned as
     * a VOElement or the appropriate specific VOElement subclass.
     *
     * @return  an array of VOElement children of this one
     */
    public VOElement[] getChildren() {
        List children = new ArrayList();
        for ( Node ch = el.getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element ) {
                DOMSource chsrc = new DOMSource( ch, systemId );
                children.add( VOElement.makeVOElement( chsrc ) );
            }
        }
        return (VOElement[]) children.toArray( new VOElement[ 0 ] );
    }

    /**
     * Returns all the child elements of this element which have a given
     * name.  Each is returned as a VOElement or the appropriate
     * specific VOElement subclass.
     *
     * @param  tagname  the element name required
     * @return an array of VOElement children of this one, all with element
     *         name <tt>tagname</tt>
     */
    public VOElement[] getChildrenByName( String tagname ) {
        List children = new ArrayList();
        for ( Node ch = el.getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 ((Element) ch).getTagName().equals( tagname ) ) {
                DOMSource chsrc = new DOMSource( ch, systemId );
                children.add( VOElement.makeVOElement( chsrc ) );
            }
        }
        return (VOElement[]) children.toArray( new VOElement[ 0 ] );
    }

    /**
     * Returns the first child element of this element which has a given
     * name.  If there are more than one with the given name, later ones
     * are ignored.  If there are none, <tt>null</tt> is returned.
     * The element is returned as a VOElement or the appropriate specific
     * VOElement subclass.
     *
     * @param  tagname  the element name required
     * @return  the first child of this one with element name <tt>tagname</tt>
     */
    public VOElement getChildByName( String tagname ) {
        for ( Node ch = el.getFirstChild(); ch != null;
              ch = ch.getNextSibling() ) {
            if ( ch instanceof Element &&
                 ((Element) ch).getTagName().equals( tagname ) ) {
                DOMSource chsrc = new DOMSource( ch, systemId );
                return VOElement.makeVOElement( chsrc );
            }
        }
        return null;
    }

    /**
     * Returns the tagname of the Element on which this object is based,
     * that is the element's name, like "RESOURCE" or "FIELD".
     *
     * @return  the element's name (note, not the value of the <tt>name</tt>
     *          attribute
     */
    public String getTagName() {
        return el.getTagName();
    }

    /**
     * Returns the DOM element on which this VOElement object is based.
     *
     * @return  the DOM element
     */
    Element getElement() {
        return el;
    }

    /**
     * Returns an XML Source corresponding to this element.
     *
     * @return  a source containing the XML content of this element
     */
    public Source getSource() {
        return new DOMSource( el, systemId );
    }

    /**
     * Returns the system identifier against which relative URIs should
     * be resolved.
     *
     * @return   the system ID, or <tt>null</tt>
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the base URL associated with this VOElement.  This will
     * normally be the same for the entire VOTable, namely the
     * URL of the VOTable document itself, and it will typically be
     * got from the System ID supplied at VOTable creation time.
     * Or, it may not be known at all, in which case <tt>null</tt> is returned.
     *
     * @return  the base URL associated with this element
     */
    public URL getContext() {
        return context;
    }

    /**
     * Returns the same value as {@link #getHandle}.
     *
     * @return  a string representation of this object
     */
    public String toString() {
        return getHandle();
    }

    /**
     * Returns something that can be used informally as a name for this
     * element.  May be ID or something other than the value of the
     * name attribute itself if no name exists.
     *
     * @return  a label for this element
     */
    public String getHandle() {
        String handle = "";
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "name" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "ID" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "ucd" );
        }
        if ( handle.length() == 0 && description != null ) {
            handle = description;
        }
        if ( handle.length() == 0 ) {
            handle = el.getTagName();
        }
        handle = handle.replaceFirst( "\n.*", "" );
        handle = handle.trim();
        return handle;
    }

    /**
     * Returns the text contained in this element.
     * Any text elements are concatenated and other elements such as
     * element structure is ignored.  This method is only designed to
     * be used on elements which have a CDATA content model, but it
     * will return a usable result for others.
     *
     * @return  the text content
     */
    public String getTextContent() {
        return DOMUtils.getTextContent( el );
    }

    /**
     * Constructs a VOElement object of the most specific type possible
     * from an XML Source.
     *
     * @param   xsrc  the XML source representing the element
     * @return  a VOElement of the most specific kind available
     * @throws  TransformerException  if <tt>xsrc</tt> is not a DOM source
     *          and there is an error transforming it to a DOM
     */
    public static VOElement makeVOElement( Source xsrc )
            throws TransformerException {
        return makeVOElement( transformToDOM( xsrc ) );
    }

    /**
     * Constructs a VOElement object of the most specific type possible
     * from a DOM Source.
     *
     * @param   xsrc  the XML source representing the element
     * @return  a VOElement of the most specific kind available
     */
    public static VOElement makeVOElement( DOMSource dsrc ) {

        /* Get the tag name. */
        Node node = dsrc.getNode();
        String name;
        if ( node instanceof Element ) {
            name = ((Element) node).getTagName();
        }
        else if ( node instanceof Document ) {
            name = ((Document) node).getDocumentElement().getTagName();
        }
        else {
            throw new IllegalArgumentException( "Unsuitable source" );
        }

        /* And build an appropriate element from the (possibly transformed)
         * source. */
        if ( name.equals( "VOTABLE" ) ) {
            return new VOTable( dsrc );
        }
        else if ( name.equals( "FIELD" ) ) {
            return new Field( dsrc );
        }
        else if ( name.equals( "PARAM" ) ) {
            return new Param( dsrc );
        }
        else if ( name.equals( "LINK" ) ) {
            return new Link( dsrc );
        }
        else if ( name.equals( "VALUES" ) ) {
            return new Values( dsrc );
        }
        else if ( name.equals( "TABLE" ) ) {
            return new Table( dsrc );
        }
        else {
            return new VOElement( dsrc );
        }
    }

    /**
     * Gets a DOMSource from a generic XML Source.  If the source is already
     * a DOMSource, there's nothing to do.  If it represents a stream
     * however, it parses it to produce a DOM, and wraps that up as a Source. 
     * The clever bit is that it intercepts SAX events indicating the
     * start and end of any DATA elements it finds so that they are
     * not incorporated as part of the DOM.  Such elements it parses
     * directly on the basis of what it knows about items that crop up
     * in VOTables.  This keeps the resulting DOM to a reasonable size.
     *
     * @param   xsrc  input XML source
     * @return  a DOMSource representing the XML document held by <tt>xsrc</tt>
     */
    private static DOMSource rawTransformToDOM( Source xsrc ) 
            throws TransformerException, SAXException, IOException {

        /* If it's a DOM source already, no problem. */
        if ( xsrc instanceof DOMSource ) {
            return (DOMSource) xsrc;
        }

        /* Otherwise we're going to need to do a custom parse of it. */
        String systemId = xsrc.getSystemId();
        InputSource insource;
        XMLReader parser = null;

        /* If it's a SAX source, mine it for its input source and parsing
         * engine. */
        if ( xsrc instanceof SAXSource ) {
            SAXSource saxsrc = (SAXSource) xsrc;
            insource = saxsrc.getInputSource();
            insource.setSystemId( systemId );
            parser = saxsrc.getXMLReader();
        }

        /* If it's a StreamSource, turn it into an input source and create
         * a default parsing engine. */
        else if ( xsrc instanceof StreamSource ) {
            StreamSource strmsrc = (StreamSource) xsrc;
            if ( strmsrc.getInputStream() != null ) {
                insource = new InputSource( strmsrc.getInputStream() );
                insource.setSystemId( systemId );
            }
            else if ( strmsrc.getReader() != null ) {
                insource = new InputSource( strmsrc.getReader() );
                insource.setSystemId( systemId );
            }
            else {
                insource = new InputSource( strmsrc.getSystemId() );
            }
        }

        /* I don't know of any other kinds of source, but if there is
         * one we'll have to transform it to DOM using brute force. */
        else {
            Node node = new SourceReader().getDOM( xsrc );
            return new DOMSource( node, systemId );
        }

        /* If we don't already have a parser, create one.  We get a 
         * non-validating one - no particular reason why, perhaps we
         * should permit it configurable?  But in most cases it won't
         * make any difference; the VOTable constructors allow this 
         * flexibility, which in most cases is where you'll start. */
        if ( parser == null ) {
            parser = makeParser( false );
        }

        /* Operate on the input source with the parser. */
        Document node = parseToDOM( parser, insource );

        /* Return a DOM source based on this with the same System ID as the
         * original source. */
        return new DOMSource( node, systemId );
    }

    /**
     * Gets a DOMSource from a generic XML Source.  If the source is already
     * a DOMSource, there's nothing to do.  If it represents a stream
     * however, it parses it to produce a DOM, and wraps that up as a Source. 
     * The clever bit is that it intercepts SAX events indicating the
     * start and end of any DATA elements it finds so that they are
     * not incorporated as part of the DOM.  Such elements it parses
     * directly on the basis of what it knows about items that crop up
     * in VOTables.  This keeps the resulting DOM to a reasonable size.
     *
     * @param   xsrc  input XML source
     * @return  a DOMSource representing the XML document held by <tt>xsrc</tt>
     * @throws  TransformerException  if <tt>xsrc</tt> is not a DOMSource
     *          and there is some error transforming it
     */
    public static DOMSource transformToDOM( Source xsrc ) 
            throws TransformerException {
        try {
            return rawTransformToDOM( xsrc );
        }
        catch ( SAXException e ) {
            throw new TransformerException( 
                "Error transforming source " + xsrc + " to DOM", e );
        }
        catch ( IOException e ) {
            throw new TransformerException( 
                "Error transforming source " + xsrc + " to DOM", e );
        }
    }

    /**
     * Constructs a new default SAX parser suitable for reading VOTables.  
     * You can choose whether you'd like a validating one.
     *
     * @param   validating  whether the returned parser ought to be validating
     * @return  new SAX parser
     */
    static XMLReader makeParser( final boolean validating )
            throws TransformerException {
        try {

            /* Get a SAX parser. */
            SAXParserFactory spfact = SAXParserFactory.newInstance();
            spfact.setValidating( validating );
            SAXParser sparser;
            try {
                sparser = spfact.newSAXParser();
            }
            catch ( ParserConfigurationException e ) {
                logger.config( "Parser configuration failed first time: " + e );
    
                /* Failed for some reason - try it with nothing fancy then. */
                try {
                    sparser = SAXParserFactory.newInstance().newSAXParser();
                }
                catch ( ParserConfigurationException e2 ) {
                    throw new TransformerException( e2 );  // shouldn't happen?
                }
            }
            XMLReader parser = sparser.getXMLReader();

            /* Install a custom entity resolver. */
            parser.setEntityResolver(
                       new StarEntityResolver( parser.getEntityResolver() ) );

            /* Configure the error handler according to whether we are 
             * validating or not. */
            parser.setErrorHandler( new ErrorHandler() {
                public void error( SAXParseException e ) throws SAXException {
                    if ( validating ) {
                        throw e;
                    }
                }
                public void fatalError( SAXParseException e )
                         throws SAXException {
                    throw e;
                }
                public void warning( SAXParseException e )
                         throws SAXException {
                    // no action
                }
            } );

            /* Return the parser. */
            return parser;
        }
        catch ( SAXException e ) {
            throw (TransformerException) 
                  new TransformerException( e.getMessage() )
                 .initCause( e );
        }
    }

    /**
     * Does a custom parse of an XML input source based on a given parser.
     * The parser's content handler is replaced with one which will
     * build a DOM, the parsing is initiated, and the resulting DOM 
     * is returned.  A custom content handler is used which does a 
     * selective parse, declining to install the bulk data (contents
     * of VOTable DATA elements) into the DOM itself.  The data 
     * contained therein is instead parsed directly and stashed away
     * using attributes of the constructed DOM for later use.
     *
     * @param  parser  base parser - used to define entity resolution,
     *         error handling etc
     * @param  insource  input source containing the stream of XML 
     */
    private static Document parseToDOM( XMLReader parser, InputSource insource )
            throws IOException, SAXException {

        /* Parse using a custom handler. */
        VOTableDOMBuilder db = new VOTableDOMBuilder();
        parser.setContentHandler( db );
        parser.parse( insource );

        /* Return the built document. */
        return db.getDocument();
    }
}
