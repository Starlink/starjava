package uk.ac.starlink.topcat;

import java.util.BitSet;

/**
 * Does something useful with a table row subset definition.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2008
 */
public interface SubsetConsumer {

    /**
     * Consumes a subset.
     *
     * @param   tcModel   model to which the row mask applies
     * @param   rowMask   bit mask in which set bits represent rows in 
     *                    the subset
     */
    void consumeSubset( TopcatModel tcModel, BitSet rowMask );
}
