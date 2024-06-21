package uk.ac.starlink.table.storage;

import uk.ac.starlink.table.Tables;

/**
 * Defines the width in bytes of a table column.
 * A couple of factory methods are supplied to give provide instances.
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class ColumnWidth {

    /**
     * Returns the number of bytes used in row <code>lrow</code> to store the
     * data for this column. 
     *
     * @param   lrow  row index
     * @return  size in bytes
     */
    public abstract int getWidth( long lrow );

    /**
     * Indicates whether the return value of <code>getWidth</code> is a 
     * varying function of <code>lrow</code> or not.
     *
     * @return  if true, <code>getWidth</code> always gives the same answer
     */
    public abstract boolean isConstant();

    /**
     * Returns a column width object with the same width for every row.
     *
     * @param   width  width value constant for each row
     * @return  new ColumnWidth
     */
    public static ColumnWidth constantColumnWidth( final int width ) {
        return new ColumnWidth() {
            public int getWidth( long lrow ) {
                return width;
            }
            public boolean isConstant() {
                return true;
            }
        };
      
    } 

    /**
     * Returns a column width object with variable widths for different rows.
     *
     * @param   widths   array of widths, one for each row
     * @return  new ColumnWidth
     */
    public static ColumnWidth variableColumnWidth( final int[] widths ) {
        return new ColumnWidth() {
            public int getWidth( long lrow ) {
                return widths[ Tables.checkedLongToInt( lrow ) ];
            }
            public boolean isConstant() {
                return false;
            }
        };
    }
}
