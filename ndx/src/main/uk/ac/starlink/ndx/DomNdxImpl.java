package uk.ac.starlink.ndx;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.xml.transform.Source;

import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.hdx.HdxResourceType;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrayFactory;
import uk.ac.starlink.array.AccessMode;

/** 
 * An NdxImpl implementation in which the hierarchical data is stored in 
 * a DOM.
 *
 * @author Norman Gray
 * @author Mark Taylor (Starlink)
 * @author Peter W. Draper
 * @version $Id$
 */
class DomNdxImpl implements NdxImpl {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ndx" );

    /**
     * A hash to handle the object contents of this object.  The keys
     * will be Strings.  Note that HashMap methods
     * aren't synchronized, so it mustn't be used in a multithreaded
     * environment.  
     */
    private Map contents = new HashMap();

    /** Holds the tree information. */
    private Element ndxElement;

    /**
     * Constructs a new object based on the information in the
     * Element.
     *
     * <p>For simplicity, we demand that the
     * incoming element be in no namespace, and have the
     * correct name.  We could make this object handle the required
     * searching itself, at the cost of being slightly more
     * complicated, but since this element is intended to be
     * constructed by a factory, which will do a variety of such
     * simple transformations and searches, it seems best to force
     * all the intelligence to be there.  For much the same reason,
     * we object by throwing an exception, if these requirements are
     * not satisfied.
     *
     * @param elem a DOM element representing an NDX.
     *
     * @throws HdxException if the input element's name is not of
     * the required type.
     */
    DomNdxImpl (Element elem)
            throws HdxException {
        if ( HdxResourceType.match( elem ) != BridgeNdx.getHdxResourceType() )
            throw new HdxException( "Element " + elem.getTagName()
                                    + " wrong type for DomNdxImpl constructor");
        ndxElement = elem;
    }

    public NDArray getImage() {
        return getChildArray( "image" );
    }

    public NDArray getVariance() {
        return getChildArray( "variance" );
    }

    public NDArray getQuality() {
        return getChildArray( "quality" );
    }

    public String getTitle() {
        return getChildText( HdxResourceType.TITLE.xmlName(),
                             HdxResourceType.TITLE.getHoistAttribute() );
    }

    public String getLabel() {
        return getChildText( "label", "value" );
    }

    public String getUnits() {
        return getChildText( "units", "value" );
    }

    public int getBadBits() {
        String bytestr = getChildText( "badbits", "value" );
        return ( bytestr == null ) ? (byte) 0
                                   : Byte.decode(bytestr).byteValue();
    }

    public Object getWCS() {
        /*
         * XXX "wcs" will turn into an HdxResourceType when Ast/WCS is
         * finalised (it had better!).  At that point, we should stop
         * hard-coding the element name in here.
         */
        return getChildElement( "wcs" );
    }

    public Source getEtc() {
        /*
         * XXX "etc" MIGHT turn into an HdxResourceType when `etc' is
         * finalised.  At that point, we should stop
         * hard-coding the element name in here.
         */
        Element etcElement = getChildElement( "etc" );
        /*
         * NdxImpl#getEtc asserts that this method _will_not_ be
         * called unless NdxImpl#hasEtc is true, so assert this, since
         * it's clearly a programming error if this isn't true.
         */
        assert etcElement != null;
        return new javax.xml.transform.dom.DOMSource( etcElement );
    }
    
    public boolean hasImage() {
        return hasChildArray( "data" );
    }

    public boolean hasVariance() {
        return hasChildArray( "variance" );
    }

    public boolean hasQuality() {
        return hasChildArray( "quality" );
    }

    public boolean hasTitle() {
        return getTitle() != null;
    }

    public boolean hasLabel() {
        return getLabel() != null;
    }

    public boolean hasUnits() {
        return getUnits() != null;
    }

    public boolean hasWCS() {
        return getChildElement( "wcs" ) != null;
    }

    public boolean hasEtc() {
        return getChildElement( "etc" ) != null;
    }

    public String toString() {
        return "DomNdxImpl<" + ndxElement.getTagName() + ">";
    }

    /** Save an NDObjectDomNode for later retrieval. */
    private void putChild ( String r, Object n ) {
        contents.put ( r, n );
        return;
    }

