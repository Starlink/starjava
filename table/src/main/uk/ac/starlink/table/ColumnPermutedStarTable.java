package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Wrapper table which provides a view of a base table in which the
 * columns are permuted.  Each column in the wrapper table is a view 
 * of one in the base table.  It is permitted for the wrapper to contain
 * multiple views of the same column in the base, but note that modifying
 * a cell in one of these will modify it in the other.
 *
 * <p>An <code>int[]</code> array, <code>columnMap</code>,
 * is used to keep track of
 * which columns in this table correspond to which columns in the base table;
 * the <code>n</code>'th column in this table corresponds to the 
 * <code>columnMap[n]</code>'th column in the base table.
 * The <code>columnMap</code> array may contain duplicate entries, but all
 * its entries must be in the range
 * <code>0..baseTable.getColumnCount()-1</code>.
 * This table will have <code>columnMap.length</code> entries.
 *
 * <p>One implementation decision to be made is how to implement the
 * <code>getRow</code> methods on this table and on any RowSequence taken
 * out on it.  It could either invoke <code>getRow</code> on the base
 * table/sequence, or invoke <code>getCell</code> multiple times.
 * The performance characteristics are likely to be different for the
 * two, though it's not obvious which will be best.  In general,
 * if this table has the same number of columns (or more) than the
 * base table, then using <code>getRow</code> will be better, and if it
 * has many fewer, then <code>getCell</code> will be.
 * The user may choose between these behaviours using the 
 * <code>readRow</code> parameter in one of the constructors.
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
     * When <code>readRow</code> is not specified explicitly, this gives the
     * threshold for determining how <code>getRow</code> methods
     * are implemented.
     * If
     * <pre>
     * getColumnCount() / getBaseTable().getColumnCount() &gt; READROW_FRACTION
     * </pre>
     * then the base <code>getRow</code> method is implemented, otherwise
     * multiple calls of <code>getCell</code> are used.
     */
    public static final double READROW_FRACTION = 0.75;

    /**
     * Constructs a new <code>ColumnPermutedStarTable</code> 
     * from a base <code>StarTable</code> and a <code>columnMap</code> array,
     * making its own decision about how to read rows.
     *
     * @param  baseTable   the table on which this one is based
     * @param  columnMap  array describing where each column of this table
     *         comes from in <code>baseTable</code>
     */
    public ColumnPermutedStarTable( StarTable baseTable, int[] columnMap ) {
        super( baseTable );
        setColumnMap( columnMap );
    }

    /**
     * Constructs a new <code>ColumnPermutedStarTable</code>
     * from a base <code>StarTable</code> and <code>columnMap</code> array,
     * with explicit instruction about how to read rows.
     *
     * @param  baseTable  the table on which this one is based
     * @param  columnMap  array describing where each column of this table
     *         comes from in <code>baseTable</code>
     * @param  readRow    true if the <code>readRow</code>
     *         implementations should
     *         invoke <code>readRow</code> on the base table, false if they
     *         should use <code>readCell</code> instead
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
        RowSequence baseseq = baseTable.getRowSequence();
        return new WrapperRowSequence( baseseq, permuteMapper( baseseq ) );
    }

    public RowAccess getRowAccess() throws IOException {
        RowAccess baseAcc = baseTable.getRowAccess();
        return new WrapperRowAccess( baseAcc, permuteMapper( baseAcc ) );
    }

    public RowSplittable getRowSplittable() throws IOException {
        return new MappingRowSplittable( baseTable.getRowSplittable(),
                                         this::permuteMapper );
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

    /**
     * Maps an underlying RowData to a RowData for output from this table.
     *
     * @param  data  base RowData
     * @return   output RowData
     */
    private RowData permuteMapper( final RowData data ) {
        return readRow_
             ? new RowData() {
                   public Object getCell( int icol ) throws IOException {
                       return data.getCell( columnMap_[ icol ] );
                   }
                   public Object[] getRow() throws IOException {
                       return permuteRow( data.getRow() );
                   }
               }
             : new RowData() {
                   final int ncol = getColumnCount();
                   public Object getCell( int icol ) throws IOException {
                       return data.getCell( columnMap_[ icol ] );
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
}
