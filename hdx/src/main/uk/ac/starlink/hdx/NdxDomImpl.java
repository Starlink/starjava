package uk.ac.starlink.hdx;

import org.dom4j.Element;
import java.net.URL;

/** Implements Ndx.
 *
 * Has package access only.
 *
 * @author Norman Gray (norman@astro.gla.ac.uk)
 * @version $Id$
 */
class NdxDomImpl implements Ndx {

    //private org.dom4j.QName myQName;

    /** A hash to handle the object contents of this object.  The keys
	will be HdxResourceType objects.  Note that HashMap methods
	aren't synchronized, so it mustn't be used in a multithreaded
	environment.  
    */
    private java.util.HashMap contents;

    /** Holds the tree information.  This implements a Bridge pattern. */
    private Element domElement;

    /** No-arg constructor creates an Ndx with no content.  This is
	more useful for error handling than for normal code.  This
	does <em>not</em> throw an {@link HdxException} if it fails
	(therefore it is safe to use in an exception handler).
    */
    NdxDomImpl () {
	domElement = null;
    }

    /** Constructs a new object based on the information in the
	org.dom4j.Element.

	<p>For simplicity, we demand that the
	incoming element be in no namespace, and have the
	correct name.  We could make this object handle the required
	searching itself, at the cost of being slightly more
	complicated, but since this element is intended to be
	constructed by a factory, which will do a variety of such
	simple transformations and searches, it seems best to force
	all the intelligence to be there.  For much the same reason,
	we object by throwing an exception, if these requirements are
	not satisfied.

	@throws NdxException if the input element's QName is not of
	the required type.
    */
    NdxDomImpl (Element e)
	    throws HdxException {
	domElement = e;
	org.dom4j.QName qn = domElement.getQName();
	org.dom4j.Namespace ns = qn.getNamespace();

	if (! qn.getName().equals(HdxResourceType.NDX.xmlName()))
	    throw new HdxException ("Invalid Element ["
				    + ns.getURI()
				    + "]" + qn.getName()
				    + "!=" + HdxResourceType.NDX.xmlName()
				    + " for NdxDomImpl constructor");

	//System.err.println ("Constructed NdxDomImpl with QName [" + ns.getURI() + "]" + qn.getName());
    }

//     /** The no-arg constructor creates a simple NDX object. */
//     NdxDomImpl() {
// 	this(new QName(HdxResourceType.NDOBJ.xmlName()));
// 	System.err.println("Constructed empty NdxDomImpl");
//     }
//
//     /** Create a new object to represent an NDX object.  The QName
// 	will almost certainly match HdxResourceType.NDOBJ.xmlName(),
// 	but it doesn't actually matter, so let's not get huffy if it
// 	isn't.
//     */
//     NdxDomImpl (QName qname) {
// 	super(qname);
// 	myQName = qname;
// 	System.err.println ("Constructed NdxDomImpl, with QName "
// 			    + myQName.getQualifiedName());
//     }
//
//     /** Create a new object to represent an NDX object.
// 	@see #NdxDomImpl(QName)
//     */
//     NdxDomImpl(QName qname, int attributeCount) {
// 	super(qname, attributeCount);
// 	myQName = qname;
// 	System.err.println ("Constructed NdxDomImpl, with QName "
// 			    + myQName.getQualifiedName()
// 			    + " and " + attributeCount + " attributes");
//     }

    /*
      XXX: Not used.  See notes in NdxHandler.serializeToXML()
    public String asXML() {
	StringBuffer sb = new StringBuffer();
	NDArrayDomNode child;
	System.err.println("in asXML()");
	sb.append("<" + HdxResourceType.NDOBJ.xmlName() + ">\n");
	// XXX: better would be to get an iterator from HdxResourceType
	if ((child = (NDArrayDomNode)getImage()) != null)
	    sb.append(child.asXML());
	if ((child = (NDArrayDomNode)getVariance()) != null)
	    sb.append(child.asXML());
	if ((child = (NDArrayDomNode)getQuality()) != null)
	    sb.append(child.asXML());
	sb.append("</" + HdxResourceType.NDOBJ.xmlName() + ">\n");
	return sb.toString();
    }
    */

    public NDArray getImage() {
	return getChild(HdxResourceType.DATA);
    }

