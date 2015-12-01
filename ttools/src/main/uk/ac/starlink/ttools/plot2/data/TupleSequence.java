package uk.ac.starlink.ttools.plot2.data;

/**
 * Interface for iterating over points to plot.
 * At each step, a tuple of values required for the plot is available.
 * Elements of the tuple are addressed by index, i.e. column.
 * Each step represents a point which is a candidate for plotting;
 * some rows of the original data set may be excluded if that data set
 * has been subsetted to produce this sequence.
 *
 * <p>Different typed access methods are provided for retrieving the 
 * result of each column.  It is up to the caller to keep track of
 * which is the right one to use.  It's done this way so that we can
 * return primitive values.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2013
 */
public interface TupleSequence {

    /**
     * Move to the next item in the sequence.
     * Must be called before accessing each row, including the first one.
     * Returns false when there are no rows left.
     *
     * @return   true iff the item moved to has data
     */
    boolean next();

    /**
     * Returns the row index for the underlying data set.
     * Note this value may not undergo a simple increment between steps
     * (if rows are excluded from the underlying data set it may increase
     * by more than one in some cases).
     *
     * @return  row index of underlying table row
     */
    long getRowIndex();

    /**
     * Returns the value of a given column as an object.
     * If that element of the tuple is not of object type,
     * the result may not be useful.
     * If the result is a mutable object, its value may be overwritten by
     * subsequent calls following a call to {@link #next}.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code> in the current row,
     *          presumed of object type
     */
    Object getObjectValue( int icol );

    /**
     * Returns the value of a given column as a double.
     * If that element of the tuple is not of numeric type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code> in the current row,
     *          presumed of numeric type
     */
    double getDoubleValue( int icol );

    /**
     * Returns the value of a given column as an integer.
     * If that element of the tuple is not of numeric type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code> in the current row,
     *          presumed of numeric type
     */
    int getIntValue( int icol );

    /**
     * Returns the value of a given column as a boolean.
     * If that element of the table is not of boolean type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code> in the current row,
     *          presumed of boolean type
     */
    boolean getBooleanValue( int icol );
}
