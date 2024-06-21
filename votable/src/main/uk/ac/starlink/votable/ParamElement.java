package uk.ac.starlink.votable;

import org.w3c.dom.Element;

/**
 * Object representing a PARAM element in a VOTable.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class ParamElement extends FieldElement {

    private String valString_;
    private Object valObject_;

    /**
     * Constructs a ParamElement from a DOM element.
     *
     * @param  base  PARAM element
     * @param  doc   owner document for new element
     */
    ParamElement( Element base, VODocument doc ) {
        super( base, doc );
    }

    /**
     * Returns the value of the <code>value</code> attribute,
     * or an empty string if it has none.
     *
     * @return  the value string
     */
    public String getValue() {
        return getAttribute( "value" );
    }

    /**
     * Returns the object represented by the value of this Param.
     * This is constructed by decoding the <code>value</code> attribute in
     * the same way as for TABLEDATA content of a table for a FIELD
     * of this kind.
     *
     * @return  the value object
     */
    public Object getObject() {
        String val = getValue();
        if ( ! val.equals( valString_ ) ) {
            valString_ = val;
            valObject_ = ( val != null && val.length() > 0 )
                       ? getDecoder().decodeString( val )
                       : null;
        }
        return valObject_;
    }
}