    public NDArray getVariance() {
	return getChild(HdxResourceType.VARIANCE);
    }

    public NDArray getQuality() {
	return getChild(HdxResourceType.QUALITY);
    }

    public boolean hasImage() {
	return hasChild(HdxResourceType.DATA);
    }

    public boolean hasVariance() {
	return hasChild(HdxResourceType.VARIANCE);
    }

    public boolean hasQuality() {
	return hasChild(HdxResourceType.QUALITY);
    }

    public Ndx add (Ndx n1, int mode) {
	System.err.println ("Called NdxDomImpl.add()");
	return null;
    }

    public String toString() {
	return "NdxDomImpl, with QName "
	    + (domElement == null ? "<empty>" : domElement.getQualifiedName());
    }

    /** Returns an XML representation of the NDX.  This default form
     * generates the output in a minimal form, without any XML
     * or namespace declaration, but this might change.
     *
     * <p>XXX This method is not at present in the Ndx interface, so
     * not really blessed, so might disappear in future versions.
     */
    public String toXML() {
        return toXML(false, null);
    }
 
    /* Writes an NDX to a string as XML, declaring elements in the
     * HDX namespace with the given prefix.
     * @param xmlDecl if true, the XML declaration (<code>&lt;?xml
     * ...?></code>) is prefixed to the output.
     * @param prefix the namespace prefix to use.  If null, use no
     * prefix.
     */
    public String toXML(boolean xmlDecl, String prefix) {
        StringBuffer sb = new StringBuffer();
        NDArray a;
        String prefixstring = (prefix == null ? "" : prefix+":");
 
        if (xmlDecl)
            sb.append("<?xml version=\"1.0\" standalone=\"yes\"?>");
        sb.append("<" + prefixstring + HdxResourceType.HDX.xmlName());
        if (prefix != null)
            sb.append(" xmlns:" + prefix + "=\""
                      + HdxResourceType.HDX_NAMESPACE + "\"");
        sb.append("><" + prefixstring + HdxResourceType.NDX.xmlName() + '>');
        // XXX This is possibly inefficient, since the getChild()
        // causes the NDArray to be constructed if it is present.  It
        // might become worthwhile to optimize this so that this
        // potentially expensive operation is avoided.  If NDArray is
        // suitably lazy, however, constructing it in order to call
        // its toXML() method might not be expensive at all.
        for (java.util.ListIterator li = HdxResourceType.getChildrenIterator();
             li.hasNext();
             ) {
            HdxResourceType t = (HdxResourceType)li.next();
            a = getChild(t);
            if (a != null)
                // it might be nice to have a NDArray.toXML() method,
                // but the NDArray doesn't (currently?) know what type 
                // it is, so we can't.
                sb.append('<' + prefixstring + t.xmlName() + '>'
                          + a.getURI()
                          + "</" + prefixstring + t.xmlName() + '>');
        }
        sb.append ("</" + prefixstring + HdxResourceType.NDX.xmlName()
                   + "></"
                   + prefixstring + HdxResourceType.HDX.xmlName() + '>');
        return sb.toString();
    }

    /** Save an NDObjectDomNode for later retrieval. */
    private void putChild (HdxResourceType r, Object n) {
	if (contents == null)
	    contents = new java.util.HashMap (5);
	contents.put (r, n);
	return;
    }

    /** Retrieve an NdxDomImpl.  Retrieves the object from the
	hash, or from the DOM tree if it hasn't been asked for the
	object before.  If there is more than one object of a certain
	type in the tree (there shouldn't be), then it will return
	only the first one, without signalling any error.

	@return the indexed object, or null if nothing was found.
    */
    private NDArray getChild (HdxResourceType r) {
	if (contents == null || !contents.containsKey(r))
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

    private boolean hasChild (HdxResourceType r) {
	if (contents == null)
	    return false;
	else
	    return (getChild(r) != null);
    }

    /** Finds the URI associated with an element.  If there's a "uri"
        attribute, that's used; if not, then the URI is the element
        content.  We assume that, if the "uri" attribute is not
        present, there is only a <em>single</em> text node below this
        one.

        @return the URI, or an empty string if none is available.
        @throws HdxException if the node violates the assumption that
        it has only a single text node child, or if the given URL is
	malformed.
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
}
