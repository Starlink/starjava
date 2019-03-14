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
}
