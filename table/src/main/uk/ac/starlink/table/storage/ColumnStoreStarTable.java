package uk.ac.starlink.table.storage;

import java.io.IOException;
import uk.ac.starlink.table.RandomRowSplittable;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * StarTable implementation which retrieves its data from 
 * {@link ColumnStore} objects.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class ColumnStoreStarTable extends WrapperStarTable {

    private final long nrow_;
    private final int ncol_;
    private final ColumnStore[] colStores_;
    private final ThreadLocal<ColumnReader>[] colReaders_;

    /**
     * Constructor.
     *
     * @param  template   template table supplying metadata
     * @param  nrow       number of rows in this table
     * @param  colStores  array of ColumnStore objects, one for each 
     *                    column in the table
     */
    public ColumnStoreStarTable( StarTable template, long nrow,
                                 ColumnStore[] colStores ) {
        super( template );
        nrow_ = nrow;
        ncol_ = template.getColumnCount();
        colStores_ = colStores;

        /* Prepare ThreadLocals to service the thread-safe table
         * random access methods. */
        @SuppressWarnings({"unchecked","rawtypes"})
        ThreadLocal<ColumnReader>[] colReaders =
            (ThreadLocal<ColumnReader>[]) new ThreadLocal[ ncol_ ];
        colReaders_ = colReaders;
        for ( int ic = 0; ic < ncol_; ic++ ) {
            final ColumnStore colStore = colStores[ ic ];
            colReaders_[ ic ] = new ThreadLocal<ColumnReader>() {
                @Override
                protected ColumnReader initialValue() {
                    return colStore.createReader();
                }
            };
        }
    }

    public boolean isRandom() {
        return true;
    }

    public long getRowCount() {
        return nrow_;
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        return colReaders_[ icol ].get().getObjectValue( lrow );
    }

    public Object[] getRow( long lrow ) throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            row[ icol ] = getCell( lrow, icol );
        }
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        final ColumnReader[] readers = new ColumnReader[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            readers[ ic ] = colStores_[ ic ].createReader();
        }
        return new RowSequence() {
            long irow = -1;
            public boolean next() {
                return ++irow < nrow_;
            }
            public Object getCell( int icol ) throws IOException {
                if ( irow >= 0 ) {
                    return readers[ icol ].getObjectValue( irow );
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public Object[] getRow() throws IOException {
                if ( irow >= 0 ) {
                    Object[] row = new Object[ ncol_ ];
                    for ( int ic = 0; ic < ncol_; ic++ ) {
                        row[ ic ] = readers[ ic ].getObjectValue( irow );
                    }
                    return row;
                }
                else {
                    throw new IllegalStateException();
                }
            }
            public void close() {
            }
        };
    }

    public RowSplittable getRowSplittable() throws IOException {
        return new RandomRowSplittable( this );
    }

    public RowAccess getRowAccess() throws IOException {
        final ColumnReader[] readers = new ColumnReader[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            readers[ ic ] = colStores_[ ic ].createReader();
        }
        final Object[] row = new Object[ ncol_ ];
        return new RowAccess() {
            long irow_ = -1;
            public void setRowIndex( long irow ) {
                irow_ = irow;
            }
            public Object getCell( int icol ) throws IOException {
                return readers[ icol ].getObjectValue( irow_ );
            }
            public Object[] getRow() throws IOException {
                for ( int ic = 0; ic < ncol_; ic++ ) {
                    row[ ic ] = readers[ ic ].getObjectValue( irow_ );
                }
                return row;
            }
            public void close() {
            }
        };
    }
}
