package uk.ac.starlink.hdx;

import org.w3c.dom.*;


/**
 * A DOM-based implementation of <code>HdxContainer</code>.
 *
 * @author Norman Gray
 * @version $Id$
 */
class DomHdxContainer
        implements HdxContainer {
    private Element hdxElement;

    /**
     * Constructs an implementation of {@link HdxContainer} from a
     * supplied DOM.  The document element of the Document which the
     * DOM represents must be an HDX element.
     *
     * <p>It is possible that the Element we receive will have
     * siblings.  Allow this, but ignore the siblings.
     *
     * @param dom the DOM which represents the Hdx which this
     *            container encapsulates
     * 
     * @throws HdxException if the implementation cannot be
     * constructed for some reason.
     */
    DomHdxContainer (Element dom)
            throws HdxException {

        if (dom == null)
            throw new HdxException("Received null DOM");

        this.hdxElement = dom;

        if (! hdxElement.getTagName().equals(HdxResourceType.HDX.xmlName()))
            throw new HdxException("DOM malformed: document element is "
                                   + hdxElement.getTagName()
                                   + ", not "
                                   + HdxResourceType.HDX.xmlName());
    }

    // XXX Should we add a constructor which takes a Source?
    // Probably, but we need to know how permanent Sources can be.  Do
    // we have to create a new Source on each call of getSource?

    public Object get(HdxResourceType type) {
        assert hdxElement.getTagName().equals(HdxResourceType.HDX.xmlName());

        NodeList children = hdxElement.getChildNodes();
        Object ret = null;
        System.err.println("DomHdxContainer.getNdx: " + children.getLength()
                           + " children...");
        for (int i=0; i<children.getLength() && ret==null; i++) {
            Node child = children.item(i);
            System.err.println("  type " + child.getNodeType());
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element)child;
                System.err.println("DomHdxContainer.getNdx: el="
                                   + el.getTagName());
                if (HdxResourceType.match(el) == type) {
                    // If this factory fails to construct an Ndx from
                    // this element, then it returns null, and we keep
                    // on going round the loop -- that is, this keeps
                    // failing silently until we manage to construct
                    // one of the possibly multiple Ndx successfully.
                    // That's OK, because the contract of this method
                    // says that we can return any one of the Ndxs we
                    // find.
                    try {
                        ret = HdxFactory.getInstance().getObject(el);
                    } catch (HdxException ex) {
                        // Should send this to a Logger
                        System.err.println
                            ("Unexpected error constructing object: " + ex);
                    }
                }
            }
        }

        return ret;
    }

    public java.util.List getList(HdxResourceType type) {
        assert hdxElement.getTagName().equals(HdxResourceType.HDX.xmlName());

        java.util.List retlist = new java.util.ArrayList();
        NodeList children = hdxElement.getChildNodes();
        try {
            for (int i=0; i<children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)child;
                    HdxFactory mf = HdxFactory.getInstance();
                    if (HdxResourceType.match(el) == type) {
                        Object t = mf.getObject(el);
                        if (t != null)
                            retlist.add(t);
                    }
                }
            }
        } catch (HdxException ex) {
            // Should send this to a Logger
            System.err.println
                ("Unexpected error constructing object: " + ex);
        }

        return retlist;
    }

    public Element getDOM() {
        return hdxElement;
    }

    public javax.xml.transform.Source getSource() {
        return new javax.xml.transform.dom.DOMSource(hdxElement);
    }
}
