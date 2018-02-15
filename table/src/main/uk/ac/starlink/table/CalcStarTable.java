package uk.ac.starlink.table;

import java.io.IOException;
import java.util.List;

/**
 * StarTable implementation whose column values are derived from
 * some single calculation on the column values of another table.
 * The idea is that the cell values in a given row of an instance
 * of this table are cheaply-obtained members of an object which
 * is obtained by a potentially expensive calculation on the cell
 * values of the corresponding row in the other table.
 *
 * @param  <C>  type of object that is calculated from each row of the
 *              input table, and supplies values to the corresponding
 *              row of this table
 */
public abstract class CalcStarTable<C> extends AbstractStarTable {

    private final StarTable base_;
    private final Col<C,?>[] columns_;
    private RowCalculation<C> rowCalc_;

    /**
     * Constructor.
     *
     * @param  base  input table
     * @param  columns   array defining the columns of this table
     */
    public CalcStarTable( StarTable base, Col<C,?>[] columns ) {
        base_ = base;
        columns_ = columns;
    }

    /**
     * Creates the calculation object for the current row of the input table.
     *
     * @param  baseSeq  row sequence of base table
     *                  positioned at row of interest
     * @return  calculation object corresponding to <code>baseSeq</code>
     *          current row
     */
    public abstract C createCalculation( RowSequence baseSeq )
        throws IOException;

    /**
     * Creates the calculation object for the given row of the input table.
     * Only works for random-access tables.
     *
     * @param  irow   row index for which calculation is required
     * @return  calculation object for input table row <code>irow</code>
     */
    public abstract C createCalculation( long irow ) throws IOException;

    /**
     * Returns the input table on which this table is based.
     *
     * @return   base table
     */
    public StarTable getBaseTable() {
        return base_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return columns_[ icol ].getInfo();
    }

    public int getColumnCount() {
        return columns_.length;
    }

    public long getRowCount() {
        return base_.getRowCount();
    }

    @Override
    public boolean isRandom() {
        return base_.isRandom();
    }

    public RowSequence getRowSequence() throws IOException {
        final RowSequence baseSeq = base_.getRowSequence();
        return new RowSequence() {
            private C calc_;
            public boolean next() throws IOException {
                calc_ = null;
                return baseSeq.next();
            }
            public Object getCell( int icol ) throws IOException {
                return getCalculatedCell( getCalculation(), icol );
            }
            public Object[] getRow() throws IOException {
                return getCalculatedRow( getCalculation() );
            }
            public void close() throws IOException {
                baseSeq.close();
            }
            private C getCalculation() throws IOException {
                if ( calc_ == null ) {
                    calc_ = createCalculation( baseSeq );
                }
                return calc_;
            }
        };
    }

    public Object[] getRow( long irow ) throws IOException {
        return getCalculatedRow( getCalculation( irow ) );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return getCalculatedCell( getCalculation( irow ), icol );
    }

    /**
     * Returns the calculation object for a given row.
     * 
     * @param  irow   row index for which calculation is required
     * @return  calculation object for input table row <code>irow</code>,
     *          lazily evaluated
     */
    private C getCalculation( long irow ) throws IOException {
        RowCalculation<C> rowCalc = rowCalc_;
        if ( rowCalc != null && rowCalc.irow_ == irow ) {
            return rowCalc.calc_;
        }
        else {
            C calc = createCalculation( irow );
            rowCalc_ = new RowCalculation<C>( irow, calc );
            return calc;
        }
    }

    /**
     * Returns the cell value for a given column of this table,
     * given the corresponding calculation object.
     *
     * @param  calculation  calculation object
     * @param  icol  column index
     * @return  cell value
     */
    private Object getCalculatedCell( C calculation, int icol ) {
        return columns_[ icol ].getValue( calculation );
    }

    /**
     * Returns the row value for this table,
     * given the corresponding calculation object.
     *
     * @param  calculation  calculation object
     * @return   row array value
     */
    private Object[] getCalculatedRow( C calculation ) {
        int nc = columns_.length;
        Object[] row = new Object[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            row[ ic ] = columns_[ ic ].getValue( calculation );
        }
        return row;
    }

    /**
     * Defines a column for use with this table implementation.
     * It supplies column metadata, and a mapping from a calculation object
     * to the column data.
     *
     * @param  <C>  calculation object type
     * @param  <T>  column content class type
     *              (must match ColumnInfo.getContentClass() result)
     */
    public interface Col<C,T> {

        /**
         * Returns the metadata for this column.
         *
         * @return  column metadata
         */
        ColumnInfo getInfo();

        /**
         * Returns the value for this column extracted from a given
         * calculation object.
         *
         * @param  calculation  calculation object
         * @return   column value
         */
        T getValue( C calculation );
    }

    /**
     * Utility class aggregating a row index and a calculation object.
     *
     * @param  <C>  calculation object type
     */
    private static class RowCalculation<C> {
        final long irow_;
        final C calc_;

        /**
         * Constructor.
         *
         * @param   irow   row index
         * @parma   calc   calculation object
         */
        RowCalculation( long irow, C calc ) {
            irow_ = irow;
            calc_ = calc;
        }
    }
}
