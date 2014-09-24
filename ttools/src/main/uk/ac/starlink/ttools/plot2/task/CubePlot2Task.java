package uk.ac.starlink.ttools.plot2.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.CubeDataGeom;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.CubeSurfaceFactory;

/**
 * Task for cube-type plots.
 *
 * @author   Mark Taylor
 * @since    11 Sep 2014
 */
public class CubePlot2Task extends TypedPlot2Task {

    private static final Map<ConfigKey<String>,Input> AXLABEL_MAP =
        createAxisLabelMap();

    /**
     * Constructor.
     */
    public CubePlot2Task() {
        super( CubePlotType.getInstance(), AXLABEL_MAP );
    }

    /**
     * Constructs a map of axis label keys to data input coordinates.
     *
     * @return mapping
     */ 
    private static Map<ConfigKey<String>,Input> createAxisLabelMap() {
        Map<ConfigKey<String>,Input> map =
            new HashMap<ConfigKey<String>,Input>();
        map.put( CubeSurfaceFactory.XLABEL_KEY,
                 CubeDataGeom.X_COORD.getInput() );
        map.put( CubeSurfaceFactory.YLABEL_KEY,
                 CubeDataGeom.Y_COORD.getInput() );
        map.put( CubeSurfaceFactory.ZLABEL_KEY,
                 CubeDataGeom.Z_COORD.getInput() );
        return Collections.unmodifiableMap( map );
    }
}
