/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.Interactor;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.event.EventListenerList;

/**
 * DrawBasicFigure extends the diva BasicFigure class to add support for
 * events that allow users of any derived figures to be made aware of
 * any changes -- figure creation, removal and transformations.
 * <p>
 * All figures used on a {@link Draw} that is using {@link DrawActions}
 * should be derived classes of this class, or implement the necessary
 * code to support the {@link DrawFigure} interface. They should also
 * invoke fireChanged in their translate and transform methods (but
 * not if calling super) and respect the transformFreely state.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 * @see DrawFigure
 */
public class DrawBasicFigure
    extends BasicFigure
    implements DrawFigure
{
    /**
     * Default constructor.
     */
    public DrawBasicFigure()
    {
        super( new Rectangle2D.Double() );
    }

    /**
     * Create a new figure with the given shape. The figure, by
     * default, has a unit-width continuous black outline and no fill.
     */
    public DrawBasicFigure( Shape shape )
    {
        super( shape );
        fireCreated();
    }

    /**
     * Create a new figure with the given shape and outline width.
     * It has no fill. The default outline paint is black.
     */
    public DrawBasicFigure( Shape shape, float lineWidth )
    {
        super( shape, lineWidth );
        setLineWidth( lineWidth );
        fireCreated();
    }

    /**
     * Create a new figure with the given paint pattern. The figure,
     * by default, has no stroke.
     */
    public DrawBasicFigure( Shape shape, Paint fill )
    {
        super( shape, fill );
        fireCreated();
    }

    /**
     * Create a new figure with the given fill and outline paints
     * and outline width.
     */
    public DrawBasicFigure( Shape shape, Paint fill, Paint outline,
                            float lineWidth )
    {
        super( shape, fill, lineWidth );
        setStrokePaint( outline );
        setLineWidth( lineWidth );
        fireCreated();
    }

    /**
     * Create a new figure with the given paint pattern and outline width.
     * The default outline paint is black.
     */
    public DrawBasicFigure( Shape shape, Paint fill, float lineWidth )
    {
        super( shape, fill, lineWidth );
        setLineWidth( lineWidth );
        fireCreated();
    }

    /**
     * Translate the figure.
     */
    public void translate( double x, double y )
    {
        super.translate( x, y );
        fireChanged();
    }

    /**
     * Transform the figure.
     */
    public void transform( AffineTransform at )
    {
        super.transform( at );
        fireChanged();
    }

    public void setVisible( boolean flag )
    {
        super.setVisible( flag );

        // Don't leave selection decorators around.
        if ( !flag ) {
            Interactor interactor = getInteractor();
            if ( interactor instanceof SelectionInteractor ) {
                SelectionModel model =
                    ((SelectionInteractor) interactor).getSelectionModel();
                if ( model.containsSelection( this ) ) {
                    model.removeSelection( this );
                }
            }
        }
        repaint();
    }

    //
    //  Transform freely interface.
    //

    /**
     * Hint that figures should ignore any transformation constraints.
     */
    protected static boolean transformFreely = false;

    /**
     * Enable the hint that a figure should allow itself to transform
     * freely, rather than obey any constraints (this is meant for
     * figures that could not otherwise redraw themselves to fit a
     * resized {@link Draw}, given their normal constraints,
     * e.g. XRangeFigure).
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
