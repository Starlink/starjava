package uk.ac.starlink.treeview.votable;

import org.w3c.dom.Element;
import uk.ac.starlink.util.DOMUtils;

public class Coosys {

    private Element el;
    private String id;
    private String equinox;
    private String epoch;
    private String system;
    private String text;

    public Coosys( Element el ) {
        this.el = el;
        if ( el.hasAttribute( "ID" ) ) {
            id = el.getAttribute( "ID" );
        }
        if ( el.hasAttribute( "equinox" ) ) {
            equinox = el.getAttribute( "equinox" );
        }
        if ( el.hasAttribute( "epoch" ) ) {
            epoch = el.getAttribute( "epoch" );
        }
        if ( el.hasAttribute( "system" ) ) {
            system = el.getAttribute( "system" );
        }
        text = DOMUtils.getTextContent( el ).trim();
        if ( text.length() == 0 ) {
            text = null;
        }
    }

    public Element getElement() {
        return el;
    }

    public String getID() {
        return id;
    }

    public String getEquinox() {
        return equinox;
    }

    public String getEpoch() {
        return epoch;
    }

    public String getSystem() {
        return system;
    }

    public String getText() {
        return text;
    }
}
