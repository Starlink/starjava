package uk.ac.starlink.votable;

import java.lang.reflect.Array;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import org.w3c.dom.NodeList;

/**
 * Table column characteristics represented by a FIELD element in a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Field extends VOElement {

    private final long[] arraysize;
    private final Decoder decoder;
    private String datatype;
    private String blank;
    private String unit;
    private String ucd;
    private boolean isVariable;
    private long sliceSize;
    private Values actualValues;
    private Values legalValues;

    static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    public Field( Source xsrc ) {
        this( xsrc, "FIELD" );
    }

    Field( Source xsrc, String tagname ) {
        super( xsrc, tagname );

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
        if ( as != null ) {
            String[] dimtxt = as.split( "x" );
            int ndim = dimtxt.length;
            arraysize = new long[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                if ( i == ndim - 1 && dimtxt[ i ].trim().endsWith( "*" ) ) {
                    arraysize[ i ] = -1;
                    isVariable = true;
                }
                else {
                    try {
                        arraysize[ i ] = Long.parseLong( dimtxt[ i ] );
                        sliceSize *= arraysize[ i ];
                    }
                    catch ( NumberFormatException e ) {
                        throw new VOTableFormatException( e );
                    }
                    if ( arraysize[ i ] <= 0 ) {
                        throw new VOTableFormatException(
                           "Negative dimensions illegal: " + as );
                    }
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
                Values vals = (Values) child;
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
     * Returns the 'null' value for this Field.
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
    public Values getLegalValues() {
        return legalValues;
    }

    /**
     * Returns a VALUES child of this element with the attribute
     * type='actual', or <tt>null</tt> if none exists.
     *
     * @return  the 'actual' Values object
     */
    public Values getActualValues() {
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
