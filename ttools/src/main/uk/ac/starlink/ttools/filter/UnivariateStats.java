package uk.ac.starlink.ttools.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
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

    /** Maximum value for cardinality counters. */
    public static final int MAX_CARDINALITY = 100;

    /**
     * Submits a single value to the statistics accumulator.
     * The submitted value should be of a type compatible with the 
     * class type of this Stats object.
     *
     * @param   value   value object
     * @param   irow    row index of input value
     */
    public abstract void acceptDatum( Object value, long irow );

    /**
     * Adds the accumulated content of a second UnivariateStats object
     * to this one.
     *
     * @param   other  compatible UnivariateStats object
     */
    public abstract void addStats( UnivariateStats other );

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
     * Returns the numeric minimum value submitted, if applicable.
     *
     * @return  minimum
     */
    public abstract Comparable<?> getMinimum();

    /**
     * Returns the maximum value submitted, if applicable.
     *
     * @return  maximum
     */
    public abstract Comparable<?> getMaximum();

    /**
     * Returns the sequence number of the minimum value submitted.
     * Returns -1 if there is no minimum or if the sequence number is not known.
     *
     * @return   row index of minimum, or -1
     */
    public abstract long getMinPos();

    /**
     * Returns the sequence number of the maximum value submitted.
     * Returns -1 if there is no maximum or if the sequence number is not known.
     *
     * @return  row index of maximum, or -1
     */
    public abstract long getMaxPos();

    /**
     * Returns the number of distinct non-null values submitted, if known.
     * If the count was not collected, or if there were too many
     * different values to count, -1 is returned.
     *
     * @return  number of distinct non-null values, or -1
     */
    public abstract int getCardinality();

    /**
     * Returns a quantiler ready to provide quantile values, or null
     * if quantiles were not gathered.
     * If a non-null quantiler is returned, the {@link Quantiler#ready}
     * value will have been called on it.
     *
     * @return  ready quantiler, or null
     */
    public abstract Quantiler getQuantiler();

    /**
     * Returns an object containing statistics applicable to
     * numeric-array-valued columns.
     *
     * @return  array stats object, or null
     */
    public abstract ArrayStats getArrayStats();

    /**
     * Factory method to construct an instance of this class for accumulating
     * particular types of values.
     *
     * @param  clazz  class of which all submitted values will be instances of
     *         (if they're not null)
     * @param  qSupplier  supplier for an object that can calculate quantiles,
     *                    or null if quantiles are not required
     * @param  doCard   true if an attempt is to be made to count
     *                  distinct values
     * @return   stats accumulator
     */
    public static UnivariateStats createStats( Class<?> clazz,
                                               Supplier<Quantiler> qSupplier,
                                               boolean doCard ) {
 
        if ( Number.class.isAssignableFrom( clazz ) ) {
            return new NumberStats( qSupplier, doCard );
        }
        else if ( clazz == Boolean.class ) {
            return new BooleanStats();
        }
        else if ( ArrayReader.forClass( clazz ) != null ) {
            return new NumericArrayStats( ArrayReader.forClass( clazz ) );
        }
        else {
            boolean doCompare = Comparable.class.isAssignableFrom( clazz );
            return new ObjectStats( doCard, doCompare );
        }
    }

    /**
     * Utility method to compare comparables.
     * Generics rules makes it a pain to do inline.
     *
     * @return  -1, 0, or 1 for lessthan, equal or greater than
     * @throws  ClassCastException  in case of a comparison failure
     */
    private static int compare( Comparable<?> c1, Comparable<?> c2 ) {
        @SuppressWarnings("unchecked")
        int cmp = ((Comparable) c1).compareTo( (Comparable) c2 );
        return cmp;
    }

    /**
     * Aggregates statistics acquired from a column whose values are
     * fixed-length numeric arrays.
     */
    public interface ArrayStats {

        /**
         * Returns array length.
         *
         * @return  fixed array length
         */
        int getLength();

        /**
         * Returns a per-element count of the number of non-blank elements
         * of submitted arrays.
         *
         * @return  n-value array of good value counts
         */
        long[] getCounts();

        /**
         * Returns a per-element sum of the non-blank elements
         * of submitted arrays.
         *
         * @return  n-value array of per-element sums
         */
        double[] getSum1s();

        /**
         * Returns a per-element sum of squares of the non-blank elements
         * of submitted arrays.
         *
         * @return  n-value array of per-element sums of squares
         */
        double[] getSum2s();
    }

    /**
     * Stats implementation for Objects.
     */
    private static class ObjectStats extends UnivariateStats {
        private boolean doCompare_;
        private long nGood_;
        private Set<Object> distincts_;
        private int ndistinct_;
        private final int maxCard_;
        private Comparable<?> min_;
        private Comparable<?> max_;
        private long minPos_ = -1L;
        private long maxPos_ = -1L;

        /**
         * Constructor.
         *
         * @param  doCard   whether to count distinct values
         * @param  doCompare  whether to try to find min/max values
         */
        ObjectStats( boolean doCard, boolean doCompare ) {
            doCompare_ = doCompare;
            distincts_ = doCard ? new HashSet<Object>() : null;
            maxCard_ = MAX_CARDINALITY;
        }

        public void acceptDatum( Object obj, long irow ) {
            if ( ! Tables.isBlank( obj ) ) {
                nGood_++;
                if ( distincts_ != null ) {
                    if ( ndistinct_ < maxCard_ ) {
                        if ( distincts_.add( obj ) ) {
                            ndistinct_++;
                        }
                    }
                    else {
                        distincts_ = null;
                    }
                }
                if ( doCompare_ && obj instanceof Comparable<?> ) {
                    try {
                        Comparable<?> cobj = (Comparable<?>) obj;
                        if ( min_ == null || compare( cobj, min_ ) < 0 ) {
                            min_ = cobj;
                            minPos_ = irow;
                        }
                        if ( max_ == null || compare( cobj, max_ ) > 0 ) {
                            max_ = cobj;
                            maxPos_ = irow;
                        }
                    }

                    /* Comparison can result in a ClassCastException.
                     * If that happens, bail out of attempting comparisons;
                     * hitting this exception every row could be very
                     * expensive and the results would be questionable. */
                    catch ( ClassCastException e ) {
                        doCompare_ = false;
                    }
                }
            }
        }

        public void addStats( UnivariateStats o ) {
            ObjectStats other = (ObjectStats) o;
            nGood_ += other.nGood_;
            doCompare_ = doCompare_ && other.doCompare_;
            if ( doCompare_ ) {
                try {
                    if ( other.min_ != null &&
                         ( min_ == null || compare( other.min_, min_ ) < 0 ) ) {
                        min_ = other.min_;
                        minPos_ = other.minPos_;
                    }
                    if ( other.max_ != null &&
                         ( max_ == null || compare( other.max_, max_ ) > 0 ) ) {
                        max_ = other.max_;
                        maxPos_ = other.maxPos_;
                    }
                }
                catch ( ClassCastException e ) {
                    doCompare_ = false;
                }
            }
            if ( distincts_ != null ) {
                if ( other.distincts_ != null ) {
                    distincts_.addAll( other.distincts_ );
                }
                ndistinct_ = distincts_.size();
                if ( ndistinct_ > maxCard_ ) {
                    distincts_ = null;
                }
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

        public Comparable<?> getMinimum() {
            return doCompare_ ? min_ : null;
        }

        public Comparable<?> getMaximum() {
            return doCompare_ ? max_ : null;
        }

        public long getMinPos() {
            return doCompare_ ? minPos_ : -1L;
        }

        public long getMaxPos() {
            return doCompare_ ? maxPos_ : -1L;
        }

        public int getCardinality() {
            return distincts_ == null ? -1 : distincts_.size();
        }

        public Quantiler getQuantiler() {
            return null;
        }

        public ArrayStats getArrayStats() {
            return null;
        }
    }

    /**
     * Stats implementation for Boolean objects.
     */
    private static class BooleanStats extends UnivariateStats {
        private long nGood_;
        private long nTrue_;

        public void acceptDatum( Object obj, long irow ) {
            if ( obj instanceof Boolean ) {
                nGood_++;
                if ( ((Boolean) obj).booleanValue() ) {
                    nTrue_++;
                }
            }
        }

        public void addStats( UnivariateStats o ) {
            BooleanStats other = (BooleanStats) o;
            nGood_ += other.nGood_;
            nTrue_ += other.nTrue_;
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

        public Comparable<?> getMinimum() {
            return null;
        }

        public Comparable<?> getMaximum() {
            return null;
        }

        public long getMinPos() {
            return -1L;
        }

        public long getMaxPos() {
            return -1L;
        }

        public int getCardinality() {
            int card = 0;
            if ( nTrue_ > 0 ) {
                card++;
            }
            if ( nGood_ > nTrue_ ) {
                card++;
            }
            return card;
        }

        public Quantiler getQuantiler() {
            return null;
        }

        public ArrayStats getArrayStats() {
            return null;
        }
    }

    /**
     * Stats implementation for Number objects.
     */
    private static class NumberStats extends UnivariateStats {
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
        private final Quantiler quantiler_;
        private Set<Object> distincts_;
        private int ndistinct_;
        private final int maxCard_;

        /**
         * Constructor.
         *
         * @param  qSupplier  quantile supplier if quantiles are required,
         *                    null if they are not
         * @param  doCard  whether to try to count distinct values
         */
        public NumberStats( Supplier<Quantiler> qSupplier, boolean doCard ) {
            quantiler_ = qSupplier == null ? null : qSupplier.get();
            distincts_ = doCard ? new HashSet<Object>() : null;
            maxCard_ = MAX_CARDINALITY;
        }

        public void acceptDatum( Object obj, long irow ) {
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
                        minPos_ = irow;
                    }
                    if ( ! ( dval <= dmax_ ) ) {  // note NaN handling
                        dmax_ = dval;
                        max_ = val;
                        maxPos_ = irow;
                    }
                    if ( distincts_ != null ) {
                        if ( ndistinct_ < maxCard_ ) {
                            if ( distincts_.add( val ) ) {
                                ndistinct_++;
                            }
                        }
                        else {
                            distincts_ = null;
                        }
                    }
                    if ( quantiler_ != null ) {
                        quantiler_.acceptDatum( dval );
                    }
                }
            }
        }

        public void addStats( UnivariateStats o ) {
            NumberStats other = (NumberStats) o;
            nGood_ += other.nGood_;
            sum1_ += other.sum1_;
            sum2_ += other.sum2_;
            sum3_ += other.sum3_;
            sum4_ += other.sum4_;
            if ( ! Double.isNaN( other.dmin_ ) && ! ( other.dmin_ >= dmin_ ) ) {
                dmin_ = other.dmin_;
                min_ = other.min_;
                minPos_ = other.minPos_;
            }
            if ( ! Double.isNaN( other.dmax_ ) && ! ( other.dmax_ <= dmax_ ) ) {
                dmax_ = other.dmax_;
                max_ = other.max_;
                maxPos_ = other.maxPos_;
            }
            if ( distincts_ != null ) {
                if ( other.distincts_ != null ) {
                    distincts_.addAll( other.distincts_ );
                }
                ndistinct_ = distincts_.size();
                if ( ndistinct_ > maxCard_ ) {
                    distincts_ = null;
                }
            }
            if ( quantiler_ != null ) {
                quantiler_.addQuantiler( other.quantiler_ );
            }
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

        public Comparable<?> getMinimum() {
            return min_ instanceof Comparable ? (Comparable<?>) min_ : null;
        }

        public Comparable<?> getMaximum() {
            return max_ instanceof Comparable ? (Comparable<?>) max_ : null;
        }

        public long getMinPos() {
            return minPos_;
        }

        public long getMaxPos() {
            return maxPos_;
        }

        public int getCardinality() {
            return distincts_ == null ? -1 : distincts_.size();
        }

        public Quantiler getQuantiler() {
            if ( quantiler_ != null ) {
                quantiler_.ready();
            }
            return quantiler_;
        }

        public ArrayStats getArrayStats() {
            return null;
        }
    }

    /**
     * Stats implementation for numeric arrays.
     */
    private static class NumericArrayStats extends UnivariateStats {

        private final ArrayReader ardr_;
        private int aleng_;
        private long nGood_;
        private long[] sum0s_;
        private double[] sum1s_;
        private double[] sum2s_;

        /**
         * Constructor.
         *
         * @param  ardr  reader for the expected type of array data
         */
        public NumericArrayStats( ArrayReader ardr ) {
            ardr_ = ardr;
        }

        public void acceptDatum( Object array, long irow ) {

            /* Determine the length of the submitted array.
             * If it's not a suitable array, this value will be -1. */
            int leng = ardr_.getLength( array );

            /* Only accumulate as long as all the submitted arrays are
             * the same length (ignoring empty and non-arrays). */
            if ( leng > 0 && aleng_ >= 0 ) {

                /* If we haven't seen a good array yet, initialise the
                 * fixed length. */
                if ( aleng_ == 0 ) {
                    initLeng( leng );
                }

                /* If the length matches the fixed length, accumulate. */
                if ( leng == aleng_ ) {
                    boolean hasGood = false;
                    for ( int i = 0; i < leng; i++ ) {
                        double d = ardr_.getValue( array, i );
                        if ( !Double.isNaN( d ) ) {
                            double d2 = d * d;
                            hasGood = true;
                            sum0s_[ i ]++;
                            sum1s_[ i ] += d;
                            sum2s_[ i ] += d2;
                        }
                    }
                    if ( hasGood ) {
                        nGood_++;
                    }
                }

                /* If we've seen two different lengths, give up. */
                else {
                    initLeng( -1 );
                }
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

        public Comparable<?> getMinimum() {
            return null;
        }

        public Comparable<?> getMaximum() {
            return null;
        }

        public long getMinPos() {
            return -1L;
        }

        public long getMaxPos() {
            return -1L;
        }

        public int getCardinality() {
            return -1;
        }

        public Quantiler getQuantiler() {
            return null;
        }

        public ArrayStats getArrayStats() {
            return aleng_ > 0
                 ? new ArrayStats() {
                       public int getLength() {
                           return aleng_;
                       }
                       public long[] getCounts() {
                           return sum0s_;
                       }
                       public double[] getSum1s() {
                           return sum1s_;
                       }
                       public double[] getSum2s() {
                           return sum2s_;
                       }
                   }
                 : null;
        }

        public void addStats( UnivariateStats o ) {
            NumericArrayStats other = (NumericArrayStats) o;

            /* Failed, no merge required. */
            if ( aleng_ < 0 ) {
                return;
            }

            /* Other has failed, mark this one as failed. */
            else if ( other.aleng_ < 0 ) {
                initLeng( -1 );
                return;
            }

            /* No results from other, no merge required. */
            else if ( other.aleng_ == 0 ) {
                return;
            }
            else {
                assert other.aleng_ > 0;

                /* No results from this one, prepare to merge from other. */
                if ( aleng_ == 0 ) {
                    initLeng( other.aleng_ );
                }

                /* Both have results; merge other to this. */
                nGood_ += other.nGood_;
                for ( int i = 0; i < aleng_; i++ ) {
                    sum0s_[ i ] += other.sum0s_[ i ];
                    sum1s_[ i ] += other.sum1s_[ i ];
                    sum2s_[ i ] += other.sum2s_[ i ];
                }
            }
        }

        /**
         * Prepare to accumulate values for a given fixed array length.
         *
         * @param  leng  if positive, fixed length for accumulation;
         *               if negative, no further results will be accumulated
         */
        private void initLeng( int leng ) {
            aleng_ = leng;    
            if ( leng > 0 ) {
                sum0s_ = new long[ leng ];
                sum1s_ = new double[ leng ];
                sum2s_ = new double[ leng ];
            }
            else if ( leng < 0 ) {
                sum0s_ = null;
                sum1s_ = null;
                sum2s_ = null;
                nGood_ = 0;
            }
            else {
                assert false;
            }
        }
    }
}
