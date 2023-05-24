package uk.ac.starlink.ttools.plot2.geom;

/**
 * Defines the data range covered by a PlaneSurface.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2013
 */
public class PlaneAspect {
    private final double xmin_;
    private final double xmax_;
    private final double ymin_;
    private final double ymax_;

    /**
     * Constructor.
     *
     * @param  xlimits  2-element array giving (min,max) X data coordinates
     * @param  ylimits  2-element array giving (min,max) Y data coordinates
     */
    public PlaneAspect( double[] xlimits, double[] ylimits ) {
        xmin_ = xlimits[ 0 ];
        xmax_ = xlimits[ 1 ];
        ymin_ = ylimits[ 0 ];
        ymax_ = ylimits[ 1 ];
    }

    /**
     * Returns X data coordinate lower bound.
     *
     * @return  x min
     */
    public double getXMin() {
        return xmin_;
    }

    /**
     * Returns X data coordinate upper bound.
     *
     * @return  x max
     */
    public double getXMax() {
        return xmax_;
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

    @Override
    public String toString() {
        return new StringBuffer()
           .append( xmin_ )
           .append( "..." )
           .append( xmax_ )
           .append( "; " )
           .append( ymin_ )
           .append( "..." )
           .append( ymax_ )
           .toString();
    }
}
