/*
 * $Id: TraceTutorial.java,v 1.7 2002/05/16 21:54:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.compat.trace;

import diva.canvas.CanvasPane;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicRectangle;

import diva.gui.BasicFrame;

import java.awt.Color;
import java.awt.Shape;

/**
 * An example showing how to construct and use a Trace surface.
 *
 * @author John Reekie
 * @version $Revision: 1.7 $
 */
public class TraceTutorial {

    // The JCanvas
    private JCanvas _canvas;

    // The TracePane
    private TracePane _tracePane;

    // The data in the trace 
    private TraceModel _model;

    /** Create a JCanvas and put it into a window
     */
    public TraceTutorial () {
        _tracePane = new TracePane();
        _canvas = new JCanvas(_tracePane);

        BasicFrame frame = new BasicFrame("Trace tutorial", _canvas);
        frame.setSize(600,400);
        frame.setVisible(true);
    }

    /** Create a TraceModel and add some data to it
     */
    public void createTraceModel () {
        _model = new TraceModel();
        
        // First trace
        TraceModel.Trace trace1 = new TraceModel.Trace();
        _model.addTrace("trace1", trace1);
        trace1.add(new TraceModel.Element(0,2,0));
        trace1.add(new TraceModel.Element(4,6,1));
        trace1.add(new TraceModel.Element(8,10,2));

        // Second trace
        TraceModel.Trace trace2 = new TraceModel.Trace();
        _model.addTrace("trace2", trace2);
        trace2.add(new TraceModel.Element(1,5,3));
        trace2.add(new TraceModel.Element(7,9,4));
    }

    /** Display the trace model
     */
    public void displayTrace () {
        ((TraceView) _tracePane.getTraceView()).setTraceModel(_model);
    }

     /** Add some more traces to the model and view
     */
    public void addMoreTraces () {
        TraceView view = _tracePane.getTraceView();
        TraceModel.Trace trace1 = _model.getTrace("trace1");
        TraceModel.Element elt1 = new TraceModel.Element(11,12,5);
        TraceModel.Element elt2 = new TraceModel.Element(12,16,6);
        trace1.add(elt1);
        trace1.add(elt2);
        view.drawTraceElement(elt1);
        view.drawTraceElement(elt2);
    }

    /** Main function
     */
    public static void main (String argv[]) {
        TraceTutorial ex = new TraceTutorial();
        ex.createTraceModel();
        ex.displayTrace();
        ex.addMoreTraces();
    }
}


