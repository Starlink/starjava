/*
 * $Id: PanZoomInterpreter.java,v 1.9 2001/08/28 06:37:13 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.StrokeSymbol;
import diva.sketch.recognition.BasicStrokeRecognizer;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.VotingStrokeRecognizer;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.toolbox.BasicFigure;

import java.awt.Color;

/**
 * 
 * @author  Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class PanZoomInterpreter extends AbstractInteractor {
    public final static String PZ = "pz.tc";
    /**
     * Used as the padding for making a rectangle around a point.
     */
    private int HALO = 10;

    /**
     * The color used for sketching.  Default to black.
     */
    private Color _penColor = Color.black;

    /**
     * The line width used to draw strokes.
     */
    private float _lineWidth = 2f;
    
    /**
     * The sketch controller.
     */
    private PanZoomController _controller;

    /**
     * The current stroke that's being drawn.
     */
    private TimedStroke _curStroke;
    
    /**
     * The current figure object that displays the current stroke.
     */
    private BasicFigure _curFigure;

    /**
     * The current symbol which wraps the current stroke and keeps 
     * visual information about the stroke (color, line width).
     */
    private StrokeSymbol _curSymbol;

    private StrokeRecognizer _recognizer;

    public PanZoomInterpreter (PanZoomController c) throws Exception {
        _controller = c;
        StrokeRecognizer r = new CachingStrokeRecognizer(new LocalRecognizer(new java.io.FileReader(PZ)));
        StrokeRecognizer []children = {new PanRecognizer(r),
                                       new ZoomRecognizer(r)};
        _recognizer = new VotingStrokeRecognizer(children);
    }
    
    /**
     * Append the given new point/timestamp to the current
     * stroke.
     */
    protected final void appendStroke (LayerEvent e) {
        if(_curStroke != null) {
            float x = (float)e.getX();
            float y = (float)e.getY();
            _curStroke.addVertex(x, y, e.getWhen());
            _curFigure.repaint();
        }
    }
    
    /**
     * Utility function.  Remove the current figure from the
     * figure layer.
     */
    protected void removeCurrentFigure () {
        FigureLayer layer = _controller.getSketchPane().getForegroundLayer();
        layer.remove(_curFigure);
    }
    
    /**
     * This method is invoked upon mouse down.  A TimedStroke object
     * and a BasicFigure object are created to represent the stroke
     * being drawn.  The figure is added to the pane, so that the user
     * can see what he's drawing.
     */
    protected final void startStroke () {
        _curStroke = new TimedStroke();
        _curSymbol = new StrokeSymbol(_curStroke, Color.black, null, 2);
        _curFigure = new BasicFigure(_curStroke);
        _curFigure.setUserObject(_curSymbol);
        _curFigure.setStrokePaint(_penColor);
        _curFigure.setLineWidth(_lineWidth);
    }

    /**
     * Called upon mouse released event.  Set current stroke,
     * symbol and figure objects to null.
     */
    protected final void finishStroke () {
        _curStroke = null;
        _curFigure = null;
	_curSymbol = null;
    }
    
    /**
     * We're not handling motion events.
     */
    public boolean isMotionEnabled () {
        return false;
    }
    
    /**
     * Update the current stroke and its visual representation.
     */
    public void mouseDragged (LayerEvent e) {
        appendStroke(e);
        RecognitionSet rset = _recognizer.strokeModified(_curStroke);
        Recognition r = rset.getBestRecognition();
        if(r != null){
            if(r.getType().equals(PanRecognizer.PanData.type)){
                PanRecognizer.PanData data =(PanRecognizer.PanData)r.getData();
                double dx = data.getPanX();
                double dy = data.getPanY();
                _controller.pan(dx, dy);
            }
            else if(r.getType().equals(ZoomRecognizer.ZoomData.type)){
                ZoomRecognizer.ZoomData data =
                    (ZoomRecognizer.ZoomData)r.getData();
                double cx = data.getCenterX();
                double cy = data.getCenterY();
                double zoom = data.getZoomAmount();
                _controller.zoom(cx, cy, zoom, _curStroke);
            }
        }
    }
    
    /**
     * Instantiate a new stroke and add its visual representation
     * as a symbol in the pane.
     */
    public void mousePressed (LayerEvent e) {
        startStroke();
        appendStroke(e);
        _recognizer.strokeStarted(_curStroke);
    }
    
    /**
     * Update the current stroke and its visual representation,
     * and interpret the stroke using either the command recognizer
     * or the UML recognizer.  This depends on with which mouse
     * button the stroke is drawn.
     */
    public void mouseReleased (LayerEvent e) {
        appendStroke(e);
        finishStroke();
        _recognizer.strokeCompleted(_curStroke);        
        _controller.strokeCompleted();
    }

    public class LocalRecognizer extends BasicStrokeRecognizer {
        public LocalRecognizer(java.io.Reader trainingFile) throws Exception {
            super(trainingFile);
        }

        public RecognitionSet strokeModified (TimedStroke s) {
            return strokeCompleted(s);
        }
    }
}



