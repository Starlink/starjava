package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * ColumnSupplement whose columns are derived by calculating values
 * based on all the columns of another table.
 * To generate any of the columns of this table, a whole row of the
 * input supplement must be read.
 * Concrete implementations of this abstract class must implement the
 * {@link #calculate} method.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2012
 */
public abstract class CalculatorColumnSupplement implements ColumnSupplement {

    private final ColumnSupplement baseSup_;
    private final ColumnInfo[] outColInfos_;
    private Object[] currentOutRow_;
    private long iCurrentRow_;

    /**
     * Constructor.
     *
     * @param  baseSup   base supplement
     * @param  outColInfos  column metadata for the supplementary columns
     *         (length defines column count)
     */
    public CalculatorColumnSupplement( ColumnSupplement baseSup,
                                       ColumnInfo[] outColInfos ) {
        baseSup_ = baseSup;
        outColInfos_ = outColInfos;
        iCurrentRow_ = -1;
    }

    /**
     * Performs the calculations which populate the columns of this table.
     * The input is a row of the base table, and the output is the
     * row of this table.
     *
     * <p>The implementation must return a new array each time,
     * not repopulate the same <code>Object[]</code> array object.
     *
     * @param   inRow  input column values
     * @return  output column values (same size as column count)
     */
    protected abstract Object[] calculate( Object[] inRow ) throws IOException;

    public int getColumnCount() {
        return outColInfos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return outColInfos_[ icol ];
    }

    public synchronized Object[] getRow( long irow ) throws IOException {
        return calculate( baseSup_.getRow( irow ) );
    }

    public synchronized Object getCell( long irow, int icol )
            throws IOException {
        if ( irow != iCurrentRow_ ) {
            currentOutRow_ = getRow( irow );
            iCurrentRow_ = irow;
        }
        return currentOutRow_[ icol ];
    }

    public SupplementSequence createSequence( RowSequence rseq )
            throws IOException {
        final SupplementSequence sseq = baseSup_.createSequence( rseq );
        return new SupplementSequence() {
            private long iSeq_ = -1;
            private Object[] row_;
            public Object[] getRow( long irow ) throws IOException {
                if ( irow != iSeq_ ) {
                    row_ = calculate( sseq.getRow( irow ) );
                    iSeq_ = irow;
                }
                return row_;
            }
            public Object getCell( long irow, int icol ) throws IOException {
                Object[] row = getRow( irow );
                assert row == row_;
                return row[ icol ];
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
