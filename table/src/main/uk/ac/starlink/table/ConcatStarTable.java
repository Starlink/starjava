package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Table representing the concatenation of rows from two tables.
 * The first (base) table provides all the metadata plus the first
 * lot of rows, and the second (secondary) one provides only additional rows.
 * Of course it must be specified which columns in the secondary table
 * correspond to the columns in the base.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    26 Mar 2004
 */
public class ConcatStarTable extends WrapperStarTable {

    private int[] colMap;
    private final StarTable t1;
    private final StarTable t2;
 
    /**
     * Constructs a new concatenated table.
     * The supplied <tt>colMap</tt> array defines which columns in the
     * secondary correspond to which columns in the base;
     * <code>colMap[iBase]=iSecondary</code> means that 
     * the data in column <tt>iBase</tt> of this table are supplied 
     * from column <tt>iSecondary</tt> of <tt>secondary</tt>.
     * If <tt>iSecondary&lt;0</tt> or <tt>colMap.length&lt;=iBase</tt> 
     * then the cells in column <tt>iBase</tt> are all <tt>null</tt>
     * for the secondary part of the table.
     *
     * @param   base  base table
     * @param   secondary  secondary table
     * @param   colMap  mapping of columns in the base to columns in the
     *          secondary
     */
    public ConcatStarTable( StarTable base, StarTable secondary,
                            int[] colMap ) {
        super( base );
        this.colMap = colMap;
        this.t1 = base;
        this.t2 = secondary;
    }

    public long getRowCount() {
        long n1 = t1.getRowCount();
        long n2 = t2.getRowCount();
        return ( n1 >= 0L && n2 >= 0L ) ? n1 + n2 : -1L;
    }

    public boolean isRandom() {
        return t1.isRandom() && t2.isRandom();
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        long nt1 = t1.getRowCount();
        if ( irow < nt1 ) {
            return t1.getCell( irow, icol );
        }
        else {
            long irow2 = irow - nt1;
            int icol2 = mapColumnIndex( icol );
            return icol2 < 0 ? null
                             : t2.getCell( irow2, icol2 );
        }
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        long nt1 = t1.getRowCount();
        if ( irow < nt1 ) {
            return t1.getRow( irow );
        }
        else {
            long irow2 = irow - nt1;
            return mapRow( t2.getRow( irow2 ) );
        }
    }

    public RowSequence getRowSequence() throws IOException {
        return new ConcatRowSequence();
    }
    
    /**
     * Returns the column in the secondary table corresponding to a
     * given column in the base table, or -1 if there isn't a corresponding 
     * one.
     *
     * @param  icol1  base column index
     * @return   secondary column index, or -1
     */
    private int mapColumnIndex( int icol1 ) {
        return icol1 < colMap.length ? colMap[ icol1 ] : -1;
    }

    /**
     * Returns the row of this table which corresponds to a row taken
     * from the secondary.
     *
     * @param  row2  row from the secondary
     * @return   row in the format of this table
     */
    private Object[] mapRow( Object[] row2 ) {
        int ncol = getColumnCount();
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            int icol2 = mapColumnIndex( icol );
            if ( icol2 >= 0 ) {
                row[ icol ] = row2[ icol2 ];
            }
        }
        return row;
    }

    /**
     * Implements RowSequence for a concatenated table.
     */
    private class ConcatRowSequence implements RowSequence {
        RowSequence rseq;
        RowSequence t2seq;
        boolean onExtras = false;
        long extraRows = 0L;

        ConcatRowSequence() throws IOException {
            rseq = t1.getRowSequence();
            t2seq = t2.getRowSequence();
        }

        public void next() throws IOException {
            if ( rseq.hasNext() ) {
                rseq.next();
            }
            else if ( ! onExtras ) {
                rseq = getSecondaryRowSequence();
                rseq.next();
            }
            else {
                assert ! hasNext();
                throw new IllegalStateException( "No more rows" );
            }
        }

        public boolean hasNext() {
            if ( rseq.hasNext() ) {
                return true;
            }
            else if ( ! onExtras ) {
                rseq = getSecondaryRowSequence();
                return rseq.hasNext();
            }
            else {
                return false;
            }
        }

        public void advance( long nrow ) throws IOException {
            if ( onExtras ) {
                rseq.advance( nrow );
            }
            else {
                while ( nrow > 0 ) {
                    if ( rseq.hasNext() ) {
                        rseq.next();
                        nrow--;
                    }
                    else {
                        advance( nrow );
                        return;
                    }
                }
            }
        }

        public Object getCell( int icol ) throws IOException {
            if ( onExtras ) {
                int icol2 = mapColumnIndex( icol );
                return icol2 < 0 ? null
                                 : rseq.getCell( icol2 );
            }
            else {
                return rseq.getCell( icol );
            }
        }

        public Object[] getRow() throws IOException {
            if ( onExtras ) {
                return mapRow( rseq.getRow() );
            }
            else {
                return rseq.getRow();
            }
        }

        public long getRowIndex() {
            return rseq.getRowIndex() + extraRows;
        }

        private RowSequence getSecondaryRowSequence() {
            if ( rseq != t2seq ) {
                extraRows = rseq.getRowIndex() + 1L;
                rseq = t2seq;
            }
            return t2seq;
        }
    }
}
