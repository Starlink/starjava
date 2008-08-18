package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.ScatterPlot;
import uk.ac.starlink.ttools.plottask.PlotTask;
import uk.ac.starlink.ttools.plottask.PlotStateFactory;

/**
 * Task for performing a 2D scatter plot.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class TablePlot2D extends PlotTask {
    public TablePlot2D() {
        super( "2D Scatter Plot",
               new PlotStateFactory( new String[] { "X", "Y" }, true, true, 2 ),
               new ScatterPlot( new PtPlotSurface() ) );
    }
}
