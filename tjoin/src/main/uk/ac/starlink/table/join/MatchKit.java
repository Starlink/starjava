package uk.ac.starlink.table.join;

/**
 * Performs the operations required for object matching.
 *
 * <p>This interface consists of two methods.
 * One tests whether two tuples count as matching or not,
 * and assigns a closeness score if they are (in practice, this is likely to
 * compare corresponding elements of the two submitted tuples allowing
 * for some error in each one).  The second is a bit more subtle:
 * it must identify a set of bins into which possible matches for the tuple
 * might fall.  For the case of coordinate matching with errors, you
 * would need to chop the whole possible space into a discrete set of
 * zones, each with a given key, and return the key for each zone
 * near enough to the submitted tuple (point) that it might contain a
 * match for it.
 *
 * <p>Formally, the requirements for correct implementations of this
 * interface are as follows:
 * <ol>
 * <li><tt>matchScore(t1,t2)</tt> == <tt>matchScore(t2,t1)</tt>
 * <li><tt>matchScore(t1,t2)&gt;=0</tt> implies a non-zero intersection of
 *     <tt>getBins(t1)</tt> and <tt>getBins(t2)</tt>
 * </ol>
 * The best efficiency will be achieved when:
 * <ol>
 * <li>the intersection of <tt>getBins(t1)</tt> and <tt>getBins(t2)</tt>
 *     is as small as possible for non-matching <tt>t1</tt> and <tt>t2</tt>
 *     (preferably 0)
 * <li>the number of bins returned by <tt>getBins</tt> is as small as
 *     possible (preferably 1)
 * </ol>
 * These two efficiency requirements are usually conflicting to some extent.
 *
 * <p>It may help to think of all this as a sort of fuzzy hash.
 *
 * <p>Instances of this class are not thread-safe, and should not be used
 * from multiple threads concurrently.
 *
 * @author   Mark Taylor
 * @since    9 May 2022
 */
public interface MatchKit {

    /**
     * Convenience constant - it's a zero-length array of objects, suitable
     * for returning from {@link #getBins} if no match can result.
     */
    static final Object[] NO_BINS = new Object[ 0 ];

    /**
     * Returns a set of keys for bins into which possible matches for
     * a given tuple might fall.
     * The returned objects can be anything, but should have their
     * <tt>equals</tt> and <tt>hashCode</tt> methods implemented
     * properly for comparison.
     *
     * @param  tuple   tuple
     * @return   set of bin keys which might be returned by invoking this
     *           method on other tuples which count as matches for the
     *           submitted <tt>tuple</tt>
     */
    Object[] getBins( Object[] tuple );

    /**
     * Indicates whether two tuples count as matching each other, and if
     * so how closely.  If <tt>tuple1</tt> and <tt>tuple2</tt> are
     * considered as a matching pair, then a non-negative value should
     * be returned indicating how close the match is - the higher the
     * number the worse the match, and a return value of zero indicates
     * a 'perfect' match.
     * If the two tuples do not consitute a matching pair, then
     * a negative number (conventionally -1.0) should be returned.
     * This return value can be thought of as (and will often
     * correspond physically with) the distance in some real or notional
     * space between the points represented by the two submitted tuples.
     *
     * <p>If there's no reason to do otherwise, the range 0..1 is
     * recommended for successul matches.  However, if the result has
     * some sort of physical meaning (such as a distance in real space)
     * that may be used instead.
     *
     * @param  tuple1  one tuple
     * @param  tuple2  the other tuple
     * @return  'distance' between <tt>tuple1</tt> and <tt>tuple2</tt>;
     *          0 is a perfect match, larger values indicate worse matches,
     *          negative values indicate no match
     */
    double matchScore( Object[] tuple1, Object[] tuple2 );
}
