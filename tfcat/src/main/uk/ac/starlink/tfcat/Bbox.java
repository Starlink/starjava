package uk.ac.starlink.tfcat;

/**
 * Represents a TFCat bbox (bounding box) structure.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class Bbox {

    private final double tmin_;
    private final double smin_;
    private final double tmax_;
    private final double smax_;

    /**
     * Constructor.
     *
     * @param  tmin  minimum time value
     * @param  smin  minimum spectral value
     * @param  tmax  maximum time value
     * @param  smax  maximum spectral value
     */
    public Bbox( double tmin, double smin, double tmax, double smax ) {
        tmin_ = tmin;
        smin_ = smin;
        tmax_ = tmax;
        smax_ = smax;
    }

    /**
     * Returns the minimum time value.
     *
     * @return  minimum value for the time coordinate
     */
    public double getTimeMin() {
        return tmin_;
    }

    /**
     * Returns the minimum spectral value.
     *
     * @return  minimum value for the spectral coordinate
     */
    public double getSpectralMin() {
        return smin_;
    }

    /**
     * Returns the maximum time value.
     *
     * @return  maximum value for the time coordinate
     */
    public double getTimeMax() {
        return tmax_;
    }

    /**
     * Returns the maximum spectral value.
     *
     * @return  maximum value for the spectral coordinate
     */
    public double getSpectralMax() {
        return smax_;
    }

    /**
     * Indicates whether a given position is within this bounding box.
     *
     * @param  pos  position
     * @return  true iff position is within or on the edge of this box
     */
    public boolean isInside( Position pos ) {
        double t = pos.getTime();
        double s = pos.getSpectral();
        return t >= tmin_ && t <= tmax_
            && s >= smin_ && s <= smax_;
    }

    @Override
    public String toString() {
        return new StringBuffer()
           .append( "[" )
           .append( tmin_ )
           .append( ", " )
           .append( smin_ )
           .append( ", " )
           .append( tmax_ )
           .append( ", " )
           .append( smax_ )
           .append( "]" )
           .toString();
    }
}
