package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.OverlayLayout;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which can display a scatter plot of points.
 * The details of the plot are determined by a {@link PlotState} object
 * which indicates what the plot will look like and a {@link Points}
 * object which provides the data to plot.  Setting these values does
 * not itself trigger a change in the component, they only take effect
 * when {@link #paintComponent} is called (e.g. following a {@link #repaint}
 * call).  The X and Y ranges of the displayed plot are not specified
 * programatically; they may be changed by user interaction.
 * The drawing of axes and other decorations is done by a decoupled
 * {@link PlotSurface} object (bridge pattern).
 * 
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2004
 */
public class ScatterPlot extends JComponent implements Printable {

    private Points points_;
    private PlotState state_;
    private PlotSurface surface_;
    private Annotations annotations_;
    private Points lastPoints_;
    private PlotState lastState_;
    private PlotSurface lastSurface_;
    private int lastWidth_;
    private int lastHeight_;
    private Image image_;

    /**
     * Constructs a new scatter plot, specifying the initial plotting surface
     * which provides axis plotting and so on.
     *
     * @param  surface  plotting surface implementation
     */
    public ScatterPlot( PlotSurface surface ) {
        setLayout( new OverlayLayout( this ) );
        setOpaque( false );
        add( new ScatterDataPanel() );
        annotations_ = new Annotations();
        setSurface( surface );
    }
    
    /**
     * Sets the plotting surface which draws axes and other decorations
     * that form the background to the actual plotted points.
     *
     * @param  surface  plotting surface implementation
     */
    public void setSurface( PlotSurface surface ) {
        if ( surface_ != null ) {
            remove( surface_.getComponent() );
        }
        surface_ = surface;
        surface_.setState( state_ );
        surface_.getComponent().setOpaque( false );
        add( surface_.getComponent() );
    }

    /**
     * Returns the plotting surface on which this component displays.
     *
     * @return   plotting surface
     */
    public PlotSurface getSurface() {
        return surface_;
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     *
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        points_ = points;
        annotations_ = new Annotations();
    }

