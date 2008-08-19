package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import uk.ac.starlink.table.Tables;

/**
 * Object for accumulating values in order to calculate quantiles.
 *
 * @author   Mark Taylor
 * @since    2 May 2006
 */
public abstract class QuantCalc {

    private final Class clazz_;

    /**
     * Constructor.
     *
     * @param  clazz  class of data objects which will be submitted;
     *          must be assignable from Number class
     */
    protected QuantCalc( Class clazz ) {
        if ( ! Number.class.isAssignableFrom( clazz ) ) {
            throw new IllegalArgumentException( clazz + " not number" );
        }
        clazz_ = clazz;
    }

    /**
     * Submits a datum for accumulation.
     *
     * @param    obj  data object - must be instance of numeric class 
     *           suitable for this accumulator
     */
    public abstract void acceptDatum( Object obj );

    /**
     * Call this method after all {@link #acceptDatum} calls have been made
     * and before any call to {@link #getQuantile}.
     */
    public abstract void ready();

    /**
     * Returns a quantile corresponding to a given point.
     *
     * @param  quant  quant value between 0 and 1, 
     *         for instance 0.5 indicates median
     */
    public abstract Number getQuantile( double quant );

    /**
     * Factory method to create a quantile accumulator for a given 
     * row count and value class.
     *
     * @param  nrow  row count; may be -1 to indicate that the row count
     *         is unknown
     * @param  clazz  class of data objects which will be submitted;
     *         must be assignable from Number.class.
     */
    public static QuantCalc createInstance( Class clazz, long nrow )
            throws IOException {
        if ( clazz == Byte.class ) {
            return new ByteSlotQuantCalc();
        }
        else if ( clazz == Short.class && ( nrow < 0 || nrow > ( 1 << 16 ) ) ) {
            return new ShortSlotQuantCalc();
        }
        else if ( clazz == Integer.class || clazz == Long.class ) {
            return new CountMapQuantCalc( clazz );
        }
        else if ( nrow >= 0 && nrow < Integer.MAX_VALUE ) {
            return new FloatArrayQuantCalc( clazz, (int) nrow );
        }
        else if ( nrow >= Integer.MAX_VALUE ) {
            throw new IOException( "Sorry, too many rows for quantile " +
                                   "calculation (" + nrow + " > " +
                                   Integer.MAX_VALUE );
        }
        else {
            return new ObjectListQuantCalc( clazz );
        }
    }

    /**
     * QuantCalc implementation which uses an ArrayList of Number objects
     * to keep track of the accumulated data.  Not very efficient on
     * memory.
     */
    static class ObjectListQuantCalc extends QuantCalc {

        final Class clazz_;
        final List list_;

        /**
         * Constructor.
         *
         * @param   clazz  class of object data
         */
        public ObjectListQuantCalc( Class clazz ) {
            super( clazz );
            clazz_ = clazz;
            list_ = new ArrayList();
        }

        public void acceptDatum( Object obj ) {
            if ( obj != null && obj.getClass().equals( clazz_ ) ) {
                double dval = ((Number) obj).doubleValue();
                if ( ! Double.isNaN( dval ) ) {
                    list_.add( obj );
                }
            }
        }

        public void ready() {
            Collections.sort( list_ );
        }

        public Number getQuantile( double quant ) {
            return list_.isEmpty() 
                 ? null
                 : (Number) list_.get( Math.min( (int) ( quant * list_.size() ),
                                                 list_.size() - 1 ) );
        }
    }

    /**
     * QuantCalc implementation which uses a float[] array.
     */
    static class FloatArrayQuantCalc extends QuantCalc {

        final float[] array_;
        final Class clazz_;
        int irow_;

        public FloatArrayQuantCalc( Class clazz, int nrow ) {
            super( clazz );
            clazz_ = clazz;
            array_ = new float[ nrow ];
        }

        public void acceptDatum( Object obj ) {
            if ( irow_ < array_.length && obj instanceof Number ) {
                float fval = ((Number) obj).floatValue();
                if ( ! Float.isNaN( fval ) ) {
                    array_[ irow_++ ] = fval;
                }
            }
        }

