package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;

/**
 * Plotter that works like a histogram with pixel-sized bins.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2015
 */
public class KernelDensityPlotter
        extends Pixel1dPlotter<KernelDensityPlotter.KDenseStyle> {

    /** Report key for plotted bin height in data coordinates. */
    public static final ReportKey<double[]> BINS_KEY =
        new ReportKey<double[]>( new ReportMeta( "bins", "Bins" ),
                                 double[].class, false );

    /** Config key for line/fill toggle. */
    public static final ConfigKey<Boolean> FILL_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "fill", "Fill" )
           .setShortDescription( "Fill bar area with solid colour?" )
           .setXmlDescription( new String[] {
                "<p>If true, the bars of the histogram will be plotted",
                "as a solid colour, so that the whole area under the curve",
                "is filled in.",
                "If false, only the top outline of the area will be drawn.",
                "</p>"
            } )
        , false );

    /** Config key for line thickness (only effective if fill==false). */
    public static final ConfigKey<Integer> THICK_KEY =
        StyleKeys.createThicknessKey( 2 );

    private static final int GUESS_PLOT_WIDTH = 300;

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     */
    public KernelDensityPlotter( FloatingCoord xCoord, boolean hasWeight ) {
        super( xCoord, hasWeight, "KDE", ResourceIcon.PLOT_KDE );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a Kernel Density Estimate",
            "giving a smoothed frequency of data values along the",
            "horizontal axis.",
            "This is a generalisation of a histogram in which",
            "the bins are always 1 pixel wide,",
            "and a smoothing kernel,",
            "whose width and shape may be varied,",
            "is applied to each data point.",
            "</p>",
            "<p>This is suitable for cases where",
            "the division into discrete bins",
            "done by a normal histogram is unnecessary or troublesome.",
            "</p>",
            "<p>Since the plotted output is quantised to the pixel level",
            "it's not a true Kernel Density Estimation,",
            "but at least on a bitmapped display it is indistinguishable",
            "from one.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.TRANSPARENCY );
        list.add( SMOOTH_KEY );
        list.add( KERNEL_KEY );
        list.add( StyleKeys.CUMULATIVE );
        list.add( StyleKeys.NORMALISE );
        list.add( FILL_KEY );
        list.add( THICK_KEY );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public KDenseStyle createStyle( ConfigMap config ) {
        Color baseColor = config.get( StyleKeys.COLOR );
        double alpha = 1 - config.get( StyleKeys.TRANSPARENCY );
        float[] rgba = baseColor.getRGBComponents( new float[ 4 ] );
        rgba[ 3 ] *= alpha;
        Color color = new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        boolean isFill = config.get( FILL_KEY );
        double width = config.get( SMOOTH_KEY );
        Kernel1dShape kernelShape = config.get( KERNEL_KEY );
        Kernel1d kernel = kernelShape.createKernel( width );
        boolean isCumulative = config.get( StyleKeys.CUMULATIVE );
        Normalisation norm = config.get( StyleKeys.NORMALISE );
        Stroke stroke =
            isFill ? null
                   : new BasicStroke( config.get( THICK_KEY ),
                                      BasicStroke.CAP_ROUND,
                                      BasicStroke.JOIN_ROUND );
        return new KDenseStyle( color, stroke, kernel, isCumulative, norm );
    }

    protected LayerOpt getLayerOpt( KDenseStyle style ) {
        Color color = style.color_;
        boolean isOpaque = color.getAlpha() == 255;
        return new LayerOpt( color, isOpaque );
    }

    protected int getPixelPadding( KDenseStyle style ) {
        return getEffectiveExtent( style.kernel_ );
    }

    protected void paintBins( PlaneSurface surface, BinArray binArray,
                              KDenseStyle style, Graphics2D g ) {
        /* Store graphics context state. */
        Color color0 = g.getColor();
        g.setColor( style.color_ );

        /* Get the data values for each pixel position. */
        Axis xAxis = surface.getAxes()[ 0 ];
        Kernel1d kernel = style.kernel_;
        double[] bins = getDataBins( binArray, xAxis, kernel,
                                     style.norm_, style.isCumulative_ );
    
        /* Work out the Y axis base of the bars in graphics coordinates. */
        Axis yAxis = surface.getAxes()[ 1 ];
        boolean yLog = surface.getLogFlags()[ 1 ];
        boolean yFlip = surface.getFlipFlags()[ 1 ];
        int gy0;
        if ( yLog ) {
            double[] dyLimits = surface.getDataLimits()[ 1 ];
            double dy0 = yAxis.dataToGraphics( dyLimits[ 0 ] );
            gy0 = (int) ( yFlip ? dy0 - 2 : dy0 + 2 );
        }
        else {
            gy0 = (int) yAxis.dataToGraphics( 0 );
        }
        Rectangle clip = surface.getPlotBounds();
        int yClipMin = clip.y - 64;
        int yClipMax = clip.y + clip.height + 64;
        gy0 = (int) clip( gy0, yClipMin, yClipMax );
                            
        /* Work out the range of bin indices that need to be painted. */
        int ixlo = binArray.getBinIndex( clip.x );
        int ixhi = binArray.getBinIndex( clip.x + clip.width );

        /* Assemble a list of the (x,y) graphics coordinates of the
         * top left hand corner for each bar in the plot region. */
        int np = ixhi - ixlo;
        int[] xs = new int[ np ];
        int[] ys = new int[ np ];
        for ( int ip = 0; ip < np; ip++ ) {
            int ix = ixlo + ip;
            double dy = yAxis.dataToGraphics( bins[ ix ] );
            xs[ ip ] = binArray.getGraphicsCoord( ix );
            ys[ ip ] = PlotUtil.isFinite( dy )
                     ? clip( dy, yClipMin, yClipMax )
                     : gy0;
        }

        /* Determine whether to accentuate or play down the jaggedness
         * of the pixel quantisation of the convolved function. */
        boolean squareLines = kernel.isSquare() || kernel.getExtent() <= 1;

        /* Either plot a rectangle (1-pixel wide bar) for each count. */
        if ( style.stroke_ == null ) {
            final int nVertex;
            final int[] pxs;
            final int[] pys;
            if ( squareLines ) {
                nVertex = np * 2 + 2;
                pxs = new int[ nVertex ];
                pys = new int[ nVertex ];
                for ( int ip = 0; ip < np; ip++ ) {
                    pxs[ ip * 2 + 1 ] = xs[ ip ];
                    pys[ ip * 2 + 1 ] = ys[ ip ];
                    pxs[ ip * 2 + 2 ] = xs[ ip ] + 1;
                    pys[ ip * 2 + 2 ] = ys[ ip ];
                }
            }
            else {
                nVertex = np + 2;
                pxs = new int[ nVertex ];
                pys = new int[ nVertex ];
                System.arraycopy( xs, 0, pxs, 1, np );
                System.arraycopy( ys, 0, pys, 1, np );
            }
            pxs[ 0 ] = xs[ 0 ];
            pys[ 0 ] = gy0;
            pxs[ nVertex - 1 ] = xs[ np - 1 ] + 1;
            pys[ nVertex - 1 ] = gy0;
            g.fillPolygon( pxs, pys, nVertex );
        }

        /* Or plot a wiggly line along the top of the bars. */
        else {
            final int nVertex;
            final int[] pxs;
            final int[] pys;
            if ( squareLines ) {
                nVertex = np * 2;
                pxs = new int[ nVertex ];
                pys = new int[ nVertex ];
                for ( int ip = 0; ip < np; ip++ ) {
                    pxs[ ip * 2 ] = xs[ ip ];
                    pys[ ip * 2 ] = ys[ ip ];
                    pxs[ ip * 2 + 1 ] = xs[ ip ] + 1;
                    pys[ ip * 2 + 1 ] = ys[ ip ];
                }
            }
            else {
                nVertex = np;
                pxs = xs;
                pys = ys;
            }
            Stroke stroke0 = g.getStroke();
            g.setStroke( style.stroke_ );
            g.drawPolyline( pxs, pys, nVertex );
            g.setStroke( stroke0 );
        }

        /* Restore graphics context. */
        g.setColor( color0 );
    }

    protected void extendPixel1dCoordinateRanges( Range[] ranges,
                                                  boolean[] logFlags,
                                                  KDenseStyle style,
                                                  DataSpec dataSpec,
                                                  DataStore dataStore ) {

        /* Calculate the height of the bars for auto-ranging purposes. */
        Range xRange = ranges[ 0 ];
        Range yRange = ranges[ 1 ];
        boolean xlog = logFlags[ 0 ];
        boolean ylog = logFlags[ 1 ];

        /* Assume y=0 is always of interest for a histogram. */
        yRange.submit( ylog ? 1 : 0 );

        /* To calculate the bin heights, we have to provide an Axis
         * instance.  We know the data limits of this from previous
         * ranging, but unfortunately there is no information
         * available at this stage about the width of the plot
         * in pixels.  The maximum bar height is
         * dependent on this, but to a first approximation it
         * shouldn't be too sensitive, so we guess a sensible
         * pixel extent, and hope for the best.  Use a value on
         * the large side for pixel extent, since this will err
         * on the side of a range that is too high (leading to
         * unused space at the top rather than clipping the plot). */
        double[] dxlimits = xRange.getFiniteBounds( xlog );
        double dxlo = dxlimits[ 0 ];
        double dxhi = dxlimits[ 1 ];
        int gxlo = 0;
        int gxhi = GUESS_PLOT_WIDTH;
        boolean xflip = false;
        Axis xAxis = Axis.createAxis( gxlo, gxhi, dxlo, dxhi, xlog, xflip );
        int xpad = getEffectiveExtent( style.kernel_ );
        BinArray binArray = readBins( xAxis, xpad, dataSpec, dataStore );
        double[] bins = getDataBins( binArray, xAxis, style.kernel_,
                                     style.norm_, style.isCumulative_ );
        int ixlo = binArray.getBinIndex( gxlo );
        int ixhi = binArray.getBinIndex( gxhi );
        for ( int ix = ixlo; ix < ixhi; ix++ ) {
            yRange.submit( bins[ ix ] );
        }
    }

    protected ReportMap getPixel1dReport( Pixel1dPlan plan,
                                          KDenseStyle style ) {
        BinArray binArray = plan.binArray_;
        Axis xAxis = plan.xAxis_;
        double[] dataBins = getDataBins( binArray, xAxis, style.kernel_,
                                         style.norm_, style.isCumulative_ );
        double[] dlimits = xAxis.getDataLimits();
        int glo = (int) Math.round( xAxis.dataToGraphics( dlimits[ 0 ] ) );
        int ghi = (int) Math.round( xAxis.dataToGraphics( dlimits[ 1 ] ) );
        if ( glo > ghi ) {
            int gt = glo;              
            glo = ghi;
            ghi = gt;
        }
        int ixlo = binArray.getBinIndex( glo );
        int nx = ghi - glo;                       
        double[] clipBins = new double[ nx ];
        System.arraycopy( dataBins, ixlo, clipBins, 0, nx );
        ReportMap report = new ReportMap();
        report.set( BINS_KEY, clipBins );
        return report;
    }

    /**
     * Clips a value to a given range.
     *
     * @param  p  input value
     * @param  lo  minimum acceptable value
     * @param  hi  maximum acceptable value
     * @return   input value clipped to given limits
     */
    private static int clip( double p, int lo, int hi ) {
        if ( Double.isNaN( p ) ) {
            return lo;
        }
        else if ( p < lo ) {
            return lo;
        }
        else if ( p > hi ) {
            return hi;
        }
        else {
            return (int) Math.round( p );
        }
    }

    /**
     * Style subclass for kernel density plots.
     */
    public static class KDenseStyle implements Style {
        private final Color color_;
        private final Stroke stroke_;
        private final Kernel1d kernel_;
        private final boolean isCumulative_;
        private final Normalisation norm_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  color  plot colour
         * @param  stroke  line stroke, null for filled area
         * @param  kernel  smoothing kernel
         * @param  isCumulative  are bins painted cumulatively
         * @param  norm   normalisation mode
         */
        public KDenseStyle( Color color, Stroke stroke, Kernel1d kernel,
                            boolean isCumulative, Normalisation norm ) {
            color_ = color;
            stroke_ = stroke;
            kernel_ = kernel;
            isCumulative_ = isCumulative;
            norm_ = norm;
            BarStyle.Form bf =
                stroke == null ? BarStyle.FORM_FILLED : BarStyle.FORM_OPEN;
            icon_ = new BarStyle( color, bf, BarStyle.PLACE_OVER );
        }

        /**
         * Returns cumulative flag.
         *
         * @return  true iff counts are cumulative
         */
        public boolean isCumulative() {
            return isCumulative_;
        }

        /**
         * Returns normalisation mode.
         *
         * @return  normalisation mode for count axis
         */
        public Normalisation getNormalisation() {
            return norm_;
        }

        public Icon getLegendIcon() {
            return icon_;
        }

        @Override
        public int hashCode() {
            int code = 33421;
            code = 23 * code + color_.hashCode();
            code = 23 * code + PlotUtil.hashCode( stroke_ );
            code = 23 * code + kernel_.hashCode();
            code = 23 * code + ( isCumulative_ ? 11 : 13 );
            code = 23 * code + PlotUtil.hashCode( norm_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof KDenseStyle ) {
                KDenseStyle other = (KDenseStyle) o;
                return this.color_.equals( other.color_ )
                    && PlotUtil.equals( this.stroke_, other.stroke_ )
                    && this.kernel_.equals( other.kernel_ )
                    && this.isCumulative_ == other.isCumulative_
                    && PlotUtil.equals( this.norm_, other.norm_ );
            }
            else {
                return false;
            }
        }
    }
}
