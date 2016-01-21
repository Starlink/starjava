package uk.ac.starlink.ttools.plot2.task;

import java.awt.Point;
import java.util.EventObject;

/**
 * Describes the result of an attempt to select a data point on a plot.
 * This is usually the result of a user click action.
 *
 * @author   Mark Taylor
 * @since    21 Nov 2014
 */
public class PointSelectionEvent extends EventObject {

    private final Point point_;
    private final int isurf_;
    private final long[] closestRows_;

    /**
     * Constructor.
     *
     * @param  source  event source
     * @param  point   point indicated by the user
     * @param  isurf   numeric label of surface to which this event applies
     * @param  closestRows  array of dataset row index for each plotted layer
     */
    public PointSelectionEvent( Object source, Point point, int isurf,
                                long[] closestRows ) {
        super( source );
        point_ = point;
        isurf_ = isurf;
        closestRows_ = closestRows;
    }

    /**
     * Returns the point indicated by the user.
     *
     * @return   indicated point
     */
    public Point getPoint() {
        return point_;
    }

    /**
     * Returns the index of the surface to which this event applies.
     *
     * @return  numeric label of surface
     */
    public int getSurfaceIndex() {
        return isurf_;
    }

    /**
     * Returns an array of row indices, one for each plotted layer.
     * Each element contains the index of of the data point plotted
     * in the corresponding {@link uk.ac.starlink.ttools.plot2.PlotLayer}
     * closest to the indicated graphics position.
     * If that layer contains no data point within a few pixels of the
     * given position, the corresponding value is -1.
     * For layers without data positions, the corresponding value is
     * always -1.
     *
     * <p>The close-enough threshold for a point to be included is
     * given by {@link uk.ac.starlink.ttools.plot2.PlotUtil#NEAR_PIXELS}
     * ({@value uk.ac.starlink.ttools.plot2.PlotUtil#NEAR_PIXELS}).
     *
     * <p>Note the event may represent no successful selections
     * (all elements of the returned array equal to -1).
     *
     * @return  array of data point indices, one for each plotted layer
     */
    public long[] getClosestRows() {
        return closestRows_;
    }
}
