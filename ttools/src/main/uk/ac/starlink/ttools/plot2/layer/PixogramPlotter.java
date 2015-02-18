package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.BarStyle;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that works like a histogram with pixel-sized bins.
 * Only works with PlaneSurfaces.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2015
 */
public class PixogramPlotter implements Plotter<PixogramPlotter.PixoStyle> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord weightCoord_;
    private final SliceDataGeom pixoDataGeom_;
    private final CoordGroup pixoCoordGrp_;
    private final int icX_;
    private final int icWeight_;
    private static final int GUESS_PLOT_WIDTH = 300;

    /** Report key for plotted bin height in data coordinates. */
    public static final ReportKey<double[]> BINS_KEY =
        new ReportKey<double[]>( new ReportMeta( "bins", "Bins" ),
                                 double[].class, false );

    /** Not a fixed limit, it's just optimisation. */
    private static final int MAX_KERNEL_WIDTH = 50;

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

    /** Config key for smoothing width. */
    public static final ConfigKey<Integer> SMOOTH_KEY =
        IntegerConfigKey.createSliderKey(
            new ConfigMeta( "smooth", "Smoothing" )
           .setShortDescription( "Smoothing half-width in pixels" )
           .setXmlDescription( new String[] {
                "<p>Sets the half-width in pixels of the kernel",
                "used to smooth the histogram bins for display.",
                "</p>",
            } )
        , 3, 0, MAX_KERNEL_WIDTH, false );

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     */
    public PixogramPlotter( FloatingCoord xCoord, boolean hasWeight ) {
        xCoord_ = xCoord;
        if ( hasWeight ) {
            weightCoord_ = FloatingCoord.WEIGHT_COORD;
            pixoCoordGrp_ =
                CoordGroup
               .createPartialCoordGroup( new Coord[] { xCoord, weightCoord_ },
                                         new boolean[] { true, true } );
        }
        else {
            weightCoord_ = null;
            pixoCoordGrp_ = CoordGroup
              .createPartialCoordGroup( new Coord[] { xCoord },
                                        new boolean[] { true } );
        }
        pixoDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = pixoCoordGrp_.getExtraCoordIndex( 0, null );
        icWeight_ = hasWeight
                  ? pixoCoordGrp_.getExtraCoordIndex( 1, null )
                  : -1;
    }

    public String getPlotterName() {
        return "Pixogram";  // think of a better name?
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.PLOT_PIXOGRAM;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots smoothed frequency of data values along the",
            "horizontal axis.",
            "This is a generalisation of a histogram in which",
            "the bins are always 1 pixel wide,",
            "and a smoothing kernel",
            "(currently of a fixed rectangular form,",
            "whose width may be varied)",
            "is applied to each data point.",
            "</p>",
            "<p>It is suitable for cases where the division into discrete bins",
            "done by a normal histogram is unnecessary or troublesome.",
            "</p>",
        } );
    }

    public CoordGroup getCoordGroup() {
        return pixoCoordGrp_;
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( StyleKeys.TRANSPARENCY );
        list.add( SMOOTH_KEY );
        list.add( StyleKeys.CUMULATIVE );
        list.add( FILL_KEY );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public PixoStyle createStyle( ConfigMap config ) {
        Color baseColor = config.get( StyleKeys.COLOR );
        double alpha = 1 - config.get( StyleKeys.TRANSPARENCY );
        float[] rgba = baseColor.getRGBComponents( new float[ 4 ] );
        rgba[ 3 ] *= alpha;
        Color color = new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        boolean isFill = config.get( FILL_KEY );
        int width = config.get( SMOOTH_KEY );
        boolean isCumulative = config.get( StyleKeys.CUMULATIVE );
        Stroke stroke =
            isFill ? null
                   : StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND );
        return new PixoStyle( color, stroke, width, isCumulative );
    }

    public boolean hasReports() {
        return false;
    }

    /**
     * The supplied <code>geom</code> is ignored.
     */
    public PlotLayer createLayer( DataGeom geom, final DataSpec dataSpec,
                                  final PixoStyle style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }

        Color color = style.color_;
        final int xpad = style.kernel_.getWidth();
        final boolean isOpaque = color.getAlpha() == 255;
        LayerOpt layerOpt = new LayerOpt( color, isOpaque );
        return new AbstractPlotLayer( this, pixoDataGeom_, dataSpec,
                                      style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          final PaperType paperType ) {
                if ( ! ( surface instanceof PlaneSurface ) ) {
                    throw new IllegalArgumentException( "Not plane surface "
                                                      + surface );
                }
                final PlaneSurface pSurf = (PlaneSurface) surface;
                final Axis xAxis = pSurf.getAxes()[ 0 ];
                final boolean xflip = pSurf.getFlipFlags()[ 0 ];
                return new Drawing() {
                    public Object calculatePlan( Object[] knownPlans,
                                                 DataStore dataStore ) {
                        for ( int ip = 0; ip < knownPlans.length; ip++ ) {
                            if ( knownPlans[ ip ] instanceof PixoPlan ) {
                                PixoPlan plan = (PixoPlan) knownPlans[ ip ];
                                if ( plan.matches( xAxis, xpad, dataSpec ) ) {
                                    return plan;
                                }
                            }
                        }
                        BinArray binArray =
                            readBins( xAxis, MAX_KERNEL_WIDTH,
                                      dataSpec, dataStore );
                        return new PixoPlan( binArray, xAxis,
                                             MAX_KERNEL_WIDTH, dataSpec );
                    }
                    public void paintData( Object plan, Paper paper,
                                           DataStore dataStore ) {
                        PixoPlan pPlan = (PixoPlan) plan;
                        final BinArray binArray = pPlan.binArray_;
                        paperType.placeDecal( paper, new Decal() {
                            public void paintDecal( Graphics g ) {
                                paintBins( pSurf, binArray, style, g );
                            }
                            public boolean isOpaque() {
                                return isOpaque;
                            }
                        } );
                    }
                    public ReportMap getReport( Object plan ) {
                        ReportMap report = new ReportMap();
                        if ( plan instanceof PixoPlan ) {
                            report.set( BINS_KEY,
                                        getPlottedBins( (PixoPlan) plan,
                                                        style, xflip ) );
                        }
                        return report;
                    }
                };
            }

            /* Override this method so that we can indicate to the plot
             * the height of the bars for auto-ranging purposes. */
            @Override
            public void extendCoordinateRanges( Range[] ranges,
                                                boolean[] logFlags,
                                                DataStore dataStore ) {
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
                Axis xAxis =
                    Axis.createAxis( gxlo, gxhi, dxlo, dxhi, xlog, xflip );
                BinArray binArray =
                    readBins( xAxis, xpad, dataSpec, dataStore );
                double[] bins = getDataBins( binArray, style, xflip );
                int ixlo = binArray.getBinIndex( gxlo );
                int ixhi = binArray.getBinIndex( gxhi );
                for ( int ix = ixlo; ix < ixhi; ix++ ) {
                    yRange.submit( bins[ ix ] );
                }
            }
        };
    }

    /**
     * Returns the DataSpec coord index used for the weighting data
     * for this plotter.  If weighting is not supported, a negative
     * value is returned.
     *
     * @return   weight coord index, or -1
     */
    public int getWeightCoordIndex() {
        return icWeight_;
    }

    /**
     * Reads per-horizontal-pixel frequency data from a given data set.
     *
     * @param  xAxis  axis along which frequencies are accumulated
     * @param  padPix  number of pixels in each direction
     *                 outside of the axis range over which counts should
     *                 be gathered
     * @param  dataSpec  specification for frequency data values
     * @param  dataStore  data storage
     */
    private BinArray readBins( Axis xAxis, int padPix, DataSpec dataSpec,
                               DataStore dataStore ) {

        /* Work out the pixel limits over which we need to accumulate counts. */
        int[] glimits = xAxis.getGraphicsLimits();
        int ilo = glimits[ 0 ] - padPix;
        int ihi = glimits[ 1 ] + padPix;

        /* Accumulate the counts into a suitable results object (BinArray)
         * and return them. */
        BinArray binArray = new BinArray( ilo, ihi );

        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        if ( weightCoord_ == null | dataSpec.isCoordBlank( icWeight_ ) ) {
            while ( tseq.next() ) {
                double dx = xCoord_.readDoubleCoord( tseq, icX_ );
                double gx = xAxis.dataToGraphics( dx );
                binArray.addToBin( gx, 1 );
            }
        }
        else {
            while ( tseq.next() ) {
                double w = weightCoord_.readDoubleCoord( tseq, icWeight_ );
                if ( PlotUtil.isFinite( w ) ) {
                    double dx = xCoord_.readDoubleCoord( tseq, icX_ );
                    double gx = xAxis.dataToGraphics( dx );
                    binArray.addToBin( gx, w );
                }
            }
        }
        return binArray;
    }

    /**
     * Draws the graphical representation of a given array of counts per
     * horizontal pixel.
     *
     * @param  surface  plotting surface
     * @param  binArray   counts per X axis pixel
     * @param  style   plotting style
     * @param  g  graphics context
     */
    private void paintBins( PlaneSurface surface, BinArray binArray,
                            PixoStyle style, Graphics g ) {

        /* Store graphics context state. */
        Color color0 = g.getColor();
        g.setColor( style.color_ );

        /* Get the data values for each pixel position. */
        boolean xflip = surface.getFlipFlags()[ 0 ];
        double[] bins = getDataBins( binArray, style, xflip );

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

        /* Either plot a rectangle (1-pixel wide bar) for each count. */
        if ( style.stroke_ == null ) {
            final int nVertex;
            final int[] pxs;
            final int[] pys;
            final boolean obliqueLines = false;
            if ( obliqueLines ) {
                nVertex = np + 2;
                pxs = new int[ nVertex ];
                pys = new int[ nVertex ];
                System.arraycopy( xs, 0, pxs, 1, np );
                System.arraycopy( ys, 0, pys, 1, np );
            }
            else {
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
            pxs[ 0 ] = xs[ 0 ];
            pys[ 0 ] = gy0;
            pxs[ nVertex - 1 ] = xs[ np - 1 ] + 1;
            pys[ nVertex - 1 ] = gy0;
            g.fillPolygon( pxs, pys, nVertex );
        }

        /* Or plot a wiggly line along the top of the bars. */
        else {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            g2.setStroke( style.stroke_ );
            g2.drawPolyline( xs, ys, np );
            g2.setStroke( stroke0 );
        }

        /* Restore graphics context. */
        g.setColor( color0 );
    }

    /**
     * Returns an array of data coordinate values, one for each bin
     * accumulated by the bin array (X pixel value).
     * This is basically the bin array results, but perhaps adjusted
     * by style elements like smoothing, cumulativeness etc.
     *
     * @param   binArray  basic results
     * @param   style   style
     * @param   xflip   true iff X axis is inverted (affects cumulative output)
     * @return  output data bin values
     */
    private static double[] getDataBins( BinArray binArray, PixoStyle style,
                                         boolean xflip ) {
        Kernel kernel = style.kernel_;
        boolean isCumulative = style.isCumulative_;
        double[] bins = binArray.bins_;
        if ( kernel != null ) {
            bins = kernel.convolve( bins );
        }
        if ( isCumulative ) {
            int nb = bins.length;
            double[] cbins = new double[ nb ];
            double sum = xflip ? binArray.hiSum_ : binArray.loSum_;
            for ( int ib = 0; ib < nb; ib++ ) {
                int jb = xflip ? nb - ib - 1 : ib;
                sum += bins[ jb ];
                cbins[ jb ] = sum;
            }
            bins = cbins;
        }
        return bins;
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
     * Returns an array of the bin values actually plotted.
     * This has one element per pixel, from left to right of the plotting
     * region, and the element values are data coordinates.
     *
     * @param   plan  plan object
     * @param   style  style
     * @param   xflip  true iff X axis is inverted
     * @return   array of plotted values
     */
    private static double[] getPlottedBins( PixoPlan plan, PixoStyle style,
                                            boolean xflip ) {
        BinArray binArray = plan.binArray_;
        double[] dataBins = getDataBins( binArray, style, xflip );
        Axis xAxis = plan.xAxis_;
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
        return clipBins;
    }

    /**
     * Style subclass for pixogram plots.
     */
    public static class PixoStyle implements Style {
        private final Color color_;
        private final Stroke stroke_;
        private final Kernel kernel_;
        private final boolean isCumulative_;
        private final Icon icon_;

        /**
         * Private constructor.
         *
         * @param  color  plot colour
         * @param  stroke  line stroke, null for filled area
         * @param  kernel  smoothing kernel
         * @param  isCumulative  are bins painted cumulatively
         */
        private PixoStyle( Color color, Stroke stroke, Kernel kernel,
                           boolean isCumulative ) {
            color_ = color;
            stroke_ = stroke;
            kernel_ = kernel;
            isCumulative_ = isCumulative;
            BarStyle.Form bf =
                stroke == null ? BarStyle.FORM_FILLED : BarStyle.FORM_OPEN;
            icon_ = new BarStyle( color, bf, BarStyle.PLACE_OVER );
        }

        /**
         * Constructor.
         *
         * @param  color   plot colour
         * @param  stroke  line stroke, null for filled area
         * @param  width   smoothing width in pixels
         * @param  isCumulative  are bins painted cumulatively
         */
        public PixoStyle( Color color, Stroke stroke, int width,
                          boolean isCumulative ) {
            this( color, stroke, new SquareKernel( width ), isCumulative );
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
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof PixoStyle ) {
                PixoStyle other = (PixoStyle) o;
                return this.color_.equals( other.color_ )
                    && PlotUtil.equals( this.stroke_, other.stroke_ )
                    && this.kernel_.equals( other.kernel_ )
                    && this.isCumulative_ == other.isCumulative_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Defines the smoothing function used for pixel bins.
     */
    @Equality
    private interface Kernel {

        /**
         * Returns the half-width in pixels over which a value at a given
         * point will be smoothed.
         *
         * @return  smoothing width in pixels
         */
        int getWidth();

        /**
         * Applies this kernel to a data array.
         * Note, the values within <code>getWidth</code> pixels of the
         * start and end of the returned data array will be distorted,
         * so this method should be called on an input array with
         * sufficient padding at either end that this effect can be ignored.
         *
         * @param  data  input data array
         * @return  output data array, same dimensions as input,
         *          but containing convolved data
         */
        double[] convolve( double[] data );
    }

    /**
     * Kernel implementation using a rectangular function.
     */
    private static class SquareKernel implements Kernel {
        final int width_;

        /**
         * Constructor.
         *
         * @param  width  half-width in pixels
         */
        public SquareKernel( int width ) {
            width_ = width;
        }

        public int getWidth() {
            return width_;
        }

        public double[] convolve( double[] in ) {
            int ns = in.length;
            double[] out = new double[ ns ];
            for ( int is = width_; is < ns - width_; is++ ) {
                for ( int ik = -width_; ik <= width_; ik++ ) {
                    out[ is ] += in[ is + ik ];
                }
            }
            double scale = 1.0 / ( width_ * 2 + 1 );
            for ( int is = 0; is < ns; is++ ) {
                out[ is ] *= scale;
            }
            return out;
        }

        @Override
        public int hashCode() {
            int code = 88234;
            code = 23 * code + width_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SquareKernel ) {
                SquareKernel other = (SquareKernel) o;
                return this.width_ == other.width_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Plot plan implementation for this class.
     */
    private static class PixoPlan {
        final BinArray binArray_;
        final Axis xAxis_;
        final int xpad_;
        final DataSpec dataSpec_;

        /**
         * Constructor.
         *
         * @param  binArray   frequency data
         * @param  xAxis  axis over which counts are accumulated
         * @param  xpad   number of pixels outside axis range in each direction
         *                that are stored in <code>binArray</code>
         * @param  dataSpec   count data specificatin
         */
        PixoPlan( BinArray binArray, Axis xAxis, int xpad, DataSpec dataSpec ) {
            binArray_ = binArray;
            xAxis_ = xAxis;
            xpad_ = xpad;
            dataSpec_ = dataSpec;
        }

        /**
         * Indicates whether this plan is suitable for use given certain
         * plotting requirements.
         *
         * @param  xAxis  axis over which counts are accumulated
         * @param  xpad   minimum number of pixels outside axis range
         *                in each direction that must be stored in the bin array
         * @param  dataSpec   count data specificatin
         */
        boolean matches( Axis xAxis, int xpad, DataSpec dataSpec ) {
            return xAxis_.equals( xAxis )
                && dataSpec_.equals( dataSpec )
                && xpad_ >= xpad;
        }
    }

    /**
     * Data object storing counts per pixel.
     */
    private static class BinArray {

        final double[] bins_;
        final int glo_;
        final int ghi_;
        double loSum_;
        double hiSum_;
        double midSum_;

        /**
         * Constructor.
         *
         * @param  glo  lowest pixel index required
         * @param  ghi  1+highest pixel index required
         */
        BinArray( int glo, int ghi ) {
            glo_ = glo;
            ghi_ = ghi;
            bins_ = new double[ ghi - glo ];
        }

        /**
         * Increments the value in the bin corresponding to a given pixel index
         * by a given amount.  If the target pixel index is out of bounds
         * for this array, there is no effect.  No checking is performed
         * on the <code>inc</code> value.
         *
         * @param   gx  target pixel index
         * @param   inc  increment amount
         */
        void addToBin( double gx, double inc ) {
            double dx = Math.round( gx - glo_ );
            if ( dx >= 0 && dx < bins_.length ) {
                bins_[ (int) dx ] += inc;
                midSum_ += inc;
            }
            else if ( dx < 0 ) {
                loSum_ += inc;
            }
            else if ( dx >= bins_.length ) {
                hiSum_ += inc;
            }
        }

        /**
         * Returns the bin index
         * (index into this object's <code>bins_</code> array)
         * for a given pixel index.
         *
         * @param  gx  pixel index
         * @return  bin index
         */
        int getBinIndex( int gx ) {
            return gx - glo_;
        }

        /**
         * Returns the pixel index for a given bin index.
         *
         * @param  index  bin index
         * @return   pixel index
         */
        int getGraphicsCoord( int index ) {
            return index + glo_;
        }
    }
}
