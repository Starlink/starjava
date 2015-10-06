package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleConfigKey;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;

/**
 * Kernel Density Plotter that uses a variable-width kernel whose width
 * is assigned using K-Nearest-Neighbours counting.
 *
 * @author   Mark Taylor
 * @since    30 Mar 2015
 */
public class KnnKernelDensityPlotter extends AbstractKernelDensityPlotter {

    /** Report key for actual minimum smoothing width. */
    public static final ReportKey<Double> MINWIDTH_RKEY =
        new ReportKey<Double>( new ReportMeta( "minwidth",
                                               "Minimum smoothing width" ),
                               Double.class, false );

    /** Report key for actual maximum smoothing width. */
    public static final ReportKey<Double> MAXWIDTH_RKEY =
        new ReportKey<Double>( new ReportMeta( "maxwidth",
                                               "Maximum smoothing width" ),
                               Double.class, false );

    /** Config key for number of nearest neighbours. */
    public static final ConfigKey<Double> KNN_CKEY =
        new DoubleConfigKey(
             new ConfigMeta( "knn", "Knn K" )
            .setShortDescription( "Number of nearest neighbours" )
            .setXmlDescription( new String[] {
                 "<p>Sets the number of nearest neighbours to count",
                 "away from a sample point to determine the width",
                 "of the smoothing kernel at that point.",
                 "For the symmetric case this is the number of nearest",
                 "neighbours summed over both directions,",
                 "and for the asymmetric case it is the number in a single",
                 "direction.",
                 "</p>",
                 "<p>The threshold is actually the weighted total of samples;",
                 "for unweighted (<code>weight=1</code>) bins",
                 "that is equivalent to the number of samples.",
                 "</p>",
             } )
        , 100 ) {
            public Specifier<Double> createSpecifier() {
                return new SliderSpecifier( 1, 10000, true, 100, false,
                                            SliderSpecifier.TextOption
                                                           .ENTER_ECHO );
            }
        };

    /** Config key for determining symmetry of KNN search. */
    public static final ConfigKey<Boolean> SYMMETRIC_CKEY =
        new BooleanConfigKey(
            new ConfigMeta( "symmetric", "Symmetric" )
           .setShortDescription( "KNN search in both directions?" )
           .setXmlDescription( new String[] {
                "<p>If true, the nearest neigbour search is carried out",
                "in both directions, and the kernel is symmetric.",
                "If false, the nearest neigbour search is carried out",
                "separately in the positive and negative directions,",
                "and the kernel width is accordingly different in the",
                "positive and negative directions.",
                "</p>",
            } )
        , true );

    /** Config key for minimum smoothing width. */
    public static final ConfigKey<BinSizer> MINSIZER_CKEY =
        createLimitSizerKey( false );

