package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
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
     */
    public FixedKernelDensityPlotter( FloatingCoord xCoord,
                                      boolean hasWeight ) {
        super( xCoord, hasWeight, "KDE", ResourceIcon.FORM_KDE );
    }

    protected ConfigKey[] getKernelConfigKeys() {
        return new ConfigKey[] {
            SMOOTHSIZER_KEY,
        };
    }

    protected KernelFigure createKernelFigure( ConfigMap config ) {
        return new FixedKernelFigure( config.get( SMOOTHSIZER_KEY ) );
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
        } );
    }

    /**
     * KernelFigure implementation for fixed-width kernel.
     */
    private static class FixedKernelFigure implements KernelFigure {
        private final BinSizer binSizer_;

        /**
         * Constructor.
         *
         * @param  binSizer   determines smoothing widths
         */
        FixedKernelFigure( BinSizer binSizer ) {
            binSizer_ = binSizer;
        }

        public Kernel1d createKernel( Kernel1dShape shape, Axis xAxis,
                                      boolean xLog ) {
            return Pixel1dPlotter.createKernel( shape, binSizer_, xAxis, xLog );
        }

        public ReportMap getReportMap( boolean xLog, double dlo, double dhi ) {
            ReportMap report = new ReportMap();
            report.put( SMOOTHWIDTH_KEY, binSizer_.getWidth( xLog, dlo, dhi ) );
            return report;
        }

        @Override
        public int hashCode() {
            int code = 23452304;
            code = 23 * code + binSizer_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedKernelFigure ) {
                FixedKernelFigure other = (FixedKernelFigure) o;
                return this.binSizer_.equals( other.binSizer_ );
            }
            else {
                return false;
            }
        }
    }
}
