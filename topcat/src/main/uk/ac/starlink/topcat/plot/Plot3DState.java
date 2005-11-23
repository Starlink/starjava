package uk.ac.starlink.topcat.plot;

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

    /** Unit matrix. */
    static final double[] UNIT_MATRIX = new double[] {
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,
    };

    private double[] rotation_ = UNIT_MATRIX;
    private double fogginess_ = 2.0;
    private boolean antialias_;

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
     * Sets whether antialiasing hint is preferred for drawing axes.
     *
     * @param  antialias   true to antialias, false not
     */
    public void setAntialias( boolean antialias ) {
        antialias_ = antialias;
    }

    /**
     * Determines whether antialiasing is preferred for drawing axes.
     *
     * @return  true to antialias, false not
     */
    public boolean getAntialias() {
        return antialias_;
    }

    public boolean equals( Object otherObject ) {
        if ( otherObject instanceof Plot3DState &&
             super.equals( otherObject ) ) {
            Plot3DState other = (Plot3DState) otherObject;
            return Arrays.equals( rotation_, other.rotation_ )
                && fogginess_ == other.fogginess_
                && antialias_ == other.antialias_;
        }
        return false;
    }

    public int hashCode() {
        int code = super.hashCode();
        for ( int i = 0; i < 9; i++ ) {
            code = 23 * code + Float.floatToIntBits( (float) rotation_[ i ] );
        }
        code = 23 * code + Float.floatToIntBits( (float) fogginess_ );
        code = 23 * code + ( antialias_ ? 0 : 1 );
        return code;
    }
}
