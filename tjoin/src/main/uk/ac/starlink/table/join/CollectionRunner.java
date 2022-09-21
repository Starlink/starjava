package uk.ac.starlink.table.join;

import java.util.Collection;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;
import uk.ac.starlink.util.SplitCollector;
import uk.ac.starlink.util.SplitPolicy;
import uk.ac.starlink.util.SplitProcessor;
import uk.ac.starlink.util.Splittable;

/**
 * Harnesses a SplitProcessor to perform parallel processing on
 * elements of a Java Collection.
 *
 * <p>This more or less duplicates part of the behaviour of the Java 8
 * streams framework, but it provides the additional control available
 * from {@link uk.ac.starlink.util.SplitProcessor}, and also works with a
 * {@link ProgressIndicator} to provide interruption control
 * and progress logging.
 *
 * @author   Mark Taylor
 * @since    21 Sep 2022
 */
public class CollectionRunner<E> {

    private final SplitProcessor<SpliteratorSplittable<E>> processor_;
    private static final int BLOCK_SIZE = 10_000;

    /** Untyped instance for sequential operation. */
    public static final CollectionRunner<?> SEQUENTIAL =
         new CollectionRunner<Object>( SplitProcessor
                                      .createSequentialProcessor() );

    /** Untyped instance using default policy. */
    public static final CollectionRunner<?> DFLT =
         new CollectionRunner<Object>(
             SplitProcessor
            .createBasicParallelProcessor( SplitPolicy.DFLT_POLICY ) );

    /**
     * Default constructor, using default policy.
     */
    public CollectionRunner() {
        this( SplitProcessor
             .createBasicParallelProcessor( SplitPolicy.DFLT_POLICY ) );
    }

    /**
     * Constructs an instance with a given SplitProcessor.
     *
     * @param  processor  controls behaviour
     */
    public CollectionRunner( SplitProcessor<?> processor ) {
        @SuppressWarnings("unchecked")
        SplitProcessor<SpliteratorSplittable<E>> cProcessor =
            (SplitProcessor<SpliteratorSplittable<E>>) processor;
        processor_ = cProcessor;
    }

    /**
     * Returns this runner's SplitProcessor.
     *
     * @return  split processor
     */
    public SplitProcessor<?> getSplitProcessor() {
        return processor_;
    }

    /**
     * Performs a collect operation without logging.
     *
     * @param   collector   defines behaviour
     * @param   collection  data on which to operate
     * @return  result of collection
     */
    public <A> A collect( ElementCollector<E,A> collector,
                          Collection<E> collection ) {
        return collect( collector, collection, null );
    }

    /**
     * Performs a collect operation with optional progress logging.
     *
     * @param   collector   defines behaviour
     * @param   collection  data on which to operate
     * @param   progger    progress logger
     * @return  result of collection
     */
    public <A> A collect( ElementCollector<E,A> collector,
                          Collection<E> collection,
                          ProgressIndicator progger ) {
        final double size1 = 1.0 / collection.size();
        final Tracker tracker = progger == null
                              ? null
                              : new Tracker( progger, collection.size() );
        SplitCollector<SpliteratorSplittable<E>,A> splitCollector =
                new SplitCollector<SpliteratorSplittable<E>,A>() {
            public A createAccumulator() {
                return collector.createAccumulator();
            }
            public void accumulate( SpliteratorSplittable<E> splittable,
                                    A acc ) {
                Spliterator<E> spliterator = splittable.spliterator_;
                Consumer<E> accConsumer = e -> collector.accumulate( e, acc );
                final int[] subCount = new int[ 1 ];
                final Consumer<E> consumer;
                if ( tracker == null ) {
                    consumer = accConsumer;
                }
                else {
                    consumer = e -> {
                        accConsumer.accept( e );
                        if ( ++subCount[ 0 ] >= BLOCK_SIZE ) {
                            tracker.addCount( subCount[ 0 ] );
                            subCount[ 0 ] = 0;
                        }
                    };
                }
                try {
                    spliterator.forEachRemaining( consumer );
                }
                catch ( TrackerInterruptedException e ) {
                    // ok
                }
                if ( subCount[ 0 ] > 0 ) {
                    tracker.addCount( subCount[ 0 ] );
                }
            }
            public A combine( A acc1, A acc2 ) {
                return collector.combine( acc1, acc2 );
            }
        };
        SpliteratorSplittable<E> splittable =
            new SpliteratorSplittable<E>( collection.spliterator() );
        return processor_.collect( splitCollector, splittable );
    }

