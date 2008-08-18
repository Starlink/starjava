package uk.ac.starlink.ttools.plot;

/**
 * Specialisation of PlotState for use with histograms.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2005
 */
public class HistogramPlotState extends PlotState {

    private double binWidth_;
    private double binBase_;
    private boolean cumulative_;
    private boolean weighted_;
    private boolean normalised_;

    /**
     * Sets the bin width for the histogram.
     * In the case of a linear X axis this is an additive value (>0), and
     * in the case of a logarithmic X axis it is a multiplicative one (>1).
     *
     * @param  width   new bin width
     */
    public void setBinWidth( double width ) {
        if ( Double.isNaN( width ) || width < 0.0 ) {
            throw new IllegalArgumentException();
        }
        binWidth_ = width;
    }

    /**
     * Returns the bin width for the histogram.
     * In the case of a linear X axis this is an additive value (>0), and
     * in the case of a logarithmic X axis it is a multiplicative one (>1).
     *
     * @return   bin width
     */
    public double getBinWidth() {
        return binWidth_;
    }

    /**
     * Sets the lower bound for one of the bins.  This determines bin phase.
     *
     * @param  base  new bin base
     */
    public void setBinBase( double base ) {
        if ( Double.isNaN( base ) ) {
            throw new IllegalArgumentException();
        }
        binBase_ = base;
    }

    /**
     * Returns the lower bound for one of the bins.  This determines bin phase.
     *
     * @return  bin base
     */
    public double getBinBase() {
        return binBase_;
    }

    /**
     * Sets whether the histogram should be conventional or cumulative.
     *
     * @param  cumulative  true iff you want a cumulative plot
     */
    public void setCumulative( boolean cumulative ) {
        cumulative_ = cumulative;
    }

    /**
     * Determines whether the histogram is conventional or cumulative.
     *
     * @return  true  iff the plot will be cumulative
     */
    public boolean getCumulative() {
        return cumulative_;
    }

    /**
     * Sets whether non-unit weighting is (maybe) in force for this state.
     *
     * @param  weighted  whether weights are used
     */
    public void setWeighted( boolean weighted ) {
        weighted_ = weighted;
    }

    /**
     * Determines whether non-unit weighting is (maybe) in force for this state.
     *
     * @return  whether weights are used
     */
    public boolean getWeighted() {
        return weighted_;
    }

    /**
     * Sets whether the histogram is to be normalised to a total value of unity.
     *
     * @param  normalised  whether to normalise histogram
     */
    public void setNormalised( boolean normalised ) {
        normalised_ = normalised;
    }

    /**
     * Determines whether the histogram is normlalised to a total value of 
     * unity.
     *
     * @return  whether normalisation is in force
     */
    public boolean getNormalised() {
        return normalised_;
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof HistogramPlotState ) {
            HistogramPlotState other = (HistogramPlotState) o;
            return binWidth_ == other.binWidth_
                && binBase_ == other.binBase_
                && cumulative_ == other.cumulative_
                && weighted_ == other.weighted_
                && normalised_ == other.normalised_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + Float.floatToIntBits( (float) binWidth_ );
        code = 23 * code + Float.floatToIntBits( (float) binBase_ );
        code = 23 * code + ( cumulative_ ? 1 : 5 );
        code = 23 * code + ( weighted_ ? 1 : 7 );
        code = 23 * code + ( normalised_ ? 1 : 11 );
        return code;
    }
}
