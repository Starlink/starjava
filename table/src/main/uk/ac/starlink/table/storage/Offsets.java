package uk.ac.starlink.table.storage;

import uk.ac.starlink.table.Tables;

/**
 * Encapsulates information about where to look in a DiskRowStore for
 * rows and columns.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
abstract class Offsets {

    /**
     * Returns the offset in bytes from the start of the stream 
     * at which the start of a given row can be found.
     * 
     * @param   lrow  row index
     * @return  byte offset of the start of row <code>lrow</code>
     */
    public abstract long getRowOffset( long lrow );

    /**
     * Returns the offset in bytes from the start of the stream 
     * at which a given cell is found.
     *
     * @param   lrow  row index
     * @param   icol  column index
     * @return  byte offset of the cell at <code>(lrow,icol)</code>
     */
    public abstract long getCellOffset( long lrow, int icol );

    /** 
     * Returns the total length of the data stream containing the 
     * serialized table.
     * 
     * @return   stream length in bytes
     */
    public abstract long getLength();

    /**
     * Indicates whether this offsets implementation is fixed (cheap)
     * or variable (expensive).
     *
     * @return  <code>true</code> iff all rows have the same structure
     */
    public abstract boolean isFixed();

    /**
     * Returns an Offsets object based on a given array of ColumnWidth
     * descriptions.
     *
     * @param  widths  array of objects describing the byte widths of
     *         each column in the table
     * @param  nrow   number of rows in the table
     * @return  new Offsets object describing where each cell is in
     *          the stream
     */
    public static Offsets getOffsets( final ColumnWidth[] widths, 
                                      final long nrow ) {
        boolean allFixed = true;
        for ( int icol = 0; icol < widths.length; icol++ ) {
            allFixed = allFixed && widths[ icol ].isConstant();
        }
        return allFixed ? (Offsets) new FixedOffsets( widths, nrow )
                        : (Offsets) new VariableOffsets( widths, nrow );
    }

    /**
     * Offsets implementation in which all all rows look the same.
     */
    private static class FixedOffsets extends Offsets {
        final long[] colOffs_;
        final long rowSize_;
        final long leng_;

        FixedOffsets( ColumnWidth[] widths, long nrow ) {
            int ncol = widths.length;
            colOffs_ = new long[ ncol ];
            long pos = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                colOffs_[ icol ] = pos;
                pos += widths[ icol ].getWidth( 0 );
            }
            rowSize_ = pos;
            leng_ = nrow * rowSize_;
        }

        public long getRowOffset( long lrow ) {
            return rowSize_ * lrow;
        }

        public long getCellOffset( long lrow, int icol ) {
            return rowSize_ * lrow + colOffs_[ icol ];
        }

        public long getLength() {
            return leng_;
        }

        public boolean isFixed() {
            return true;
        }
    }

    /**
     * Offsets implementation in which columns may have different widths
     * in different rows.
     */
    private static class VariableOffsets extends Offsets {
        final ColumnWidth[] widths_;
        final int nrow_;
        final long[] rowOffsets_;
        final long leng_;

        VariableOffsets( ColumnWidth[] widths, long nrow ) {
            widths_ = widths;
            nrow_ = Tables.checkedLongToInt( nrow );
            rowOffsets_ = new long[ nrow_ ];
            int ncol = widths.length;
            long pos = 0;
            for ( int irow = 0; irow < nrow_; irow++ ) {
                rowOffsets_[ irow ] = pos;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    pos += widths[ icol ].getWidth( irow );
                }
            }
            leng_ = pos;
        }

        public long getRowOffset( long lrow ) {
            int irow = Tables.checkedLongToInt( lrow );
            return rowOffsets_[ irow ];
        }

        public long getCellOffset( long lrow, int icol ) {
            long off = getRowOffset( lrow );
            for ( int ic = 0; ic < icol; ic++ ) {
                off += widths_[ ic ].getWidth( lrow );
            }
            return off;
        }

        public long getLength() {
            return leng_;
        }

        public boolean isFixed() {
            return false;
        }
    }
}
