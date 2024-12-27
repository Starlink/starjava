package uk.ac.starlink.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Invokes processing on Splittables.
 *
 * <p>Instances of this class perform a similar function to that
 * implemented in the Stream class from the Java 8 Streams framework.
 * There is much less functionality and cleverness here,
 * but the behaviour is predictable and permits external iteration
 * rather than imposing internal iteration.
 *
 * @author  Mark Taylor
 * @since   9 Sep 2019
 */
public abstract class SplitProcessor<S extends Splittable<S>> {

    private final String name_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /**
     * Constructor.
     *
     * @param  name  name of this instance
     */
    protected SplitProcessor( String name ) {
        name_ = name;
    }

    /**
     * Indicates whether this processor will attempt to split the given
     * splittable object when processing it.
     * If for instance its size is known to be too small for this processor's
     * policy, false will be returned.
     *
     * @param  content  splittable object
     * @return  true iff splitting will be attempted during processing
     */
    public abstract boolean willAttemptSplit( S content );

    /**
     * Collects content from a splittable object into an accumulator,
     * as defined by a provided collector.
     *
     * @param  collector  defines collection semantics
     * @param  content    input data
     * @return  accumulator into which content has been collected
     */
    public abstract <A> A collect( SplitCollector<S,A> collector, S content );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a SplitProcessor instance that performs sequential processing.
     *
     * @return  new processor
     */
    public static <S extends Splittable<S>> SplitProcessor<S>
            createSequentialProcessor() {
        return new SequentialProcessor<S>( "Sequential" );
    }

    /**
     * Returns a SplitProcessor instance that works in parallel,
     * with one accumulator created for every forked subtask.
     *
     * @param   policy  parallel execution policy, or null for default
     * @return  new processor
     */
    public static <S extends Splittable<S>> SplitProcessor<S>
            createBasicParallelProcessor( SplitPolicy policy ) {
        if ( policy == null ) {
            policy = new SplitPolicy();
        }
        return new BasicParallelProcessor<S>( "BasicParallel", policy );
    }

    /**
     * Returns a SplitProcessor instance that works in parallel,
     * with a pool of reusable accumulators shared by forked subtasks.
     * This is expected to be beneficial if accumulator construction or
     * combination is computationally expensive.  However, it's probably
     * not necessary when working with accumulators that simply have
     * a large memory footprint, since multiple accumulators ought not
     * to be in simultaneous use either way.
     *
     * @param   policy  parallel execution policy, or null for default
     * @return  new processor
     */
    public static <S extends Splittable<S>> SplitProcessor<S>
            createPoolParallelProcessor( SplitPolicy policy ) {
        if ( policy == null ) {
            policy = new SplitPolicy();
        }
        return new PoolParallelProcessor<S>( "PoolParallel", policy );
    }

    /**
     * Returns a suitable processor instance.
     * This will defer to one of
     * {@link #createSequentialProcessor createSequentialProcessor},
     * {@link #createBasicParallelProcessor createBasicParallelProcessor} or
     * {@link #createPoolParallelProcessor createPoolParallelProcessor},
     * depending on its arguments.
     *
     * @param   policy  parallel execution policy, or null for default
     * @param   isPool   true to prefer pooling
     * @return  new processor
     */
    public static <S extends Splittable<S>> SplitProcessor<S>
            createStandardProcessor( SplitPolicy policy, boolean isPool ) {
        if ( policy == null ) {
            policy = new SplitPolicy();
        }
        return policy.getForkJoinPool().getParallelism() > 1
             ? ( isPool ? createPoolParallelProcessor( policy )
                        : createBasicParallelProcessor( policy ) )
             : createSequentialProcessor();
    }

