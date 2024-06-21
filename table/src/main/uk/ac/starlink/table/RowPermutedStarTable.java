package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;

/**
 * Wrapper table which provides a view of a base table in which the 
 * rows are permuted.  Each row in the wrapper table is a view
 * of one in the base table.  It is permitted for the wrapper to contain
 * multiple views of the same column in the base.
 * <p>
 * A <code>long[]</code> array, <code>rowMap</code>, is used to keep
 * track of which rows in this table correspond to which rows in the
 * base table; the <code>n</code>'th row in this table corresponds to the
 * <code>rowMap[n]</code>'th row in the base table.
 * The <code>rowMap</code> array may contain duplicate entries, but should
 * not contain any entries larger than the number of rows in the base table.
 * Any negative entry is treated as a special case resulting in a 'blank'
 * row of all null values.
 * It can be modified during the life of the table, but it's not a good
 * idea to do this while a <code>RowSequence</code> is active.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowPermutedStarTable extends WrapperStarTable {

    private long[] rowMap;

    /**
     * Constructs a new <code>RowPermutedStarTable</code> from a base table
     * and a <code>rowMap</code> array.
     * <p>
     * <code>baseTable</code> must provide random access.
     *
     * @param  baseTable  base table
     * @param  rowMap     array mapping rows in the new permuted table to 
     *                    rows in <code>baseTable</code>
     * @throws IllegalArgumentException  if <code>baseTable.isRandom</code>
     *         returns <code>false</code>
     */
    public RowPermutedStarTable( StarTable baseTable, long[] rowMap ) {
        super( baseTable );
        if ( ! baseTable.isRandom() ) {
            throw new IllegalArgumentException( "No random access in base " +
                                                "table " + baseTable );
        }
        this.rowMap = rowMap;
    }

    /**
     * Constructs a new RowPermutedTable with rows initially in unpermuted
     * order.
     *
     * @param  baseTable  base table
     */
    public RowPermutedStarTable( StarTable baseTable ) {
        this( baseTable, 
              new long[ Math.max( checkedLongToInt( baseTable.getRowCount() ),
                                  0 ) ] );
        int nrow = rowMap.length;
        for ( int i = 0; i < nrow; i++ ) {
            rowMap[ i ] = i;
        }
    }

    /**
     * Returns the mapping array.
     * 
     * @return  array mapping rows in this table to rows in the base table
     */
    public long[] getRowMap() {
        return rowMap;
    }

    /**
     * Sets the mapping array.
     *
     * @param  rowMap array mapping rows in this table to rows in the base table
     */
    public void setRowMap( long[] rowMap ) {
        this.rowMap = rowMap;
    }

    public long getRowCount() {
        return (long) rowMap.length;
    }

    /**
     * Returns true.
     */
    public boolean isRandom() {
        return true;
    }

    public RowAccess getRowAccess() throws IOException {
        final RowAccess baseAcc = baseTable.getRowAccess();
        final int ncol = baseTable.getColumnCount();
        final Object[] emptyRow = new Object[ ncol ];
        return new RowAccess() {
            private long baseIrow_ = -1;
            public void setRowIndex( long irow ) throws IOException {
                baseIrow_ = rowMap[ checkedLongToInt( irow ) ];
                baseAcc.setRowIndex( baseIrow_ );
            }
            public Object getCell( int icol ) throws IOException {
                return baseIrow_ >= 0 ? baseAcc.getCell( icol )
                                      : null;
            }
            public Object[] getRow() throws IOException {
                return baseIrow_ >= 0 ? baseAcc.getRow()
                                      : emptyRow();
            }
            public void close() throws IOException {
                baseAcc.close();
            }
            private Object[] emptyRow() {
                Arrays.fill( emptyRow, null );
                return emptyRow;
            }
        };
    }

    public RowSequence getRowSequence() throws IOException {
        return AccessRowSequence.createInstance( this );
    }

    public RowSplittable getRowSplittable() throws IOException {
        return Tables.getDefaultRowSplittable( this );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        long baseRow = rowMap[ checkedLongToInt( irow ) ];
        return baseRow >= 0 ? baseTable.getCell( baseRow, icol )
                            : null;
    }

    public Object[] getRow( long irow ) throws IOException {
        long baseRow = rowMap[ checkedLongToInt( irow ) ];
        return baseRow >= 0 ? baseTable.getRow( baseRow )
                            : new Object[ baseTable.getColumnCount() ];
    }
}
