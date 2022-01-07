package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import java.util.BitSet;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the combination mode for accumulating values into a bin.
 *
 * <p>Instances of this class can produce <code>Container</code>
 * and <code>BinList</code> objects into which values can be accumulated.
 * Once populated, those objects can be interrogated to find
 * combined values.
 * <strong>Note</strong> that in general those accumulated results
 * should be multiplied by the result of calling
 * {@link Type#getBinFactor} before use.
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
    private final Type type_;
    private final boolean hasBigBin_;

    /** Calculate the number of submitted values. */
    public static final Combiner COUNT;

    /** Calculate the density of all submitted values. */
    public static final Combiner DENSITY;

    /** Calculate the sum of all submitted values. */
    public static final Combiner SUM;

    /** Calculate the weighted density of all submitted values. */
    public static final Combiner WEIGHTED_DENSITY;

    /** Calculate the mean of all submitted values. */
    public static final Combiner MEAN;

    /** Calculate the median of all submitted values (slow). */
    public static final Combiner MEDIAN;

    /** Calculate the sample standard deviation of all submitted values. */
    public static final Combiner SAMPLE_STDEV;

    /** Calculate the minimum of all submitted values. */
    public static final Combiner MIN;

    /** Calculate the maximum of all submitted values. */
    public static final Combiner MAX;

    /** Return 1 if any value submitted, 0 otherwise. */
    public static final Combiner HIT;

    private static final Combiner[] COMBINERS = new Combiner[] {
        SUM = new SumCombiner(),
        WEIGHTED_DENSITY = new WeightedDensityCombiner(),
        COUNT = new CountCombiner(),
        DENSITY = new DensityCombiner(),
        MEAN = new MeanCombiner(),
        MEDIAN = new MedianCombiner(),
        MIN = new MinCombiner(),
        MAX = new MaxCombiner(),
        SAMPLE_STDEV = new StdevCombiner( true ),
        HIT = new HitCombiner(),
    };

    /**
     * Constructor.
     *
     * @param   name  name
     * @param   description  short textual description
     * @param   type    defines the kind of aggregation performed;
     *                  note the implementation of this class does not
     *                  use this value to affect the bin results
     *                  calculated by this combiner, but users of this
     *                  class should make use of it to interpret the
     *                  bin results
     * @param   hasBigBin  indicates whether the bins used by this combiner
     *                     are large
     *                     (take more memory than a <code>double</code>)
     */
    protected Combiner( String name, String description, Type type,
                        boolean hasBigBin ) {
        name_ = name;
        description_ = description;
        type_ = type;
        hasBigBin_ = hasBigBin;
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
    public abstract ArrayBinList createArrayBinList( int size );

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

    /**
     * Indicates the aggregation type.  This value should be used to make
     * sense of the output bin list results.
     *
     * @return  aggregation type
     */
    public Type getType() {
        return type_;
    }

    /**
     * Indicates whether the bin objects used by this combiner are large.
     * Large means, roughly, take more memory than a <code>double</code>.
     * This flag may be used to decide whether to compact bin list results.
     *
     * @return   true if this combiner uses big bins
     */
    public boolean hasBigBin() {
        return hasBigBin_;
    }

    /**
     * Returns a metadata object that describes the result of applying
     * this combiner to data described by a given metadata object.
     *
     * @param  info  metadata for values to be combined, usually numeric;
     *               may be null if metadata unknown
     * @param  scaleUnit      unit of bin extent by which bin values
     *                        are divided for density-like combiners;
     *                        may be null for unknown/natural units
     * @return  metadata for combined values; the content class must be
     *          be a primitive numeric wrapper class
     */
    public abstract ValueInfo createCombinedInfo( ValueInfo info,
                                                  Unit scaleUnit );

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
     * Utility method to return a string describing the content of a ValueInfo.
     * It will come up with something, even if the description member is empty.
     *
     * @param  info  metadata item
     * @return   some text describing it, suitable for inclusion in the
     *           description member of another ValueInfo based on it
     */
    private static String getInfoDescription( ValueInfo info ) {
        String descrip = info.getDescription();
        return descrip != null && descrip.trim().length() > 0 
             ? descrip
             : info.getName();
    }

    /**
     * Modifies a UCD by appending a given modifier word, if possible.
     *
     * @param   ucd   base UCD
     * @param   word  modifier UCD word
     * @param   isPrimary   true if word is marked P[rimary] in the UCD list,
     *                      false if word is marked S[econdary] in the UCD list
     *                      (note other options exist in the UCD list,
     *                      but are not currently catered for here)
     * @return  modified UCD
     * @see   <a href="http://www.ivoa.net/documents/latest/UCDlist.html"
     *           >UCDlist</a>
     */
    private static String modifyUcd( String ucd, String word,
                                     boolean isPrimary ) {
        if ( isPrimary ) {
            return word;
        }
        else {
            if ( ucd == null || ucd.trim().length() == 0 ) {
                return ucd;
            }
            else {
                return ucd + ";" + word;
            }
        }
    }

    /**
     * Defines the scaling properties of a combiner.
     * This also provides information on how the results of the
     * populated bin list should be interpreted.
     */
    public enum Type {

        /**
         * Sum-like aggregation.
         * To first order, the bin result quantities scale linearly
         * with the number of values submitted.
         * If a bin value is NaN because no values have been submitted,
         * it can be interpreted as zero.
         */
        EXTENSIVE( true, false ),

        /**
         * Average-like aggregation.
         * To first order, the bin result quantities are not dependent on
         * the number of values submitted.
         * No numeric value can be assumed if a bin value is NaN because
         * no values have been submitted.
         */
        INTENSIVE( false, false ),

        /**
         * Density-like aggregation.  This is like {@link #EXTENSIVE},
         * but results should be divided by bin size;
         * the {@link #getBinFactor getBinFactor} method in general will
         * return a value that is not unity.
         */
        DENSITY( true, true );

        private final boolean isExtensive_;
        private final boolean isScaling_;

        /**
         * Constructor.
         */
        Type( boolean isExtensive, boolean isScaling ) {
            isExtensive_ = isExtensive;
            isScaling_ = isScaling;
        }

        /**
         * Indicates whether the bin values scale to first order
         * with the number of submitted values per bin.
         *
         * @return  true iff no submitted values corresponds to a zero bin value
         */
        public boolean isExtensive() {
            return isExtensive_;
        }

        /**
         * Returns a scaling factor which ought to be applied to bin values.
         * This may be unity, or it may depend on the supplied bin extent.
         *
         * @param  binExtent  bin size in some natural units
         * @return   factor to multiply bin contents by before use
         */
        public double getBinFactor( double binExtent ) {
            return isScaling_ ? 1.0 / binExtent : 1.0;
        }
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
         * Combines the content of a given container with this one.
         * The effect is the same as if all the data submitted to the
         * supplied container had in fact been submitted to this one.
         * The other container is assumed to be of the same type as this one;
         * if not, a ClassCastException is likely to occur.
         *
         * @param   container  other container, of the same type as this one
         */
        void add( Container container );

        /**
         * Returns the combined result of all the values submitted so far.
         * In general, if no values have been submitted,
         * a NaN should be returned.
         *
         * @return  combined value of all submitted data
         */
        double getCombinedValue();
    }

    /**
     * Combiner implementation that calculates the mean.
     */
    private static class MeanCombiner extends Combiner {

        /**
         * Constructor.
         */
        public MeanCombiner() {
            super( "mean", "the mean of the combined values", Type.INTENSIVE,
                   true );
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new MeanBinList( size, this );
        }

        public Container createContainer() {
            return new MeanContainer();
        }

        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info = new DefaultValueInfo( dataInfo );
            info.setContentClass( Double.class );
            info.setDescription( getInfoDescription( dataInfo )
                               + ", mean value in bin" );
            info.setUCD( modifyUcd( dataInfo.getUCD(), "stat.mean", false ) );
            return info;
        }

        /**
         * ArrayBinList subclass for MeanCombiner.
         */
        private static class MeanBinList extends ArrayBinList {
            final int[] counts_;
            final double[] sums_;
            MeanBinList( int size, MeanCombiner combiner ) {
                super( size, combiner );
                counts_ = new int[ size ];
                sums_ = new double[ size ];
            }
            public void submitToBinInt( int index, double value ) {
                counts_[ index ]++;
                sums_[ index ] += value;
            }
            public double getBinResultInt( int index ) {
                int count = counts_[ index ];
                return count == 0 ? Double.NaN
                                  : sums_[ index ] / (double) count;
            }
            public void copyBin( int index, Container bin ) {
                MeanContainer container = (MeanContainer) bin;
                counts_[ index ] = container.count_;
                sums_[ index ] = container.sum_;
            }
            public void addBin( int index, ArrayBinList other ) {
                MeanBinList meanOther = (MeanBinList) other;
                counts_[ index ] += meanOther.counts_[ index ];
                sums_[ index ] += meanOther.sums_[ index ];
            }
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
            public void add( Container other ) {
                MeanContainer meanOther = (MeanContainer) other;
                count_ += meanOther.count_;
                sum_ += meanOther.sum_;
            }
            public double getCombinedValue() {
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

        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info = new DefaultValueInfo( dataInfo );
            info.setContentClass( Double.class );
            info.setDescription( getInfoDescription( dataInfo )
                               + ", median value in bin" );
            info.setUCD( modifyUcd( dataInfo.getUCD(), "stat.median", false ) );
            return info;
        }
    }

    /**
     * Combiner implementation that calculates the standard deviation.
     */
    private static class StdevCombiner extends Combiner {
        private final boolean isSampleStdev_;

        /**
         * Constructor.
         *
         * @param  isSampleStdev  false for population standard deviation,
         *                        true for sample standard deviation
         */
        public StdevCombiner( boolean isSampleStdev ) {
            super( "stdev",
                   "the " + ( isSampleStdev ? "sample" : "population" )
                          + " standard deviation of the combined values",
                   Type.INTENSIVE, true );
            isSampleStdev_ = isSampleStdev;
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new StdevBinList( size, this );
        }

        public Container createContainer() {
            return isSampleStdev_ ? new SampleStdevContainer()
                                  : new PopulationStdevContainer();
        }

        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info =
                new DefaultValueInfo( dataInfo.getName() + "_stdev",
                                      Double.class,
                                      ( isSampleStdev_ ? "Sample "
                                                       : "Population " )
                                    + "standard deviation of "
                                    + getInfoDescription( dataInfo ) );
            info.setUnitString( dataInfo.getUnitString() );
            info.setUCD( modifyUcd( dataInfo.getUCD(), "stat.stdev", true ) );
            return info;
        }

        /**
         * ArrayBinList subclass for StdevCombiner.
         */
        private static class StdevBinList extends ArrayBinList {
            final boolean isSampleStdev_;
            final int[] counts_;
            final double[] sum1s_;
            final double[] sum2s_;
            StdevBinList( int size, StdevCombiner combiner ) {
                super( size, combiner );
                isSampleStdev_ = combiner.isSampleStdev_;
                counts_ = new int[ size ];
                sum1s_ = new double[ size ];
                sum2s_ = new double[ size ];
            }
            public void submitToBinInt( int index, double value ) {
                counts_[ index ]++;
                sum1s_[ index ] += value;
                sum2s_[ index ] += value * value;
            }
            public double getBinResultInt( int index ) {
                return getStdev( isSampleStdev_, counts_[ index ],
                                 sum1s_[ index ], sum2s_[ index ] );
            }
            public void copyBin( int index, Container bin ) {
                StdevContainer container = (StdevContainer) bin;
                counts_[ index ] = container.count_;
                sum1s_[ index ] = container.sum1_;
                sum2s_[ index ] = container.sum2_;
            }
            public void addBin( int index, ArrayBinList other ) {
                StdevBinList stdevOther = (StdevBinList) other;
                counts_[ index ] += stdevOther.counts_[ index ];
                sum1s_[ index ] += stdevOther.sum1s_[ index ];
                sum2s_[ index ] += stdevOther.sum2s_[ index ];
            }
        }

        /**
         * Partial Container implementation for calculating
         * standard deviation-like quantities.
         */
        private static abstract class StdevContainer implements Container {
            int count_;
            double sum1_;
            double sum2_;
            public void submit( double datum ) {
                count_++;
                sum1_ += datum;
                sum2_ += datum * datum;
            }
            public void add( Container other ) {
                StdevContainer stdevOther = (StdevContainer) other;
                count_ += stdevOther.count_;
                sum1_ += stdevOther.sum1_;
                sum2_ += stdevOther.sum2_;
            }
        }

        /**
         * Container to calculate a population standard deviation.
         * Note that this is a static class with no unnecessary members
         * to keep memory usage down if there are many instances.
         */
        private static class PopulationStdevContainer extends StdevContainer {
            public double getCombinedValue() {
                return getStdev( false, count_, sum1_, sum2_ );
            }
        }

        /**
         * Container to calculate a sample standard deviation.
         * Note that this is a static class with no unnecessary members
         * to keep memory usage down if there are many instances.
         */
        private static class SampleStdevContainer extends StdevContainer {
            public double getCombinedValue() {
                return getStdev( true, count_, sum1_, sum2_ );
            }
        }

        /**
         * Utility method to calculate a population or sample
         * standard deviation from the relevant accumulated quantities.
         *
         * @param  isSampleStdev   false for population standard deviation,
         *                         true for sample standard deviation
         * @param  count  number of accumulated values
         * @param  sum1   sum of accumulated values
         * @param  sum2   sum of squares of accumulated values
         * @return  standard deviation, or NaN if insufficient data
         */
        private static double getStdev( boolean isSampleStdev, int count,
                                        double sum1, double sum2 ) {
            if ( count < ( isSampleStdev ? 2 : 1 ) ) {
                return Double.NaN;
            }
            else {
                double dcount = (double) count;
                double nvar = sum2 - sum1 * sum1 / dcount;
                double divisor = isSampleStdev ? ( dcount - 1 ) : dcount;
                return Math.sqrt( nvar / divisor );
            }
        }
    }

    /**
     * Partial Combiner implementation that counts submissions.
     */
    private static abstract class AbstractCountCombiner extends Combiner {

        /**
         * Constructor.
         *
         * @param  name  name
         * @param  descrip  description
         * @param  type  combiner type
         */
        AbstractCountCombiner( String name, String descrip, Type type ) {
            super( name, descrip, type, false );
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new AbstractCountBinList( size, this );
        }

        public Container createContainer() {
            return new CountContainer();
        }

        /**
         * ArrayBinList subclass for AbstractCountCombiner.
         */
        static class AbstractCountBinList extends ArrayBinList {
            final int[] counts_;
            AbstractCountBinList( int size, AbstractCountCombiner combiner ) {
                super( size, combiner );
                counts_ = new int[ size ];
            }
            public void submitToBinInt( int index, double value ) {
                counts_[ index ]++;
            }
            public double getBinResultInt( int index ) {
                int count = counts_[ index ];
                return count == 0 ? Double.NaN : count;
            }
            public void copyBin( int index, Container bin ) {
                CountContainer container = (CountContainer) bin;
                counts_[ index ] = container.count_;
            }
            public void addBin( int index, ArrayBinList other ) {
                AbstractCountBinList countOther = (AbstractCountBinList) other;
                counts_[ index ] += countOther.counts_[ index ];
            }
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
            public void add( Container other ) {
                CountContainer countOther = (CountContainer) other;
                count_ += countOther.count_;
            }
            public double getCombinedValue() {
                return count_ == 0 ? Double.NaN : count_;
            }
        }
    }

    /**
     * Combiner instance that counts submissions per bin. 
     */
    private static class CountCombiner extends AbstractCountCombiner {
        CountCombiner() {
            super( "count",
                   "the number of non-blank values per bin (weight is ignored)",
                   Type.EXTENSIVE );
        }
        public ValueInfo createCombinedInfo( ValueInfo inInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo outInfo = 
                new DefaultValueInfo( "count", Integer.class,
                                      "Number of items counted per bin" );
            outInfo.setUCD( "meta.number" );
            return outInfo;
        }
    }

    /**
     * Combiner instance that counts submissions per unit of bin extent.
     */
    private static class DensityCombiner extends AbstractCountCombiner {
        DensityCombiner() {
            super( "count-per-unit",
                   "the number of non-blank values per unit of bin size "
                 + "(weight is ignored)",
                   Type.DENSITY );
        }
        public ValueInfo createCombinedInfo( ValueInfo inInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo outInfo =
                new DefaultValueInfo( "density", Double.class,
                                      "Number of items counted per "
                                    +  scaleUnit.getTextName() );
            outInfo.setUCD( "src.density" );
            outInfo.setUnitString( "1/" + scaleUnit.getSymbol() );
            return outInfo;
        }
    }

    /**
     * Partial Combiner implementation that calculates the sum of
     * submitted values.
     */
    private static abstract class AbstractSumCombiner extends Combiner {

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
         *
         * @param  name  name
         * @param  descrip  description
         * @param  type  combiner type
         */
        AbstractSumCombiner( String name, String descrip, Type type ) {
            super( name, descrip, type, false );
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new AbstractSumBinList( size, this );
        }

        public Container createContainer() {
            return new SumContainer();
        }

        /**
         * ArrayBinList subclass for AbstractSumCombiner.
         */
        static class AbstractSumBinList extends ArrayBinList {
            final double[] sums_;
            AbstractSumBinList( int size, AbstractSumCombiner combiner ) {
                super( size, combiner );
                sums_ = new double[ size ];
                Arrays.fill( sums_, Double.NaN );
            }
            public void submitToBinInt( int index, double datum ) {
                sums_[ index ] = combineSum( sums_[ index ], datum );
            }
            public double getBinResultInt( int index ) {
                return sums_[ index ];
            }
            public void copyBin( int index, Container bin ) {
                SumContainer container = (SumContainer) bin;
                sums_[ index ] = container.sum_;
            }
            public void addBin( int index, ArrayBinList other ) {
                double otherSum = ((AbstractSumBinList) other).sums_[ index ];
                if ( ! Double.isNaN( otherSum ) ) {
                    sums_[ index ] = combineSum( sums_[ index ], otherSum );
                }
            }
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
            public void add( Container other ) {
                double otherSum = ((SumContainer) other).sum_;
                if ( ! Double.isNaN( otherSum ) ) {
                    sum_ = combineSum( sum_, otherSum );
                }
            }
            public double getCombinedValue() {
                return sum_;
            }
        }
    }

    /**
     * Combiner instance that sums submitted values per bin.
     */
    private static class SumCombiner extends AbstractSumCombiner {
        SumCombiner() {
            super( "sum", "the sum of all the combined values per bin",
                   Type.EXTENSIVE );
        }
        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info =
                new DefaultValueInfo( dataInfo.getName() + "_sum",
                                      Double.class,
                                      getInfoDescription( dataInfo )
                                    + ", sum in bin" );
            info.setUnitString( dataInfo.getUnitString() );
            info.setUCD( modifyUcd( dataInfo.getUCD(), "arith.sum", false ) );
            return info;
        }
    }

    /**
     * Combiner instance that sums submitted values per unit of bin extent.
     */
    private static class WeightedDensityCombiner extends AbstractSumCombiner {
        WeightedDensityCombiner() {
            super( "sum-per-unit",
                   "the sum of all the combined values per unit of bin size",
                   Type.DENSITY );
        }
        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info =
                new DefaultValueInfo( dataInfo.getName() + "_density",
                                      Double.class,
                                      getInfoDescription( dataInfo )
                                    + ", density per "
                                    + scaleUnit.getTextName() );
            String inUnit = dataInfo.getUnitString();
            if ( inUnit == null || inUnit.trim().length() == 0 ) {
                inUnit = "1";
            }
            info.setUnitString( inUnit + "/" + scaleUnit.getSymbol() );
            return info;
        }
    }

    /**
     * Combiner implementation that calculates the minimum submitted value.
     */
    private static class MinCombiner extends Combiner {

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
            super( "min", "the minimum of all the combined values",
                   Type.INTENSIVE, false );
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new MinBinList( size, this );
        }

        public Container createContainer() {
            return new MinContainer();
        }

        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info =
                new DefaultValueInfo( dataInfo.getName() + "_min",
                                      dataInfo.getContentClass(),
                                      getInfoDescription( dataInfo )
                                    + ", minimum value in bin" );
            info.setUnitString( dataInfo.getUnitString() );
            info.setUCD( modifyUcd( dataInfo.getUCD(), "stat.min", false ) );
            return info;
        }

        /**
         * ArrayBinList subclass for MinCombiner.
         */
        private static class MinBinList extends ArrayBinList {
            final double[] mins_;
            MinBinList( int size, MinCombiner combiner ) {
                super( size, combiner );
                mins_ = new double[ size ];
                Arrays.fill( mins_, Double.NaN );
            }
            public void submitToBinInt( int index, double datum ) {
                mins_[ index ] = combineMin( mins_[ index ], datum );
            }
            public double getBinResultInt( int index ) {
                return mins_[ index ];
            }
            public void copyBin( int index, Container bin ) {
                MinContainer container = (MinContainer) bin;
                mins_[ index ] = container.min_;
            }
            public void addBin( int index, ArrayBinList other ) {
                double otherMin = ((MinBinList) other).mins_[ index ];
                if ( ! Double.isNaN( otherMin ) ) {
                    mins_[ index ] = combineMin( mins_[ index ], otherMin );
                }
            }
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
            public void add( Container other ) {
                double otherMin = ((MinContainer) other).min_;
                if ( ! Double.isNaN( otherMin ) ) {
                    min_ = combineMin( min_, otherMin );
                }
            }
            public double getCombinedValue() {
                return min_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the maximum submitted value.
     */
    private static class MaxCombiner extends Combiner {

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
            super( "max", "the maximum of all the combined values",
                   Type.INTENSIVE, false );
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new MaxBinList( size, this );
        }

        public Container createContainer() {
            return new MaxContainer();
        }

        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            DefaultValueInfo info =
                new DefaultValueInfo( dataInfo.getName() + "_max",
                                      dataInfo.getContentClass(),
                                      getInfoDescription( dataInfo )
                                    + ", maximum value in bin" );
            info.setUnitString( dataInfo.getUnitString() );
            info.setUCD( modifyUcd( dataInfo.getUCD(), "stat.max", false ) );
            return info;
        }

        /**
         * ArrayBinList subclass for StdevCombiner.
         */
        private static class MaxBinList extends ArrayBinList {
            final double[] maxs_;
            MaxBinList( int size, MaxCombiner combiner ) {
                super( size, combiner );
                maxs_ = new double[ size ];
                Arrays.fill( maxs_, Double.NaN );
            }
            public void submitToBinInt( int index, double datum ) {
                maxs_[ index ] = combineMax( maxs_[ index ], datum );
            }
            public double getBinResultInt( int index ) {
                return maxs_[ index ];
            }
            public void copyBin( int index, Container bin ) {
                MaxContainer container = (MaxContainer) bin;
                maxs_[ index ] = container.max_;
            }
            public void addBin( int index, ArrayBinList other ) {
                double otherMax = ((MaxBinList) other).maxs_[ index ];
                if ( ! Double.isNaN( otherMax ) ) {
                    maxs_[ index ] = combineMax( maxs_[ index ], otherMax );
                }
            }
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
            public void add( Container other ) {
                double otherMax = ((MaxContainer) other).max_;
                if ( ! Double.isNaN( otherMax ) ) {
                    max_ = combineMax( max_, otherMax );
                }
            }
            public double getCombinedValue() {
                return max_;
            }
        }
    }

    /**
     * Combiner that just registers whether any data have been submitted.
     */
    private static class HitCombiner extends Combiner {

        /**
         * Constructor.
         */
        HitCombiner() {
            super( "hit",
                   "1 if any values present, NaN otherwise (weight is ignored)",
                   Type.EXTENSIVE, false );
        }

        public ArrayBinList createArrayBinList( int size ) {
            return new HitBinList( size, this );
        }

        public Container createContainer() {
            return new HitContainer();
        }

        public ValueInfo createCombinedInfo( ValueInfo dataInfo,
                                             Unit scaleUnit ) {
            return new DefaultValueInfo( "hit", Short.class,
                                         "1 if bin contains data, 0 if not" );
        }

        /**
         * ArrayBinList subclass for HitCombiner.
         */
        private static class HitBinList extends ArrayBinList {
            final BitSet mask_;
            HitBinList( int size, HitCombiner combiner ) {
                super( size, combiner );
                mask_ = new BitSet();
            }
            public void submitToBinInt( int index, double datum ) {
                mask_.set( index );
            }
            public double getBinResultInt( int index ) {
                return mask_.get( index ) ? 1 : Double.NaN;
            }
            public void copyBin( int index, Container bin ) {
                HitContainer container = (HitContainer) bin;
                mask_.set( index, container.hit_ );
            }
            public void addBin( int index, ArrayBinList other ) {
                if ( ((HitBinList) other).mask_.get( index ) ) {
                    mask_.set( index );
                }
            }
        }

        private static class HitContainer implements Container {
            boolean hit_;
            public void submit( double datum ) {
                hit_ = true;
            }
            public void add( Container other ) {
                boolean otherHit = ((HitContainer) other).hit_;
                if ( otherHit ) {
                    hit_ = true;
                }
            }
            public double getCombinedValue() {
                return hit_ ? 1 : Double.NaN;
            }
        }
    }
}
