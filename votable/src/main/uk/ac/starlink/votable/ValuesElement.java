package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

/**
 * Field or Param value restriction set represented by a VALUES element
 * in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ValuesElement extends VOElement {

    /**
     * Constructs a ValuesElement from a DOM element.
     *
     * @param  base  VALUES element
     * @para   doc   owner document for new element
     */
    ValuesElement( Element base, VODocument doc ) {
        super( base, doc, "VALUES" );
    }

    /**
     * Returns the specified maximum value for this ValuesElement object
     * (the value of any Maximum child).
     *
     * @return  maximum value, or <code>null</code> if none specified
     */
    public String getMaximum() {
        VOElement maxel = getChildByName( "MAX" );
        if ( maxel != null ) {
            return maxel.hasAttribute( "value" )
                 ? maxel.getAttribute( "value" )
                 : null;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the specified minimum value for this ValuesElement object
     * (the value of any Minimum child).
     *
     * @return  minimum value, or <code>null</code> if none specified
     */
    public String getMinimum() {
        VOElement minel = getChildByName( "MIN" );
        if ( minel != null ) {
            return minel.hasAttribute( "value" )
                 ? minel.getAttribute( "value" )
                 : null;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the specified option values for this ValuesElement object.
     *
     * @return  an array of option strings ('value' attributes
     *          of OPTION children)
     */
    public String[] getOptions() {
        VOElement[] optels = getChildrenByName( "OPTION" );
        List<String> options = new ArrayList<String>();
        for ( int i = 0; i < optels.length; i++ ) {
            if ( optels[ i ].hasAttribute( "value" ) ) {
                options.add( optels[ i ].getAttribute( "value" ) );
            }
        }
        return options.toArray( new String[ 0 ] );
    }

    /**
     * Returns the 'null' value for this ValuesElement object, that is the
     * value which represents an undefined data value.  This is
     * the value of the 'null' attribute of the VALUES element,
     * but does not have anything to do with the Java language
     * <code>null</code> value.
     *
     * @return   the 'null' value for this ValuesElement object or, confusingly,
     *           <code>null</code> if none is defined
     */
    public String getNull() {
        return hasAttribute( "null" )
             ? getAttribute( "null" )
             : null;
    }

    /**
     * Returns the supplied or implied value of the 'type' attribute of this
     * ValuesElement object.  According to the VOTable definition this 
     * ought to be one of the strings "actual" or "legal".
     *
     * @return  values type
     */
    public String getType() {
        return hasAttribute( "type" )
             ? getAttribute( "type" )
             : "legal";
    }

}
