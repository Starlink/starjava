package uk.ac.starlink.votable;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.ValueInfo;

/**
 * Handles writing an Object to a VOTable element.
 * Obtain a concrete instance of one of this abstract class's subclasses
 * using the static {@link #getEncoder} method.
 *
 * @author  Mark Taylor (Starlink)
 */
class Encoder {

    private ValueInfo info;

    /**
     * Private sole constructor - initialises an Encoder from a ValueInfo
     * object.
     *
     * @param   info  the ValueInfo representing the kinds of object
     *          this will have to encode
     */
    private Encoder( ValueInfo info ) {
        this.info = info;
    }

    /**
     * Returns a text string which represents a given value of the type
     * encoded by this object.  The text is suitable for use as the
     * content of a VOTable CDATA element, except that no escaping of
     * XML special characters has been done.
     *
     * @param  value  an object of the type handled by this encoder
     * @return  text representing the value suitable for inclusion in
     *          an XML attribute value or CDATA element
     */
    public String encodeAsText( Object value ) {

        /* You might expect the encoding to be done using this object's
         * knowledge about the objects it is supposed to encode.  
         * However, that would't really gain you much, so it just does it
         * based on the object value itself. */
        if ( value == null ) {
            return "";
        }
        else if ( value.getClass().getComponentType() == null ) {
            return formatValue( value );
        }
        else {
            int nel = Array.getLength( value );
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < nel; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ' ' );
                }
                sbuf.append( formatValue( Array.get( value, i ) ) );
            }
            return sbuf.toString();
        }
    }

    /**
     * Returns a map of key, value pairs representing a set of XML
     * attributes describing the objects encoded by this object.
     * The attributes can be used to qualify a FIELD or PARAM 
     * element in a VOTable document.
     *
     * @return  a map of attribute name, attribute value pairs applying
     *          to this encoder
     */
    public Map getFieldAttributes() {
        Map atts = new HashMap();

        /* Name attribute. */
        String name = info.getName();
        if ( name != null ) {
            atts.put( "name", name.trim() );
        }

        /* Unit attribute. */
        String units = info.getUnitString();
        if ( units != null ) {
            atts.put( "unit", units );
        }

        /* UCD attribute. */
        String ucd = info.getUCD();
        if ( ucd != null ) {
            atts.put( "ucd", ucd );
        }

        /* Get the class of objects to be encoded. */
        Class cls = info.getContentClass();
        if ( cls == null ) {
            cls = Object.class;
        }

        /* Get the datatype. */
        String datatype;
        boolean stringLike = false;
        if ( cls.equals( Byte.class ) || cls.equals( byte[].class ) ) {
            datatype = "short";  // Java byte type is signed, so use short here
        }
        else if ( cls.equals( Short.class ) || cls.equals( short[].class ) ) {
            datatype = "short";
        }
        else if ( cls.equals( Integer.class ) || cls.equals( int[].class ) ) {
            datatype = "int";
        }
        else if ( cls.equals( Long.class ) || cls.equals( long[].class ) ) {
            datatype = "long";
        }
        else if ( cls.equals( Float.class ) || cls.equals( float[].class ) ) {
            datatype = "float";
        }
        else if ( cls.equals( Double.class ) || cls.equals( double[].class ) ) {
            datatype = "double";
        }
        else if ( cls.equals( Boolean.class ) || 
                  cls.equals( boolean[].class ) ) {
            datatype = "boolean";
        }
        else if ( cls.equals( String.class ) ) { 
            datatype = "char";  // shoud be unicodeChar really?
            stringLike = true;
        }
        else if ( cls.equals( char[].class ) ) {
            datatype = "char";
            stringLike = true;
        }
        else if ( cls.equals( Character.class ) ) {
            datatype = "char";
        }
        else {
            datatype = "char";  // generic
            stringLike = true;
        }
        atts.put( "datatype", datatype );

        /* If we have an array type, work out what shape it will be. */
        int[] shape = null;
        if ( cls.getComponentType() != null ) {
            shape = info.getShape();
        }

        /* If we have a string-like type, give it an extra variable-sized
         * dimension, since in VOTable strings have to be represented as
         * arrays of characters. */
        if ( stringLike ) {
            if ( shape == null ) {
                shape = new int[] { -1 };
            }
            else {
                int[] s2 = shape; 
                int ndim = s2.length;
                shape = new int[ ndim + 1 ];
                System.arraycopy( s2, 0, shape, 0, ndim );
                shape[ ndim ] = -1;
            }
        }

        /* If necessary, set the arraysize attribute. */
        if ( shape != null ) {
            StringBuffer sbuf = new StringBuffer();
            if ( shape != null ) {
                for ( int i = 0; i < shape.length; i++ ) {
                    boolean last = ( i == ( shape.length - 1 ) );
                    int dim = shape[ i ];
                    if ( i < shape.length - 1 ) {
                        sbuf.append( dim )
                            .append( 'x' );
                    }
                    else {
                        if ( dim < 0 ) {
                            sbuf.append( '*' );
                        }
                        else {
                            sbuf.append( dim );
                        }
                    }
                }
            }
            atts.put( "arraysize", sbuf.toString() );
        }

        /* Return the attribute map. */
        return atts;
    }

    /**
     * Returns any text which should go inside the FIELD (or PARAM) element
     * corresponding to this Encoder.  Such text may contain XML markup,
     * so should not be further escaped.
     *
     * @return  a string containing XML text for inside the FIELD element -
     *          may be empty but will not be <tt>null</tt>
     */
    public String getFieldContent() {
        String desc = info.getDescription();
        if ( desc != null ) {
            desc = desc.trim();
            if ( desc.length() > 0 ) {
                return new StringBuffer()
                    .append( "<DESCRIPTION>" )
                    .append( VOTableWriter.formatText( desc ) )
                    .append( "</DESCRIPTION>" )
                    .toString();
            }
        }
        return "";
    }

    /**
     * Returns an Encoder suitable for encoding values described by a
     * given ValueInfo object.
     *
     * @param   info  a description of the type of value which needs to
     *          be encoded
     * @return  an encoder object which can do it
     */
    public static Encoder getEncoder( ValueInfo info ) {

        /* This just invokes the constructor.  You can imagine implementations
         * which return objects of different Encoder subclass types
         * based on the info argument though, so the public interface is
         * like this in case the implementation changes in the future. */
        return new Encoder( info );
    }

    /**
     * Turns a single object into a string for text encoding.
     */
    private String formatValue( Object value ) {
        if ( value instanceof Boolean ) {
            return ((Boolean) value).booleanValue() ? "T" : "F";
        }
        else {
            return value.toString();
        }
    }
}
