package uk.ac.starlink.ttools.plot2;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.Timer;

/**
 * Listener that receives mouse events and uses them in conjunction with
 * a supplied navigator to feed navigation actions to a set of one or
 * more plot surfaces.
 *
 * @author   Mark Taylor
 * @since    30 Oct 2013
 */
public abstract class NavigationListener<A>
        implements MouseListener, MouseMotionListener, MouseWheelListener {

    private final Timer decTimer_;
    private DragContext dragged_;

    /** Lifetime of an auto-cancelled navigation decoration. */
    private static final int DECORATION_AUTO_MILLIS = 500;

    /**
     * Constructor.
     */
    protected NavigationListener() {
        decTimer_ = new Timer( DECORATION_AUTO_MILLIS, new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                setDecoration( null );
            }
        } );
    }

    /**
     * Return an index labelling the plotting surface which provides
     * the context for navigation actions referenced at a given point.
     *
     * @param   pos   reference point for navigation
     * @return   numeric label for plotting surface relevant for actions
     *           at, or starting at, the given point
     */
    public abstract int getSurfaceIndex( Point pos );

    /**
     * Returns the current plotting surface corresponding to a given
     * numeric label.
     * The supplied index must be one returned from {@link #getSurfaceIndex}
     * (if not, behaviour is undefined).
     *
     * @param   isurf  surface index returned from <code>getSurfaceIndex</code>
     * @return  current plotting surface corresponding to the given index;
     *          may be null
     */
    public abstract Surface getSurface( int isurf );

    /**
     * Returns a navigator which is used to convert mouse gestures into
     * navigation actions.
     *
     * @param   isurf  surface index returned from <code>getSurfaceIndex</code>
     * @return  current navigator for indicated surface, may be null
     */
    public abstract Navigator<A> getNavigator( int isurf );

    /**
     * Returns an iterable over a sequence of data space positions,
     * which may be required to make sense of a click action.
     *
     * @param    pos   reference position for data
     * @return   iterable over data positions, may be null
     * @see   Navigator#click
     */
    public abstract Iterable<double[]> createDataPosIterable( Point pos );

    /**
     * Receives a new aspect requested by user interface actions in
     * conjunction with this object.  The supplied aspect corresponds to
     * the surface with the given index, as supplied by the
     * {@link #getSurfaceIndex} method.
     *
     * @param  isurf   label for surface to which new aspect applies
     * @param  aspect   definition of requested plot surface
     */
    protected abstract void setAspect( int isurf, A aspect );

    /**
     * Sets a decoration to display over the plot to indicate navigation
     * actions in progress.  This decoration should be displayed until
     * further notice, that is, until this method is called again with
     * a null argument.
     *
     * <p>This method is called by {@link #updateDecoration updateDecoration}.
     * It should not be called directly.
     *
     * @param   decoration  navigation decoration, or null for none
     */
    protected abstract void setDecoration( Decoration decoration );

    /**
     * Requests a change of the current navigation decoration.
     * This performs some housekeeping operations, and calls
     * {@link #setDecoration}.
     * The <code>autoCancel</code> parameter controls whether the decoration
     * will be cancelled automatically or by hand.
     * If the caller can guarantee to make a matching call with a null
     * decoration in the future, <code>autoCancel</code> may be false,
     * otherwise it should be true.
     *
     * @param  dec  new decoration
     * @param  autoCancel  if true, decoration will be automatically cancelled
     */
    public void updateDecoration( Decoration dec, boolean autoCancel ) {
        decTimer_.stop();
        setDecoration( dec );
        if ( autoCancel && dec != null ) {
            decTimer_.start();
        }
    }

    public void mousePressed( MouseEvent evt ) {

        /* Start a drag gesture. */
        Point pos = evt.getPoint();
        int isurf = getSurfaceIndex( pos );
        Surface surface = getSurface( isurf );
        dragged_ = surface == null ? null
                                   : new DragContext( isurf, surface, pos );

        /* Arrange to display the relevant decoration if there is one. */
        Navigator<A> navigator = getNavigator( isurf );
        if ( dragged_ != null && navigator != null ) {
            int ibutt = PlotUtil.getButtonDownIndex( evt );
            NavAction<A> drag0act =
                navigator.drag( dragged_.surface_, pos, ibutt,
                                dragged_.start_ );
            Decoration dragDec = drag0act == null ? null
                                                  : drag0act.getDecoration();
            if ( dragDec != null ) {
                updateDecoration( dragDec, false );
            }
        }
    }

    public void mouseDragged( MouseEvent evt ) {

        /* Reposition surface midway through drag gesture. */
        if ( dragged_ != null ) {
            Navigator<A> navigator = getNavigator( dragged_.isurf_ );
            if ( navigator != null ) {
                Point pos = evt.getPoint();
                int ibutt = PlotUtil.getButtonDownIndex( evt );
                NavAction<A> navact =
                    navigator.drag( dragged_.surface_, pos, ibutt,
                                    dragged_.start_ );
                if ( navact != null ) {
                    updateDecoration( navact.getDecoration(), false );
                    A aspect = navact.getAspect();
                    if ( aspect != null ) {
                        setAspect( dragged_.isurf_, aspect );
                    }
                }
            }
        }
    }

    public void mouseReleased( MouseEvent evt ) {

        /* Handle the end of a drag action, if one is pending. */
        if ( dragged_ != null ) {

            /* Pass on the endDrag action, if appropriate. */
            Navigator<A> navigator = getNavigator( dragged_.isurf_ );
            final NavAction<A> navact;
            if ( navigator != null ) {
                Point pos = evt.getPoint();
                int ibutt = PlotUtil.getButtonChangedIndex( evt );
                navact =
                    navigator.endDrag( dragged_.surface_, pos, ibutt,
                                       dragged_.start_ );
            }
            else {
                navact = null;
            }
            Decoration dec = navact == null ? null : navact.getDecoration();

            /* Eliminate any decorations associated with a current drag. */
            updateDecoration( dec, false );

            /* Update aspect if the endDrag produced a new one. */
            if ( navact != null ) {
                A aspect = navact.getAspect();
                if ( aspect != null ) {
                    setAspect( dragged_.isurf_, aspect );
                }
            }
        }

        /* Terminate any current drag gesture. */
        dragged_ = null;
    }

    public void mouseClicked( MouseEvent evt ) {
        Point pos = evt.getPoint();
        int ibutt = PlotUtil.getButtonChangedIndex( evt );
        if ( ibutt == 3 ) {
            int isurf = getSurfaceIndex( pos );
            Navigator<A> navigator = getNavigator( isurf );
            if ( navigator != null ) {
                handleClick( navigator, isurf, pos, ibutt,
                             createDataPosIterable( pos ) );
            }
        }
    }

    /**
     * Performs the actual work when a mouse click event is detected.
     * This method is invoked by {@link #mouseClicked mouseClicked}.
     * The default behaviour is to get a corresponding navigation action
     * from the navigator,
     * and call {@link #setAspect} and {@link #updateDecoration} accordingly.
     * However, it may be overridden by subclasses.
     *
     * @param   navigator   navigator
     * @param   isurf   surface numeric label
     * @param   pos   mouse position
     * @param   ibutt  logical mouse button index
     * @param   dposIt  iterable over points if available
     */
    protected void handleClick( Navigator<A> navigator, int isurf,
                                Point pos, int ibutt,
                                Iterable<double[]> dposIt ) {
        Surface surface = getSurface( isurf );
        NavAction<A> navact = navigator.click( surface, pos, ibutt, dposIt );
        if ( navact != null ) {
            updateDecoration( navact.getDecoration(), true );
            A aspect = navact.getAspect();
            if ( aspect != null ) {
                setAspect( isurf, aspect );
            }
        }
    }

    public void mouseWheelMoved( MouseWheelEvent evt ) {
        Point pos = evt.getPoint();
        int isurf = getSurfaceIndex( pos );
        Navigator<A> navigator = getNavigator( isurf );
        Surface surface = getSurface( isurf );
        if ( navigator != null && surface != null ) {
            int wheelrot = evt.getWheelRotation();
            NavAction<A> navact = navigator.wheel( surface, pos, wheelrot );
            if ( navact != null ) {
                updateDecoration( navact.getDecoration(), true );
                A aspect = navact.getAspect();
                if ( aspect != null ) {
                    setAspect( isurf, aspect );
                }
            }
        }
    }

    public void mouseMoved( MouseEvent evt ) {
    }
    public void mouseEntered( MouseEvent evt ) {
    }
    public void mouseExited( MouseEvent evt ) {
    }

    /**
     * Convenience method to install this listener on a graphical component.
     * This currently just calls
     * <code>addMouseListener</code>,
     * <code>addMouseMotionListener</code> and
     * <code>addMouseWheelListener</code>.
     *
     * @param   component   component to which this object should listen
     */
    public void addListeners( Component component ) {
        component.addMouseListener( this );
        component.addMouseMotionListener( this );
        component.addMouseWheelListener( this );
    }

    /**
     * Reverses the effect of {@link #addListeners addListeners}.
     *
     * @param  component  component to which this listener was previously added
     */
    public void removeListeners( Component component ) {
        component.removeMouseListener( this );
        component.removeMouseMotionListener( this );
        component.removeMouseWheelListener( this );
    }

    /**
     * Aggregates a surface index, a surface, and a start point.
     */
    private static class DragContext {
        final int isurf_;
        final Surface surface_;
        final Point start_;

        /**
         * Constructor.
         *
         * @param  isurf  numeric label for surface
         * @param  surface  surface object
         * @param  start  drag start position
         */
        DragContext( int isurf, Surface surface, Point start ) {
            isurf_ = isurf;
            surface_ = surface;
            start_ = start;
        }
    }
}