        public void ready() {
            Arrays.sort( array_, 0, irow_ );
        }

        public Number getQuantile( double quant ) {
            if ( irow_ == 0 ) {
                return null;
            }
            float quantile = array_[ Math.min( (int) ( quant * irow_ ),
                                               irow_ - 1 ) ]; 
            if ( clazz_ == Float.class || clazz_ == Double.class ) {
                return new Float( quantile );
            }
            else if ( clazz_ == Byte.class ) {
                return new Byte( (byte) quantile );
            }
            else if ( clazz_ == Short.class ) {
                return new Short( (short) quantile );
            }
            else if ( clazz_ == Integer.class ) {
                return new Integer( (int) quantile );
            }
            else if ( clazz_ == Long.class ) {
                return new Long( (long) quantile );
            }
            else {
                return null;
            }
        }
    }

    /**
     * QuantCalc implementation for Byte types which uses a frequency 
     * count array.
     */
    static class ByteSlotQuantCalc extends QuantCalc {
        private final int offset_ = 1 << 7;
        private final int[] slots_;
        private long count_;

        public ByteSlotQuantCalc() {
            super( Byte.class );
            slots_ = new int[ offset_ * 2 ];
        }

        public void acceptDatum( Object obj ) {
            if ( obj instanceof Byte ) {
                byte bval = ((Byte) obj).byteValue();
                count_++;
                slots_[ bval + offset_ ]++;
            }
        }

        public void ready() {
        }

        public Number getQuantile( double quant ) {
            long point = Math.min( (long) ( quant * count_ ), count_ - 1 );
            long nval = 0;
            for ( byte bval = (byte) (-offset_); bval < offset_; bval++ ) {
                nval += slots_[ bval + offset_ ];
                if ( nval > point ) {
                    return new Byte( bval );
                }
            }
            return null;
        }
    }

    /**
     * QuantCalc implementation for Short types which uses a frequency 
     * count array.
     */
    static class ShortSlotQuantCalc extends QuantCalc {
        private final int offset_ = 1 << 15;
        private final int[] slots_;
        private long count_;

        public ShortSlotQuantCalc() {
            super( Short.class );
            slots_ = new int[ offset_ * 2 ];
        }

        public void acceptDatum( Object obj ) {
            if ( obj instanceof Short ) {
                short sval = ((Short) obj).shortValue();
                count_++;
                slots_[ sval + offset_ ]++;
            }
        }

        public void ready() {
        }

        public Number getQuantile( double quant ) {
            long point = Math.min( (long) ( quant * count_ ), count_ - 1 );
            long nval = 0;
            for ( short sval = (short) (-offset_); sval < offset_; sval++ ) {
                nval += slots_[ sval + offset_ ];
                if ( nval > point ) {
                    return new Short( sval );
                }
            }
            return null;
        }
    }

    /**
     * QuantCalc implementation intended for integers which uses a Map of
     * counts.
     */
    static class CountMapQuantCalc extends QuantCalc {
        private final Class clazz_;
        private Map countMap_;
        private long count_;
        private static final Integer ONE = new Integer( 1 );

        public CountMapQuantCalc( Class clazz ) {
            super( clazz );
            clazz_ = clazz;
            countMap_ = new HashMap();
        }

        public void acceptDatum( Object obj ) {
            if ( obj != null && obj.getClass() == clazz_ &&
                 ! Tables.isBlank( obj ) ) {
                count_++;
                Integer value = (Integer) countMap_.get( obj );
                if ( value == null ) {
                    countMap_.put( obj, ONE );
                }
                else {
                    countMap_.put( obj, new Integer( value.intValue() + 1 ) );
                }
            }
        }

        public void ready() {
            countMap_ = new TreeMap( countMap_ );
        }

        public Number getQuantile( double quant ) {
            long point = Math.min( (long) ( quant * count_ ), count_ - 1 );
            long nval = 0;
            for ( Iterator it = countMap_.entrySet().iterator();
                  it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                nval += ((Integer) entry.getValue()).intValue();
                if ( nval > point ) {
                    return (Number) entry.getKey();
                }
            }
            return null;
        }
    }
}
