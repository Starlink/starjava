package uk.ac.starlink.ttools.task;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.filter.StatsFilter;
import uk.ac.starlink.ttools.plot2.layer.Combiner;
import uk.ac.starlink.util.ByteList;
import uk.ac.starlink.util.DoubleList;
import uk.ac.starlink.util.FloatList;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.LongList;
import uk.ac.starlink.util.ShortList;

/**
 * Provides instances of the Aggregator interface.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2022
 */
public class Aggregators {

    /** Aggregator for counting items, with int-valued counter. */
    public static final Aggregator COUNT;

    /** Aggregator for counting non-blank items, with int-valued counter. */
    public static final Aggregator NGOOD;

    /** Aggregator for summing items. */
    public static final Aggregator SUM;

    /** Aggregator that calculates the mean. */
    public static final Aggregator MEAN;

    /** Aggregator that calculates the median. */
    public static final Aggregator MEDIAN;

    /** Aggregator that calculates the sample standard deviation. */
    public static final Aggregator SAMPLE_STDEV;

    /** Aggregator that calculates the population standard deviation. */
    public static final Aggregator POP_STDEV;

    /** Aggregator that calculates the maximum. */
    public static final Aggregator MAX;

    /** Aggregator that calculates the minimum. */
    public static final Aggregator MIN;

    /** Aggregator that assembles an array of all non-blank inputs. */
    public static final Aggregator ARRAY_NOBLANKS;

    /** Aggregator that assembles an array of all inputs. */
    public static final Aggregator ARRAY_WITHBLANKS;

    /** Aggregator for counting items, with long-valued counter. */
    public static final Aggregator COUNT_LONG;

    /** Aggregator for counting non-blank items, with long-valued counter. */
    public static final Aggregator NGOOD_LONG;
 
    /** Useful instances. */
    private static final Aggregator[] INSTANCES = {
        COUNT = new CountAggregator( "count", false ),
        NGOOD = new CountGoodAggregator( "ngood", false ),
        SUM = new CombinerAggregator( "sum", Combiner.SUM ),
        MEAN = new CombinerAggregator( "mean", Combiner.MEAN ),
        MEDIAN = new CombinerAggregator( "median", Combiner.MEDIAN ),
        SAMPLE_STDEV = new CombinerAggregator( "stdev", Combiner.SAMPLE_STDEV ),
        POP_STDEV = new CombinerAggregator( "stdev-pop", Combiner.POP_STDEV ),
        MAX = new ExtremumAggregator( "max", true ),
        MIN = new ExtremumAggregator( "min", false ),
        ARRAY_NOBLANKS = new ArrayAggregator( "array", false ),
        ARRAY_WITHBLANKS = new ArrayAggregator( "array-withblanks", true ),
        COUNT_LONG = new CountAggregator( "count-long", true ),
        NGOOD_LONG = new CountGoodAggregator( "ngood-long", true ),
    };

    /**
     * Private sole contructor prevents instantiation.
     */
    private Aggregators() {
    }

    /**
     * Returns an array of useful Aggregator instances.
     *
     * @return   aggregator array
     */
    public static Aggregator[] getAggregators() {
        return INSTANCES.clone();
    }

    /**
     * Gets an aggregator instance from its name.
     *  
     * @param  aggTxt  string specification of aggregator
     * @return  aggregator, or null if nothing can be made of it
     */
    public static Aggregator getAggregator( String aggTxt ) {
        if ( aggTxt == null ) {
            return null;
        }
        for ( Aggregator agg : INSTANCES ) {
            if ( aggTxt.equalsIgnoreCase( agg.getName() ) ) {
                return agg;
            }
        }
        double quant = StatsFilter.parseQuantileSpecifier( aggTxt );
        if ( ! Double.isNaN( quant ) ) {
            Combiner qcombiner =
                Combiner.createQuantileCombiner( aggTxt, null, quant );
            return new CombinerAggregator( aggTxt, qcombiner );
        }
        Aggregator reflectAgg =
            Loader.getClassInstance( aggTxt, Aggregator.class );
        if ( reflectAgg != null ) {
            return reflectAgg;
        }
        return null;
    }

