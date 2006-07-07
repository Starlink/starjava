package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Modifies a table by adding a set of columns to it which are calculated
 * as a function of existing columns.  Concrete subclasses must provide
 * an implementation of the {@link #calculateValues} method.
 *
 * <p>Some attempt is made to arrange things so that if the output 
 * columns are read for the same row, the <code>calculateValues</code>
 * method is called only once, for the sake of efficiency.
 *
 * @author   Mark Taylor
 * @since    30 Aug 2005
 */
public abstract class AddColumnsTable extends WrapperStarTable {

    private final StarTable baseTable_;
    private final int[] inColIndices_;
    private final ColumnInfo[] outColInfos_;
    private final int[] colMap_;
    private long currentRow_ = -1L;
    private Object[] outValues_;

    /**
     * Constructor.
     *
     * @param   baseTable  base table
     * @param   inColIndices  array of indices of the columns in the base
     *          table which are used to calculate the new columns in the 
     *          resulting table
     * @param   outColInfos  array of column info headers for each of the
     *          new columns which are added in the resulting table
     * @param   ipos    position which the first of the new columns is
     *          to occupy in the resulting table (other new columns
     *          follow immediately)
     */
    public AddColumnsTable( StarTable baseTable, int[] inColIndices, 
                            ColumnInfo[] outColInfos, int ipos ) {
        super( baseTable );
        baseTable_ = baseTable;
        inColIndices_ = inColIndices;
        outColInfos_ = outColInfos;

        /* Store a map of which column in the base table is used for each
         * column in this table.  Negative values indicate calculated
         * columns; -1 is the first one, -2 is the second and so on. */
        colMap_ = new int[ baseTable.getColumnCount() + outColInfos.length ];
        int j = 0;
        for ( int i = 0; i < colMap_.length; i++ ) {
            colMap_[ i ] = ( i >= ipos && i < ipos + outColInfos.length )
                         ? ipos - i - 1
                         : j++;
        }
        assert j == baseTable.getColumnCount();
    }

    /**
     * Calcuates the values for the new columns added to this table
     * based on the values from the relevant columns from the base table.
     *
     * @param  inValues  array of values from the base table, 
     *         one from each of the columns indicated by the
     *         <code>inColIndices</code> array
     * @return array of values to be included in this table,
     *         one for each of the columns indicated by the 
     *         <code>outColInfos</code> array
     */
    protected abstract Object[] calculateValues( Object[] inValues );

    public int getColumnCount() {
        return colMap_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        int ibase = colMap_[ icol ];
        return ibase >= 0 ? super.getColumnInfo( ibase )
                          : outColInfos_[ -1 - ibase ];
    }

    public Object getCell( long irow, int icol ) throws IOException {
        int ibase = colMap_[ icol ];
        if ( ibase >= 0 ) {
            return super.getCell( irow, ibase );
        }
        else {
            synchronized ( this ) {
                if ( irow != currentRow_ || currentRow_ < 0 ) {
                    currentRow_ = -1;
                    Object[] inValues = new Object[ inColIndices_.length ];
                    for ( int i = 0; i < inColIndices_.length; i++ ) {
                        inValues[ i ] = super.getCell( irow,
                                                       inColIndices_[ i ] );
                    }
                    outValues_ = calculateValues( inValues );
                    currentRow_ = irow;
                }
                assert irow == currentRow_;
                return outValues_[ -1 - ibase ];
            }
        }
    }

    public Object[] getRow( long irow ) throws IOException {
        return calculateRow( super.getRow( irow ) );
    }

    /**
     * Returns a full row of this table given a full row of the base table.
     *
     * @param   baseRow  row of base table
     * @return  row of this table
     */
    private Object[] calculateRow( Object[] baseRow ) {
        Object[] inValues = new Object[ inColIndices_.length ];
        for ( int i = 0; i < inColIndices_.length; i++ ) {
            inValues[ i ] = baseRow[ inColIndices_[ i ] ];
        }
        Object[] outVals = calculateValues( inValues );
        Object[] row = new Object[ colMap_.length ];
        for ( int icol = 0; icol < row.length; icol++ ) {
            int ibase = colMap_[ icol ];
            row[ icol ] = ibase >= 0 ? baseRow[ ibase ]
                                     : outVals[ -1 - ibase ];
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( super.getRowSequence() ) {

            private Object[] seqOutValues_;

            public boolean next() throws IOException {
                seqOutValues_ = null;
                return super.next();
            }

            public Object getCell( int icol ) throws IOException {
                int ibase = colMap_[ icol ];
                if ( ibase >= 0 ) {
                    return super.getCell( icol );
                }
                else {
                    if ( seqOutValues_ == null ) {
                        Object[] inValues = new Object[ inColIndices_.length ];
                        for ( int i = 0; i < inColIndices_.length; i++ ) {
                            inValues[ i ] = super.getCell( inColIndices_[ i ] );
                        }
                        seqOutValues_ = calculateValues( inValues );
                    }
                    return seqOutValues_[ -1 - ibase ];
                }
            }

            public Object[] getRow() throws IOException {
                return calculateRow( super.getRow() );
            }
        };
    }
}
