package uk.ac.starlink.table.join;

/**
 * RowLink subclass which specifically contains two items (a pair).
 *
 * <p>As well as standard <code>RowLink</code> functionality, this object
 * can also contain a 'score', which is an uninterpreted number.
 * Typically this is used to record how good the match represented
 * by a link is.  This value is not taken account of in either
 * <code>compareTo</code>, <code>equals</code> or <code>hashCode</code>
 * methods.
 *
 * @author   Mark Taylor
 * @since    23 Nov 2007
 */
public class RowLink2 extends RowLink {

    private final RowRef ref1_;
    private final RowRef ref2_;
    private double score_ = Double.NaN;

    /**
     * Constructor.
     *
     * @param  refA  one row
     * @param  refB  the other row
     */
    public RowLink2( RowRef refA, RowRef refB ) {
        if ( refA.compareTo( refB ) <= 0 ) {
            ref1_ = refA;
            ref2_ = refB;
        }
        else {
            ref1_ = refB;
            ref2_ = refA;
        }
    }

    public int size() {
        return 2;
    }

    public RowRef getRef( int i ) {
        switch ( i ) {
            case 0:
                return ref1_;
            case 1:
                return ref2_;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the score associated with this link.
     *
     * @param  score  new score
     */
    public void setScore( double score ) {
        score_ = score;
    }

    /**
     * Returns the score associated with this link.
     * If it has not been set explicitly, it will be <code>NaN</code>.
     *
     * @return  score
     */
    public double getScore() {
        return score_;
    }
}
