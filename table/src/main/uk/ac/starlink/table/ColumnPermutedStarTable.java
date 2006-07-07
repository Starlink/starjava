package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Wrapper table which provides a view of a base table in which the
 * columns are permuted.  Each column in the wrapper table is a view 
 * of one in the base table.  It is permitted for the wrapper to contain
 * multiple views of the same column in the base, but note that modifying
 * a cell in one of these will modify it in the other.
 *
 * <p>An <tt>int[]</tt> array, <tt>columnMap</tt>, is used to keep track of
 * which columns in this table correspond to which columns in the base table;
 * the <tt>n</tt>'th column in this table corresponds to the 
 * <tt>columnMap[n]</tt>'th column in the base table.
 * The <tt>columnMap</tt> array may contain duplicate entries, but all
 * its entries must be in the range <tt>0..baseTable.getColumnCount()-1</tt>.
 * This table will have <tt>columnMap.length</tt> entries.
 *
 * <p>One implementation decision to be made is how to implement the
 * <tt>getRow</tt> methods on this table and on any RowSequence taken
 * out on it.  It could either invoke <tt>getRow</tt> on the base
 * table/sequence, or invoke <tt>getCell</tt> multiple times.
 * The performance characteristics are likely to be different for the
 * two, though it's not obvious which will be best.  In general,
 * if this table has the same number of columns (or more) than the
 * base table, then using <tt>getRow</tt> will be better, and if it
 * has many fewer, then <tt>getCell</tt> will be.
 * The user may choose between these behaviours using the 
 * <tt>readRow</tt> parameter in one of the constructors.
 * If the other constructor (which doesn't have this argument) is used
 * then a choice will be made on the basis of the ratio of columns
 * in this table to the ratio of the columns in the base table
 * (the threshold is given by {@link #READROW_FRACTION}).
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnPermutedStarTable extends WrapperStarTable {

    private int[] columnMap_;
    private boolean fixReadRow_;
    private boolean readRow_;

    /**
     * When <tt>readRow</tt> is not specified explicitly, this gives the
     * threshold for determining how <tt>getRow</tt> methods are implemented.
     * If
     * <pre>
     * getColumnCount() / getBaseTable().getColumnCount() &gt; READROW_FRACTION
     * </pre>
     * then the base <tt>getRow</tt> method is implemented, otherwise
     * multiple calls of <tt>getCell</tt> are used.
     */
    public static final double READROW_FRACTION = 0.75;

    /**
     * Constructs a new <tt>ColumnPermutedStarTable</tt> 
     * from a base <tt>StarTable</tt> and a <tt>columnMap</tt> array,
     * making its own decision about how to read rows.
     *
     * @param  baseTable   the table on which this one is based
     * @param  columnMap  array describing where each column of this table
     *         comes from in <tt>baseTable</tt>
     */
    public ColumnPermutedStarTable( StarTable baseTable, int[] columnMap ) {
        super( baseTable );
        setColumnMap( columnMap );
    }

    /**
     * Constructs a new <tt>ColumnPermutedStarTable</tt>
     * from a base <tt>StarTable</tt> and <tt>columnMap</tt> array,
     * with explicit instruction about how to read rows.
     *
     * @param  baseTable  the table on which this one is based
     * @param  columnMap  array describing where each column of this table
     *         comes from in <tt>baseTable</tt>
     * @param  readRow    true if the <tt>readRow</tt> implementations should
     *         invoke <tt>readRow</tt> on the base table, false if they
     *         should use <tt>readCell</tt> instead
     */
    public ColumnPermutedStarTable( StarTable baseTable, int[] columnMap,
                                    boolean readRow ) {
        this( baseTable, columnMap );
        readRow_ = readRow;
        fixReadRow_ = true;
    }

    /**
     * Returns the mapping used to define the permutation of the columns
     * of this table with respect to the base table.
     *
     * @return  column permutation map
     */
    public int[] getColumnMap() {
        return columnMap_;
    }

    /**
     * Sets the mapping used to define the permutation of the columns
     * of this table with respect to the base table.
     *
     * @param  columnMap  column permutation map
     */
    public void setColumnMap( int[] columnMap ) {
        columnMap_ = columnMap;
        if ( ! fixReadRow_ ) {
            readRow_ = 
                ( getColumnCount() / (double) getBaseTable().getColumnCount() )
                > READROW_FRACTION;
        }
    }

    public int getColumnCount() {
        return columnMap_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return baseTable.getColumnInfo( columnMap_[ icol ] );
    }

    public RowSequence getRowSequence() throws IOException {
        final int ncol = getColumnCount();
        return readRow_
             ? (RowSequence)
               new WrapperRowSequence( baseTable.getRowSequence() ) {
                   public Object getCell( int icol ) throws IOException {
                       return baseSeq.getCell( columnMap_[ icol ] );
                   }
                   public Object[] getRow() throws IOException {
                       return permuteRow( baseSeq.getRow() );
                   }
               }
             : (RowSequence)
               new WrapperRowSequence( baseTable.getRowSequence() ) {
                   public Object getCell( int icol ) throws IOException {
                       return baseSeq.getCell( columnMap_[ icol ] );
                   }
                   public Object[] getRow() throws IOException {
                       Object[] row = new Object[ ncol ];
                       for ( int icol = 0; icol < ncol; icol++ ) {
                           row[ icol ] = getCell( icol );
                       }
                       return row;
                   }
               };
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return baseTable.getCell( irow, columnMap_[ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( readRow_ ) {
            return permuteRow( baseTable.getRow( irow ) );
        }
        else {
            int ncol = columnMap_.length;
            Object[] row = new Object[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                row[ icol ] = getCell( irow, icol );
            }
            return row;
        }
    }

    /**
     * Turns a row of the base table into a row of this table.
     *
     * @param  baseRow  a row from the base table
     * @return  the corresponding row in this table
     */
    private Object[] permuteRow( Object[] baseRow ) {
        int ncol = columnMap_.length;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = baseRow[ columnMap_[ icol ] ];
        }
        return row;
    }

    /**
     * Returns a <code>ColumnPermutedStarTable</code> with selected
     * columns deleted.
     * This utility method simply works out a permutation map which
     * corresponds to deletion of the columns indexed by the elements
     * of the <code>icols</code> array and passes it to the constructor.
     * A single column index may harmlessly be listed multiple times
     * in the <code>icols</code> array.  It is not permitted for any
     * element of <code>icols</code> to be less than zero or
     * greater or equal than the number of columns in <code>baseTable</code>.
     *
     * @param   baseTable  table whose columns are to be deleted
     * @param   icols   array with elements listing the column indices
     *          which are to be removed
     */
    public static ColumnPermutedStarTable deleteColumns( StarTable baseTable,
                                                         int[] icols ) {
        int nIn = baseTable.getColumnCount();
        boolean[] delFlags = new boolean[ nIn ];
        for ( int i = 0; i < icols.length; i++ ) {
            delFlags[ icols[ i ] ] = true;
        }
        int nOut = nIn;
        for ( int i = 0; i < nIn; i++ ) {
            if ( delFlags[ i ] ) {
                nOut--;
            }
        }
        int[] colMap = new int[ nOut ];
        int j = 0;
        for ( int i = 0; i < nIn; i++ ) {
            if ( ! delFlags[ i ] ) {
                colMap[ j++ ] = i;
            }
        }
        assert j == nOut;
        return new ColumnPermutedStarTable( baseTable, colMap );
    }
}
