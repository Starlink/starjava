package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Arrays;
import java.util.BitSet;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which can display a density plot, which is to say, a
 * two-dimensional histogram.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2005
 */
public class DensityPlot extends SurfacePlot {

    private BinGrid[] grids_;
    private double[] gridLoBounds_;
    private double[] gridHiBounds_;
    private PointSelection lastPointSelection_;
    private BitSet visible_;
    private BufferedImage image_;
    private Rectangle lastPlotZone_;
    private DensityPlotState lastState_;

    /**
     * Constructor.
     *
     * @param  surface  plotting surface to receive pixels
     */
    public DensityPlot( PlotSurface surface ) {
        super();
        setPreferredSize( new Dimension( 400, 400 ) );
        surface.getComponent().setOpaque( false );

        /* I'd like to add these components in the opposite order, so that
         * the grid lines are drawn over the top of the plot.  However, 
         * it doesn't work, because the surface only works out its
         * dimensions during its paintComponent method, and if that isn't
         * called first then the DensityDataPanel draws itself using
         * the wrong geometry. */
        // setSurface( surface );
        // add( new DensityDataPanel() );
        add( new DensityDataPanel() );
        setSurface( surface );
    }

    public void setState( PlotState state ) {
        super.setState( state );
        if ( state.getPointSelection() != lastPointSelection_ ) {
            grids_ = null;
            image_ = null;
            lastPointSelection_ = state.getPointSelection();
        }
    }

    public void setPoints( Points points ) {
        Points lastPoints = getPoints();
        if ( points != lastPoints ) {
            grids_ = null;
            image_ = null;
        }
        super.setPoints( points );
    }

