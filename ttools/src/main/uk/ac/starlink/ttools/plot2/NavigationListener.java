package uk.ac.starlink.ttools.plot2;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Listener that receives mouse events and uses them in conjunction with
 * a supplied navigator to feed navigation actions to a plot surface.
 *
 * @author   Mark Taylor
 * @since    30 Oct 2013
 */
public abstract class NavigationListener<A>
        implements MouseListener, MouseMotionListener, MouseWheelListener {
    private Surface dragSurface_;
    private Point startPoint_;

    /**
     * Returns a plotting surface which provides the context for navigation
     * actions.
     *
     * @return   current plotting surface, may be null
     */
    public abstract Surface getSurface();

    /**
     * Returns a navigator which is used to convert mouse gestures into
     * navigation actions.
     *
     * @return  current navigator, may be null
     */
    public abstract Navigator<A> getNavigator();

    /**
     * Returns an iterable over a sequence of data space positions,
     * which may be required to make sense of a click action.
     *
     * @return   iterable over data positions, may be null
     * @see   Navigator#click
     */
    public abstract Iterable<double[]> createDataPosIterable();

    /**
     * Receives a new aspect requested by user interface actions in
     * conjunction with this object.
     *
     * @param  aspect   definition of requested plot surface
     */
    public abstract void setAspect( A aspect );

    public void mousePressed( MouseEvent evt ) {

        /* Start a drag gesture. */
        startPoint_ = evt.getPoint();
        dragSurface_ = getSurface();
    }

    public void mouseDragged( MouseEvent evt ) {

        /* Reposition surface midway through drag gesture. */
        if ( dragSurface_ != null ) {
            Navigator<A> navigator = getNavigator();
            if ( navigator != null ) {
                NavAction<A> navact =
                    navigator.drag( dragSurface_, evt, startPoint_ );
                if ( navact != null ) {
                    processAction( navact );
                }
            } 
        } 
    }   

    public void mouseReleased( MouseEvent evt ) {

        /* Terminate any current drag gesture. */
        dragSurface_ = null;
        startPoint_ = null;
    }

    public void mouseClicked( MouseEvent evt ) {
        int iButt = evt.getButton();
        if ( iButt == MouseEvent.BUTTON3 ) {
            Navigator<A> navigator = getNavigator();
            Surface surface = getSurface();
            if ( navigator != null && surface != null ) {
                handleClick( navigator, surface, evt, createDataPosIterable() );
            }
        }
    }

    /**
     * Performs the actual work when a mouse click event is detected.
     * This method is invoked by {@link #mouseClicked mouseClicked}.
     * The default behaviour is to get a corresponding navigation action
     * from the navigator, and call <code>setAspect</code> accordingly.
     * However, it may be overridden by subclasses.
     *
     * @param   navigator   navigator
     * @param   surface  plot surface
     * @param   evt  mouse event
     * @param   dposIt  iterable over points if available
     */
    protected void handleClick( Navigator<A> navigator, Surface surface,
                                MouseEvent evt, Iterable<double[]> dposIt ) {
        NavAction<A> navact =
            navigator.click( surface, evt, createDataPosIterable() );
        if ( navact != null ) {
            A aspect = navact.getAspect();
            if ( aspect != null ) {
                setAspect( aspect );
            }
        }
    }

    public void mouseWheelMoved( MouseWheelEvent evt ) {
        Navigator<A> navigator = getNavigator();
        Surface surface = getSurface();
        if ( navigator != null && surface != null ) {
            NavAction<A> navact = navigator.wheel( surface, evt );
            if ( navact != null ) {
                processAction( navact );
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
     * Receives a new navigation action produced by user interface actions in
     * conjunction with this object.
     *
     * @param  navact   navigation action
     */
    private void processAction( NavAction<A> navact ) {
        if ( navact != null ) {
            A aspect = navact.getAspect();
            if ( aspect != null ) {
                setAspect( aspect );
            }
        }
    }
}
