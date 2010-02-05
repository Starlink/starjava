package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Defines the details of object matching criteria.
 * This interface provides methods for ascertaining whether two table
 * rows are to be linked - this usually means that they are to be
 * assumed to refer to the same object.
 * The methods act on 'tuples' - an array of objects defining the relevant
 * characteristics of a row.  Of course these tuples have to be prepared
 * with understanding of what a particular implementation of this interface
 * knows how to deal with, which can be obtained from the {@link #getTupleInfos}
 * method.  Typically a tuple will be a list of coordinates,
 * such as RA and Dec.
 * <p>
 * The business end of the interface consists of two methods.  
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
 * <p>
 * Formally, the requirements for correct implementations of this 
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
 * <p>
 * It may help to think of all this as a sort of fuzzy hash.
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface MatchEngine {

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

    /**
     * Returns a description of the value returned by the 
     * {@link #matchScore} method.  The content class should be numeric
     * (though need not be <code>Double</code>), and the name,
     * description and units should be descriptive of whatever the
     * physical significance of the value is.
     * If the result of <code>matchScore</code> is not interesting
     * (for instance, if it's always either 0 or -1),
     * <code>null</code> may be returned.
     *
     * @return   metadata for the match score results
     */
    ValueInfo getMatchScoreInfo();

    /**
     * Returns a set of ValueInfo objects indicating what is required for
     * the elements of each tuple.  The length of this array is the 
     * number of elements in the tuple.  Each element should at least
     * have a defined name and content class.  The info's <tt>nullable</tt> 
     * attribute has a special meaning: if true it means that it makes
     * sense for this element of the tuple to be always blank (for instance
     * assigned to no column).
     *
     * @return  array of objects describing the requirements on each 
     *          element of the tuples used for matching
     */
    ValueInfo[] getTupleInfos();

    /**
     * Returns a set of DescribedValue objects whose values can be modified
     * to modify the matching criteria.  Typically at least one of these
     * will be some sort of tolerance separation which determines how
     * close tuples must be to count as a match.
     * This match engine's behaviour can be modified by calling 
     * {@link uk.ac.starlink.table.DescribedValue#setValue} on the 
     * returned objects.
     *
     * @return  array of described values which influence the match
     */
    DescribedValue[] getMatchParameters();

    /**
     * Returns a set of DescribedValue objects whose values can be modified
     * to tune the performance of the match.
     * This match engine's performance can be influenced by calling 
     * {@link uk.ac.starlink.table.DescribedValue#setValue} on the 
     * returned objects.
     *
     * <p>Changing these values will make no difference to the output of
     * {@link #matchScore}, but may change the output of {@link #getBins}.
     * This may change the CPU and memory requirements of the match,
     * but will not change the result.  The default value should be
     * something sensible, so that setting the value of these parameters
     * is not in general required.
     *
     * @return  array of described values which may influence match performance
     */
    DescribedValue[] getTuningParameters();

    /**
     * Given a range of tuple values, returns a range outside which 
     * no match to anything within that range can result.
     * If the tuples on which this engine works represent some kind of
     * space, the input values and output values specify a 
     * hyper-rectangular region of this space.
     * In the common case in which the match criteria are based on 
     * proximity in this space up to a certain error, this method should
     * return a rectangle which is like the input one but broadened in
     * each direction by an amount corresponding to the error.
     *
     * <p>Both the input and output rectangles are specified by tuples
     * representing its opposite corners; equivalently, they are the
     * minimum and maximum values of each tuple element.
     * In either the input or output min/max tuples, any element may be
     * <tt>null</tt> to indicate that no information is available on
     * the bounds of that tuple element (coordinate).
     *
     * <p>This method can be used by match algorithms which know in advance
     * the range of coordinates they will match against and wish 
     * to reduce workload by not attempting matches which are bound to fail.
     *
     * <p>For example, a 1-d Cartesian match engine with an 
     * isotropic match error 0.5
     * would turn input values of ((0,200),(10,210)) into output values 
     * ((-0.5,199.5),(10.5,210.5)).
     *
     * <p>This method will only be called if {@link #canBoundMatch}
     * returns true.  Thus engines that cannot provide any useful 
     * information along these lines (for instance because none of its
     * tuple elements is {@link java.lang.Comparable} do not need to
     * implement it in a meaningful way.
     *
     * @param  minTuple  tuple consisting of the minimum values of each
     *         tuple element in a possible match
     *         (to put it another way - coordinates of one corner of a
     *         tuple-space rectangle containing such a match)
     * @param  maxTuple  tuple consisting of the maximum values of each
     *         tuple element in a possible match
     *         (to put it another way - coordinates of the other corner of a
     *         tuple-space rectangle containing such a match)
     * @return 2-element array of tuples -
     *         effectively <tt>(minTuple,maxTuple)</tt> broadened by errors
     * @see   #canBoundMatch
     */
    Comparable[][] getMatchBounds( Comparable[] minTuple,
                                   Comparable[] maxTuple );

    /**
     * Indicates that the {@link #getMatchBounds} method can be invoked
     * to provide some sort of useful result.
     *
     * @return  true  iff  <tt>getMatchBounds</tt> may provide useful 
     *          information
     */
    boolean canBoundMatch();
}
