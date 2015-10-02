package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Extracts range information from plot data.
 *
 * @since   4 Feb 2013
 * @author  Mark Taylor
 */
public interface AuxReader {

    /**
     * Called for a tuple sequence, may update (usually extend) the given range.
     *
     * @param  surface  plot data destination surface
     * @param  tseq    plot data supplier
     * @param  range   range object to be updated with range information
     */
    void adjustAuxRange( Surface surface, TupleSequence tseq, Range range );
}
