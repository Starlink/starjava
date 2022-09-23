package uk.ac.starlink.ttools.plot2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.logging.Logger;
import uk.ac.starlink.util.SplitCollector;
import uk.ac.starlink.util.SplitPolicy;
import uk.ac.starlink.util.SplitProcessor;
import uk.ac.starlink.util.Splittable;

/**
 * Utility class for making use of SplitProcessor instances.
 *
 * @author  Mark Taylor
 * @since   17 Sep 2019
 */
public abstract class SplitRunner<S extends Splittable<S>> {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );
    private static final SplitPolicy DFLT_POLICY = createDefaultPolicy();

    /**
     * Performs a collection operation.
     * This usually does not do accumulator pooling.
     *
     * @param  collector   collector
     * @param  splitSupplier  supplier for splittable object to process
     * @return   collected result
     */
    public abstract <A> A collect( SplitCollector<S,A> collector,
                                   Supplier<S> splitSupplier );

    /**
     * Performs a collection operation with a hint that accumulator pooling
     * is recommended.
     *
     * @param  collector   collector
     * @param  splitSupplier  supplier for splittable object to process
     */
    public abstract <A> A collectPool( SplitCollector<S,A> collector,
                                       Supplier<S> splitSupplier );

    /**
     * Indicates whether an attempt will be made to split a top-level
     * splittable in order to process it.  If it's too small for instance,
     * false will be returned.
     *
     * @param  content  splittable
     * @return   true iff processing will try to split content
     */
    public abstract boolean willAttemptSplit( S content );

    /**
     * Returns an instance that uses SplitProcessor instances in the normal way
     * with the default concurrency policy.
     *
     * @return  default parallel instance
     */
    public static <S extends Splittable<S>> SplitRunner<S>
            createDefaultRunner() {
        return createStandardRunner( DFLT_POLICY );
    }

    /**
     * Returns an instance that uses SplitProcessor instances in the normal way
     * with a given concurrency policy.
     *
     * @param  policy   concurrency policy
     * @return  standard parallel instance
     */
    public static <S extends Splittable<S>> SplitRunner<S>
            createStandardRunner( final SplitPolicy policy ) {
        return policy.getForkJoinPool().getParallelism() > 1
            ? new PoolSwitchRunner<S>(
                  SplitProcessor.createBasicParallelProcessor( policy ),
                  SplitProcessor.createPoolParallelProcessor( policy )
              ) {
                  public boolean willAttemptSplit( S content ) {
                      return policy.willAttemptSplit( content );
                  }
                  @Override
                  public String toString() {
                      return "Standard[" + policy + "]";
                  }
              }
            : createSequentialRunner();
    }

    /**
     * Returns an instance that invokes multiple SplitProcessor instances
     * of different types and reports comparative timings to stdout.
     * This is not intended for production use, obviously.
     *
     * @return  benchmarking instance
     */
    public static <S extends Splittable<S>>
                  SplitRunner<S> createBenchRunner() {
        final SplitPolicy policy = DFLT_POLICY;
        return new BenchRunner<S>( Arrays.asList(
            SplitProcessor.createSequentialProcessor(),
            SplitProcessor.createBasicParallelProcessor( policy ),
            SplitProcessor.createPoolParallelProcessor( policy )
        ) ) {
            public boolean willAttemptSplit( S content ) {

                /* This isn't quite right, since for the sequential part
                 * it won't split, but it's the best we can do. */
                return policy.willAttemptSplit( content );
            }
            @Override
            public String toString() {
                return "Bench[" + policy + "]";
            }
        };
    }

    /**
     * Returns an instance that performs single-threaded sequential
     * execution.
     *
     * @return  sequential execution instance
     */
    public static <S extends Splittable<S>>
                  SplitRunner<S> createSequentialRunner() {
        SplitProcessor<S> seqProc = SplitProcessor.createSequentialProcessor();
        return new PoolSwitchRunner<S>( seqProc, seqProc ) {
            public boolean willAttemptSplit( S content ) {
                return false;
            }
            @Override
            public String toString() {
                return "Sequential";
            }
        };
    }

    /**
     * Returns the SplitPolicy used by this runner.
     *
     * @return  concurrency policy
     */
    private static SplitPolicy createDefaultPolicy() {

        /* Use the common ForkJoinPool.  The parallelism for this
         * may be set using the system property
         * java.util.concurrent.ForkJoinPool.common.parallelism. */
        Supplier<ForkJoinPool> fjPoolSupplier = () -> ForkJoinPool.commonPool();

        /* The range suggested in the ForkJoinTask javadocs is 1e2-1e4,
         * so this is conservative; but for plots with fewer than 1e5
         * rows the plotting is in most cases usually 'fast enough'
         * for interactive use; we stick with sequential computation in
         * those cases because there's less to go wrong. */
        int minTaskSize = 100_000;

        /* The value 8 is suggested by the example in the Spliterator javadocs.
         * Since accumulator creation may in some cases be expensive,
         * and accumulator pooling is not always used even in those cases,
         * we do want to restrict the total number of tasks. */
        short maxTasksPerCore = 8;

        /* Return default policy. */
        SplitPolicy policy =
            new SplitPolicy( fjPoolSupplier, minTaskSize, maxTasksPerCore );
        logger_.info( "Default concurrency: " + policy );
        return policy;
    }

    /**
     * Implementation that will use one of two supplied SplitProcessors
     * depending on the accumulator pooling hint.
     */
    private static abstract class PoolSwitchRunner<S extends Splittable<S>>
            extends SplitRunner<S> {

        private final SplitProcessor<S> procNoPool_;
        private final SplitProcessor<S> procWithPool_;

        /**
         * Constructor.
         *
         * @param  procNoPool    processor to use if accumulator pooling
         *                       is not requested
         * @param  procWithPool  processor to use if accumulator pooling
         *                       is requested
         */
        PoolSwitchRunner( SplitProcessor<S> procNoPool,
                          SplitProcessor<S> procWithPool ) {
            procNoPool_ = procNoPool;
            procWithPool_ = procWithPool;
        }

        public <A> A collect( SplitCollector<S,A> collector,
                              Supplier<S> splitSupplier ) {
            return procNoPool_.collect( collector, splitSupplier.get() );
        }

        public <A> A collectPool( SplitCollector<S,A> collector,
                              Supplier<S> splitSupplier ) {
            return procWithPool_.collect( collector, splitSupplier.get() );
        }
    }

    /**
     * Implementation that will use all of a given list of SplitProcessors
     * one at a time, and report benchmarking results to standard output.
     * Not generally intended for production use, obviously.
     */
    private static abstract class BenchRunner<S extends Splittable<S>>
            extends SplitRunner<S> {

        private final List<SplitProcessor<S>> procs_;

        /**
         * Constructor.
         *
         * @param  procs  list of processors, each of which will be used
         */
        BenchRunner( List<SplitProcessor<S>> procs ) {
            procs_ = procs;
        }

        public <A> A collect( SplitCollector<S,A> collector,
                              Supplier<S> splitSupplier ) {
            StringBuffer tbuf = new StringBuffer();
            A result = null;
            int millis0 = -1;
            for ( SplitProcessor<S> proc : procs_ ) {
                long t0 = System.nanoTime();
                result = proc.collect( collector, splitSupplier.get() );
                int millis = (int) ( ( System.nanoTime() - t0 ) / 1_000_000 );
                tbuf.append( "   " )
                    .append( proc )
                    .append( ":" )
                    .append( String.format( "%5d", millis ) );
                if ( millis0 < 0 ) {
                    millis0 = millis;
                }
                else {
                    tbuf.append( " (" )
                        .append( millis > 0
                               ? String.format( "%4.1f",
                                                millis0 / (double) millis )
                               : "    " )
                        .append( ")" );
                }
            }
            System.out.println( tbuf.toString() );
            return result;
        }

        public <A> A collectPool( SplitCollector<S,A> collector,
                                  Supplier<S> splitSupplier ) {
            return collect( collector, splitSupplier );
        }
    }
}
