package uk.ac.starlink.frog.plot;

import diva.canvas.GraphicsPane;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionRenderer;

/*
 * Original heading from BasicController: COPYRIGHT file is in the
 * Diva part of release.
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the DIVA COPYRIGHT file for details.
 *
 */
/** 
 * A basic controller implementation. This controller creates a useful
 * and common interaction that can be used in simple applications. A
 * single interactor provides selection and dragging. Clients that
 * wish to use this default interaction can give this interactor to
 * figures that they add to the foreground layer of the corresponding
 * pane.
 *
 * @version $Revision$
 * @author John Reekie
 *
 * @since $Date$
 * @since 12-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research
 *            Councils 
 */
public class DivaController 
{
    /** 
     * The interactor that drags objects by default
     */
    protected DragInteractor _dragInteractor;

    /** 
     * The selection interactor.
     */
    protected SelectionInteractor _selectionInteractor;

    /** 
     * The selection renderer.
     */
    protected SelectionRenderer _selectionRenderer;

    /** 
     * The selection dragger
     */
    protected DragRegion _selectionDragger;

    /** 
     * The pane that this controller is associated with.
     */
    protected GraphicsPane _pane;

    /** 
     * Create a new controller for the given pane
     */ 
    public DivaController (GraphicsPane pane) 
    {
	_pane = pane;

	// Create the selection interactor
	_selectionInteractor = new SelectionInteractor();

        // Create a selection drag-selector
	_selectionDragger = new DragRegion( pane );
	_selectionDragger.addSelectionInteractor(_selectionInteractor);

	// Add the drag interactor to the selection interactor so
        // selected items are dragged
	_dragInteractor = new DragInteractor();
	_selectionInteractor.addInteractor(_dragInteractor);
    }

    /** 
     * Get the drag interactor
     */
    public DragInteractor getDragInteractor () 
    {
        return _dragInteractor;
    }

    /** Get the selection interactor
     */
    public DragRegion getSelectionDragger () 
    {
        return _selectionDragger;
    }

    /** Get the selection renderer
     */
    public SelectionRenderer getSelectionRenderer () 
    {
        return _selectionInteractor.getSelectionRenderer();
    }

    /** Get the selection interactor
     */
    public SelectionInteractor getSelectionInteractor () 
    {
        return _selectionInteractor;
    }

    /** Set the prototype selection manipulator. Selected figures
     * will have a copy of this manipulator wrapped around them.
     * This method nullifies any previous renderers set with
     * setSelectionRenderer();
     */
    public void setSelectionManipulator (Manipulator manipulator) 
    {
        _selectionInteractor.setSelectionManipulator(manipulator);
    }

    /** Set the selection renderer. Selected figures will be highlighted
     * with this renderer.
     */
    public void setSelectionRenderer (SelectionRenderer renderer) 
    {
        _selectionInteractor.setSelectionRenderer(renderer);
    }
}
