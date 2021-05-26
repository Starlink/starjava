package uk.ac.starlink.ttools.votlint;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object which knows how to interpret the values associated with a
 * FIELD or PARAM object.  This interpretation takes the form only of
 * checking whether it is encoded legally, writing some message to the
 * context if it is not, and throwing away the result
 * (if it was ever calculated in the first place).
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public abstract class ValueParser {

    private VotLintContext context_;
    private static final Pattern DOUBLE_REGEX =
        Pattern.compile( "([+-])?"
                       + "[0-9]*([0-9]|[0-9]\\.|\\.[0-9])[0-9]*"
                       + "([Ee][+-]?[0-9]{1,3})?" );

    /**
     * Checks the value of a string which contains the value. 
     * This is presumably either the content of the <tt>value</tt> attribute
     * of a PARAM element or the contents of a TD element.
     *
     * @param  text  value string
     */
    public abstract void checkString( String text );

    /**
     * Checks the value of a table element which is encoded in a BINARY
     * stream.
     *
     * @param  in  input stream
     */
    public abstract void checkStream( InputStream in ) throws IOException;

    /**
     * Returns the class of values which would be used in Java to represent
     * an object parsed by this parser, although this class does not 
     * actually return such values.  This should be the same class
     * that {@link uk.ac.starlink.table.ValueInfo#getContentClass}
     * would return for this object if a StarTable was being built.
     *
     * @return  value class
     */
    public abstract Class<?> getContentClass();

    /**
     * Returns the number of items of class {@link #getContentClass} which
     * correspond to values parsed by this parser.  This should be the
     * same as the product of shape elements returned by
     * {@link uk.ac.starlink.table.ValueInfo#getShape()}.
     * If the number is indeterminate, -1 should be returned.
     *
     * @return  number of elements per value
     */
    public abstract int getElementCount();

    /**
     * Sets this parser's context.
     * This method should be called shortly after construction.
     *
     * @param  context  lint context
     */
    public void setContext( VotLintContext context ) {
        context_ = context;
    }

    /**
     * Returns this parser's context.
     *
     * @return   lint context
     */
    public VotLintContext getContext() {
        return context_;
    }

    /**
     * Writes an info mesage to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     */
    public void info( VotLintCode code, String msg ) {
        getContext().info( code, msg );
    }

    /**
     * Writes a warning mesage to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     */
    public void warning( VotLintCode code, String msg ) {
        getContext().warning( code, msg );
    }

    /**
     * Writes an error mesage to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     */
    public void error( VotLintCode code, String msg ) {
        getContext().error( code, msg );
    }

    /**
     * Constructs a ValueParsers for a given element.
     *
     * @param   handler  element handler
     * @param   datatype  datatype attribute value
     * @param   arraysize  arraysize attribute value
     * @return   a suitable ValueParser, or <tt>null</tt> if one can't
     *           be constructed
     */
    public static ValueParser makeParser( ElementHandler handler, 
                                          String datatype, String arraysize ) {

        /* If no datatype has been specified, we can't do much. */
        if ( datatype == null || datatype.trim().length() == 0 ) {
            handler.error( new VotLintCode( "DT0" ),
                           "No datatype specified for " + handler + 
                           " Can't parse values" );
            return null;
        }

        /* Work out the array shape. */
        int[] shape;
        if ( arraysize == null || arraysize.trim().length() == 0 ) {
            shape = new int[] { 1 };
            if ( "char".equals( datatype ) ||
                 "unicodeChar".equals( datatype ) ) {
                handler.info( new VotLintCode( "AR1" ),
                              "No arraysize for character, " + handler +
                              " implies single character" );
            }
        }
        else {
            String[] dims = arraysize.split( "x" );
            shape = new int[ dims.length ];
            for ( int i = 0; i < dims.length; i++ ) {
                if ( i == dims.length - 1 && dims[ i ].endsWith( "*" ) ) {
                    String num = dims[ i ]
                                .substring( 0, dims[ i ].length() - 1 );
                    if ( num.length() > 0 ) {
                        try {
                            Integer.parseInt( num );
                        }
                        catch ( NumberFormatException e ) {
                            handler.error( new VotLintCode( "ARB" ),
                                           "Bad arraysize value '" +
                                           arraysize + "'" );
                        }
                    }
                    shape[ i ] = -1;
                }
                else {
                    try {
                        shape[ i ] = Integer.parseInt( dims[ i ] );
                    }
                    catch ( NumberFormatException e ) {
                        handler.error( new VotLintCode( "ARB" ),
                                       "Bad arraysize value '" +
                                       arraysize + "'" );
                        return null;
                    }
                    if ( shape[ i ] < 0 ) {
                        handler.error( new VotLintCode( "DMN" ),
                                       "Negative dimensions element " +
                                       shape[ i ] );
                        return null;
                    }
                }
            }
        }

        /* Calculate the total number of elements. */
        int nel = 1;
        for ( int i = 0; i < shape.length; i++ ) {
            nel *= shape[ i ];
        }

        /* Return a suitable parser. */
        if ( "char".equals( datatype ) || 
             "unicodeChar".equals( datatype ) ) {
            boolean ascii = "char".equals( datatype );
            int stringLeng = shape[ 0 ];
            if ( nel == 1 ) {
                return new SingleCharParser( ascii );
            }
            else if ( shape.length == 1 ) {
                if ( stringLeng < 0 ) {
                    return new VariableCharParser( ascii );
                }
                else {
                    return new FixedCharParser( ascii, stringLeng );
                }
            }
            else {
                if ( nel < 0 ) {
                    return new VariableCharArrayParser( ascii );
                }
                else {
                    return new FixedCharArrayParser( ascii, nel, stringLeng );
                }
            }
        }
        else if ( "bit".equals( datatype ) ) {
            if ( nel < 0 ) {
                return new VariableBitParser();
            }
            else {
                return new FixedBitParser( nel );
            }
        }
        else if ( "floatComplex".equals( datatype ) ) {
            if ( nel < 0 ) {
                return new VariableArrayParser( new FloatParser(),
                                                float[].class );
            }
            else {
                return new FixedArrayParser( new FloatParser(), 
                                             float[].class, nel * 2 );
            }
        }
        else if ( "doubleComplex".equals( datatype ) ) {
            if ( nel < 0 ) {
                return new VariableArrayParser( new DoubleParser(),
                                                double[].class );
            }
            else {
                return new FixedArrayParser( new DoubleParser(), 
                                             double[].class, nel * 2 );
            }
        }
        else {
            if ( nel == 1 ) {
                return makeScalarParser( datatype, handler );
            }
            else {
                ValueParser base = makeScalarParser( datatype, handler );
                if ( base == null ) {
                    return null;
                }
                else {
                    Class<?> clazz = getArrayClass( base.getContentClass() );
                    return nel < 0 
                       ? (ValueParser) new VariableArrayParser( base, clazz )
                       : (ValueParser) new FixedArrayParser( base, clazz, nel );
                }
            }
        }
    }

    /**
     * Constructs a parser for a single value.
     *
     * @param   datatype  value of datatype attribute
     * @param   handler   element 
     */
    private static ValueParser makeScalarParser( String datatype,
                                                 ElementHandler handler ) {
        if ( "boolean".equals( datatype ) ) {
            return new BooleanParser();
        }
        else if ( "unsignedByte".equals( datatype ) ) {
            return new IntegerParser( 1, 0, 255, Short.class );
        }
        else if ( "short".equals( datatype ) ) {
            return new IntegerParser( 2, Short.MIN_VALUE, Short.MAX_VALUE,
                                      Short.class );
        }
        else if ( "int".equals( datatype ) ) {
            return new IntegerParser( 4, Integer.MIN_VALUE, Integer.MAX_VALUE,
                                      Integer.class );
        }
        else if ( "long".equals( datatype ) ) {
            return new IntegerParser( 8, Long.MIN_VALUE, Long.MAX_VALUE,
                                      Long.class );
        }
        else if ( "float".equals( datatype ) ) {
            return new FloatParser();
        }
        else if ( "double".equals( datatype ) ) {
            return new DoubleParser();
        }
        else {
            handler.error( new VotLintCode( "DTX" ),
                           "Unknown datatype '" + datatype + "'" + 
                           " - can't parse column" );
            return null;
        }
    }

    /**
     * Abstract parser superclass which just keeps track of parser
     * class and count.
     */
    private static abstract class AbstractParser extends ValueParser {
        private final Class<?> clazz_;
        private final int count_;

        /**
         * Constructor.
         *
         * @param   clazz  element class
         * @param   count  element count
         */
        public AbstractParser( Class<?> clazz, int count ) {
            clazz_ = clazz;
            count_ = count;
        }
        public Class<?> getContentClass() {
            return clazz_;
        }
        public int getElementCount() {
            return count_;
        }
    }

    /**
     * Parser whose stream reading method uncritically reads in a 
     * fixed number of bytes.
     */
    private static abstract class SlurpParser extends ValueParser {
        private final int nbyte_;
        private final Class<?> clazz_;
        private final int count_;

        /**
         * Constructor.
         *
         * @param  nbyte  number of bytes read by stream reading method.
         * @param  clazz  class of elements
         * @param  count  number of elements
         */
        SlurpParser( int nbyte, Class<?> clazz, int count ) {
            nbyte_ = nbyte;
            clazz_ = clazz;
            count_ = count;
        }
        public Class<?> getContentClass() {
            return clazz_;
        }
        public int getElementCount() {
            return count_;
        }
        public void checkStream( InputStream in ) throws IOException {
            slurpStream( in, nbyte_ );
        }
    }


    /**
     * Parser which reads a fixed number of scalar elements.
     */
    private static class FixedArrayParser extends AbstractParser {
        final ValueParser base_;
        final int count_;

        /**
         * Constructor.
         *
         * @param   base  parser which can read a scalar
         * @param   count  number of scalar elements read by this parser
         */
        FixedArrayParser( ValueParser base, Class<?> clazz, int count ) {
            super( clazz, count );
            base_ = base;
            count_ = count;
            base_.toString();
        }
        public VotLintContext getContext() {
            return base_.getContext();
        }
        public void setContext( VotLintContext context ) {
            base_.setContext( context );
        }
        public void checkString( String text ) {
            StringTokenizer stok = new StringTokenizer( text );
            int ntok = stok.countTokens();
            if ( ntok != count_ ) {
                error( new VotLintCode( "E09" ),
                       "Wrong number of elements in array (" + 
                       ntok + " found, " + count_ + " expected)" );
            }
            while ( stok.hasMoreTokens() ) {
                base_.checkString( stok.nextToken() );
            }
        }
        public void checkStream( InputStream in ) throws IOException {
            for ( int i = 0; i < count_; i++ ) {
                base_.checkStream( in );
            }
        }
    }

    /**
     * Parser which reads a variable number of scalar elements.
     */
    private static class VariableArrayParser extends AbstractParser {
        final ValueParser base_;

        /**
         * Constructor.
         *
         * @param  base  parser which can read a scalar
         */
        VariableArrayParser( ValueParser base, Class<?> clazz ) {
            super( clazz, -1 );
            base_ = base;
            base_.toString();
        }
        public VotLintContext getContext() {
            return base_.getContext();
        }
        public void setContext( VotLintContext context ) {
            base_.setContext( context );
        }
        public void checkString( String text ) {
            for ( StringTokenizer stok = new StringTokenizer( text );
                  stok.hasMoreTokens(); ) {
                base_.checkString( stok.nextToken() );
            }
        }
        public void checkStream( InputStream in ) throws IOException {
            int count = readCount( in );
            for ( int i = 0; i < count; i++ ) {
                try {
                    base_.checkStream( in );
                }
                catch ( EOFException e ) {
                    error( new VotLintCode( "EOF" ),
                           "End of stream while reading " + count +
                           " elements (probable stream corruption)" );
                    throw e;
                }
            }
        }
    }

    /**
     * Parser for boolean scalars.
     */
    private static class BooleanParser extends AbstractParser {

        public BooleanParser() {
            super( Boolean.class, 1 );
        }

        public void checkString( String text ) {
            int leng = text.length();
            if ( leng == 0 ) {
                return;
            }
            else if ( leng == 1 ) {
                switch ( text.charAt( 0 ) ) {
                    case 'T': case 't': case '1':
                    case 'F': case 'f': case '0':
                    case ' ': case '?': case '\0':
                        return;
                    default:
                        error( new VotLintCode( "TFX" ),
                               "Bad boolean value '" + text + "'" );
                }
            }
            else if ( text.equalsIgnoreCase( "true" ) ||
                      text.equalsIgnoreCase( "false" ) ) {
                return;
            }
            else {
                error( new VotLintCode( "TFX" ),
                       "Bad boolean value '" + text + "'" );
            }
        }

        public void checkStream( InputStream in ) throws IOException {
            char chr = (char) ( 0xffff & in.read() );
            switch ( chr ) {
                case 'T': case 't': case '1':
                case 'F': case 'f': case '0':
                case ' ': case '?': case '\0':
                    return;
                case (char) -1:
                    error( new VotLintCode( "EOF" ),
                           "End of stream during read" );
                    throw new EOFException();
                default:
                    error( new VotLintCode( "TFX" ),
                           "Bad boolean value '" + chr + "'" );
             }
        }
    }

    /**
     * Parser for integer values.
     */
    private static class IntegerParser extends SlurpParser {
        final long minVal_;
        final long maxVal_;

        /**
         * Constructor.
         *
         * @param   nbyte  number of bytes per integer in stream mode
         * @param   minVal  minimum legal value for integer
         * @param   maxVal  maximum legal value for integer
         */
        IntegerParser( int nbyte, long minVal, long maxVal, Class<?> clazz ) {
            super( nbyte, clazz, 1 );
            minVal_ = minVal;
            maxVal_ = maxVal;
        }

        public void checkString( String text ) {
            int pos = 0;
            int leng = text.length();
            while ( pos < leng && text.charAt( pos ) == ' ' ) {
                pos++;
            }
            long value;
            if ( leng - pos > 1 &&
                 text.charAt( pos + 1 ) == 'x' &&
                 text.charAt( pos ) == '0' ) {
                try {
                    value = Long.parseLong( text.substring( pos + 2 ), 16 );
                }
                catch ( NumberFormatException e ) {
                    error( new VotLintCode( "HX0" ),
                           "Bad hexadecimal string '" + text + "'" );
                    return;
                }
            }
            else if ( text.length() == 0 ) {
                if ( ! getContext().getVersion().allowEmptyTd() ) {
                    error( new VotLintCode( "ETD" ),
                           "Empty cell illegal for integer value" );
                }
                return;
            }
            else {
                try {
                    value = Long.parseLong( text );
                }
                catch ( NumberFormatException e ) {
                    error( new VotLintCode( "IT0" ),
                           "Bad integer string '" + text + "'" );
                    return;
                }
            }
            if ( value < minVal_ || value > maxVal_ ) {
                error( new VotLintCode( "BND" ),
                       "Value " + text + " outside type range " + 
                       minVal_ + "..." + maxVal_ );
            }
        }
    }

    /**
     * Parser for float values.
     */
    private static class FloatParser extends SlurpParser {
        FloatParser() {
            super( 4, Float.class, 1 );
        }
        public void checkString( String text ) {
            text = text.trim();
            if ( "NaN".equals( text ) ||
                 "+Inf".equals( text ) ||
                 "-Inf".equals( text ) ||
                 text.length() == 0 ) {
                return;
            }
            else {
                Matcher matcher = DOUBLE_REGEX.matcher( text );
                if ( ! matcher.matches() ) {
                    error( new VotLintCode( "FP0" ),
                           "Bad float string '" + text + "'" );
                }
            }
        }
    }

    /**
     * Parser for double values.
     */
    private static class DoubleParser extends SlurpParser {
        DoubleParser() {
            super( 8, Double.class, 1 );
        }
        public void checkString( String text ) {
            text = text.trim();
            if ( "NaN".equals( text ) ||
                 "+Inf".equals( text ) ||
                 "-Inf".equals( text ) ||
                 text.length() == 0 ) {
                return;
            }
            else {
                Matcher matcher = DOUBLE_REGEX.matcher( text );
                if ( ! matcher.matches() ) {
                    error( new VotLintCode( "FP0" ),
                           "Bad double string '" + text + "'" );
                }
            }
        }
    }

    /**
     * Parser for fixed length bit vectors.
     */
    private static class FixedBitParser extends SlurpParser {
        final int count_;

        /**
         * Constructor.
         *
         * @param  count  number of bits
         */
        FixedBitParser( int count ) {
            super( ( count + 7 ) / 8, Boolean.class, count );
            count_ = count;
        }
        public void checkString( String text ) {
            int leng = text.length();
            int nbit = 0;
            for ( int i = 0; i < leng; i++ ) {
                switch ( text.charAt( i ) ) {
                    case '0': case '1':
                        nbit++;
                        break;
                    case ' ': case '\n':
                        break;
                    default:
                        error( new VotLintCode( "BT0" ),
                               "Bad value for bit vector " + text );
                        return;
                }
            }
            if ( nbit != count_ ) {
                error( new VotLintCode( "CT9" ),
                       "Wrong number of elements in array (" +
                       nbit + " found, " + count_ + " expected)" );
            }
        }
    }

    /**
     * Parser for variable length bit vectors.
     */
    private static class VariableBitParser extends AbstractParser {
        public VariableBitParser() {
            super( boolean[].class, -1 );
        }
        public void checkString( String text ) {
            int leng = text.length();
            for ( int i = 0; i < leng; i++ ) {
                switch ( text.charAt( i ) ) {
                    case '0': case '1':
                    case ' ': case '\n':
                        break;
                    default:
                        error( new VotLintCode( "BV0" ),
                               "Bad value for bit vector " + text );
                        return;
                }
            }
        }
        public void checkStream( InputStream in ) throws IOException {
            slurpStream( in, ( 1 + readCount( in  ) ) / 8 );
        }
    }

    /**
     * Parser for single characters.
     */
    private static class SingleCharParser extends SlurpParser {
        private final boolean ascii_;

        /**
         * Constructor.
         *
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         */
        public SingleCharParser( boolean ascii ) {
            super( ascii ? 1 : 2, Character.class, 1 );
            ascii_ = ascii;
        }
        public void checkString( String text ) {
            int leng = text.length();
            switch ( leng ) {
                case 0:
                    warning( new VotLintCode( "CH0" ),
                             "Empty character value is questionable" );
                    break;
                case 1:
                    break;
                default:
                    warning( new VotLintCode( "CH1" ),
                             "Characters after first in char scalar ignored" +
                             " (missing arraysize?)" );
            }
            if ( ascii_ && leng > 0 && text.charAt( 0 ) > 0x7f ) {
                error( new VotLintCode( "CRU" ),
                       "Non-ascii character in 'char' data" );
            }
        }
    }

    /**
     * Parser for fixed-length character arrays.
     */
    private static class FixedCharParser extends SlurpParser {

        /**
         * Constructor.
         *
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         * @param  count  number of characters in string
         */
        public FixedCharParser( boolean ascii, int count ) {
            super( count * ( ascii ? 1 : 2 ), String.class, 1 );
        }
        public void checkString( String text ) {
        }
    }

    /**
     * Parser for variable-length character arrays.
     */
    private static class VariableCharParser extends AbstractParser {
        final boolean ascii_;

        /**
         * Constructor.
         *
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         */
        VariableCharParser( boolean ascii ) {
            super( String.class, 1 );
            ascii_ = ascii;
        }
        public void checkString( String text ) {
        }
        public void checkStream( InputStream in ) throws IOException {
            slurpStream( in, readCount( in ) * ( ascii_ ? 1 : 2 ) );
        }
    }

    /**
     * Parser for variable-length multi-dimensional character arrays.
     */
    private static class VariableCharArrayParser extends AbstractParser {
        final boolean ascii_;
 
        /**
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         */
        VariableCharArrayParser( boolean ascii ) {
            super( String[].class, -1 );
            ascii_ = ascii;
        }

        public void checkStream( InputStream in ) throws IOException {
            slurpStream( in, readCount( in ) * ( ascii_ ? 1 : 2 ) );
        }

        public void checkString( String text ) {
        }
    }

    /**
     * Parser for fixed-length multi-dimensional character arrays.
     */
    private static class FixedCharArrayParser extends AbstractParser {
        final boolean ascii_;
        final int nchar_;

        /**
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         * @param  nchar  number of characters
         * @param  stringLeng  characters per string
         */
        FixedCharArrayParser( boolean ascii, int nchar, int stringLeng ) {
            super( String[].class, nchar / stringLeng );
            ascii_ = ascii;
            nchar_ = nchar;
        }

        public void checkStream( InputStream in ) throws IOException {
            slurpStream( in, nchar_ * ( ascii_ ? 1 : 2 ) );
        }

        public void checkString( String text ) {
            int leng = text.length();
            if ( text.length() != nchar_ ) {
                warning( new VotLintCode( "C09" ),
                         "Wrong number of characters in string (" +
                         leng + " found, " + nchar_ + " expected)" );
            }
        }
    }

    /**
     * Uncritically reads in a fixed number of bytes from a stream.
     *
     * @param  in  input stream
     * @param  nbyte  number of bytes to read
     */
    void slurpStream( InputStream in, int nbyte ) throws IOException {
        slurpStream( in, nbyte, getContext() );
    }

    /**
     * Uncritically reads in a fixed number of bytes from a stream.
     * An error is reported if the stream ends mid-read.
     *
     * @param  in  input stream
     * @param  nbyte  number of bytes to read
     * @param  context  error reporting context
     */
    public static void slurpStream( InputStream in, int nbyte,
                                    VotLintContext context )
            throws IOException {
        for ( int i = 0; i < nbyte; i++ ) {
            if ( in.read() < 0 ) {
                context.error( new VotLintCode( "EOF" ),
                               "Stream ended during data read; done "
                             + i + "/" + nbyte );
                throw new EOFException();
            }
        }
    }

    /**
     * Reads an integer from a stream.  This is used to read the number of
     * elements of a variable-length array in the stream which follows.
     *
     * @param  in  input stream
     * @return  integer value
     */
    int readCount( InputStream in ) throws IOException {
        int c1 = in.read();
        int c2 = in.read();
        int c3 = in.read();
        int c4 = in.read();
        if ( c1 < 0 || c2 < 0 || c3 < 0 || c4 < 0 ) {
            error( new VotLintCode( "EOF" ),
                   "End of stream while reading element count" +
                   " (probable stream corruption)" );
            throw new EOFException();
        }
        else {
            int count = ( ( c1 & 0xff ) << 24 )
                      | ( ( c2 & 0xff ) << 16 )
                      | ( ( c3 & 0xff ) <<  8 )
                      | ( ( c4 & 0xff ) <<  0 );
            if ( count < 0 ) {
                error( new VotLintCode( "MEL" ),
                       "Apparent negative element count " +
                       "(probably stream corruption)" );
                throw new IOException( "Unrecoverable stream error" );
            }
            else {
                return count;
            }
        }
    }

    /**
     * Returns the array class corresponding to a wrapper class.
     *
     * @param   wclazz  wrapper class
     * @return  corresponding primitive array class
     */
    private static Class<?> getArrayClass( Class<?> wclazz ) {
        if ( wclazz == Boolean.class ) {
            return boolean[].class;
        }
        else if ( wclazz == Character.class ) {
            return char[].class;
        }
        else if ( wclazz == Byte.class ) {
            return byte[].class;
        }
        else if ( wclazz == Short.class ) {
            return short[].class;
        }
        else if ( wclazz == Integer.class ) {
            return int[].class;
        }
        else if ( wclazz == Long.class ) {
            return long[].class;
        }
        else if ( wclazz == Float.class ) {
            return float[].class;
        }
        else if ( wclazz == Double.class ) {
            return double[].class;
        }
        else {
            assert false;
            return Array.newInstance( wclazz, 0 ).getClass();
        }
    }
       
}
