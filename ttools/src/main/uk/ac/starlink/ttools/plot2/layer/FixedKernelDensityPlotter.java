package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.PerUnitConfigKey;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;

/**
 * Kernel density plotter that uses fixed-width kernels.
 *
 * @author   Mark Taylor
 * @since    30 Mar 2015
 */
public class FixedKernelDensityPlotter extends AbstractKernelDensityPlotter {

    /**
     * Constructor.
     *
     * @param   xCoord  X axis coordinate
     * @param   hasWeight   true to permit histogram weighting
     * @param   unitKey  config key to select X axis physical units,
     *                   or null if no unit selection required
     */
    public FixedKernelDensityPlotter( FloatingCoord xCoord, boolean hasWeight,
                                      PerUnitConfigKey<Unit> unitKey ) {
        super( xCoord, hasWeight, unitKey, "KDE", ResourceIcon.FORM_KDE );
    }

    protected ConfigKey<?>[] getKernelConfigKeys() {
        return new ConfigKey<?>[] {
            SMOOTHSIZER_KEY,
            getCombinerKey(),
        };
    }

    protected KernelFigure createKernelFigure( ConfigMap config ) {
        BinSizer sizer = config.get( SMOOTHSIZER_KEY );
        Combiner combiner = config.get( getCombinerKey() );
        boolean isLikeMean = ! combiner.getType().isExtensive();
        return new FixedKernelFigure( sizer, isLikeMean );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a Discrete Kernel Density Estimate",
            "giving a smoothed frequency of data values along the",
            "horizontal axis, using a fixed-width smoothing kernel.",
            "This is a generalisation of a histogram in which",
            "the bins are always 1 pixel wide,",
            "and a smoothing kernel is applied to each bin.",
            "The width and shape of the kernel may be varied.",
            "</p>",
            "<p>This is suitable for cases where",
            "the division into discrete bins",
            "done by a normal histogram is unnecessary or troublesome.",
            "</p>",
            "<p>Note this is not a true Kernel Density Estimate,",
            "since, for performance reasons,",
            "the smoothing is applied to the (pixel-width) bins",
            "rather than to each data sample.",
            "The deviation from a true KDE caused by this quantisation",
            "will be at the pixel level,",
            "hence in most cases not visually apparent.",
            "</p>",
            getWeightingDescription(),
        } );
    }

    /**
     * KernelFigure implementation for fixed-width kernel.
     */
    private static class FixedKernelFigure implements KernelFigure {
        private final BinSizer binSizer_;
        private final boolean isMean_;

        /**
         * Constructor.
         *
         * @param  binSizer   determines smoothing widths
         * @param   isMean   true if the smoothing is to suitable for
         *                   intensive quantities like the mean,
         *                   false for extensive quantities like a sum
         */
        FixedKernelFigure( BinSizer binSizer, boolean isMean ) {
            binSizer_ = binSizer;
            isMean_ = isMean;
        }

        public Kernel1d createKernel( Kernel1dShape shape, Axis xAxis ) {
            return Pixel1dPlotter
                  .createKernel( shape, binSizer_, xAxis, isMean_ );
        }

        public ReportMap getReportMap( Axis xAxis ) {
            ReportMap report = new ReportMap();
            double[] dLimits = xAxis.getDataLimits();
            Scale scale = xAxis.getScale();
            double w =
                binSizer_.getScaleWidth( scale, dLimits[ 0 ], dLimits[ 1 ],
                                         false );
            report.put( SMOOTHWIDTH_KEY, w );
            return report;
        }

        @Override
        public int hashCode() {
            int code = 23452304;
            code = 23 * code + binSizer_.hashCode();
            code = 23 * code + ( isMean_ ? 29 : 37 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedKernelFigure ) {
                FixedKernelFigure other = (FixedKernelFigure) o;
                return this.binSizer_.equals( other.binSizer_ )
                    && this.isMean_ == other.isMean_;
            }
            else {
                return false;
            }
        }
    }
}
