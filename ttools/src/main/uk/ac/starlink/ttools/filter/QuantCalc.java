package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
     * Returns the number of non-blank values accumulated by this calculator.
     *
     * @return  value count
     */
    public abstract long getValueCount();

    /**
     * Returns an iterator over all the non-blank values
     * accumulated by this calculator.
     * If {@link #ready} has been called, they will be in ascending order.
     * The number of values it iterates over will be equal to
     * the result of {@link #getValueCount}.
     *
     * @return   value iterator
     */
    public abstract Iterator<Number> getValueIterator();

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
        else if ( clazz == Integer.class ) {
            return new CountMapQuantCalc( Integer.class );
        }
        else if ( clazz == Long.class ) {
            return new CountMapQuantCalc( Long.class );
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
     * Calculates the median absolute deviation of the statistics
     * accumulated by a QuantCalc.
     *
     * @param   qcalc  calculator in ready state
     * @return   sum(abs(x_i - median))
     */
    public static double calculateMedianAbsoluteDeviation( QuantCalc qcalc )
            throws IOException {
        double median = qcalc.getQuantile( 0.5 ).doubleValue();
        QuantCalc madCalc =
            QuantCalc.createInstance( Double.class, qcalc.getValueCount() );
        for ( Iterator<Number> it = qcalc.getValueIterator(); it.hasNext(); ) {
            double val = it.next().doubleValue();
            madCalc.acceptDatum( Math.abs( val - median ) );
        }
        madCalc.ready();
        return madCalc.getQuantile( 0.5 ).doubleValue();
    }

    /**
     * QuantCalc implementation which uses an ArrayList of Number objects
     * to keep track of the accumulated data.  Not very efficient on
     * memory.
     */
    static class ObjectListQuantCalc extends QuantCalc {

        final Class clazz_;
        final List<Number> list_;

        /**
         * Constructor.
         *
         * @param   clazz  class of object data
         */
        public ObjectListQuantCalc( Class clazz ) {
            super( clazz );
            clazz_ = clazz;
            list_ = new ArrayList<Number>();
        }

        public void acceptDatum( Object obj ) {
            if ( obj != null && obj.getClass().equals( clazz_ ) ) {
                Number num = ((Number) obj);
                double dval = num.doubleValue();
                if ( ! Double.isNaN( dval ) ) {
                    list_.add( num );
                }
            }
        }

        public void ready() {
            Collections.sort( (List) list_ );
        }

        public long getValueCount() {
            return list_.size();
        }

        public Number getQuantile( double quant ) {
            return list_.isEmpty() 
                 ? null
                 : list_.get( Math.min( (int) ( quant * list_.size() ),
                                        list_.size() - 1 ) );
        }

        public Iterator<Number> getValueIterator() {
            return list_.iterator();
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

        public long getValueCount() {
            return irow_;
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

        public Iterator<Number> getValueIterator() {
            return new Iterator<Number>() {
                int i;
                public boolean hasNext() {
                    return i < irow_;
                }
                public Number next() {
                    return new Float( array_[ i++ ] );
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
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

        public long getValueCount() {
            return count_;
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

        public Iterator<Number> getValueIterator() {
            return new Iterator<Number>() {
                int is;
                int ic;
                Byte bval;
                public boolean hasNext() {
                    while ( ic == 0 && is < slots_.length ) {
                        bval = new Byte( (byte) ( is - offset_ ) );
                        ic = slots_[ is++ ];
                    }
                    return ic > 0;
                }
                public Number next() {
                    if ( hasNext() ) {
                        ic--;
                        return bval;
                    }
                    else {
                        throw new NoSuchElementException();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
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

        public long getValueCount() {
            return count_;
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

        public Iterator<Number> getValueIterator() {
            return new Iterator<Number>() {
                int is;
                int ic;
                Short sval;
                public boolean hasNext() {
                    while ( ic == 0 && is < slots_.length ) {
                        sval = new Short( (short) ( is - offset_ ) );
                        ic = slots_[ is++ ];
                    }
                    return ic > 0;
                }
                public Number next() {
                    if ( hasNext() ) {
                        ic--;
                        return sval;
                    }
                    else {
                        throw new NoSuchElementException();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * QuantCalc implementation intended for integers which uses a Map of
     * counts.
     */
    static class CountMapQuantCalc extends QuantCalc {
        private final Class clazz_;
        private Map<Number,Integer> countMap_;
        private long count_;
        private static final Integer ONE = new Integer( 1 );

        public CountMapQuantCalc( Class<? extends Number> clazz ) {
            super( clazz );
            clazz_ = clazz;
            countMap_ = new HashMap<Number,Integer>();
        }

        public void acceptDatum( Object obj ) {
            if ( obj != null && obj.getClass() == clazz_ &&
                 ! Tables.isBlank( obj ) ) {
                count_++;
                Number num = (Number) obj;
                Integer value = countMap_.get( obj );
                if ( value == null ) {
                    countMap_.put( num, ONE );
                }
                else {
                    countMap_.put( num, new Integer( value.intValue() + 1 ) );
                }
            }
        }

        public void ready() {
            countMap_ = new TreeMap<Number,Integer>( countMap_ );
        }

        public long getValueCount() {
            return count_;
        }

        public Number getQuantile( double quant ) {
            long point = Math.min( (long) ( quant * count_ ), count_ - 1 );
            long nval = 0;
            for ( Map.Entry<Number,Integer> entry : countMap_.entrySet() ) {
                nval += entry.getValue().intValue();
                if ( nval > point ) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public Iterator<Number> getValueIterator() {
            return new Iterator<Number>() {
                Iterator<Map.Entry<Number,Integer>> countIt =
                    countMap_.entrySet().iterator();
                int ic;
                Number num;
                public boolean hasNext() {
                    while ( ic == 0 && countIt.hasNext() ) {
                        Map.Entry<Number,Integer> entry = countIt.next();
                        num = entry.getKey();
                        ic = entry.getValue().intValue();
                    }
                    return ic > 0;
                }
                public Number next() {
                    if ( hasNext() ) {
                        ic--;
                        return num;
                    }
                    else {
                        throw new NoSuchElementException();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
