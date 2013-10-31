package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.CombinationConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Navigator for use with time plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class TimeNavigator implements Navigator<TimeAspect> {

    private final double zoomFactor_;
    private final boolean tZoom_;
    private final boolean yZoom_;
    private final boolean tPan_;
    private final boolean yPan_;

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> NAVAXES_KEY =
        new AxisCombinationConfigKey( new ConfigMeta( "navaxes",
                                                      "Pan/Zoom Axes" ),
                                      true, false );

    /**
     * Constructor.
     *
     * @param  zoomFactor  amount of zoom for one mouse wheel click
     * @param  tZoom  true iff wheel operation will zoom in horizontal direction
     * @param  yZoom  true iff wheel operation will zoom in vertical direction
     * @param  tPan   true iff drag operation will pan in horizontal direction
     * @param  yPan   true iff drag operation will pan in vertical direction
     */
    public TimeNavigator( double zoomFactor, boolean tZoom, boolean yZoom,
                          boolean tPan, boolean yPan ) {
        zoomFactor_ = zoomFactor;
        tZoom_ = tZoom;
        yZoom_ = yZoom;
        tPan_ = tPan;
        yPan_ = yPan;
    }

    public TimeAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        boolean[] useFlags =
            PlaneNavigator.getAxisNavFlags( surface, origin, tPan_, yPan_ );
        TimeSurface tsurf = (TimeSurface) surface;
        Point point = evt.getPoint();
        if ( PlotUtil.isZoomDrag( evt ) ) {
            return tsurf
                  .zoom( origin,
                         useFlags[ 0 ] ? PlotUtil.toZoom( zoomFactor_, origin,
                                                          point, false )
                                       : 1,
                         useFlags[ 1 ] ? PlotUtil.toZoom( zoomFactor_, origin,
                                                          point, true )
                                       : 1 );
        }
        else {
            return ((TimeSurface) surface)
                  .pan( origin, point, useFlags[ 0 ], useFlags[ 1 ] );
        }
    }

    public TimeAspect wheel( Surface surface, MouseWheelEvent evt ) {
        Point pos = evt.getPoint();
        boolean[] useFlags =
            PlaneNavigator.getAxisNavFlags( surface, pos, tZoom_, yZoom_ );
        double zfact = PlotUtil.toZoom( zoomFactor_, evt );
        return ((TimeSurface) surface)
              .zoom( pos, useFlags[ 0 ] ? zfact : 1,
                          useFlags[ 1 ] ? zfact : 1 );
    }

    public TimeAspect click( Surface surface, MouseEvent evt,
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
            NAVAXES_KEY,
            StyleKeys.ZOOM_FACTOR,
        };
    }

    /**
     * Creates a navigator instance from a config map.
     * The keys defined by {@link #getConfigKeys} are used.
     *
     * @param   config   configuration map
     * @return   navigator
     */
    public static TimeNavigator createNavigator( ConfigMap config ) {
        boolean[] navFlags = config.get( NAVAXES_KEY );
        boolean tnav = navFlags[ 0 ];
        boolean ynav = navFlags[ 1 ];
        double zoom = config.get( StyleKeys.ZOOM_FACTOR );
        return new TimeNavigator( zoom, tnav, ynav, tnav, ynav );
    }

    /**
     * ConfigKey for selecting a combination (0, 1 or 2) of the axes
     * of a time plot.
     */
    private static class AxisCombinationConfigKey extends CombinationConfigKey {

        /**
         * Constructor.
         *
         * @param  metadata
         * @param  tDflt    true to include time axis by default
         * @param  yDflt    true to include Y axis by default
         */
        AxisCombinationConfigKey( ConfigMeta meta,
                                  boolean tDflt, boolean yDflt) {
            super( meta, new boolean[] { tDflt, yDflt },
                   new String[] { "Time", "Y" }, false );
        }

        /**
         * Allow X as an alias for T
         */
        @Override
        public int optCharToIndex( char c ) throws ConfigException {
            return Character.toLowerCase( c ) == 'x'
                 ? 0
                 : super.optCharToIndex( c );
        }
    }
}
