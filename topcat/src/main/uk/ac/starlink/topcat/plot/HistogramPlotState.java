package uk.ac.starlink.topcat.plot;

/**
 * Specialisation of PlotState for use with histograms.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2005
 */
public class HistogramPlotState extends PlotState {

    private double binWidth_;

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

    public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof HistogramPlotState ) {
            HistogramPlotState other = (HistogramPlotState) o;
            return binWidth_ == other.binWidth_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + Float.floatToIntBits( (float) binWidth_ );
        return code;
    }
}
