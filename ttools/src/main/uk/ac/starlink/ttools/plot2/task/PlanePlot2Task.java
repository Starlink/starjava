package uk.ac.starlink.ttools.plot2.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;

/**
 * Task for Plane-type plots.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class PlanePlot2Task extends TypedPlot2Task {

    private static final Map<ConfigKey<String>,Input> AXLABEL_MAP =
        createAxisLabelMap();

    /**
     * Constructor.
     */
    public PlanePlot2Task() {
        super( PlanePlotType.getInstance(), AXLABEL_MAP );
    }

    /**
     * Constructs a map of axis label keys to data input coordinates.
     *
     * @return mapping
     */
    private static Map<ConfigKey<String>,Input> createAxisLabelMap() {
        Map<ConfigKey<String>,Input> map =
            new HashMap<ConfigKey<String>,Input>();
        map.put( PlaneSurfaceFactory.XLABEL_KEY,
                 PlaneDataGeom.X_COORD.getInput() );
        map.put( PlaneSurfaceFactory.YLABEL_KEY,
                 PlaneDataGeom.Y_COORD.getInput() );
        return Collections.unmodifiableMap( map );
    }
}
