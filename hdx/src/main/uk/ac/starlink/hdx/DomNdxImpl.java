package uk.ac.starlink.hdx;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.hdx.array.NDArrayFactory;

/** 
 * An NdxImpl implementation in which the hierarchical data is stored in 
 * a DOM.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @author Mark Taylor (Starlink)
 * @author Peter W. Draper
 * @version $Id$
 */
class DomNdxImpl implements NdxImpl {

    //private org.dom4j.QName myQName;

    /** A hash to handle the object contents of this object.  The keys
     *  will be HdxResourceType objects.  Note that HashMap methods
     *  aren't synchronized, so it mustn't be used in a multithreaded
     *  environment.  
     */
    private Map contents = new HashMap();

    /** Holds the tree information. */
    private Element domElement;

    /** Constructs a new object based on the information in the
     *  org.dom4j.Element.
     *
     *  <p>For simplicity, we demand that the
     *  incoming element be in no namespace, and have the
     *  correct name.  We could make this object handle the required
     *  searching itself, at the cost of being slightly more
     *  complicated, but since this element is intended to be
     *  constructed by a factory, which will do a variety of such
     *  simple transformations and searches, it seems best to force
     *  all the intelligence to be there.  For much the same reason,
     *  we object by throwing an exception, if these requirements are
     *  not satisfied.
     *
     *  @throws NdxException if the input element's QName is not of
     *  the required type.
     */
    DomNdxImpl (Element e)
            throws HdxException {
        domElement = e;
        org.dom4j.QName qn = domElement.getQName();
        org.dom4j.Namespace ns = qn.getNamespace();

        if (! qn.getName().equals(HdxResourceType.NDX.xmlName()))
            throw new HdxException ("Invalid Element ["
                                    + ns.getURI()
                                    + "]" + qn.getName()
                                    + "!=" + HdxResourceType.NDX.xmlName()
                                    + " for DomNdxImpl constructor");

        //System.err.println ("Constructed DomNdxImpl with QName [" + ns.getURI() + "]" + qn.getName());
    }

    public NDArray getImage() {
        return getChildArray(HdxResourceType.DATA);
    }

    public NDArray getVariance() {
        return getChildArray(HdxResourceType.VARIANCE);
    }

    public NDArray getQuality() {
        return getChildArray(HdxResourceType.QUALITY);
    }

    public String getTitle() {
        return getChildText(HdxResourceType.TITLE);
    }

    public byte getBadBits() {
        String bytestr = getChildText( HdxResourceType.BADBITS );
        return ( bytestr == null ) ? (byte) 0
                                   : Byte.decode( bytestr ).byteValue();
    }

    public org.w3c.dom.Element getWCSElement()
    {
        Element wcs = getChild( HdxResourceType.WCS );
        if ( wcs == null ) {
            return null;
        }
        return dom4jElementToW3c( wcs );
    }

    public boolean hasImage() {
        return hasChildArray(HdxResourceType.DATA);
    }

    public boolean hasVariance() {
        return hasChildArray(HdxResourceType.VARIANCE);
    }

    public boolean hasQuality() {
        return hasChildArray(HdxResourceType.QUALITY);
    }

    public boolean hasWCS() {
        return hasChild( HdxResourceType.WCS );
    }

    public String toString() {
        return "DomNdxImpl, with QName "
            + (domElement == null ? "<empty>" : domElement.getQualifiedName());
    }

    /** Save an NDObjectDomNode for later retrieval. */
    private void putChild (HdxResourceType r, Object n) {
        contents.put (r, n);
        return;
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
    private NDArray getChildArray (HdxResourceType r) {
        if (!contents.containsKey(r))
        {
            if (domElement == null) {
                putChild(r, null);
            } else {
                java.util.List nl = domElement.selectNodes(r.xmlName());
                if (nl.isEmpty())
                    putChild(r, null);
                else
                {
                    try {
                        putChild (r,
                                  NDArrayFactory.makeReadableNDArray
                                  (getURIFromElement((Element)nl.get(0))));
                    } catch (IOException ex) {
                        System.err.println("Error creating NDArray ("
                                           + ex + ")");
                        putChild (r, null);
                    } catch (HdxException ex) {
                        System.err.println("Error creating NDArray ("
                                           + ex + ")");
                        putChild (r, null);
                    }
                }
            }
        }
        return (NDArray) contents.get(r);
    }

    private boolean hasChildArray (HdxResourceType r) {
        return (getChildArray(r) != null);
    }

    private String getChildText( HdxResourceType r ) {
        if (!contents.containsKey(r))
        {
            if (domElement == null) {
                putChild(r, null);
            } else {
                java.util.List nl = domElement.selectNodes(r.xmlName());
                if (nl.isEmpty())
                    putChild(r, null);
                else
                {
                    putChild(r, getTextFromElement((Element)nl.get(0)));
                }
            }
        }
        return (String) contents.get(r);
    }


    /**
     * Finds the URI associated with an element.  If there's a "uri"
     * attribute, that's used; if not, then the URI is the element
     * content.  We assume that, if the "uri" attribute is not
     * present, there is only a <em>single</em> text node below this
     * one.
     *
     * @return the URI, or an empty string if none is available.
     * @throws HdxException if the node violates the assumption that
     * it has only a single text node child, or if the given URL is
     * malformed.
     */
    private URL getURIFromElement(Element e)
            throws HdxException {
        String uri = e.attributeValue("uri");
        try {
            if (uri != null && uri.length() > 0)
                return new URL(uri);
            else
            {
                uri = e.getStringValue();
                if (uri.length() == 0)
                    throw new HdxException
                        ("Element " + e.getQualifiedName()
                         + " has no text content");
                return new URL(uri);
            }
        } catch (java.net.MalformedURLException ex) {
            throw new HdxException("Element " + e.getQualifiedName()
                                   + ": URI " + uri + " malformed");
        }
    }

    private String getTextFromElement(Element e) {
        return e.getText();
    }

    /**
     * Retrieve an Element.  Retrieves the object from the hash, or
     * from the DOM tree if it hasn't been asked for the object
     * before.  If there is more than one object of a certain type in
     * the tree (there shouldn't be), then it will return only the
     * first one, without signalling any error.
     *
     * @return the Element, or null if nothing was found.
     */
    private Element getChild( HdxResourceType r ) 
    {
        if ( ! contents.containsKey( r ) ) {
            if ( domElement == null ) {
                putChild( r, null );
            } 
            else {
                java.util.List nl = domElement.selectNodes( r.xmlName() );
                if ( nl.isEmpty() ) {
                    putChild( r, null );
                }
                else {
                    putChild( r, nl.get( 0 ) );
                }
            }
        }
        return (Element) contents.get( r );
    }

    /**
     * Return if the DOM contains a specified child element.
     */
    private boolean hasChild( HdxResourceType r ) 
    {
        return ( getChild( r ) != null );
    }

    /**
     * Convert a dom4j Element into a w3c one. Returns null if fails.
     */
    protected org.w3c.dom.Element dom4jElementToW3c( Element wcs )
    {
        //  Somewhat nastily this can only be done via a complete
        //  dom4j Document. TODO: refactor this away by removing dom4j.
        DOMWriter writer = new DOMWriter();
        org.w3c.dom.Document domDoc = null;
        Document dom4jDoc = null;
        DocumentFactory factory = DocumentFactory.getInstance();
        dom4jDoc = factory.createDocument( (Element) wcs.clone() );
        try {
            domDoc = writer.write( dom4jDoc );
            return domDoc.getDocumentElement();
        } 
        catch ( DocumentException e ) {
            e.printStackTrace();
            return null;
        }
    }
}
