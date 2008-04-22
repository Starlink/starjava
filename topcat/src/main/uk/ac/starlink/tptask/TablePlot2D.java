package uk.ac.starlink.tptask;

import uk.ac.starlink.tplot.*;

/**
 * Task for performing a 2D scatter plot.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class TablePlot2D extends PlotTask {
    public TablePlot2D() {
        super( "2D Scatter Plot",
               new PlotStateFactory( new String[] { "X", "Y", } ),
               new ScatterPlot( new PtPlotSurface() ) );
    }
}
