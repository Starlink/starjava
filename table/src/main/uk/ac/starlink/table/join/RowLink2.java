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

    private double score_ = Double.NaN;

    /**
     * Constructor.
     *
     * @param  rowA  one row
     * @param  rowB  the other row
     */
    public RowLink2( RowRef rowA, RowRef rowB ) {
        super( new RowRef[] { rowA, rowB } );
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