    /**
     * Returns the data set for this point.
     * 
     * @return  data points
     */
    public Points getPoints() {
        return points_;
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( PlotState state ) {
        state_ = state;
        annotations_.validate();
        if ( surface_ != null ) {
            surface_.setState( state_ );
        }
    }

    /**
     * Returns the most recently set state for this plot.
     *
     * @return  plot state
     */
    public PlotState getState() {
        return state_;
    }

    /**
     * Sets the point at index <tt>ip</tt> of the Points object "active".
     * It will be marked out somehow or other when plotted.
     *
     * @param  ip  active point index, or -1 to indicate no active point
     */
    public void setActivePoint( int ip ) {
        annotations_.activePoint_ = ip;
    }

    /**
     * Updates the X and Y ranges of the plotting surface so that all the
     * data points which are currently selected for plotting will fit in
     * nicely.
     */
    public void rescale() {
        boolean xlog = state_.isXLog();
        boolean ylog = state_.isYLog();
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        double ylo = Double.POSITIVE_INFINITY;
        double yhi = ylog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        /* Go through all points getting max/min values. */
        int nok = 0;
        Points points = points_;
        if ( points != null ) {
            RowSubset[] rsets = getState().getSubsets();
            int nrset = rsets.length;
            int np = points.getCount();
            double[] xv = points.getXVector();
            double[] yv = points.getYVector();
            for ( int ip = 0; ip < np; ip++ ) {

                /* First see if this point will be plotted. */
                boolean use = false;
                long lp = (long) ip;
                for ( int is = 0; ! use && is < nrset; is++ ) {
                    use = use || rsets[ is ].isIncluded( lp );
                }
                if ( use ) {
                    double xp = xv[ ip ];
                    double yp = yv[ ip ];
                    if ( ! Double.isNaN( xp ) && 
                         ! Double.isNaN( yp ) &&
                         ! Double.isInfinite( xp ) && 
                         ! Double.isInfinite( yp ) &&
                         ( ! xlog || xp > 0.0 ) &&
                         ( ! ylog || yp > 0.0 ) ) {
                        nok++;
                        if ( xp < xlo ) {
                            xlo = xp;
                        }
                        if ( xp > xhi ) {
                            xhi = xp;
                        }
                        if ( yp < ylo ) {
                            ylo = yp;
                        }
                        if ( yp > yhi ) {
                            yhi = yp;
                        }
                    }
                }
            }
        }

        /* Default to sensible ranges if we didn't find any good data. */
        if ( nok == 0 ) {
            xlo = xlog ? 1e-1 : -1.0;
            xhi = xlog ? 1e+1 : +1.0; 
            ylo = ylog ? 1e-1 : -1.0;
            yhi = ylog ? 1e+1 : +1.0;
        }

        /* Ask the plotting surface to set the new ranges accordingly. */
        surface_.setDataRange( xlo, ylo, xhi, yhi );
    }

    /**
     * Plots the points of this scatter plot onto a given graphics context
     * using its current plotting surface to define the mapping of data
     * to graphics space.
     *
     * @param  g  graphics context
     */
    private void drawPoints( Graphics g ) {
        Points points = points_;
        PlotState state = state_;

        Shape oldClip = g.getClip();
        g.setClip( surface_.getClip() );

        double[] xv = points.getXVector();
        double[] yv = points.getYVector();
        int np = points.getCount();
        RowSubset[] sets = state.getSubsets();
        MarkStyle[] styles = state.getStyles();
        int nset = sets.length;
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = styles[ is ];
            int maxr = style.getMaximumRadius();
            for ( int ip = 0; ip < np; ip++ ) {
                if ( sets[ is ].isIncluded( (long) ip ) ) {
                    Point point = surface_.dataToGraphics( xv[ ip ], yv[ ip ] );
                    if ( point != null ) {
                        int xp = point.x;
                        int yp = point.y;
                        if ( g.hitClip( xp, yp, maxr, maxr ) ) {
                            style.drawMarker( g, xp, yp );
                        }
                    }
                }
            }
        }
        g.setClip( oldClip );
    }

