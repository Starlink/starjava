/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2005, 2006
 */
package net.ivoa.adql.convert;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import javax.xml.transform.TransformerException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

/**
 * an abstract parent class for classes that parse ADQL/s into a DOM tree.  
 */
public abstract class ADQLs2DOMParser {

    protected Document doc = null;
    protected Node parent = null;
    protected int indent = -1;
    protected short nsmode = MODE_ALWAYS_QUALIFIED;

    public final static String XSI_NS =
        "http://www.w3.org/2001/XMLSchema-instance";

    public ADQLs2DOMParser() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            parent = doc;
        }
        catch (ParserConfigurationException pce) {
            throw new InternalError("programmer error: DOM new doc failure");
        }
//         System.err.println("doc is ready");
//         System.err.flush();
    }

    /**
     * create a parser that will append the result to the given parent
     */
    public ADQLs2DOMParser(Node parent) {
        this.parent = parent;
        doc = parent.getOwnerDocument();
    }

    /**
     * Parse the input ADQL/s Select query string into a DOM element
     **/
    public abstract Element parseSelect() throws TransformerException;
    
    /**
     * Parse the input ADQL/s Where clause string into a DOM element
     **/
    public abstract Element parseWhere() throws TransformerException;

    /**
     * create an ADQL Element with the proper namespace
     */
    protected abstract Element createADQLElement(String name);
    
    /**
     * set the desired pretty-fying indent amount.  If indent = 0, only 
     * carriage returns are inserted between each element.  If indent < 0,
     * no indentation or carriage returns will be inserted.  
     * @param indent  the amount of indentation per depth level
     */
    public void setIndent(int indent) { this.indent = indent; }

    /**
     * return the pretty-fying indent amount that will be inserted.  
     * If indent = 0, only carriage returns are inserted between each
     * element.  If indent < 0, no indentation or carriage returns will 
     * be inserted.  
     * @param indent  the amount of indentation per depth level
     */
    public int getIndent() { return indent; }

    /**
     * insert the text nodes that provide pretty indentations.  This 
     * will avoid inserting indentation multiple times.  
     */
    public void indent() {
        Element parent = doc.getDocumentElement();
        if (parent == null || indent < 0) return;
        indent(parent, "\n", indent);
    }

    /**
     * insert the text nodes that provide pretty indentations.  This 
     * will avoid inserting indentation multiple times.  
     * @param parent    the node to insert text nodes into
     * @param indent    the current indentation string on parent.  If null,
     *                     no indentation is currently in place.
     * @param incr      the indentation increment
     */
    public void indent(Element parent, String indent, int incr) {
        if (! parent.hasChildNodes()) return;
        if (indent == null) indent = "\n";
        String childIndent = addIndentation(indent, incr);

        boolean skipElement = false;
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                skipElement = true;
            }
            else {
                if (! skipElement) {
                    Node txt = doc.createTextNode(childIndent);
                    parent.insertBefore(txt, child);
                }
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    indent((Element) child, childIndent, incr);
                }
                skipElement = false;
            }
            child = child.getNextSibling();
        }
        if (! skipElement) {
            Node txt = doc.createTextNode(indent);
            parent.appendChild(txt);
        }
    }

    /**
     * append spaceCount spaces after the given base string
     */
    public static String addIndentation(String base, int spaceCount) {
        StringBuffer sb = new StringBuffer(base);
        for(int i=0; i < spaceCount; i++) sb.append(' ');
        return sb.toString();
    }

    /**
     * the namespace qualification mode in which all elements will always
     * be fully qualified with a prefix
     */
    public final static short MODE_ALWAYS_QUALIFIED = 0;

    /**
     * the namespace qualification mode in which default namespaces
     * are defined to minimize the qualification with prefixes.
     */
    public final static short MODE_DEFAULT_NS = 1;

    /**
     * the total number of namespace qualification modes supported
     */
    protected final static short MODE_COUNT = 2;

    /**
     * set the namespace qualification mode to use
     */
    public void setNSMode(short mode) {
        if (mode >= MODE_COUNT)
            throw new IllegalArgumentException("Undefined namespace " +
                                               "qualification modes (" + mode +
                                               ")");
        nsmode = mode;
    }

    /**
     * return the namespace qualification mode that will be used
     */
    public short getNSMode(short mode) { return nsmode; }
        
    protected Element getChildByTag(Element el, String name) {
        Node child = el.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                child.getNodeName().equals(name))
              return ((Element) child);
            child = child.getNextSibling();
        }
        return null;
    }

    protected boolean matchesXSIType(Element el, String qtype) {
        String type = el.getAttributeNS(XSI_NS, "xsi:type");
        return (type != null && type.length() > 0 && type.equals(qtype));
    }

    protected Element renameADQLElement(Element el, String newname, 
                                        Node parent) 
    {
        // create a new replacement element
        Element out = createADQLElement(newname);

        // copy over all the attributes
        Attr attr = null;
        NamedNodeMap attrs = el.getAttributes();
        while (attrs.getLength() > 0) {
            attr = (Attr) attrs.item(0);
            if (attr == null) break;
            el.removeAttributeNode(attr);
            out.setAttributeNode(attr);
        }

        // copy over all children
        Node node = el.getFirstChild();
        while (node != null) {
            el.removeChild(node);
            out.appendChild(node);
            node = el.getFirstChild();
        }

        if (parent != null) {
            parent.insertBefore(out, el);
            parent.removeChild(el);
        }

        return out;
    }

}
