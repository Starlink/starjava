package uk.ac.starlink.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class DOMUtils {

    /** Private dummy sole constructor. */
    private DOMUtils() {}

    /**
     * Returns the first child element of a node which has a given name.
     *
     * @param  parent  the node whose children are to be searched
     * @param  name    the name of the element being searched for
     * @return the first child of <tt>parent</tt> which is an <tt>Element</tt>
     *         and has the tagname <tt>name</tt>, or <tt>null</tt> if none
     *         match
     */
    public static Element getChildElementByName( Node parent, String name ) {
        for ( Node child = parent.getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof Element ) {
                Element childEl = (Element) child;
                String childName = childEl.getTagName();
                if ( childName.equals( name ) ) {
                    return childEl;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representing the plain text content of an element.
     * Any comments, attributes, elements or other non-text children 
     * are ignored, and all CDATA and Text nodes are merged to 
     * give a single string.
     * 
     * @param   el  the element whose text content is wanted
     * @return  the pure text content.  If there is none, an empty 
     *          string is returned.
     */
    public static String getTextContent( Element el ) {
        StringBuffer sb = new StringBuffer();
        for ( Node child = el.getFirstChild(); child != null; 
              child = child.getNextSibling() ) {
            if ( child instanceof Text ) {
                Text childText = (Text) child;
                sb.append( childText.getData() );
            }
        }
        return sb.toString();
    }

}
