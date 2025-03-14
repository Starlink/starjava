package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.util.SplitCollector;

/**
 * Abstract superclass for histogram-like plotters that have pixel-sized
 * bins with optional smoothing.
 * Only works with PlanarSurfaces.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2015
 */
public abstract class Pixel1dPlotter<S extends Style> implements Plotter<S> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord weightCoord_;
    private final ConfigKey<Combiner> combinerKey_;
    private final String name_;
    private final Icon icon_;
    private final CoordGroup pixoCoordGrp_;
    private final int icX_;
    private final int icWeight_;

    /** Not a fixed limit, it's just optimisation. */
    private static final int MAX_KERNEL_WIDTH = 50;
    private static final int MAX_KERNEL_EXTENT = 150;

    /** Report key for smoothing width. */
    public static final ReportKey<Double> SMOOTHWIDTH_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "smoothwidth",
                                                   "Smoothing Width" ),
                                   false );

    /** Config key for smoothing width configuration. */
    public static final ConfigKey<BinSizer> SMOOTHSIZER_KEY =
        BinSizer.createSizerConfigKey(
            new ConfigMeta( "smooth", "Smoothing" )
           .setStringUsage( "+<width>|-<count>" )
           .setShortDescription( "Smoothing width specification" )
           .setXmlDescription( new String[] {
                "<p>Configures the smoothing width for kernel density",
                "estimation.",
                "This is the characteristic width of the kernel function",
                "to be convolved with the density to produce the visible plot.",
                "</p>",
                BinSizer.getConfigKeyDescription(),
            } )
        , SMOOTHWIDTH_KEY, 100, true );

    /** Config key for smoothing kernel shape. */
    public static final ConfigKey<Kernel1dShape> KERNEL_KEY =
        new OptionConfigKey<Kernel1dShape>(
            new ConfigMeta( "kernel", "Kernel" )
           .setShortDescription( "Smoothing kernel functional form" )
           .setXmlDescription( new String[] {
                "<p>The functional form of the smoothing kernel.",
                "The functions listed refer to the unscaled shape;",
                "all kernels are normalised to give a total area of unity.",
                "</p>",
            } )
        , Kernel1dShape.class,
        StandardKernel1dShape.getStandardOptions(),
        StandardKernel1dShape.EPANECHNIKOV ) {
            public String getXmlDescription( Kernel1dShape kshape ) {
                return kshape.getDescription();
            }
        }
       .setOptionUsage()
       .addOptionsXml();

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     * @param   unitKey  config key to select X axis physical units,
     *                   or null if no unit selection required
     * @param   name  plotter name
     * @param   icon  plotter icon
     */
    protected Pixel1dPlotter( FloatingCoord xCoord, boolean hasWeight,
                              ConfigKey<Unit> unitKey,
                              String name, Icon icon ) {
        xCoord_ = xCoord;
        name_ = name;
        icon_ = icon;
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
        combinerKey_ = createCombinerKey( weightCoord_, unitKey );

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = pixoCoordGrp_.getExtraCoordIndex( 0, null );
        icWeight_ = hasWeight
                  ? pixoCoordGrp_.getExtraCoordIndex( 1, null )
                  : -1;
    }

    public String getPlotterName() {
        return name_;
    }

    public Icon getPlotterIcon() {
        return icon_;
    }

    public CoordGroup getCoordGroup() {
        return pixoCoordGrp_;
    }

    public boolean hasReports() {
        return false;
    }

    /**
     * Returns an XML description snippet (zero or more P elements)
     * discussing use of weighted coordinates for this plotter.
     *
     * @return  text suitable for inclusion in getPlotterDescription
     *          return value
     */
    protected String getWeightingDescription() {
        if ( icWeight_ >= 0 ) {
            return PlotUtil.concatLines( new String[] {
                "<p>A weighting may be applied to the calculated levels",
                "by supplying the",
                "<code>" + weightCoord_.getInput().getMeta().getShortName()
                         + "</code>",
                "coordinate.",
                "In this case you can choose how these weights are aggregated",
                "in each pixel bin using the",
                "<code>" + combinerKey_.getMeta().getShortName() + "</code>",
                "parameter.",
                "The result is something like a smoothed version of the",
                "corresponding weighted histogram.",
                "Note that some combinations of the available parameters",
                "(e.g. a normalised cumulative median-aggregated KDE)",
                "may not make much visual sense.",
                "</p>",
            } );
        }
        else {
            return "";
        }
    }

    /**
     * Returns the sideways flag implied by a given style.
     *
     * @param   style  plot style
     * @return   if true, plot is sideways
     */
    protected abstract boolean isY( S style );

    /**
     * Returns the LayerOpt suitable for a given style for this plotter.
     *
     * @param  style  plot style
     * @return   layer option flags
     */
    protected abstract LayerOpt getLayerOpt( S style );

    /**
     * Returns the padding required at both ends of the array of
     * pixel bins for calculations.
     *
     * @param  style  plotting style
     * @param  surf   plotting surface
     * @return   padding in pixels required in bin array
     */
    protected abstract int getPixelPadding( S style, PlanarSurface surf );

    /**
     * Returns the bin aggregation mode implied by a given style.
     *
     * @param  style  plotting style
     * @return  pixel bin aggregation mode
     */
    protected abstract Combiner getCombiner( S style );

    /** 
     * Draws the graphical representation of a given array of counts per
     * horizontal pixel.
     *  
     * @param  surface  plotting surface 
     * @param  binArray   counts per X axis pixel
     * @param  style   plotting style
     * @param  g  graphics context
     */                                      
    protected abstract void paintBins( PlanarSurface surface, BinArray binArray,
                                       S style, Graphics2D g );

    /**
     * Performs any required range extension.  May be a no-op.
     *
     * @param  ranges   array of data space dimension ranges to update
     * @param  logFlags  array of linear/log flags corresponding to ranges
     * @param  style   plotting style
     * @param  dataSpec  data specification
     * @param  dataStore  data storage object
     * @see    uk.ac.starlink.ttools.plot2.PlotLayer#extendCoordinateRanges
     */
    protected abstract void
        extendPixel1dCoordinateRanges( Range[] ranges, boolean[] logFlags,
                                       S style, DataSpec dataSpec,
                                       DataStore dataStore );

    /**
     * Returns information associated with the plot.
     *
     * @param   plan  plotting plan
     * @param   style  plot style
     * @param   xLog  true iff X axis is logarithmic
     * @return  report info, may be null
     * @see   uk.ac.starlink.ttools.plot2.Drawing#getReport
     */
    protected abstract ReportMap getPixel1dReport( Pixel1dPlan plan, S style,
                                                   boolean xLog );

    /**
     * Returns the combination mode configuration key for this plotter.
     *
     * @return  combiner key
     */
    public ConfigKey<Combiner> getCombinerKey() {
        return combinerKey_;
    }

    /**
     * The supplied <code>geom</code> is ignored.
     */
    public PlotLayer createLayer( DataGeom geom, final DataSpec dataSpec,
                                  final S style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        final boolean isY = isY( style );
        final LayerOpt layerOpt = getLayerOpt( style );
        DataGeom pixoDataGeom =
              isY
            ? new SliceDataGeom( new FloatingCoord[] { null, xCoord_ }, "Y" )
            : new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );
        return new AbstractPlotLayer( this, pixoDataGeom, dataSpec,
                                      style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          final PaperType paperType ) {
                if ( ! ( surface instanceof PlanarSurface ) ) {
                    throw new IllegalArgumentException( "Not planar surface "
                                                      + surface );
                }
                final PlanarSurface pSurf = (PlanarSurface) surface;
                final Axis xAxis = pSurf.getAxes()[ isY ? 1 : 0 ];
                final boolean xLog = pSurf.getLogFlags()[ isY ? 1 : 0 ];
                final int xpad = getPixelPadding( style, pSurf );
                final Combiner combiner = getCombiner( style );
                return new Drawing() {
                    public Object calculatePlan( Object[] knownPlans,
                                                 DataStore dataStore ) {
                        for ( int ip = 0; ip < knownPlans.length; ip++ ) {
                            if ( knownPlans[ ip ] instanceof Pixel1dPlan ) {
                                Pixel1dPlan plan =
                                    (Pixel1dPlan) knownPlans[ ip ];
                                if ( plan.matches( xAxis, xpad, combiner,
                                                   dataSpec ) ) {
                                    return plan;
                                }
                            }
                        }
                        BinArray binArray =
                            readBins( xAxis, MAX_KERNEL_WIDTH, combiner,
                                      dataSpec, dataStore );
                        return new Pixel1dPlan( binArray, xAxis,
                                                MAX_KERNEL_WIDTH, combiner,
                                                dataSpec );
                    }
                    public void paintData( Object plan, Paper paper,
                                           DataStore dataStore ) {
                        Pixel1dPlan pPlan = (Pixel1dPlan) plan;
                        final BinArray binArray = pPlan.binArray_;
                        paperType.placeDecal( paper, new Decal() {
                            public void paintDecal( Graphics g ) {
                                paintBins( pSurf, binArray, style,
                                           (Graphics2D) g );
                            }
                            public boolean isOpaque() {
                                return layerOpt.isOpaque();
                            }
                        } );
                    }
                    public ReportMap getReport( Object plan ) {
                        return plan instanceof Pixel1dPlan
                             ? getPixel1dReport( (Pixel1dPlan) plan,
                                                 style, xLog )
                             : null;
                    }
                };
            }

            @Override
            public void extendCoordinateRanges( Range[] ranges,
                                                boolean[] logFlags,
                                                DataStore dataStore ) {
                extendPixel1dCoordinateRanges( ranges, logFlags, style,
                                               dataSpec, dataStore );
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
     * @param  combiner   bin aggregation mode
     * @param  dataSpec  specification for frequency data values
     * @param  dataStore  data storage
     */
    public BinArray readBins( final Axis xAxis, int padPix,
                              final Combiner combiner,
                              DataSpec dataSpec, DataStore dataStore ) {

        /* Work out the pixel limits over which we need to accumulate counts. */
        int[] glimits = xAxis.getGraphicsLimits();
        final int ilo = glimits[ 0 ] - padPix;
        final int ihi = glimits[ 1 ] + padPix;
        final boolean isUnweighted =
            weightCoord_ == null || dataSpec.isCoordBlank( icWeight_ );

        /* Accumulate the counts into a suitable results object (BinArray)
         * and return them. */
        SplitCollector<TupleSequence,BinAccumulator> collector =
                new SplitCollector<TupleSequence,BinAccumulator>() {
            public BinAccumulator createAccumulator() {
                return new BinAccumulator( ilo, ihi, combiner );
            }
            public void accumulate( TupleSequence tseq,
                                    BinAccumulator binAcc ) {
                if ( isUnweighted ) {
                    while ( tseq.next() ) {
                        double dx = xCoord_.readDoubleCoord( tseq, icX_ );
                        double gx = xAxis.dataToGraphics( dx );
                        binAcc.submitToBin( gx, 1 );
                    }
                }
                else {
                    while ( tseq.next() ) {
                        double w =
                            weightCoord_.readDoubleCoord( tseq, icWeight_ );
                        if ( PlotUtil.isFinite( w ) ) {
                            double dx = xCoord_.readDoubleCoord( tseq, icX_ );
                            double gx = xAxis.dataToGraphics( dx );
                            binAcc.submitToBin( gx, w );
                        }
                    }
                }
            }
            public BinAccumulator combine( BinAccumulator acc1,
                                           BinAccumulator acc2 ) {
                acc1.add( acc2 );
                return acc1;
            }
        };
        return PlotUtil.tupleCollect( collector, dataSpec, dataStore )
              .getResult();                      
    }

    /**
     * Returns an array of data coordinate values, one for each bin
     * accumulated by the bin array (X pixel value).
     * This is basically the bin array results, but perhaps adjusted
     * by style elements like smoothing, cumulativeness etc.
     *
     * @param   binArray  basic results
     * @param   xAxis   axis over which counts are accumulated
     * @param   kernel   smoothing kernel
     * @param   norm   normalisation mode
     * @param   ctype  combiner type used to populate bins
     * @param   unit    unit for scaling X axis bin width
     * @param   cumul   cumulative representation
     * @return  output data bin values
     */
    public static double[] getDataBins( BinArray binArray, Axis xAxis,
                                        Kernel1d kernel, Normalisation norm,
                                        Combiner.Type ctype, Unit unit,
                                        Cumulation cumul ) {
        double[] bins = binArray.getBins();
        int nb = bins.length;

        /* Smooth. */
        if ( kernel != null ) {
            bins = kernel.convolve( bins );
        }

        /* Work out the maximum bin height, which may be required for
         * normalisation (Normalisation.MAXIMUM mode, non-cumulative only).
         * This procedure is flawed, since it will fail to pick up
         * maximum bar heights outside of the range covered by the bins array.
         * It probably should do that, but it would require the BinArray
         * to keep track of a lot of values it doesn't otherwise need to
         * worry about - both increases complication of the code, and
         * potentially a large memory footprint.  For now leave it be,
         * but note that MAXIMUM normalisation may not work perfectly
         * when the X axis is zoomed to a region that does not include
         * the highest bar. */
        double max = 0;
        for ( int ib = 0; ib < bins.length; ib++ ) {
            double val = bins[ ib ];
            if ( ! Double.isNaN( val ) ) {
                max = Math.max( max, Math.abs( val ) );
            }
        }

        /* Normalise.  This probably doesn't make much sense for intensive
         * combiners like MEAN. */
        double total = binArray.loBin_ + binArray.midBin_ + binArray.hiBin_;
        double binWidth =
            PlotUtil.getPixelScaleExtent( xAxis ) / unit.getExtent();
        double scale = norm.getScaleFactor( total, max, binWidth, ctype,
                                            cumul.isCumulative() );
        if ( scale != 1.0 ) {
            double[] nbins = new double[ nb ];
            for ( int ib = 0; ib < nb; ib++ ) {
                nbins[ ib ] = scale * bins[ ib ];
            }
            bins = nbins;
        }

        /* Cumulate.  This probably doesn't make much sense for intensive
         * combiners like MEAN. */
        if ( cumul.isCumulative() ) {
            double[] dlimits = xAxis.getDataLimits();
            boolean xflip = ( xAxis.dataToGraphics( dlimits[ 0 ] )
                            > xAxis.dataToGraphics( dlimits[ 1 ] ) )
                          ^ cumul.isReverse();
            double[] cbins = new double[ nb ];
            double sum = scale * ( xflip ? binArray.hiBin_ : binArray.loBin_ );
            for ( int ib = 0; ib < nb; ib++ ) {
                int jb = xflip ? nb - ib - 1 : ib;
                double value = bins[ jb ];
                if ( ! Double.isNaN( value ) ) {
                    sum += bins[ jb ];
                }
                cbins[ jb ] = sum;
            }
            bins = cbins;
        }

        /* Return result. */
        return bins;
    }

    /**
     * Returns the range of a given kernel over which it will be evaluated
     * for the purposes of this plotter.
     * This is basically the kernel's extent, but it may be limited to some
     * maximum for practical purposes.
     *
     * @param  kernel  smoothing kernel
     * @return  effective extent
     */
    public static int getEffectiveExtent( Kernel1d kernel ) {
        return Math.min( kernel.getExtent(), MAX_KERNEL_EXTENT );
    }

    /**
     * Creates a new kernel from configuration items.
     *
     * @param   kernelShape  functional form
     * @param   sizer   determines width in data coordinates
     * @param   xAxis   axis on which samples occur
     * @param   isMean   true if the smoothing is to suitable for
     *                   intensive quantities like the mean,
     *                   false for extensive quantities like a sum
     * @return  kernel
     */
    public static Kernel1d createKernel( Kernel1dShape kernelShape,
                                         BinSizer sizer, Axis xAxis,
                                         boolean isMean ) {
        double width = getPixelWidth( sizer, xAxis );
        return isMean
             ? kernelShape.createMeanKernel( width )
             : kernelShape.createFixedWidthKernel( width );
    }

    /**
     * Calculates the width in pixel coordinates represented by a
     * bin sizer applied to a given axis.
     *
     * @param   sizer   determines width in data coordinates
     * @param   xAxis   axis on which samples occur
     * @return   width in pixel coordinates represented by sizer,
     *           never negative
     */
    public static double getPixelWidth( BinSizer sizer, Axis xAxis ) {
        double[] dLimits = xAxis.getDataLimits();
        int[] gLimits = xAxis.getGraphicsLimits();
        int gExtent = gLimits[ 1 ] - gLimits[ 0 ];
        Scale xScale = xAxis.getScale();
        double sWidth =
            sizer.getScaleWidth( xScale, dLimits[ 0 ], dLimits[ 1 ], false );
        double slo = xScale.dataToScale( dLimits[ 0 ] );
        double shi = xScale.dataToScale( dLimits[ 1 ] );
        return Math.abs( gExtent * ( sWidth / ( shi - slo ) ) );
    }

    /**
     * Creates a config key for selecting combination modes for a
     * KDE-like plotter.
     *
     * @param  weightCoord   weight coordinate, or null for no weighting
     * @param  unitKey     X-axis unit scaling key, or null for
     *                     no unit selection
     * @return   new config key
     */
    private static ConfigKey<Combiner>
            createCombinerKey( FloatingCoord weightCoord,
                               ConfigKey<Unit> unitKey ) {
        ConfigMeta meta = new ConfigMeta( "combine", "Combine" );
        boolean hasUnit = unitKey != null;
        meta.setShortDescription( "Weight combination mode" );
        StringBuffer dbuf = new StringBuffer();
        dbuf.append( PlotUtil.concatLines( new String[] {
            "<p>Defines how values contributing to the same bin",
            "are combined together to produce the value assigned to that bin,",
            "and hence its height.",
            "The bins in this case are 1-pixel wide, so lack much physical",
            "significance.",
            "This means that while some combination modes, such as",
            "<code>" + Combiner.WEIGHTED_DENSITY + "</code> and",
            "<code>" + Combiner.MEAN + "</code> make sense,",
            "others such as",
            "<code>" + Combiner.SUM + "</code> do not.",
            "</p>",
            "<p>The combined values are those given by the",
            "<code>" + weightCoord.getInput().getMeta().getShortName()
                     + "</code> coordinate,",
            "but if no weight is supplied,",
            "a weighting of unity is assumed.",
            "</p>",
        } ) );
        if ( hasUnit ) {
            dbuf.append( PlotUtil.concatLines( new String[] {
                "<p>For density-like values",
                "(<code>" + Combiner.DENSITY + "</code>,",
                "<code>" + Combiner.WEIGHTED_DENSITY + "</code>)",
                "the scaling is additionally influenced by the",
                "<code>" + unitKey.getMeta().getShortName() + "</code>",
                "parameter.",
                "</p>",
            } ) );
        }
        meta.setXmlDescription( dbuf.toString() );
        OptionConfigKey<Combiner> key =
                new OptionConfigKey<Combiner>( meta, Combiner.class,
                                               Combiner.getKnownCombiners(),
                                               Combiner.WEIGHTED_DENSITY ) {
            public String getXmlDescription( Combiner combiner ) {
                return combiner.getDescription();
            }
        };
        key.setOptionUsage();
        key.addOptionsXml();
        return key;
    }

    /**
     * Plot plan implementation for this class.
     */
    public static class Pixel1dPlan {
        final BinArray binArray_;
        final Axis xAxis_;
        final int xpad_;
        final Combiner combiner_;
        final DataSpec dataSpec_;

        /**
         * Constructor.
         *
         * @param  binArray   frequency data
         * @param  xAxis  axis over which counts are accumulated
         * @param  xpad   number of pixels outside axis range in each direction
         *                that are stored in <code>binArray</code>
         * @param  combiner  bin aggregation mode
         * @param  dataSpec   count data specificatin
         */
        Pixel1dPlan( BinArray binArray, Axis xAxis, int xpad,
                     Combiner combiner, DataSpec dataSpec ) {
            binArray_ = binArray;
            xAxis_ = xAxis;
            xpad_ = xpad;
            combiner_ = combiner;
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
        boolean matches( Axis xAxis, int xpad, Combiner combiner,
                         DataSpec dataSpec ) {
            return xAxis_.equals( xAxis )
                && combiner_.equals( combiner )
                && dataSpec_.equals( dataSpec )
                && xpad_ >= xpad;
        }
    }

    /**
     * Data object storing counts per pixel.
     */
    public static class BinArray {

        private final int glo_;
        private final int ghi_;
        private final double[] bins_;
        private final double loBin_;
        private final double hiBin_;
        private final double midBin_;

        /**
         * Constructor.
         *
         * @param  glo  lowest pixel index required
         * @param  ghi  1+highest pixel index required
         * @param  bins   accumulated bin values for each integer between
         *                glo and ghi
         * @param  loBin  accumulated value for 
         */
        private BinArray( int glo, int ghi, double[] bins,
                          double loBin, double hiBin, double midBin ) {
            glo_ = glo;
            ghi_ = ghi;
            bins_ = bins;
            loBin_ = loBin;
            hiBin_ = hiBin;
            midBin_ = midBin;
        }

        /**
         * Returns the array of pixel-width bins containing the counts
         * accumulated by this object.
         *
         * @return  array of count values, one per pixel
         */
        public double[] getBins() {
            return bins_;
        }

        /**
         * Returns the bin index
         * (index into this object's <code>bins_</code> array)
         * for a given pixel index.
         *
         * @param  gx  pixel index
         * @return  bin index
         */
        public int getBinIndex( int gx ) {
            return gx - glo_;
        }

        /**
         * Returns the pixel index for a given bin index.
         *
         * @param  index  bin index
         * @return   pixel index
         */
        public int getGraphicsCoord( int index ) {
            return index + glo_;
        }
    }

    /**
     * Data object storing counts per pixel.
     */
    private static class BinAccumulator {

        private final Combiner.Container[] bins_;
        private final Combiner.Container loBin_;
        private final Combiner.Container hiBin_;
        private final Combiner.Container midBin_;
        private final int glo_;
        private final int ghi_;

        /**
         * Constructor.
         *
         * @param  glo  lowest pixel index required
         * @param  ghi  1+highest pixel index required
         * @param  combiner   aggregation mode
         */
        BinAccumulator( int glo, int ghi, Combiner combiner ) {
            glo_ = glo;
            ghi_ = ghi;
            int nbin = ghi - glo;
            bins_ = new Combiner.Container[ nbin ];
            for ( int i = 0; i < nbin; i++ ) {
                bins_[ i ] = combiner.createContainer();
            }
            loBin_ = combiner.createContainer();
            hiBin_ = combiner.createContainer();
            midBin_ = combiner.createContainer();
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
        void submitToBin( double gx, double inc ) {
            double dx = Math.round( gx - glo_ );
            if ( dx >= 0 && dx < bins_.length ) {
                bins_[ (int) dx ].submit( inc );
                midBin_.submit( inc );
            }
            else if ( dx < 0 ) {
                loBin_.submit( inc );
            }
            else if ( dx >= bins_.length ) {
                hiBin_.submit( inc );
            }
        }

        /**
         * Merges the contents of another compatible BinAccumulator
         * with this one.
         *
         * @param  other  compatible accumulator
         */
        void add( BinAccumulator other ) {
            loBin_.add( other.loBin_ );
            hiBin_.add( other.hiBin_ );
            midBin_.add( other.midBin_ );
            for ( int i = 0; i < bins_.length; i++ ) {
                bins_[ i ].add( other.bins_[ i ] );
            }
        }

        /**
         * Returns the current accumulated state as a BinArray object.
         *
         * @return   bin state
         */
        BinArray getResult() {
            double[] dbins = new double[ bins_.length ];
            for ( int i = 0; i < bins_.length; i++ ) {
                double val = bins_[ i ].getCombinedValue();
                dbins[ i ] = val;
            }
            double loBin = getDefiniteValue( loBin_ );
            double hiBin = getDefiniteValue( hiBin_ );
            double midBin = getDefiniteValue( midBin_ );
            return new BinArray( glo_, ghi_, dbins, loBin, hiBin, midBin );
        }

        /**
         * Returns the result value of a container as a definite number.
         * Zero is returned instead of NaN.
         *
         * @param  container  container to interrogate
         * @return  definite result value, not NaN
         */
        private double getDefiniteValue( Combiner.Container container ) {
            double d = container.getCombinedValue();
            return Double.isNaN( d ) ? 0 : d;
        }
    }
}
