package uk.ac.starlink.table.join;

/**
 * Defines the details of object matching criteria.
 * This interface provides methods for ascertaining whether two table
 * rows are to be linked - this usually means that they are to be
 * assumed to refer to the same object.
 * The methods act on 'tuples' - an array of objects defining the relevant
 * characteristics of a row.  Of course these tuples have to be prepared
 * with understanding of what a particular implementation of this interface
 * knows how to deal with.  Typically a tuple will be a list of coordinates,
 * such as RA and Dec.
 * <p>
 * The interface consists of two methods.  One simply tests whether two
 * tuples count as the same or not (in practice, this is likely to 
 * compare corresponding elements of the two submitted tuples allowing
 * for some error in each one).  The second is a bit more subtle:
 * it must identify a set of bins into which possible matches for the tuple
 * might fall.  For the case of coordinate matching with errors, you 
 * would need to chop the whole possible space into a discrete set of 
 * zones, each with a given key, and return the key for each zone 
 * near enough to the submitted tuple (point) that it might contain a
 * match for it.
 * <p>
 * Formally, the requirements for correct implementations of this 
 * interface are as follows:
 * <ol>
 * <li><tt>matches(t1,t2) == matches(t2,t1)</tt>
 * <li><tt>matches(t1,t2) implies a non-zero intersection of 
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
 * <p>
 * It may help to think of all this as a sort of fuzzy hash.
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface MatchEngine {

    /**
     * Returns a set of keys for bins into which possible matches for 
     * a given tuple might fall.
     * The returned objects can be anything, but should have their
     * <tt>equals</tt> and <tt>hashCode</tt> methods implemented 
     * properly for comparison.
     *
     * @param  tuple  
     * @return   set of bin keys which might be returned by invoking this
     *           method on other tuples which count as matches for the
     *           submitted <tt>tuple</tt>
     */
    Object[] getBins( Object[] tuple );

    /**
     * Indicates whether two tuples are to be linked.
     *
     * @param  tuple1  one tuple
     * @param  tuple2  the other tuple
     * @return  <tt>true</tt> iff <tt>tuple1</tt> should be considered a
     *          match for <tt>tuple2</tt>
     */
    boolean matches( Object[] tuple1, Object[] tuple2 );
}
