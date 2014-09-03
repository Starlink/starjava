package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;

/**
 * Task for Plane-type plots.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class PlanePlot2Task extends TypedPlot2Task {
    public PlanePlot2Task() {
        super( PlanePlotType.getInstance() );
    }
}
