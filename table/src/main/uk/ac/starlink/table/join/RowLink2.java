package uk.ac.starlink.table.join;

/**
 * Represents a pair of table rows which are linked (usually this means
 * that they are considered to reference the same object).
 * This {@link RowLink} subclass is provided for convenience (it may
 * just be a bit more efficient than <tt>RowLink</tt> too).
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowLink2 extends RowLink {

    private final RowRef rowRef1;
    private final RowRef rowRef2;

    /**
     * Constructs a new RowLink2 from a pair of (unordered) row references.
     *
     * @param  refA  one of the row references
     * @param  refB  the other row reference
     */
    public RowLink2( RowRef refA, RowRef refB ) {
        super( new RowRef[] { refA, refB } );
        rowRef1 = (RowRef) getRowRefs().first();
        rowRef2 = (RowRef) getRowRefs().last();
    }

    /**
     * Returns the first row reference (the 'lower').
     *
     * @return  row 1
     */
    public RowRef getRef1() {
        return rowRef1;
    }
 
    /**
     * Returns the second row referece (the 'higher').
     *
     * @return  row 2
     */
    public RowRef getRef2() {
        return rowRef2;
    }
}
