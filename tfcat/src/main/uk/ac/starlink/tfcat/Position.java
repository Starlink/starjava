package uk.ac.starlink.tfcat;

/**
 * Represents a position in (time, spectral) space.
 * The detailed meaning of the coordinates is determined by the
 * Coordinate Reference System in force.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class Position {

    private final double time_;
    private final double spectral_;

    /**
     * Constructor.
     *
     * @param  time  temporal coordinate
     * @param  spectral  spectral coordinate
     */
    public Position( double time, double spectral ) {
        time_ = time;
        spectral_ = spectral;
    }

    /**
     * Returns the time coordinate.
     *
     * @return  temporal coordinate
     */
    public double getTime() {
        return time_;
    }

    /**
     * Returns the spectral coordinate.
     *
     * @return   spectral coordinate
     */
    public double getSpectral() {
        return spectral_;
    }

    @Override
    public int hashCode() {
        int code = 9901;
        code = 23 * code + Float.floatToIntBits( (float) time_ );
        code = 23 * code + Float.floatToIntBits( (float) spectral_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Position ) {
            Position other = (Position) o;
            return this.time_ == other.time_
                && this.spectral_ ==  other.spectral_;
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuffer()
             .append( "[" )
             .append( getTime() )
             .append( ", " )
             .append( getSpectral() )
             .append( "]" )
             .toString();
    }
}
