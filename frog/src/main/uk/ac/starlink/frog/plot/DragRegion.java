package uk.ac.starlink.frog.plot;

import diva.canvas.GraphicsPane;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.SelectionDragger;

import java.awt.geom.Rectangle2D;

import javax.swing.event.EventListenerList;

/**
 * A class that implements rubber-banding on a canvas. It contains
 * references to one or more instances of SelectionInteractor, which it
 * notifies whenever dragging includes or excludes a figure in the
 * given layers (which can be from a GraphicsPane) and additionally
 * (which is the reason for this class to be used instead of
 * SelectionDragger) informs any FigureListeners when the region
 * is released (uses of this are expected to be when creating figures
 * and zooming the plot).
 *
 * @since $Date$
 * @since 12-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see SelectionDragger, Plot
 */
public class DragRegion extends SelectionDragger
{
    /**
     * Create a new DragRegion.
     */
    public DragRegion()
    {
        super();
    }

    /**
     * Create a new DragRegion attached to the given graphics
     * pane.
     */
    public DragRegion( GraphicsPane gpane )
    {
        super( gpane );
    }

    /**
     * Delete the rubber-band. Inform any FigureListeners of this.
     */
    public void mouseReleased( LayerEvent event )
    {
        _finalRubberBand = _rubberBand;
        super.mouseReleased( event );
        fireCompleted( event );
    }

    /**
     * Reference to rubber-band dimensions when released.
     */
    private Rectangle2D _finalRubberBand;

    /**
     * Get shape of rectangle when released.
     */
    public Rectangle2D getFinalShape()
    {
        return _finalRubberBand;
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
     * Send an event to all FigureListeners that this pseudo-figure
     * has completed its work. The event that trigger this is passed
     * along so that the MouseEvent features may be queried.
     */
    protected void fireCompleted( LayerEvent le )
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this,
                                                FigureChangedEvent.REMOVED,
                                                le );
                }
                ((FigureListener)list[i+1]).figureRemoved( e );
            }
        }
    }
}