    /**
     * Performs a collection operation using the Java 8 streams framework.
     * This does not use any of the implementation code associated with
     * SplitProcessor, it just creates a {@link java.util.stream.Collector}
     * based on the supplied <code>ElementCollector</code> and feeds the
     * stream to it.
     *
     * <p>This method is only intended for testing and comparison purposes;
     * if you actually want to use Java 8 streams, you might as well
     * use the Collector interface directly.
     *
     * @param  collector  collector
     * @param   stream  input stream
     * @return   result of collection operation
     */
    public static <E,A> A collectStream( ElementCollector<E,A> collector,
                                         Stream<E> stream ) {
        return stream.collect( Collector.of(
            collector::createAccumulator,
            (acc, el) -> collector.accumulate( el, acc ),
            collector::combine,
            new Collector.Characteristics[] {
                Collector.Characteristics.UNORDERED,
                Collector.Characteristics.IDENTITY_FINISH,
            }
        ) );
    }

    /**
     * Interface to define a collection operation for use with this class.
     * It resembles {@link uk.ac.starlink.util.SplitCollector},
     * but does not subclass it, since the elements it accumulates are
     * not Splittables.
     */
    public static interface ElementCollector<E,A> {

        /**
         * Returns a new accumulator into which results can be collected.
         * Accumulator instances may only be used from one thread at
         * any one time.
         * @return  new accumulator
         */
        A createAccumulator();

        /**
         * Consumes an element, accumulating it into the supplied accumulator.
         *
         * @param  element   object to accumulate
         * @param  accumulator  accumulator
         */
        void accumulate( E element, A accumulator );

        /**
         * Combines the content of two accumulators.
         * The returned value may or may not be the same object as
         * one of the input values.
         * The input values should not be used following this call.
         * The sequence of the input values is not significant.
         *
         * @param  acc1  one input accumulator
         * @param  acc2  other input accumulator
         * @return   accumulator containing the combined result of the inputs
         */
        A combine( A acc1, A acc2 );
    }

    /**
     * Splittable implementation based on a Java 8 Spliterator.
     */
    private static class SpliteratorSplittable<E>
            implements Splittable<SpliteratorSplittable<E>> {

        private final Spliterator<E> spliterator_;

        /**
         * Constructor.
         *
         * @param  spliterator  spliterator
         */
        public SpliteratorSplittable( Spliterator<E> spliterator ) {
            spliterator_ = spliterator;
        }

        public long splittableSize() {
            long size = spliterator_.estimateSize();
            return size == Long.MAX_VALUE ? -1 : size;
        }

        public SpliteratorSplittable<E> split() {
            Spliterator<E> subSpliterator = spliterator_.trySplit();
            return subSpliterator == null
                 ? null
                 : new SpliteratorSplittable<E>( subSpliterator );
        }
    }

    /**
     * Manages access to a ProgressIndicator.
     */
    private static class Tracker {

        final ProgressIndicator progger_;
        final double size1_;
        final AtomicInteger count_;

        /**
         * Constructor.
         *
         * @param   progger  progress indicator
         * @param   size     number of elements expected to be processed
         */
        Tracker( ProgressIndicator progger, int size ) {
            progger_ = progger;
            size1_ = size > 0 ? 1.0 / size : 0;
            count_ = new AtomicInteger();
        }

        /**
         * Logs that a given number of elements have been processed.
         *
         * @param   inc  increment to count
         * @throws  TrackerInterruptedException  if progress update signals
         *                                       an interrupt
         */
        public void addCount( int inc ) {
            try {
                progger_.setLevel( count_.addAndGet( inc ) * size1_ );
            }
            catch ( InterruptedException e ) {
                throw new TrackerInterruptedException( e );
            }
        }
    }

    /**
     * Specialist RuntimeException class to wrap an InterruptedException.
     */
    private static class TrackerInterruptedException extends RuntimeException {
        TrackerInterruptedException( InterruptedException e ) {
            super( "Interrupted", e );
        }
    }
}
