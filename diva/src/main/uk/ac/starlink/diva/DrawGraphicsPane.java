/*
 * Copyright (C) 2003-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-JAN-2001 (Peter W. Draper):
 *        Original version.
 *     19-DEC-2003 (Peter W. Draper):
 *        Major re-write to a more generic form.
 *     15-JAN-2004 (Peter W. Draper):
 *        Moved out of SPLAT and into DIVA.
 */
package uk.ac.starlink.diva;

import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionListener;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionModel;

import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * The pane for displaying any interactive graphic Figures associated
 * with a {@link Draw}. The graphics will normally be created by an
 * instance of the {@link DrawActions} class.
 * <p>
 * Also provides a listener mechanism for Figure changes (selection,
 * dragging).
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Draw
 * @see DrawActions
 */
public class DrawGraphicsPane
    extends GraphicsPane
{
    /**
     * The controller
     */
    private DrawController controller;

    /**
     * The interactor to give to all figures
     */
    private SelectionInteractor selectionInteractor;

    /**
     * The layer to draw all figures in
     */
    private FigureLayer figureLayer;

    /**
     * DragRegion, used in addition to SelectionDragger. This is used
     * for implementing the interactive zoom functions.
     */
    private DragRegion dragRegion;

    /**
     *  Constructor accepts a FigureDecorator. Normally this will be tuned
     *  to interact with all the types of figures that will be offered
     *  (see {@link DrawActions} and the TypedDecorator it creates).
     */
    public DrawGraphicsPane( FigureDecorator decorator )
    {
        super();

        // Get the figure layer
        figureLayer = getForegroundLayer();

        // Set the halo size used to select figures. Default is 0.5,
        // which means pointing to within a pixel, which is a bit
        // tight.
        figureLayer.setPickHalo( 2.0 );

        // Construct a simple controller and get the default selection
        // interactor.
        controller = new DrawController( this );
        selectionInteractor = controller.getSelectionInteractor();

        // Tell the controller to use the decorator for deciding how
        // to wrap selected figures.
        selectionInteractor.setPrototypeDecorator( decorator );

        // Add the additional drag region interactor to work with
        // mouse button 2.
        MouseFilter mFilter = new MouseFilter( InputEvent.BUTTON2_MASK |
                                               InputEvent.BUTTON3_MASK );
        dragRegion = new DragRegion( this );
        dragRegion.setSelectionFilter( mFilter );
    }

    /**
     * Add an existing Figure.
     */
    public void addFigure( Figure figure )
    {
        //  Add Figure to the figure layer.
        figureLayer.add( figure );

        //  Make our interactors deal with it.
        figure.setInteractor( selectionInteractor );
    }

    /**
     * Get the index of a Figure in the Figure Layer.
     */
    public int indexOf( Figure figure )
    {
        return figureLayer.indexOf( figure );
    }

    /**
     * Lower a Figure to the bottom of the ZList.
     */
    public void lowerFigure( Figure figure )
    {
        clearSelection();
        figureLayer.setIndex( figureLayer.getFigureCount() - 1, figure );
    }

    /**
     * Raise a figure to the front of the ZList.
     */
    public void raiseFigure( Figure figure )
    {
        clearSelection();
        figureLayer.setIndex( 0, figure );
    }

    /**
     *  Get the selection interactor.
     */
    public SelectionInteractor getSelectionInteractor()
    {
        return selectionInteractor;
    }

    /**
     *  Get the controller.
     */
    public DrawController getController()
    {
        return controller;
    }

    /**
     *  Add a listener for any SelectionEvents.
     */
    public void addSelectionListener( SelectionListener l )
    {
        selectionInteractor.getSelectionModel().addSelectionListener( l );
    }

    /**
     *  Get a list of the currently selected Figures.
     */
    public Object[] getSelectionAsArray()
    {
        return selectionInteractor.getSelectionModel().getSelectionAsArray();
    }

    /**
     * Clear the selection.
     */
    public void clearSelection()
    {
        selectionInteractor.getSelectionModel().clearSelection();
    }

    /**
     * Select the given figure.
     */
    public void select( Figure figure )
    {
        Interactor i = figure.getInteractor();
        if ( i instanceof SelectionInteractor ) {
            SelectionInteractor si = (SelectionInteractor) i;
            si.getSelectionModel().addSelection( figure );
        }
    }

    /**
     *  Get the figure layer that we draw into.
     */
    public FigureLayer getFigureLayer()
    {
        return figureLayer;
    }

    /**
     *  Add a FigureListener to the DragRegion used for interacting
     *  with figures.
     */
    public void addFigureDraggerListener( FigureListener l )
    {
        controller.getSelectionDragger().addListener( l );
    }

    /**
     *  Remove a FigureListener to the DragRegion used for interacting
     *  with figures.
     */
    public void removeFigureDraggerListener( FigureListener l )
    {
        controller.getSelectionDragger().removeListener( l );
    }

    /**
     *  Add a FigureListener to the DragRegion used for non-figure
     *  selection work.
     */
    public void addZoomDraggerListener( FigureListener l )
    {
        dragRegion.addListener( l );
    }

    /**
     *  Remove a FigureListener from the DragRegion used for non-figure
     *  selection work.
     */
    public void removeZoomDraggerListener( FigureListener l )
    {
        dragRegion.removeListener( l );
    }

    /**
     *  Remove a Figure.
     *
     *  @param figure the figure to remove.
     */
    public void removeFigure( Figure figure )
    {
        Interactor interactor = figure.getInteractor();
        if ( interactor instanceof SelectionInteractor ) {
            // remove any selection handles, etc.
            SelectionModel model =
                ((SelectionInteractor) interactor).getSelectionModel();
            if ( model.containsSelection( figure ) ) {
                model.removeSelection( figure );
            }
        }
        figureLayer.remove( figure );
    }

    /**
     * Return the current properties of a figure.
     */
    public FigureProps getFigureProps( Figure figure )
    {
        Rectangle2D bounds = figure.getBounds();
        return new FigureProps( bounds.getX(), bounds.getY(),
                                bounds.getWidth(), bounds.getHeight() );
    }

    /**
     * Switch off selection using the drag box interactor.
     */
    public void disableFigureDraggerSelection()
    {
        //  Do this by the slight of hand that replaces the
        //  FigureLayer with one that has no figures.
        dragRegion.setFigureLayer( emptyFigureLayer );
    }
    private FigureLayer emptyFigureLayer = new FigureLayer();

    /**
     * Switch selection using the drag box interactor back on.
     */
    public void enableFigureDraggerSelection()
    {
        dragRegion.setFigureLayer( figureLayer );
    }
}
