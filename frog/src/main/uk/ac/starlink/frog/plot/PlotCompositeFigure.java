package uk.ac.starlink.frog.plot;

import diva.canvas.Figure;
import diva.canvas.toolbox.BackgroundedCompositeFigure;

import java.awt.geom.AffineTransform;

import javax.swing.event.EventListenerList;

/**
 * PlotCompositeFigure extends the diva BackgroundedCompositeFigure
 * class to add support for events that allow users of any derived
 * figures to be made aware of any changes, i.e. composite figure
 * creation, removal and transformations.
 *
 * All composite figures used on a Plot should be derived classes of
 * this class, or implement the necessary code to support the
 * FigureListener class. They should also invoke fireChanged in their
 * translate and transform methods (but not if calling super) and
 * respect the transformFreely state.
 * 
 * @since $Date$
 * @since 19-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see Plot, BackgroundedCompositeFigure, Figure 
 */
public class PlotCompositeFigure extends BackgroundedCompositeFigure
    implements PlotFigure
{
    /**
     * Construct a backgrounded composite figure with
     * no background and no children.
     */
    public PlotCompositeFigure() {
        this(null);
    }
    
    /**
     * Construct a backgrounded composite figure with the
     * given background and no children.
     */
    public PlotCompositeFigure(Figure background) {
        super( background );
        fireCreated();
    }

    /** 
     * Translate the figure the given distance, but only in X.
     */
    public void translate( double x, double y ) {
        super.translate(x,y);
        fireChanged();
    }

    /** 
     * Transform the figure. Just allow transforms of X scale.
     */
    public void transform( AffineTransform at ) {
        super.transform( at );
        fireChanged();
    }

    //
    //  Events interface.
    //
    protected EventListenerList listeners = new EventListenerList();

    /**
     *  Registers a listener for to be informed when figure changes
     *  occur. 
     *
     *  @param l the FigureListener
     */
    public void addListener( FigureListener l ) {
        listeners.add( FigureListener.class, l );
    }

    /**
     * Remove a listener.
     *
     * @param l the FigureListener
     */
    public void removeListener( FigureListener l ) {
        listeners.remove( FigureListener.class, l );
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has created to all listeners.
     */
    protected void fireCreated() {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this, 
                                                FigureChangedEvent.CREATED );
                }
                ((FigureListener)list[i+1]).figureCreated( e );
            }
        }
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has been removed.
     */
    protected void fireRemoved() {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this, 
                                                FigureChangedEvent.REMOVED );
                }
                ((FigureListener)list[i+1]).figureRemoved( e );
            }
        }
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has changed.
     */
    protected void fireChanged() {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this, 
                                                FigureChangedEvent.CHANGED );
                }
                ((FigureListener)list[i+1]).figureChanged( e );
            }
        }
    }

    //
    //  Transform freely interface.
    //
    /**
     * Hint that figures should ignore any transformation constraints
     * (to match a resize of Plot).
     */
    protected static boolean transformFreely = false;

    /**
     * Enable the hint that a figure should allow itself to transform
     * freely, rather than obey any constraints (this is meant for
     * figures that could not otherwise redraw themselves to fit a
     * resized Plot, given their normal constraints, e.g. XRangeFigure).
     */
    public void setTransformFreely( boolean state )
    {
        transformFreely = state;
    }

    /**
     * Find out if this is an occasion when a figure should give up
     * any constraints and traneform freely.
     */
    public static boolean isTransformFreely() {
        return transformFreely;
    }
}
