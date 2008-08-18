package uk.ac.starlink.ttools.plot;

/**
 * PlotState specialist subclass used for density maps.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public class DensityPlotState extends PlotState {

    private boolean rgb_;
    private boolean zLog_;
    private double loCut_;
    private double hiCut_;
    private int pixSize_;
    private boolean weighted_;
    private Shader indexedShader_;

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
     * Sets whether the colour intensity is to be plotted on a log or
     * linear scale.
     *
     * @param  zLog  true iff you want logarithmic scaling of intensity
     */
    public void setLogZ( boolean zLog ) {
        zLog_ = zLog;
    }

    /**
     * Determines whether the colour intensity is to be plotted on a log or
     * linear scale.
     *
     * @return  true iff scaling will be logarithmic
     */
    public boolean getLogZ() {
        return zLog_;
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
     * Sets the shader object to be used for shading pixels in 
     * indexed (non-RGB) mode.
     *
     * @param  indexedShader  shader
     */
    public void setIndexedShader( Shader indexedShader ) {
        indexedShader_ = indexedShader;
    }

    /**
     * Returns the shader to be used for shading pixels in 
     * indexed (non-RGB) mode.
     *
     * @return  shader
     */
    public Shader getIndexedShader() {
        return indexedShader_;
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) && o instanceof DensityPlotState ) {
            DensityPlotState other = (DensityPlotState) o;
            return rgb_ == other.rgb_
                && zLog_ == other.zLog_
                && loCut_ == other.loCut_
                && hiCut_ == other.hiCut_
                && pixSize_ == other.pixSize_
                && weighted_ == other.weighted_
                && indexedShader_ == other.indexedShader_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + ( rgb_ ? 3 : 0 );
        code = 23 * code + ( zLog_ ? 5 : 0 );
        code = 23 * code + Float.floatToIntBits( (float) loCut_ );
        code = 23 * code + Float.floatToIntBits( (float) hiCut_ );
        code = 23 * code + pixSize_;
        code = 23 * code + ( weighted_ ? 7 : 0 );
        code = 23 * code + ( indexedShader_ == null
                                 ? 0
                                 : indexedShader_.hashCode() );
        return code;
    }
}
