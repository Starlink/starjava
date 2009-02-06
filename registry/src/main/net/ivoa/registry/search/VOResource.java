/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * a class for extracting VOResource metadata out of a DOM tree.
 */
public class VOResource extends Metadata {

    private String title = null;
    private String identifier = null;
    private String shortName = null;
    private String restype = null;

    /**
     * create a VOResource metadata extracter
     */
    public VOResource(Element el) {
        super(el);
    }

    /**
     * return the resource class.  This is the value of the xsi:type attribute
     * on the VOResource root element.  The namespace prefix is stripped off
     * before returning.  
     */
    public String getResourceClass() {
        if (restype != null) return restype;

        String out = ((Element) getDOMNode()).getAttributeNS(XSI_NS, "type");
        if (out == null) out = "Resource";
        int c = out.indexOf(":");
        if (c >= 0) out = out.substring(c+1);
        restype = out;

        return out;
    }

    void cacheIdentityData(String name) {
        Node child = getDOMNode().getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (title == null && child.getNodeName().equals("title")) {
                    title = child.getNodeValue();
                    if (name.equals("title")) break;
                }
                else if (shortName == null && 
                         child.getNodeName().equals("shortName")) 
                {
                    shortName = child.getNodeValue();
                    if (name.equals("shortName")) break;
                }
                else if (identifier == null && 
                         child.getNodeName().equals("identifier")) 
                {
                    identifier = child.getNodeValue();
                    if (name.equals("identifier")) break;
                }
            }
        }
    }

    /**
     * return the resource's title
     */
    public String getTitle() {
        if (title == null) cacheIdentityData("title");
        return title;
    }

    /**
     * return the resource's identifier
     */
    public String getIdentifier() {
        if (identifier == null) cacheIdentityData("identifier");
        return identifier;
    }

    /**
     * return the resource's short name
     */
    public String getShortName() {
        if (shortName == null) cacheIdentityData("shortName");
        return shortName;
    }

    /**
     * return the capability element of a specified type.
     * @param type         the value of the xsi:type without a namespace prefix
     * @return Capability  the matching capability element or null if it 
     *                       doesn't exist
     */
    public Capability findCapabilityByType(String type) {
        Metadata[] out = getBlocks("capability");
        if (out == null) return null;

        for(int i=0; i < out.length; i++) {
            String xsitype = out[i].getXSIType();
            if (type.equals(xsitype)) 
                return new Capability(((Element) out[i].getDOMNode()), xsitype,
                                      null);
        }

        return null;
    }

    /**
     * return the capability element of a specified type.
     * @param id           the value of the standardID 
     * @return Capability  the matching capability element or null if it 
     *                       doesn't exist
     */
    public Capability findCapabilityByID(String id) {
        Metadata[] out = getBlocks("capability");
        if (out == null) return null;

        for(int i=0; i < out.length; i++) {
            String stdid = 
                ((Element) out[i].getDOMNode()).getAttribute("standardID");
            if (id.equals(stdid)) 
                return new Capability(((Element) out[i].getDOMNode()), 
                                      null, stdid);
        }

        return null;
    }

}
