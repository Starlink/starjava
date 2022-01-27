package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.lang.reflect.Array;
import java.util.function.IntFunction;

/**
 * Provides an array-&gt;array function in which input elements are
 * mapped to output elements by use of a given JEL expression.
 *
 * <p>Instances of this class are not threadsafe.
 *
 * @author   Mark Taylor
 * @since    23 Mar 2021
 */
public class JELArrayFunction<I,O> {

    private final String ivarName_;
    private final String xvarName_;
    private final String fexpr_;
    private final XResolver xResolver_;
    private final CompiledExpression fCompex_;
    private final Object[] args_;
    private final IntFunction<O> outSupplier_;
    private final ArrayTransfer elEval_;
    private final BadTransfer badEval_;

    /**
     * Constructor.
     *
     * @param   ivarName  name of the array index variable (0-based)
     * @param   xvarName  name of the array element variable (for instance "x")
     * @param   fexpr  text of expression giving the function value,
     *                 in terms of <code>xvarname</code> (for instance "x+1")
     * @param   inClazz  type of input array; must be an array type of
     *                   primitive or object elements
     * @param   outClazz  type of output array; if not known, Object.class
     *                    may be given, and the output type will be determined
     *                    from the expression
     */
    public JELArrayFunction( String ivarName, String xvarName, String fexpr,
                             Class<I> inClazz, Class<O> outClazz )
            throws CompilationException {
        ivarName_ = ivarName;
        xvarName_ = xvarName;
        fexpr_ = fexpr;

        /* Compile the expression. */
        Class<?>[] staticLib =
            JELUtils.getStaticClasses().toArray( new Class<?>[ 0 ] );
        xResolver_ = new XResolver( ivarName, xvarName, inClazz );
        Class<?>[] dynamicLib = new Class<?>[] { xResolver_.getClass() };
        Library lib =
            JELUtils.createLibrary( staticLib, dynamicLib, xResolver_ );
        Class<?> reqElClazz = outClazz.isArray() ? outClazz.getComponentType()
                                                 : null;
        fCompex_ = Evaluator.compile( fexpr, lib, reqElClazz );
        args_ = new Object[] { xResolver_ };

        /* Prepare to create output arrays of the right type. */
        final Class<?> outElClazz = reqElClazz == null
                                  ? getElementType( fCompex_ )
                                  : reqElClazz;
        @SuppressWarnings("unchecked")
        IntFunction<O> outSupplier =
            (IntFunction<O>) ( n -> (O) Array.newInstance( outElClazz, n ) );
        outSupplier_ = outSupplier;

        /* Prepare to populate output arrays of the right type. */
        if ( outElClazz.equals( byte.class ) ) {
            elEval_ = (out, i) ->
                ((byte[]) out)[ i ] = fCompex_.evaluate_byte( args_ );
            badEval_ = null;
        }
        else if ( outElClazz.equals( short.class ) ) {
            elEval_ = (out, i) ->
                ((short[]) out)[ i ] = fCompex_.evaluate_short( args_ );
            badEval_ = null;
        }
        else if ( outElClazz.equals( int.class ) ) {
            elEval_ = (out, i) ->
                ((int[]) out)[ i ] = fCompex_.evaluate_int( args_ );
            badEval_ = null;
        }
        else if ( outElClazz.equals( long.class ) ) {
            elEval_ = (out, i) ->
                ((long[]) out)[ i ] = fCompex_.evaluate_long( args_ );
            badEval_ = null;
        }
        else if ( outElClazz.equals( float.class ) ) {
            elEval_ = (out, i) ->
                ((float[]) out)[ i ] = fCompex_.evaluate_float( args_ );
            badEval_ = (out, i) ->
                ((float[]) out)[ i ] = Float.NaN;
        }
        else if ( outElClazz.equals( double.class ) ) {
            elEval_ = (out, i) ->
                ((double[]) out)[ i ] = fCompex_.evaluate_double( args_ );
            badEval_ = (out, i) ->
                ((double[]) out)[ i ] = Double.NaN;
        }
        else if ( outElClazz.equals( boolean.class ) ) {
            elEval_ = (out, i) ->
                ((boolean[]) out)[ i ] = fCompex_.evaluate_boolean( args_ );
            badEval_ = null;
        }
        else if ( outElClazz.equals( char.class ) ) {
            elEval_ = (out, i) ->
                ((char[]) out)[ i ] = fCompex_.evaluate_char( args_ );
            badEval_ = null;
        }
        else {
            elEval_ = (out, i) ->
                ((Object[]) out)[ i ] = fCompex_.evaluate( args_ );
            badEval_ = null;
        }
    }