    /**
     * Attempts to split a splittable if its size is not already too small.
     *
     * @param  content  splittable
     * @param  minSize   minimum acceptable size of a split task
     * @return  if split has taken place, the other half of the
     *          supplied content; otherwise null
     */
    private static <S extends Splittable<S>> S maybeSplit( S content,
                                                           int minSize ) {
        long size = content.splittableSize();
        return size >= 0 && size >= 2 * minSize
             ? content.split()
             : null;
    }

    /**
     * Returns the minimum size into which a given splittable's subtasks
     * should be divided for execution.
     *
     * @param  content  top-level (so far unsplit) splittable object
     * @param  policy   concurrency policy
     * @return  maximum advised subtask size
     */
    private static int getMinSplitSize( Splittable<?> content,
                                        SplitPolicy policy ) {
        int minSize = policy.getMinTaskSize();
        short maxPerCore = policy.getMaxTasksPerCore();
        if ( maxPerCore > 0 ) {
            long size = content.splittableSize();
            if ( size >= 0 ) {
                int ncore = policy.getForkJoinPool().getParallelism();
                long limit = size / ( maxPerCore * ncore );
                return limit > Integer.MAX_VALUE
                     ? Integer.MAX_VALUE
                     : Math.max( (int) limit, minSize );
            }
        }
        return minSize;
    }

    /**
     * SplitProcessor that processes content sequentially.
     */
    private static class SequentialProcessor<S extends Splittable<S>>
            extends SplitProcessor<S> {

        /**
         * Constructor.
         *
         * @param  name  instance name
         */
        SequentialProcessor( String name ) {
            super( name );
        }

        public boolean willAttemptSplit( S content ) {
            return false;
        }

        public <A> A collect( SplitCollector<S,A> collector, S content ) {
            A acc = collector.createAccumulator();
            collector.accumulate( content, acc );
            return acc;
        }
    }

    /**
     * SplitProcessor that works in parallel, using a new accumulator
     * instance for every forked task.
     */
    private static class BasicParallelProcessor<S extends Splittable<S>>
            extends SplitProcessor<S> {

        private final SplitPolicy policy_;

        /**
         * Constructor.
         *
         * @param  name  instance name
         * @param  policy   defines details of parallelisation policy
         */
        BasicParallelProcessor( String name, SplitPolicy policy ) {
            super( name );
            policy_ = policy;
        }

        public boolean willAttemptSplit( S content ) {
            return policy_.willAttemptSplit( content );
        }

        public <A> A collect( SplitCollector<S,A> collector, S content ) {
            AtomicInteger nfork = new AtomicInteger();
            int minSize = getMinSplitSize( content, policy_ );
            ForkJoinTask<A> task =
                new BasicSplitTask<S,A>( null, collector, content, minSize,
                                         nfork );
            long start = System.nanoTime();
            A acc = policy_.getForkJoinPool().invoke( task );
            String msg = new StringBuffer()
               .append( this )
               .append( " - " )
               .append( "tasks: " )
               .append( nfork )
               .append( ", time: " )
               .append( ( System.nanoTime() - start ) / 1000000 )
               .toString();
            logger_.info( msg );
            return acc;
        }

        /**
         * ForkJoinTask used for content collection in which
         * a new accumulator is created every time one is used.
         */
        private static class BasicSplitTask<S extends Splittable<S>,A>
                extends CountedCompleter<A> {

            private final SplitCollector<S,A> collector_;
            private final S content_;
            private final int minSize_;
            private final AtomicInteger nfork_;
            private BasicSplitTask<S,A> sibling_;
            private A result_;

            /**
             * Constructor.
             *
             * @param   parent  owner task
             * @param   collector  collector
             * @param   content   input data
             * @param   minSize   minimum size of split tasks
             * @param   nfork  task fork counter
             */
            BasicSplitTask( BasicSplitTask<S,A> parent,
                            SplitCollector<S,A> collector, S content,
                            int minSize, AtomicInteger nfork ) {
                super( parent );
                collector_ = collector;
                content_ = content;
                minSize_ = minSize;
                nfork_ = nfork;
            }

            public void compute() {
                S content1 = maybeSplit( content_, minSize_ );
                if ( content1 != null ) {
                    BasicSplitTask<S,A> t0 =
                       new BasicSplitTask<S,A>( this, collector_, content_,
                                                minSize_, nfork_ );
                    BasicSplitTask<S,A> t1 =
                       new BasicSplitTask<S,A>( this, collector_, content1,
                                                minSize_, nfork_ );
                    t0.sibling_ = t1;
                    t1.sibling_ = t0;
                    setPendingCount( 1 );
                    t1.fork();
                    t0.compute();
                }
                else {
                    A accumulator = collector_.createAccumulator();
                    collector_.accumulate( content_, accumulator );
                    result_ = accumulator;
                    if ( nfork_ != null ) {
                        nfork_.incrementAndGet();
                    }
                    tryComplete();
                }
            }

            public void onCompletion( CountedCompleter<?> caller ) {
                if ( caller != this ) {
                    @SuppressWarnings("unchecked")
                    BasicSplitTask<S,A> child1 = (BasicSplitTask<S,A>) caller;
                    BasicSplitTask<S,A> child2 = child1.sibling_;
                    if ( child2 == null || child2.result_ == null ) {
                        result_ = child1.result_;
                    }
                    else {
                        result_ = collector_.combine( child1.result_,
                                                      child2.result_ );
                    }
                }
            }

            public A getRawResult() {
                return result_;
            }
        }
    }

