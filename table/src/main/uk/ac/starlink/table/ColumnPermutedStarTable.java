package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Wrapper table which provides a view of a base table in which the
 * columns are permuted.  Each column in the wrapper table is a view 
 * of one in the base table.  It is permitted for the wrapper to contain
 * multiple views of the same column in the base, but note that modifying
 * a cell in one of these will modify it in the other.
 * <p>
 * An <tt>int[]</tt> array, <tt>columnMap</tt>, is used to keep track of
 * which columns in this table correspond to which columns in the base table;
 * the <tt>n</tt>'th column in this table corresponds to the 
 * <tt>columnMap[n]</tt>'th column in the base table.
 * The <tt>columnMap</tt> array may contain duplicate entries, but all
 * its entries must be in the range <tt>0..baseTable.getColumnCount()-1</tt>.
 * This table will have <tt>columnMap.length</tt> entries.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnPermutedStarTable extends WrapperStarTable {

    private int[] columnMap;

    /**
     * Constructs a new <tt>ColumnPermutedStarTable</tt> 
     * from a base <tt>StarTable</tt> and a <tt>columnMap</tt> array.
     *
     * @param  baseTable   the table on which this one is based
     * @param  columnMap  array describing where each column of this table
     *         comes from in <tt>baseTable</tt>
     */
    public ColumnPermutedStarTable( StarTable baseTable, int[] columnMap ) {
        super( baseTable );
        this.columnMap = columnMap;
    }

    /**
     * Returns the mapping used to define the permutation of the columns
     * of this table with respect to the base table.
     *
     * @return  column permutation map
     */
    public int[] getColumnMap() {
        return columnMap;
    }

    /**
     * Sets the mapping used to define the permutation of the columns
     * of this table with respect to the base table.
     *
     * @param  columnMap  column permutation map
     */
    public void setColumnMap( int[] columnMap ) {
        this.columnMap = columnMap;
    }

    public int getColumnCount() {
        return columnMap.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return baseTable.getColumnInfo( columnMap [ icol ] );
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( baseTable.getRowSequence() ) {
            public Object getCell( int icol ) throws IOException {
                return baseSeq.getCell( columnMap [ icol ] );
            }
            public Object[] getRow() throws IOException {
                return permuteRow( baseSeq.getRow() );
            }
        };
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return baseTable.getCell( irow, columnMap [ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        return permuteRow( baseTable.getRow( irow ) );
    }

    /**
     * Turns a row of the base table into a row of this table.
     *
     * @param  baseRow  a row from the base table
     * @return  the corresponding row in this table
     */
    private Object[] permuteRow( Object[] baseRow ) {
        int ncol = columnMap.length;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = baseRow[ columnMap [ icol ] ];
        }
        return row;
    }

}
