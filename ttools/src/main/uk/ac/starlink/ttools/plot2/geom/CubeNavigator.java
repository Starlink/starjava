package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Navigator for use with cube plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class CubeNavigator implements Navigator<CubeAspect> {

    private final double zoomFactor_;

    /**
     * Constructor.
     *
     * @param  zoomFactor  amount of zoom for one mouse wheel click
     */
    public CubeNavigator( double zoomFactor ) {
        zoomFactor_ = zoomFactor;
    }

    public CubeAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        return ((CubeSurface) surface).pan( origin, evt.getPoint() );
    }

    public CubeAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((CubeSurface) surface)
              .zoom( PlotUtil.toZoom( zoomFactor_, evt ) );
    }

    public CubeAspect click( Surface surface, MouseEvent evt,
                             Iterable<double[]> dposIt ) {
        double[] dpos = surface.graphicsToData( evt.getPoint(), dposIt );
        return dpos == null ? null : ((CubeSurface) surface).center( dpos );
    }

    /**
     * Returns the config keys for use with this navigator.
     *
     * @return  config keys
     */
    public static ConfigKey[] getConfigKeys() {
        return new ConfigKey[ 0 ];
    }

    /**
     * Creates a navigator instance from a config map.
     * The keys defined by {@link #getConfigKeys} are used.
     *
     * @param   config   configuration map
     * @return   navigator
     */
    public static CubeNavigator createNavigator( ConfigMap config ) {
        return new CubeNavigator( 1.2 );
    }
}
