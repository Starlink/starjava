/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     19-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.canvas.Figure;
import diva.canvas.toolbox.BackgroundedCompositeFigure;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.Interactor;

import java.awt.Shape;
import java.awt.Paint;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.geom.AffineTransform;

import javax.swing.event.EventListenerList;

/**
 * DrawCompositeFigure extends the Diva BackgroundedCompositeFigure
 * class to add support for events that allow users of any derived
 * figures to be made aware of any changes, i.e.<!-- --> composite figure
 * creation, removal and transformations.
 * <p>
 * All composite figures used on a {@link Draw} implementation should
 * be derived classes of this class, or implement the necessary code
 * to support the FigureListener class. They should also invoke
 * fireChanged in their translate and transform methods (but not if
 * calling super) and respect the transformFreely state.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 * @see BackgroundedCompositeFigure
 * @see DrawFigure
 */
public class DrawCompositeFigure
    extends BackgroundedCompositeFigure
    implements DrawFigure
{
    /**
     * Construct a backgrounded composite figure with
     * no background and no children.
     */
    public DrawCompositeFigure()
    {
        this( null );
    }

    /**
     * Construct a backgrounded composite figure with the
     * given background and no children.
     */
    public DrawCompositeFigure( Figure background )
    {
        super( background );
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

    /**
     * Set the background Figure using a Shape.
     */
    public void setShape( Shape shape )
    {
        setBackgroundFigure( new DrawBasicFigure( shape ) );
    }

    /**
     * Set the fill paint of the background figure.
     */
    public void setFillPaint( Paint fill )
    {
        ((DrawFigure)getBackgroundFigure()).setFillPaint( fill );
    }

    /**
     * Get the fill paint of the background figure.
     */
    public Paint getFillPaint()
    {
        return ((DrawFigure)getBackgroundFigure()).getFillPaint();
    }

    /**
     * Set the outline paint of the background figure.
     */
    public void setStrokePaint( Paint outline )
    {
        ((DrawFigure)getBackgroundFigure()).setStrokePaint( outline );
    }

    /**
     * Get the outline paint of the background figure.
     */
    public Paint getStrokePaint()
    {
        return ((DrawFigure)getBackgroundFigure()).getStrokePaint();
    }

    /**
     * Set the composite of the background figure.
     */
    public void setComposite( AlphaComposite composite )
    {
        ((DrawFigure)getBackgroundFigure()).setComposite( composite );
    }

    /**
     * Get the composite of the background figure.
     */
    public Composite getComposite()
    {
        return ((DrawFigure)getBackgroundFigure()).getComposite();
    }

    /** Set line width */
    public void setLineWidth( float width )
    {
        ((DrawFigure)getBackgroundFigure()).setLineWidth( width );
    }

    /** Get line width */
    public float getLineWidth()
    {
        return ((DrawFigure)getBackgroundFigure()).getLineWidth();
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
    //  Events interface.
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

    //
    //  Transform freely interface.
    //
    /**
     * Hint that figures should ignore any transformation constraints
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
}
