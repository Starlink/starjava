package uk.ac.starlink.ttools.plot2;

/**
 * Accumulates a set of data values to provide range information.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2019
 */
public interface Ranger {

    /**
     * Accepts a data value.
     *
     * @param  d  datum
     */
    void submitDatum( double d );

    /**
     * Returns an object characterising the range of data submitted so far.
     * This should not be called while another thread might be calling
     * {@link #submitDatum}.
     *
     * @return  span of accumulated data
     */
    Span createSpan();

    /**
     * Merges the contents of the supplied ranger into this one.
     * The effect is as if all the results accumulated into other
     * had been accumulated into this one.
     * The effect on the supplied other is undefined.
     *
     * <p>The supplied ranger is assumed to be <em>compatible</em>
     * with this one, which probably means created in the same way.
     * If not, some RuntimeException such as a ClassCastException
     * may result.
     *
     * @param   other  compatible ranger instance
     */
    void add( Ranger other );

    /**
     * Returns a Ranger instance that is compatible with this one.
     * It has no content (does not copy any data from this one),
     * but the two may be merged using the {@link #add} method.
     *
     * @return   new compatible ranger instance
     */
    Ranger createCompatibleRanger();
}
