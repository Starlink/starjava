/*
 * $Id: HighlightInterpreter.java,v 1.5 2001/07/22 22:02:26 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;
import diva.sketch.BasicInterpreter;
import diva.sketch.SketchController;
import diva.sketch.SketchModel;
import diva.sketch.StrokeSymbol;
import diva.sketch.recognition.TimedStroke;
import diva.canvas.GraphicsPane;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.event.LayerEvent;

/**
 * This interpreter highlights, meaning that it puts ink
 * at the back of the drawing rather than at the front.
 
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 * @rating Red
 */
public class HighlightInterpreter extends BasicInterpreter {
    
    /**
     * Create a HighlightInterpreter which is used by the
     * specified controller to interpret sketch input.
     */
    public HighlightInterpreter (SketchController c) {
        super(c);
    }
    
    /**
     * Append the given new point/timestamp to the current
     * stroke.  Consume the event when done.
     */
    protected final void appendStroke (LayerEvent e) {
        _strokeBuffer.addVertex((float)e.getLayerX(),
                (float)e.getLayerY(), e.getWhen());
        GraphicsPane gp = _controller.getSketchPane();
        _controller.getSketchModel().updateSymbol(_curSymbol);
    }

     /**
     * Called at the end of the mouseReleased method to
     * finish the drawing of a stroke.
     */
    protected final void finishStroke (LayerEvent e) {
        GraphicsPane gp = _controller.getSketchPane();
        _curStroke = new TimedStroke(_strokeBuffer);
        _curSymbol.setStroke(_curStroke);
    }
    
    /**
     */
    protected final void startStroke (LayerEvent e) {
        _strokeBuffer.reset();
        _curStroke = _strokeBuffer;
        _curSymbol = new StrokeSymbol(_curStroke, _controller.getPenColor(),
                _controller.getFillColor(),_controller.getLineWidth());
        SketchModel model = _controller.getSketchModel();
        model.addSymbol(model.getSymbolCount(), _curSymbol);
    }   
}








