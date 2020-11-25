package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.function.LongSupplier;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.SequentialRowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowAccess;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table which looks at only every n'th row.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class EveryTable extends WrapperStarTable {

    private final long step_;
    private final boolean isExact_;

    /**
     * Constructor.
     *
     * @param   base  base table
     * @param   step  number of rows of base table per single row of this one
     * @param   isExact   if true, the stepping must be exact;
     *                    if false, approximate stepping is OK
     */
    public EveryTable( StarTable base, long step, boolean isExact ) {
        super( base );
        step_ = step;
        isExact_ = isExact;
    }

    public long getRowCount() {
        long baseCount = super.getRowCount();
        if ( baseCount >= 0 ) {
            return ( ( baseCount - 1 ) / step_ ) + 1;
        }
        else {
            return baseCount;
        }
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return super.getCell( irow * step_, icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return super.getRow( irow * step_ );
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( super.getRowSequence() ) {
            boolean started;
            public boolean next() throws IOException {
                if ( started ) {
                    for ( int i = 0; i < step_; i++ ) {
                        if ( ! super.next() ) {
                            return false;
                        }
                    }
                }
                else {
                    started = true;
                    return super.next();
                }
                return true;
            }
        };
    }

    public RowAccess getRowAccess() throws IOException {
        return new WrapperRowAccess( super.getRowAccess() ) {
            @Override
            public void setRowIndex( long irow ) throws IOException {
                super.setRowIndex( irow * step_ );
            }
        };
    }

    public RowSplittable getRowSplittable() throws IOException {
        RowSplittable baseSplittable = super.getRowSplittable();
        LongSupplier rowIndex = baseSplittable.rowIndex();
        if ( rowIndex == null ) {
            return isExact_
                 ? new SequentialRowSplittable( this )
                 : new ApproxEverySplittable( baseSplittable );
        }
        else {
            return new ExactEverySplittable( baseSplittable );
        }
    }

    /**
     * Stepping RowSplittable implementation that relies on a working
     * row index accessor.
     */
    private class ExactEverySplittable
            extends WrapperRowSequence implements RowSplittable {
        final RowSplittable baseSplit_;
        final LongSupplier baseIndex_;
        final LongSupplier rowIndex_;

        /**
         * Constructor.
         *
         * @param  baseSplit  base table splittable
         */
        ExactEverySplittable( RowSplittable baseSplit ) {
            super( baseSplit );
            baseSplit_ = baseSplit;
            baseIndex_ = baseSplit_.rowIndex();
            rowIndex_ = () -> baseIndex_.getAsLong() / step_;
            assert baseIndex_ != null;
        }
        public long splittableSize() {
            long baseSize = baseSplit_.splittableSize();
            return baseSize >= 0 ? baseSize / step_ : -1;
        }
        public LongSupplier rowIndex() {
            return rowIndex_;
        }
        public boolean next() throws IOException {
            while ( baseSplit_.next() ) {
                if ( baseIndex_.getAsLong() % step_ == 0 ) {
                    return true;
                }
            }
            return false;    
        }
        public RowSplittable split() {
            RowSplittable spl = baseSplit_.split();
            return spl == null
                 ? null
                 : new ExactEverySplittable( spl );
        }
    }

    /**
     * RowSplittable implementation that doesn't need a row index accessor,
     * but may not get stepping exactly right.
     */
    private class ApproxEverySplittable
            extends WrapperRowSequence implements RowSplittable {
        final RowSplittable baseSplit_;
        boolean started_;

        /**
         * Constructor.
         *
         * @param  baseSplit  base table splittable
         */
        ApproxEverySplittable( RowSplittable baseSplit ) {
            super( baseSplit );
            baseSplit_ = baseSplit;
        }
        public long splittableSize() {
            long baseSize = baseSplit_.splittableSize();
            return baseSize >= 0 ? baseSize / step_ : -1;
        }
        public LongSupplier rowIndex() {
            return null;
        }
        public boolean next() throws IOException {
            if ( started_ ) {
                for ( int i = 0; i < step_; i++ ) {
                    if ( ! baseSplit_.next() ) {
                        return false;
                    }
                }
            }
            else {
                started_ = true;
                return super.next();
            }
            return true;
        }
        public RowSplittable split() {
            RowSplittable spl = baseSplit_.split();
            return spl == null
                 ? null
                 : new ApproxEverySplittable( spl );
        }
    }
}
