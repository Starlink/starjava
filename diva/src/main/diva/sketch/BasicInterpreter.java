/*
 * $Id: BasicInterpreter.java,v 1.38 2001/07/22 22:01:40 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.toolbox.RemoveDupPtsStrokeFilter;
import diva.canvas.GraphicsPane;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.AbstractInteractor;

/**
 * A class that interprets changes to a stroke.  BasicInterpreter is a
 * sketch interpreter that gets called whenever a user sketches on the
 * screen.  It extends from diva.canvas.AbstractInteractor and should
 * be added to the pane layer in order to receive mouse events.
 *
 * Since BasicInterpreter is a very simple implementation of a sketch
 * interpreter, it doesn't do any interpretation on user input.  In
 * general, a sketch interpreter would receive mouse events and use a
 * recognizer to process these events.  The recognizer comes back with
 * an answer, and depending on what the answer is, the interpreter
 * tells the sketch controller to perform the corresponding
 * application-specific actions.  The recognizer can be arbitrarily
 * complex (or simple).  It could be a low-level recognizer that
 * determines the geometric shape of a user stroke or a voting
 * recognizer that uses a bunch of different recognizers.  The
 * recognizer is meant to be highly configurable.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  Heloise (hwawen@eecs.berkeley.edu) 
 * @version $Revision: 1.38 $
 * @rating Red
 */
public class BasicInterpreter extends AbstractInteractor {
    /**
     * The sketch controller that uses this interpreter to interpret
     * sketch input.
     */
    protected SketchController _controller;

    /**
     * A buffer for the stroke that's currently being drawn.  This is
     * to avoid allocating a TimedStroke object at the beginning of
     * each new stroke, thus avoiding the overhead of memory
     * allocation.  The TimedStroke is initialized to have a capacity
     * of 2000 points which should be plenty for a typical stroke.
     */
    protected TimedStroke _strokeBuffer = new TimedStroke(2000);
    
    /**
     * The current stroke that's being drawn, gets its data from
     * _strokeBuffer and is reallocated at the end of a stroke.
     */
    protected TimedStroke _curStroke;
       
    /**
     * The current symbol which wraps the current stroke and keeps 
     * visual information about the stroke (color, line width).
     */
    protected StrokeSymbol _curSymbol;
    
    /**
     * Create a BasicInterpreter which is used by the
     * specified controller to interpret sketch input.
     */
    public BasicInterpreter (SketchController c) {
        _controller = c;
        setEnabled(true);
    }
    
    /**
     * Append the given new point/timestamp to the current
     * stroke.  Consume the event when done.
     *
     * This appends the most recent point to the _strokeBuffer, not
     * _curStroke.  It then invokes appendStroke in SketchLayer (if
     * we're using a SketchPane) which paints this last segment in the
     * stroke (not the whole stroke).  The prior segments of the
     * stroke must have been drawn by the SketchLayer and they stay
     * rendered (canvas does not get repainted, so they are still
     * visible).  Consume the event at the end so that other layers
     * don't attempt to process the event.
     *
     * If we're not using a SketchPane, then call updateSymbol on the
     * sketch model.  This is because the stroke in _curSymbol has
     * changed and must be rerendered.  The model will send an event
     * to the RepaintListener (in SketchController) telling it to
     * update the symbol.
     */
    protected /*final*/ void appendStroke (LayerEvent e) {
        _strokeBuffer.addVertex((float)e.getLayerX(),
                (float)e.getLayerY(), e.getWhen());
        GraphicsPane gp = _controller.getSketchPane();
        if(gp instanceof SketchPane) {
            SketchLayer l = ((SketchPane)gp).getSketchLayer();
            l.appendStroke(e.getLayerX(), e.getLayerY());
        }
        else {
            _controller.getSketchModel().updateSymbol(_curSymbol);
        }
    }

