/*
 * $Id: PanZoomController.java,v 1.6 2001/07/22 22:01:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */
package diva.sketch.toolbox;

import diva.sketch.SketchController;
import diva.sketch.recognition.TimedStroke;

import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.SelectionDragger;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionRenderer;

import java.awt.geom.AffineTransform;
import java.util.Iterator;

/** A controller that combines conventional click- and drag-selection
 * on a pane with gesture-based panning and zooming.
 * 
 * <p>
 * The interactors that perform conventional interaction can be
 * accessed through the methods getSelectionDragger(), getDragInteractor(),
 * and getSelectionInteractor().
 *
 * <P>
 * The gesture pan and zoom is activated by gesturing on the
 * background of the pane. To start zooming, draw a "Z" shape on
 * the background. (The "Z" must be drawn in the usual way, starting
 * at the top left and ending at the bottom right. Without releasing
 * the mouse button, move the mouse up or down to zoom in and out
 * respectively. To start panning, draw a "P" on the canvas, starting
 * at the bottom of the vertical stroke and proceeding in a single
 * stroke upwards and around the loop. Again without releasing the
 * mouse button, move the mouse in any direction to pan in that
 * direction.
 *
 * @version	$Revision: 1.6 $
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @author 	Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author 	John Reekie (johnr@eecs.berkeley.edu)
 */
public class PanZoomController extends SketchController {

    /**
     *The name of the training file that contains "P" and "Z"
     *gestures.
     */
    public static final String PZ_TRAINING_DATA = "pz.tc";

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

    private Interactor _interpreter;

    /** Create a new controller for the given pane
     */ 
    public PanZoomController () {}

    protected void initializeInteraction(){
	// Create the selection interactor
	_selectionInteractor = new SelectionInteractor();

        // Create a selection drag-selector
	_selectionDragger = new SelectionDragger(getSketchPane());
	_selectionDragger.addSelectionInteractor(_selectionInteractor);

	// Add the drag interactor to the selection interactor so
        // selected items are dragged
	_dragInteractor = new DragInteractor();
	_selectionInteractor.addInteractor(_dragInteractor);

        // Add the gesture recognition
        try {
            _interpreter = new PanZoomInterpreter(this);
            getSketchPane().getBackgroundEventLayer().addInteractor(_interpreter);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /** Get the drag interactor
     */
    public DragInteractor getDragInteractor () {
        return _dragInteractor;
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

    ///////////////////////////////////////////////////////////////

    /** 
     * Changes the transform of the * controller's pane according to
     * the event type and the * translation or scaling data in the
     * event.
     *
     * <p>In order to prevent weird interactions with the conventional
     * selection interactor, the first classification event following
     * a gesture started event disables the selection dragger on the
     * background event layer of the pane. The gesture completed event
     * re-enables that interactor. In order to do this, this class
     * also implements GestureListener
     */

    /** The current gesture and zoom amount
     */
    private TimedStroke _curStroke = null;
    private double _curZoom;
        
    /** A flag saying whether we are gesturing
     */
    private boolean _gesturing = false;

    /** Disable the selection
     */
    private void disableSelection () {
        _selectionDragger.terminateDragSelection();
        _selectionDragger.setEnabled(false);
        _gesturing = true;
    }
            
    /** Enable the selection
     */
    private void enableSelection () {
        _selectionDragger.setEnabled(true);
        _gesturing = false;
    }

    public void pan(double dx, double dy){
        if (!_gesturing) {
            disableSelection();
        }
        getSketchPane().translate(dx, dy);
    }

    public void zoom(double centerX, double centerY, double zoomAmount, TimedStroke s){
        // Disable the selection interactor
        if (!_gesturing) {
            disableSelection();
        }
        // Scale the pane
        AffineTransform at = getSketchPane().getTransformContext().getTransform();
     
        if(s != _curStroke){
            _curStroke = s;
            _curZoom = at.getScaleX();
        }
        double dscale = _curZoom*zoomAmount/at.getScaleX();
        getSketchPane().scale(centerX, centerY, dscale, dscale);
    }

    /** Re-enable the selection, now that gesturing is completed.
     */
    public void strokeCompleted() {
        if (_gesturing) {
            enableSelection();
        }
    }
}

