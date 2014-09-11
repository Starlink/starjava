package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.geom.CubePlotType;

/**
 * Task for cube-type plots.
 *
 * @author   Mark Taylor
 * @since    11 Sep 2014
 */
public class CubePlot2Task extends TypedPlot2Task {
    public CubePlot2Task() {
        super( CubePlotType.getInstance() );
    }
}