    /**
     * Called at the end of the mouseReleased method to
     * finish the drawing of a stroke.
     *
     * Instantiate a new TimedStroke for _curStroke.  The data is
     * copied over from _strokeBuffer.  Set _curSymbol's stroke to be
     * _curStroke.  If we're using a SketchPane, we would need to add
     * the symbol to the sketch model at this point.  This is because
     * so far, we've been using the SketchLayer to render the last
     * segment in the stroke, so the stroke figure doesn't exist, and
     * if the canvas repaints, the rendered segments will go away.  By
     * adding the symbol to the sketch model, we're making the stroke
     * permanent so that it will be redrawn every time the canvas
     * repaints.  The SketchLayer rendering is used to speed up the
     * rendering process as the user draws, therefore it only paints
     * the last segment of a stroke.  Since we're not repainting the
     * whole canvas nor the whole stroke, the prior drawing stays with
     * the addition of the last piece of the stroke painted on the
     * canvas.
     *
     * If we're not using a SketchPane, the symbol would have already
     * been added to the model in startStroke, otherwise we wouldn't
     * see the stroke as it is being drawn on the canvas.
     */
    protected /*final*/ void finishStroke (LayerEvent e) {
        GraphicsPane gp = _controller.getSketchPane();
        //        _curStroke = new TimedStroke(_strokeBuffer);
        _curStroke = RemoveDupPtsStrokeFilter.removeDupPts(_strokeBuffer);
        _curSymbol.setStroke(_curStroke);
        if(gp instanceof SketchPane) {
            _controller.getSketchModel().addSymbol(_curSymbol);
        }
    }

    /**
     * Return the controller that uses this interpreter.
     */
    public final SketchController getController () {
        return _controller;
    }

    /**
     * Return the current stroke being drawn.
     */
    public final TimedStroke getCurrentStroke () {
        return _curStroke;
    }

    /**
     * Return the symbol for current stroke being drawn.
     */
    public final Symbol getCurrentSymbol(){
        return _curSymbol;
    }
    
    /**
     * We're consuming motion events.
     */
    public boolean isMotionEnabled () {
        return true;
    }

    /**
     * Update the current stroke and its visual representation.
     */
    public void mouseDragged (LayerEvent e) {
        appendStroke(e);
        e.consume();
    }
    
    /** Consume the event, for efficiency.
     */
    public void mouseEntered (LayerEvent e) {
        e.consume();
    }

    /** Consume the event, for efficiency.
     */
    public void mouseExited (LayerEvent e) {
        e.consume();
    }
 
    /** Consume the event, for efficiency.
     */
    public void mouseMoved (LayerEvent e) {
        e.consume();
    }
    
    /**
     * Instantiate a new stroke and add its visual representation
     * as a symbol in the pane.
     */
    public void mousePressed (LayerEvent e) {
        startStroke(e);
        appendStroke(e);
        e.consume();
    }
    
    /**
     * Update the current stroke and its visual representation,
     * then reset the stroke to "null".
     */
    public void mouseReleased (LayerEvent e) {
        appendStroke(e);
        finishStroke(e);
        e.consume();
    }

    /**
     * Remove the current figure from the figure layer.
     */
    public void removeCurrentSymbol () {
        // public for jdk1.2.2 compatability
        _controller.getSketchModel().removeSymbol(_curSymbol);
    }
    
    /**
     * This method is invoked upon mouse down.  Reset the
     * _strokeBuffer to clear the previou stroke.  Set the current
     * stroke (_curStroke) to point to the stroke buffer, and
     * instantiate a StrokeSymbol (_curSymbol) that paints the stroke
     * with outline/fill color and line width specified in the
     * controller.
     *
     * If a SketchPane is used, get the SketchLayer and call
     * startStroke, this will set the starting point of the line
     * segment to be drawn.  SketchLayer will take care of drawing the
     * last line segment in the current stroke, the prior segments
     * stay on the canvas, they are not being rerendered.  If a
     * SketchPane is not being used, then add _curSymbol to the sketch
     * model so that it can be rendered ('cause there will be no
     * SketchLayer).
     */
    protected /*final*/ void startStroke (LayerEvent e) {
        _strokeBuffer.reset();
        _curStroke = _strokeBuffer;
        _curSymbol = new StrokeSymbol(_curStroke, _controller.getPenColor(),
                _controller.getFillColor(),_controller.getLineWidth());

        GraphicsPane gp = _controller.getSketchPane();
        if(gp instanceof SketchPane) {
            SketchLayer l = ((SketchPane)gp).getSketchLayer();
            l.startStroke(e.getLayerX(), e.getLayerY());
        }
        else {
            _controller.getSketchModel().addSymbol(_curSymbol);
        }
    }
}

