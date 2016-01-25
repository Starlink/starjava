package uk.ac.starlink.ttools.plot2;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Defines how a set of related plot zones is presented together on a
 * graphics plane.  Each zone can contain one plotting surface.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public interface Gang {

    /**
     * Returns the number of zones in this gang.
     *
     * @return   zone count
     */
    int getZoneCount();

    /**
     * Returns the data bounds for a given zone.
     * This is the region within which data can be plotted,
     * it does not include space for external axis labels etc.
     *
     * @param   iz   index of zone
     * @return  plot bounds for zone
     */
    Rectangle getZonePlotBounds( int iz );

    /**
     * Returns the zone index for the zone to which navigation gestures
     * referenced at a particular graphics position should be delegated.
     *
     * <p>In most cases, if the position falls within the data bounds
     * of a given zone, that zone index will be returned, but if the
     * position falls outside of any zones, it may still be useful to
     * return the index of a zone whose navigator can take care of it.
     * A negative value may be returned to indicate no zone,
     * but generally it's better to indicate some zone rather than none.
     *
     * @param  pos  graphics position relating to user navigation gesture
     * @return   index of zone for navigation actions, or negative for no zone
     */
    int getNavigationZoneIndex( Point pos );
}
