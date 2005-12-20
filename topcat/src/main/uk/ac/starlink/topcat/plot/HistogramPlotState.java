package uk.ac.starlink.topcat.plot;

/**
 * Specialisation of PlotState for use with histograms.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2005
 */
public class HistogramPlotState extends PlotState {

    private double binWidth_;
    private boolean zeroMid_;
    private boolean cumulative_;

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
     * Sets the flag which determines whether a zero value on the X axis
     * should fall in the middle of a bin or on a bin boundary.
     *
     * @param   zeroMid  true for zero in the middle of a bin, false for
     *          zero on a bin boundary
     */
    public void setZeroMid( boolean zeroMid ) {
        zeroMid_ = zeroMid;
    }

    /**
     * Returns the flag which determines whether a zero value on the X axis
     * should fall in the middle of a bin or on a bin boundary.
     *
     * @return   true of zero in the middle of a bin, false for zero on a
     *           bin boundary
     */
    public boolean getZeroMid() {
        return zeroMid_;
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

    public boolean sameAxes( PlotState other ) {
        if ( other instanceof HistogramPlotState ) {
            return super.sameAxes( other ) 
                && this.cumulative_ == ((HistogramPlotState) other).cumulative_;
        } 
        else {
            return false;
        }
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof HistogramPlotState ) {
            HistogramPlotState other = (HistogramPlotState) o;
            return binWidth_ == other.binWidth_
                && zeroMid_ == other.zeroMid_
                && cumulative_ == other.cumulative_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + Float.floatToIntBits( (float) binWidth_ );
        code = 23 * code + ( zeroMid_ ? 1 : 3 );
        code = 23 * code + ( cumulative_ ? 1 : 3 );
        return code;
    }
}
