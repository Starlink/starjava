package uk.ac.starlink.votable;

import org.w3c.dom.Element;

/**
 * Object representing a PARAM element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ParamElement extends FieldElement {

    private Object valueObject;

    public ParamElement( Element el, String systemId,
                         VOElementFactory factory ) {
        super( el, systemId, "PARAM", factory );
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
