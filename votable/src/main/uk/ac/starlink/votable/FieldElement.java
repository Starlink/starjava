package uk.ac.starlink.votable;

import java.util.logging.Logger;
import org.w3c.dom.Element;

/**
 * Table column characteristics represented by a FIELD element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FieldElement extends VOElement {

    private final long[] arraysize;
    private final Decoder decoder;
    private String datatype;
    private String blank;
    private String unit;
    private String ucd;
    private boolean isVariable;
    private long sliceSize;
    private ValuesElement actualValues;
    private ValuesElement legalValues;

    static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    public FieldElement( Element el, String systemId ) {
        this( el, systemId, "FIELD" );
    }

    FieldElement( Element el, String systemId, String tagname ) {
        super( el, systemId, tagname );

        /* Get datatype. */
        datatype = getAttribute( "datatype" );
        boolean assumedType = false;
        if ( datatype == null ) {
            logger.warning( "Missing datatype attribute for " + getHandle() +
                            " - assume char(*)" );
            datatype = "char";
            assumedType = true;
        }

        /* Get array size (as long as we haven't got an unknown datatype). */
        sliceSize = 1;
        isVariable = false;
        String as = assumedType ? "*" : getAttribute( "arraysize" );
        as = as == null ? as : as.trim();
        if ( as != null && as.length() > 0 ) {
            String[] dimtxt = as.split( "x" );
            int ndim = dimtxt.length;
            arraysize = new long[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                if ( i == ndim - 1 && dimtxt[ i ].trim().endsWith( "*" ) ) {
                    arraysize[ i ] = -1;
                    isVariable = true;
                }
                else {
                    long dim;
                    try {
                        dim = Long.parseLong( dimtxt[ i ] );
                    }
                    catch ( NumberFormatException e ) {
                        dim = 1;
                        logger.warning( "Bad arraysize element " + dimtxt[ i ]
                                      + " - assuming 1" );
                    }
                    if ( dim <= 0 ) {
                        dim = 1;
                        logger.warning( "Bad arraysize element " + dimtxt[ i ]
                                      + " - assuming 1" );
                    }
                    arraysize[ i ] = dim;
                    sliceSize *= arraysize[ i ];
                }
            }
        }
        else {
            arraysize = new long[ 0 ];
        }

        /* Get Values children. */
        VOElement[] children = getChildren();
        for ( int i = 0; i < children.length; i++ ) {
            VOElement child = children[ i ];
            if ( child.getTagName().equals( "VALUES" ) ) {
                ValuesElement vals = (ValuesElement) child;
                if ( vals.getType().equals( "legal" ) ) {
                    legalValues = vals;
                }
                else if ( vals.getType().equals( "actual" ) ) {
                    actualValues = vals;
                }
            }
        }

        /* Set blank value. */
        if ( legalValues != null ) {
            blank = legalValues.getNull();
        }
        else if ( actualValues != null ) {
            blank = actualValues.getNull();
        }

        /* Construct decoder. */
        decoder = Decoder.makeDecoder( datatype, arraysize, blank );

        /* Get simple attributes. */
        this.unit = getAttribute( "unit" );
        this.ucd = getAttribute( "ucd" );
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
        return (long[]) arraysize.clone();
    }

    /**
     * Returns the value of the <tt>datatype</tt> attribute.
     *
     * @return  the datatype
     */
    public String getDatatype() {
        return datatype;
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
        return blank;
    }

    /**
     * Returns the value of the <tt>unit</tt> attribute, 
     * or <tt>null</tt> if there is none.
     *
     * @return  the unit string
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns the value of the <tt>ucd</tt> attribute,
     * or <tt>null</tt> if there is none.
     *
     * @return  the ucd string
     * @see     uk.ac.starlink.table.UCD
     */
    public String getUcd() {
        return ucd;
    }

    /**
     * Returns a VALUES child of this element with the attribute 
     * type='legal', or <tt>null</tt> if none exists.
     *
     * @return  the 'legal' Values object
     */
    public ValuesElement getLegalValues() {
        return legalValues;
    }

    /**
     * Returns a VALUES child of this element with the attribute
     * type='actual', or <tt>null</tt> if none exists.
     *
     * @return  the 'actual' Values object
     */
    public ValuesElement getActualValues() {
        return actualValues;
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
        return decoder;
    }

}
