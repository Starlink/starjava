package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowData;
import uk.ac.starlink.table.StarTable;

/**
 * ColumnSupplement implementation which contains a selection of the
 * columns in the base table.  Each column is a view of one in the base table.
 *
 * <p>An <code>int[]</code> array, <code>colMap</code>, is used to keep track of
 * which columns in this table correspond to which columns in the base table;
 * the <code>i</code>'th column in this table corresponds to the
 * <code>colMap[i]</code>'th column in the base table.
 * The <code>colMap</code> array may contain duplicate entries, but all
 * its entries must be in the range <code>0..baseSup.getColumnCount()-1</code>.
 * This table will have <code>colMap.length</code> columns.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2012
 */
public class PermutedColumnSupplement implements ColumnSupplement {

    private final ColumnSupplement baseSup_;
    private final int[] colMap_;
    private final int ncol_;

    /**
     * Constructs a permuted column supplement based on a given
     * column supplement.
     *
     * @param  baseSup   column supplement supplying the base data
     * @param  colMap   array of column indices, one for each column in this
     *                  object
     */
    public PermutedColumnSupplement( ColumnSupplement baseSup, int[] colMap ) {
        baseSup_ = baseSup;
        colMap_ = colMap;
        ncol_ = colMap.length;
    }

    /**
     * Constructs a permuted column supplement based on a given table.
     *
     * @param   baseTable  table supplying the base data
     * @param  colMap   array of column indices, one for each column in this
     *                  object
     */
    public PermutedColumnSupplement( StarTable baseTable, int[] colMap ) {
        this( new UnitColumnSupplement( baseTable ), colMap );
    }

    public int getColumnCount() {
        return colMap_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return baseSup_.getColumnInfo( colMap_[ icol ] );
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return baseSup_.getCell( irow, colMap_[ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            row[ ic ] = baseSup_.getCell( irow, colMap_[ ic ] );
        }
        return row;
    }

    public SupplementData createSupplementData( RowData rdata )
            throws IOException {
        final SupplementData sdata = baseSup_.createSupplementData( rdata );
        return new SupplementData() {
            public Object getCell( long irow, int icol ) throws IOException {
                return sdata.getCell( irow, colMap_[ icol ] );
            }
            public Object[] getRow( long irow ) throws IOException {
                Object[] row = new Object[ ncol_ ]; 
                for ( int ic = 0; ic < ncol_; ic++ ) {
                    row[ ic ] = sdata.getCell( irow, colMap_[ ic ] );
                }
                return row;
            }
        };
    }
}
