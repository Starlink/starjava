package uk.ac.starlink.hdx;

import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.NodeDescendants;

import org.w3c.dom.*;

import java.util.Iterator;


/**
 * A DOM-based implementation of <code>HdxContainer</code>.
 *
 * @author Norman Gray
 * @version $Id$
 */
class DomHdxContainer
        implements HdxContainer {
    private Element hdxElement;
//     private HdxFactory myFactory;
    
    private static java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger( "uk.ac.starlink.hdx" );

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
        // XXX Hmm, should we insist that this DOM must have an
        // associated Document.  Would it be better to have a Document
        // argument?

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
    // Probably, but we need to be sure how permanent Sources can be,
    // which isn't terrifically clear from the documentation.  Do we
    // have to create a new Source on each call of getSource?

    public Object get(HdxResourceType type) {
        assert hdxElement.getTagName().equals(HdxResourceType.HDX.xmlName());

        try {
            for (Iterator ni = new NodeDescendants
                         (hdxElement, NodeDescendants.SHOW_ELEMENT).iterator();
                 ni.hasNext();
                 )
            {
                Element el = (Element)ni.next();
                if (HdxResourceType.match(el) == type) {
                    Object ret = HdxFactory.getInstance().getObject(el);
                    // If this factory fails to construct an Ndx from
                    // this element, then it returns null, and we keep
                    // on going round the loop -- that is, this keeps
                    // failing silently until we manage to construct
                    // one of the possibly multiple Ndx successfully.
                    // That's OK, because the contract of this method
                    // says that we can return any one of the Ndxs we
                    // find.
                    if (ret != null)
                        return ret; // JUMP OUT
                }
            }
        } catch (HdxException ex) {
            // This is detectable by the caller, by virtue of this
            // method returning null, but it's worth logging it here
            logger.warning
                    ("Unexpected error constructing object: "+ex);
        }
        return null;
    }
//     public Object get(HdxResourceType type) {
//         assert hdxElement.getTagName().equals(HdxResourceType.HDX.xmlName());

//         try {
//             for (Iterator ni = DOMUtils.treeIterator(hdxElement);
//                  ni.hasNext();
//                  )
//             {
//                 Node child = (Node)ni.next();
//                 if (child.getNodeType() == Node.ELEMENT_NODE) {
//                     Element el = (Element)child;
//                     if (HdxResourceType.match(el) == type) {
//                         Object ret = HdxFactory.getInstance().getObject(el);
//                         // If this factory fails to construct an Ndx from
//                         // this element, then it returns null, and we keep
//                         // on going round the loop -- that is, this keeps
//                         // failing silently until we manage to construct
//                         // one of the possibly multiple Ndx successfully.
//                         // That's OK, because the contract of this method
//                         // says that we can return any one of the Ndxs we
//                         // find.
//                         if (ret != null)
//                             return ret; // JUMP OUT
//                     }
//                 }
//             }
//         } catch (HdxException ex) {
//             // This is detectable by the caller, by virtue of this
//             // method returning null, but it's worth logging it here
//             logger.warning
//                     ("Unexpected error constructing object: "+ex);
//         }
//         return null;
//     }

    public java.util.List getList(HdxResourceType type) {
        assert hdxElement.getTagName().equals(HdxResourceType.HDX.xmlName());

        java.util.List retlist = new java.util.ArrayList();
        try {
            HdxFactory factory = HdxFactory.getInstance();
            for (Iterator ni = new NodeDescendants(hdxElement).iterator();
                 ni.hasNext();
                 )
            {
                Node child = (Node)ni.next();
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element)child;
                    if (HdxResourceType.match(el) == type) {
                        Object t = factory.getObject(el);
                        if (t != null)
                            retlist.add(t);
                    }
                }
            }
        } catch (HdxException ex) {
            logger.warning("Unexpected error constructing object: " + ex);
        }
//         NodeList children = hdxElement.getChildNodes();
//         try {
//             for (int i=0; i<children.getLength(); i++) {
//                 Node child = children.item(i);
//                 if (child.getNodeType() == Node.ELEMENT_NODE) {
//                     Element el = (Element)child;
//                     HdxFactory mf = HdxFactory.getInstance();
//                     if (HdxResourceType.match(el) == type) {
//                         Object t = mf.getObject(el);
//                         if (t != null)
//                             retlist.add(t);
//                     }
//                 }
//             }
//         } catch (HdxException ex) {
//             logger.warning("Unexpected error constructing object: " + ex);
//         }

        return retlist;
    }

    public Element getDOM(java.net.URI base) {
        // If we need to relativize the DOM, then we call cloneNode on
        // it, so we modify a copy.  This is potentially very
        // expensive -- is it the best thing?
        if (base == null)
            return hdxElement;
        else {
            Element t = (Element)uk.ac.starlink.util.DOMUtils.relativizeDOM
                    (hdxElement.cloneNode(true), base, null);
//             System.err.println("DomHdxContainer.getDOM("+base
//                                + ") produced "
//                                + HdxDocument.NodeUtil.serializeNode(t));
            return t;
        }
    }

    public javax.xml.transform.Source getSource(java.net.URI base) {
        return new javax.xml.transform.dom.DOMSource(getDOM(base));
    }

//     public HdxFactory getFactory() {
//         if (myFactory == null)
//             return HdxFactory.getInstance();
//         else
//             return myFactory;
//     }

//     public void setFactory(HdxFactory factory) {
//         myFactory = factory;
//     }
}
