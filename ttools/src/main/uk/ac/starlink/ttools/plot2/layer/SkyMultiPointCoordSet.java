package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;

/**
 * MultiPointCoordSet sub-interface for coordinates on the sky.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2023
 */
public interface SkyMultiPointCoordSet extends MultiPointCoordSet {

    /**
     * Reads the non-central points from a appropriate span of columns
     * in a supplied tuple.
     *
     * <p>The central data position must be supplied as input.
     * The <code>icExtra</code> value gives the column corresponding to
     * the first coord of this coordinate set in the sequence;
     * the following few columns are assumed to correspond 1:1 to
     * the coords in this coord set.
     *
     * @param  tuple  tuple
     * @param  icExtra   index of tuple field corresponding to
     *                   the first of the coordinates in this set
     * @param  dpos0  nDataDim-element array giving central data position
     * @param  unitInDegrees  unit in degrees of the size-defining
     *                        tuple values
     * @param  geom   converter between tuple values and
     *                data space coordinates
     * @param  dposExtras   [nPointCount][nDataDim]-shaped array into which
     *                      the non-central data positions will be written
     * @return  true iff the conversion was successful
     */
    boolean readPoints( Tuple tuple, int icExtra, double[] dpos0,
                        double unitInDegrees, SkyDataGeom geom,
                        double[][] dposExtras );

    /**
     * Returns the characteristic size of the multi-point structure
     * represented by a supplied tuple.
     * The returned value is a measure of linear extent
     * on the tangent plane of the multi-point data contained in the tuple,
     * in the same units as the size-defining parts of the input.
     *
     * @param  tuple  tuple
     * @param  icExtra   index of tuple field corresponding to
     *                   the first of the coordinates in this set
     * @param  dpos0  nDataDim-element array giving central data position
     * @return  linear extent of multi-point shape on the tangent plane
     */
    double readSize( Tuple tuple, int icExtra, double[] dpos0 );
}
