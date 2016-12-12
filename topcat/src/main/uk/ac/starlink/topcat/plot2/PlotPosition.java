package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Characterises explicit settings for how to position a plot component
 * in graphics coordinates.
 * This aggregates width and height, giving the external dimensions
 * of the whole plot graphic, and a Padding object, giving the gaps
 * between the dimension and the data region of the plot (this is where
 * axis annotations etc are usually drawn).
 *
 * <p>Any of these Integer dimensions may be null, indicating that no
 * explicit settings are in force.  Where there is no explicit setting,
 * the plotting machinery is expected to come up with sensible defaults.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2014
 */
@Equality
public class PlotPosition {

    private final Integer width_;
    private final Integer height_;
    private final Padding padding_;

    /**
     * Constructs a PlotPosition with no explicit settings.
     */
    public PlotPosition() {
        this( null, null, new Padding() );
    }

    /**
     * Constructs a PlotPosition from a Dimension and Insets.
     *
     * @param   width  external plot width, may be null
     * @param   height   external plot height, may be null
     * @param   padding  border between external plot dimensions
     *                   and data region, may be null or have null members
     */
    public PlotPosition( Integer width, Integer height, Padding padding ) {
        width_ = width;
        height_ = height;
        padding_ = padding == null ? new Padding() : padding;
    }

    /**
     * Returns the external width for the plot, if specified.
     *
     * @return  required external plot width in pixels, or null
     */
    public Integer getWidth() {
        return width_;
    }

    /**
     * Returns the external height for the plot, if specified.
     *
     * @return  required external plot height in pixels, or null
     */
    public Integer getHeight() {
        return height_;
    }

    /**
     * Returns settings for the border between the data region and exterior
     * dimensions of a plot.  The return value is not null, but any of
     * its members may be.
     *
     * @return  settings for border between plot data region and exterior,
     *          not null
     */
    public Padding getPadding() {
        return padding_;
    }

    @Override
    public int hashCode() {
        int code = 8874;
        code = 23 * code + PlotUtil.hashCode( width_ );
        code = 23 * code + PlotUtil.hashCode( height_ );
        code = 23 * code + PlotUtil.hashCode( padding_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PlotPosition ) {
            PlotPosition other = (PlotPosition) o;
            return PlotUtil.equals( this.width_, other.width_ )
                && PlotUtil.equals( this.height_, other.height_ )
                && PlotUtil.equals( this.padding_, other.padding_ );
        }
        else {
            return false;
        }
    }
}
