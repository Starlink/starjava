package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * @param   class of data objects which will be submitted;
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
     * @return   obj  data object - must be instance of numeric class 
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
    public static QuantCalc createInstance( long nrow, Class clazz ) {
        return new ObjectListQuantCalc( clazz );
    }

    /**
     * QuantCalc implementation which uses an ArrayList of Number objects
     * to keep track of the accumulated data.  Not very efficient on
     * memory.
     */
    private static class ObjectListQuantCalc extends QuantCalc {

        final List list_ = new ArrayList();
        final Class clazz_;

        /**
         * Constructor.
         *
         * @param   clazz  class of object data
         */
        private ObjectListQuantCalc( Class clazz ) {
            super( clazz );
            clazz_ = clazz;
        }

        public void acceptDatum( Object obj ) {
            if ( obj != null && obj.getClass().equals( clazz_ ) ) {
                double dval = ((Number) obj).doubleValue();
                if ( dval >= - Double.MAX_VALUE && dval <= Double.MAX_VALUE ) {
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
}
