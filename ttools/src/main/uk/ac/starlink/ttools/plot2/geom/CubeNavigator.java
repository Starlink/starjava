package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.CombinationConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Navigator for use with cube plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class CubeNavigator implements Navigator<CubeAspect> {

    private final double zoomFactor_;
    private final boolean[] axisFlags_;

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> ZOOMAXES_KEY =
        new CombinationConfigKey( new ConfigMeta( "zoomaxes", "Zoom Axes" ),
                                  null, new String[] { "X", "Y", "Z" },
                                  "Auto" );

    /**
     * Constructor.
     *
     * @param  zoomFactor  amount of zoom for one mouse wheel click
     * @param  axisFlags  3-element array of flags for whether a zoom
     *                    operation will zoom in X, Y, Z directions;
     *                    if null, drag zoom will be along 2 facing axes
     */
    public CubeNavigator( double zoomFactor, boolean[] axisFlags ) {
        zoomFactor_ = zoomFactor;
        axisFlags_ = axisFlags;
    }

    public CubeAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        CubeSurface csurf = (CubeSurface) surface;
        Point point = evt.getPoint();
        if ( PlotUtil.isZoomDrag( evt ) ) {
            return axisFlags_ == null
                 ? csurf.pointZoom( origin,
                                    PlotUtil.toZoom( zoomFactor_, origin,
                                                     point, false ),
                                    PlotUtil.toZoom( zoomFactor_, origin,
                                                     point, true ) )
                 : csurf.centerZoom( PlotUtil.toZoom( zoomFactor_, origin,
                                                      point, null ),
                                     axisFlags_[ 0 ],
                                     axisFlags_[ 1 ],
                                     axisFlags_[ 2 ] );
        }
        else {
            return csurf.pan( origin, point );
        }
    }

    public CubeAspect wheel( Surface surface, MouseWheelEvent evt ) {
        final boolean xZoom;
        final boolean yZoom;
        final boolean zZoom;
        if ( axisFlags_ == null ) {
            xZoom = true;
            yZoom = true;
            zZoom = true;
        }
        else {
            xZoom = axisFlags_[ 0 ];
            yZoom = axisFlags_[ 1 ];
            zZoom = axisFlags_[ 2 ];
        }
        return ((CubeSurface) surface)
              .centerZoom( PlotUtil.toZoom( zoomFactor_, evt ),
                           xZoom, yZoom, zZoom );
    }

    public CubeAspect click( Surface surface, MouseEvent evt,
                             Iterable<double[]> dposIt ) {
        double[] dpos = surface.graphicsToData( evt.getPoint(), dposIt );
        return dpos == null ? null : ((CubeSurface) surface).center( dpos );
    }

    /**
     * Returns the config keys for use with this navigator.
     *
     * @param   isIso  whether navigator will be configured for isotropic mode
     * @return  config keys
     */
    public static ConfigKey[] getConfigKeys( boolean isIso ) {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        if ( ! isIso ) {
            list.add( ZOOMAXES_KEY );
        }
        list.add( StyleKeys.ZOOM_FACTOR );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    /**
     * Creates a navigator instance from a config map.
     * The keys defined by {@link #getConfigKeys} are used.
     *
     * @param   isIso  whether navigator will be configured for isotropic mode
     * @param   config   configuration map
     * @return   navigator
     */
    public static CubeNavigator createNavigator( boolean isIso,
                                                 ConfigMap config ) {
        double zoom = config.get( StyleKeys.ZOOM_FACTOR );
        boolean[] zoomFlags = isIso ? new boolean[] { true, true, true }
                                    : config.get( ZOOMAXES_KEY );
        return new CubeNavigator( zoom, zoomFlags );
    }
}
