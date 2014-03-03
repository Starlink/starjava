package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Gesture;
import uk.ac.starlink.ttools.plot2.NavAction;
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

    public NavAction<SkyAspect> drag( Surface surface, MouseEvent evt,
                                      Point origin ) {
        SkySurface ssurf = (SkySurface) surface;
        Point pos = evt.getPoint();
        int ibutt = PlotUtil.getButtonDownIndex( evt );
        if ( ibutt == 3 ) {
            double fact = PlotUtil.toZoom( zoomFactor_, origin, pos, null );
            SkyAspect aspect = ssurf.zoom( origin, fact );
            Decoration dec =
                NavDecorations
               .createDragDecoration( origin, fact, fact, true, true,
                                      surface.getPlotBounds() );
            return new NavAction<SkyAspect>( aspect, dec );
        }
        else {
            SkyAspect aspect = ssurf.pan( origin, pos );
            return new NavAction<SkyAspect>( aspect, null );
        }
    }

    public NavAction<SkyAspect> wheel( Surface surface, MouseWheelEvent evt ) {
        SkySurface ssurf = (SkySurface) surface;
        Point pos = evt.getPoint();
        double fact = PlotUtil.toZoom( zoomFactor_, evt );
        SkyAspect aspect = ssurf.zoom( pos, fact );
        Decoration dec =
            NavDecorations
           .createWheelDecoration( pos, fact, fact, true, true,
                                   surface.getPlotBounds() );
        return new NavAction<SkyAspect>( aspect, dec );
    }

    public NavAction<SkyAspect> click( Surface surface, MouseEvent evt,
                                       Iterable<double[]> dposIt ) {
        return null;
    }

    public Map<Gesture,String> getNavOptions( Surface surface, Point pos ) {
        Map<Gesture,String> map = new LinkedHashMap<Gesture,String>();
        map.put( Gesture.DRAG_1, "Pan" );
        map.put( Gesture.DRAG_3, "Zoom" );
        map.put( Gesture.WHEEL, "Zoom" );
        return map;
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
