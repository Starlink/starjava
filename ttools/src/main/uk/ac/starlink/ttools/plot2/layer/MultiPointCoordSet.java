package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Defines non-central coordinates used by a MultiPointPlotter.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public interface MultiPointCoordSet {

    /**
     * Returns the coordinate definitions.
     *
     * @return   coords
     */
    Coord[] getCoords();

    /**
     * Returns the number of (non-central) data positions defined by this
     * coord set.
     *
     * @return   data position count
     */
    int getPointCount();

    /**
     * Reads the non-central points from a appropriate span of columns
     * in the current row of a supplied TupleSequence.
     * The central data position must be supplied as input.
     * The <code>icol</code> value gives the column corresponding to
     * the first coord of this coordinate set in the sequence;
     * the following few columns are assumed to correspod 1:1 to
     * the coords in this coord set.
     *
     * @param  tseq  tuple sequence, positioned at the row of interest
     * @param  icol   index of column in the sequence corresponding to
     *                the first of the coordinates in this set
     * @param  dpos0  nDataDim-element array giving central data position
     * @param  dposExtras   [nPointCount][nDataDim]-shaped array into which
     *                      the non-central data positions will be written
     * @return  true iff the conversion was successful
     */
    boolean readPoints( TupleSequence tseq, int icol, double[] dpos0,
                        double[][] dposExtras );
}
