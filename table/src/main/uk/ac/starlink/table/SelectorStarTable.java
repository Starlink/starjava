package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Sequential wrapper table which selects only certain rows of its base table.
 * No random access is provided, and the row sequence evaluates
 * the abstract {@link #isIncluded} method for each row of the base
 * table as it is iterated over.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public abstract class SelectorStarTable extends WrapperStarTable {

    /**
     * Constructor.
     *
     * @param   baseTable  table on which this is based
     */
    public SelectorStarTable( StarTable baseTable ) {
        super( baseTable );
    }

    /**
     * Evaluated to determine whether rows of the base table are included
     * in this one.
     *
     * @param  baseSeq  row sequence of the base table
     * @return  true iff the current row of <code>baseSeq</code>
     *          is to be included
     */
    public abstract boolean isIncluded( RowSequence baseSeq )
            throws IOException;

    /**
     * Returns false.
     */
    public boolean isRandom() {
        return false;
    }

    /**
     * Returns -1 (length unknown).
     */
    public long getRowCount() {
        return -1L;
    }

    public Object[] getRow() {
        throw new UnsupportedOperationException( "Not random" );
    }

    public Object getCell( int icol ) {
        return getRow()[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {
        final RowSequence baseSeq_ = super.getRowSequence();

        return new WrapperRowSequence( baseSeq_ ) {
            public boolean next() throws IOException {
                while ( baseSeq_.next() ) {
                    if ( isIncluded( baseSeq_ ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public RowSplittable getRowSplittable() throws IOException {
        return Tables.getDefaultRowSplittable( this );
    }

    public RowAccess getRowAccess() {
        throw new UnsupportedOperationException();
    }
}