    /**
     * Returns an XML element listing the possible options for
     * specification of an Aggregator.
     * This corresponds to the suitable inputs for the
     * {@link #getAggregator(String)} method.
     *
     * @return  options description in as a &lt;ul&gt; element
     */
    public static String getOptionsDescription() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<ul>\n" );
        for ( Aggregator agg : INSTANCES ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( agg.getName() )
                .append( "</code>: " )
                .append( agg.getDescription() )
                .append( "</li>\n" );
        }
        sbuf.append( "<li><code>Q.nnn</code>: " )
            .append( "quantile nnn (e.g. Q.05 is the fifth percentile)" )
            .append( "</li>\n" );
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }

    /**
     * Partial aggregator implementation.
     */
    private static abstract class AbstractAggregator implements Aggregator {
        final String name_;
        final String description_;

        /**
         * Constructor.
         *
         * @param  name  aggregator name
         * @param  description  aggregator description
         */
        AbstractAggregator( String name, String description ) {
            name_ = name;
            description_ = description;
        }

        public String getName() {
            return name_;
        }

        public String getDescription() {
            return description_;
        }
    }

    /**
     * Basic Aggregation implementation.
     */
    private static class DefaultAggregation implements Aggregator.Aggregation {
        final ValueInfo outInfo_;
        final Supplier<Aggregator.Accumulator> accSupplier_;

        /**
         * Constructor.
         *
         * @param  outInfo   metadata describing aggregated result
         * @param  accSupplier   supplier for typed accumulator
         */
        DefaultAggregation( ValueInfo outInfo,
                            Supplier<Aggregator.Accumulator> accSupplier ) {
            outInfo_ = outInfo;
            accSupplier_ = accSupplier;
        }

        public ValueInfo getResultInfo() {
            return outInfo_;
        }

        public Aggregator.Accumulator createAccumulator() {
            return accSupplier_.get();
        }
    }

    /**
     * Partial accumulator implementation that just reports an incrementable
     * integer value.
     */
    private static abstract class CountAccumulator
                            implements Aggregator.Accumulator {
        private final boolean isLong_;
        private long count_;

        /**
         * Constructor.
         *
         * @param  isLong  true for long-valued result type, false for int
         */
        CountAccumulator( boolean isLong ) {
            isLong_ = isLong;
        }

        /**
         * Increments counter.
         */
        public void increment() {
            count_++;
        }

        public Object getResult() {
            if ( isLong_ ) {
                return Long.valueOf( count_ );
            }
            else {
                int icount = (int) count_;
                return Integer.valueOf( icount == count_ ? icount : -1 );
            }
        }

        public void add( Aggregator.Accumulator other ) {
            count_ += ((CountAccumulator) other).count_;
        }
    }

    /**
     * Aggregator for counting all rows.
     * Since this class pays no attention to the metadata
     * it also forms the sole implementation of its own Aggregation.
     */
    private static class CountAggregator extends AbstractAggregator
                                         implements Aggregator.Aggregation {
        private final boolean isLong_;

        /**
         * Constructor.
         *
         * @param  name  aggregator name
         * @param  isLong  true for long-valued output type, false for int
         */
        CountAggregator( String name, boolean isLong ) {
            super( name,
                   "counts the number of rows"
                 + ( isLong ? ", works for >2 billion" : "" ) );
            isLong_ = isLong;
        }

        public Aggregation createAggregation( ValueInfo info ) {
            return this;
        }

        public ValueInfo getResultInfo() {
            return new DefaultValueInfo( "count",
                                         createAccumulator().getResult()
                                                            .getClass(),
                                         "number of rows in group" );
        }

        public Accumulator createAccumulator() {
            return new CountAccumulator( isLong_ ) {
                public void submit( Object datum ) {
                    increment();
                }
            };
        }
    }

    /**
     * Aggregator for counting non-blank items.
     */
    private static class CountGoodAggregator extends AbstractAggregator {
        private final boolean isLong_;

        /**
         * Constructor.
         *
         * @param  name  aggregator name
         * @param  isLong  true for long-valued output type, false for int
         */
        CountGoodAggregator( String name, boolean isLong ) {
            super( name,
                   "counts the number of non-blank items"
                 + ( isLong ? ", works for >2 billion" : "" ) );
            isLong_ = isLong;
        }

        public Aggregation createAggregation( ValueInfo inInfo ) {
            final Class<?> clazz = inInfo.getContentClass();
            String inName = inInfo.getName();
            Supplier<Accumulator> accSupplier =
                    () -> new CountAccumulator( isLong_ ) {
                public void submit( Object datum ) {
                    if ( clazz.isInstance( datum ) &&
                         !Tables.isBlank( datum ) ) {
                        increment();
                    }
                }
            };
            ValueInfo outInfo =
                new DefaultValueInfo( "ngood_" + inName,
                                      accSupplier.get().getResult().getClass(),
                                      "Number of non-blank entries in "
                                    + inName );
            return new DefaultAggregation( outInfo, accSupplier );
        }
    }

    /**
     * Aggregator for acquiring minimum/maximum values.
     */
    private static class ExtremumAggregator extends AbstractAggregator {
        final boolean isMax_;

        /**
         * Constructor.
         *
         * @param  name  aggregator name
         * @param  isMax  true for maximum, false for minimum
         */
        ExtremumAggregator( String name, boolean isMax ) {
            super( name,
                   "records the " + ( isMax ? "max" : "min" ) + "imum value" );
            isMax_ = isMax;
        }

        public Aggregation createAggregation( ValueInfo inInfo ) {
            Class<?> clazz = inInfo.getContentClass();
            if ( Comparable.class.isAssignableFrom( clazz ) ) {
                String inName = inInfo.getName();
                String outName = ( isMax_ ? "max_" : "min_" ) + inName;
                String outDescrip = ( isMax_ ? "Maximum" : "Minimum" )
                                  + " value for " + inName;
                return new DefaultAggregation(
                           new DefaultValueInfo( outName, clazz, outDescrip ),
                           () -> createExtremumAccumulator( clazz ) );
            }
            else {
                return null;
            }
        }

        /**
         * Acquires an accumulator for determining min/max,
         * for use with a given input class.
         *
         * @param  comparableClazz  input data class, must implement
         *                          java.lang.Comparable
         * @return  accumulator
         */
        private Accumulator
                createExtremumAccumulator( Class<?> comparableClazz ) {
            if ( ! Comparable.class.isAssignableFrom( comparableClazz ) ) {
                throw new IllegalArgumentException();
            }

            // What I want to do is to cast comparableClazz to a class I
            // can use to pass to the ExtremumAccumulator constructor,
            // which requires Class<T extends Comparable<T>>.
            // I can't work out how to do that (I suspect it is impossible),
            // so behold this nasty hack which uses and ignores raw types
            // to get round it.
            @SuppressWarnings("rawtypes")
            Class clazz = comparableClazz;
            @SuppressWarnings({"rawtypes","unchecked"})
            Accumulator acc = new ExtremumAccumulator( isMax_, clazz );
            return acc;
        }

        /**
         * Accumulator for ExtremumAggregator.
         */
        private static class ExtremumAccumulator<T extends Comparable<T>>
                implements Accumulator {
            final boolean isMax_;
            final Class<T> clazz_;
            T extremum_;

            /**
             * Constructor.
             *
             * @param  isMax  true for maximum, false for minimum
             * @param  clazz  input data class, required to be Comparable
             */
            ExtremumAccumulator( boolean isMax, Class<T> clazz ) {
                isMax_ = isMax;
                clazz_ = clazz;
            }

            public void submit( Object datum ) {
                if ( clazz_.isInstance( datum ) && !Tables.isBlank( datum ) ) {
                    T tvalue = clazz_.cast( datum );
                    if ( extremum_ == null ) {
                        extremum_ = tvalue;
                    }
                    else {
                        int comparison = tvalue.compareTo( extremum_ );
                        if ( isMax_ ? comparison > 0 : comparison < 0 ) {
                            extremum_ = tvalue;
                        }
                    }
                }
            }

            public T getResult() {
                return extremum_;
            }

            public void add( Accumulator other ) {
                submit( ((ExtremumAccumulator) other).extremum_ );
            }
        }
    }

    /**
     * Aggregator implementation that uses a Combiner to do the work.
     * There is a good selection of Combiner instances,
     * but they only work with numeric input and (<code>double</code>)
     * output values.
     */
    private static class CombinerAggregator extends AbstractAggregator {
        final Combiner combiner_;

        /**
         * Constructor.
         *
         * @param  name   aggregator name
         * @param  combiner  combiner
         */
        CombinerAggregator( String name, Combiner combiner ) {
            super( name, combiner.getDescription() );
            combiner_ = combiner;
        }

        public Aggregation createAggregation( ValueInfo info ) {
            return Number.class.isAssignableFrom( info.getContentClass() )
                ? new DefaultAggregation(
                          combiner_.createCombinedInfo( info, null ),
                          () -> new CombinerAccumulator( combiner_
                                                        .createContainer() ) )
                : null;
        }

        /**
         * Accumulator implementation for use with Combiners.
         */
        private static class CombinerAccumulator implements Accumulator {
            final Combiner.Container container_;

            /**
             * Constructor.
             *
             * @param  container  combiner-specific aggregation container
             */
            CombinerAccumulator( Combiner.Container container ) {
                container_ = container;
            }

            public void submit( Object datum ) {
                if ( datum instanceof Number ) {
                    double dval = ((Number) datum).doubleValue();
                    if ( ! Double.isNaN( dval ) ) {
                        container_.submit( dval );
                    }
                }
            }

            public Object getResult() {
                return Double.valueOf( container_.getCombinedValue() );
            }

            public void add( Accumulator other ) {
                container_.add( ((CombinerAccumulator) other).container_ );
            }
        }
    }

    /**
     * Aggregator that collects all values into a suitably typed array.
     */
    private static class ArrayAggregator extends AbstractAggregator {
        final boolean includeBlanks_;

        /**
         * Constructor.
         *
         * @param  name  aggregator name
         * @param  includeBlanks  true if blank input values appear
         *                        in the aggregated array
         */
        ArrayAggregator( String name, boolean includeBlanks ) {
            super( name,
                   includeBlanks
                       ? ( "collects all values into an array; " +
                           "blank values are represented as zero for integers" )
                       : "collects all non-blank values into an array" );
            includeBlanks_ = includeBlanks;
        }

        public Aggregation createAggregation( ValueInfo info ) {
            Class<?> clazz = info.getContentClass();

            /* For wrapper classes, return a value typed as an array
             * of the corresponding primitive class. */
            if ( clazz == Byte.class ) {
                return new ArrayAggregation<byte[]>( byte[].class, info, () ->
                    new ArrayAccumulator<byte[],ByteList>( new ByteList() ) {
                        public void submit( Object datum ) {
                            if ( datum instanceof Number ) {
                                list_.add( ((Number) datum).byteValue() );
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( (byte) 0 );
                            }
                        }
                        public byte[] getResult() {
                            return list_.toByteArray();
                        }
                        public void addList( ByteList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }
            else if ( clazz == Short.class ) {
                return new ArrayAggregation<short[]>( short[].class, info, () ->
                    new ArrayAccumulator<short[],ShortList>( new ShortList() ) {
                        public void submit( Object datum ) {
                            if ( datum instanceof Number ) {
                                list_.add( ((Number) datum).shortValue() );
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( (short) 0 );
                            }
                        }
                        public short[] getResult() {
                            return list_.toShortArray();
                        }
                        public void addList( ShortList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }
            else if ( clazz == Integer.class ) {
                return new ArrayAggregation<int[]>( int[].class, info, () ->
                    new ArrayAccumulator<int[],IntList>( new IntList() ) {
                        public void submit( Object datum ) {
                            if ( datum instanceof Number ) {
                                list_.add( ((Number) datum).intValue() );
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( 0 );
                            }
                        }
                        public int[] getResult() {
                            return list_.toIntArray();
                        }
                        public void addList( IntList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }
            else if ( clazz == Long.class ) {
                return new ArrayAggregation<long[]>( long[].class, info, () ->
                    new ArrayAccumulator<long[],LongList>( new LongList() ) {
                        public void submit( Object datum ) {
                            if ( datum instanceof Number ) {
                                list_.add( ((Number) datum).longValue() );
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( 0L );
                            }
                        }
                        public long[] getResult() {
                            return list_.toLongArray();
                        }
                        public void addList( LongList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }
            else if ( clazz == Float.class ) {
                return new ArrayAggregation<float[]>( float[].class, info, () ->
                    new ArrayAccumulator<float[],FloatList>( new FloatList() ) {
                        public void submit( Object datum ) {
                            if ( datum instanceof Number ) {
                                float fval = ((Number) datum).floatValue();
                                if ( includeBlanks_ || !Float.isNaN( fval ) ) {
                                    list_.add( fval );
                                }
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( Float.NaN );
                            }
                        }
                        public float[] getResult() {
                            return list_.toFloatArray();
                        }
                        public void addList( FloatList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }
            else if ( clazz == Double.class ) {
                return new ArrayAggregation<double[]>( double[].class, info,
                                                       () ->
                    new ArrayAccumulator<double[],DoubleList>
                                        ( new DoubleList() ){
                        public void submit( Object datum ) {
                            if ( datum instanceof Number ) {
                                double dval = ((Number) datum).doubleValue();
                                if ( includeBlanks_ || !Double.isNaN( dval ) ) {
                                    list_.add( dval );
                                }
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( Double.NaN );
                            }
                        }
                        public double[] getResult() {
                            return list_.toDoubleArray();
                        }
                        public void addList( DoubleList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }
            else if ( clazz == Boolean.class ) {
                return new ArrayAggregation<boolean[]>( boolean[].class, info,
                                                        () ->
                    new ArrayAccumulator<boolean[],BooleanList>
                                        ( new BooleanList() ) {
                        public void submit( Object datum ) {
                            if ( datum instanceof Boolean ) {
                                list_.add( ((Boolean) datum).booleanValue() );
                            }
                            else if ( includeBlanks_ ) {
                                list_.add( false );
                            }
                        }
                        public boolean[] getResult() {
                            return list_.toBooleanArray();
                        }
                        public void addList( BooleanList otherList ) {
                            list_.addAll( otherList );
                        }
                    }
                );
            }

            /* For non-primitive STIL-friendly classes (currently that
             * just means String), just make an array of the input values. */
            else if ( clazz == String.class ) {
                return createObjectArrayAggregation( String.class, info,
                                                     includeBlanks_ );
            }

            /* Don't do that for array input values; most likely these
             * are primitive arrays, which ought to get treated differently
             * than making an array of the input values.  But I haven't
             * written that code yet. */
            else if ( clazz.getComponentType() != null ) {
                return null;
            }

            /* For non-primitive non-STIL friendly classes, just make an
             * object array; this is probably no worse than the input value. */
            else {
                return createObjectArrayAggregation( clazz, info,
                                                     includeBlanks_ );
            }
        }

        /**
         * Returns an Aggregation implementation for cases where the output
         * array is an array of the input scalar objects.
         * Note this is not a good choice for input objects with primitive
         * representations, since they should turn into primitive arrays,
         * not arrays of wrapper objects.
         *
         * @param  elClazz  input value content type, which must be
         *                  compatible with the input value metadata
         * @param  inInfo   input value metadata
         * @param  includeBlanks  whether to include blank values
         * @return  aggregation
         */
        private static <E> ArrayAggregation<E[]>
                createObjectArrayAggregation( Class<E> elClazz,
                                              ValueInfo inInfo,
                                              boolean includeBlanks ) {
            if ( ! elClazz.isAssignableFrom( inInfo.getContentClass() ) ) {
                throw new IllegalArgumentException();
            }
            @SuppressWarnings("unchecked")
            Class<E[]> arrayClazz =
                (Class<E[]>) Array.newInstance( elClazz, 0 ).getClass();
            return new ArrayAggregation<E[]>(
                       arrayClazz, inInfo,
                       () -> new ObjectArrayAccumulator<E>( elClazz,
                                                            includeBlanks ) );
        }

        /**
         * Skeleton aggregation implementation for use with ArrayAggregator.
         * A supplier must be provided for accumulators.
         */
        private static class ArrayAggregation<A> implements Aggregation {
            final ValueInfo outInfo_;
            final Supplier<ArrayAccumulator<A,?>> accSupplier_;

            /**
             * Constructor.
             *
             * @param   arrayClazz  class of output array values
             * @param   inInfo      metadata of input array values
             * @param   accSupplier  supplier for suitable accumulators
             */
            ArrayAggregation( Class<A> arrayClazz, ValueInfo inInfo,
                              Supplier<ArrayAccumulator<A,?>> accSupplier ) {
                String inName = inInfo.getName();
                String descrip = "Collection of all values for " + inName;
                outInfo_ = new DefaultValueInfo( inName, arrayClazz, descrip );
                accSupplier_ = accSupplier;
            }

            public ValueInfo getResultInfo() {
                return outInfo_;
            }

            public Accumulator createAccumulator() {
                return accSupplier_.get();
            }
        }

        /**
         * Abstract accumulator superclass for use with array aggregators.
         * An implementation-specific list object of type &lt;L&gt; is used
         * for intermediate storage of the array elements.
         */
        private static abstract class ArrayAccumulator<A,L>
                                implements Accumulator {
            final L list_;

            /**
             * Constructor.
             *
             * @param  list  list instance for storing elements
             */
            ArrayAccumulator( L list ) {
                list_ = list;
            }

            public abstract A getResult();

            public void add( Accumulator other ) {
                @SuppressWarnings("unchecked")
                L otherList = (L) ((ArrayAccumulator) other).list_;
                addList( otherList );
            }

            /**
             * Adds the content of the list from another compatible
             * ArrayAccumulator to the the list of this one.
             *
             * @param  otherList  list from other compatible accumulator
             */
            abstract void addList( L otherList );
        }

        /**
         * Accumulator implementation for cases where the output array
         * is an array of the input scalar objects.
         */
        private static class ObjectArrayAccumulator<E>
                extends ArrayAccumulator<E[],List<E>> {
            final Class<E> elClazz_;
            final boolean includeBlanks_;

            /**
             * Constructor.
             *
             * @param  elClazz  input element class
             * @param  includeBlanks  whether to include blank values
             */
            ObjectArrayAccumulator( Class<E> elClazz, boolean includeBlanks ) {
                super( new ArrayList<E>() );
                elClazz_ = elClazz;
                includeBlanks_ = includeBlanks;
            }

            public void submit( Object datum ) {
                if ( elClazz_.isInstance( datum ) ) {
                    E edatum = elClazz_.cast( datum );
                    if ( includeBlanks_ || ! Tables.isBlank( datum ) ) {
                        list_.add( edatum );
                    }
                }
                else if ( includeBlanks_ ) {
                    list_.add( null );
                }
            }

            public E[] getResult() {
                @SuppressWarnings("unchecked")
                E[] array0 = (E[]) Array.newInstance( elClazz_, 0 );
                return list_.toArray( array0 );
            }

            public void addList( List<E> otherList ) {
                list_.addAll( otherList );
            }
        }
    }

    /**
     * PrimitiveList-like object that can store a sequence of boolean values.
     * This is built on top of java.util.BitSet, but needs a bit more work
     * since BitSet does not know how many bits it actually contains.
     */
    private static class BooleanList {
        private final BitSet bitset_;
        private int nbit_;

        BooleanList() {
            bitset_ = new BitSet();
        }

        /**
         * Adds an entry to the list.
         *
         * @param  bit  boolean value
         */
        public void add( boolean bit ) {
            if ( bit ) {
                bitset_.set( nbit_ );
            }
            nbit_++;
        }

        /**
         * Returns a boolean array the same length as the number of calls
         * to {@link #add}.
         *
         * @return  output array
         */
        public boolean[] toBooleanArray() {
            boolean[] bits = new boolean[ nbit_ ];
            for ( int ipos = bitset_.nextSetBit( 0 ); ipos >= 0;
                  ipos = bitset_.nextSetBit( ipos + 1 ) ) {
                bits[ ipos ] = true;
            }
            return bits;
        }

        /**
         * Adds the content of another BooleanList to this one.
         *
         * @param  otherList  other list
         */
        public void addAll( BooleanList otherList ) {

            // This implementation is probably not optimal.
            for ( int i = 0; i < otherList.nbit_; i++ ) {
                add( otherList.bitset_.get( i ) );
            }
        }
    }
}
