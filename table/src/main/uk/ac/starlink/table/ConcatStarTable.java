package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Table representing the concatenation of rows from a list of tables.
 * The first (base) table provides all the metadata plus the first
 * lot of rows, and subsequent ones provide only additional rows.
 * All the constituent tables have to have similar columns in the same
 * order (compatible column class types) or there will be trouble when
 * they come to be read.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    26 Mar 2004
 */
public class ConcatStarTable extends WrapperStarTable {

    private final StarTable[] tables_;

    private static final RowSequence EMPTY_SEQUENCE = new EmptyRowSequence();
 
    /**
     * Constructs a new concatenated table.
     *
     * @param   tables   array of constituent tables
     */
    public ConcatStarTable( StarTable[] tables ) {
        super( tables[ 0 ] );
        tables_ = tables;
    }

    public long getRowCount() {
        long nrow = 0;
        for ( int i = 0; i < tables_.length; i++ ) {
            long nr = tables_[ i ].getRowCount();
            if ( nr < 0 ) {
                return -1L;
            }
            else {
                nrow += nr;
            }
        }
        return nrow;
    }

    public boolean isRandom() {
        boolean isRandom = true;
        for ( int i = 0; isRandom && i < tables_.length; i++ ) {
            isRandom = isRandom && tables_[ i ].isRandom();
        }
        return isRandom;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        for ( int itab = 0; itab < tables_.length; itab++ ) {
            StarTable t = tables_[ itab ];
            long nr = t.getRowCount();
            if ( irow < nr ) {
                return t.getCell( irow, icol );
            }
            irow -= nr;
        }
        throw new ArrayIndexOutOfBoundsException( 
            "Attempt to access row beyond end of table" );
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        for ( int itab = 0; itab < tables_.length; itab++ ) {
            StarTable t = tables_[ itab ];
            long nr = t.getRowCount();
            if ( irow < nr ) {
                return t.getRow( irow );
            }
            irow -= nr;
        }
        throw new ArrayIndexOutOfBoundsException( 
            "Attempt to access row beyond end of table" );
    }

    public RowSequence getRowSequence() throws IOException {
        return new ConcatRowSequence();
    }
    
    /**
     * Implements RowSequence for a concatenated table.
     */
    private class ConcatRowSequence implements RowSequence {
        final Iterator tabIt_ = Arrays.asList( tables_ ).iterator();
        RowSequence rseq_ = EMPTY_SEQUENCE;

        public boolean next() throws IOException {
            while ( ! rseq_.next() ) {
                if ( rseq_ != EMPTY_SEQUENCE ) {
                    rseq_.close();
                }
                if ( tabIt_.hasNext() ) {
                    rseq_ = ((StarTable) tabIt_).getRowSequence();
                }
                else {
                    return false;
                }
            }
            return true;
        }

        public void close() throws IOException {
            rseq_.close();
        }

        public Object getCell( int icol ) throws IOException {
            return rseq_.getCell( icol );
        }

        public Object[] getRow() throws IOException {
            return rseq_.getRow();
        }
    }
}
