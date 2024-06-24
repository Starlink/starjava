package uk.ac.starlink.array;

import java.util.Arrays;

/**
 * Handles bad pixel values.
 * Instances of this class can detect and write values in an array with
 * a `magic' bad value.
 * Since its methods reference values within arrays, client code does
 * not need to be written in a primitive-type-specific fashion.
 * <p>
 * Obtain an instance of this class using the {@link #getHandler} 
 * static method.
 * <p>
 * It is not expected to be necessary to extend this class for normal 
 * purposes, but there is a protected constructor in case this is
 * required.  Such subclassing might be useful to provided specialised
 * bad value handlers in which, for instance, floating <code>NaN</code>
 * values do not count as bad, or infinite values do.  Such subclassing
 * should be done and used with care.
 * 
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public abstract class BadHandler {
    
    /**
     * Indicates whether an element of an array represents a bad value
     * for this NDArray.
     *
     * @param  array  the array in which the pixel resides
     * @param  pos    the position in array of the pixel to be tested.
     * @throws ClassCastException  if array is not an array of primitives with
     *                             type matching the type of this handler
     * @throws IndexOutOfBoundsException  if pos is outside the bounds of array
     */
    abstract public boolean isBad( Object array, int pos );

    /**
     * Writes a sequence of bad values into an array.
     *
     * @param  array   array into which to write bad values, which must be
     *                 an array of primitive type matching this handler's
     *                 Type with at least start+length elements
     * @param  start   the position in array at which to start writing
     *                 bad values
     * @param  size    the number of elements of array to write bad values to
     *
     * @throws ClassCastException  if array is not an array of primitives with
     *                             type matching the type of this handler
     * @throws IndexOutOfBoundsException  if access outside the bounds of the
     *                                    array is attempted
     */
    abstract public void putBad( Object array, int start, int size );

    /**
     * Writes a single bad value into an array.
     *
     * @param  array   array into which to write a bad value; it must be
     *                 an array of primitive type matching this handler's
     *                 Type with at least pos-1 elements
     * @param  pos     the position at which to write the bad value
     *
     * @throws ClassCastException  if array is not an array of primitives with
     *                             type matching the type of this handler
     * @throws IndexOutOfBoundsException  if access outside the bounds of the
     *                                    array is attempted
     */
    abstract public void putBad( Object array, int pos );

    /**
     * Turns a single element of an array into a {@link java.lang.Number} 
     * object of the appropriate type.  The return value will be 
     * one of the wrapper types Byte, Short, Integer, Float or Double 
     * wrapping the value in question, unless the array element is
     * bad, in which case the null value will be returned.
     *
     * @param  array   array containing the value to be converted
     * @param  pos     the index into array at which the element of interest
     *                 is to be found
     * @return   a Number object wrapping the pos'th element of array,
     *           or null if that element is bad
     * @throws IndexOutOfBoundsException  if access outside the bounds of the
     *                                    array is attempted
     */
    abstract public Number makeNumber( Object array, int pos );

    /**
     * Returns an <code>ArrayHandler</code> object for testing/setting
     * bad values
     * in a given primitive array according to the bad value handling
     * rules of this <code>BadHandler</code>.  The same functionality can
     * be achieved by use of the {@link #isBad} and {@link #putBad(Object,int)}
     * methods of this class, but using an <code>ArrayHandler</code> can avoid
     * repeated typecasts and provide better performance.
     *
     * @throws  ClassCastException  if array is not an array of primitives with
     *                              type matching the type of this handler
     */
    abstract public ArrayHandler arrayHandler( Object array );

    /**
     * Class provided for testing and setting bad values in a given 
     * primitive array.  An object of this class is returned by the
     * {@link #arrayHandler} method.
     */
    public static interface ArrayHandler {

        /**
         * Indicates whether an element of this ArrayHandler's primitive 
         * array is bad.
         * 
         * @param  pos    the position in array of the pixel to be tested.
         * @throws IndexOutOfBoundsException  if pos is outside the bounds 
         *                                    of the array
         */
        public boolean isBad( int pos );

        /**
         * Writes a single bad value into this ArrayHandler's array.
         *
         * @param  pos     the position at which to write the bad value
         * @throws IndexOutOfBoundsException  if access outside the 
         *                 bounds of the array is attempted
         */
        public void putBad( int pos );
    }

    private final Number badValue;
    private final Type type;

    /**
     * Constructor which may be used for subclassing.
     */
    protected BadHandler( Type type, Number badValue ) {
        this.type = type;
        this.badValue = badValue;
    }

    /**
     * Gets the Type of this handler.
     *
     * @return  the type 
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the bad data value.  This will be an instance of the primitive
     * wrapper class corresponding to this array's type.  Any elements
     * matching the number thus represented will be considered to be bad;
     * calling the isBad method on them will return true.  The value may
     * be null, in which case no data of integer type is considered bad,
     * but floating point values which are NaN still are.
     *
     * @return  a Number object of appropriate type wrapping the bad data value.
     *          May be null.
     */
    public Number getBadValue() {
        return badValue;
    }

    /**
     * Returns an instance of this class capable of dealing with primitives
     * of a given type and using a given bad value.
     *
     * @param   type      a Type object determining what kind of primitive
     *                    data this handler will deal with 
     * @param   badValue  a Number object giving the magic bad value.
     *                    This will be an instance of the primitive wrapper
     *                    class corresponding to this array's type.
     *                    The value may be null, in which case no data
     *                    of integer type is considered bad, but floating
     *                    point values which are NaN still are.
     *                    In this case a default bad value will be written
     *                    if the putBad methods are invoked
     * @return  a suitable BadHandler
     */
    public static BadHandler getHandler( Type type, Number badValue ) {
        if ( type == Type.BYTE ) {
            if ( equalValues( type, badValue, null ) ) {
                return BYTE_NULL_HANDLER;
            }
            else {
                final byte byteBad = ((Byte) badValue).byteValue();
                return new BadHandler( type, badValue ) {
                    public final boolean isBad( Object array, int pos ) {
                        return ((byte[]) array)[ pos ] == byteBad;
                    }
                    public final void putBad( Object array, int pos ) {
                        ((byte[]) array)[ pos ] = byteBad;
                    }
                    public final void putBad( Object array, int start, 
                                              int size ) {
                        Arrays.fill( (byte[]) array, start, start + size,
                                     byteBad );
                    }
                    public final Number makeNumber( Object array, int pos ) {
                        byte val = ((byte[]) array)[ pos ];
                        return ( val == byteBad ) ? null : new Byte( val );
                    }
                    public final ArrayHandler arrayHandler( final Object arr ) {
                        return new ArrayHandler() {
                            byte[] array = (byte[]) arr;
                            public boolean isBad( int pos ) {
                                return array[ pos ] == byteBad;
                            }
                            public void putBad( int pos ) {
                                array[ pos ] = byteBad;
                            }
                        };
                    }
                };
            }
        } 
        else if ( type == Type.SHORT ) {
            if ( equalValues( type, badValue, null) ) {
                return SHORT_NULL_HANDLER;
            }
            else {
                final short shortBad = ((Short) badValue).shortValue();
                return new BadHandler( type, badValue ) {
                    public final boolean isBad( Object array, int pos ) {
                        return ((short[]) array)[ pos ] == shortBad;
                    }
                    public final void putBad( Object array, int pos ) {
                        ((short[]) array)[ pos ] = shortBad;
                    }
                    public final void putBad( Object array, int start,
                                              int size ) {
                        Arrays.fill( (short[]) array, start, start + size,
                                     shortBad );
                    }
                    public final Number makeNumber( Object array, int pos ) {
                        short val = ((short[]) array)[ pos ];
                        return ( val == shortBad ) ? null : new Short( val );
                    }
                    public final ArrayHandler arrayHandler( final Object arr ) {
                        return new ArrayHandler() {
                            short[] array = (short[]) arr;
                            public boolean isBad( int pos ) {
                                return array[ pos ] == shortBad;
                            }
                            public void putBad( int pos ) {
                                array[ pos ] = shortBad;
                            }
                        };
                    }
                };
            }
        }
        else if ( type == Type.INT ) {
            if ( equalValues( type, badValue, null) ) {
                return INT_NULL_HANDLER;
            }
            else {
                final int intBad = ((Integer) badValue).intValue();
                return new BadHandler( type, badValue ) {
                    public final boolean isBad( Object array, int pos ) {
                        return ((int[]) array)[ pos ] == intBad;
                    }
                    public final void putBad( Object array, int pos ) {
                        ((int[]) array)[ pos ] = intBad;
                    }
                    public final void putBad( Object array, int start, 
                                              int size ) {
                        Arrays.fill( (int[]) array, start, start + size,
                                     intBad );
                    }
                    public final Number makeNumber( Object array, int pos ) {
                        int val = ((int[]) array)[ pos ];
                        return ( val == intBad ) ? null : new Integer( val );
                    }
                    public final ArrayHandler arrayHandler( final Object arr ) {
                        return new ArrayHandler() {
                            int[] array = (int[]) arr;
                            public boolean isBad( int pos ) {
                                return array[ pos ] == intBad;
                            }
                            public void putBad( int pos ) {
                                array[ pos ] = intBad;
                            }
                        };
                    }
                };
            }
        }
        else if ( type == Type.FLOAT ) {
            if ( equalValues( type, badValue, type.defaultBadValue() ) ) {
                return FLOAT_DEFAULT_HANDLER;
            }
            else {
                final float floatBad = ((Float) badValue).floatValue();
                return new BadHandler( type, badValue ) {
                    public final boolean isBad( Object array, int pos ) {
                        float val = ((float[]) array)[ pos ];
                        return val == floatBad || Float.isNaN( val );
                    }
                    public final void putBad( Object array, int pos ) {
                        ((float[]) array)[ pos ] = floatBad;
                    }
                    public final void putBad( Object array, int start,
                                              int size ) {
                        Arrays.fill( (float[]) array, start, start + size,
                                     floatBad );
                    }
                    public final Number makeNumber( Object array, int pos ) {
                        float val = ((float[]) array)[ pos ];
                        return ( val == floatBad || Float.isNaN( val ) ) 
                            ? null : new Float( val );
                    }
                    public final ArrayHandler arrayHandler( final Object arr ) {
                        return new ArrayHandler() {
                            float[] array = (float[]) arr;
                            public boolean isBad( int pos ) {
                                float val = array[ pos ];
                                return val == floatBad || Float.isNaN( val );
                            }
                            public void putBad( int pos ) {
                                array[ pos ] = floatBad;
                            }
                        };
                    }
                };
            }
        }
        else if ( type == Type.DOUBLE ) {
            if ( equalValues( type, badValue, type.defaultBadValue() ) ) {
                return DOUBLE_DEFAULT_HANDLER;
            }
            else {
                final double doubleBad = ((Double) badValue).doubleValue();
                return new BadHandler( type, badValue ) {
                    public final boolean isBad( Object array, int pos ) {
                        double val = ((double[]) array)[ pos ];
                        return val == doubleBad || Double.isNaN( val );
                    }
                    public final void putBad( Object array, int pos ) {
                        ((double[]) array)[ pos ] = doubleBad;
                    }
                    public final void putBad( Object array, int start,
                                              int size ) {
                        Arrays.fill( (double[]) array, start, start + size, 
                                     doubleBad );
                    }
                    public final Number makeNumber( Object array, int pos ) {
                        double val = ((double[]) array)[ pos ];
                        return ( val == doubleBad || Double.isNaN( val ) )
                            ? null : new Double( val );
                    }
                    public final ArrayHandler arrayHandler( final Object arr ) {
                        return new ArrayHandler() {
                            double[] array = (double[]) arr;
                            public boolean isBad( int pos ) {
                                double val = array[ pos ];
                                return val == doubleBad || Double.isNaN( val );
                            }
                            public void putBad( int pos ) {
                                array[ pos ] = doubleBad;
                            }
                        };
                    }
                };
            }
        }
        else if ( type == null ) {
            throw new NullPointerException();
        }
        else {
            throw new AssertionError( "Unknown type " + type );
        }
    }

    public boolean equals( Object other ) {
        return other != null 
            && this.getClass() == other.getClass() 
            && this.getType() == ((BadHandler) other).getType() 
            && equalValues( this.getType(), 
                            this.getBadValue(), 
                            ((BadHandler) other).getBadValue() );
    }

    public int hashCode() {
        return ( type.toString() + badValue.toString() ).hashCode();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer( "BadHandler:" );
        buf.append( type )
           .append( ':' )
           .append( badValue );
        return buf.toString();
    }

    private static boolean equalValues( Type type, Number bad1, Number bad2 ) {
        if ( type == Type.FLOAT ) {
            if ( bad1 == null ) {
                bad1 = type.defaultBadValue();
            }
            if ( bad2 == null ) {
                bad2 = type.defaultBadValue();
            }
        }
        else if ( type == Type.DOUBLE ) {
            if ( bad1 == null ) {
                bad1 = type.defaultBadValue();
            }
            if ( bad2 == null ) {
                bad2 = type.defaultBadValue();
            }
        }
        if ( bad1 == null && bad2 == null ) {
            return true;
        }
        else if ( bad1 == null || bad2 == null ) {
            return false;
        }
        else if ( bad1.getClass() != bad2.getClass() ) {
            return false;
        }
        else {
            if ( type == Type.BYTE ) {
                return ((Byte) bad1).byteValue()
                    == ((Byte) bad2).byteValue();
            }
            else if ( type == Type.SHORT ) {
                return ((Short) bad1).shortValue() 
                    == ((Short) bad2).shortValue();
            }
            else if ( type == Type.INT ) {
                return ((Integer) bad1).intValue()
                    == ((Integer) bad2).intValue();
            }
            else if ( type == Type.FLOAT ) {
                return Float.floatToRawIntBits( ((Float) bad1).floatValue() )
                    == Float.floatToRawIntBits( ((Float) bad2).floatValue() );
            }
            else if ( type == Type.DOUBLE ) {
                return 
                   Double.doubleToRawLongBits( ((Double) bad1).doubleValue() ) 
                == Double.doubleToRawLongBits( ((Double) bad2).doubleValue() );
            }
            else {
                throw new AssertionError( "Unknown type " + type );
            }
        }
    }


    private static abstract class NullHandler extends BadHandler {
        NullHandler( Type type ) {
            super( type, null );
        }
        final public boolean isBad( Object array, int pos ) {
            return false;
        }
    }

    private static abstract class NullArrayHandler implements ArrayHandler {
        final public boolean isBad( int pos ) {
            return false;
        }
        abstract public void putBad( int pos );
    }

    private static final BadHandler BYTE_NULL_HANDLER = 
        new NullHandler( Type.BYTE ) {
            private final byte byteBad = 
                Type.BYTE.defaultBadValue().byteValue();
            public final void putBad( Object array, int pos ) {
                ((byte[]) array)[ pos ] = byteBad;
            }
            public final void putBad( Object array, int start, int size ) {
                Arrays.fill( (byte[]) array, start, start + size, byteBad );
            }
            public final Number makeNumber( Object array, int pos ) {
                return new Byte( ((byte[]) array)[ pos ] );
            }
            public final ArrayHandler arrayHandler( final Object arr ) {
                return new NullArrayHandler() {
                    byte[] array = (byte[]) arr;
                    public void putBad( int pos ) {
                        array[ pos ] = byteBad;
                    }
                };
            }
        };
    private static final BadHandler SHORT_NULL_HANDLER = 
        new NullHandler( Type.SHORT ) {
            private final short shortBad = 
                Type.SHORT.defaultBadValue().shortValue();
            public final void putBad( Object array, int pos ) {
                ((short[]) array)[ pos ] = shortBad;
            }
            public final void putBad( Object array, int start, int size ) {
                Arrays.fill( (short[]) array, start, start + size, shortBad );
            }
            public final Number makeNumber( Object array, int pos ) {
                return new Short( ((short[]) array)[ pos ] );
            }
            public final ArrayHandler arrayHandler( final Object arr ) {
                return new NullArrayHandler() {
                    short[] array = (short[]) arr;
                    public void putBad( int pos ) {
                        array[ pos ] = shortBad;
                    }
                };
            }
        };
    private static final BadHandler INT_NULL_HANDLER = 
        new NullHandler( Type.INT ) {
            private final int intBad = 
                Type.INT.defaultBadValue().intValue();
            public final void putBad( Object array, int pos ) {
                ((int[]) array)[ pos ] = intBad;
            }
            public final void putBad( Object array, int start, int size ) {
                Arrays.fill( (int[]) array, start, start + size, intBad );
            }
            public final Number makeNumber( Object array, int pos ) {
                return new Integer( ((int[]) array)[ pos ] );
            }
            public final ArrayHandler arrayHandler( final Object arr ) {
                return new NullArrayHandler() {
                    int[] array = (int[]) arr;
                    public void putBad( int pos ) {
                        array[ pos ] = intBad;
                    }
                };
            }
        };
    private static final BadHandler FLOAT_DEFAULT_HANDLER =
        new BadHandler( Type.FLOAT, Type.FLOAT.defaultBadValue() ) {
            private final float floatBad = 
                Type.FLOAT.defaultBadValue().floatValue();
            public final boolean isBad( Object array, int pos ) {
                float val = ((float[]) array)[ pos ];
                return Float.isNaN( val );
            }
            public final void putBad( Object array, int pos ) {
                ((float[]) array)[ pos ] = floatBad;
            }
            public final void putBad( Object array, int start, int size ) {
                Arrays.fill( (float[]) array, start, start + size, floatBad );
            }
            public final Number makeNumber( Object array, int pos ) {
                float val = ((float[]) array)[ pos ];
                return Float.isNaN( val ) ? null : new Float( val );
            }
            public final ArrayHandler arrayHandler( final Object arr ) {
                return new ArrayHandler() {
                    float[] array = (float[]) arr;
                    public boolean isBad( int pos ) {
                        float val = array[ pos ];
                        return Float.isNaN( val );
                    }
                    public void putBad( int pos ) {
                        array[ pos ] = floatBad;
                    }
                };
            }
        };
    private static final BadHandler DOUBLE_DEFAULT_HANDLER = 
        new BadHandler( Type.DOUBLE, Type.DOUBLE.defaultBadValue() ) {
            private final double doubleBad =
                Type.DOUBLE.defaultBadValue().doubleValue();
            public final boolean isBad( Object array, int pos ) {
                double val = ((double[]) array)[ pos ];
                return Double.isNaN( val );
            }
            public final void putBad( Object array, int pos ) {
                ((double[]) array)[ pos ] = doubleBad;
            }
            public final void putBad( Object array, int start, int size ) {
                Arrays.fill( (double[]) array, start, start + size, doubleBad );
            }
            public final Number makeNumber( Object array, int pos ) {
                double val = ((double[]) array)[ pos ];
                return Double.isNaN( val ) ? null : new Double( val );
            }
            public final ArrayHandler arrayHandler( final Object arr ) {
                return new ArrayHandler() {
                    double[] array = (double[]) arr;
                    public boolean isBad( int pos ) {
                        double val = array[ pos ];
                        return Double.isNaN( val );
                    }
                    public void putBad( int pos ) {
                        array[ pos ] = doubleBad;
                    }
                };
            }
        };

}
