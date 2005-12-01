package uk.ac.starlink.topcat.plot;

/**
 * PlotState specialist subclass used for density maps.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public class DensityPlotState extends PlotState {

    private boolean rgb_;
    private double loCut_;
    private double hiCut_;
    private int pixSize_;

    /**
     * Sets whether the plot will be coloured.
     * 
     * @param   rgb  true for colour, false for monochrome
     */
    public void setRgb( boolean rgb ) {
        rgb_ = rgb;
    }

    /**
     * Determines whether the plot will be coloured.
     *
     * @return  true for colour, false for monochrome
     */
    public boolean getRgb() {
        return rgb_;
    }

    /**
     * Sets the size of each data pixel (bin) in screen pixels.
     *
     * @param  psize  pixel size
     */
    public void setPixelSize( int psize ) {
        pixSize_ = psize;
    }

    /**
     * Gets the size of each data pixel (bin) in screen pixels.
     *
     * @return   pixel size
     */
    public int getPixelSize() {
        return pixSize_;
    }

    /**
     * Sets the lower cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @param  frac   lower cut value (0-1)
     */
    public void setLoCut( double frac ) {
        loCut_ = frac;
    }

    /**
     * Gets the lower cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @return  lower cut value (0-1)
     */
    public double getLoCut() {
        return loCut_;
    }

    /**
     * Sets the upper cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @param  frac   upper cut value (0-1)
     */
    public void setHiCut( double frac ) {
        hiCut_ = frac;
    }

    /**
     * Gets the upper cut value, as a fraction of the visible bins.
     * This determines the brightness of the plot.
     *
     * @return  upper cut value (0-1)
     */
    public double getHiCut() {
        return hiCut_;
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof DensityPlotState ) {
            DensityPlotState other = (DensityPlotState) o;
            return rgb_ == other.rgb_
                && loCut_ == other.loCut_
                && hiCut_ == other.hiCut_
                && pixSize_ == other.pixSize_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + ( rgb_ ? 1 : 0 );
        code = 23 * code + Float.floatToIntBits( (float) loCut_ );
        code = 23 * code + Float.floatToIntBits( (float) hiCut_ );
        code = 23 * code + pixSize_;
        return code;
    }
}
