/*
 * Created by Ray Plante for the National Virtual Observatory
 * c. 2006
 */
package net.ivoa.registry.search;

import java.util.List;
import java.util.ListIterator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * a class for extracting VOResource metadata out of a DOM tree.
 */
public class Capability extends Metadata {

    private String captype = null;
    private String standardid = null;

    /**
     * create a Capability metadata extractor
     */
    public Capability(Element el) {
        super(el);
    }

    Capability(Element el, String xsitype, String stdid) {
        super(el);
        captype = xsitype;
        standardid = stdid;
    }

    /**
     * return the capability class.  This is the value of the xsi:type attribute
     * on the VOResource root element.  The namespace prefix is stripped off
     * before returning.  
     */
    public String getCapabilityClass() {
        if (captype != null) return captype;

        String out = ((Element) getDOMNode()).getAttributeNS(XSI_NS, "type");
        if (out == null) out = "Capability";
        int c = out.indexOf(":");
        if (c >= 0) out = out.substring(c+1);
        captype = out;

        return out;
    }

    /**
     * return the standard identifier for this capability or null if it 
     * does not have one set.
     */
    public String getStandardID() {
        if (standardid != null) return standardid;

        standardid = ((Element) getDOMNode()).getAttribute("standardID");
        return standardid;
    }

    /**
     * return the standard Interface description for this capability
     * @param version  the version of the protocol to get; if null, 1.0 is 
     *                   assumed.
     */
    public Metadata getStandardInterface(String version) {
        if (version == null) version = "1.0";
        return getInterface("std", version);
    }

    /**
     * return the Interface description for this capability
     * @param role     the role to look for
     * @param version  the version of the protocol to get; if null, 1.0 is 
     *                   assumed.
     */
    public Metadata getInterface(String role, String version) {
        Metadata intf = null;

        List interfaces = findBlocks("interface");
        ListIterator iter = interfaces.listIterator();
        while (iter.hasNext()) {
            intf = (Metadata) iter.next();
            if (role.equals(intf.getParameter("role")) && 
                version.equals(intf.getParameter("version")))
              return intf;
        }

        // implement!
        return null;
    }

    /**
     * return the access URL for the standard interface for this capability
     */
    public String getStandardAccessURL(String version) {
        return getAccessURL("std", version);
    }

    /**
     * return the access URL for the standard interface for this capability
     */
    public String getAccessURL(String role, String version) {
        Metadata intf = getInterface(role, version);
        return intf.getParameter("accessURL");
    }



}