    /**
     * Evaluates this expression.
     * Elements for which the evaluation failed are given some type-dependent
     * default value, such as NaN, null, or zero.
     *
     * @param  inArray   input array
     * @return  outArray   output array, same length as input
     */
    public O evaluate( I inArray ) {
        if ( inArray == null || ! inArray.getClass().isArray() ) {
            return null;
        }
        int nel = Array.getLength( inArray );
        O outArray = outSupplier_.apply( nel );
        xResolver_.array_ = inArray;
        for ( int i = 0; i < nel; i++ ) {
            xResolver_.index_ = i;
            try {
                elEval_.transfer( outArray, i );
            }
            catch ( Throwable e ) {
                if ( badEval_ != null ) {
                    badEval_.transfer( outArray, i );
                }
            }
        }
        return outArray;
    }

    /**
     * Utility method to create and use an array function in one go.
     *
     * @param   ivarName  name of the array index variable (for instance "i")
     * @param   xvarName  name of the array element variable (for instance "x")
     * @param   fexpr  text of expression giving the function value,
     *                 in terms of <code>xvarname</code> (for instance "x+1")
     * @param   inArray  input array
     * @return   output array, same length as input
     */
    public static <I> Object evaluate( String ivarName, String xvarName,
                                       String fexpr, I inArray )
            throws CompilationException {
        if ( inArray == null ) {
            return null;
        }
        else {
            @SuppressWarnings("unchecked")
            Class<I> inClazz = (Class<I>) inArray.getClass();
            return typedEvaluate( ivarName, xvarName, fexpr, inArray,
                                  inClazz, Object.class );
        }
    }

    /**
     * Creates and uses an array function in one go, with types explicitly
     * supplied.
     *
     * @param   ivarName  name of the array index variable (for instance "i")
     * @param   xvarName  name of the array element variable (for instance "x")
     * @param   fexpr  text of expression giving the function value,
     *                 in terms of <code>xvarname</code> (for instance "x+1")
     * @param   inArray  input array
     * @param   inClazz   class of inArray
     * @param   required class of output array, or Object.class if not known
     * @return   output array, same length as input
     */
    private static <I,O> O typedEvaluate( String ivarName, String xvarName,
                                          String fexpr,
                                          I inArray, Class<I> inClazz,
                                          Class<O> outClazz )
            throws CompilationException {
        return new JELArrayFunction<I,O>( ivarName, xvarName, fexpr,
                                          inClazz, outClazz )
              .evaluate( inArray );
    }

    /**
     * Returns the output type for a compiled JEL expression.
     * This will be a primitive type if applicable.
     *
     * @param  compEx  compiled expression
     * @return   primitive or object type
     */
    private static Class<?> getElementType( CompiledExpression compEx ) {
        Class<?> clazz = compEx.getTypeC();
        if ( clazz.equals( Byte.class ) ) {
            return byte.class;
        }
        else if ( clazz.equals( Short.class ) ) {
            return short.class;
        }
        else if ( clazz.equals( Integer.class ) ) {
            return int.class;
        }
        else if ( clazz.equals( Long.class ) ) {
            return long.class;
        }
        else if ( clazz.equals( Float.class ) ) {
            return float.class;
        }
        else if ( clazz.equals( Double.class ) ) {
            return double.class;
        }
        else if ( clazz.equals( Boolean.class ) ) {
            return boolean.class;
        }
        else if ( clazz.equals( Character.class ) ) {
            return char.class;
        }
        else {
            return clazz;
        }
    }

