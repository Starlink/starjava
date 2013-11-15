package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Navigator for use with sky plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class SkyNavigator implements Navigator<SkyAspect> {

    private final double zoomFactor_;

    /**
     * Constructor.
     *
     * @param   zoomFactor   amount of zoom for one mouse wheel click
     */
    public SkyNavigator( double zoomFactor ) {
        zoomFactor_ = zoomFactor;
    }

    public SkyAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        SkySurface ssurf = (SkySurface) surface;
        return PlotUtil.isZoomDrag( evt )
             ? ssurf.zoom( origin, PlotUtil.toZoom( zoomFactor_, origin,
                                                    evt.getPoint(), null ) )
             : ssurf.pan( origin, evt.getPoint() );
    }

    public SkyAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((SkySurface) surface)
              .zoom( evt.getPoint(), PlotUtil.toZoom( zoomFactor_, evt ) );
    }

    public SkyAspect click( Surface surface, MouseEvent evt,
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
    public static SkyNavigator createNavigator( ConfigMap navConfig ) {
        double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
        return new SkyNavigator( zoom );
    }
}
