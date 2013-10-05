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
    private final boolean xZoom_;
    private final boolean yZoom_;
    private final boolean zZoom_;

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> ZOOMAXES_KEY =
        new CombinationConfigKey( new ConfigMeta( "zoomaxes", "Zoom Axes" ),
                                  new String[] { "X", "Y", "Z" } );

    /**
     * Constructor.
     *
     * @param  zoomFactor  amount of zoom for one mouse wheel click
     * @param  xZoom   true iff wheel operation will zoom in X direction
     * @param  yZoom   true iff wheel operation will zoom in Y direction
     * @param  zZoom   true iff wheel operation will zoom in Z direction
     */
    public CubeNavigator( double zoomFactor,
                          boolean xZoom, boolean yZoom, boolean zZoom ) {
        zoomFactor_ = zoomFactor;
        xZoom_ = xZoom;
        yZoom_ = yZoom;
        zZoom_ = zZoom;
    }

    public CubeAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        return ((CubeSurface) surface).pan( origin, evt.getPoint() );
    }

    public CubeAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((CubeSurface) surface)
              .zoom( PlotUtil.toZoom( zoomFactor_, evt ),
                     xZoom_, yZoom_, zZoom_ );
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
        return new CubeNavigator( zoom, zoomFlags[ 0 ], zoomFlags[ 1 ],
                                  zoomFlags[ 2 ] );
    }
}