    /**
     * This public class is an implementation detail,
     * not intended for external use.
     */
    public static class XResolver extends DVMap {
        private final String ivarName_;
        private final String xvarName_;
        private final String typeName_;
        private Object array_;
        private int index_;
       
        /**
         * Constructor.
         *
         * @param   ivarName  name of the array index variable
         * @param   xvarName  name of the array element variable
         * @param   arrayClazz   array class of the input array
         */
        private XResolver( String ivarName, String xvarName,
                           Class<?> arrayClazz ) {
            xvarName_ = xvarName;
            ivarName_ = ivarName;
            final String tname;
            if ( arrayClazz.equals( byte[].class ) ) {
                tname = "Byte";
            }
            else if ( arrayClazz.equals( short[].class ) ) {
                tname = "Short";
            }
            else if ( arrayClazz.equals( int[].class ) ) {
                tname = "Int";
            }
            else if ( arrayClazz.equals( long[].class ) ) {
                tname = "Long";
            }
            else if ( arrayClazz.equals( float[].class ) ) {
                tname = "Float";
            }
            else if ( arrayClazz.equals( double[].class ) ) {
                tname = "Double";
            }
            else if ( arrayClazz.equals( boolean[].class ) ) {
                tname = "Boolean";
            }
            else if ( arrayClazz.equals( char[].class ) ) {
                tname = "Char";
            }
            else if ( arrayClazz.equals( String[].class ) ) {
                tname = "String";
            }
            else {
                tname = "Object";
            }
            typeName_ = tname;
        }

        public String getTypeName( String name ) {
            if ( name.equals( ivarName_ ) ) {
                return "Int";
            }
            else if ( name.equals( xvarName_ ) ) {
                return typeName_;
            }
            else {
                return null;
            }
        }

        public byte getByteProperty( String name ) {
            return name.equals( xvarName_ ) ? ((byte[]) array_)[ index_ ]
                                            : (byte) 0;
        }

        public short getShortProperty( String name ) {
            return name.equals( xvarName_ ) ? ((short[]) array_)[ index_ ]
                                            : (short) 0;
        }

        public int getIntProperty( String name ) {
            if ( name.equals( ivarName_ ) ) {
                return index_;
            }
            else if ( name.equals( xvarName_ ) ) {
                return ((int[]) array_)[ index_ ];
            }
            else {
                return 0;
            }
        }

        public long getLongProperty( String name ) {
            return name.equals( xvarName_ ) ? ((long[]) array_)[ index_ ]
                                            : 0;
        }

        public float getFloatProperty( String name ) {
            return name.equals( xvarName_ ) ? ((float[]) array_)[ index_ ]
                                            : Float.NaN;
        }

        public double getDoubleProperty( String name ) {
            return name.equals( xvarName_ ) ? ((double[]) array_)[ index_ ]
                                            : Double.NaN;
        }

        public boolean getBooleanProperty( String name ) {
            return name.equals( xvarName_ ) ? ((boolean[]) array_)[ index_ ]
                                            : false;
        }

        public char getCharProperty( String name ) {
            return name.equals( xvarName_ ) ? ((char[]) array_)[ index_ ]
                                            : (char) 0;
        }

        public String getStringProperty( String name ) {
            return name.equals( xvarName_ ) ? ((String[]) array_)[ index_ ]
                                            : null;
        }

        public Object getObjectProperty( String name ) {
            return name.equals( xvarName_ ) ? ((Object[]) array_)[ index_ ]
                                            : null;
        }
    }

    /**
     * Defines how to place an element into a target array,
     * with exceptions possible.
     */
    @FunctionalInterface
    private interface ArrayTransfer {
        void transfer( Object array, int index ) throws Throwable;
    }

    /**
     * Defines how to place an element into a target array,
     * with no exceptions possible.
     */
    @FunctionalInterface
    private interface BadTransfer {
        void transfer( Object array, int index );
    }
}
