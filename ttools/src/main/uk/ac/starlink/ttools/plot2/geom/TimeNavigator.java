package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * Navigator for use with time plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class TimeNavigator implements Navigator<TimeAspect> {

    private final double zoomFactor_;
    private final boolean timeOnly_;

    /**
     * Constructor.
     *
     * @param  zoomFactor  amount of zoom for one mouse wheel click
     * @param  timeOnly   if true, only time axis is zoomed;
     *                    if false, both time and Y axes are zoomed
     */
    public TimeNavigator( double zoomFactor, boolean timeOnly ) {
        zoomFactor_ = zoomFactor;
        timeOnly_ = timeOnly;
    }

    public TimeAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        return ((TimeSurface) surface).pan( origin, evt.getPoint() );
    }

    public TimeAspect wheel( Surface surface, MouseWheelEvent evt ) {
        return ((TimeSurface) surface)
              .zoom( evt.getPoint(), PlotUtil.toZoom( zoomFactor_, evt ),
                     timeOnly_ );
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
        return new ConfigKey[ 0 ];
    }

    /**
     * Creates a navigator instance from a config map.
     * The keys defined by {@link #getConfigKeys} are used.
     *
     * @param   config   configuration map
     * @return   navigator
     */
    public static TimeNavigator createNavigator( ConfigMap config ) {
        return new TimeNavigator( 1.2, true );
    }
}
