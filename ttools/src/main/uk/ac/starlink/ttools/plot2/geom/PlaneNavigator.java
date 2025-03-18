package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import uk.ac.starlink.ttools.plot2.CoordSequence;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gesture;
import uk.ac.starlink.ttools.plot2.NavAction;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Surface;

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
    private final double xAnchor_;
    private final double yAnchor_;

    /**
     * Constructor.
     *
     * @param   zoomFactor   amount of zoom for one mouse wheel click
     * @param   xZoom   true iff wheel operation will zoom in X direction
     * @param   yZoom   true iff wheel operation will zoom in Y direction
     * @param   xPan    true iff drag operation will pan in X direction
     * @param   yPan    true iff drag operation will pan in Y direction
     * @param   xAnchor  data value to pin X coordinate at during zooms;
     *                   NaN for no anchor
     * @param   yAnchor  data value to pin Y coordinate at during zooms;
     *                   NaN for no anchor
     */
    public PlaneNavigator( double zoomFactor, boolean xZoom, boolean yZoom,
                           boolean xPan, boolean yPan,
                           double xAnchor, double yAnchor ) {
        zoomFactor_ = zoomFactor;
        xZoom_ = xZoom;
        yZoom_ = yZoom;
        xPan_ = xPan;
        yPan_ = yPan;
        xAnchor_ = xAnchor;
        yAnchor_ = yAnchor;
    }

    public NavAction<PlaneAspect> drag( Surface surface, Point point,
                                        int ibutt, Point origin ) {
        boolean[] useFlags = getAxisNavFlags( surface, origin, xPan_, yPan_ );
        PlaneSurface psurf = (PlaneSurface) surface;
        boolean xUse = useFlags[ 0 ];
        boolean yUse = useFlags[ 1 ];
        Rectangle plotBounds = surface.getPlotBounds();
        if ( ibutt == 3 ) {
            int[] offs = getAnchorOffsets( psurf, origin );
            Point g0 = new Point( origin.x + offs[ 0 ], origin.y + offs[ 1 ] );
            Point gp = new Point( point.x + offs[ 0 ], point.y + offs[ 1 ] );
            double xf = useFlags[ 0 ]
                      ? PlotUtil.toZoom( zoomFactor_, g0, gp, false )
                      : 1;
            double yf = useFlags[ 1 ]
                      ? PlotUtil.toZoom( zoomFactor_, g0, gp, true )
                      : 1;
            PlaneAspect aspect = psurf.zoom( g0, xf, yf );
            Decoration dec =
                NavDecorations
               .createDragDecoration( g0, xf, yf, xUse, yUse, plotBounds );
            return new NavAction<PlaneAspect>( aspect, dec );
        }
        else if ( ibutt == 2 ) {
            Decoration dec =
                NavDecorations
               .createBandDecoration( origin, point, xUse, yUse, plotBounds );
            return new NavAction<PlaneAspect>( null, dec );
        }
        else {
            PlaneAspect aspect = psurf.pan( origin, point, xUse, yUse );
            return new NavAction<PlaneAspect>( aspect, null );
        }
    }

    public NavAction<PlaneAspect> endDrag( Surface surface, Point pos,
                                           int ibutt, Point origin ) {
        if ( ibutt == 2 ) {
            boolean[] useFlags =
                getAxisNavFlags( surface, origin, xPan_, yPan_ );
            PlaneSurface psurf = (PlaneSurface) surface;
            boolean xUse = useFlags[ 0 ];
            boolean yUse = useFlags[ 1 ];
            Rectangle bounds = surface.getPlotBounds();
            BandDecoration dec =
                NavDecorations
               .createBandDecoration( origin, pos, xUse, yUse, bounds );
            if ( dec != null ) {
                PlaneAspect aspect = psurf.reframe( dec.getTargetRectangle() );
                return new NavAction<PlaneAspect>( aspect, null );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public NavAction<PlaneAspect> wheel( Surface surface, Point pos,
                                         int wheelrot ) {
        PlaneSurface psurf = (PlaneSurface) surface;
        boolean[] useFlags = getAxisNavFlags( surface, pos, xZoom_, yZoom_ );
        double zfact = PlotUtil.toZoom( zoomFactor_, wheelrot );
        double xf = useFlags[ 0 ] ? zfact : 1;
        double yf = useFlags[ 1 ] ? zfact : 1;
        int[] offs = getAnchorOffsets( psurf, pos );
        Point gp = new Point( pos.x + offs[ 0 ], pos.y + offs[ 1 ] );
        PlaneAspect aspect = psurf.zoom( gp, xf, yf );
        Decoration dec =
            NavDecorations
           .createWheelDecoration( gp, xf, yf, useFlags[ 0 ], useFlags[ 1 ],
                                   surface.getPlotBounds() );
        return new NavAction<PlaneAspect>( aspect, dec );
    }

    public NavAction<PlaneAspect> click( Surface surface, Point pos, int ibutt,
                                         Supplier<CoordSequence> dposSup ) {
        return null;
    }

    public Map<Gesture,String> getNavOptions( Surface surface, Point pos ) {
        boolean[] useFlags = getAxisNavFlags( surface, pos, xPan_, yPan_ );
        boolean xUse = useFlags[ 0 ];
        boolean yUse = useFlags[ 1 ];
        final String isoTxt;
        final String freeTxt;
        if ( xUse && yUse ) {
            freeTxt = "X/Y";
            isoTxt = "Iso";
        }
        else {
            if ( xUse ) {
               freeTxt = "X ";
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
     * Calculates offsets to a reference point required to achieve
     * anchoring of zoom operations at the X/Y anchor values set for this
     * navigator.  If no anchor values are set, the offsets will be zero.
     *
     * @param  surface  current surface
     * @param  refpos   reference graphics position on submitted surface
     * @return  2-element array giving X,Y graphics coordinate offsets to
     *          refpos for anchoring
     */
    private int[] getAnchorOffsets( PlaneSurface surface, Point refpos ) {
        double[] d0 = surface
                     .graphicsToData( surface.getPlotBounds().getLocation(),
                                      null );
        Point2D.Double pc = new Point2D.Double();
        Scale[] scales = surface.getScales();
        int xoff = ! Double.isNaN( xAnchor_ ) &&
                   ( xAnchor_ > 0 || ! scales[ 0 ].isPositiveDefinite() ) &&
                   surface.dataToGraphics( new double[] { xAnchor_, d0[ 1 ] },
                                           false, pc ) &&
                   PlotUtil.isPointFinite( pc )
                 ? (int) Math.round( pc.x - refpos.x )
                 : 0;
        int yoff = ! Double.isNaN( yAnchor_ ) &&
                   ( yAnchor_ > 0 || ! scales[ 1 ].isPositiveDefinite() ) &&
                   surface.dataToGraphics( new double[] { d0[ 0 ], yAnchor_ },
                                           false, pc ) &&
                   PlotUtil.isPointFinite( pc )
                 ? (int) Math.round( pc.y - refpos.y )
                 : 0;
        return new int[] { xoff, yoff };
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
}
