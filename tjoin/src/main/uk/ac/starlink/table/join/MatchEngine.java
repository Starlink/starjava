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
     *
     * <p>The returned value is immutable, and is not affected by subsequent
     * changes of the settings of this object.
     *
     * @return  match kit supplier
     */
    Supplier<MatchKit> createMatchKitFactory();

    /**
     * Returns a supplier for coverage objects.
     * Each such coverage can be used to characterise a region of tuple space.
     * When populated with a set of tuples A,
     * any tuple for which the inclusion function defined by its
     * {@link Coverage#createTestFactory} method returns false
     * is guaranteed not to match any tuple in A according to this object's
     * match criteria.
     *
     * <p>The returned value is immutable, and is not affected by subsequent
     * changes of the settings of this object.
     *
     * <p>If no suitable implementation is available, null may be returned.
     *
     * @return  supplier of coverage objects, or null
     */
    Supplier<Coverage> createCoverageFactory();

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
}