    public double[] getFullDataRange() {
        boolean xlog = getState().getLogFlags()[ 0 ];
        boolean ylog = getState().getLogFlags()[ 1 ];
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        double ylo = Double.POSITIVE_INFINITY;
        double yhi = ylog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        int nok = 0;
        Points points = getPoints();
        if ( points != null ) {
            RowSubset[] rsets = getPointSelection().getSubsets();
            int nrset = rsets.length;
            int np = points.getCount();
            double[] coords = new double[ 2 ];
            for ( int ip = 0; ip < np; ip++ ) {

                /* First see if this point will be plotted. */
                boolean use = false;
                long lp = (long) ip;
                for ( int is = 0; ! use && is < nrset; is++ ) {
                    use = use || rsets[ is ].isIncluded( lp );
                }
                if ( use ) {
                    points.getCoords( ip, coords );
                    double xp = coords[ 0 ];
                    double yp = coords[ 1 ];
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
        return nok == 0 ? null : new double[] { xlo, ylo, xhi, yhi };
    }

    /**
     * This is where the work is done.
     *
     * @param   g   graphics context
     */
    private void drawData( Graphics2D g2 ) {
        Rectangle plotZone = getSurface().getClip().getBounds();
        DensityPlotState state = (DensityPlotState) getState();
        g2 = (Graphics2D) g2.create();
        if ( state.getValid() ) {
            int psize = state.getPixelSize();
            BufferedImage im = getImage( plotZone, state );
            BufferedImageOp scaleOp = 
                new AffineTransformOp( AffineTransform
                                      .getScaleInstance( psize, psize ),
                                       AffineTransformOp
                                      .TYPE_NEAREST_NEIGHBOR );
            g2.setClip( plotZone );
            g2.drawImage( im, scaleOp, plotZone.x, plotZone.y );
        }
        else {
            g2.setColor( Color.BLACK );
            g2.fillRect( plotZone.x, plotZone.y, 
                         plotZone.width, plotZone.height );
        }
    }
        
    /**
     * Returns an image representing the plot.
     * The returned image has one pixel per bin; each bin is 
     * <code>state.getPixelSize()</code> screen pixels along a side.
     *
     * @param  plotZone  the region of the screen for which the 
     *         image is destined (this, along with the plotting surface,
     *         defines the region of data space)
     * @param  state    plot state describing attributes of the plot
     * @return  image with the plot in it
     */
    private BufferedImage getImage( Rectangle plotZone,
                                    DensityPlotState state ) {

        /* Be lazy - if the data hasn't changed since last time we draw the
         * image return the same one. */
        if ( image_ == null ||
             ! plotZone.equals( lastPlotZone_ ) ||
             ! state.equals( lastState_ ) ) {

            /* Work out some dimensions. */
            int xsize = plotZone.width;
            int ysize = plotZone.height;
            int psize = state.getPixelSize();
            int xpix = ( xsize + psize - 1 ) / psize;
            int ypix = ( ysize + psize - 1 ) / psize;
            int npix = xpix * ypix;

            /* Bin the data.  This is where the points are read and the main
             * work of generating the data structure representing the
             * density image is done. */
            BinGrid[] grids = getBinnedData( plotZone, psize );
            
            /* Work out the cut levels to be used for determining pixel
             * values. */
            double loCut = state.getLoCut();
            double hiCut = state.getHiCut();

            /* Create and populate an array of 32-bit ARGB colour pixel values
             * which will provide the image. */
            Style[] styles = state.getPointSelection().getStyles();
            int nset = styles.length;
            int[] rgb = new int[ npix ];
            Arrays.fill( rgb, 0xff000000 );
            for ( int is = 0; is < grids.length; is++ ) {
                DensityStyle style = (DensityStyle) styles[ is ];
                BinGrid grid = grids[ is ];
                byte[] data = grid.getBytes( grid.getCut( loCut ),
                                             grid.getCut( hiCut ) );
                for ( int ipix = 0; ipix < npix; ipix++ ) {
                    rgb[ ipix ] = rgb[ ipix ] | style.levelBits( data[ ipix ] );
                }
            }

            /* Create an image contaning the pixels we've prepared. */
            BufferedImage image =
                new BufferedImage( xsize, ysize, BufferedImage.TYPE_INT_ARGB );
            image.setRGB( 0, 0, xpix, ypix, rgb, 0, xpix );
            image_ = image;

            /* Record the state for which this image was calculated. */
            lastPlotZone_ = plotZone;
            lastState_ = state;
        }

        /* Return the result. */
        return image_;
    }

    /**
     * Returns a bit vector describing which points in this plot's
     * PointSelection were plotted last time this component painted
     * itself.
     *
     * @return   bit vector of visible points
     */
    public BitSet getVisibleMask() {
        return visible_;
    }

    /**
     * Returns a bit vector describing which points in this plot's 
     * PointSelection, plotted last time this component painted itself, 
     * fall within a given region on the screen.
     *
     * @param  zone  shape whose insides we are interested in
     * @return  bit vector of contained points
     */
    public BitSet getContainedMask( Shape zone ) {
        BitSet mask = new BitSet();
        Points points = getPoints();
        int np = points.getCount();
        PlotSurface surface = getSurface();
        double[] coords = new double[ 2 ];
        for ( int ip = 0; ip < np; ip++ ) {
            if ( visible_.get( ip ) ) {
                points.getCoords( ip, coords );
                Point gpos = surface.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                     false );
                assert gpos != null;
                if ( zone.contains( gpos ) ) {
                    mask.set( ip );
                }
            }
        }
        return mask;    
    }

    /**
     * Prepares and returns an array of grids describing the 2-d histograms
     * which this component is to plot.
     *
     * <p>This method actually works in two slightly different ways according
     * to whether plotting is RGB or monochrome.  In the former case an
     * array of N BinGrids will be returned, where N is not greater than
     * 3 and each one represents a separate subset from the PointSelection.
     * In the latter case a 1-element array is returned, the values being
     * a sum of counts from all the subsets.
     *
     * @param   zone  region of the screen for which values are required;
     *                in conjunction with the current PlottingSurface, this
     *                also defines the region of data space
     * @param   pixsize  pixel size multiplier - bins in the returned grid(s)
     *               will be <code>pixsize</code> screen pixels along a side
     */
    private BinGrid[] getBinnedData( Rectangle zone, int pixsize ) {

        /* Work out the data space bounds of the required grid. */
        DensityPlotState state = (DensityPlotState) getState();
        boolean xflip = state.getFlipFlags()[ 0 ];
        boolean yflip = state.getFlipFlags()[ 1 ];
        boolean xlog = state.getLogFlags()[ 0 ];
        boolean ylog = state.getLogFlags()[ 1 ];
        double[] loBounds =
            getSurface().graphicsToData( xflip ? zone.x + zone.width : zone.x,
                                         yflip ? zone.y : zone.y + zone.height,
                                         false );
        double[] hiBounds =
            getSurface().graphicsToData( xflip ? zone.x : zone.x + zone.width,
                                         yflip ? zone.y + zone.height : zone.y,
                                         false );

        /* See if we already have BinGrids with the right characteristics.
         * If not, we have to calculate one. */
        if ( grids_ == null ||
             grids_.length == 0 ||
             grids_[ 0 ].getSizeX() != zone.width ||
             grids_[ 0 ].getSizeY() != zone.height ||
             ! Arrays.equals( loBounds, gridLoBounds_ ) ||
             ! Arrays.equals( hiBounds, gridHiBounds_ ) ) {
            int xsize = zone.width;
            int ysize = zone.height;
            int xpix = ( xsize + pixsize - 1 ) / pixsize;
            int ypix = ( ysize + pixsize - 1 ) / pixsize;
            double xBase = loBounds[ 0 ];
            double yBase = loBounds[ 1 ];
            double xMax = hiBounds[ 0 ];
            double yMax = hiBounds[ 1 ];
            double xBin = xsize 
                        / ( xlog ? Math.log( hiBounds[ 0 ] / loBounds[ 0 ] )
                                 : ( hiBounds[ 0 ] - loBounds[ 0 ] ) )
                        / pixsize;
            double yBin = ysize
                        / ( ylog ? Math.log( hiBounds[ 1 ] / loBounds[ 1 ] )
                                 : ( hiBounds[ 1 ] - loBounds[ 1 ] ) )
                        / pixsize;

            RowSubset[] rsets = getPointSelection().getSubsets();
            int nset = rsets.length;
            Points points = getPoints();
            int np = points.getCount();
            double[] coords = new double[ 2 ];
            BitSet visible = new BitSet();
            BinGrid[] grids;

            /* Decide if we're working in monochrome mode (sum all subset
             * counts) or RGB mode (accumulate counts for different subsets
             * into different bin grids). */
            boolean sumAll = ! state.getRgb() || nset > 3;
            if ( sumAll ) {
                grids = new BinGrid[] { new BinGrid( xpix, ypix ) };
            }
            else {
                grids = new BinGrid[ nset ];
                for ( int is = 0; is < nset; is++ ) {
                    grids[ is ] = new BinGrid( xpix, ypix );
                }
            }

            boolean[] setFlags = new boolean[ nset ];
            int nInclude = 0;
            int nVisible = 0;
            for ( int ip = 0; ip < np; ip++ ) {
                long lp = (long) ip;
                boolean use = false;
                for ( int is = 0; is < nset; is++ ) {
                    boolean inc = rsets[ is ].isIncluded( lp );
                    setFlags[ is ] = inc;
                    use = use || inc;
                }
                if ( use ) {
                    nInclude++;
                    points.getCoords( ip, coords );
                    int ix =
                        (int) ( xBin * ( xlog
                                       ? Math.log( coords[ 0 ] / loBounds[ 0 ] )
                                       : ( coords[ 0 ] - loBounds[ 0 ] ) ) );
                    int iy =
                        (int) ( yBin * ( ylog
                                       ? Math.log( coords[ 1 ] / loBounds[ 1 ] )
                                       : ( coords[ 1 ] - loBounds[ 1 ] ) ) );
                    if ( ix >= 0 && ix < xpix && iy >= 0 && iy < ypix ) {
                        visible.set( ip );
                        if ( xflip ) {
                            ix = xpix - 1 - ix;
                        }
                        if ( yflip ) {
                            iy = ypix - 1 - iy;
                        }
                        for ( int is = 0; is < nset; is++ ) {
                            if ( setFlags[ is ] ) {
                                grids[ sumAll ? 0 : is ].submitDatum( ix, iy );
                            }
                        }
                    }
                }
            }
            grids_ = grids;
            gridLoBounds_ = loBounds;
            gridHiBounds_ = hiBounds;
            visible_ = visible;

            /* Notify information about the points that were plotted.
             * I'm not sure that this has to be done outside of the paint
             * call, but it seems like the sort of thing that might be true,
             * so do it to be safe. */
            final int np1 = np;
            final int ni1 = nInclude;
            final int nv1 = visible.cardinality();
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    reportCounts( np1, ni1, nv1 );
                }
            } );
        }

        /* Return the result. */
        return grids_;
    }

    /**
     * This component calls this method following a repaint with the
     * values of the number of points that were plotted.
     * It is intended as a hook for clients which want to know that
     * information in a way which is kept up to date.
     * The default implementation does nothing.
     *
     * @param   nPoint  total number of points available
     * @param   nIncluded  number of points included in marked subsets
     * @param   nVisible  number of points actually plotted (may be less
     *          nIncluded if some are out of bounds)
     */
    protected void reportCounts( int nPoint, int nIncluded, int nVisible ) {
    }

    /**
     * Component containing the plot.
     */
    private class DensityDataPanel extends JComponent {
        DensityDataPanel() {
            setOpaque( false );
        }
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            drawData( (Graphics2D) g );
        }
    }
}
