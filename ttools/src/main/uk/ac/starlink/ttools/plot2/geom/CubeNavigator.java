package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gesture;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.NavAction;
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
    private static final char[] XYZ = new char[] { 'X', 'Y', 'Z' };

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> ZOOMAXES_KEY =
        new CombinationConfigKey(
            new ConfigMeta( "zoomaxes", "Zoom Axes" )
           .setShortDescription( "Axes affected by zooming" )
           .setXmlDescription( new String[] {
                "<p>Determines which axes are affected by zoom navigation",
                "actions.",
                "</p>",
                "<p>If no value is supplied (the default),",
                "the mouse wheel zooms around the center of the cube,",
                "and right-button (or CTRL-) drag zooms in the two dimensions",
                "most closely aligned with the plane of the screen,",
                "with the reference position set by the initial position",
                "of the mouse.",
                "</p>",
                "<p>If this value is set",
                "(legal values are",
                "<code>x</code>, <code>y</code>, <code>z</code>,",
                "<code>xy</code>, <code>yz</code>, <code>xz</code>",
                "and <code>xyz</code>)",
                "then all zoom operations are around the cube center",
                "and affect the axes named.",
                "</p>",
            } )
           .setStringUsage( "[[x][y][z]]" )
        , null, new String[] { "X", "Y", "Z" }, "Auto" );

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

    public NavAction<CubeAspect> drag( Surface surface, Point pos, int ibutt,
                                       Point origin ) {
        CubeSurface csurf = (CubeSurface) surface;
        if ( ibutt == 1 ) {
            CubeAspect aspect = csurf.pan( origin, pos );
            return new NavAction<CubeAspect>( aspect, null );
        }
        else if ( ibutt == 2 ) {
            CubeAspect aspect = csurf.pointPan( origin, pos );
            Decoration dec =
                NavDecorations3D.create2dPanDecoration( csurf, pos );
            return new NavAction<CubeAspect>( aspect, dec );
        }
        else if ( ibutt == 3 ) {
            if ( axisFlags_ == null ) {
                double xf = PlotUtil.toZoom( zoomFactor_, origin, pos, false );
                double yf = PlotUtil.toZoom( zoomFactor_, origin, pos, true );
                CubeAspect aspect = csurf.pointZoom( origin, xf, yf );
                Decoration dec =
                    NavDecorations3D
                   .create2dZoomDecoration( csurf, origin, xf, yf );
                return new NavAction<CubeAspect>( aspect, dec );
            }
            else {
                double fact = PlotUtil.toZoom( zoomFactor_, origin, pos, null );
                CubeAspect aspect = csurf.centerZoom( fact, axisFlags_ );
                Decoration dec =
                    NavDecorations3D
                   .createCenterDragDecoration( csurf, fact, axisFlags_ );
                return new NavAction<CubeAspect>( aspect, dec );
            }
        }
        else {
            assert false;
            return null;
        }
    }

    public NavAction<CubeAspect> endDrag( Surface surface, Point pos,
                                          int ibutt, Point origin ) {
        return null;
    }

    public NavAction<CubeAspect> wheel( Surface surface, Point pos,
                                        int wheelrot ) {
        final boolean xZoom;
        final boolean yZoom;
        final boolean zZoom;
        boolean[] useFlags = axisFlags_ == null
                           ? new boolean[] { true, true, true }
                           : axisFlags_;
        CubeSurface csurf = (CubeSurface) surface;
        double fact = PlotUtil.toZoom( zoomFactor_, wheelrot );
        CubeAspect aspect = csurf.centerZoom( fact, useFlags );
        Decoration dec =
            NavDecorations3D
           .createCenterWheelDecoration( csurf, fact, useFlags );
        return new NavAction<CubeAspect>( aspect, dec );
    }

    public NavAction<CubeAspect> click( Surface surface, Point pos, int ibutt,
                                        Iterable<double[]> dposIt ) {
        CubeSurface csurf = (CubeSurface) surface;
        double[] dpos = surface.graphicsToData( pos, dposIt );
        if ( dpos == null ) {
            return null;
        }
        else {
            CubeAspect aspect = ((CubeSurface) surface).center( dpos );
            Decoration dec =
                NavDecorations3D.createRecenterDecoration( csurf, pos );
            return new NavAction<CubeAspect>( aspect, dec );
        }
    }

    public Map<Gesture,String> getNavOptions( Surface surface, Point pos ) {
        int[] dirs = ((CubeSurface) surface).getScreenDirections();
        String planeTxt = new String( new char[] { XYZ[ dirs[ 0 ] ], '/',
                                                   XYZ[ dirs[ 1 ] ] } ); 
        final String wzoomTxt;
        if ( axisFlags_ == null ||
             ( axisFlags_[ 0 ] && axisFlags_[ 1 ] && axisFlags_[ 2 ] ) ) {
            wzoomTxt = " Iso";
        }
        else {
            int leng = 4;
            StringBuffer wzBuf = new StringBuffer( leng );
            for ( int idim = 0; idim < 3; idim++ ) {
                if ( axisFlags_[ idim ] ) {
                    if ( wzBuf.length() > 0 ) {
                        wzBuf.append( '/' );
                    }
                    wzBuf.append( XYZ[ idim ] );
                }
            }
            wzBuf.insert( 0, ' ' );
            while ( wzBuf.length() < leng ) {
                wzBuf.append( ' ' );
            }
            wzoomTxt = wzBuf.toString();;
            assert wzoomTxt.length() == leng;
        }
        
        Map<Gesture,String> map = new LinkedHashMap<Gesture,String>();
        map.put( Gesture.DRAG_1, "Rotate" );
        map.put( Gesture.WHEEL, "Zoom Center" + wzoomTxt );
        map.put( Gesture.CLICK_3, "Re-center" );
        map.put( Gesture.DRAG_2, "Pan " + planeTxt );
        map.put( Gesture.DRAG_3, ( axisFlags_ == null
                                 ? "Zoom " + planeTxt
                                 : "Zoom Center" + wzoomTxt ) );
        return map;
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
