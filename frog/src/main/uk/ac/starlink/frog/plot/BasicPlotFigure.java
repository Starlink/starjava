package uk.ac.starlink.frog.plot;

import diva.canvas.toolbox.BasicFigure;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.event.EventListenerList;

/**
 * PlotFigure extends the diva BasicFigure class to add support for
 * events that allow users of any derived figures to be made aware of
 * any changes, i.e. figure creation, removal and transformations.
 *
 * All figures used on a Plot should be derived classes of this class,
 * or implement the necessary code to support the FigureListener
 * class. They should also invoke fireChanged in their translate and
 * transform methods (but not if calling super) and respect the
 * transformFreely state.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see Plot, Figure
 */
public class BasicPlotFigure extends BasicFigure
    implements PlotFigure
{
    /**
     * Default constructor.
     */
    public BasicPlotFigure()
    {
        super( new Rectangle2D.Double() );
    }

    /**
     * Create a new figure with the given shape. The figure, by
     * default, has a unit-width continuous black outline and no fill.
     */
    public BasicPlotFigure( Shape shape )
    {
        super( shape );
        fireCreated();
    }

    /**
     * Create a new figure with the given shape and outline width.
     * It has no fill. The default outline paint is black.
     */
    public BasicPlotFigure( Shape shape, float lineWidth )
    {
        super( shape, lineWidth );
        fireCreated();
    }

    /**
     * Create a new figure with the given paint pattern. The figure,
     * by default, has no stroke.
     */
    public BasicPlotFigure( Shape shape, Paint fill )
    {
        super( shape, fill );
        fireCreated();
    }

    /**
     * Create a new figure with the given paint pattern and outline width.
     * The default outline paint is black.
     */
    public BasicPlotFigure( Shape shape, Paint fill, float lineWidth )
    {
        super( shape, fill, lineWidth );
        fireCreated();
    }

    /**
     * Translate the figure the given distance, but only in X.
     */
    public void translate( double x, double y )
    {
        super.translate(x,y);
        fireChanged();
    }

    /**
     * Transform the figure. Just allow transforms of X scale.
     */
    public void transform( AffineTransform at )
    {
        super.transform( at );
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
    public static boolean isTransformFreely()
    {
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
    public void addListener( FigureListener l )
    {
        listeners.add( FigureListener.class, l );
    }

    /**
     * Remove a listener.
     *
     * @param l the FigureListener
     */
    public void removeListener( FigureListener l )
    {
        listeners.remove( FigureListener.class, l );
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has created to all listeners.
     */
    protected void fireCreated()
    {
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
    protected void fireRemoved()
    {
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
    protected void fireChanged()
    {
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
