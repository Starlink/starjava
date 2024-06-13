package uk.ac.starlink.ttools.votlint;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.ttools.func.Times;

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
    private static final Pattern ISO_REGEX =
        Pattern.compile( "[0-9]{4}-[01][0-9]-[0-3][0-9]"
                       + "(T[0-2][0-9]:[0-5][0-9]:[0-6][0-9]"
                       + "([.][0-9]+)?)?Z?" );

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
     * Writes an info message to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     */
    public void info( VotLintCode code, String msg ) {
        getContext().info( code, msg );
    }

    /**
     * Writes a warning message to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     */
    public void warning( VotLintCode code, String msg ) {
        getContext().warning( code, msg );
    }

    /**
     * Writes an error message to the user.
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
     * @param   xtype   xtype (extended type) attribute value
     * @return   a suitable ValueParser, or <tt>null</tt> if one can't
     *           be constructed
     */
    public static ValueParser makeParser( ElementHandler handler, 
                                          String datatype, String arraysize,
                                          String xtype ) {

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
        int nel = Arrays.stream( shape ).reduce( 1, (a, b) -> a * b );

        /* If we can make a parser based on the declared xtype of the field,
         * return that. */
        ValueParser xtypeParser =
            makeXtypeParser( handler.getContext(), xtype, datatype, shape );
        if ( xtypeParser != null ) {
            return xtypeParser;
        }

        /* Otherwise, return a suitable generic type-based parser. */
        else {
            if ( "char".equals( datatype ) || 
                 "unicodeChar".equals( datatype ) ) {
                boolean ascii = "char".equals( datatype );
                int stringLeng = shape[ 0 ];
                if ( nel == 1 ) {
                    return new SingleCharParser( ascii );
                }
                else if ( shape.length == 1 ) {
                    return stringLeng < 0
                         ? new VariableCharParser( ascii )
                         : new FixedCharParser( ascii, stringLeng );
                }
                else {
                    return nel < 0
                         ? new VariableCharArrayParser( ascii )
                         : new FixedCharArrayParser( ascii, nel, stringLeng );
                }
            }
            else if ( "bit".equals( datatype ) ) {
                return nel < 0 ? new VariableBitParser()
                               : new FixedBitParser( nel );
            }
            else if ( "floatComplex".equals( datatype ) ) {
                return nel < 0
                     ? new VariableArrayParser( new FloatParser(),
                                                float[].class )
                     : new FixedArrayParser( new FloatParser(), 
                                             float[].class, nel * 2 );
            }
            else if ( "doubleComplex".equals( datatype ) ) {
                return nel < 0
                     ? new VariableArrayParser( new DoubleParser(),
                                                double[].class )
                     : new FixedArrayParser( new DoubleParser(), 
                                             double[].class, nel * 2 );
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
                        Class<?> clazz =
                            getArrayClass( base.getContentClass() );
                        return nel < 0 
                           ? new VariableArrayParser( base, clazz )
                           : new FixedArrayParser( base, clazz, nel );
                    }
                }
            }
        }
    }

    /**
     * Attempts to construct a ValueParser based on an Xtype value.
     * If it can't be done, null is returned.
     * If the xtype is recognised but there's something wrong with the
     * metadata, suitable messages may additionally be written to the
     * context.
     *
     * @param  context  reporting context
     * @param  xtype    xtype attribute value
     * @param  datatype  datatype attribute value
     * @param  shape    parsed arraysize (final -1 element indicates unbounded)
     */
    private static ValueParser makeXtypeParser( VotLintContext context,
                                                String xtype, String datatype,
                                                int[] shape ) {
        if ( xtype == null || xtype.trim().length() == 0 ) {
            return null;
        }
                                         
        /* Consider xtype values with regard to rules in DALI 1.1 sec 3.3.
         * If the metadata are appropriate for a DALI-endorsed extended type,
         * return an appropriate parser, or if the xtype looks like DALI
         * but the other metadata doesn't, issue an error and continue. */
        boolean isFloating = "float".equals( datatype )
                          || "double".equals( datatype );
        boolean isNumeric = isFloating
                         || "short".equals( datatype )
                         || "int".equals( datatype )
                         || "long".equals( datatype );
        boolean isArray1d = shape.length == 1;
        int arraysize1d = isArray1d ? shape[ 0 ] : 0;
        int nel = Arrays.stream( shape ).reduce( 1, (a, b) -> a * b );

        /* Note DALI mostly requires explicitly datatype="char" without
         * allowing for "unicodeChar". */
        boolean isChars = isArray1d && "char".equals( datatype );

        if ( "timestamp".equals( xtype ) ) {
            if ( ! isChars ) {
                context.error( new VotLintCode( "XTS" ),
                               "xtype='timestamp' for non-string-type value" );
                return null;
            }
            else {
                return makeTimestampParser( arraysize1d );
            }
        }

        else if ( "interval".equals( xtype ) ) {
            if ( arraysize1d != 2 ) {
                context.error( new VotLintCode( "XI2" ),
                               "xtype='interval' for arraysize != 2" );
                return null;
            }
            else if ( !isNumeric ) {
                context.error( new VotLintCode( "XI9" ),
                               "xtype='interval' for non-numeric datatype" );
                return null;
            }
            else if ( isFloating ) {
                return makeFloatingIntervalParser( datatype );
            }
            else {
                // Integer intervals are permitted, but we don't have
                // a checker for them.  There's not much to check in any case,
                // beyond normal 2-element array constraints.
                return null;
            }
        }

        else if ( "point".equals( xtype ) ) {
            if ( arraysize1d != 2 ) {
                context.error( new VotLintCode( "XP2" ),
                               "xtype='point' for arraysize != 2" );
                return null;
            }
            else if ( !isFloating ) {
                context.error( new VotLintCode( "XP9" ),
                               "xtype='point' for non-floating datatype" );
                return null;
            }
            else {
                return makePointParser( datatype );
            }
        }

        else if ( "circle".equals( xtype ) ) {
            if ( arraysize1d != 3 ) {
                context.error( new VotLintCode( "XC3" ),
                               "xtype='circle' for arraysize != 3" );
                return null;
            }
            else if ( !isFloating ) {
                context.error( new VotLintCode( "XC9" ),
                               "xtype='circle' for non-floating datatype" );
                return null;
            }
            else {
                return makeCircleParser( datatype );
            }
        }

        else if ( "polygon".equals( xtype ) ) {
            if ( !isArray1d ) {
                context.error( new VotLintCode( "XSV" ),
                               "xtype='polygon' for non-vector arraysize" );
                return null;
            }
            else if ( !isFloating ) {
                context.error( new VotLintCode( "XS9" ),
                               "xtype='polygon' for non-floating datatype" );
                return null;
            }
            else {
                return makePolygonParser( datatype, arraysize1d );
            }
        }

        /* DALI sec 3.3: namespaced xtypes are explicitly allowed. */
        else if ( xtype.indexOf( ':' ) > 0 ) {
            context.info( new VotLintCode( "XNI" ),
                          "Namespaced non-DALI xtype value \"" + xtype + "\"" );
            return null;
        }

        /* Warn for non-standard non-namespaced xtypes. */
        else {
            context.warning( new VotLintCode( "XDL" ),
                             "Non-DALI 1.1 xtype value \"" + xtype + "\"" );
            return null;
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
     * Returns a parser for xtype='timestamp' (DALI 1.1 sec 3.3.3).
     *
     * @param  stringLeng   fixed element count for array values,
     *                      or negative value for variable element count
     * @return  new parser
     */
    private static ValueParser makeTimestampParser( int stringLeng ) {
        return makeStringParser( stringLeng, (context, txt) -> {
            if ( txt != null && txt.trim().length() > 0 ) {
                if ( ! ISO_REGEX.matcher( txt ).matches() ) {
                    context.error( new VotLintCode( "TSR" ),
                                   "Timestamp value \"" + txt + "\""
                                 + " does not match "
                                 + "YYYY-MM-DD['T'hh:mm:ss[.SSS]]['Z']" );
                }
                else {
                    // This looks like it should be a more rigorous check.
                    // Actually, it's not because the Times parsing
                    // currently uses a lenient GregorianCalendar.
                    // Maybe replace this by a non-lenient parser
                    // at some point.
                    try {
                        Times.isoToMjd( txt );
                    }
                    catch ( RuntimeException e ) {
                        context.error( new VotLintCode( "TSR" ),
                                       "Bad timestamp \"" + txt + "\" "
                                     + e.getMessage() );
                    }
                }
            }
        } );
    }

    /**
     * Returns a parser for xtype='interval' (DALI 1.1 sec 3.3.4).
     * This parser only handles floating point intervals,
     * though integer ones are also permitted.
     *
     * @param  datatype  datatype value
     * @return  new parser
     */
    private static ValueParser makeFloatingIntervalParser( String datatype ) {
        return makeFloatingArrayParser( datatype, 2, (context, values) -> {
            double d0 = values[ 0 ];
            double d1 = values[ 1 ];
            if ( Double.isNaN( d0 ) != Double.isNaN( d1 ) ) {
                context.error( new VotLintCode( "XIN" ),
                               "One but not both interval limit is NaN "
                             + Arrays.toString( values ) );
            }
        } );
    }

    /**
     * Returns a parser for xtype='point' (DALI 1.1 sec 3.3.5).
     *
     * @param  datatype  datatype value
     * @return  new parser
     */
    private static ValueParser makePointParser( String datatype ) {
        return makeFloatingArrayParser( datatype, 2, (context, values) -> {
            double d0 = values[ 0 ];
            double d1 = values[ 1 ];
            if ( Double.isNaN( d0 ) != Double.isNaN( d1 ) ) {
                context.error( new VotLintCode( "XIN" ),
                               "One but not both point coordinate is NaN "
                             + Arrays.toString( values ) );
            }
            else if ( Double.isInfinite( d0 ) || Double.isInfinite( d1 ) ) {
                context.error( new VotLintCode( "XIZ" ),
                               "Infinite point coordinate(s) "
                             + Arrays.toString( values ) );
            }
        } );
    }

    /**
     * Returns a parser for xtype='circle' (DALI 1.1 sec 3.3.6).
     *
     * @param  datatype  datatype value
     * @return  new parser
     */
    private static ValueParser makeCircleParser( String datatype ) {
        return makeFloatingArrayParser( datatype, 3, (context, values) -> {
            double c1 = values[ 0 ];
            double c2 = values[ 1 ];
            double r = values[ 2 ];
            if ( Double.isNaN( c1 ) != Double.isNaN( c2 ) ||
                 Double.isNaN( c1 ) != Double.isNaN( r ) ) {
                context.error( new VotLintCode( "XIN" ),
                               "Some but not all circle parameters are NaN "
                             + Arrays.toString( values ) );
            }
        } );
    }

    /**
     * Returns a parser for xtype='polygon' (DALI 1.1 sec 3.3.7).
     *
     * @param  datatype  datatype value
     * @param  nel     fixed element count of array value,
     *                 or negative for variable element count
     * @return  new parser
     */
    private static ValueParser makePolygonParser( String datatype, int nel ) {
        return makeFloatingArrayParser( datatype, nel, (context, values) -> {
            int ncoord = values.length;
            if ( nel >= 0 && ncoord != nel ) {
                context.error( new VotLintCode( "E09" ),
                               "Wrong number of elements in array ("
                             + ncoord + " found, " + nel + " expected)" );
            }
            else if ( ncoord % 2 != 0 ) {
                context.error( new VotLintCode( "XSO" ),
                       "Odd number of polygon coords (" + ncoord + ")");
            }
            else if ( ncoord > 0 && ncoord < 6 ) {
                context.error( new VotLintCode( "XSF" ),
                               "Too few polygon coords (" + ncoord + ")" );
            }
        } );
    }

    /**
     * Creates a generic parser that works with fixed or variable-length
     * 1-d char arrays.  This is not intended to work for unicodeChar
     * arrays (though in practice it might do).
     *
     * @param  stringLeng   fixed element count for array values,
     *                      or negative value for variable element count
     * @param  stringChecker  callback to perform checking on a String value
     * @return  new parser
     */
    private static ValueParser
            makeStringParser( int stringLeng,
                              BiConsumer<VotLintContext,String> stringChecker ){
        return new AbstractParser( String.class, 1 ) {
            public void checkString( String txt ) {
                stringChecker.accept( getContext(), txt );
            }
            public void checkStream( InputStream in ) throws IOException {
                int nchar = stringLeng >= 0 ? stringLeng : readCount( in );
                String txt = new String( readStreamBytes( in, nchar ),
                                         StandardCharsets.UTF_8 );
                stringChecker.accept( getContext(), txt );
            }
        };
    }

    /**
     * Creates a generic parser that works with fixed- or variable-length
     * arrays of floating point values.
     * Note that this currently only works with <code>datatype</code>
     * values of "<code>float</code>" or "<code>double</code>".
     *
     * @param  datatype  value of (scalar) datatype attribute;
     * @param  nel   fixed element count for array values,
     *               or negative value for variable element count
     * @param  arrayChecker  callback to perform checking on an array
     *                       of floating point values read in;
     *                       the array passed in is guaranteed to be
     *                       non-null and consistent with <code>nel</code>
     * @return   new parser
     */
    private static ValueParser
            makeFloatingArrayParser( String datatype, int nel,
                                     BiConsumer<VotLintContext,double[]>
                                                arrayChecker ) {
        final Class<?> aclazz;
        final int elSize;

        /* Set up readers.  As it happens the DataInput readFloat/readDouble
         * methods are correct for reading from VOTable BINARY streams. */
        final FloatReader floatReader;
        if ( "float".equals( datatype ) ) {
            aclazz = float[].class;
            elSize = 4;
            floatReader = DataInput::readFloat;
        }
        else if ( "double".equals( datatype ) ) {
            aclazz = double[].class;
            elSize = 8;
            floatReader = DataInput::readDouble;
        }
        else {
            throw new AssertionError( "datatype? " + datatype );
        }
        return new AbstractParser( aclazz, nel >= 0 ? nel : -1 ) {
            public void checkString( String text ) {
                double[] values = readString( text );
                if ( values != null ) {
                    VotLintContext context = getContext();
                    if ( nel >= 0 && values.length != nel ) {
                        context.error( new VotLintCode( "E08" ),
                                       "Wrong number of elements in array (" + 
                                     + values.length + " found, "
                                     + nel + " expected)" );
                    }
                    else {
                        arrayChecker.accept( context, values );
                    }
                }
            }
            public void checkStream( InputStream in ) throws IOException {
                double[] values = readStream( in );
                if ( values != null ) {
                    assert nel < 0 || values.length == nel;
                    arrayChecker.accept( getContext(), values );
                }
            }

            /**
             * Reads a floating point array from a string value.
             *
             * @param  text  string value of field
             * @return   content as double array, or null if not parseable
             */
            private double[] readString( String text ) {
                String[] sitems = text.trim().split( "\\s+" );
                int n = sitems.length;
                double[] ditems = new double[ n ];
                for ( int i = 0; i < n; i++ ) {
                    String sitem = sitems[ i ];
                    final double ditem;
                    if ( "NaN".equals( sitem ) ) {
                        ditem = Double.NaN;
                    }
                    else if ( "+Inf".equals( sitem ) ) {
                        ditem = Double.POSITIVE_INFINITY;
                    }
                    else if ( "-Inf".equals( sitem ) ) {
                        ditem = Double.NEGATIVE_INFINITY;
                    }
                    else {
                        Matcher matcher = DOUBLE_REGEX.matcher( sitem );
                        if ( matcher.matches() ) {
                            try {
                                ditem = Double.parseDouble( sitem );
                            }
                            catch ( NumberFormatException e ) {
                                // shouldn't happen
                                error( new VotLintCode( "FPX" ),
                                       "Unexpected bad " + datatype + " string"
                                     + " '" + sitem + "'" );
                                return null;
                            }
                        }
                        else {
                            error( new VotLintCode( "FP0" ),
                                   "Bad " + datatype + " string"
                                 + " '" + sitem + "'" );
                            return null;
                        }
                    }
                    ditems[ i ] = ditem;
                }
                return ditems;
            }

            /**
             * Reads a double array from a stream.
             *
             * @param  in  input stream
             * @return  floating point array of size defined by this parser
             */
            private double[] readStream( InputStream in ) throws IOException {
                int nitem = nel >= 0 ? nel : readCount( in );
                DataInputStream dataIn = new DataInputStream( in );
                double[] ditems = new double[ nitem ];
                for ( int i = 0; i < nitem; i++ ) {
                    ditems[ i ] = floatReader.readDouble( dataIn );
                }
                return ditems;
            }
        };
    }

    /**
     * Interface for extracting a double from a binary stream.
     */
    @FunctionalInterface
    private static interface FloatReader {

        /**
         * Reads a numeric value from a DataInput.
         *
         * @param  dataIn  input stream
         * @return  double value
         */
        double readDouble( DataInput dataIn ) throws IOException;
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
     * Reads and returns a fixed number of bytes from a stream.
     *
     * @param  in  input stream
     * @param  nbyte  required byte count
     * @return   full buffer of size <code>nbyte</code>
     * @throws  IOException  if read could not complete
     */
    byte[] readStreamBytes( InputStream in, int nbyte ) throws IOException {
        return readStreamBytes( in, nbyte, getContext() );
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
     * Reads and returns a fixed number of bytes from a stream.
     * An error is reported if the stream ends mid-read.
     *
     * @param  in  input stream
     * @param  nbyte  number of bytes to read
     * @param  context  error reporting context
     * @return   full buffer of size <code>nbyte</code>
     * @throws  IOException  if read could not complete
     */
    public static byte[] readStreamBytes( InputStream in, int nbyte,
                                          VotLintContext context )
            throws IOException {
        byte[] buf = new byte[ nbyte ];
        for ( int ip = 0; ip < nbyte; ) {
            int nr = in.read( buf, ip, nbyte - ip );
            if ( nr < 0 ) {
                context.error( new VotLintCode( "EOF" ),
                               "Scream ended during data read; done "
                             + ip + "/" + nbyte );
                throw new EOFException();
            }
            ip += nr;
        }
        return buf;
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
