package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Wrapper table which provides a view of a base table in which the 
 * rows are permuted.  Each row in the wrapper table is a view
 * of one in the base table.  It is permitted for the wrapper to contain
 * multiple views of the same column in the base.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowPermutedStarTable extends WrapperStarTable {

    private long[] rowMap;

    /**
     * Constructs a new <tt>RowPermutedStarTable</tt> from a base table
     * and an array of <tt>long</tt>s describing which rows in the new
     * wrapper table correspond to which rows in the base one.
     * The new table will have <tt>rowMap.length</tt> rows, and 
     * row <tt>i</tt> in the new table will correspond to row 
     * <tt>rowMap[i]</tt> in the base one.  The <tt>rowMap</tt> array
     * may contain duplicate entries; any negative entry is treated as
     * a special case resulting in a 'blank' row of all null objects.
     * <p>
     * <tt>baseTable</tt> must provide random access.
     *
     * @param  baseTable  base table
     * @param  rowMap     array mapping rows in <tt>baseTable</tt> to rows in
     *                    the new permuted table
     * @throws IllegalArgumentException  if <tt>baseTable.isRandom</tt> returns
     *         <tt>false</tt>
     */
    public RowPermutedStarTable( StarTable baseTable, long[] rowMap ) {
        super( baseTable );
        if ( ! baseTable.isRandom() ) {
            throw new IllegalArgumentException( "No random access in base " +
                                                "table " + baseTable );
        }
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

    public RowSequence getRowSequence() {
        return new RandomRowSequence( this );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return baseTable.getCell( rowMap[ checkedLongToInt( irow ) ], icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        long baseRow = rowMap[ checkedLongToInt( irow ) ];
        return baseRow >= 0 ? baseTable.getRow( baseRow )
                            : new Object[ baseTable.getColumnCount() ];
    }
}
