package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Sequential WrapperStarTable implementation which gets its row ordering
 * from a supplied sequence of RowLinks.
 * The base table may be either a random access table,
 * or a non-random table whose row order is consistent with 
 * (monotonically increasing with) the ordering supplied by the RowLinks.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2010
 */
abstract class RowLinkTable extends WrapperStarTable {

    private final StarTable base_;
    private final int iTable_;
    private final ColumnInfo[] colInfos_;
    private Boolean hasBlanks_;

    /**
     * Constructor.
     *
     * @param   base  base table
     * @param   iTable  table index with reference to indices in the supplied
     *          RowLinks
     */
    RowLinkTable( StarTable base, int iTable ) {
        super( base );
        base_ = base;
        iTable_ = iTable;
        colInfos_ = new ColumnInfo[ getColumnCount() ];
    }

    public boolean isRandom() {
        return false;
    }

    public Object getCell( long irow, int icol ) {
        throw new UnsupportedOperationException();
    }

    public Object[] getRow( long irow ) {
        throw new UnsupportedOperationException();
    }

    public long getRowCount() {
        return -1L;
    }

    /**
     * Returns an iterator over {@link RowLink} objects which determines the
     * sequence of rows in this table.
     *
     * @param   iterator over RowLinks
     */
    public abstract Iterator getLinkIterator() throws IOException;

    public ColumnInfo getColumnInfo( int icol ) {
        if ( hasBlanks_ == null ) {
            boolean hasBlanks = false;
            try {
                for ( Iterator it = getLinkIterator();
                      it.hasNext() && ! hasBlanks; ) {
                    RowLink link = (RowLink) it.next();
                    if ( getBaseRowIndex( link ) < 0 ) {
                        hasBlanks = true;
                    }
                }
                hasBlanks_ = Boolean.valueOf( hasBlanks );
            }
            catch ( IOException e ) {
                hasBlanks_ = Boolean.TRUE;
            }
        }
        if ( colInfos_[ icol ] == null ) {
            colInfos_[ icol ] = new ColumnInfo( super.getColumnInfo( icol ) );
            if ( hasBlanks_.booleanValue() ) {
                colInfos_[ icol ].setNullable( true );
            }
        }
        return colInfos_[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {
        return base_.isRandom() ? (RowSequence) new RandomLinkRowSequence()
                                : (RowSequence) new SequentialLinkRowSequence();
    }

    /**
     * Returns the row index in the base table corresponding to a given
     * RowLink.
     *
     * @param  link  row link
     * @return   base table row index, or -1 if none
     */
    private long getBaseRowIndex( RowLink link ) {
        int nr = link.size();
        for ( int ir = 0; ir < nr; ir++ ) {
            RowRef ref = link.getRef( ir );
            if ( ref.getTableIndex() == iTable_ ) {
                return ref.getRowIndex();
            }
        }
        return -1L;
    }

    /**
     * Returns a cell of this table given a RowLink and a column index.
     *
     * @param  link  row link
     * @param  icol  column index
     * @param  cell contents, or null if no such row
     */
    public Object getCell( RowLink link, int icol ) throws IOException {
        long jrow = getBaseRowIndex( link );
        return jrow >= 0 ? base_.getCell( jrow, icol )
                         : null;
    }

    /**
     * Returns a row of this table given a RowLink.
     *
     * @param   link  row link
     * @param   row contents, may be array of nulls
     */
    public Object[] getRow( RowLink link ) throws IOException {
        long jrow = getBaseRowIndex( link );
        return jrow >= 0 ? base_.getRow( jrow )
                         : new Object[ getColumnCount() ];
    }

    /**
     * RowSequence implementation based on a LinkIterator based on 
     * random access to the base table.
     */
    private class RandomLinkRowSequence implements RowSequence {
        final Iterator linkIt_;
        RowLink link_;

        RandomLinkRowSequence() throws IOException {
            linkIt_ = getLinkIterator();
        }

        public boolean next() {
            if ( linkIt_.hasNext() ) {
                link_ = (RowLink) linkIt_.next();
                return true;
            }
            else {
                link_ = null;
                return false;
            }
        }

        public Object getCell( int icol ) throws IOException {
            return RowLinkTable.this.getCell( link_, icol );
        }

        public Object[] getRow() throws IOException {
            return RowLinkTable.this.getRow( link_ );
        }

        public void close() {
        }
    }

    /**
     * RowSequence implementation based on a LinkIterator based on
     * sequential access to the base table.
     * Will only work if the links in the link iterator represent
     * monotonically increasing row indices for the base table.
     */
    private class SequentialLinkRowSequence extends WrapperRowSequence {
        final Iterator linkIt_;
        long irow_ = -1L;

        SequentialLinkRowSequence() throws IOException {
            super( base_.getRowSequence() );
            linkIt_ = getLinkIterator();
        }

        public boolean next() throws IOException {
            if ( linkIt_.hasNext() ) {
                RowLink link = (RowLink) linkIt_.next();
                return advanceTo( getBaseRowIndex( link ) );
            }
            else {
                return false;
            }
        }

        private boolean advanceTo( long target ) throws IOException {
            if ( irow_ > target ) {
                throw new IOException( "Badly ordered RowLinks" );
            }
            boolean hasNext = true;
            while ( irow_ < target && hasNext ) {
                irow_++;
                hasNext = baseSeq.next();
            }
            return hasNext;
        }
    }
}
