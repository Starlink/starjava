package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gesture;
import uk.ac.starlink.ttools.plot2.NavAction;
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
        createNavAxesKey();

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

    public NavAction<TimeAspect> drag( Surface surface, Point point, int ibutt,
                                       Point origin ) {
        boolean[] useFlags =
            PlaneNavigator.getAxisNavFlags( surface, origin, tPan_, yPan_ );
        TimeSurface tsurf = (TimeSurface) surface;
        boolean tUse = useFlags[ 0 ];
        boolean yUse = useFlags[ 1 ];
        Rectangle plotBounds = surface.getPlotBounds();
        if ( ibutt == 3 ) {
            double tf = tUse
                      ? PlotUtil.toZoom( zoomFactor_, origin, point, false )
                      : 1;
            double yf = yUse
                      ? PlotUtil.toZoom( zoomFactor_, origin, point, true )
                      : 1;
            TimeAspect aspect = tsurf.zoom( origin, tf, yf );
            Decoration dec =
                NavDecorations
               .createDragDecoration( origin, tf, yf, tUse, yUse, plotBounds );
            return new NavAction<TimeAspect>( aspect, dec );
        }
        else if ( ibutt == 2 ) {
            Decoration dec =
                NavDecorations
               .createBandDecoration( origin, point, tUse, yUse, plotBounds ); 
            return new NavAction<TimeAspect>( null, dec );
        }
        else {
            TimeAspect aspect = tsurf.pan( origin, point, tUse, yUse );
            return new NavAction<TimeAspect>( aspect, null );
        }
    }

    public NavAction<TimeAspect> endDrag( Surface surface, Point pos,
                                          int ibutt, Point origin ) {
        if ( ibutt == 2 ) {
            boolean[] useFlags =
                PlaneNavigator.getAxisNavFlags( surface, origin, tPan_, yPan_ );
            TimeSurface tsurf = (TimeSurface) surface;
            boolean tUse = useFlags[ 0 ];
            boolean yUse = useFlags[ 1 ];
            Rectangle plotBounds = surface.getPlotBounds();
            BandDecoration dec =
                NavDecorations
               .createBandDecoration( origin, pos, tUse, yUse, plotBounds );
            if ( dec != null ) {
                TimeAspect aspect = tsurf.reframe( dec.getTargetRectangle() );
                return new NavAction<TimeAspect>( aspect, null );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public NavAction<TimeAspect> wheel( Surface surface, Point pos,
                                        int wheelrot ) {
        boolean[] useFlags =
            PlaneNavigator.getAxisNavFlags( surface, pos, tZoom_, yZoom_ );
        double zfact = PlotUtil.toZoom( zoomFactor_, wheelrot );
        double tf = useFlags[ 0 ] ? zfact : 1;
        double yf = useFlags[ 1 ] ? zfact : 1;
        TimeAspect aspect = ((TimeSurface) surface).zoom( pos, tf, yf );
        Decoration dec =
            NavDecorations
           .createWheelDecoration( pos, tf, yf, useFlags[ 0 ], useFlags[ 1 ],
                                   surface.getPlotBounds() );
        return new NavAction<TimeAspect>( aspect, dec );
    }

    public NavAction<TimeAspect> click( Surface surface, Point pos, int ibutt,
                                        Iterable<double[]> dposIt ) {
        return null;
    }

    public Map<Gesture,String> getNavOptions( Surface surface, Point pos ) {
        boolean[] useFlags =
            PlaneNavigator.getAxisNavFlags( surface, pos, tZoom_, yZoom_ );
        return getNavOptions( useFlags[ 0 ], useFlags[ 1 ] );
    }

    /**
     * Returns a description of available navigator options for a time plot,
     * given X/Y zoom flags.
     *
     * @param  tUse  true iff X zoom is in effect
     * @param  yUse  true iff Y zoom is in effect
     * @return  mapping of gestures to navigation action descriptions
     */
    public static Map<Gesture,String> getNavOptions( boolean tUse,
                                                     boolean yUse ) {
        final String isoTxt;
        final String freeTxt;
        if ( tUse && yUse ) {
            freeTxt = "t/Y";
            isoTxt = "Iso";
        }
        else {
            if ( tUse ) {
                freeTxt = "t  ";
            }
            else if ( yUse ) {
                freeTxt = "Y  ";
            }
            else {
                freeTxt = "   ";
            }
            isoTxt = freeTxt;
        }
        Map<Gesture,String> map = new LinkedHashMap<Gesture,String>();
        map.put( Gesture.DRAG_1, "Pan " + freeTxt );
        map.put( Gesture.DRAG_3, "Stretch " + freeTxt );
        map.put( Gesture.DRAG_2, "Frame " + freeTxt );
        map.put( Gesture.WHEEL, "Zoom " + isoTxt );
        return map;
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
     * Returns a config key for selecting which axes of a time plot
     * will be pan/zoomable.
     *
     * @return   config key
     */
    private static CombinationConfigKey createNavAxesKey() {
        ConfigMeta meta = new ConfigMeta( "navaxes", "Pan/Zoom Axes" );
        meta.setStringUsage( "t|y|ty" );
        meta.setShortDescription( "Axes affected by pan/zoom" );
        meta.setXmlDescription( new String[] {
            "<p>Determines the axes which are affected by",
                "the interactive navigation actions (pan and zoom).",
                "The default is <code>t</code>",
                "which means that the various mouse gestures",
                "will provide panning and zooming in the Time direction only.",
                "However, if it is set to <code>ty</code>",
                "mouse actions will affect both the horizontal and vertical",
                "axes.",
            "</p>",
        } );

        /* Allow X as an alias for T. */
        CombinationConfigKey key =
                new CombinationConfigKey( meta, new boolean[] { true, false },
                                          new String[] { "Time", "Y" }, null ) {
            @Override
            public int optCharToIndex( char c ) throws ConfigException {
                return Character.toLowerCase( c ) == 'x'
                     ? 0
                     : super.optCharToIndex( c );
            }
        };
        return key;
    }
}
