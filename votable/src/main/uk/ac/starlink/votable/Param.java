package uk.ac.starlink.votable;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

/**
 * Object representing a PARAM element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Param extends Field {

    private Object valueObject;

    public Param( Source xsrc ) throws TransformerException {
        this( transformToDOM( xsrc ) );
    }

    public Param( DOMSource dsrc ) {
        super( dsrc, "PARAM" );
    }

    /**
     * Returns the value of the <tt>value</tt> attribute,
     * or an empty string if it has none.
     *
     * @param  the value string
     */
    public String getValue() {
        return getAttribute( "value" );
    }

    /**
     * Returns the object represented by the value of this Param.
     * This is constructed by decoding the <tt>value</tt> attribute in 
     * the same way as for TABLEDATA content of a table for a FIELD
     * of this kind.
     *
     * @return  the value object
     */
    public Object getObject() {
        if ( valueObject == null ) {
            String val = getValue();
            return ( val != null && val.length() > 0 )
                 ? getDecoder().decodeString( val ) 
                 : null;
        }
        return valueObject;
    }

    public String toString() {
        return super.toString() + "=\"" + getValue() + "\"";
    }

}
