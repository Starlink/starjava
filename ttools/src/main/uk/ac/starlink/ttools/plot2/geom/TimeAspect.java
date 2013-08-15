package uk.ac.starlink.ttools.plot2.geom;

/**
 * Defines the data range covered by a TimeSurface.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2013
 */
public class TimeAspect {

    private final double tmin_;
    private final double tmax_;
    private final double ymin_;
    private final double ymax_;

    /**
     * Constructor.
     *
     * @param   tlimits  2-element array giving (min,max) time data coordinates,
     *                   in seconds since the Unix epoch
     * @param   ylimits  2-element array giving (min,max) Y data doordinates
     */
    public TimeAspect( double[] tlimits, double[] ylimits ) {
        tmin_ = tlimits[ 0 ];
        tmax_ = tlimits[ 1 ];
        ymin_ = ylimits[ 0 ];
        ymax_ = ylimits[ 1 ];
    }

    /**
     * Returns time data coordinate lower bound.
     *
     * @return  time minimum in unix seconds
     */
    public double getTMin() {
        return tmin_;
    }

    /**
     * Returns time data coordinate upper bound.
     *
     * @return  time maximum in unix seconds
     */
    public double getTMax() {
        return tmax_;
    }

    /**
     * Returns Y data coordinate lower bound.
     *
     * @return  y min
     */
    public double getYMin() {
        return ymin_;
    }

    /**
     * Returns Y data coordinate upper bound.
     *
     * @return  y max
     */
    public double getYMax() {
        return ymax_;
    }
}
