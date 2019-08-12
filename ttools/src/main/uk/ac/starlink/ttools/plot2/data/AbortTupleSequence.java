package uk.ac.starlink.ttools.plot2.data;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Wrapping TupleSequence that tries to give up when a condition
 * becomes true.
 *
 * @author   Mark Taylor
 * @since    12 Aug 2019
 */
public class AbortTupleSequence extends WrapperTuple implements TupleSequence {

    private final TupleSequence base_;
    private final BooleanSupplier abortCondition_;
    private final int checkPeriod_;
    private final AtomicBoolean aborted_;
    private final BooleanSupplier checkCondition_;
    private int count_;

    /**
     * Constructor with checking period of 1 (every element).
     *
     * @param  base   base sequence
     * @param   abortCondition  condition on which the stream should stop
     */
    public AbortTupleSequence( TupleSequence base,
                               BooleanSupplier abortCondition ) {
        this( base, abortCondition, 1 );
    }

    /**
     * Constructor with specified checking period.
     *
     * @param  base   base sequence
     * @param   abortCondition  condition on which the stream should stop
     * @param   checkPeriod  approximate frequency of checking the condition
     */
    public AbortTupleSequence( TupleSequence base,
                               BooleanSupplier abortCondition,
                               int checkPeriod ) {
        this( base, abortCondition, checkPeriod, new AtomicBoolean( false ) );
    }

    /**
     * Constructor for internal use (recursion).
     *
     * @param  baseSplitter   base sequence
     * @param   abortCondition  condition on which the stream should stop
     * @param   checkPeriod  approximate frequency of checking the condition
     * @param   aborted   shared flag for whether stream should stop
     */
    private AbortTupleSequence( TupleSequence base,
                                BooleanSupplier abortCondition,
                                int checkPeriod, AtomicBoolean aborted ) {
        super( base );
        base_ = base;
        abortCondition_ = abortCondition;
        checkPeriod_ = checkPeriod;
        aborted_ = aborted;
        checkCondition_ =
              checkPeriod <= 1
            ? abortCondition
            : () -> count_++ % checkPeriod_ == 0 &&
                    abortCondition_.getAsBoolean();
    }

    public boolean next() {
        return checkRunning() && base_.next();
    }

    public AbortTupleSequence split() {
        TupleSequence baseSplit = base_.split();
        if ( baseSplit != null ) {
            return checkRunning()
                 ? new AbortTupleSequence( baseSplit, abortCondition_,
                                           checkPeriod_, aborted_ )
                 : new AbortTupleSequence( PlotUtil.EMPTY_TUPLE_SEQUENCE,
                                           () -> true, 0,
                                           new AtomicBoolean( true ) );
        }
        else {
            return null;
        }
    }

    public long splittableSize() {
        return base_.splittableSize();
    }

    /**
     * Indicates whether the abort condition has been detected.
     *
     *
     * @return  true iff the abort condition has been detected
     */
    public boolean isAborted() {
        return aborted_.get();
    }

    /**
     * Checks whether this spliterator should still be running
     * and updates the abort state as required.
     *
     * @return  true if iteration should continue
     */
    private boolean checkRunning() {
        if ( aborted_.get() ) {
            return false;
        }
        else if ( checkCondition_.getAsBoolean() ) {
            aborted_.set( true );
            return false;
        }
        else {
            return true;
        }
    }
}
