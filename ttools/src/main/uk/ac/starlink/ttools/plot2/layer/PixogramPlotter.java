package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
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

    private static final ConfigKey<Integer> WIDTH_KEY =
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
        return new ConfigKey[] {
            StyleKeys.COLOR,
            StyleKeys.TRANSPARENCY,
            WIDTH_KEY,
        };
    }

    public PixoStyle createStyle( ConfigMap config ) {
        Color baseColor = config.get( StyleKeys.COLOR );
        double alpha = 1 - config.get( StyleKeys.TRANSPARENCY );
        float[] rgba = baseColor.getRGBComponents( new float[ 4 ] );
        rgba[ 3 ] *= alpha;
        Color color = new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        int width = config.get( WIDTH_KEY );
        return new PixoStyle( color, width );
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
                                                        style ) );
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

                /* Assume y=0 is always of interest for a histogram. */
                yRange.submit( 0 );

                /* The range in X will have been already calculated on
                 * the basis of the X values in this and any other layers
                 * by earlier stages of the auto-ranging process.
                 * We have to use this to get the bin sizes which will
                 * in turn determine the heights of the histogram bars. */
                double[] dxlimits = xRange.getFiniteBounds( xlog );
                double dxlo = dxlimits[ 0 ];
                double dxhi = dxlimits[ 1 ];

                /* To calculate the bin heights, we have to provide an Axis
                 * instance.  Unfortunately we can't know this yet - there
                 * is no information available at this stage about the
                 * width of the plot in pixels.  The maximum bar height is
                 * dependent on this, but to a first approximation it
                 * shouldn't be too sensitive, so we guess a sensible
                 * pixel extent, and hope for the best.  Use a value on
                 * the large side for pixel extent, since this will err
                 * on the side of a range that is too high (leading to
                 * unused space at the top rather than clipping the plot). */
                int gxlo = 0;
                int gxhi = GUESS_PLOT_WIDTH;
                Axis xAxis = Axis.createAxis( gxlo, gxhi, dxlo, dxhi,
                                              logFlags[ 0 ], false );
                BinArray binArray =
                    readBins( xAxis, xpad, dataSpec, dataStore );
                double[] bins = style.kernel_.convolve( binArray.bins_ );
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

        /* Smooth the count bins as required. */
        double[] bins = style.kernel_.convolve( binArray.bins_ );

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

        /* Plot a rectangle (1-pixel wide bar) for each count. */
        for ( int ix = ixlo; ix < ixhi; ix++ ) {
            int gx = binArray.getGraphicsCoord( ix );
            int dy = (int) Math.round( bins[ ix ] );
            double gy = clip( yAxis.dataToGraphics( dy ), yClipMin, yClipMax );
            double deltaY = gy - gy0;
            int h = (int) Math.round( deltaY );
            if ( h > 0 ) {
                g.drawRect( gx, gy0, 1, h );
            }
            else if ( h < 0 ) {
                g.drawRect( gx, (int) Math.round( gy ), 1, -h );
            }
        }

        /* Restore graphics context. */
        g.setColor( color0 );
    }

    /**          
     * Clips a value to a given range.
     *                
     * @param  p  input value
     * @param  lo  minimum acceptable value
     * @param  hi  maximum acceptable value
     * @return   input value clipped to given limits
     */              
    private static double clip( double p, double lo, double hi ) {
        return Math.max( Math.min( p, hi ), lo );
    }               

    /**
     * Returns an array of the bin values actually plotted.
     * This has one element per pixel and the elements are data coordinates.
     *
     * @return   array of plotted values
     */
    private static double[] getPlottedBins( PixoPlan plan, PixoStyle style ) {
        BinArray binArray = plan.binArray_;
        double[] dataBins = style.kernel_.convolve( binArray.bins_ );
        Axis xAxis = plan.xAxis_;
        double[] dlimits = xAxis.getDataLimits();
        int glo = (int) Math.round( xAxis.dataToGraphics( dlimits[ 0 ] ) );
        int ghi = (int) Math.round( xAxis.dataToGraphics( dlimits[ 1 ] ) );
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
        private final BarStyle barStyle_;
        private final Kernel kernel_;

        /**
         * Private constructor.
         *
         * @param  color  plot colour
         * @param  kernel  smoothing kernel
         */
        private PixoStyle( Color color, Kernel kernel ) {
            color_ = color;
            kernel_ = kernel;
            barStyle_ =
                new BarStyle( color, BarStyle.FORM_SPIKE, BarStyle.PLACE_OVER );
        }

        /**
         * Constructor.
         *
         * @param  color   plot colour
         * @param  width   smoothing width in pixels
         */
        public PixoStyle( Color color, int width ) {
            this( color, new SquareKernel( width ) );
        }

        public Icon getLegendIcon() {
            return barStyle_;
        }

        @Override
        public int hashCode() {
            int code = 33421;
            code = 23 * code + color_.hashCode();
            code = 23 * code + kernel_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof PixoStyle ) {
                PixoStyle other = (PixoStyle) o;
                return this.color_.equals( other.color_ )
                    && this.kernel_.equals( other.kernel_ );
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
