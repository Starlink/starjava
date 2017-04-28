package uk.ac.starlink.ttools.plot2.data;

/**
 * Interface for iterating over points to plot.
 * At each step, a tuple of values required for the plot is available.
 * Elements of the tuple are addressed by index, i.e. column.
 * Each step represents a point which is a candidate for plotting;
 * some rows of the original data set may be excluded if that data set
 * has been subsetted to produce this sequence.
 *
 * <p>This interface extends the {@link Tuple} interface.
 * Objects implementing this interface allow access to each of their
 * fields at the current row only.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2013
 */
public interface TupleSequence extends Tuple {

    /**
     * Move to the next item in the sequence.
     * Must be called before accessing each row, including the first one.
     * Returns false when there are no rows left.
     *
     * @return   true iff the item moved to has data
     */
    boolean next();
}
