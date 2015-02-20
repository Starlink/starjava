package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
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
 * Abstract superclass for histogram-like plotters that have pixel-sized
 * bins with optional smoothing.
 * Only works with PlaneSurfaces.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2015
 */
public abstract class Pixel1dPlotter<S extends Style> implements Plotter<S> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord weightCoord_;
    private final String name_;
    private final Icon icon_;
    private final SliceDataGeom pixoDataGeom_;
    private final CoordGroup pixoCoordGrp_;
    private final int icX_;
    private final int icWeight_;

    /** Not a fixed limit, it's just optimisation. */
    private static final int MAX_KERNEL_WIDTH = 50;

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
     * @param   name  plotter name
     * @param   icon  plotter icon
     */
    protected Pixel1dPlotter( FloatingCoord xCoord, boolean hasWeight,
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
     * @return   padding in pixels required in bin array
     */
    protected abstract int getPixelPadding( S style );

    /** 
     * Draws the graphical representation of a given array of counts per
     * horizontal pixel.
     *  
     * @param  surface  plotting surface 
     * @param  binArray   counts per X axis pixel
     * @param  style   plotting style
     * @param  g  graphics context
     */                                      
    protected abstract void paintBins( PlaneSurface surface, BinArray binArray,
                                       S style, Graphics2D g );

    /**
     * Performs any required range extension.  May be a no-op.
     *
     * @param  array of data space dimension ranges to update
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
     * @return  report info, may be null
     * @see   uk.ac.starlink.ttools.plot2.Drawing#getReport
     */
    protected abstract ReportMap getPixel1dReport( Pixel1dPlan plan, S style );

    /**
     * Calculates the plan object for this plotter.
     *
     * @param   knownPlans  available plan objects
     * @param   xAxis  bin width axis
     * @param   xpad  number of pixel bins required each side of plot range
     * @param   dataSpec  data specification
     * @param   dataStore  data storage object
     */
    public Pixel1dPlan calculatePixelPlan( Object[] knownPlans,
                                           Axis xAxis, int xpad,
                                           DataSpec dataSpec,
                                           DataStore dataStore ) {
        for ( int ip = 0; ip < knownPlans.length; ip++ ) {
            if ( knownPlans[ ip ] instanceof Pixel1dPlan ) {
                Pixel1dPlan plan = (Pixel1dPlan) knownPlans[ ip ];
                if ( plan.matches( xAxis, xpad, dataSpec ) ) {
                    return plan;
                }
            }
        }
        BinArray binArray =
            readBins( xAxis, MAX_KERNEL_WIDTH, dataSpec, dataStore );
        return new Pixel1dPlan( binArray, xAxis, MAX_KERNEL_WIDTH, dataSpec );
    }

    /**
     * The supplied <code>geom</code> is ignored.
     */
    public PlotLayer createLayer( DataGeom geom, final DataSpec dataSpec,
                                  final S style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        final LayerOpt layerOpt = getLayerOpt( style );
        return new AbstractPlotLayer( this, pixoDataGeom_, dataSpec,
                                      style, layerOpt ) {
            final int xpad = getPixelPadding( style );
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
                            if ( knownPlans[ ip ] instanceof Pixel1dPlan ) {
                                Pixel1dPlan plan =
                                    (Pixel1dPlan) knownPlans[ ip ];
                                if ( plan.matches( xAxis, xpad, dataSpec ) ) {
                                    return plan;
                                }
                            }
                        }
                        BinArray binArray = readBins( xAxis, MAX_KERNEL_WIDTH,
                                                      dataSpec, dataStore );
                        return new Pixel1dPlan( binArray, xAxis,
                                                MAX_KERNEL_WIDTH, dataSpec );
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
                             ? getPixel1dReport( (Pixel1dPlan) plan, style )
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
     * @param  dataSpec  specification for frequency data values
     * @param  dataStore  data storage
     */
    public BinArray readBins( Axis xAxis, int padPix, DataSpec dataSpec,
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
     * Returns an array of data coordinate values, one for each bin
     * accumulated by the bin array (X pixel value).
     * This is basically the bin array results, but perhaps adjusted
     * by style elements like smoothing, cumulativeness etc.
     *
     * @param   binArray  basic results
     * @param   xAxis   axis over which counts are accumulated
     * @param   kernel   smoothing kernel
     * @param   norm   normalisation mode
     * @param   cumul   true for cumulative representation
     * @return  output data bin values
     */
    public static double[] getDataBins( BinArray binArray, Axis xAxis,
                                        Kernel kernel, Normalisation norm,
                                        boolean cumul ) {
        double[] bins = binArray.getBins();
        int nb = bins.length;

        /* Smooth. */
        if ( kernel != null ) {
            bins = kernel.convolve( bins );
        }

        /* Work out the maximum bin height, which may be required for
         * normalisation (Normalisation.MAXIMUM mode, cumul=false only).
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
            max = Math.max( max, Math.abs( bins[ ib ] ) );
        }

        /* Normalise. */
        double total = binArray.getSum();
        double binWidth = getPixelDataWidth( xAxis );
        double scale = norm.getScaleFactor( total, max, binWidth, cumul );
        if ( scale != 1.0 ) {
            double[] nbins = new double[ nb ];
            for ( int ib = 0; ib < nb; ib++ ) {
                nbins[ ib ] = scale * bins[ ib ];
            }
            bins = nbins;
        }

        /* Cumulate. */
        if ( cumul ) {
            double[] dlimits = xAxis.getDataLimits();
            boolean xflip = xAxis.dataToGraphics( dlimits[ 0 ] )
                          > xAxis.dataToGraphics( dlimits[ 1 ] );
            double[] cbins = new double[ nb ];
            double sum = binArray.getLowerSum( xflip );
            for ( int ib = 0; ib < nb; ib++ ) {
                int jb = xflip ? nb - ib - 1 : ib;
                sum += bins[ jb ];
                cbins[ jb ] = sum;
            }
            bins = cbins;
        }

        /* Return result. */
        return bins;
    }

    /**
     * Returns a smoothing kernel with a given width.
     *
     * @param  width  half-width in pixels
     * @return  kernel
     */
    public static Kernel createKernel( int width ) {
        return new SquareKernel( width );
    }

    /**
     * Works out the constant width in data coordinates of a pixel-sized
     * bin on a given axis.  If there is no such constant value
     * (for instance a logarithmic axis), NaN is returned.
     *
     * @param  axis  axis
     * @return   width of pixel in data coordinates, or NaN
     */
    private static double getPixelDataWidth( Axis axis ) {
        if ( axis.isLinear() ) {
            int[] glimits = axis.getGraphicsLimits();
            double gmid = 0.5 * ( glimits[ 0 ] + glimits[ 1 ] );
            return Math.abs( axis.graphicsToData( gmid + 0.5 )
                           - axis.graphicsToData( gmid - 0.5 ) );
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Defines the smoothing function used for pixel bins.
     */
    @Equality
    public interface Kernel {

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
    public static class Pixel1dPlan {
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
        Pixel1dPlan( BinArray binArray, Axis xAxis, int xpad,
                     DataSpec dataSpec ) {
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
    public static class BinArray {

        private final double[] bins_;
        private final int glo_;
        private final int ghi_;
        private double loSum_;
        private double hiSum_;
        private double midSum_;

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
        private void addToBin( double gx, double inc ) {
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
         * Returns the array of pixel-width bins containing the counts
         * accumulated by this object.
         *
         * @return  array of count values, one per pixel
         */
        public double[] getBins() {
            return bins_;
        }

        /**
         * Returns the total sum of values accumulated into this bin array.
         *
         * @return   running total
         */
        public double getSum() {
            return loSum_ + midSum_ + hiSum_;
        }

        /**
         * Returns the sum of all the counts at one end of the axis
         * not captured by this object's bins array.
         *
         * @param  flip   false for low-coordinate end,
         *                true for high-coordinate end
         */
        public double getLowerSum( boolean flip ) {
            return flip ? hiSum_ : loSum_;
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
}
