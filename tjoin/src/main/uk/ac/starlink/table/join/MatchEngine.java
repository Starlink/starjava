package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Defines the details of object matching criteria.
 *
 * <p>This class manages the configuration of matching criteria.
 * Application code can manipulate the
 * {@link uk.ac.starlink.table.DescribedValue}s provided by
 * this class in accordance with user preferences, and then
 * call the {@link #createMatchKitFactory} method to supply objects
 * which implement the configured matching functionality itself.
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface MatchEngine {

    /**
     * Returns a factory for MatchKit instances corresponding
     * to the current settings of this object.
     * The returned value is immutable, and is not affected by subsequent
     * changes of the settings of this object.
     *
     * @return  match kit supplier
     */
    Supplier<MatchKit> createMatchKitFactory();

    /**
     * Returns a description of the value returned by the 
     * {@link MatchKit#matchScore} method.  The content class should be numeric
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
     * Returns a scale value for the match score.
     * The intention is that the result of
     * {@link MatchKit#matchScore matchScore}/{@link #getScoreScale}
     * is of order unity, and is thus comparable between
     * different match engines.
     *
     * <p>As a general rule, the result should be the maximum value ever
     * returned from the <code>matchScore</code> method,
     * corresponding to the least good successful match.
     * For binary MatchEngine implementations
     * (all matches are either score=0 or failures)
     * a value of 1 is recommended.
     * If nothing reliable can be said about the scale, NaN may be returned.
     *
     * @return   scale of successful match scores,
     *           a positive finite number or NaN
     */
    double getScoreScale();

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
     * {@link MatchKit#matchScore}, but may change the output of
     * {@link MatchKit#getBins}.
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
     * <p>An array of n-dimensional ranges is given, though only one of them
     * (specified by the <code>index</code> value) forms the basis for
     * the output range.  The other ranges in the input array may in some
     * cases be needed as context in order to do the calculation.
     * If the match error is fixed, only the single input n-d range is needed
     * to work out the single output range.  However, if the errors are
     * obtained by looking at the tuples themselves (match errors are per-row)
     * then in general the broadening has to be done using the maximum
     * error of <em>any</em> of the tables involved in the match,
     * not just the one to be broadened.
     * For a long time, I didn't realise this, so versions of this software
     * up to STIL v3.0-14 (Oct 2015) were not correctly broadening these
     * ranges, leading to potentially missed associations near the edge
     * of bounded regions.
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
     * tuple elements is {@link java.lang.Comparable}) do not need to
     * implement it in a meaningful way.
     *
     * @param inRanges  array of input ranges for the tables on which
     *                  the match will take place;
     *                  each element bounds the values for each tuple
     *                  element in its corresponding table
     *                  in a possible match
     *                  (to put it another way - each element gives the
     *                  coordinates of the opposite corners of a tuple-space
     *                  rectangle covered by one input table)
     * @param  index    which element of the <code>inRanges</code> array
     *                  for which the broadened output value is required
     * @return  output range, effectively <code>inRanges[index]</code>
     *          broadened by errors
     * @see   #canBoundMatch
     */
    NdRange getMatchBounds( NdRange[] inRanges, int index );

    /**
     * Indicates that the {@link #getMatchBounds} method can be invoked
     * to provide some sort of useful result.
     *
     * @return  true  iff  <tt>getMatchBounds</tt> may provide useful 
     *          information
     */
    boolean canBoundMatch();
}
