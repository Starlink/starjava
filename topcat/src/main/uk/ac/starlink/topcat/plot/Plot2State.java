package uk.ac.starlink.topcat.plot;

import java.util.Arrays;

/**
 * PlotState implementation specific to a scatter plot.
 *
 * @author   Mark Taylor
 * @since    26 Oct 2005
 */
public class Plot2State extends PlotState {

    private boolean[] regressions_;

    /**
     * Constructor.
     */
    public Plot2State() {
        super( 2 );
    }

    /**
     * Sets an array of flags to indicate whether regression lines are
     * to be plotted for each subset.
     * Indexing matches {@link #getSubsets} array.
     *
     * @param  regressions  regression flag array
     */
    public void setRegressions( boolean[] regressions ) {
        regressions_ = regressions;
    }

    /**
     * Returns an array of flags to indicate whether regression lines are
     * to be plotted for each subset.
     * Indexing matches {@link #getSubsets} array.
     *
     * @return   regression flag array
     */
    public boolean[] getRegressions() {
        return regressions_;
    }

    public boolean equals( Object otherObject ) {
        if ( ! ( otherObject instanceof Plot2State ) ) {
            return false;
        }
        Plot2State other = (Plot2State) otherObject;
        return super.equals( other )
            && Arrays.equals( regressions_, other.regressions_ );
    }

    public int hashCode() {
        int code = super.hashCode();
        for ( int i = 0; i < regressions_.length; i++ ) {
            code = 23 * code + ( regressions_[ i ] ? 99 : 9 );
        }
        return code;
    }
}
