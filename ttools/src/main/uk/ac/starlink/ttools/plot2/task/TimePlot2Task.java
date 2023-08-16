package uk.ac.starlink.ttools.plot2.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.TimeAspect;
import uk.ac.starlink.ttools.plot2.geom.TimeDataGeom;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.geom.TimeSurfaceFactory;

/**
 * Task for time plots.
 *
 * @author   Mark Taylor
 * @since    11 Sep 2014
 */
public class TimePlot2Task extends
        TypedPlot2Task<TimeSurfaceFactory.Profile,TimeAspect> {

    private static final TimePlotType PLOTTYPE = TimePlotType.getInstance();
    private static final Map<ConfigKey<String>,Input> AXLABEL_MAP =
        createAxisLabelMap();

    /**
     * Constructor.
     */
    public TimePlot2Task() {
        super( PLOTTYPE, AXLABEL_MAP, createDefaultPlotContext( PLOTTYPE ) );
    }

    /**
     * Constructs a map of axis label keys to data input coordinates.
     *
     * @return mapping
     */ 
    private static Map<ConfigKey<String>,Input> createAxisLabelMap() {
        Map<ConfigKey<String>,Input> map =
            new HashMap<ConfigKey<String>,Input>();
        map.put( TimeSurfaceFactory.YLABEL_KEY,
                 TimeDataGeom.Y_COORD.getInput() );
        return Collections.unmodifiableMap( map );
    }
}
