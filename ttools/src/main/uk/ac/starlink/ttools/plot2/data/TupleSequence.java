package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.util.SplittableSequence;

/**
 * Interface for iterating over points to plot.
 * At each step, a tuple of values required for the plot is available.
 * Elements of the tuple are addressed by index, i.e. column.
 * Each step represents a point which is a candidate for plotting;
 * some rows of the original data set may be excluded if that data set
 * has been subsetted to produce this sequence.
 *
 * <p>This interface extends the {@link Tuple} interface,
 * and is also a SplittableSequence.
 * Objects implementing this interface allow access to each of their
 * fields at the current row only.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2013
 */
public interface TupleSequence
        extends Tuple,
                SplittableSequence<TupleSequence> {
}
