package uk.ac.starlink.topcat.plot;

/**
 * PlotState subclass which has specific features for specifying the
 * state of 3D scatter plots.  The most important extra features are
 * the viewing angles.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Plot3DState extends PlotState {

    private double theta_;
    private double phi_;

    /**
     * Sets the zenithal viewing angle.
     *
     * @param  theta  angle in radians
     */
    public void setTheta( double theta ) {
        theta_ = theta;
    }

    /**
     * Returns the zenithal viewing angle.
     *
     * @return   angle in radians
     */
    public double getTheta() {
        return theta_;
    }

    /**
     * Sets the azimuthal viewing angle.
     *
     * @param   angle in radians
     */
    public void setPhi( double phi ) {
        phi_ = phi;
    }

    /**
     * Returns the azimuthal viewing angle.
     *
     * @return  angle in radians
     */
    public double getPhi() {
        return phi_;
    }

    public boolean equals( Object otherObject ) {
        if ( otherObject instanceof Plot3DState &&
             super.equals( otherObject ) ) {
            Plot3DState other = (Plot3DState) otherObject;
            return other.theta_ == theta_
                && other.phi_ == phi_;
        }
        return false;
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + Float.floatToIntBits( (float) theta_ );
        code = 23 * code + Float.floatToIntBits( (float) phi_ );
        return code;
    }

}
