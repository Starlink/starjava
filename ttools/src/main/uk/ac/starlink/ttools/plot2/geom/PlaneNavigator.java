package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Navigator for use with plane plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class PlaneNavigator implements Navigator<PlaneAspect> {

    private final double zoomFactor_;

    /**
     * Constructor.
     *
     * @param   zoomFactor   amount of zoom for one mouse wheel click
     */
    public PlaneNavigator( double zoomFactor ) {
        zoomFactor_ = zoomFactor;
    }

    public PlaneAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        return ((PlaneSurface) surface).pan( origin, evt.getPoint() );
    }

    public PlaneAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((PlaneSurface) surface)
              .zoom( evt.getPoint(), PlotUtil.toZoom( zoomFactor_, evt ) );
    }

    public PlaneAspect click( Surface surface, MouseEvent evt,
                              Iterable<double[]> dposIt ) {
        return null;
    }

    /**
     * Returns the config keys for use with this navigator.
     *
     * @return  config keys
     */
    public static ConfigKey[] getConfigKeys() {
        return new ConfigKey[] {
            StyleKeys.ZOOM_FACTOR,
        };
    }

    /**
     * Creates a navigator instance from a config map.
     * The keys defined by {@link #getConfigKeys} are used.
     *
     * @param   navConfig   configuration map
     * @return   navigator
     */
    public static PlaneNavigator createNavigator( ConfigMap navConfig ) {
        double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
        return new PlaneNavigator( zoom );
    }
}
