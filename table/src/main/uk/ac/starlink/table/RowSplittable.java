package uk.ac.starlink.table;

import java.util.function.LongSupplier;
import uk.ac.starlink.util.Splittable;

/**
 * RowSequence subinterface that is also suitable for parallel processing.
 *
 * @author   Mark Taylor
 * @since    4 Aug 2020
 */
public interface RowSplittable extends RowSequence, Splittable<RowSplittable> {

    /**
     * Returns a supplier for the index of the row currently being processed,
     * if possible.
     * The row index is a global value; when accessing the first row
     * of a top-level RowSplittable, the value returned by this supplier
     * should be the first index in the table.
     * But for objects resulting from splitting a top-level instance
     * into sub-instances, the return value should reflect the index
     * in the original sequence of rows; each index should be returned
     * from only one of the splittables resulting from splitting a
     * top-level instance.
     *
     * <p>Typically, the row index will start at 0 and increment for each row,
     * but the numbering may be different depending on the requirements
     * of the implementation or usage.
     *
     * <p>Before the {@link RowSequence#next} method has been called,
     * the return value will be one less than the first row index.
     * After <code>RowSequence.next</code> has returned false,
     * the value is undefined.
     *
     * <p>Depending on the implementation, it may not be possible to
     * determine the row index (for instance if the sequence is
     * split into sub-splittables of unknown size).
     * In such cases, this method returns null.
     * The null-ness of the return value must be the same for all
     * instances of the splittable hierarchy for a given sequence,
     * so don't return a non-null value for the first splittable
     * and then null values for some or all of its children.
     *
     * @return   supplier for the globally-referenced row index value
     *           of the current row of this sequence, or null
     */
    LongSupplier rowIndex();
}
