package uk.ac.starlink.ttools.plot;

import java.util.Arrays;

/**
 * PlotState subclass which has specific features for specifying the
 * state of 3D scatter plots.  The most important extra feature is
 * the rotation matrix, which describes the viewing angle for the
 * 3D data space.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Plot3DState extends PlotState {

    private double[] rotation_;
    private double fogginess_;
    private boolean isRotating_;
    private double zoomScale_;

    /**
     * Sets the rotation matrix.
     *
     * @param  matrix  9-element 3d rotation matrix
     */
    public void setRotation( double[] matrix ) {
        rotation_ = (double[]) matrix.clone();
    }

    /**
     * Returns the rotation matrix.
     *
     * @return  9-element 3d rotation matrix
     */
    public double[] getRotation() {
        return rotation_;
    }

    /**
     * Sets the intensity of fog used for depth rendering 
     * (1 is a reasonable amount; 0 is no fog).
     *
     * @param  fog  fog intensity
     */
    public void setFogginess( double fog ) {
        fogginess_ = fog;
    }

    /**
     * Returns the intensity of fog used for depth rendering.
     *
     * @return fog intensity
     */
    public double getFogginess() {
        return fogginess_;
    }

    /**
     * Sets whether the plot is currently rotating.  A true value for this
     * indicates that the whole set of data points does not need to be
     * drawn; a sequence of plot requests with <code>isRotating</code> true
     * must be followed quickly by one with <code>isRotating</code> false.
     *
     * @param  isRotating  true iff this plot is currently in the middle
     *         of a sequence of rotation operations
     */
    public void setRotating( boolean isRotating ) {
        isRotating_ = isRotating;
    }

    /**
     * Indicates whether the plot is currently rotating.  A true value for
     * indicates that the whole set of data points does not need to be
     * drawn; a sequence of plot requests with <code>isRotating</code> true
     * must be followed quickly by one with <code>isRotating</code> false.
     *
     * @return  true if not all the points need to be drawn this time
     */
    public boolean getRotating() {
        return isRotating_;
    }

    /**
     * Returns the zoom level.  
     * Unity is normal size, and a larger number means more magnification.
     *
     * @param zoomScale  zoom level
     */
    public void setZoomScale( double zoomScale ) {
        zoomScale_ = zoomScale;
    }

    /**
     * Sets the zoom level.
     *
     * @return  zoom level
     */
    public double getZoomScale() {
        return zoomScale_;
    }

    public boolean equals( Object otherObject ) {
        if ( otherObject instanceof Plot3DState &&
             super.equals( otherObject ) ) {
            Plot3DState other = (Plot3DState) otherObject;
            return Arrays.equals( rotation_, other.rotation_ )
                && fogginess_ == other.fogginess_
                && isRotating_ == other.isRotating_
                && zoomScale_ == other.zoomScale_;
        }
        return false;
    }

    public int hashCode() {
        int code = super.hashCode();
        for ( int i = 0; i < 9; i++ ) {
            code = 23 * code + Float.floatToIntBits( (float) rotation_[ i ] );
        }
        code = 23 * code + Float.floatToIntBits( (float) fogginess_ );
        code = 23 * code + ( isRotating_ ? 0 : 1 );
        code = 23 * code + Float.floatToIntBits( (float) zoomScale_ );
        return code;
    }
}