    /**
     * SplitProcessor that works in parallel, with a pool of reusable
     * accumulators shared by forked tasks.
     */
    private static class PoolParallelProcessor<S extends Splittable<S>>
            extends SplitProcessor<S> {

        private final SplitPolicy policy_;

        /**
         * Constructor.
         *
         * @param  name  instance name
         * @param  policy   defines details of parallelisation policy
         */
        PoolParallelProcessor( String name, SplitPolicy policy ) {
            super( name );
            policy_ = policy;
        }

        public boolean willAttemptSplit( S content ) {
            return policy_.willAttemptSplit( content );
        }

        public <A> A collect( SplitCollector<S,A> collector, S content ) {
            Collection<A> accPool = new ArrayList<A>();
            AtomicInteger nfork = new AtomicInteger();
            int minSize = getMinSplitSize( content, policy_ );
            long t0 = System.nanoTime();
            ForkJoinTask<A> accTask =
                new PoolSplitTask<S,A>( null, collector, content, minSize,
                                        accPool, nfork );
            ForkJoinPool fjPool = policy_.getForkJoinPool();
            fjPool.invoke( accTask );
            int npool = accPool.size();
            long t1 = System.nanoTime();
            ForkJoinTask<A> joinTask =
                new AccumulatorJoinRecursiveTask<A>( collector, accPool );
            accPool = null;
            A result = fjPool.invoke( joinTask );
            long t2 = System.nanoTime();
            String msg = new StringBuffer()
               .append( this )
               .append( " - " )
               .append( "tasks: " )
               .append( nfork )
               .append( ", pool: " )
               .append( npool )
               .append( ", time: " )
               .append( ( t1 - t0 ) / 1000000 )
               .append( " + " )
               .append( ( t2 - t1 ) / 1000000 )
               .toString();
            logger_.info( msg );
            return result;
        }

        /**
         * ForkJoinTask used for content collection in which a pool of
         * reusable accumulators is used.
         */
        private static class PoolSplitTask<S extends Splittable<S>,A>
                extends CountedCompleter<A> {

            private final SplitCollector<S,A> collector_;
            private final S content_;
            private final int minSize_;
            private final Collection<A> accPool_;
            private final AtomicInteger nfork_;

            /**
             * Constructor.
             *
             * @param   parent  owner task
             * @param   collector  collector
             * @param   content   input data
             * @param   minSize   minimum size of split tasks
             * @param   accPool   pool of accumulators,
             *                    will be expanded as required
             * @param   nfork  task fork counter
             */
            PoolSplitTask( PoolSplitTask<S,A> parent,
                           SplitCollector<S,A> collector, S content,
                           int minSize, Collection<A> accPool,
                           AtomicInteger nfork ) {
                super( parent );
                collector_ = collector;
                content_ = content;
                minSize_ = minSize;
                accPool_ = accPool;
                nfork_ = nfork;
            }

            public void compute() {
                S content1 = maybeSplit( content_, minSize_ );
                if ( content1 != null ) {
                    PoolSplitTask<S,A> t0 =
                        new PoolSplitTask<S,A>( this, collector_, content_,
                                                minSize_, accPool_, nfork_ );
                    PoolSplitTask<S,A> t1 =
                        new PoolSplitTask<S,A>( this, collector_, content1,
                                                minSize_, accPool_, nfork_ );
                    setPendingCount( 1 );
                    t1.fork();
                    t0.compute();
                }
                else {
                    A accumulator = getAccumulator();
                    collector_.accumulate( content_, accumulator );
                    releaseAccumulator( accumulator );
                    if ( nfork_ != null ) {
                        nfork_.incrementAndGet();
                    }
                    tryComplete();
                }
            }

            /**
             * Returns an accumulator ready for use.
             * Objects obtained using this method should be released when
             * no longer required.
             * May be called from any thread.
             *
             * @return  accumulator not in use by any other thread
             */
            private A getAccumulator() {
                synchronized ( accPool_ ) {
                    if ( accPool_.size() == 0 ) {
                        accPool_.add( collector_.createAccumulator() );
                    }
                    A item = accPool_.iterator().next();
                    boolean removed = accPool_.remove( item );
                    assert removed;
                    return item;
                }
            }

            /**
             * Releases an accumulator previously acquired from the pool.
             * It must not be used for further accumulation following this call.
             * May be called from any thread.
             *
             * @param  accumulator
             */
            private void releaseAccumulator( A accumulator ) {
                synchronized ( accPool_ ) {

                    /* I used to have an assertion here
                     * assert !accPool_.contains(accumulator).
                     * But don't do that - the pool may contain distinct
                     * entries that are equal by Object.equals(). */
                    accPool_.add( accumulator );
                }
            }
        }

        /**
         * ForkJoinTask that can combine multiple accumulators together.
         * This implementation uses a RecursiveTask doing divide and conquer.
         */
        private static class AccumulatorJoinRecursiveTask<A>
                extends RecursiveTask<A> {

            private final SplitCollector<?,A> collector_;
            private final Collection<A> accList_;

            /**
             * Constructor.
             *
             * @param   collector  collector
             * @param   accPool   list of populated accumulators to be combined
             */
            AccumulatorJoinRecursiveTask( SplitCollector<?,A> collector,
                                          Collection<A> accList ) {
                collector_ = collector;
                accList_ = accList;
            }

            public A compute() {
                int n = accList_.size();
                if ( n > 2 ) {
                    Pair<Collection<A>> pair = Pair.splitCollection( accList_ );
                    Collection<A> sub0 = pair.getItem1();
                    Collection<A> sub1 = pair.getItem2();
                    AccumulatorJoinRecursiveTask<A> t0 =
                        new AccumulatorJoinRecursiveTask<A>( collector_, sub0 );
                    AccumulatorJoinRecursiveTask<A> t1 =
                        new AccumulatorJoinRecursiveTask<A>( collector_, sub1 );
                    t1.fork();
                    return collector_.combine( t0.compute(), t1.join() );
                }
                else if ( n == 2 ) {
                    Iterator<A> it = accList_.iterator();
                    return collector_.combine( it.next(), it.next() );
                }
                else if ( n == 1 ) {
                    Iterator<A> it = accList_.iterator();
                    return it.next();
                }
                else {
                    throw new AssertionError();
                }
            }
        }
    }
}