    /** Config key for maximum smoothing width configuration. */
    public static final ConfigKey<BinSizer> MAXSIZER_CKEY =
        createLimitSizerKey( true );

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     */
    public KnnKernelDensityPlotter( FloatingCoord xCoord,
                                    boolean hasWeight ) {
        super( xCoord, hasWeight, "Knn", ResourceIcon.FORM_KNN );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a Discrete Kernel Density Estimate",
            "giving a smoothed frequency of data values along the",
            "horizontal axis, using an adaptive (K-Nearest-Neighbours)",
            "smoothing kernel.",
            "This is a generalisation of a histogram in which",
            "the bins are always 1 pixel wide,",
            "and a smoothing kernel is applied to each bin.",
            "The width and shape of the kernel may be varied.",
            "</p>",
            "<p>The K-Nearest-Neighbour figure gives the number of",
            "points in each direction to determine the width of the",
            "smoothing kernel for smoothing each bin.",
            "Upper and lower limits for the kernel width are also supplied;",
            "if the upper and lower limits are equal, this is equivalent",
            "to a fixed-width kernel.",
            "</p>",
            "<p>Note this is not a true Kernel Density Estimate,",
            "since, for performance reasons,",
            "the smoothing is applied to the (pixel-width) bins",
            "rather than to each data sample.",
            "The deviation from a true KDE caused by this quantisation",
            "will be at the pixel level,",
            "hence in most cases not visually apparent.",
            "</p>",
        } );
    }

    protected ConfigKey[] getKernelConfigKeys() {
        return new ConfigKey[] {
            KNN_CKEY,
            SYMMETRIC_CKEY,
            MINSIZER_CKEY,
            MAXSIZER_CKEY,
        };
    }

    protected KernelFigure createKernelFigure( ConfigMap config )
            throws ConfigException {
        double k = config.get( KNN_CKEY );
        boolean isSymmetric = config.get( SYMMETRIC_CKEY );
        BinSizer minSizer = config.get( MINSIZER_CKEY );
        BinSizer maxSizer = config.get( MAXSIZER_CKEY );
        if ( minSizer.getWidth( false, 0, 1 ) >
             maxSizer.getWidth( false, 0, 1 ) ) {
            throw new ConfigException( MINSIZER_CKEY,
                                       "Smoothing min/max are "
                                     + "the wrong way round" );
        }
        return new KnnKernelFigure( k, isSymmetric, minSizer, maxSizer );
    }

    /**
     * Returns a binsizer key for use in configuring limiting smoothing
     * widths for use with this plotter.
     *
     * @param  isMax  true for upper limit, false for lower limit
     * @retrurn   new config key
     */
    private static ConfigKey<BinSizer> createLimitSizerKey( boolean isMax ) {
        ConfigMeta meta =
            new ConfigMeta( ( isMax ? "max" : "min" ) + "smooth",
                            ( isMax ? "Max" : "Min" ) + " Smoothing" );
        meta.setStringUsage( "+<width>|-<count>" );
        meta.setShortDescription( ( isMax ? "Upper" : "Lower" )
                                + " size limit of smoothing kernel" );
        meta.setXmlDescription( new String[] {
            "<p>Fixes the",
            isMax ? "maximum" : "minimum",
            "size of the smoothing kernel.",
            "This functions as",
            isMax ? "an upper" : "a lower",
            "limit on the distance that is otherwise determined by",
            "searching for the K nearest neighbours at each sample point.",
            "</p>",
            BinSizer.getConfigKeyDescription(),
        } );
        ReportKey<Double> reportKey = isMax ? MAXWIDTH_RKEY : MINWIDTH_RKEY;
        int dfltNbin = isMax ? 100 : 0;
        boolean rounding = false;
        boolean allowZero = true;
        return BinSizer.createSizerConfigKey( meta, reportKey, dfltNbin,
                                              rounding, allowZero );
    }

    /**
     * KernelFigure implementation for a K-Nearest-Neighbour adaptive-width
     * kernel.
     */
    private static class KnnKernelFigure implements KernelFigure {
        private final double knn_;
        private final boolean isSymmetric_;
        private final BinSizer minSizer_;
        private final BinSizer maxSizer_;

        /**
         * Constructor.
         *
         * @param   knn  nearest neighbour threshold
         * @param   isSymmetric  true for bidirectional KNN search,
         *                       false for unidirectional
         * @param   minSizer   determines minimum smoothing width
         * @param   maxSizer   determines maximum smoothing width
         */
        KnnKernelFigure( double knn, boolean isSymmetric,
                         BinSizer minSizer, BinSizer maxSizer ) {
            knn_ = knn;
            isSymmetric_ = isSymmetric;
            minSizer_ = minSizer;
            maxSizer_ = maxSizer;
        }

        public Kernel1d createKernel( Kernel1dShape shape, Axis xAxis,
                                      boolean xLog ) {
            int minWidth = (int) getPixelWidth( minSizer_, xAxis, xLog );
            int maxWidth = (int) getPixelWidth( maxSizer_, xAxis, xLog );
            return shape.createKnnKernel( knn_, isSymmetric_,
                                          minWidth, maxWidth );
        }

        public ReportMap getReportMap( boolean xLog, double dlo, double dhi ) {
            ReportMap report = new ReportMap();
            report.put( MINWIDTH_RKEY, minSizer_.getWidth( xLog, dlo, dhi ) );
            report.put( MAXWIDTH_RKEY, maxSizer_.getWidth( xLog, dlo, dhi ) );
            return report;
        }

        @Override
        public int hashCode() {
            int code = 2134233;
            code = 23 * code + Float.floatToIntBits( (float) knn_ );
            code = 23 * code + ( isSymmetric_ ? 11 : 13 );
            code = 23 * code + minSizer_.hashCode();
            code = 23 * code + maxSizer_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof KnnKernelFigure ) {
                KnnKernelFigure other = (KnnKernelFigure) o;
                return this.knn_ == other.knn_
                    && this.isSymmetric_ == other.isSymmetric_
                    && this.minSizer_.equals( other.minSizer_ )
                    && this.maxSizer_.equals( other.maxSizer_ );
            }
            else {
                return false;
            }
        }
    }
}
