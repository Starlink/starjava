package uk.ac.starlink.treeview.votable;

import org.w3c.dom.Element;

/**
 * Object representing a PARAM element in a VOTable.
 */
public class Param extends Field {

    private String value;

    public Param( Element el ) {
        super( el );
        this.value = el.getAttribute( "value" );
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        String result = super.toString();
        if ( value != "" ) {
            result += " = " + value;
        }
        return result;
    }
}
