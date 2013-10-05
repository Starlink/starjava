package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.CombinationConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Navigator for use with plane plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class PlaneNavigator implements Navigator<PlaneAspect> {

    private final double zoomFactor_;
    private final boolean xZoom_;
    private final boolean yZoom_;

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> ZOOMAXES_KEY =
        new CombinationConfigKey( new ConfigMeta( "zoomaxes", "Zoom Axes" ),
                                  new String[] { "X", "Y" } );

    /**
     * Constructor.
     *
     * @param   zoomFactor   amount of zoom for one mouse wheel click
     * @param   xZoom   true iff wheel operation will zoom in X direction
     * @param   yZoom   true iff wheel operation will zoom in Y direction
     */
    public PlaneNavigator( double zoomFactor, boolean xZoom, boolean yZoom ) {
        zoomFactor_ = zoomFactor;
        xZoom_ = xZoom;
        yZoom_ = yZoom;
    }

    public PlaneAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        return ((PlaneSurface) surface).pan( origin, evt.getPoint() );
    }

    public PlaneAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((PlaneSurface) surface)
              .zoom( evt.getPoint(), PlotUtil.toZoom( zoomFactor_, evt ),
                     xZoom_, yZoom_ );
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
            ZOOMAXES_KEY,
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
        boolean[] zoomFlags = navConfig.get( ZOOMAXES_KEY );
        double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
        return new PlaneNavigator( zoom, zoomFlags[ 0 ], zoomFlags[ 1 ] );
    }
}
