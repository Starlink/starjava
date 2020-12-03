package uk.ac.starlink.topcat;

import java.io.IOException;
import java.util.function.LongSupplier;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * StarTable that applies a row subset selection to a given input table.
 * This implementation is not random access, but does supply a
 * non-trivial RowSplittable.
 *
 * @author   Mark Taylor
 * @since    14 Dec 2020
 */
public class SubsetStarTable extends WrapperStarTable {

    private final RowSubset rset_;

    /**
     * Constructor.
     *
     * @param   base  table supplying row data
     * @param   rset  subset defining row inclusion
     */
    public SubsetStarTable( StarTable base, RowSubset rset ) {
        super( base );
        rset_ = rset;
    }

    @Override
    public boolean isRandom() {
        return false;
    }

    @Override
    public long getRowCount() {
        return -1L;
    }

    @Override
    public Object getCell( long irow, int icol ) {
        throw new UnsupportedOperationException( "Not random" );
    }

    @Override
    public Object[] getRow( long irow ) {
        throw new UnsupportedOperationException( "Not random" );
    }
       
    @Override
    public RowAccess getRowAccess() {
        throw new UnsupportedOperationException( "Not random" );
    }

    @Override
    public RowSequence getRowSequence() throws IOException {
        final RowSequence baseSeq = super.getRowSequence();
        return new WrapperRowSequence( baseSeq ) {
            long lrow = -1;
            @Override
            public boolean next() throws IOException {
                while ( baseSeq.next() ) {
                    ++lrow;
                    if ( rset_.isIncluded( lrow ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Note the row index reflects the index in the underlying table,
     * not that of the subset table.
     */
    @Override
    public RowSplittable getRowSplittable() throws IOException {
        return new SubsetSplittable( super.getRowSplittable() );
    }

    /**
     * RowSplittable implementation for a SubsetStarTable.
     */
    private class SubsetSplittable extends WrapperRowSequence
                                   implements RowSplittable {
        final RowSplittable baseSplit_;
        final LongSupplier rowIndex_;

        /**
         * Constructor.
         *
         * @param   baseSplit  base splittable
         */
        SubsetSplittable( RowSplittable baseSplit ) {
            super( baseSplit );
            baseSplit_ = baseSplit;
            rowIndex_ = baseSplit.rowIndex();
            if ( rowIndex_ == null ) {
                throw new IllegalArgumentException( "Unknown row index"
                                                  + " for splittable" );
            }
        }

        public RowSplittable split() {
            RowSplittable spl = baseSplit_.split();
            return spl == null ? null : new SubsetSplittable( spl );
        }

        public LongSupplier rowIndex() {
            return rowIndex_;
        }

        public long splittableSize() {
            return baseSplit_.splittableSize();
        }

        @Override
        public boolean next() throws IOException {
            while ( baseSplit_.next() ) {
                if ( rset_.isIncluded( rowIndex_.getAsLong() ) ) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns a table with row selection characteristics matching
     * those of the given subset.
     */
    public static StarTable createTable( StarTable table, RowSubset rset ) {

        // It would be possible to check for a rowMap corresponding to the
        // given subset and return a random-access implementation based on that
        // if available (it probably exists if rset is the Current Subset).
        // But that would not allow progress logging, so for now don't do it.
        return rset == RowSubset.ALL
             ? table
             : new SubsetStarTable( table, rset );
    }
}
