package uk.ac.starlink.ttools.plot;

import uk.ac.starlink.table.ValueInfo;

/**
 * PlotState specific to spherical 3D plots.
 *
 * @author   Mark Taylor
 * @since    5 Jan 2006
 */
public class SphericalPlotState extends Plot3DState {

    private SimpleValueInfo radialInfo_;
    private boolean radialLog_;

    /**
     * Sets metadata information about the radial coordinate.
     * A null value indicates that there is no useful radial information
     * (all radial values are to be taken as unity).
     * Note some information from the submitted <code>info</code> object
     * may be discarded, only the parts required for plotting are retained.
     *
     * @param  info  radial coordinate metadata
     */
    public void setRadialInfo( ValueInfo info ) {
        radialInfo_ = info == null ? null : new SimpleValueInfo( info );
    }

    /**
     * Returns metadata information about the radial coordinate.
     * A null value indicates that there is no useful radial information
     * (all radial values are to be taken as unity).
     *
     * @return  radial coordinate metadata
     */
    public ValueInfo getRadialInfo() {
        return radialInfo_;
    }

    /**
     * Sets whether the radial dimension should be plotted on a logarithmic
     * scale.  This is only relevant for spherical plots.
     *
     * @param  radialLog  true for logarithmic treatment of the radial
     *         coordinate, false for linear
     */
    public void setRadialLog( boolean radialLog ) {
        radialLog_ = radialLog;
    }

    /**
     * Determines whether the radial dimension should be plotted on a
     * logarithmic scale.  This is only relevant for spherical plots.
     *
     * @return   true for logarithmic treatment of the radial coordinate,
     *           false for linear
     */
    public boolean getRadialLog() {
        return radialLog_;
    }

    public boolean equals( Object o ) {
        if ( o instanceof SphericalPlotState && super.equals( o ) ) {
            SphericalPlotState other = (SphericalPlotState) o;
            return ( radialInfo_ == null
                         ? other.radialInfo_ == null
                         : radialInfo_.equals( other.radialInfo_ ) )
                && ( radialInfo_ == null
                         ? true 
                         : radialLog_ == other.radialLog_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + ( radialInfo_ == null ? 0 : radialInfo_.hashCode() );
        code = 23 * code + ( radialLog_ ? 1 : 0 );
        return code;
    }

}
