package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.plot.CartesianPlot3D;
import uk.ac.starlink.ttools.plottask.PlotTask;
import uk.ac.starlink.ttools.plottask.Plot3DStateFactory;

/**
 * Task for performing a 3D scatter plot.
 *
 * @author   Mark Taylor
 * @since    20 Oct 2008
 */
public class TablePlot3D extends PlotTask {
    @SuppressWarnings("this-escape")
    public TablePlot3D() {
        super( "Old-style 3D Scatter Plot",
               new Plot3DStateFactory( new String[] { "X", "Y", "Z" },
                                       true, true, 3 ),
               new CartesianPlot3D() );
        getXpixParameter().setStringDefault( getYpixParameter()
                                            .getStringDefault() );
    }
}
