package uk.ac.starlink.treeview.votable;

import org.w3c.dom.Element;
import uk.ac.starlink.util.DOMUtils;

public class GenericElement {

    private String description;
    private String id;
    private String name;
    private Element el;
 
    public GenericElement( Element el ) {
        this.el = el;
        if ( el.hasAttribute( "ID" ) ) {
            id = el.getAttribute( "ID" );
        }
        if ( el.hasAttribute( "name" ) ) {
            name = el.getAttribute( "name" );
        }
        Element descEl = DOMUtils.getChildElementByName( el, "DESCRIPTION" );
        if ( descEl != null ) {
            description = DOMUtils.getTextContent( descEl );
        }
    }

    public String getDescription() {
        return description;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Element getElement() {
        return el;
    }

    public String toString() {
        return getHandle();
    }

    /**
     * Returns something that can be used informally as a name for this
     * element.  May be ID or something other than the value of the
     * name attribute itself.
     */
    public String getHandle() {
        String handle = "";
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "name" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "ID" );
        }
        if ( handle.length() == 0 ) {
            handle = el.getAttribute( "ucd" );
        }
        if ( handle.length() == 0 && description != null ) {
            handle = description;
        }
        if ( handle.length() == 0 ) {
            handle = el.getTagName();
        }
        handle = handle.replaceFirst( "\n.*", "" );
        handle = handle.trim();
        return handle;
    }
}
