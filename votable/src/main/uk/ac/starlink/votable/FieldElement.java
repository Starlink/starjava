package uk.ac.starlink.votable;

import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Table column characteristics represented by a FIELD element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FieldElement extends VOElement {

    private final static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructs a FieldElement from a DOM element.
     *
     * @param  base  FIELD element
     * @param  doc   owner document for new element
     */
    FieldElement( Element base, VODocument doc ) {
        super( base, doc );
    }

    /**
     * Returns the array size.  The returned value is an array of
     * <tt>long</tt>, with one element for each dimension.
     * The final dimension only may have the value -1, which indicates
     * that this dimension is unknown.  All other elements will be positive.
     *
     * @return   array giving dimensions of data in this field.
     */
    public long[] getArraysize() {
        int sliceSize = 1;
        String as = hasAttribute( "datatype" ) 
                  ? getAttribute( "arraysize" )
                  : "*";
        as = as == null ? null : as.trim();
        if ( as != null && as.length() > 0 ) {
            String[] dimtxt = as.split( "x" );
            int ndim = dimtxt.length;
            long[] arraysize = new long[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                if ( i == ndim - 1 && dimtxt[ i ].trim().endsWith( "*" ) ) {
                    arraysize[ i ] = -1;
                }
                else {
                    long dim;
                    try {
                        dim = Long.parseLong( dimtxt[ i ] );
                    }
                    catch ( NumberFormatException e ) {
                        dim = 1;
                        logger_.warning( "Bad arraysize element " + dimtxt[ i ]
                                       + " - assuming 1" );
                    }
                    if ( dim <= 0 ) {
                        dim = 1;
                        logger_.warning( "Bad arraysize element " + dimtxt[ i ]
                                       + " - assuming 1" );
                    }
                    arraysize[ i ] = dim;
                    sliceSize *= arraysize[ i ];
                }
            }
            return arraysize;
        }
        else {
            return new long[ 0 ];
        }
    }

    /**
     * Returns the 'null' value for this FieldElement.
     * This is the value of the 'null' attribute of the VALUES child
     * with type='legal', or if that doesn't exist the 'null' attribute
     * of the VALUES child with type='actual' (this is some kind of
     * guesswork based on what is not written in the VOTable document).
     * This has nothing to do with the java <tt>null</tt> value.
     *
     * @return  the bad ("null") value or, confusingly, <tt>null</tt> if
     *          none is defined
     */
    public String getNull() {
        String blank = null;
        for ( Node child = getFirstChild(); child != null;
              child = child.getNextSibling() ) {
            if ( child instanceof ValuesElement ) {
                ValuesElement vals = (ValuesElement) child;
                if ( blank == null || "legal".equals( vals.getType() ) ) {
                    blank = vals.getNull();
                }
            }
        }
        return blank;
    }

    /**
     * Returns the value of the <tt>datatype</tt> attribute.
     * If no datatype attribute has been defined (which is illegal, but
     * not uncommon) then "char" will be returned.
     *
     * @return  the datatype
     */
    public String getDatatype() {
        if ( hasAttribute( "datatype" ) ) {
            return getAttribute( "datatype" );
        }
        else {
            logger_.warning( "Missing datatype attribute for " + getHandle() +
                             " - assume char(*)" );
            return "char";
        }
    }

    /**
     * Returns the value of the <tt>unit</tt> attribute,
     * or <tt>null</tt> if there is none.
     *
     * @return  the unit string
     */
    public String getUnit() {
        return hasAttribute( "unit" ) ? getAttribute( "unit" ) : null;
    }

    /**
     * Returns the value of the <tt>ucd</tt> attribute,
     * or <tt>null</tt> if there is none.
     *
     * @return  the ucd string
     * @see     uk.ac.starlink.table.UCD
     */
    public String getUcd() {
        return hasAttribute( "ucd" ) ? getAttribute( "ucd" ) : null;
    }

    /**
     * Returns a VALUES child of this element with the attribute
     * type='legal', or <tt>null</tt> if none exists.
     *
     * @return  the 'legal' Values object
     */
    public ValuesElement getLegalValues() {
        for ( Node child = getFirstChild(); child != null; 
              child = child.getNextSibling() ) {
            if ( child instanceof ValuesElement &&
                 "legal".equals( ((ValuesElement) child).getType() ) ) {
                return (ValuesElement) child;
            }
        }
        return null;
    }

    /**
     * Returns a VALUES child of this element with the attribute
     * type='actual', or <tt>null</tt> if none exists.
     *
     * @return  the 'actual' Values object
     */
    public ValuesElement getActualValues() {
        for ( Node child = getFirstChild(); child != null; 
              child = child.getNextSibling() ) {
            if ( child instanceof ValuesElement &&
                 "actual".equals( ((ValuesElement) child).getType() ) ) {
                return (ValuesElement) child;
            }
        }
        return null;
    }

    public String toString() {
        String str = getHandle();
        StringBuffer sbuf = new StringBuffer( str );
        if ( hasAttribute( "datatype" ) ) {
            sbuf.append( ' ' )
                .append( getAttribute( "datatype" ) );
        }
        if ( hasAttribute( "arraysize" ) ) {
            sbuf.append( " (" )
                .append( getAttribute( "arraysize" ) )
                .append( ")" );
        }
        if ( hasAttribute( "units" ) ) {
            sbuf.append( " / " + getAttribute( "units" ) );
        }
        return sbuf.toString();
    }

    /**
     * Returns the decoder object which knows how to turn raw data into
     * usable objects.  This is package-private for because it probably
     * doesn't need to be public, but it (and the Decoder class itself)
     * could be public if there was some reason for it to be so.
     */
    Decoder getDecoder() {
        return Decoder.makeDecoder( getDatatype(), getArraysize(), getNull() );
    }
}
