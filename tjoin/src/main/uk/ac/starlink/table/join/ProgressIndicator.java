package uk.ac.starlink.table.join;

/**
 * Callback interface for indicating how an algorithm is progressing.
 * An instance can be passed to a time-consuming routine which will
 * make periodic calls on it as the work progresses.
 *
 * <p>Implementations must be thread-safe; calls to {@link #setLevel}
 * may come from multiple different threads.
 *
 * @author   Mark Taylor (Starlink)
 * @since    24 Mar 2004
 */
public interface ProgressIndicator {

    /**
     * Indicates that a number of {@link #setLevel} calls may follow,
     * followed by a {@link #endStage} call.
     *
     * @param   stage  name/description of the processing stage
     */
    void startStage( String stage );

    /**
     * Specifies that the work of the most recently-started stage is a certain
     * proportion complete.  Calls to this method must take place 
     * between paired calls to {@link #startStage} and {@link #endStage},
     * preferably with non-decreasing values of <code>level</code>.
     *
     * <p>The method may throw an InterruptedException as a message to the
     * caller that the work should be interrupted.  A caller which 
     * receives such an exception should stop using resources and tidy
     * up as soon as is convenient.
     *
     * @param   level   value between 0. and 1. indicating amount of completion
     * @throws  InterruptedException  as a message to the caller that the
     *          work is no longer required
     *          <em>Is this abuse of InterruptedException??</em>
     */
    void setLevel( double level ) throws InterruptedException;

    /**
     * Indicates that no more {@link #setLevel} calls will be made until
     * the next {@link #startStage}.
     */
    void endStage();

    /**
     * Registers a comment about the progress of the algorithm.
     * This should not be called between calls to {@link #startStage} 
     * and {@link #endStage}.
     *
     * @param  msg  message
     */
    void logMessage( String msg );
}