    /**
     * Plots the registered annotations of this scatter plot onto a given
     * graphics context using its current plotting surface to define the
     * mapping of data to graphics space.
     *
     * @param  g  graphics context
     */
    private void drawAnnotations( Graphics g ) {
        annotations_.draw( g );
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
            Graphics2D g2 = (Graphics2D) g.create();
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
            g2.translate( xinset, yinset );
            g2.scale( xscale, yscale );

            /* Draw the plot. */
            print( g2 );
            return PAGE_EXISTS;
        }
        else {
            return NO_SUCH_PAGE;
        }
    }

    /**
     * This class takes care of all the markings plotted over the top of
     * the plot proper.  It's coded as an extra class just to make it tidy,
     * these workings could equally be in the body of ScatterPlot.
     */
    private class Annotations {

        int activePoint_ = -1;

        /**
         * Paints all the current annotations onto a given graphics context.
         *
         * @param  g  graphics context
         */
        void draw( Graphics graphics ) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            /* Draw an active point if there is one. */
            if ( activePoint_ >= 0 ) {
                Point p = surface_
                      .dataToGraphics( points_.getXVector()[ activePoint_ ], 
                                       points_.getYVector()[ activePoint_ ] );
                if ( p != null ) {
                    Graphics2D g = (Graphics2D) g2.create();
                    g.setColor( new Color( 0, 0, 0, 192 ) );
                    g.setStroke( new BasicStroke( 2 ) );
                    g.translate( p.x, p.y );
                    g.drawOval( -6, -6, 13, 13 );
                    g.drawLine( 0, +4, 0, +8 );
                    g.drawLine( 0, -4, 0, -8 );
                    g.drawLine( +4, 0, +8, 0 );
                    g.drawLine( -4, 0, -8, 0 );
                }
            }
        }

        /**
         * Updates this annotations object as appropriate for the current
         * state of the plot.
         */
        void validate() {

            /* If there is an active point which no longer corresponds to
             * a visible point, drop it. */
            if ( activePoint_ >= 0 ) {
                boolean in = false;
                RowSubset[] sets = state_.getSubsets();
                int nset = sets.length;
                for ( int is = 0; is < nset && !in; is++ ) {
                    if ( sets[ is ].isIncluded( (long) activePoint_ ) ) {
                        in = true;
                    }
                }
                if ( ! in ) {
                    activePoint_ = -1;
                }
            }
        }
    }


    /**
     * Graphical component which does the actual plotting of the points.
     * Its basic job is to call {@link @drawPoints} and {@link drawAnnotations}
     * in its {@link paintComponent} method.  
     * However it makes things a bit more
     * complicated than that for the purposes of efficiency.
     */
    private class ScatterDataPanel extends JComponent {

        ScatterDataPanel() {
            setOpaque( false );
        }

        /**
         * Draws the component to a graphics context which probably 
         * represents the screen, by calling {@link #drawPoints}
         * and {@link #drawAnnotations}.
         * Since <tt>drawPoints</tt> might be fairly expensive 
         * (if there are a lot of points), and <tt>paintComponent</tt> 
         * can be called often without the data changing 
         * (e.g. cascading window uncover events) it is advantageous 
         * to cache the drawn points in an Image and effectively
         * blit it to the screen on subsequent paint requests.
         */
        protected void paintComponent( Graphics g ) {
 
            /* If the plotting requirements have changed since last time
             * we painted, we can use the cached image. */
            int width = getBounds().width;
            int height = getBounds().height;
            if ( state_ == lastState_ &&
                 surface_ == lastSurface_ &&
                 points_ == lastPoints_ &&
                 width == lastWidth_ &&
                 height == lastHeight_ ) {
                assert image_ != null;
            }

            /* We need to replot the points. */
            else {

                /* Get an image to plot into.  Take care that this is an
                 * image which can take advantage of hardware acceleration
                 * (e.g. "new BufferedImage()" is no good). */
                image_ = createImage( width, height );
                Graphics ig = image_.getGraphics();

                /* Unfortunately, accelerated-graphics-capable contexts
                 * are unable to handle transparency very well.  This means
                 * that the plotting surface will not show up behind the
                 * plotted points in the cached image buffer, even though
                 * this component is supposed to be transparent. 
                 * So we fudge it by calling the plotting surface's 
                 * <tt>paintSurface</tt> method (introduced for this purpose)
                 * here.  This is not incredibly tidy. */
                surface_.paintSurface( ig );

                /* Plot the actual points into the cached buffer. */
                drawPoints( ig );

                /* Record the state which corresponds to the most recent
                 * plot into the cached buffer. */
                lastState_ = state_;
                lastSurface_ = surface_;
                lastPoints_ = points_;
                lastWidth_ = width;
                lastHeight_ = height;
            }

            /* Copy the image from the (new or old) cached buffer onto
             * the graphics context we are being asked to paint into. */
            boolean done = g.drawImage( image_, 0, 0, null );
            assert done;
            drawAnnotations( g );
        }

        /**
         * The normal implementation for this method calls 
         * <tt>paintComponent</tt> somewhere along the way.
         * However, if we are painting the component 
         * for some reason other than posting it
         * on the screen, we don't want to use the cached-image approach
         * used in <tt>paintComponent</tt>, since it might be a 
         * non-pixelised graphics context (e.g. postscript).
         * So for <tt>printComponent</tt>, just invoke <tt>drawPoints</tt>
         * straightforwardly.
         *
         * <p>A more respectable way to do this might be to test the
         * <tt>Graphics2D.getDeviceConfiguration().getDevice().getType()</tt>
         * value in <tt>paintComponent</tt> - however, for at least one
         * known printer-type graphics context 
         * (<tt>org.jibble.epsgraphics.EpsGraphics2D</tt>) this returns
         * the wrong value (TYPE_IMAGE_BUFFER not TYPE_PRINTER).
         */
        protected void printComponent( Graphics g ) {
            drawPoints( g );
            drawAnnotations( g );
        }
    }

}
