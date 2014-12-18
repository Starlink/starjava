package uk.ac.starlink.topcat.plot2;

import java.awt.Dimension;
import java.awt.Insets;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Characterises explicit settings for how to position a plot component
 * in graphics coordinates.
 * This aggregates a Dimension object, giving the external dimensions
 * of the whole plot graphic, and an Insets object, giving the gaps
 * between the dimension and the data region of the plot (this is where
 * axis annotations etc are usually drawn).
 *
 * <p>Either or both of these items may be null, indicating that no
 * explicit settings are in force.  Additionally any of the integer members
 * of the size or insets may be negative, meaning that there is no explicit
 * setting for the value in question.  Where there is no explicit setting,
 * the plotting machinery is expected to come up with sensible defaults.
 *
 * <p>Although Dimension and Insets are mutable, they are treated here
 * as if immutable (cloned on the way in and out).
 *
 * @author   Mark Taylor
 * @since    18 Dec 2014
 */
@Equality
public class PlotPosition {

    private final Dimension size_;
    private final Insets insets_;

    /**
     * Constructs a PlotPosition with no explicit settings.
     */
    public PlotPosition() {
        this( null, null );
    }

    /**
     * Constructs a PlotPosition from a Dimension and Insets.
     *
     * @param   size  external plot dimensions, may be null
     * @param   insets  border between external plot dimensions and data region
     */
    public PlotPosition( Dimension size, Insets insets ) {
        size_ = size == null ? null : size.getSize();
        insets_ = insets == null ? null : (Insets) insets.clone();
    }

    /**
     * Returns settings for the exterior dimensions of a plot.
     * The return value may be null, or either of its members may be -1,
     * indicating no default setting.
     *
     * @return  settings for plot exterior size
     */ 
    public Dimension getPlotSize() {
        return size_ == null ? null : size_.getSize();
    }

    /**
     * Returns settings for the border between the data region and exterior
     * dimensions of a plot.
     * The return value may be null, or any of its members may be -1,
     * indicating no default setting.
     *
     * @return  settings for border between plot data region and exterior
     */
    public Insets getPlotInsets() {
        return insets_ == null ? null : (Insets) insets_.clone();
    }

    @Override
    public int hashCode() {
        int code = 8874;
        code = 23 * code + PlotUtil.hashCode( size_ );
        code = 23 * code + PlotUtil.hashCode( insets_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PlotPosition ) {
            PlotPosition other = (PlotPosition) o;
            return PlotUtil.equals( this.size_, other.size_ )
                && PlotUtil.equals( this.insets_, other.insets_ );
        }
        else {
            return false;
        }
    }
}
