package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.data.Coord;

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
}
