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

    private final ReportElement el_;
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
     * Constructor.
     *
     * @param  el  reporting context
     */
    private ValueParser( ReportElement el ) {
        el_ = el;
    }

    /**
     * Checks the value of a string which contains the value. 
     * This is presumably either the content of the <code>value</code> attribute
     * of a PARAM element or the contents of a TD element.
     *
     * @param  text  value string
     * @param  irow  row index at which error occurred,
     *               or negative if unknown/inapplicable
     */
    public abstract void checkString( String text, long irow );

    /**
     * Checks the value of a table element which is encoded in a BINARY
     * stream.
     *
     * @param  in  input stream
     * @param  irow  row index at which error occurred,
     *               or negative if unknown/inapplicable
     */
    public abstract void checkStream( InputStream in, long irow )
            throws IOException;

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
     * @param   irow   row index associated with message,
     *                 or -1 for unknown/inapplicable
     */
    public void info( VotLintCode code, String msg, long irow ) {
        getContext().info( code, el_.msg( msg, irow ) );
    }

    /**
     * Writes a warning message to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     * @param   irow   row index associated with message,
     *                 or -1 for unknown/inapplicable
     */
    public void warning( VotLintCode code, String msg, long irow ) {
        getContext().warning( code, el_.msg( msg, irow ) );
    }

    /**
     * Writes an error message to the user.
     *
     * @param   code  message identifier
     * @param   msg  message text
     * @param   irow   row index associated with message,
     *                 or -1 for unknown/inapplicable
     */
    public void error( VotLintCode code, String msg, long irow ) {
        getContext().error( code, el_.msg( msg, irow ) );
    }

    /**
     * Constructs a ValueParsers for a given element.
     *
     * @param   handler  element handler
     * @param   name    name attribute value
     * @param   datatype  datatype attribute value
     * @param   arraysize  arraysize attribute value
     * @param   xtype   xtype (extended type) attribute value
     * @return   a suitable ValueParser, or <code>null</code> if one can't
     *           be constructed
     */
    public static ValueParser makeParser( ElementHandler handler, String name,
                                          String datatype, String arraysize,
                                          String xtype ) {
        ReportElement el = new ReportElement( handler, name );

        /* If no datatype has been specified, we can't do much.
         * Just return, this will trigger an error report elsewhere. */
        if ( datatype == null || datatype.trim().length() == 0 ) {
            return null;
        }

        /* Work out the array shape. */
        int[] shape;
        if ( arraysize == null || arraysize.trim().length() == 0 ) {
            shape = new int[] { 1 };
            if ( "char".equals( datatype ) ||
                 "unicodeChar".equals( datatype ) ) {
                handler.info( new VotLintCode( "AR1" ),
                              el.msg( "No arraysize for datatype='character'"
                                    + "; implies single character" ) );
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
                                           el.msg( "Bad arraysize value '"
                                                 + arraysize + "'" ) );
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
                                       el.msg( "Bad arraysize value '"
                                             + arraysize + "'" ) );
                        return null;
                    }
                    if ( shape[ i ] < 0 ) {
                        handler.error( new VotLintCode( "DMN" ),
                                       el.msg( "Negative dimensions element "
                                             + shape[ i ] ) );
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
            makeXtypeParser( el, handler.getContext(), xtype, datatype, shape );
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
                    return new SingleCharParser( el, ascii );
                }
                else if ( shape.length == 1 ) {
                    return stringLeng < 0
                         ? new VariableCharParser( el, ascii )
                         : new FixedCharParser( el, ascii, stringLeng );
                }
                else {
                    return nel < 0
                         ? new VariableCharArrayParser( el, ascii )
                         : new FixedCharArrayParser( el, ascii, nel,
                                                     stringLeng );
                }
            }
            else if ( "bit".equals( datatype ) ) {
                return nel < 0 ? new VariableBitParser( el )
                               : new FixedBitParser( el, nel );
            }
            else if ( "floatComplex".equals( datatype ) ) {
                FloatParser fParser = new FloatParser( new ReportElement() );
                return nel < 0
                     ? new VariableArrayParser( el, fParser, float[].class )
                     : new FixedArrayParser( el, fParser, float[].class,
                                             nel * 2 );
            }
            else if ( "doubleComplex".equals( datatype ) ) {
                DoubleParser dParser = new DoubleParser( new ReportElement() );
                return nel < 0
                     ? new VariableArrayParser( el, dParser, double[].class )
                     : new FixedArrayParser( el, dParser, double[].class,
                                             nel * 2 );
            }
            else {
                if ( nel == 1 ) {
                    return makeScalarParser( el, datatype, handler );
                }
                else {
                    ValueParser base = makeScalarParser( new ReportElement(),
                                                         datatype, handler );
                    if ( base == null ) {
                        return null;
                    }
                    else {
                        Class<?> clazz =
                            getArrayClass( base.getContentClass() );
                        return nel < 0 
                           ? new VariableArrayParser( el, base, clazz )
                           : new FixedArrayParser( el, base, clazz, nel );
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
     * @param  el  reporting context
     * @param  context  reporting context
     * @param  xtype    xtype attribute value
     * @param  datatype  datatype attribute value
     * @param  shape    parsed arraysize (final -1 element indicates unbounded)
     */
    private static ValueParser makeXtypeParser( ReportElement el,
                                                VotLintContext context,
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
                               el.msg( "xtype='timestamp' "
                                     + "for non-string-type value" ) );
                return null;
            }
            else {
                return makeTimestampParser( el, arraysize1d );
            }
        }

        else if ( "interval".equals( xtype ) ) {
            if ( arraysize1d != 2 ) {
                context.error( new VotLintCode( "XI2" ),
                               el.msg( "xtype='interval' for arraysize != 2" ));
                return null;
            }
            else if ( !isNumeric ) {
                context.error( new VotLintCode( "XI9" ),
                               el.msg( "xtype='interval'"
                                     + " for non-numeric datatype" ) );
                return null;
            }
            else if ( isFloating ) {
                return makeFloatingIntervalParser( el, datatype );
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
                               el.msg( "xtype='point' for arraysize != 2" ) );
                return null;
            }
            else if ( !isFloating ) {
                context.error( new VotLintCode( "XP9" ),
                               el.msg( "xtype='point'"
                                     + " for non-floating datatype" ) );
                return null;
            }
            else {
                return makePointParser( el, datatype );
            }
        }

        else if ( "circle".equals( xtype ) ) {
            if ( arraysize1d != 3 ) {
                context.error( new VotLintCode( "XC3" ),
                               el.msg( "xtype='circle' for arraysize != 3" ) );
                return null;
            }
            else if ( !isFloating ) {
                context.error( new VotLintCode( "XC9" ),
                               el.msg( "xtype='circle'"
                                     + " for non-floating datatype" ) );
                return null;
            }
            else {
                return makeCircleParser( el, datatype );
            }
        }

        else if ( "polygon".equals( xtype ) ) {
            if ( !isArray1d ) {
                context.error( new VotLintCode( "XSV" ),
                               el.msg( "xtype='polygon'"
                                     + " for non-vector arraysize" ) );
                return null;
            }
            else if ( !isFloating ) {
                context.error( new VotLintCode( "XS9" ),
                               el.msg( "xtype='polygon'"
                                     + " for non-floating datatype" ) );
                return null;
            }
            else {
                return makePolygonParser( el, datatype, arraysize1d );
            }
        }

        /* DALI sec 3.3: namespaced xtypes are explicitly allowed. */
        else if ( xtype.indexOf( ':' ) > 0 ) {
            context.info( new VotLintCode( "XNI" ),
                          el.msg( "Namespaced non-DALI xtype value \""
                                + xtype + "\"" ) );
            return null;
        }

        /* Warn for non-standard non-namespaced xtypes. */
        else {
            context.warning( new VotLintCode( "XDL" ),
                             el.msg( "Non-DALI 1.1 xtype value \""
                                   + xtype + "\"" ) );
            return null;
        }
    }

    /**
     * Constructs a parser for a single value.
     *
     * @param  el  reporting context
     * @param   datatype  value of datatype attribute
     * @param   handler   element 
     */
    private static ValueParser makeScalarParser( ReportElement el,
                                                 String datatype,
                                                 ElementHandler handler ) {
        if ( "boolean".equals( datatype ) ) {
            return new BooleanParser( el );
        }
        else if ( "unsignedByte".equals( datatype ) ) {
            return new IntegerParser( el, 1, 0, 255, Short.class );
        }
        else if ( "short".equals( datatype ) ) {
            return new IntegerParser( el, 2, Short.MIN_VALUE, Short.MAX_VALUE,
                                      Short.class );
        }
        else if ( "int".equals( datatype ) ) {
            return new IntegerParser( el, 4, Integer.MIN_VALUE,
                                      Integer.MAX_VALUE, Integer.class );
        }
        else if ( "long".equals( datatype ) ) {
            return new IntegerParser( el, 8, Long.MIN_VALUE, Long.MAX_VALUE,
                                      Long.class );
        }
        else if ( "float".equals( datatype ) ) {
            return new FloatParser( el );
        }
        else if ( "double".equals( datatype ) ) {
            return new DoubleParser( el );
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
     * @param  el   reporting context
     * @param  stringLeng   fixed element count for array values,
     *                      or negative value for variable element count
     * @return  new parser
     */
    private static ValueParser makeTimestampParser( ReportElement el,
                                                    int stringLeng ) {
        return makeStringParser( el, stringLeng, (context, txt, irow ) -> {
            if ( txt != null && txt.trim().length() > 0 ) {
                if ( ! ISO_REGEX.matcher( txt ).matches() ) {
                    context.error( new VotLintCode( "TSR" ),
                                   el.msg( "Timestamp value \"" + txt + "\""
                                         + " does not match "
                                         + "YYYY-MM-DD['T'hh:mm:ss[.SSS]]['Z']",
                                           irow ) );
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
                                       el.msg( "Bad timestamp \"" + txt + "\" "
                                             + e.getMessage(), irow ) );
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
     * @param  el  reporting context
     * @param  datatype  datatype value
     * @return  new parser
     */
    private static ValueParser makeFloatingIntervalParser( ReportElement el,
                                                           String datatype ) {
        return makeFloatingArrayParser( el, datatype, 2,
                                        (context, values, irow ) -> {
            double d0 = values[ 0 ];
            double d1 = values[ 1 ];
            if ( Double.isNaN( d0 ) != Double.isNaN( d1 ) ) {
                context.error( new VotLintCode( "XIN" ),
                               el.msg( "One but not both interval limit is NaN "
                                     + Arrays.toString( values ), irow ) );
            }
        } );
    }

    /**
     * Returns a parser for xtype='point' (DALI 1.1 sec 3.3.5).
     *
     * @param  el  reporting context
     * @param  datatype  datatype value
     * @return  new parser
     */
    private static ValueParser makePointParser( ReportElement el,
                                                String datatype ) {
        return makeFloatingArrayParser( el, datatype, 2,
                                        (context, values, irow) -> {
            double d0 = values[ 0 ];
            double d1 = values[ 1 ];
            if ( Double.isNaN( d0 ) != Double.isNaN( d1 ) ) {
                context.error( new VotLintCode( "XIN" ),
                               el.msg( "One but not both point coordinate "
                             + "is NaN " + Arrays.toString( values ), irow ) );
            }
            else if ( Double.isInfinite( d0 ) || Double.isInfinite( d1 ) ) {
                context.error( new VotLintCode( "XIZ" ),
                               el.msg( "Infinite point coordinate(s) "
                                     + Arrays.toString( values ), irow ) );
            }
        } );
    }

    /**
     * Returns a parser for xtype='circle' (DALI 1.1 sec 3.3.6).
     *
     * @param  el  reporting context
     * @param  datatype  datatype value
     * @return  new parser
     */
    private static ValueParser makeCircleParser( ReportElement el,
                                                 String datatype ) {
        return makeFloatingArrayParser( el, datatype, 3,
                                        (context, values, irow) -> {
            double c1 = values[ 0 ];
            double c2 = values[ 1 ];
            double r = values[ 2 ];
            if ( Double.isNaN( c1 ) != Double.isNaN( c2 ) ||
                 Double.isNaN( c1 ) != Double.isNaN( r ) ) {
                context.error( new VotLintCode( "XIN" ),
                               el.msg( "Some but not all circle parameters are"
                                     + " NaN " + Arrays.toString( values ),
                                       irow ) );
            }
        } );
    }

    /**
     * Returns a parser for xtype='polygon' (DALI 1.1 sec 3.3.7).
     *
     * @param  el  reporting context
     * @param  datatype  datatype value
     * @param  nel     fixed element count of array value,
     *                 or negative for variable element count
     * @return  new parser
     */
    private static ValueParser makePolygonParser( ReportElement el,
                                                  String datatype, int nel ) {
        return makeFloatingArrayParser( el, datatype, nel,
                                        (context, values, irow) -> {
            int ncoord = values.length;
            if ( nel >= 0 && ncoord != nel ) {
                context.error( new VotLintCode( "E09" ),
                               el.msg( "Wrong number of elements in array, "
                                     + ncoord + " found, " + nel + " expected",
                                       irow ) );
            }
            else if ( ncoord % 2 != 0 ) {
                context.error( new VotLintCode( "XSO" ),
                               el.msg( "Odd number (" + ncoord + ")"
                                     + " of polygon coords", irow ) );
            }
            else if ( ncoord > 0 && ncoord < 6 ) {
                context.error( new VotLintCode( "XSF" ),
                               el.msg( "Too few (" + ncoord
                                     + ") polygon coords", irow ) );
            }
        } );
    }

    /**
     * Creates a generic parser that works with fixed or variable-length
     * 1-d char arrays.  This is not intended to work for unicodeChar
     * arrays (though in practice it might do).
     *
     * @param  el  reporting context
     * @param  stringLeng   fixed element count for array values,
     *                      or negative value for variable element count
     * @param  stringChecker  callback to perform checking on a String value
     * @return  new parser
     */
    private static ValueParser
            makeStringParser( ReportElement el, int stringLeng,
                              StringChecker stringChecker ) {
        return new AbstractParser( el, String.class, 1 ) {
            public void checkString( String txt, long irow ) {
                stringChecker.check( getContext(), txt, irow );
            }
            public void checkStream( InputStream in, long irow )
                    throws IOException {
                int nchar = stringLeng >= 0 ? stringLeng : readCount( in );
                String txt = new String( readStreamBytes( in, nchar ),
                                         StandardCharsets.UTF_8 );
                stringChecker.check( getContext(), txt, irow );
            }
        };
    }

    /**
     * Creates a generic parser that works with fixed- or variable-length
     * arrays of floating point values.
     * Note that this currently only works with <code>datatype</code>
     * values of "<code>float</code>" or "<code>double</code>".
     *
     * @param  el  reporting context
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
            makeFloatingArrayParser( ReportElement el, String datatype, int nel,
                                     DoubleArrayChecker arrayChecker ) {
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
        return new AbstractParser( el, aclazz, nel >= 0 ? nel : -1 ) {
            public void checkString( String text, long irow ) {
                double[] values = readString( text, irow );
                if ( values != null ) {
                    VotLintContext context = getContext();
                    if ( nel >= 0 && values.length != nel ) {
                        error( new VotLintCode( "E08" ),
                               "Wrong number of elements in array, " + 
                             + values.length + " found, "
                             + nel + " expected", irow );
                    }
                    else {
                        arrayChecker.check( context, values, irow );
                    }
                }
            }
            public void checkStream( InputStream in, long irow )
                    throws IOException {
                double[] values = readStream( in );
                if ( values != null ) {
                    assert nel < 0 || values.length == nel;
                    arrayChecker.check( getContext(), values, irow );
                }
            }

            /**
             * Reads a floating point array from a string value.
             *
             * @param  text  string value of field
             * @param  irow  row index, or -1
             * @return   content as double array, or null if not parseable
             */
            private double[] readString( String text, long irow ) {
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
                                     + " '" + sitem + "'", irow );
                                return null;
                            }
                        }
                        else {
                            error( new VotLintCode( "FP0" ),
                                   "Bad " + datatype + " string"
                                 + " '" + sitem + "'", irow );
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
     * Interface for checking string values.
     */
    @FunctionalInterface
    private interface StringChecker {

        /**
         * Examine a supplied string and report any problems
         * through the supplied context.
         *
         * @param   context  global context
         * @param   txt   string to check
         * @param   irow   row index associated with text,
         *                 or -1 if unknown/inapplicable
         */
        void check( VotLintContext context, String txt, long irow );
    }

    /**
     * Interface for checking double array values.
     */
    @FunctionalInterface
    private interface DoubleArrayChecker {

        /**
         * Examine a supplied double array and report any problems
         * through the supplied context.
         *
         * @param   context  global context
         * @param   array   array to check, not null
         * @param   irow   row index associated with array,
         *                 or -1 if unknown/inapplicable
         */
        void check( VotLintContext context, double[] array, long irow );
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
         * @param  el  reporting context
         * @param   clazz  element class
         * @param   count  element count
         */
        public AbstractParser( ReportElement el, Class<?> clazz, int count ) {
            super( el );
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
         * @param  el   reporting context
         * @param  nbyte  number of bytes read by stream reading method.
         * @param  clazz  class of elements
         * @param  count  number of elements
         */
        SlurpParser( ReportElement el, int nbyte, Class<?> clazz, int count ) {
            super( el );
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
        public void checkStream( InputStream in, long irow )
                throws IOException {
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
         * @param  el   reporting context
         * @param   base  parser which can read a scalar
         * @param   count  number of scalar elements read by this parser
         */
        FixedArrayParser( ReportElement el, ValueParser base,
                          Class<?> clazz, int count ) {
            super( el, clazz, count );
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
        public void checkString( String text, long irow ) {
            StringTokenizer stok = new StringTokenizer( text );
            int ntok = stok.countTokens();
            if ( ntok != count_ ) {
                error( new VotLintCode( "E09" ),
                       "Wrong number of elements in array, " + 
                       ntok + " found, " + count_ + " expected", irow );
            }
            while ( stok.hasMoreTokens() ) {
                base_.checkString( stok.nextToken(), irow );
            }
        }
        public void checkStream( InputStream in, long irow )
                throws IOException {
            for ( int i = 0; i < count_; i++ ) {
                base_.checkStream( in, irow );
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
         * @param  el   reporting context
         * @param  base  parser which can read a scalar
         */
        VariableArrayParser( ReportElement el, ValueParser base,
                             Class<?> clazz ) {
            super( el, clazz, -1 );
            base_ = base;
            base_.toString();
        }
        public VotLintContext getContext() {
            return base_.getContext();
        }
        public void setContext( VotLintContext context ) {
            base_.setContext( context );
        }
        public void checkString( String text, long irow ) {
            for ( StringTokenizer stok = new StringTokenizer( text );
                  stok.hasMoreTokens(); ) {
                base_.checkString( stok.nextToken(), irow );
            }
        }
        public void checkStream( InputStream in, long irow )
                throws IOException {
            int count = readCount( in );
            for ( int i = 0; i < count; i++ ) {
                try {
                    base_.checkStream( in, irow );
                }
                catch ( EOFException e ) {
                    error( new VotLintCode( "EOF" ),
                           "End of stream while reading " + count +
                           " elements (probable stream corruption)", irow );
                    throw e;
                }
            }
        }
    }

    /**
     * Parser for boolean scalars.
     */
    private static class BooleanParser extends AbstractParser {

        /**
         * Constructor.
         *
         * @param  el  reporting context
         */
        public BooleanParser( ReportElement el ) {
            super( el, Boolean.class, 1 );
        }

        public void checkString( String text, long irow ) {
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
                               "Bad boolean value '" + text + "'", irow );
                }
            }
            else if ( text.equalsIgnoreCase( "true" ) ||
                      text.equalsIgnoreCase( "false" ) ) {
                return;
            }
            else {
                error( new VotLintCode( "TFX" ),
                       "Bad boolean value '" + text + "'", irow );
            }
        }

        public void checkStream( InputStream in, long irow )
                throws IOException {
            char chr = (char) ( 0xffff & in.read() );
            switch ( chr ) {
                case 'T': case 't': case '1':
                case 'F': case 'f': case '0':
                case ' ': case '?': case '\0':
                    return;
                case (char) -1:
                    error( new VotLintCode( "EOF" ),
                           "End of stream during read", irow );
                    throw new EOFException();
                default:
                    error( new VotLintCode( "TFX" ),
                           "Bad boolean value '" + chr + "'", irow );
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
         * @param  el  reporting context
         * @param   nbyte  number of bytes per integer in stream mode
         * @param   minVal  minimum legal value for integer
         * @param   maxVal  maximum legal value for integer
         */
        IntegerParser( ReportElement el, int nbyte, long minVal, long maxVal,
                       Class<?> clazz ) {
            super( el, nbyte, clazz, 1 );
            minVal_ = minVal;
            maxVal_ = maxVal;
        }

        public void checkString( String text, long irow ) {
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
                           "Bad hexadecimal string '" + text + "'", irow );
                    return;
                }
            }
            else if ( text.length() == 0 ) {
                if ( ! getContext().getVersion().allowEmptyTd() ) {
                    error( new VotLintCode( "ETD" ),
                           "Empty cell illegal for integer value", irow );
                }
                return;
            }
            else {
                try {
                    value = Long.parseLong( text );
                }
                catch ( NumberFormatException e ) {
                    error( new VotLintCode( "IT0" ),
                           "Bad integer string '" + text + "'", irow );
                    return;
                }
            }
            if ( value < minVal_ || value > maxVal_ ) {
                error( new VotLintCode( "BND" ),
                       "Value " + text + " outside type range " + 
                       minVal_ + "..." + maxVal_, irow );
            }
        }
    }

    /**
     * Parser for float values.
     */
    private static class FloatParser extends SlurpParser {

        /**
         * Constructor.
         *
         * @param  el  reporting context
         */
        FloatParser( ReportElement el ) {
            super( el, 4, Float.class, 1 );
        }
        public void checkString( String text, long irow ) {
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
                           "Bad float string '" + text + "'", irow );
                }
            }
        }
    }

    /**
     * Parser for double values.
     */
    private static class DoubleParser extends SlurpParser {

        /**
         * Constructor.
         *
         * @param  el  reporting context
         */
        DoubleParser( ReportElement el ) {
            super( el, 8, Double.class, 1 );
        }
        public void checkString( String text, long irow ) {
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
                           "Bad double string '" + text + "'", irow );
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
         * @param  el  reporting context
         * @param  count  number of bits
         */
        FixedBitParser( ReportElement el, int count ) {
            super( el, ( count + 7 ) / 8, Boolean.class, count );
            count_ = count;
        }
        public void checkString( String text, long irow ) {
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
                               "Bad value for bit vector " + text, irow );
                        return;
                }
            }
            if ( nbit != count_ ) {
                error( new VotLintCode( "CT9" ),
                       "Wrong number of elements in array (" +
                       nbit + " found, " + count_ + " expected)", irow );
            }
        }
    }

    /**
     * Parser for variable length bit vectors.
     */
    private static class VariableBitParser extends AbstractParser {

        /**
         * Constructor.
         *
         * @param  el  reporting context
         */
        public VariableBitParser( ReportElement el ) {
            super( el, boolean[].class, -1 );
        }
        public void checkString( String text, long irow ) {
            int leng = text.length();
            for ( int i = 0; i < leng; i++ ) {
                switch ( text.charAt( i ) ) {
                    case '0': case '1':
                    case ' ': case '\n':
                        break;
                    default:
                        error( new VotLintCode( "BV0" ),
                               "Bad value for bit vector " + text, irow );
                        return;
                }
            }
        }
        public void checkStream( InputStream in, long irow )
                throws IOException {
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
         * @param  el  reporting context
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         */
        public SingleCharParser( ReportElement el, boolean ascii ) {
            super( el, ascii ? 1 : 2, Character.class, 1 );
            ascii_ = ascii;
        }
        public void checkString( String text, long irow ) {
            int leng = text.length();
            switch ( leng ) {
                case 0:
                    warning( new VotLintCode( "CH0" ),
                             "Empty character value is questionable", irow );
                    break;
                case 1:
                    break;
                default:
                    warning( new VotLintCode( "CH1" ),
                             "Characters after first in char scalar ignored" +
                             " - missing arraysize?", irow );
            }
            if ( ascii_ && leng > 0 && text.charAt( 0 ) > 0x7f ) {
                error( new VotLintCode( "CRU" ),
                       "Non-ascii character in 'char' data", irow );
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
         * @param  el  reporting context
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         * @param  count  number of characters in string
         */
        public FixedCharParser( ReportElement el, boolean ascii, int count ) {
            super( el, count * ( ascii ? 1 : 2 ), String.class, 1 );
        }
        public void checkString( String text, long irow ) {
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
         * @param  el  reporting context
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         */
        VariableCharParser( ReportElement el, boolean ascii ) {
            super( el, String.class, 1 );
            ascii_ = ascii;
        }
        public void checkString( String text, long irow ) {
        }
        public void checkStream( InputStream in, long irow )
                throws IOException {
            slurpStream( in, readCount( in ) * ( ascii_ ? 1 : 2 ) );
        }
    }

    /**
     * Parser for variable-length multi-dimensional character arrays.
     */
    private static class VariableCharArrayParser extends AbstractParser {
        final boolean ascii_;
 
        /**
         * Constructor.
         *
         * @param  el  reporting context
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         */
        VariableCharArrayParser( ReportElement el, boolean ascii ) {
            super( el, String[].class, -1 );
            ascii_ = ascii;
        }

        public void checkStream( InputStream in, long irow )
                throws IOException {
            slurpStream( in, readCount( in ) * ( ascii_ ? 1 : 2 ) );
        }

        public void checkString( String text, long irow ) {
        }
    }

    /**
     * Parser for fixed-length multi-dimensional character arrays.
     */
    private static class FixedCharArrayParser extends AbstractParser {
        final boolean ascii_;
        final int nchar_;

        /**
         * Constructor.
         *
         * @param  el  reporting context
         * @param  ascii  true for 7-bit ASCII characters, false for 
         *                16-bit unicode
         * @param  nchar  number of characters
         * @param  stringLeng  characters per string
         */
        FixedCharArrayParser( ReportElement el, boolean ascii, int nchar,
                              int stringLeng ) {
            super( el, String[].class, nchar / stringLeng );
            ascii_ = ascii;
            nchar_ = nchar;
        }

        public void checkStream( InputStream in, long irow )
                throws IOException {
            slurpStream( in, nchar_ * ( ascii_ ? 1 : 2 ) );
        }

        public void checkString( String text, long irow ) {
            int leng = text.length();
            if ( text.length() != nchar_ ) {
                warning( new VotLintCode( "C09" ),
                         "Wrong number of characters in string (" +
                         leng + " found, " + nchar_ + " expected)", irow );
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
                   " (probable stream corruption)", -1 );
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
                       "(probably stream corruption)", -1 );
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

    /**
     * Records the FIELD or PARAM element for which a ValueParser was created,
     * and manages creation of user messages that include this context.
     */
    private static class ReportElement {
        final ElementHandler handler_;
        final String name_;

        /**
         * Constructor.
         *
         * @param  element   handler, usually type Field or Param
         * @param  name   value of element name attribute
         */
        ReportElement( ElementHandler handler, String name ) {
            handler_ = handler;
            name_ = name;
        }

        /**
         * Constructs a ReportElement that doesn't report any context.
         */
        ReportElement() {
            this( null, null );
        }

        /**
         * Returns a user-directed message that includes
         * context information alongside the supplied text.
         *
         * @param   txt  basic message text
         * @param   irow  0-based row index to which message is associated,
         *                or negative for unknown/inapplicable
         * @return   message text with context
         */
        String msg( String txt, long irow ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( txt );
            if ( handler_ != null || name_ != null ) {
                sbuf.append( " (" );
                if ( irow < 0 ) {
                    if ( handler_ != null ) {
                        sbuf.append( handler_.toString() )
                            .append( ' ' );
                    }
                }
                if ( name_ != null ) {
                    sbuf.append( name_ );
                }
                if ( irow >= 0 ) {
                    sbuf.append( " row " )
                        .append( ( irow + 1 ) );
                }
                sbuf.append( ')' );
            }
            return sbuf.toString();
        }

        /**
         * Returns a user-directed message that includes
         * context information alongside the supplied text,
         * without a row index.
         *
         * @param   txt  basic message text
         * @return   message text with context
         */
        String msg( String txt ) {
            return msg( txt, -1 );
        }
    }
}