    /**
     * Returns a child element which corresponds to the given HDX type.
     * If there is more than one element of a certain
     * type in the tree (there shouldn't be), then this will return
     * only the first one, without signalling any error.
     *
     * @param res the name of the element to return
     * @return the required element, or null if none can be found.
     */
    private Element getChildElement ( String xmlname ) {
        if ( ndxElement == null )
            return null;

        for (Node kid = ndxElement.getFirstChild();
             kid != null;
             kid = kid.getNextSibling()) {
            if ( kid.getNodeType() == Node.ELEMENT_NODE
                && ((Element)kid).getTagName().equals( xmlname ) ) {
                return ( Element )kid;
            }
        }
        return null;
    }

    /**
     * Retrieve an DomNdxImpl.  Retrieves the object from the
     * hash, or from the DOM tree if it hasn't been asked for the
     * object before.  If there is more than one object of a certain
     * type in the tree (there shouldn't be), then it will return
     * only the first one, without signalling any error.
     *
     * @return the indexed object, or null if nothing was found.
     */
    private NDArray getChildArray ( String xmlname ) {
        if ( !contents.containsKey(xmlname) )
        {
            Element kid = getChildElement( xmlname );
            if ( kid == null ) {
                putChild( xmlname, null );
            } else {
                try {
                    URL url = getURLFromElement( kid );
                    if ( url == null ) {
                        logger.warning( "No URL in element: " + kid );
                        putChild( xmlname, null );
                    } else {
                        putChild (xmlname,
                                  new NDArrayFactory()
                                  .makeNDArray
                                  ( url, AccessMode.READ) );
                    }
                } catch ( IOException ex ) {
                    logger.warning( "Error creating NDArray ("
                                    + ex + ")" );
                    putChild ( xmlname, null );
                } catch ( HdxException ex ) {
                    logger.warning( "Error creating NDArray ("
                                    + ex + ")" );
                    putChild ( xmlname, null );
                }
            }
        }
        return (NDArray) contents.get( xmlname );
    }

    private boolean hasChildArray ( String xmlname ) {
        return ( getChildArray( xmlname ) != null );
    }

    /**
     * Returns the text associated with a child of a certain name.
     * The text value of the child is the text content (in the sense
     * of <code>Element.getNodeValue()</code>, unless the
     * <code>substAttribute</code> parameter is non-null <em>and</em>
     * that attribute is present.
     *
     * @param xmlname The name of the child element to examine
     * @param substAttribute If this is non-null and an attribute with this
     *                       name is present, then this is taken to be the
     *                       element's text value.
     * @return The text value as a String, or null if there was no such element
     *         or it had no associated text.
     */
    private String getChildText ( String xmlname, String substAttribute ) {
        if ( !contents.containsKey( xmlname ) )
        {
            Element kid = getChildElement( xmlname );
            if ( kid == null )
                putChild( xmlname, null );
            else {
                if (substAttribute != null
                    && kid.hasAttribute( substAttribute) )
                    putChild( xmlname, kid.getAttribute(substAttribute) );
                else
                    putChild( xmlname, kid.getNodeValue() );
            }
        }
        return (String) contents.get( xmlname );
    }


    /**
     * Finds the URL associated with an element.  Examine, in order,
     * the element's <code>url</code> and <code>uri</code> attributes.
     *
     * <p>XXX We may soon forbid getting the URL from the "uri"
     * attribute, restricting it to the "url" attribute alone.
     *
     * @return the URL, or null if none is available.
     * @throws HdxException the given URL is malformed.
     */
    private URL getURLFromElement( Element e )
            throws HdxException {

        String urlString = e.getAttribute( "url" );
        assert urlString != null;
        if ( urlString.length() == 0 )
            urlString = e.getAttribute( "uri" );
        assert urlString != null;

        URL url;
        if ( urlString.length() == 0 )
            url = null;
        else
            url = uk.ac.starlink.hdx.HdxFactory
                    .findFactory( e )
                    .fullyResolveURI( urlString, e );
        
        logger.fine( "DomNdxImpl.getURLFromElement(" + e + ")=" + url );
        return url;
            
    }
}
