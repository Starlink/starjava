package uk.ac.starlink.treeview.votable;

import org.w3c.dom.Element;

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
