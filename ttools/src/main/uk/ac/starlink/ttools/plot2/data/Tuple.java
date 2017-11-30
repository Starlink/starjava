package uk.ac.starlink.ttools.plot2.data;

/**
 * Defines a tuple of typed values for plotting.  Such tuples are
 * implicitly part of a sequence, that is they represent a row in a table.
 * Elements of the tuple may be referred to as fields or columns,
 * and are accessed by field/column index.
 *
 * <p>Different typed access methods are provided for retrieving the 
 * result of each column.  It is up to the caller to keep track of
 * which is the right one to use.  It's done this way so that we can
 * return primitive values.
 *
 * <p>Note that in most cases a <code>Tuple</code> instance is also a
 * {@link TupleSequence}.  That means that once the TupleSequence has
 * been advanced to the next row, calling the Tuple methods will give
 * different values.  It follows that calls to a Tuple instance should
 * be made and the results consumed synchronously before the next
 * iteration, such instances are generally not suitable for hanging
 * on to outside of an iteration step.
 *
 * <p>The point of separating the two interfaces is to make it clear when
 * a client is only permitted to read the values and not to advance
 * the iterator.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2017
 */
public interface Tuple {

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
     *
     * <p>If the result is a mutable object, its value may be overwritten by
     * subsequent calls to this method
     * (especially following calls to {@link TupleSequence#next} if this
     * object also implements <code>TupleSequence</code>).
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code>,
     *          presumed of object type
     */
    Object getObjectValue( int icol );

    /**
     * Returns the value of a given column as a double.
     * If that element of the tuple is not of numeric type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code>,
     *          presumed of numeric type
     */
    double getDoubleValue( int icol );

    /**
     * Returns the value of a given column as an integer.
     * If that element of the tuple is not of numeric type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code>,
     *          presumed of numeric type
     */
    int getIntValue( int icol );

    /**
     * Returns the value of a given column as a long.
     * If that element of the tuple is not of numeric type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code>,
     *          presumed of numeric type
     */
    long getLongValue( int icol );

    /**
     * Returns the value of a given column as a boolean.
     * If that element of the table is not of boolean type,
     * the result may not be useful.
     *
     * @param   icol  column index
     * @return  value of element <code>icol</code>,
     *          presumed of boolean type
     */
    boolean getBooleanValue( int icol );
}
