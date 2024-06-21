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

    private final boolean strict_;
    private final static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.votable" );
    final static long ASSUMED_ARRAYSIZE = -2L;

    /**
     * Constructs a FieldElement from a DOM element.
     *
     * @param  base  FIELD element
     * @param  doc   owner document for new element
     */
    FieldElement( Element base, VODocument doc ) {
        super( base, doc );
        strict_ = doc.isStrict(); 
    }

    /**
     * Returns the array size.  The returned value is an array of
     * <code>long</code>, with one element for each dimension.
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
                    if ( dim < 0 ) {
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
     * This has nothing to do with the java <code>null</code> value.
     *
     * @return  the bad ("null") value or, confusingly, <code>null</code> if
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
     * Returns the value of the <code>datatype</code> attribute.
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
            logger_.info( "Missing datatype attribute for " + getHandle() +
                          " - assume char(*)" );
            return "char";
        }
    }

    /**
     * Returns the value of the <code>unit</code> attribute,
     * or <code>null</code> if there is none.
     *
     * @return  the unit string
     */
    public String getUnit() {
        return hasAttribute( "unit" ) ? getAttribute( "unit" ) : null;
    }

    /**
     * Returns the value of the <code>ucd</code> attribute,
     * or <code>null</code> if there is none.
     *
     * @return  the ucd string
     * @see     uk.ac.starlink.table.UCD
     */
    public String getUcd() {
        return hasAttribute( "ucd" ) ? getAttribute( "ucd" ) : null;
    }

    /**
     * Returns the value of the <code>utype</code> attribute,
     * or <code>null</code> if there is none.
     *
     * @return  the utype string
     */
    public String getUtype() {
        return hasAttribute( "utype" ) ? getAttribute( "utype" ) : null;
    }

    /**
     * Returns the value of the <code>xtype</code> attribute,
     * or <code>null</code> if there is none.
     *
     * @return  the xtype string
     */
    public String getXtype() {
        return hasAttribute( "xtype" ) ? getAttribute( "xtype" ) : null;
    }

    /**
     * Returns the index of this field in a given table; that is the
     * index of the column it represents.  The first FIELD child of a
     * TABLE element has index 0, and so on.
     * If this field is not associated with <code>table</code>, -1 is returned.
     * 
     * @param   table  table within which to locate this field
     * @return  0-based index of this field in <code>table</code>, or -1
     */
    public int getIndexInTable( TableElement table ) {
        FieldElement[] fields = table.getFields();
        for ( int i = 0; i < fields.length; i++ ) {
            if ( fields[ i ] == this ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a VALUES child of this element with the attribute
     * type='legal', or <code>null</code> if none exists.
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
     * type='actual', or <code>null</code> if none exists.
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

    /**
     * Returns the COOSYS element corresponding to this field, if any.
     *
     * @return   referenced element with tagname COOSYS, or null
     */
    public VOElement getCoosys() {
        return getReferencedElement( "ref", "COOSYS" );
    }

    /**
     * Returns the TIMESYS element corresponding to this field, if any.
     *
     * @return   referenced element with tagname TIMESYS, or null
     */
    public TimesysElement getTimesys() {
        VOElement el = getReferencedElement( "ref", "TIMESYS" );
        return el instanceof TimesysElement ? (TimesysElement) el : null;
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

        /* Get information we need to create a decoder for this field. */
        String datatype = getDatatype();
        long[] arraysize = getArraysize();
        String nul = getNull();

        /* Doctor it.  This is to work around the fact that many FIELD
         * elements in practice omit an "arraysize='*'" attribute for
         * character datatypes.  According to a strict reading of the
         * standard this means each data cell contains a single character.
         * What is meant is almost always an N-character string though. */
        if ( ( "char".equals( datatype ) || 
               "unicodeChar".equals( datatype ) ) &&
             arraysize.length == 0 ) {
            if ( strict_ ) {
                logger_.info( getHandle()
                            + " - unspecified arraysize implies single"
                            + " character for "
                            + datatype + " datatype (strict)" );
            }
            else {
                arraysize = new long[] { ASSUMED_ARRAYSIZE };
                logger_.warning( getHandle()
                               + " - assuming unspecified arraysize='*' for "
                               + datatype + " datatype (non-strict)" );
            }
        }

        /* Create a decoder. */
        return Decoder.makeDecoder( datatype, arraysize, nul );
    }
}
