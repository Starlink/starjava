package uk.ac.starlink.table;

import java.util.List;

/**
 * Defines basic table functionality.
 * A table consists of <i>Nrow</i> rows and <i>Ncol</i> columns; the 
 * first row and first column are numbered 0.
 * <p>
 * This interface models a table as a grid whose dimensions are known.
 * Note this is different from the VOTable model of a table whose 
 * columns are known, but of which the number of rows is unknown until
 * all these rows have been read (they may be streaming).
 *
 * @author   Mark Taylor (Starlink)
 */
public interface StarTable {

    /**
     * Returns the number of columns in this table.
     *
     * @return  the number of columns
     */
    int getColumnCount();

    /**
     * Returns the number of rows in this table.
     *
     * @return  the number of rows
     */
    int getRowCount();

    /**
     * Returns the contents of a given table cell.  The class of the returned
     * object should be the same as, or a subclass of, the class returned
     * by the corresponding {@link ColumnHeader#getContentClass} call.
     * <p>
     * For a numeric value, either one of the primitive type wrapper classes
     * (java.lang.Integer etc) may be used, or a 1-element array
     * of the relevant primitive type.
     *
     * @param  irow  the index of the cell's row (first row is 0)
     * @param  icol  the index of the cell's column (first column is 0)
     * @return  the contents of this cell
     */
    Object getValueAt( int irow, int icol );

    /**
     * Returns the header object describing the data in a given column.
     *
     * @param   icol  the column for which header information is required
     * @return  a header object for column <tt>icol</tt>
     */
    ColumnHeader getHeader( int icol );

    /**
     * Returns an list of the strings which may crop up as keys in the
     * metadata maps returned by the <tt>metadata</tt> method of the
     * <tt>ColumnHeader</tt> objects associated with this table.
     * The order of the list may indicate some sort of natural ordering
     * of these keys.  The returned list is not guaranteed to be complete;
     * it is legal to return an empty list if nothing is known about
     * metadata.
     *
     * @return  an unmodifiable ordered list of known metadata keys
     * @see  ColumnHeader#getMetadata
     */
    List getColumnMetadataKeys();
}
