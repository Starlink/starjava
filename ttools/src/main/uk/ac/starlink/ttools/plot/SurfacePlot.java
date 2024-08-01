package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JComponent;
import javax.swing.OverlayLayout;

/**
 * Abstract superclass for plot components which display on a
 * {@link PlotSurface}.  Note that the PlotSurface is not specified in
 * the constructor but must be set before any plotting is done.
 * This class mainly handles keeping track of the current state
 * (<code>PlotState</code> and <code>PlotSurface</code> members).
 * The actual work is done by the <code>paintComponent</code>
 * method of concrete subclasses.
 *
 * <p>The details of the plot are determined by a {@link PlotState} object
 * which indicates what the plot will look like.
 * Setting these values does not itself trigger a change in the component,
 * they only take effect when {@link #paintComponent} is called 
 * (e.g. following a {@link #repaint} call).
 * The drawing of axes and other decorations is done by a decoupled
 * {@link PlotSurface} object (bridge pattern).
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public abstract class SurfacePlot extends TablePlot implements Printable {

    private PlotSurface surface_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    protected SurfacePlot() {
        setLayout( new OverlayLayout( this ) );
    }

    /**
     * Sets the plotting surface which draws axes and other decorations
     * that form the background to the actual plotted points.
     * This must be called before the component is drawn.
     *
     * @param  surface  plotting surface implementation
     */
    public void setSurface( PlotSurface surface ) {
        if ( surface_ != null ) {
            Component comp = surface_.getComponent();
            remove( comp );
        }
        surface_ = surface;
        surface_.setState( getState() );
        Font font = getFont();
        if ( font != null && surface_ instanceof PtPlotSurface ) {
            ((PtPlotSurface) surface_).setFont( font );
        }
        Component comp = surface_.getComponent();
        add( comp );
    }

    /**
     * Returns the plotting surface on which this component displays.
     *
     * @return   plotting surface
     */
    public PlotSurface getSurface() {
        return surface_;
    }

    public Rectangle getPlotBounds() {
        Rectangle bounds = getSurface().getClip().getBounds();
        bounds.y--;
        bounds.height += 2;
        return bounds;
    }

    public void setState( PlotState state ) {
        super.setState( state );
        if ( surface_ != null ) {
            surface_.setState( state );
        }
    }

    public void setFont( Font font ) {
        super.setFont( font );
        if ( surface_ instanceof PtPlotSurface ) {
            ((PtPlotSurface) surface_).setFont( font );
        }
    }

    /**
     * Implements the {@link java.awt.print.Printable} interface.
     * At time of writing, this method is not used by TOPCAT, though it
     * could be; in particular it is not used to implement the
     * export to EPS functionality.
     * The code is mostly pinched from SPLAT.
     */
    public int print( Graphics g, PageFormat pf, int pageIndex ) {
        if ( pageIndex == 0 ) {

            /* Get a graphics object scaled for this component to print on. */
            int gap = 70;  // points
            double pageWidth = pf.getImageableWidth() - 2.0 * gap;
            double pageHeight = pf.getImageableHeight() - 2.0 * gap;
            double xinset = pf.getImageableX() + gap;
            double yinset = pf.getImageableY() + gap;
            double compWidth = (double) getWidth();
            double compHeight = (double) getHeight();
            double xscale = pageWidth / compWidth;
            double yscale = pageHeight / compHeight;
            if ( xscale < yscale ) {
                yscale = xscale;
            }
            else {
                xscale = yscale;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.translate( xinset, yinset );
            g2.scale( xscale, yscale );

            /* Draw the plot. */
            print( g2 );

            /* Restore. */
            g2.scale( 1.0 / xscale, 1.0 / yscale );
            g2.translate( - xinset, - yinset );
            return PAGE_EXISTS;
        }
        else {
            return NO_SUCH_PAGE;
        }
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        PlotState state = getState();
        if ( state != null && state.getValid() ) {
            double[][] bounds = state.getRanges();
            surface_.setDataRange( bounds[ 0 ][ 0 ], bounds[ 1 ][ 0 ],
                                   bounds[ 0 ][ 1 ], bounds[ 1 ][ 1 ] );
        }
        if ( isOpaque() ) {
            Color color = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( color );
        }
    }
}
