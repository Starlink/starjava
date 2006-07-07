package uk.ac.starlink.ttools.filter;

import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.Tables;

/**
 * Calculates univariate statistics for a variable.
 * Feed data to an instance of this object by repeatedly calling 
 * {@link #acceptDatum} and then call the various accessor methods to 
 * get accumulated values.
 *
 * @author   Mark Taylor
 * @since    27 Apr 2006
 */
public abstract class UnivariateStats {

    /**
     * Submits a single value to the statistics accumulator.
     * The submitted value should be of a type compatible with the 
     * class type of this Stats object.
     *
     * @param   value   value object
     */
    public abstract void acceptDatum( Object value );

    /**
     * Returns the number of good (non-null) values accumulated.
     *
     * @return good value count
     */
    public abstract long getCount();

    /**
     * Returns the numeric sum of values accumulated.
     * 
     * @return  sum of values
     */
    public abstract double getSum();

    /**
     * Returns the sum of squares of values accumulated.
     *
     * @return  sum of squared values
     */
    public abstract double getSum2();

    /**
     * Returns the sum of cubes of values accumulated.
     *
     * @return  sum of cubed values
     */
    public abstract double getSum3();

    /**
     * Returns the sum of fourth powers of values accumulated.
     *
     * @return  sum of fourth powers
     */
    public abstract double getSum4();

    /**
     * Returns the numeric minimum value submitted.
     *
     * @return  minimum
     */
    public abstract Number getMinimum();

    /**
     * Returns the numeric maximum value submitted.
     *
     * @return  maximum
     */
    public abstract Number getMaximum();

    /**
     * Returns the sequence number of the minimum value submitted.
     * Returns -1 if there is no minimum.
     *
     * @return   row index of minimum
     */
    public abstract long getMinPos();

    /**
     * Returns the sequence number of the maximum value submitted.
     * Returns -1 if there is no maximum.
     *
     * @return  row index of maximum
     */
    public abstract long getMaxPos();

    /**
     * Factory method to construct an instance of this class for accumulating
     * particular types of values.
     *
     * @param  clazz  class of which all submitted values will be instances of
     *         (if they're not null)
     */
    public static UnivariateStats createStats( Class clazz ) {
        if ( Number.class.isAssignableFrom( clazz ) ) {
            return new NumberStats();
        }
        else if ( clazz == Boolean.class ) {
            return new BooleanStats();
        }
        else {
            return new ObjectStats();
        }
    }

    /**
     * Stats implementation for Objects.
     */
    private static class ObjectStats extends UnivariateStats {
        private long nGood_;

        public void acceptDatum( Object obj ) {
            if ( ! Tables.isBlank( obj ) ) {
                nGood_++;
            }
        }

        public long getCount() {
            return nGood_;
        }

        public double getSum() {
            return Double.NaN;
        }

        public double getSum2() {
            return Double.NaN;
        }

        public double getSum3() {
            return Double.NaN;
        }

        public double getSum4() {
            return Double.NaN;
        }

        public Number getMinimum() {
            return null;
        }

        public Number getMaximum() {
            return null;
        }

        public long getMinPos() {
            return -1L;
        }

        public long getMaxPos() {
            return -1L;
        }
    }

    /**
     * Stats implementation for Boolean objects.
     */
    private static class BooleanStats extends UnivariateStats {
        private long nGood_;
        private long nTrue_;

        public void acceptDatum( Object obj ) {
            if ( obj instanceof Boolean ) {
                nGood_++;
                if ( ((Boolean) obj).booleanValue() ) {
                    nTrue_++;
                }
            }
        }

        public long getCount() {
            return nGood_;
        }

        public double getSum() {
            return (double) nTrue_;
        }

        public double getSum2() {
            return Double.NaN;
        }

        public double getSum3() {
            return Double.NaN;
        }

        public double getSum4() {
            return Double.NaN;
        }

        public Number getMinimum() {
            return null;
        }

        public Number getMaximum() {
            return null;
        }

        public long getMinPos() {
            return -1L;
        }

        public long getMaxPos() {
            return -1L;
        }
    }

    /**
     * Stats implementation for Number objects.
     */
    private static class NumberStats extends UnivariateStats {
        private long iDatum_;
        private long nGood_;
        private double sum1_;
        private double sum2_;
        private double sum3_;
        private double sum4_;
        private double dmin_ = Double.NaN;
        private double dmax_ = Double.NaN;
        private Number min_;
        private Number max_;
        private long minPos_ = -1L;
        private long maxPos_ = -1L;
        
        public void acceptDatum( Object obj ) {
            if ( obj instanceof Number ) {
                Number val = (Number) obj;
                double dval = val.doubleValue();
                if ( ! Double.isNaN( dval ) ) {
                    nGood_++;
                    double s1 = dval;
                    double s2 = dval * s1;
                    double s3 = dval * s2;
                    double s4 = dval * s3;
                    sum1_ += s1;
                    sum2_ += s2;
                    sum3_ += s3;
                    sum4_ += s4;
                    if ( ! ( dval >= dmin_ ) ) {  // note NaN handling
                        dmin_ = dval;
                        min_ = val;
                        minPos_ = iDatum_;
                    }
                    if ( ! ( dval <= dmax_ ) ) {  // note NaN handling
                        dmax_ = dval;
                        max_ = val;
                        maxPos_ = iDatum_;
                    }
                }
            }
            iDatum_++;
        }

        public long getCount() {
            return nGood_;
        }

        public double getSum() {
            return sum1_;
        }

        public double getSum2() {
            return sum2_;
        }

        public double getSum3() {
            return sum3_;
        }

        public double getSum4() {
            return sum4_;
        }

        public Number getMinimum() {
            return min_;
        }

        public Number getMaximum() {
            return max_;
        }

        public long getMinPos() {
            return minPos_;
        }

        public long getMaxPos() {
            return maxPos_;
        }
    }
}
