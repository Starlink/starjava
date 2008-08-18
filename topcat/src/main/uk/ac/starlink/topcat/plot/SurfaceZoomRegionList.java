package uk.ac.starlink.topcat.plot;

import java.awt.Rectangle;
import java.util.AbstractList;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.SurfacePlot;

/**
 * List of zoom regions for use with a 
 * {@link uk.ac.starlink.ttools.plot.SurfacePlot}.
 * The number of elements changes according to the current state of the plot,
 * so don't copy it into an array and use that instead.
 *
 * @author   Mark Taylor
 * @since    2 Apr 2008
 */
public abstract class SurfaceZoomRegionList extends AbstractList {

    private final SurfacePlot plot_;
    private final Rectangle display_;
    private final Rectangle xTarget_;
    private final Rectangle yTarget_;
    private final ZoomRegion[] regions_;
    private boolean ok_;

    /**
     * Constructor.
     *
     * @param  plot  plot for zooming
     */
    public SurfaceZoomRegionList( SurfacePlot plot ) {
        plot_ = plot;
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
                    PlotState state = plot_.getState();
                    boolean xflip = state.getFlipFlags()[ 0 ];
                    boolean yflip = state.getFlipFlags()[ 1 ];
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
                    boolean xflip = plot_.getState().getFlipFlags()[ 0 ];
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
                    boolean yflip = plot_.getState().getFlipFlags()[ 1 ];
                    requestZoom( new double[][] {
                        null,
                        yflip ? new double[] { y0, y1 }
                              : new double[] { y1, y0 },
                    } );
                }
            },
        };
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

    public int size() {
        return ok_ ? regions_.length : 0;
    }

    public Object get( int index ) {
        return ok_ ? regions_[ index ] : null;
    }

    /**
     * Configures this list appropriately for the current state of the plot.
     * Should be called whenever plot geometry changes (including before
     * first use).
     */
    public void reconfigure() {
        Rectangle display = plot_.getSurface().getClip().getBounds();
        if ( display != null ) {
            ok_ = true;

            display_.x = display.x;
            display_.y = display.y;
            display_.width = display.width;
            display_.height = display.height;

            xTarget_.x = display.x;
            xTarget_.y = display.y + display.height;
            xTarget_.width = display.width;
            xTarget_.height = plot_.getHeight() - display_.y - display_.height;

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
        return plot_.getSurface().graphicsToData( i, display_.y, false )[ 0 ];
    }

    /**
     * Converts a dimensionless Y value to a value in Y data coordinates.
     *
     * @param  v  dimensionless value along the Y axis
     * @return  value in Y data coordinates
     */
    private double vToY( double v ) {
        int i = (int) Math.round( display_.y + v * display_.height );
        return plot_.getSurface().graphicsToData( display_.x, i, false )[ 1 ];
    }
}
