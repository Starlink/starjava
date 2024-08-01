package uk.ac.starlink.ttools.plot;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComponent;

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
    private int nPotential_;
    private int nIncluded_;
    private int nVisible_;
    private PlotData lastData_;
    private BufferedImage image_;
    private Rectangle lastPlotZone_;
    private DensityPlotState lastState_;
    private double[] loCuts_;
    private double[] hiCuts_;
    private DensityStyle[] styles_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot" );

    /**
     * Constructor.
     *
     * @param  surface  plotting surface to receive pixels
     */
    @SuppressWarnings("this-escape")
    public DensityPlot( PlotSurface surface ) {
        super();
        setPreferredSize( new Dimension( 400, 400 ) );
        add( new DensityDataPanel() );
        surface.getComponent().setOpaque( false );
        setSurface( surface );
    }

    public void setState( PlotState state ) {
        super.setState( state );
        PlotData data = state.getPlotData();
        if ( ! data.equals( lastData_ ) ) {
            grids_ = null;
            image_ = null;
            lastData_ = data;
        }
    }

    /**
     * This is where the work is done.
     *
     * @param   g   graphics context
     */
    private void drawData( Graphics2D g2 ) {
        Rectangle plotZone = getSurface().getClip().getBounds();
        if ( plotZone.isEmpty() ) {
            return;
        }
        DensityPlotState state = (DensityPlotState) getState();
        final double[] loCuts;
        final double[] hiCuts;
        final DensityStyle[] styles;
        Shape clip = g2.getClip();
        Color color = g2.getColor();
        if ( state != null && state.getValid() ) {
            int psize = state.getPixelSize();
            BufferedImage im = getImage( plotZone, state );
            BufferedImageOp scaleOp = 
                new AffineTransformOp( AffineTransform
                                      .getScaleInstance( psize, psize ),
                                       AffineTransformOp
                                      .TYPE_NEAREST_NEIGHBOR );
            g2.setClip( plotZone );

            /* There was a problem with OutOfMemoryErrors for large pixel
             * sizes here.  I'm reasonably sure I've fixed it, but if it
             * rears its head again, see earlier versions of this code
             * (May 2010). */
            g2.drawImage( im, scaleOp, plotZone.x, plotZone.y );
            loCuts = loCuts_;
            hiCuts = hiCuts_;
            styles = styles_;
        }
        else {
            g2.setColor( Color.BLACK );
            g2.fillRect( plotZone.x, plotZone.y, 
                         plotZone.width, plotZone.height );
            loCuts = null;
            hiCuts = null;
            styles = null;
        }
        g2.setClip( clip );
        g2.setColor( color );
        firePlotChangedLater( new DensityPlotEvent( this, state, nPotential_,
                                                    nIncluded_, nVisible_,
                                                    loCuts, hiCuts ) );
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
            boolean weighted = state.getWeighted();

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

            /* Ensure that we have no more than one grid to plot for each
             * style (i.e. each colour channel). */
            PlotData data = state.getPlotData();
            int nset = data.getSetCount();
            Style[] styles = new Style[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                styles[ is ] = data.getSetStyle( is );
            }
            if ( grids.length > 1 ) {
                grids = grids.clone();
                styles = styles.clone();
                foldGrids( grids, styles );
            }

            /* Create and populate an array of 32-bit ARGB colour pixel values
             * which will provide the image. */
            int[] rgb = new int[ npix ];
            Arrays.fill( rgb, 0xff000000 );
            List<double[]> cutList = new ArrayList<double[]>();
            if ( styles.length > 0 ) {
                for ( int is = 0; is < grids.length; is++ ) {
                    DensityStyle style = (DensityStyle) styles[ is ];
                    BinGrid grid = grids[ is ];
                    assert ( grid == null ) == ( style == null );
                    if ( grid != null ) {
                        double lo = grid.getCut( loCut );
                        double hi = grid.getCut( hiCut );
                        if ( lo == 1 && ! weighted ) {
                            lo = 0;
                        }
                        cutList.add( new double[] { lo, hi } );
                        byte[] bdata = grid.getBytes( lo, hi, state.getLogZ() );
                        for ( int ipix = 0; ipix < npix; ipix++ ) {
                            rgb[ ipix ] =
                               rgb[ ipix ] | style.levelBits( bdata[ ipix ] );
                        }
                    }
                }
            }
            loCuts_ = new double[ cutList.size() ];
            hiCuts_ = new double[ cutList.size() ];
            styles_ = new DensityStyle[ cutList.size() ];
            for ( int i = 0; i < cutList.size(); i++ ) {
                double[] cuts = cutList.get( i );
                loCuts_[ i ] = cuts[ 0 ];
                hiCuts_[ i ] = cuts[ 1 ];
                styles_[ i ] = (DensityStyle) styles[ i ];
            }

            /* Create an image contaning the pixels we've prepared. */
            BufferedImage image =
                new BufferedImage( xpix, ypix, BufferedImage.TYPE_INT_ARGB );
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
     * Takes an array of grids and corresponding styles and arranges it
     * so that if any of the styles are the same (represent the same
     * colour channel) then they are combined by summing their count arrays.
     * On output the elements of the <code>grids</code> and <code>styles</code>
     * arrays may be replaced by new values; these arrays may end up 
     * with fewer elements than on input.  In this case, some elements will
     * contain nulls on exit.
     *
     * @param   grids   input array of data grids (modified on exit)
     * @param   styles  input array of plotting DensityStyles corresponding to 
     *          <code>grids</code> (modified on exit)
     */
    private void foldGrids( BinGrid[] grids, Style[] styles ) {
        int ngrid = grids.length;
        if ( styles.length != ngrid ) {
            throw new IllegalArgumentException();
        }
        BinGrid[] rgbGrids = new BinGrid[ 3 ];
        DensityStyle[] rgbStyles = new DensityStyle[ 3 ];
        List<DensityStyle> seenStyles = new ArrayList<DensityStyle>();
        for ( int is = 0; is < ngrid; is++ ) {
            int iseen = seenStyles.indexOf( styles[ is ] );
            if ( iseen < 0 ) {
                DensityStyle style = (DensityStyle) styles[ is ];
                iseen = seenStyles.size();
                seenStyles.add( style );
                rgbGrids[ iseen ] = grids[ is ];
                rgbStyles[ iseen ] = style;
            }
            else {
                double[] c1 = grids[ is ].getSums();
                double[] c0 = rgbGrids[ iseen ].getSums();
                int npix = c0.length;
                assert npix == c1.length;
                for ( int i = 0; i < npix; i++ ) {
                    c0[ i ] += c1[ i ];
                }
                rgbGrids[ iseen ].recalculate();
            }
        }
        Arrays.fill( grids, null );
        Arrays.fill( styles, null );
        for ( int irgb = 0; irgb < seenStyles.size(); irgb++ ) {
            grids[ irgb ] = rgbGrids[ irgb ];
            styles[ irgb ] = rgbStyles[ irgb ];
        }
    }

    /**
     * Returns an iterator over the points plotted last time this component
     * plotted itself.
     * 
     * @return  point iterator
     */
    public PointIterator getPlottedPointIterator() {
        return new PlotDataPointIterator( getState().getPlotData(),
                                          getPointPlacer() );
    }

    /**
     * Returns a point placer suitable for this plot.
     * 
     * @return  point placer
     */
    public PointPlacer getPointPlacer() {
        final PlotSurface surface = getSurface();
        return new PointPlacer() {
            public Point getXY( double[] coords ) {
                return surface.dataToGraphics( coords[ 0 ], coords[ 1 ], true );
            }
        };
    }

    /**
     * Prepares and returns an array of grids describing the 2-d histograms
     * which this component is to plot.
     *
     * <p>This method actually works in two slightly different ways according
     * to whether plotting is RGB or monochrome.  In the former case an
     * array of N BinGrids will be returned, one for each subset from 
     * the PlotData.
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
        int xsize = zone.width;
        int ysize = zone.height;
        int xpix = ( xsize + pixsize - 1 ) / pixsize;
        int ypix = ( ysize + pixsize - 1 ) / pixsize;
        if ( grids_ == null ||
             grids_.length == 0 ||
             grids_[ 0 ].getSizeX() != xpix ||
             grids_[ 0 ].getSizeY() != ypix ||
             ! Arrays.equals( loBounds, gridLoBounds_ ) ||
             ! Arrays.equals( hiBounds, gridHiBounds_ ) ) {
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

            PlotData data = state.getPlotData();
            int nset = data.getSetCount();
            BinGrid[] grids;

            /* Decide if we're working in monochrome mode (sum all subset
             * counts) or RGB mode (accumulate counts for different subsets
             * into different bin grids). */
            boolean sumAll = ! state.getRgb();
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
            PointSequence pseq = data.getPointSequence();
            int ip = 0;
            for ( ; pseq.next(); ip++ ) {
                boolean use = false;
                for ( int is = 0; is < nset; is++ ) {
                    boolean inc = pseq.isIncluded( is );
                    setFlags[ is ] = inc;
                    use = use || inc;
                }
                if ( use ) {
                    double[] coords = pseq.getPoint();
                    if ( ! Double.isNaN( coords[ 0 ] ) &&
                         ! Double.isNaN( coords[ 1 ] ) &&
                         ! Double.isNaN( coords[ 2 ] ) &&
                         ! Double.isInfinite( coords[ 0 ] ) &&
                         ! Double.isInfinite( coords[ 1 ] ) &&
                         ! Double.isInfinite( coords[ 2 ] ) &&
                         ! ( xlog && coords[ 0 ] <= 0.0 ) &&
                         ! ( ylog && coords[ 1 ] <= 0.0 ) ) {
                        nInclude++;
                        int ix = (int) Math.floor( xBin *
                            ( xlog ? Math.log( coords[ 0 ] / loBounds[ 0 ] )
                                   : ( coords[ 0 ] - loBounds[ 0 ] ) ) );
                        int iy = (int) Math.floor( yBin *
                            ( ylog ? Math.log( coords[ 1 ] / loBounds[ 1 ] )
                                   : ( coords[ 1 ] - loBounds[ 1 ] ) ) );
                        if ( ix >= 0 && ix < xpix && iy >= 0 && iy < ypix ) {
                            nVisible++;
                            if ( xflip ) {
                                ix = xpix - 1 - ix;
                            }
                            if ( yflip ) {
                                iy = ypix - 1 - iy;
                            }
                            for ( int is = 0; is < nset; is++ ) {
                                if ( setFlags[ is ] ) {
                                    grids[ sumAll ? 0 : is ]
                                        .submitDatum( ix, iy, coords[ 2 ] );
                                }
                            }
                        }
                    }
                }
            }
            pseq.close();
            grids_ = grids;
            gridLoBounds_ = loBounds;
            gridHiBounds_ = hiBounds;
            nPotential_ = ip;
            nIncluded_ = nInclude;
            nVisible_ = nVisible;
        }

        /* Return the result. */
        return grids_;
    }

    /**
     * Returns the binned grid(s) corresponding to the image currently
     * displayed by this component.
     *
     * @return  binned data object(s)
     */
    public BinGrid[] getBinnedData() {
        return grids_;
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

        protected void printComponent( Graphics g ) {

            /* Tweak so that exact positioning of lines between pixel and
             * graphics plotting doesn't look wrong.  Possibly this is only
             * required for (a bug in) org.jibble.epsgraphics.EpsGraphics2D? */
            if ( isVectorContext( g ) ) {
                Rectangle clip = getSurface().getClip().getBounds();
                int cx = clip.x - 2;
                int cy = clip.y - 2;
                int cw = clip.width + 4;
                int ch = clip.height + 4;
                g.clearRect( cx, cy, cw, ch );
            }
            super.printComponent( g );
        }
    }
}
