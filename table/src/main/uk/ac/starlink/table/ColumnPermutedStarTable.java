package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Wrapper table which provides a view of a base table in which the
 * columns are permuted.  Each column in the wrapper table is a view 
 * of one in the base table.  It is permitted for the wrapper to contain
 * multiple views of the same column in the base, but note that modifying
 * a cell in one of these will modify it in the other.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnPermutedStarTable extends WrapperTable {

    private int[] colMap;
    private int ncol;

    /**
     * Constructs a new <tt>ColumnPermutedStarTable</tt> 
     * from a base <tt>StarTable</tt> and
     * an array describing which columns in the new wrapper table correspond
     * to which columns in the base one.
     * The new table will have <tt>colMap.length</tt> columns, and 
     * the column <tt>i</tt> in the new table will correspond to
     * column <tt>colMap[i]</tt> in <tt>baseTable</tt>.  
     * The <tt>colMap</tt> array may contain duplicate entries, but 
     * all its entries must be in the range
     * <tt>0..baseTable.getColumnCount()-1</tt>.
     *
     * @param  baseTable   the table on which this one is based
     * @param  colMap array describing where each column of this table
     *         comes from in <tt>baseTable</tt>
     */
    public ColumnPermutedStarTable( StarTable baseTable, int[] colMap ) {
        super( baseTable );
        this.colMap = (int[]) colMap.clone();
        ncol = colMap.length;

        /* Validate the permutation map. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( colMap[ icol ] < 0 || 
                 colMap[ icol ] > baseTable.getColumnCount() ) {
                throw new IllegalArgumentException(
                    "Illegal column permutation map: " +
                    "colMap[" + icol + "] outside range 0.." + 
                    baseTable.getColumnCount() );
            }
        }
    }

    /**
     * Returns the mapping used to define the permutation of the columns
     * of this table with respect to the base table.
     * Column <tt>i</tt> of this table is the same as column 
     * <tt>getColMap()[i]</tt> of the base table.
     *
     * @return  column permutation map
     */
    public int[] getColMap() {
        return (int[]) colMap.clone();
    }

    public int getColumnCount() {
        return ncol;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return baseTable.getColumnInfo( colMap[ icol ] );
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( baseTable.getRowSequence() ) {
            public Object getCell( int icol ) throws IOException {
                return baseSeq.getCell( colMap[ icol ] );
            }
            public Object[] getRow() throws IOException {
                return permuteRow( baseSeq.getRow() );
            }
        };
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return baseTable.getCell( irow, colMap[ icol ] );
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
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = baseRow[ colMap[ icol ] ];
        }
        return row;
    }

}
