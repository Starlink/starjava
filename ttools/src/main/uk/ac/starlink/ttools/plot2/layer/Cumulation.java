package uk.ac.starlink.ttools.plot2.layer;

/**
 * Cumulative mode for histogram-like plots.
 *
 * @author   Mark Taylor
 * @since    10 Mar 2021
 */
public enum Cumulation {

    /** Not cumulative. */
    NONE( false, false ),

    /** Cumulative in increasing coordinate direction. */
    FORWARD( true, false ),

    /** Cumulative in decreasing coordinate direction. */
    REVERSE( true, true );

    private final boolean isCumulative_;
    private final boolean isReverse_;

    /**
     * Constructor.
     *
     * @param  isCumulative  whether this mode is cumulative or not
     * @param  isReverse   true for cumulativeness in decreasing direction
     */
    private Cumulation( boolean isCumulative, boolean isReverse ) {
        isCumulative_ = isCumulative;
        isReverse_ = isReverse;
    }

    /**
     * Indicates whether this mode is cumulative.
     *
     * @return   true for cumulative, false for normal
     */
    public boolean isCumulative() {
        return isCumulative_;
    }

    /**
     * Indicates whether any cumulativeness is in the positive coordinate
     * direction.
     *
     * @return   false for positive direction, true for negative direction
     */
    public boolean isReverse() {
        return isReverse_;
    }

    /**
     * Returns a plain-text description of the meaning of this mode.
     *
     * @return  plain-text description
     */
    public String getTextDescription() {
        if ( isCumulative_ ) {
            return "The value plotted for each bin uses "
                 + "the samples accumulated all the way from "
                 + ( isReverse() ? "positive " : "negative " )
                 + "infinity to that bin.";
        }
        else {
            return "The value plotted for each bin uses "
                 + "the samples accumulated in that bin.";
        }
    }
}
