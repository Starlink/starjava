package uk.ac.starlink.votable;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;

/**
 * Contains the actual cell contents of a VOTable TABLE element.
 * <p>
 * The cell data may always be retrieved sequentially from the table
 * by acquiring a <tt>RowSequence</tt> object (method {@link #getRowSequence}) -
 * this allows you to read all the data from the first row to the last.
 * Multiple <tt>RowSequences</tt> may be simultaneously active.
 * In some cases random access may also be available; if {@link #isRandom}
 * returns true, then the {@link #getRow} and {@link #getCell} methods
 * can be used to retrieve cell values in any order.
 * <p>
 * The objects retrieved from cells in a given column are of course
 * determined by the corresponding FIELD element ({@link FieldElement} object),
 * in particular its <tt>arraysize</tt> and <tt>datatype</tt> attributes.
 * What object is returned from each column is described by the following
 * rules:
 * <ul>
 * <li>If the element is a scalar or (fixed-dimension) 1-element array,
 *     a primitive wrapper object (<tt>Integer</tt>, <tt>Float</tt> etc)
 *     will be normally be returned
 * <li>If the element is an array, a java array of primitives
 *     (<tt>int[]</tt>, <tt>float[]</tt> etc) will normally be returned.
 *     This is stored in column-major order, where that makes a
 *     difference (for arrays with more than one dimension).
 * <li>Complex types types are treated by adding an extra dimension to the
 *     shape of the data, the most rapidly varying, of size 2.
 * <li>Character (<tt>char</tt> and <tt>unicodeChar</tt>) arrays are
 *     automatically turned into Strings or String arrays, with
 *     dimensionality one less than that suggested by the <tt>arraysize</tt>
 *     attribute
 * <li>The element may be <tt>null</tt>
 * </ul>
 * In any case the class of returned objects in a given column may be
 * determined by calling the {@link #getContentClass} method.
 *
 * @author   Mark Taylor
 */
public interface TabularData {

    /**
     * Returns the number of columns in the table data.
     *
     * @return   number of cells in each row
     */
    int getColumnCount();

    /**
     * Returns the number of rows in the table data.  If this cannot be
     * determined (easily), the value -1 may be returned.  The result will
     * always be positive if {@link #isRandom} returns true.
     *
     * @return   number of rows, or -1 if unknown
     */
    long getRowCount();

    /**
     * Returns a class to which all elements in a given column can be cast.
     *
     * @param  icol  the column (0-based)
     * @return  a class to which any non-null element returned by this 
     *          object in column <tt>icol</tt> will belong
     */
    Class getContentClass( int icol );

    /**
     * Returns an object which can iterate over all the rows in the
     * table data sequentially.
     *
     * @return  an object providing sequential access to the data
     */
    RowSequence getRowSequence() throws IOException;

    /**
     * Indicates whether random access is provided by this table.
     * Only if the result is true may the {@link #getRow} and {@link #getCell}
     * methods be used.
     *
     * @return  <tt>true</tt> iff random access methods are available
     */
    boolean isRandom();

    /**
     * Returns the contents of a given table cell (optional).
     * The class of the returned object will be compatible with that 
     * returned by <tt>getContentClass(icol)</tt>.
     * Only provided if <tt>getRandom</tt> returns true.
     * 
     * @param  irow  row index
     * @param  icol  column index
     * @return  contents of the cell at <tt>irow</tt>, <tt>icol</tt>
     * @throws  IOException  if there is I/O trouble
     * @throws  UnsupportedOperationException if <tt>isRandom</tt> returns false
     */
    Object getCell( long irow, int icol ) throws IOException;

    /**
     * Returns the contents of a given table row (optional).
     * Only provided if <tt>getRandom</tt> returns true.
     *
     * @param  irow  row index
     * @return  array of objects giving the cells in row <tt>irow</tt>
     *          (one cell per column)
     * @throws  IOException  if there is I/O trouble
     * @throws  UnsupportedOperationException if <tt>isRandom</tt> returns false
     */
    public Object[] getRow( long irow ) throws IOException;
}
