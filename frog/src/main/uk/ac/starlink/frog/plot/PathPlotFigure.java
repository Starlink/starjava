package uk.ac.starlink.frog.plot;

import diva.canvas.toolbox.PathFigure;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import javax.swing.event.EventListenerList;

/**
 * PathPlotFigure extends the diva PathFigure class to add support for
 * events that allow users of any derived figures to be made aware of
 * any changes, i.e. figure creation, removal and transformations.
 *
 * This base class should be used for non-filled figures, use
 * BasicPlotFigure for filled types.
 *
 * All figures used on a Plot should be derived classes of this class
 * or BasicPlotFigure, or implement the necessary code to support the
 * FigureListener class. They should also invoke fireChanged in their
 * translate and transform methods (but not if calling super) and
 * respect the transformFreely state.
 *
 * @since $Date$
 * @since 20-JUN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see BasicPlotFigure.
 */
public class PathPlotFigure extends PathFigure
{
    /**
     * Create a new figure with the given shape. The figure, by
     * default, is stroked with a unit-width continuous black stroke.
     */
    public PathPlotFigure( Shape shape )
    {
        super( shape );
        fireCreated();
    }

    /**
     * Create a new figure with the given shape and width.  The
     * default paint is black.
     */
    public PathPlotFigure( Shape shape, float lineWidth )
    {
        super( shape, lineWidth );
        fireCreated();
    }

    /**
     * Create a new figure with the given paint and width.
     */
    public PathPlotFigure( Shape shape, Paint paint, float lineWidth )
    {
        super( shape, paint, lineWidth );
        fireCreated();
    }
    /** 
     * Transform the figure with the supplied transform.
     */
    public void transform( AffineTransform at ) 
    {
        super.transform( at );
        fireChanged();
    }

    /** 
     * Translate the figure with by the given distance.
     */
    public void translate( double x, double y ) 
    {
        super.translate( x, y );
        fireChanged();
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

//
//  FigureListener events.
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
}
