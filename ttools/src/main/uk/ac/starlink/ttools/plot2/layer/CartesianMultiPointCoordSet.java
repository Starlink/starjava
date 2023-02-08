package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * MultiPointCoordSet sub-interface for Cartesian geometry.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2023
 */
public interface CartesianMultiPointCoordSet extends MultiPointCoordSet {

    /**
     * Reads the non-central points from a appropriate span of columns
     * in a supplied tuple.
     * The central data position must be supplied as input.
     * The <code>icExtra</code> value gives the column corresponding to
     * the first coord of this coordinate set in the sequence;
     * the following few columns are assumed to correspond 1:1 to
     * the coords in this coord set.
     *
     * @param  tuple  tuple
     * @param  icExtra  index of tuple field corresponding to
     *                  the first of the coordinates in this set
     * @param  dpos0  nDataDim-element array giving central data position
     * @param  dposExtras   [nPointCount][nDataDim]-shaped array into which
     *                      the non-central data positions will be written
     * @return  true iff the conversion was successful
     */
    boolean readPoints( Tuple tuple, int icExtra, double[] dpos0,
                        double[][] dposExtras );
}
