package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.geom.TimePlotType;

/**
 * Task for time plots.
 *
 * @author   Mark Taylor
 * @since    11 Sep 2014
 */
public class TimePlot2Task extends TypedPlot2Task {
    public TimePlot2Task() {
        super( TimePlotType.getInstance() );
    }
}
