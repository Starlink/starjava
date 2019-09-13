package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.CoordSequence;
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

    public NavAction<SkyAspect> drag( Surface surface, Point pos, int ibutt,
                                      Point origin ) {
        SkySurface ssurf = (SkySurface) surface;
        Rectangle bounds = surface.getPlotBounds();
        if ( ibutt == 3 ) {
            double fact = PlotUtil.toZoom( zoomFactor_, origin, pos, null );
            SkyAspect aspect = ssurf.zoom( origin, fact );
            Decoration dec =
                NavDecorations
               .createDragDecoration( origin, fact, fact, true, true, bounds );
            return new NavAction<SkyAspect>( aspect, dec );
        }
        else if ( ibutt == 2 ) {
            Point isoPos = toIsoPos( origin, pos, bounds );
            Decoration dec =
                NavDecorations
               .createBandDecoration( origin, isoPos, true, true, bounds );
            return new NavAction<SkyAspect>( null, dec );
        }
        else {
            SkyAspect aspect = ssurf.pan( origin, pos );
            return new NavAction<SkyAspect>( aspect, null );
        }
    }

    public NavAction<SkyAspect> endDrag( Surface surface, Point pos,
                                         int ibutt, Point origin ) {
        if ( ibutt == 2 ) {
            SkySurface ssurf = (SkySurface) surface;
            Rectangle bounds = surface.getPlotBounds();
            Point isoPos = toIsoPos( origin, pos, bounds );
            BandDecoration dec =
                NavDecorations
               .createBandDecoration( origin, isoPos, true, true, bounds );
            if ( dec != null ) {
                Rectangle target = dec.getTargetRectangle();
                Point cp = new Point( target.x + target.width / 2,
                                      target.y + target.height / 2 );
                double xf = bounds.width * 1.0 / target.width;
                double yf = bounds.height * 1.0 / target.height;
                double fact = 0.5 * ( xf + yf );
                SkyAspect aspect = ssurf.reframe( cp, fact );
                return new NavAction<SkyAspect>( aspect, null );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public NavAction<SkyAspect> wheel( Surface surface, Point pos,
                                       int wheelrot ) {
        SkySurface ssurf = (SkySurface) surface;
        double fact = PlotUtil.toZoom( zoomFactor_, wheelrot );
        SkyAspect aspect = ssurf.zoom( pos, fact );
        Decoration dec =
            NavDecorations
           .createWheelDecoration( pos, fact, fact, true, true,
                                   surface.getPlotBounds() );
        return new NavAction<SkyAspect>( aspect, dec );
    }

    public NavAction<SkyAspect> click( Surface surface, Point pos, int ibutt,
                                       Supplier<CoordSequence> dposSupplier ) {
        return null;
    }

    public Map<Gesture,String> getNavOptions( Surface surface, Point pos ) {
        Map<Gesture,String> map = new LinkedHashMap<Gesture,String>();
        map.put( Gesture.DRAG_1, "Pan" );
        map.put( Gesture.DRAG_3, "Zoom" );
        map.put( Gesture.DRAG_2, "Frame" );
        map.put( Gesture.WHEEL, "Zoom" );
        return map;
    }

    /**
     * Returns the config keys for use with this navigator.
     *
     * @return  config keys
     */
    public static ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
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

    /**
     * Returns a position related to a given (cursor) point and an origin,
     * but constrained to represent an isotropic region, in the context
     * of a given plot boundary rectangle.
     *
     * @param   origin   drag region origin
     * @param   point    drag region destination
     * @param   plotBounds  boundary of plotting area
     * @return  position resembling the input <code>point</code> but
     *          constrained so that the rectangle it forms
     *          w.r.t. <code>origin</code> represents an isotropic
     *          region of space
     */
    private static Point toIsoPos( Point origin, Point point,
                                   Rectangle plotBounds ) {
        int w = plotBounds.width;
        int h = plotBounds.height;
        int dx = point.x - origin.x;
        int dy = point.y - origin.y;
        int sx = dx >= 0 ? +1 : -1;
        int sy = dy >= 0 ? +1 : -1;
        double fx = dx * 1.0 / plotBounds.width;
        double fy = dy * 1.0 / plotBounds.height;
        double f = Math.abs( fx ) >= Math.abs( fy ) ? Math.abs( fx )
                                                    : Math.abs( fy );
        return new Point( (int) Math.round( origin.x + f * sx * w ),
                          (int) Math.round( origin.y + f * sy * h ) );
    }
}
