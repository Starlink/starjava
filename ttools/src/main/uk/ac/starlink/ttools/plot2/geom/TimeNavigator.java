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

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> ZOOMAXES_KEY =
        new CombinationConfigKey( new ConfigMeta( "zoomaxes", "Zoom Axes" ),
                                  new boolean[] { true, false },
                                  new String[] { "Time", "Y" } ) {
            // Allow X as an alias for T
            @Override
            public int optCharToIndex( char c ) throws ConfigException {
                return Character.toLowerCase( c ) == 'x'
                     ? 0
                     : super.optCharToIndex( c );
            }
        };

    /**
     * Constructor.
     *
     * @param  zoomFactor  amount of zoom for one mouse wheel click
     * @param  tZoom  true iff wheel operation will zoom in horizontal direction
     * @param  yZoom  true iff wheel operation will zoom in vertical direction
     */
    public TimeNavigator( double zoomFactor, boolean tZoom, boolean yZoom ) {
        zoomFactor_ = zoomFactor;
        tZoom_ = tZoom;
        yZoom_ = yZoom;
    }

    public TimeAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        return ((TimeSurface) surface).pan( origin, evt.getPoint() );
    }

    public TimeAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((TimeSurface) surface)
              .zoom( evt.getPoint(), PlotUtil.toZoom( zoomFactor_, evt ),
                     tZoom_, yZoom_ );
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
            ZOOMAXES_KEY,
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
        boolean[] zoomFlags = config.get( ZOOMAXES_KEY );
        double zoom = config.get( StyleKeys.ZOOM_FACTOR );
        return new TimeNavigator( zoom, zoomFlags[ 0 ], zoomFlags[ 1 ] );
    }
}
