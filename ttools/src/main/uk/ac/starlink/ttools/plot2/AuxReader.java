package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Extracts range information from a plot data.
 *
 * @since   4 Feb 2013
 * @author  Mark Taylor
 */
public interface AuxReader {

    /**
     * Called once for each applicable row of a tuple sequence.
     * Each call may extend the given range.
     *
     * @param  surface  plot data destination surface
     * @param  tseq    plot data supplier
     * @param  range   range object to be updated with range information
     */
    void updateAuxRange( Surface surface, TupleSequence tseq, Range range );
}
