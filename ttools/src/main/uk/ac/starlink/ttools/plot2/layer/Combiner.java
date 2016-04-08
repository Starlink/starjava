package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import java.util.BitSet;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the combination mode for accumulating values into a bin.
 *
 * <p>Note that the {@link #SUM} mode is usually sensible for unweighted values,
 * but if the values are weighted it may be more revealing to use
 * one of the others (like {@link #MEAN}).
 *
 * @author    Mark Taylor
 * @since     20 Sep 2015
 */
@Equality
public abstract class Combiner {

    private final String name_;
    private final String description_;

    /** Report the number of submitted values. */
    public static final Combiner COUNT;

    /** Calculate the sum of all submitted values. */
    public static final Combiner SUM;

    /** Calculate the mean of all submitted values. */
    public static final Combiner MEAN;

    /** Calculate the median of all submitted values (slow). */
    public static final Combiner MEDIAN;

    /** Calculate the sample variance of all submitted values. */
    public static final Combiner SAMPLE_VARIANCE;

    /** Calculate the minimum of all submitted values. */
    public static final Combiner MIN;

    /** Calculate the maximum of all submitted values. */
    public static final Combiner MAX;

    /** Return 1 if any value submitted, 0 otherwise. */
    public static final Combiner HIT;

    private static final Combiner[] COMBINERS = new Combiner[] {
        SUM = new SumCombiner(),
        MEAN = new MeanCombiner(),
        MEDIAN = new MedianCombiner(),
        MIN = new MinCombiner(),
        MAX = new MaxCombiner(),
        SAMPLE_VARIANCE = new VarianceCombiner( true ),
        COUNT = new CountCombiner(),
        HIT = new HitCombiner(),
    };

    /**
     * Constructor.
     *
     * @param   name  name
     * @param   description  short textual description
     */
    protected Combiner( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Creates an object which can be used to accumulate values.
     *
     * <p><strong>Note:</strong> Since many container instances may
     * by generated (when using a HashBinList) it is desirable to
     * keep the returned objects as small as possible.
     * In particular, it's a good idea to make the returned objects
     * instances of a static class, to avoid an unncecessary reference
     * to the owner object, unless there's a really compelling reason
     * to do otherwise.
     *
     * @return  new container
     */
    public abstract Container createContainer();

    /**
     * May be able to create a bin list suitable for non-sparse,
     * moderate-sized index ranges.
     * If a combiner implementation is able to provide an ArrayBinList
     * implementation that should be significantly more efficient
     * than a HashBinList, this method should return it.
     * If not, it can return null.
     *
     * @param  size   index range of required bin list
     * @return   array-based bin list, or null
     */
    public abstract BinList createArrayBinList( int size );

    /**
     * Creates a generaly purpose bin list, which may be especially
     * suitable for sparse index ranges.
     *
     * @param  size   index range of required bin list
     * @return   array-based bin list, not null
     */
    public abstract BinList createHashBinList( long size );

    /**
     * Returns this combiner's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short textual description of this combiner.
     *
     * @return  short description
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of the known general purpose instances of this class.
     *
     * @return   combiner list
     */
    public static Combiner[] getKnownCombiners() {
        return COMBINERS.clone();
    }

    /**
     * Defines an object that can be used to accumulate values and
     * retrieve a result.
     */
    public interface Container {

        /**
         * Submits a new numeric value for accumulation to the result.
         * In general, NaN values should not be submitted.
         *
         * @param  datum  new value to accumulate
         */
        void submit( double datum );

        /**
         * Returns the combined result of all the values submitted so far.
         * In general, if no values have been submitted,
         * a NaN should be returned.
         *
         * @return  combined value of all submitted data
         */
        double getResult();
    }

    /**
     * Utility partial implementation of Combiner.
     * This just handles the isCopyResult flag.
     */
    private static abstract class AbstractCombiner extends Combiner {
        final boolean isCopyResult_;

        /**
         * Constructor.
         *
         * @param   name  name
         * @param   description  short textual description
         * @param   isCopyResult  whether to use a copy for Result creation;
         *                        set true for bins using more than 8 bytes,
         *                        false otherwise
         */
        AbstractCombiner( String name, String description,
                          boolean isCopyResult ) {
            super( name, description );
            isCopyResult_ = isCopyResult;
        }

        public BinList createHashBinList( long size ) {
            return new HashBinList( size, this, isCopyResult_ );
        }
    }

    /**
     * Combiner implementation that calculates the mean.
     */
    private static class MeanCombiner extends AbstractCombiner {

        /**
         * Constructor.
         */
        public MeanCombiner() {
            super( "mean", "the mean of the combined values", true );
        }

        public BinList createArrayBinList( int size ) {
            final int[] counts = new int[ size ];
            final double[] sums = new double[ size ];
            return new ArrayBinList( size, this, isCopyResult_ ) {
                public void submitToBinInt( int index, double value ) {
                    counts[ index ]++;
                    sums[ index ] += value;
                }
                public double getBinResultInt( int index ) {
                    int count = counts[ index ];
                    return count == 0 ? Double.NaN
                                      : sums[ index ] / (double) count;
                }
            };
        }

        public Container createContainer() {
            return new MeanContainer();
        }

        /**
         * Container that holds a count and a sum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MeanContainer implements Container {
            int count_;
            double sum_;
            public void submit( double datum ) {
                count_++;
                sum_ += datum;
            }
            public double getResult() {
                return count_ == 0 ? Double.NaN : sum_ / (double) count_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the median.
     */
    private static class MedianCombiner extends QuantileCombiner {
        MedianCombiner() {
            super( "median",
                   "the median of the combined values (may be slow)",
                   new QuantileCombiner.Quantiler() {
                       public double calculateValue( double[] sortedValues ) {
                           int nv = sortedValues.length;
                           if ( nv % 2 == 1 ) {
                               return sortedValues[ nv / 2 ];
                           }
                           else if ( nv > 0 ) {
                               int nv2 = nv / 2;
                               return 0.5 * ( sortedValues[ nv2 - 1 ]
                                            + sortedValues[ nv2 ] );
                           }
                           else {
                               return Double.NaN;
                           }
                       }
                   } );
        }
    }

    /**
     * Combiner implementation that calculates the variance.
     */
    private static class VarianceCombiner extends AbstractCombiner {
        private final boolean isSampleVariance_;

        /**
         * Constructor.
         *
         * @param  isSampleVariance  false for population variance,
         *                           true for sample variance,
         */
        public VarianceCombiner( boolean isSampleVariance ) {
            super( "variance",
                   "the " + ( isSampleVariance ? "sample" : "population" )
                          + " variance of the combined values",
                   true );
            isSampleVariance_ = isSampleVariance;
        }

        public BinList createArrayBinList( int size ) {
            final int[] counts = new int[ size ];
            final double[] sum1s = new double[ size ];
            final double[] sum2s = new double[ size ];
            return new ArrayBinList( size, this, isCopyResult_ ) {
                public void submitToBinInt( int index, double value ) {
                    counts[ index ]++;
                    sum1s[ index ] += value;
                    sum2s[ index ] += value * value;
                }
                public double getBinResultInt( int index ) {
                    return getVariance( isSampleVariance_, counts[ index ],
                                        sum1s[ index ], sum2s[ index ] );
                }
            };
        }

        public Container createContainer() {
            return isSampleVariance_ ? new SampleVarianceContainer()
                                     : new PopulationVarianceContainer();
        }

        /**
         * Partial Container implementation for calculating variance-like
         * quantities.
         */
        private static abstract class VarianceContainer implements Container {
            int count_;
            double sum1_;
            double sum2_;
            public void submit( double datum ) {
                count_++;
                sum1_ += datum;
                sum2_ += datum * datum;
            }
        }

        /**
         * Container to calculate a population variance.
         * Note that this is a static class with no unnecessary members
         * to keep memory usage down if there are many instances.
         */
        private static class PopulationVarianceContainer
                extends VarianceContainer {
            public double getResult() {
                return getVariance( false, count_, sum1_, sum2_ );
            }
        }

        /**
         * Container to calculate a sample variance.
         * Note that this is a static class with no unnecessary members
         * to keep memory usage down if there are many instances.
         */
        private static class SampleVarianceContainer
                extends VarianceContainer {
            public double getResult() {
                return getVariance( true, count_, sum1_, sum2_ );
            }
        }

        /**
         * Utility method to calculate a population or sample variance
         * from the relevant accumulated quantities.
         *
         * @param  isSampleVariance   false for population variance,
         *                            true for sample variance
         * @param  count  number of accumulated values
         * @param  sum1   sum of accumulated values
         * @param  sum2   sum of squares of accumulated values
         * @return  variance, or NaN if insufficient data
         */
        private static double getVariance( boolean isSampleVariance, int count,
                                           double sum1, double sum2 ) {
            if ( count < ( isSampleVariance ? 2 : 1 ) ) {
                return Double.NaN;
            }
            else {
                double dcount = (double) count;
                double nvar = sum2 - sum1 * sum1 / dcount;
                return nvar / ( isSampleVariance ? ( dcount - 1 ) : dcount );
            }
        }
    }

    /**
     * Combiner instance that just counts submissions.
     */
    private static class CountCombiner extends AbstractCombiner {

        /**
         * Constructor.
         */
        CountCombiner() {
            super( "count",
                   "the number of non-blank values (weight is ignored)",
                   false );
        }

        public BinList createArrayBinList( int size ) {
            final int[] counts = new int[ size ];
            return new ArrayBinList( size, this, isCopyResult_ ) {
                public void submitToBinInt( int index, double value ) {
                    counts[ index ]++;
                }
                public double getBinResultInt( int index ) {
                    int count = counts[ index ];
                    return count == 0 ? Double.NaN : count;
                }
            };
        }

        public Container createContainer() {
            return new CountContainer();
        }

        /**
         * Container that holds a count.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class CountContainer implements Container {
            int count_;
            public void submit( double datum ) {
                count_++;
            }
            public double getResult() {
                return count_ == 0 ? Double.NaN : count_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the sum.
     */
    private static class SumCombiner extends AbstractCombiner {

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (sum).
         *
         * @param  oldValue  previous value
         * @param  datum   newly submitted value
         * @return  new state
         */
        private static double combineSum( double oldValue, double datum ) {
            return Double.isNaN( oldValue ) ? datum : oldValue + datum;
        }

        /**
         * Constructor.
         */
        SumCombiner() {
            super( "sum", "the sum of all the combined values", false );
        }

        public BinList createArrayBinList( int size ) {
            final double[] sums = new double[ size ];
            Arrays.fill( sums, Double.NaN );
            return new ArrayBinList( size, this, isCopyResult_ ) {
                public void submitToBinInt( int index, double datum ) {
                    sums[ index ] = combineSum( sums[ index ], datum );
                }
                public double getBinResultInt( int index ) {
                    return sums[ index ];
                }
            };
        }

        public Container createContainer() {
            return new SumContainer();
        }

        /**
         * Container that holds a sum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class SumContainer implements Container {
            double sum_ = Double.NaN;
            public void submit( double datum ) {
                sum_ = combineSum( sum_, datum );
            }
            public double getResult() {
                return sum_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the minimum submitted value.
     */
    private static class MinCombiner extends AbstractCombiner {

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (min).
         *
         * @param  oldValue  previous value
         * @param  datum   newly submitted value
         * @return  new state
         */
        private static double combineMin( double oldValue, double datum ) {
            return Double.isNaN( oldValue ) ? datum
                                            : Math.min( oldValue, datum );
        }

        /**
         * Constructor.
         */
        MinCombiner() {
            super( "min", "the minimum of all the combined values", false );
        }

        public BinList createArrayBinList( int size ) {
            final double[] mins = new double[ size ];
            Arrays.fill( mins, Double.NaN );
            return new ArrayBinList( size, this, isCopyResult_ ) {
                public void submitToBinInt( int index, double datum ) {
                    mins[ index ] = combineMin( mins[ index ], datum );
                }
                public double getBinResultInt( int index ) {
                    return mins[ index ];
                }
            };
        }

        public Container createContainer() {
            return new MinContainer();
        }

        /**
         * Container that accumulates a minimum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MinContainer implements Container {
            private double min_ = Double.NaN;
            public void submit( double datum ) {
                min_ = combineMin( min_, datum );
            }
            public double getResult() {
                return min_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the maximum submitted value.
     */
    private static class MaxCombiner extends AbstractCombiner {

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (max).
         *
         * @param  oldValue  previous value
         * @param  datum   newly submitted value
         * @return  new state
         */
        private static double combineMax( double oldValue, double datum ) {
            return Double.isNaN( oldValue ) ? datum
                                            : Math.max( oldValue, datum );
        }

        /**
         * Constructor.
         */
        MaxCombiner() {
            super( "max", "the maximum of all the combined values", false );
        }

        public BinList createArrayBinList( int size ) {
            final double[] maxs = new double[ size ];
            Arrays.fill( maxs, Double.NaN );
            return new ArrayBinList( size, this, isCopyResult_ ) {
                public void submitToBinInt( int index, double datum ) {
                    maxs[ index ] = combineMax( maxs[ index ], datum );
                }
                public double getBinResultInt( int index ) {
                    return maxs[ index ];
                }
            };
        }

        public Container createContainer() {
            return new MaxContainer();
        }

        /**
         * Container that accumulates a maximum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MaxContainer implements Container {
            private double max_ = Double.NaN;
            public void submit( double datum ) {
                max_ = combineMax( max_, datum );
            }
            public double getResult() {
                return max_;
            }
        }
    }

    /**
     * Combiner that just registers whether any data have been submitted.
     */
    private static class HitCombiner extends AbstractCombiner {

        /**
         * Constructor.
         */
        HitCombiner() {
            super( "hit",
                   "1 if any values present, NaN otherwise (weight is ignored)",
                   false );
        }

        public BinList createArrayBinList( int size ) {
            final BitSet mask = new BitSet();
            return new ArrayBinList( size, this, false ) {
                public void submitToBinInt( int index, double datum ) {
                    mask.set( index );
                }
                public double getBinResultInt( int index ) {
                    return mask.get( index ) ? 1 : Double.NaN;
                }
            };
        }

        public Container createContainer() {
            return new HitContainer();
        }

        private static class HitContainer implements Container {
            boolean hit_;
            public void submit( double datum ) {
                hit_ = true;
            }
            public double getResult() {
                return hit_ ? 1 : Double.NaN;
            }
        }
    }
}
