package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

/**
 * Field or Param value restriction set represented by a VALUES element
 * in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Values extends VOElement {

    private String minimum;
    private String maximum;
    private String[] options;
    private String blank;
    private String type;
    private boolean isInvalid;

   
    public Values( Source xsrc ) throws TransformerException {
        this( transformToDOM( xsrc ) );
    }

    /**
     * Constructs a Values object from an XML Source containing a VALUES
     * element.
     *
     * @param  dsrc  a DOM Source containing a VALUES element
     */
    public Values( DOMSource dsrc ) {
        super( dsrc, "VALUES" );
        blank = getAttribute( "null" );
        type = getAttribute( "type" );
        isInvalid = getAttribute( "invalid" ) == "yes";
        if ( type == null ) {
            type = "legal";
        }

        VOElement[] children = getChildren();
        List opts = new ArrayList();
        for ( int i = 0; i < children.length; i++ ) {
            VOElement child = children[ i ];
            if ( child.getTagName().equals( "MAX" ) ) {
                maximum = child.getAttribute( "value" );
            }
            else if ( child.getTagName().equals( "MIN" ) ) {
                minimum = child.getAttribute( "value" );
            }
            else if ( child.getTagName().equals( "OPTION" ) ) {
                opts.add( child.getAttribute( "name" ) );
            }
        }
        options = (String[]) opts.toArray( new String[ 0 ] );
    }

    /**
     * Returns the specified maximum value for this Values object
     * (the value of any Maximum child).
     *
     * @return  maximum value, or <tt>null</tt> if none specified
     */
    public String getMaximum() {
        return maximum;
    }

    /**
     * Returns the specified minimum value for this Values object
     * (the value of any MINIMUM child).
     *
     * @return  minimum value, or <tt>null</tt> if none specified
     */
    public String getMinimum() {
        return minimum;
    }

    /**
     * Returns the specified option values for this Values object.
     *
     * @return  an array of option strings ('value' attributes
     *          of OPTION children)
     */
    public String[] getOptions() {
        return (String[]) options.clone();
    }

    /**
     * Returns the 'null' value for this Values object, that is the
     * value which represents an undefined data value.  This is
     * the value of the 'null' attribute of the VALUES element, 
     * but does not have anything to do with the Java language
     * <tt>null</tt> value.
     *
     * @return   the 'null' value for this Values object or, confusingly,
     *           <tt>null</tt> if none is defined
     */
    public String getNull() {
        return blank;
    }

    /**
     * Returns the supplied or implied value of the 'type' attribute of this
     * Values object. 
     *
     * @return  one of the strings 'actual' or 'legal'
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the sense of the supplied or implied 'invalid' attribute 
     * of this Values object.  I don't know what the semantics of this
     * is supposed to be though, I can't find it referenced 
     * in the VOTable document.
     *
     * @return  is the 'invalid' attribute present and equal to "yes"?
     */
    public boolean isInvalid() {
        return isInvalid;
    }
}
