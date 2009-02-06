/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * this class provides convenient access to information inside 
 * a DOM Node
 */
public class Metadata {

    // the delegate node
    private Node del = null;
    private String parentPath = null;
    protected HashMap cache = new HashMap();

    protected static final String XSI_NS = 
        "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * wrap a DOM Node
     * @param el    the node to wrap
     */
    public Metadata(Node el) {
        del = el;
        parentPath = el.getNodeName();
    }

    /**
     * wrap a DOM element
     * @param el    the element to wrap
     * @param path  a path name to assume for the element
     */
    public Metadata(Node el, String path) {
        del = el;
        parentPath = path;
    }

    /** 
     * return the wrapped Node
     */
    public Node getDOMNode() { return del; }

    /**
     * return the pathname configured with this element
     */
    public String getPathName() { return parentPath; }

    class MatchedBlocks extends LinkedList {
        public MatchedBlocks(Metadata first) {
            addLast(first);
        }

        public void appendNode(Node node, String base) { 
            addLast(new Metadata(node, base + "/" + node.getNodeName()));
        }

        public Metadata pop() { return (Metadata) removeFirst(); }
    }

    /**
     * return all metadata blocks that match a given path name as a List
     * @param path   the path to the desired XML node.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    protected List findBlocks(String path) throws IllegalArgumentException {
        String name;
        Node node;
        int i;

        MatchedBlocks matched = new MatchedBlocks(this);
        StringTokenizer tok = new StringTokenizer(path, "/");
        while (tok.hasMoreTokens()) {
            boolean attrOnly = false;
            name = tok.nextToken();
            if (name.startsWith("@")) {
                if (tok.hasMoreTokens())
                    throw new IllegalArgumentException("Illegal path: " + path);
                attrOnly = true;
                name = name.substring(1);
            }

            int len = matched.size();
            for(i=0; i < len; i++) {
                Metadata candidate = matched.pop();
                boolean findElements = true;
                node = candidate.getDOMNode();
                if (! tok.hasMoreTokens() && 
                    node.getNodeType() == Node.ELEMENT_NODE) 
                {
                    // at the end of the path; check for matching attributes
                    // Axis BUG!
//                     Node att = (name.equals("xsi:type")) 
//                         ? ((Element) node).getAttributeNodeNS(XSI_NS, "type")
//                         : ((Element) node).getAttributeNode(name);
                    // workaround code:
                    NamedNodeMap attrs = node.getAttributes();
                    Node att = null;
                    if (attrs != null) {
                        att = (name.equals("xsi:type"))
                            ? attrs.getNamedItemNS(XSI_NS, "type")
                            : attrs.getNamedItem(name);
                    }
                    if (att != null) 
                        matched.appendNode(att, candidate.getPathName());
                    if (attrOnly) findElements = false;
                }

                if (findElements) {
                    for(node = node.getFirstChild(); 
                        node != null; 
                        node = node.getNextSibling())
                    {
                        if (node.getNodeType() == Node.ELEMENT_NODE && 
                            node.getNodeName().equals(name))
                        {
                            matched.appendNode(node, candidate.getPathName());
                        }
                    }
                }
            }
        }

        return matched;
    }

    /**
     * return all Metadata blocks that match a given path name.  Metadata
     * blocks are elements that contain other elements or attributes containing
     * metadata information.  
     * @param path   the path to the desired XML node.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    public Metadata[] getBlocks(String path) 
         throws IllegalArgumentException 
    {
        List matched = findBlocks(path);
        Metadata[] out = new Metadata[matched.size()];
        ListIterator iter = matched.listIterator();
        for(int i=0; i < out.length && iter.hasNext(); i++) {
            out[i] = (Metadata) iter.next();
        }
        return out;
    }

    /**
     * return values of all parameters with a given name
     * @param path   the path to the desired parameter.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    public String[] getParameters(String path) {
        String[] out = (String[]) cache.get(path);
        if (out != null) return out;

        List matched = findBlocks(path);

        LinkedList values = new LinkedList();
        ListIterator iter = matched.listIterator();
        while(iter.hasNext()) {
            Node node = ((Metadata) iter.next()).getDOMNode();
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                values.addLast(node.getNodeValue());
            }
            else if (node.getNodeType() == Node.ELEMENT_NODE) {
                for (node = node.getFirstChild();
                     node != null && node.getNodeType() != Node.TEXT_NODE;
                     node = node.getNextSibling()) 
                { 
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        node = null;
                        break;
                    }
                }
                if (node != null) 
                    values.addLast(node.getNodeValue().trim());
            }
        }

        out = new String[values.size()];
        iter = values.listIterator();
        for(int i=0; i < out.length; i++) {
            out[i] = (String) iter.next();
        }
        cache.put(path, out);

        return out;
    }

    /**
     * return the first value matching the given parameter name
     * @param path   the path to the desired parameter.  This is a 
     *                  slash-delimited string in which each field between 
     *                  slashes is usually an element name.  The last field 
     *                  may either be the name of an element or an attribute.  
     *                  If the last field begins with an "at" character (@), 
     *                  only a matching attribute will be returned.
     * @throws IllegalArgumentException  if any field except the last one 
     *             contains an "@" character
     */
    public String getParameter(String path) {
        String[] out = getParameters(path);
        return ((out != null && out.length > 0) ? out[0] : null);
    }

    /**
     * return the value of the xsi:type attribute if it exists.
     * @return String  the xsi:type with the namespace prefix removed
     */
    public String getXSIType() {
        if (del.getNodeType() != Node.ELEMENT_NODE) return null;
        String out = ((Element) del).getAttributeNS(XSI_NS, "type");
        if (out == null) return null;
        int c = out.indexOf(":");
        if (c >= 0) out = out.substring(c+1);
        return out;
    }

    /**
     * clear the internal parameter cache.  Call this if the underlying 
     * DOM model has been updated.  
     */
    public void clearCache() {
        cache.clear();
    }
}
