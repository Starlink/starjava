package uk.ac.starlink.votable;

import javax.xml.transform.Source;

/**
 * Object representing a PARAM element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Param extends Field {

    private Object valueObject;

    public Param( Source xsrc ) {
        super( xsrc, "PARAM" );
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
            valueObject = getDecoder().decodeString( getValue() );
        }
        return valueObject;
    }

    public String toString() {
        return super.toString() + "=\"" + getValue() + "\"";
    }

}
