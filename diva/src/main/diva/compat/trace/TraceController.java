/*
 * $Id: TraceController.java,v 1.6 2002/05/16 21:20:05 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.compat.trace;

import diva.canvas.GraphicsPane;
import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Manipulator;

import diva.canvas.interactor.BasicSelectionRenderer;
import diva.canvas.interactor.SelectionDragger;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionRenderer;

/** A controller for trace diagrams. This controller sets up all
 * the interaction on the pane, and creates interactors and other objects
 * that control how the surface responds to user interaction. Currently,
 * the degree of parameterization is limited, but this will be increased
 * in future.
 *
 * @version	$Revision: 1.6 $
 * @author 	John Reekie
 */
public class TraceController {

    /** The interactor that drags objects by default
     */
    private DragInteractor _dragInteractor;

    /** The selection interactor.
     */
    private SelectionInteractor _selectionInteractor;

    /** The selection renderer.
     */
    private SelectionRenderer _selectionRenderer;

    /** The selection dragger
     */
    private SelectionDragger _selectionDragger;

    /** The pane that this controller is associated with.
     */
    private GraphicsPane _pane;

    /** The trace model
     */
    private TraceModel _model;

    /** The trace view
     */
    private TraceView _view;

    /** Create a new controller
     */ 
    public TraceController () {
	// empty
    }

   /** Create a new controller for the given pane
     */ 
    public TraceController (GraphicsPane pane) {
	_pane = pane;
    }

    /** Get the drag interactor
     */
    public DragInteractor getDragInteractor () {
        return _dragInteractor;
    }

    /**
     * Get the trace model.
     */
    public final TraceModel getTraceModel () {
        return _model;
    }

    /**
     * Get the trace view.
     */
    public final TraceView getTraceView () {
        return _view;
    }

     /** Get the selection interactor
     */
    public SelectionDragger getSelectionDragger () {
        return _selectionDragger;
    }

    /** Get the selection renderer
     */
    public SelectionRenderer getSelectionRenderer () {
        return _selectionInteractor.getSelectionRenderer();
    }

    /** Get the selection interactor
     */
    public SelectionInteractor getSelectionInteractor () {
        return _selectionInteractor;
    }

    /**
     * Initialize all interaction on the trace pane. By the time
     * this method is called, all relevant references to views,
     * panes, and interactors must already have been set up.
     */
    public void initializeInteraction () {
        GraphicsPane pane = getTraceView().getGraphicsPane();


	// Create the selection interactor
	_selectionInteractor = new SelectionInteractor();

        // Create a selection drag-selector
	_selectionDragger = new SelectionDragger(pane);
	_selectionDragger.addSelectionInteractor(_selectionInteractor);

	// Add the drag interactor to the selection interactor so
        // selected items are dragged
	_dragInteractor = new DragInteractor();
	_selectionInteractor.addInteractor(_dragInteractor);        
    }

    /**
     * Set the trace model that is being viewed. Unsubscribe from
     * the previous one if there is one. Then subscribe to the new one.
     */
    public void setTraceModel(TraceModel m) {
        if(_model != null) {
            //_model.removeGraphListener(this);
        }
        _model = m;
        
        if(_model != null) {
            //_model.addGraphListener(this);
            
            //force an update
            //GraphEvent update = new GraphEvent(
            //        GraphEvent.STRUCTURE_CHANGED, this, _model.getGraph());
            //structureChanged(update);
        }
    }

    /**
     * Set the view that this controller operates on. This method
     * sets the view, and calls initializeInteraction().
     */
    public void setTraceView (TraceView view) {
        _view = view;
        initializeInteraction();
    }

    /** Set the prototype selection manipulator. Selected figures
     * will have a copy of this manipulator wrapped around them.
     * This method nullifies any previous renderers set with
     * setSelectionRenderer();
     */
    public void setSelectionManipulator (Manipulator manipulator) {
        _selectionInteractor.setPrototypeDecorator(manipulator);
    }

    /** Set the selection renderer. Selected figures will be highlighted
     * with this renderer.
     */
    public void setSelectionRenderer (SelectionRenderer renderer) {
        _selectionInteractor.setSelectionRenderer(renderer);
    }
}


