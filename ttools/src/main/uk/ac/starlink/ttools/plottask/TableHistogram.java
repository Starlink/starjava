package uk.ac.starlink.ttools.plottask;

import uk.ac.starlink.ttools.plot.Histogram;
import uk.ac.starlink.ttools.plot.PtPlotSurface;

/**
 * Task for performing a 1D histogram.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2008
 */
public class TableHistogram extends PlotTask {
    public TableHistogram() {
        super( "Histogram",
               new HistogramPlotStateFactory(),
               new Histogram( new PtPlotSurface() ) );
    }
}
