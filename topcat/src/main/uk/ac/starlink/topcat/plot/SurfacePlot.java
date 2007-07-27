package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.AbstractList;
import javax.swing.JComponent;
import javax.swing.OverlayLayout;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Abstract superclass for plot components which display on a
 * {@link PlotSurface}.  Note that the PlotSurface is not specified in
 * the constructor but must be set before any plotting is done.
 * This class mainly handles keeping track of the current state
 * (<code>PlotState</code>, <code>Points</code> and <code>PlotSurface</code>
 * members).  The actual work is done by the <code>paintComponent</code>
 * method of concrete subclasses.  It also provides zooming in X, Y and
 * XY directions.
 *
 * <p>The details of the plot are determined by a {@link PlotState} object
 * which indicates what the plot will look like and a {@link Points}
 * object which provides the data to plot.  Setting these values does
 * not itself trigger a change in the component, they only take effect
 * when {@link #paintComponent} is called (e.g. following a {@link #repaint}
 * call).
 * The drawing of axes and other decorations is done by a decoupled
 * {@link PlotSurface} object (bridge pattern).
 *
 * @author   Mark Taylor
 * @since    11 Nov 2005
 */
public abstract class SurfacePlot extends JComponent implements Printable {

    private Points points_;
    private PlotState state_;
    private PlotSurface surface_;
    private final SurfaceZoomRegions zoomRegions_;
    private final Zoomer zoomer_;

    /**
     * Constructor.
     */
    protected SurfacePlot() {
        setLayout( new OverlayLayout( this ) );
        zoomRegions_ = new SurfaceZoomRegions();
        zoomer_ = new Zoomer();
        zoomer_.setRegions( zoomRegions_ );
        zoomer_.setCursorComponent( this );
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
            comp.removeMouseListener( zoomer_ );
            comp.removeMouseMotionListener( zoomer_ );
        }
        surface_ = surface;
        surface_.setState( state_ );

        Component comp = surface_.getComponent();
        add( comp );
        comp.addMouseListener( zoomer_ );
        comp.addMouseMotionListener( zoomer_ );
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
     * Returns the current point selection.
     * This convenience method just retrieves it from the current plot state.
     *
     * @return   point selection
     */
    public PointSelection getPointSelection() {
        return state_.getPointSelection();
    }

    /**
     * Invoked when the user indicates by mouse gestures that a zoomed
     * view is wanted.  The elements of the <code>bounds</code> array 
     * are 2-element <code>double[]</code> arrays giving (lower, upper) 
     * bounds of the range along each axis which is required.
     * A null element indicates that no zooming along that axis is required.
     * Boundary values are in data coordinates.
     *
     * @param   bounds  zoom request details
     */
    protected abstract void requestZoom( double[][] bounds );

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
        if ( state_ != null && state_.getValid() ) {
            double[][] bounds = state_.getRanges();
            surface_.setDataRange( bounds[ 0 ][ 0 ], bounds[ 1 ][ 0 ],
                                   bounds[ 0 ][ 1 ], bounds[ 1 ][ 1 ] );
        }
        if ( isOpaque() ) {
            Color color = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( color );
        }
        zoomRegions_.configure( surface_.getClip().getBounds() );
    }

    /**
     * List implementation containing as elements the zoom regions appropriate
     * for this plot.
     */
    private class SurfaceZoomRegions extends AbstractList {

        private boolean ok_;
        private final Rectangle display_;
        private final Rectangle xTarget_;
        private final Rectangle yTarget_;
        private final ZoomRegion[] regions_;

        /**
         * Constructor.
         */
        SurfaceZoomRegions() {
            display_ = new Rectangle();
            xTarget_ = new Rectangle();
            yTarget_ = new Rectangle();
            regions_ = new ZoomRegion[] {

                /* Region for X/Y zooming on the main display surface. */
                new XYZoomRegion( display_ ) {
                    public void zoomed( double[][] bounds ) {
                        double x0 = vToX( bounds[ 0 ][ 0 ] );
                        double x1 = vToX( bounds[ 0 ][ 1 ] );
                        double y0 = vToY( bounds[ 1 ][ 0 ] );
                        double y1 = vToY( bounds[ 1 ][ 1 ] );
                        boolean xflip = state_.getFlipFlags()[ 0 ];
                        boolean yflip = state_.getFlipFlags()[ 1 ];
                        requestZoom( new double[][] {
                            xflip ? new double[] { x1, x0 }
                                  : new double[] { x0, x1 },
                            yflip ? new double[] { y0, y1 }
                                  : new double[] { y1, y0 },
                        } );
                    }
                },

                /* Region for X zooming below the X axis. */
                new AxisZoomRegion( true, xTarget_, display_ ) {
                    public void zoomed( double[][] bounds ) {
                        double x0 = vToX( bounds[ 0 ][ 0 ] );
                        double x1 = vToX( bounds[ 0 ][ 1 ] );
                        boolean xflip = state_.getFlipFlags()[ 0 ];
                        requestZoom( new double[][] { 
                            xflip ? new double[] { x1, x0 }
                                  : new double[] { x0, x1 },
                            null,
                        } );
                    }
                },

                /* Region for Y zooming to the left of the Y axis. */
                new AxisZoomRegion( false, yTarget_, display_ ) {
                    public void zoomed( double[][] bounds ) {
                        double y0 = vToY( bounds[ 0 ][ 0 ] );
                        double y1 = vToY( bounds[ 0 ][ 1 ] );
                        boolean yflip = state_.getFlipFlags()[ 1 ];
                        requestZoom( new double[][] {
                            null,
                            yflip ? new double[] { y0, y1 }
                                  : new double[] { y1, y0 },
                        } );
                    }
                },
            };
        }

        public int size() {
            return ok_ ? regions_.length : 0;
        }

        public Object get( int index ) {
            return ok_ ? regions_[ index ] : null;
        }

        /**
         * Configures this list appropriately for a given display region.
         *
         * @param   display  rectangle giving the part of the component on
         *          which the data is actually plotted (between the axes)
         */
        public void configure( Rectangle display ) {
            if ( display != null ) {
                ok_ = true;

                display_.x = display.x;
                display_.y = display.y;
                display_.width = display.width;
                display_.height = display.height;

                xTarget_.x = display.x;
                xTarget_.y = display.y + display.height;
                xTarget_.width = display.width;
                xTarget_.height = getHeight() - display_.y - display_.height;

                yTarget_.x = 0;
                yTarget_.y = display.y;
                yTarget_.width = display.x;
                yTarget_.height = display.height;
            }
            else {
                ok_ = false;
            }
        }

        /**
         * Converts a dimensionless X value to a value in X data coordinates.
         *
         * @param   v  dimensionless value along the X axis
         * @return  value in X data coordinates
         */
        private double vToX( double v ) {
            int i = (int) Math.round( display_.x + v * display_.width );
            return surface_.graphicsToData( i, display_.y, false )[ 0 ];
        }

        /**
         * Converts a dimensionless Y value to a value in Y data coordinates.
         *
         * @param  v  dimensionless value along the Y axis
         * @return  value in Y data coordinates
         */
        private double vToY( double v ) {
            int i = (int) Math.round( display_.y + v * display_.height );
            return surface_.graphicsToData( display_.x, i, false )[ 1 ];
        }
    }
}
