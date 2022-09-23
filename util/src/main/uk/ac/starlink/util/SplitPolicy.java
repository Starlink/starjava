package uk.ac.starlink.util;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Defines the details of a concurrency policy as used by SplitProcessor.
 *
 * @author   Mark Taylor
 * @since    5 Nov 2019
 */
public class SplitPolicy {

    private final Supplier<ForkJoinPool> fjPoolSupplier_;
    private final int minTaskSize_;
    private final short maxTasksPerCore_;
    private ForkJoinPool fjPool_;

    /**
     * Default value for minimum subtask size.
     * The current value for this is conservative, in that it will not
     * result in concurrency for relatively small jobs.
     * The (JDK8) {@link java.util.concurrent.ForkJoinTask} javadocs recommend
     * as a rough rule of thumb a value in the range of 1e2-1e4 steps,
     * so this possibly could be reduced.
     */
    public static final int DFLT_MIN_TASK_SIZE = 100_000;

    /**
     * Default value for maximum average number of subtasks per core.
     * The value of 8 is suggested by the example parallel computation
     * framework sketch in the (JDK8) {@link java.util.Spliterator} javadocs.
     */
    public static final short DFLT_MAX_TASKS_PER_CORE = 8;

    /** Default splitting policy. */
    public static final SplitPolicy DFLT_POLICY = new SplitPolicy();

    /**
     * Constructs a policy with default configuration.
     */
    public SplitPolicy() {
        this( null, 0, (short) -1 );
    }

    /**
     * Constructs a policy with supplied configuration options.
     *
     * @param  fjPoolSupplier  supplier for fork/join pool for execution,
     *                         or null to use the common pool;
     *                         if non-null this supplier will be used
     *                         a maximum of once for lazy acquisition
     * @param  minTaskSize  smallest acceptable size of sub-task
     *                      to split tasks into, or non-positive value
     *                      for default ({@link #DFLT_MIN_TASK_SIZE})
     * @param  maxTasksPerCore  maximum number of tasks (on average)
     *                          to be executed on each core as a result
     *                          of decomposition, or zero for no limit,
     *                          or negative value for default limit
     *                          ({@link #DFLT_MAX_TASKS_PER_CORE})
     */
    public SplitPolicy( Supplier<ForkJoinPool> fjPoolSupplier, int minTaskSize,
                        short maxTasksPerCore ) {
        fjPoolSupplier_ = fjPoolSupplier == null
                        ? () -> ForkJoinPool.commonPool()
                        : fjPoolSupplier;
        minTaskSize_ = minTaskSize > 0 ? minTaskSize
                                       : DFLT_MIN_TASK_SIZE;
        maxTasksPerCore_ = maxTasksPerCore >= 0 ? maxTasksPerCore
                                                : DFLT_MAX_TASKS_PER_CORE;
    }

    /**
     * Returns the ForkJoinPool used by this policy.
     *
     * @return  forkjoinpool
     */
    public synchronized ForkJoinPool getForkJoinPool() {
        if ( fjPool_ == null ) {
            fjPool_ = fjPoolSupplier_.get();
        }
        return fjPool_;
    }

    /**
     * Returns the smallest task size used by this policy.
     *
     * @return  smallest acceptable size of sub-task to split tasks into
     */
    public int getMinTaskSize() {
        return minTaskSize_;
    }

    /**
     * Returns the maximum number of tasks (on average) to be executed
     * on each core as a result of decomposition, or zero for no limit.
     *
     * @return   maximum tasks per core, or zero
     */
    public short getMaxTasksPerCore() {
        return maxTasksPerCore_;
    }

    /**
     * Indicates whether an attempt should be made to split a splittable
     * in order to process it.
     * If it's too small for instance, false will be returned.
     *
     * @param  content  splittable
     * @return   true iff processing will try to split content
     */
    public boolean willAttemptSplit( Splittable<?> content ) {
        long size = content.splittableSize();
        return size >= 0 && size >= 2 * getMinTaskSize();
    }

    @Override
    public String toString() {
        return new StringBuffer()
              .append( "SplitPolicy(" )
              .append( "parallelism=" )
              .append( getForkJoinPool().getParallelism() )
              .append( ", " )
              .append( "minTaskSize=" )
              .append( getMinTaskSize() )
              .append( ", " )
              .append( "maxTasksPerCore=" )
              .append( getMaxTasksPerCore() )
              .toString();
    }
}
