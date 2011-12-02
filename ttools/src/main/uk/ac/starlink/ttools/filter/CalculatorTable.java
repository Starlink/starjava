package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Creates a new table whose columns are derived by calculating values
 * based on all the columns of another table.
 * To generate any of the columns of this table, a whole row of the
 * input table must be read.
 * Concrete implementations of this abstract class must implement the
 * {@link #calculate} method.
 *
 * <p>Table randomness, row count etc is taken from the input table,
 * but not table parameters.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2011
 */
public abstract class CalculatorTable extends AbstractStarTable {

    private final StarTable inTable_;
    private final ColumnInfo[] outColInfos_;
    private Object[] currentOutRow_;
    private long iCurrentRow_;

    /**
     * Constructor.
     *
     * @param  inTable   input table
     * @param  outColInfos  column metadata for the constructed table
     *                      (defines column count)
     */
    public CalculatorTable( StarTable inTable, ColumnInfo[] outColInfos ) {
        inTable_ = inTable;
        outColInfos_ = outColInfos;
        iCurrentRow_ = -1;
    }

    /**
     * Performs the calculations which populate the columns of this table.
     * The input is a row of the input table, and the output is the
     * row of this table.
     *
     * <p>The implementation must return a new array each time,
     * not repopulate the same <code>Object[]</code> array object.
     *
     * @param   inRow  input column values
     * @return  output column values (same size as column count)
     */
    protected abstract Object[] calculate( Object[] inRow ) throws IOException;

    public long getRowCount() {
        return inTable_.getRowCount();
    }

    public int getColumnCount() {
        return outColInfos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return outColInfos_[ icol ];
    }

    public boolean isRandom() {
        return inTable_.isRandom();
    }

    public synchronized Object[] getRow( long irow ) throws IOException {
        return calculate( inTable_.getRow( irow ) );
    }

    public synchronized Object getCell( long irow, int icol )
            throws IOException {
        if ( irow != iCurrentRow_ ) {
            currentOutRow_ = getRow( irow );
            iCurrentRow_ = irow;
        }
        return currentOutRow_[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {
        final RowSequence inSeq = inTable_.getRowSequence();
        return new RowSequence() {
            private Object[] row_;
            public boolean next() throws IOException {
                if ( inSeq.next() ) {
                    row_ = null;
                    return true;
                }
                else {
                    return false;
                }
            }
            public Object[] getRow() throws IOException {
                if ( row_ == null ) {
                    row_ = calculate( inSeq.getRow() );
                }
                return row_;
            }
            public Object getCell( int icol ) throws IOException {
                return getRow()[ icol ];
            }
            public void close() throws IOException {
                inSeq.close();
            }
        };
    }

    /**
     * Utility method to turn an Object into a floating point value.
     * If the submitted value is not a Number (including if it is null),
     * NaN will be returned.
     *
     * @param  value  object value
     * @return  floating point value
     */
    public static double getDouble( Object value ) {
        return value instanceof Number ? ((Number) value).doubleValue()
                                       : Double.NaN;
    }
}
