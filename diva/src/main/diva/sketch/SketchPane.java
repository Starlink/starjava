/*
 * $Id: SketchPane.java,v 1.15 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
import diva.canvas.CanvasComponent;
import diva.canvas.CanvasLayer;
import diva.canvas.DamageRegion;
import diva.canvas.FigureLayer;
import diva.util.UnitIterator;
import java.util.Iterator;

/**
 * SketchPane is a sketching surface which can be customized to
 * perform recognition for a particular application.  It relies on a
 * SketchController to give it smarts.  It also optimizes the
 * drawing of strokes so that it is not necessary to perform
 * a redraw of the canvas for every mouse event.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.15 $
 * @rating Red
 */
public class SketchPane extends GraphicsPane {
    /**
     * A layer on which to draw strokes in progress
     */
    private SketchLayer _sketchLayer;

    /**
     * A very nasty hack that allows us to only paint one layer, rather
     * than the whole set of layers, so that sketching strokes is fast.
     */
    private boolean _isSketching = false;
    
    /**
     * The sketch controller that processes the input sketch on this
     * pane.
     */
    private SketchController _controller;

    /**
     * Create a new sketch pane with a SketchController to
     * process the input sketch.
     */
    public SketchPane () {
        this(new SketchController());
    }

    /**
     * Create a new sketch pane with the given controller
     * that controls the behavior of this pane.
     */
    public SketchPane (SketchController controller) {
        super();
        _sketchLayer = new SketchLayer();
        _initNewLayer(_sketchLayer);
        setSketchController(controller);
        _rebuildLayerArray();
    }

    /**
     * Return the sketch layer for optimized drawing
     * of strokes.
     */
    SketchLayer getSketchLayer() {
        return _sketchLayer;
    }
    
    /**
     * Get the sketch controller that controls
     * the behavior of this pane.
     */
    public SketchController getSketchController () {
        return _controller;
    }

    /**
     * Set the sketch controller that controls
     * the behavior of this pane.
     */
    private void setSketchController (SketchController controller) {
        _controller = controller;
        _controller.setSketchPane(this);
    }

    /** Rebuild the array of layers for use by iterators.
     * Override superclass to include sketch layer.
     */
    protected void _rebuildLayerArray () {
        _layers = new CanvasLayer[6];
        int cursor = 0;
        _layers[cursor++] = _foregroundEventLayer;
        _layers[cursor++] = _sketchLayer;
        _layers[cursor++] = _overlayLayer;
        _layers[cursor++] = _foregroundLayer;
        _layers[cursor++] = _backgroundLayer;
        _layers[cursor++] = _backgroundEventLayer;
    }
}


