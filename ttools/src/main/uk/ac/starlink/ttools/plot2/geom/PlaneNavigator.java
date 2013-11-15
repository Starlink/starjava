package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.CombinationConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Navigator for use with plane plot.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2013
 */
public class PlaneNavigator implements Navigator<PlaneAspect> {

    private final double zoomFactor_;
    private final boolean xZoom_;
    private final boolean yZoom_;
    private final boolean xPan_;
    private final boolean yPan_;

    /** Config key to select which axes zoom will operate on. */
    public static final ConfigKey<boolean[]> NAVAXES_KEY =
        new CombinationConfigKey( new ConfigMeta( "navaxes", "Pan/Zoom Axes" ),
                                  new String[] { "X", "Y" } );

    /**
     * Constructor.
     *
     * @param   zoomFactor   amount of zoom for one mouse wheel click
     * @param   xZoom   true iff wheel operation will zoom in X direction
     * @param   yZoom   true iff wheel operation will zoom in Y direction
     * @param   xPan    true iff drag operation will pan in X direction
     * @param   yPan    true iff drag operation will pan in Y direction
     */
    public PlaneNavigator( double zoomFactor, boolean xZoom, boolean yZoom,
                           boolean xPan, boolean yPan ) {
        zoomFactor_ = zoomFactor;
        xZoom_ = xZoom;
        yZoom_ = yZoom;
        xPan_ = xPan;
        yPan_ = yPan;
    }

    public PlaneAspect drag( Surface surface, MouseEvent evt, Point origin ) {
        boolean[] useFlags = getAxisNavFlags( surface, origin, xPan_, yPan_ );
        PlaneSurface psurf = (PlaneSurface) surface;
        Point point = evt.getPoint();
        if ( PlotUtil.isZoomDrag( evt ) ) {
            return psurf
                  .zoom( origin,
                         useFlags[ 0 ] ? PlotUtil.toZoom( zoomFactor_, origin,
                                                          point, false )
                                       : 1,
                         useFlags[ 1 ] ? PlotUtil.toZoom( zoomFactor_, origin,
                                                          point, true )
                                       : 1 );
        }
        else {
            return psurf.pan( origin, point, useFlags[ 0 ], useFlags[ 1 ] );
        }
    }

    public PlaneAspect wheel( Surface surface, MouseWheelEvent evt ) {
        Point pos = evt.getPoint();
        boolean[] useFlags = getAxisNavFlags( surface, pos, xZoom_, yZoom_ );
        double zfact = PlotUtil.toZoom( zoomFactor_, evt );
        return ((PlaneSurface) surface)
              .zoom( pos, useFlags[ 0 ] ? zfact : 1,
                          useFlags[ 1 ] ? zfact : 1 );
    }

    public PlaneAspect click( Surface surface, MouseEvent evt,
                              Iterable<double[]> dposIt ) {
        return null;
    }

    /**
     * Determines which axes navigation should be performed on.
     * Navigation may be active by default on zero or more axes,
     * and if the position is within the plot bounds these defaults
     * are used.  However, if the position is outside the plot bounds
     * and alongside one of the axes, this overrides any default.
     *
     * @param  surface  plotting surface
     * @param  pos   context position for mouse
     * @param  xFlag  whether navigation on X axis is active by default
     * @param  yFlag  whether navigation on Y axis is active by default
     * @return  2-element flag array; whether navigation
     *          should be performed on (X,Y) axis
     */
    public static boolean[] getAxisNavFlags( Surface surface, Point pos,
                                             boolean xFlag, boolean yFlag ) {
        Rectangle bounds = surface.getPlotBounds();
        boolean inX = pos.x >= bounds.x && pos.x <= bounds.x + bounds.width;
        boolean inY = pos.y >= bounds.y && pos.y <= bounds.y + bounds.height;
        if ( inX && inY ) {
            return new boolean[] { xFlag, yFlag };
        }
        else if ( inX && ! inY ) {
            return new boolean[] { true, false };
        }
        else if ( inY && ! inX ) {
            return new boolean[] { false, true };
        }
        else {
            return new boolean[] { false, false };
        }
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
     * @param   navConfig   configuration map
     * @return   navigator
     */
    public static PlaneNavigator createNavigator( ConfigMap navConfig ) {
        boolean[] navFlags = navConfig.get( NAVAXES_KEY );
        boolean xnav = navFlags[ 0 ];
        boolean ynav = navFlags[ 1 ];
        double zoom = navConfig.get( StyleKeys.ZOOM_FACTOR );
        return new PlaneNavigator( zoom, xnav, ynav, xnav, ynav );
    }
}
