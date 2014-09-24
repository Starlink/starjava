package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.ttools.plot2.geom.SpherePlotType;

/**
 * Task for sphere (isotropic 3D)-type plots.
 *
 * @author   Mark Taylor
 * @since    11 Sep 2014
 */
public class SpherePlot2Task extends TypedPlot2Task {
    public SpherePlot2Task() {
        super( SpherePlotType.getInstance(), null );
    }
}
