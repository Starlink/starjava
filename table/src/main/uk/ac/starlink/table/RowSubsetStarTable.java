package uk.ac.starlink.table;

import java.io.IOException;
import java.util.BitSet;

/**
 * Wraps a StarTable to present only a subset of its rows.
 * A {@link java.util.BitSet} is used to keep track of which rows in the
 * base table should be visible from this one; a set (true) bit in the
 * mask indicates a row in the base table which will be visible in this one.
 * It is the responsibility of the user to ensure that no bits in the
 * mask are set beyond the end of the underlying table - behaviour is
 * undefined in the case that this condition is violated (but probably
 * procured RowSequence objects will misbehave).
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowSubsetStarTable extends WrapperStarTable {

    private BitSet mask;

    /**
     * Constructs a new RowSubsetStarTable with no rows showing, 
     * in which a set bit in the mask indicates a visible row.
     *
     * @param  baseTable  base table which provides the underlying data
     */
    public RowSubsetStarTable( StarTable baseTable ) {
        this( baseTable, new BitSet() );
    }

    /**
     * Constructs a new RowSubsetStarTable with a given mask.
     *
     * @param  baseTable  base table which provides the underlying data
     * @param  mask    bitmask determining which rows in <tt>baseTable</tt>
     *                 are seen
     */
    public RowSubsetStarTable( StarTable baseTable, BitSet mask ) {
        super( baseTable );
        this.mask = mask;
    }

    /**
     * Returns the bit mask which defines which rows are seen.
     *
     * @return   row mask
     */
    public BitSet getMask() {
        return mask;
    }

    /**
     * Sets the mask which determines which rows are seen.
     * 
     * @param   row mask
     */
    public void setMask( BitSet mask ) {
        this.mask = mask;
    }

    /**
     * Returns false.
     */
    public boolean isRandom() {
        return false;
    }

    public long getRowCount() {
        return (long) mask.cardinality();
    }

    public RowSequence getRowSequence() throws IOException {
        return new RowSequence() {
            RowSequence baseSeq = baseTable.getRowSequence();
            int iBase = -1;

            public boolean hasNext() {
                return mask.nextSetBit( iBase + 1 ) >= 0;
            }

            public void next() throws IOException {
                int nextBase = mask.nextSetBit( iBase + 1 );
                if ( nextBase == -1 ) {
                    assert ! hasNext();
                    throw new IllegalStateException( "No more rows" );
                }
                int nskip = nextBase - iBase;
                assert nskip > 0;
                while ( nskip-- > 0 ) {
                    baseSeq.next();
                    iBase++;
                }
            }

            public Object getCell( int icol ) throws IOException {
                return baseSeq.getCell( icol );
            }

            public Object[] getRow() throws IOException {
                return baseSeq.getRow();
            }

            public void close() throws IOException {
                baseSeq.close();
            }
        };
    }
}
