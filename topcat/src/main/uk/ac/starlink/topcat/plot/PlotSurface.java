package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Shape;
import java.awt.print.PageFormat;
import javax.swing.JComponent;

/**
 * Defines a surface onto which plots are made.
 * This surface will typically deal with drawing axes and labels
 * and so on.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public interface PlotSurface extends GraphicsSurface {

    /**
     * Signals to the plot the characteristics of the plot which will 
     * be performed.  Setting this has no immediate effect, but 
     * when the component supplied by {@link #getComponent} 
     * next paints itself it should do so following the specifications
     * made here.
     *
     * @param  state  plot characteristics
     */
    void setState( PlotState state );

    /**
     * Requests a range of data space values to be visible on
     * this plotting surface.  The requested values are a hint which 
     * may (though probably shouldn't) be ignored, and in particular
     * may be overridden by other considerations at a later date,
     * for instance some sort of zoom action initiated by the user and
     * detected by the graphical component.
     * 
     * @param  xlo  (approximate) lower bound of X coordinate
     * @param  ylo  (approximate) lower bound of Y coordinate
     * @param  xhi  (approximate) upper bound of X coordinate
     * @param  yhi  (approximate) upper bound of Y coordinate
     */
    void setDataRange( double xlo, double ylo, double xhi, double yhi );

    /**
     * Returns the graphical component on which the plotting surface is
     * displayed.  This will contain things like axes, grids, labels etc.
     * This component will normally override
     * {@link javax.swing.JComponent#paintComponent}
     * to give a plotting background in accordance with the most recently
     * set <tt>PlotState</tt>.
     *
     * @return  plot surface display component
     */
    JComponent getComponent();

    /**
     * Causes the features of this surface to be printed as a single page.
     * It must also return a <tt>GraphicsSurface</tt> which can be used to
     * paint additional marks onto the same graphics context.
     * Invoking this method may make changes to the graphics context 
     * <tt>g</tt> - any additional plotting (markers) should be plotted onto
     * the graphics context following this method's invocation.
     *
     * @param   g   printing graphics context - may be changed by this method
     * @param   pf  printing page format
     * @return  graphics surface object which can be used for adding to
     *          the graphical output of this surface
     * @see  {@link java.awt.print.Printable}
     */
    GraphicsSurface print( Graphics g, PageFormat pf );
}
