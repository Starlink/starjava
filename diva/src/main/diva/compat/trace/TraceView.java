/*
 * $Id: TraceView.java,v 1.11 2002/05/16 21:20:06 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.trace;

import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.LabelFigure;

import java.util.Iterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingConstants;

/**
 * TraceView is the part of the TracePane widget which controls the display
 * of trace elements.
 *
 * @author   John Reekie (johnr@eecs.berkeley.edu)
 * @version  $Revision: 1.11 $
 * @rating      Red
 */
public class TraceView {
    /* Layout parameters
     */
    private double _traceX = 10;
    private double _traceY = 10;
    private double _traceWidth = 400;
    private double _traceHeight = 50;
    private double _traceSpacing = 10;

    /** The scaling factor, from time to coordinates
     */
    private double _timeScale = 10.0;

    /** The number of traces
     */
    private double _traceCount = 0;

    /** The model that is being displayed.
     */
    private TraceModel _model;

    /** The controller of this view.
     */
    private TraceController _controller;

    /** The graphics pane that this is controlling.
     */
    private GraphicsPane _pane;

    /** The figure layer that we draw on.
     */
    private FigureLayer _layer;

    /** The trace renderer
     */
    private TraceRenderer _renderer = new BasicTraceRenderer();

    /**
     * Construct a trace view. The trace cannot be used until
     * it has a pane set with setTracePane(), which is
     * called when the view is added to the pane.
     */
    public TraceView () {
        //
    }

    /**
     * Debugging output.
     */
    private void debug(String s) {
        System.err.println("TraceView: " + s);
    }
    
    /** Clear the view. This removes all information about the
     * figure from the view and from the renderer. Note that the
     * model must not be cleared until after this method is called!
     */
    public void clear () {
        for (Iterator i = _model.traces(); i.hasNext(); ) {
             TraceModel.Trace trace = (TraceModel.Trace) i.next();
             Figure traceFigure = _renderer.getTraceRendering(trace);
             _pane.getForegroundLayer().remove(traceFigure);
             _renderer.forgetTrace(trace);
        }
        _traceCount = 0;
    }

    /**
     * Draw the current model into the view. This method
     * assumes that none of the data in the model
     * has been drawn yet.
     */
    public void drawModel () {
        // FIXME allow vertical layout
        for (Iterator i = _model.traces(); i.hasNext(); ) {
            TraceModel.Trace trace = (TraceModel.Trace) i.next();
            drawTrace(trace);
        }
    }

    /**
     * Draw a new trace on the view.
     */
    public void drawTrace (TraceModel.Trace trace) {
        // Figure out where to put it
        double y = _traceY + _traceCount * (_traceHeight + _traceSpacing);

        // Create the figure for the whole trace
        Rectangle2D bounds = new Rectangle2D.Double(
                _traceX, y, _traceWidth, _traceHeight);
        CompositeFigure traceFigure = _renderer.renderTrace(
                trace, bounds);

        // Add the elements
        for (Iterator i = trace.elements(); i.hasNext(); ) {
            TraceModel.Element elt = (TraceModel.Element) i.next();
            Rectangle2D eltbounds =  new Rectangle2D.Double(
                    _traceX + (elt.startTime * _timeScale), y,
                    (elt.stopTime - elt.startTime) * _timeScale,
                    _traceHeight);
            Figure eltFigure = _renderer.renderTraceElement(
                    elt, eltbounds);
            traceFigure.add(eltFigure);
        }
        // Change the transform
        //AffineTransform at = traceFigure.getTransformContext().getTransform();
        //at.scale(_timeScale, 1.0);

        // display it
        _pane.getForegroundLayer().add(traceFigure);
        _traceCount++;

        // If the user object is a string, draw it
        Object o = trace.getUserObject();
        if (o != null && o instanceof String) {
            LabelFigure labelFigure = new LabelFigure((String) o);
            // FIXME: bogosity here...
            labelFigure.setAnchor(SwingConstants.NORTH_WEST);
            labelFigure.translateTo(_traceX, y+_traceHeight);
            _pane.getForegroundLayer().add(labelFigure);
        }
    }

    /**
     * Draw a new trace element
     */
    public void drawTraceElement (TraceModel.Element elt) {
        // Figure out where to put it
        int index = elt.getTrace().getID();
        double y = _traceY + index * (_traceHeight + _traceSpacing);

        CompositeFigure traceFigure =
            _renderer.getTraceRendering(elt.getTrace());

        Rectangle2D eltbounds =  new Rectangle2D.Double(
                _traceX + (elt.startTime * _timeScale), y,
                (elt.stopTime - elt.startTime) * _timeScale,
                _traceHeight);

        Figure eltFigure = _renderer.renderTraceElement(
                elt, eltbounds);
        traceFigure.add(eltFigure);
        eltFigure.repaint();
    }
        
    /** Return the trace controller.
     */
    public TraceController getTraceController() {
        return _controller;
    }

    /**
     * Return the trace model being viewed.
     */
    public TraceModel getTraceModel() {
        return _model;
    }

    /**
     * Return the graphics pane of this controller.
     */
    public GraphicsPane getGraphicsPane() {
        return _pane;
    }

    /**
     * Set the trace controller.
     */
    public void setTraceController(TraceController c) {
        _controller = c;
    }

    /**
     * Set the layout parameters of the trace. The coordinates
     * define the rectangular region of the first trace in
     * the view.
     */
    public void setLayout (
            double x, double y, double width, double height, double spacing) {
        // FIXME: rescale existing traces
        _traceX = x;
        _traceY = y;
        _traceWidth = width;
        _traceHeight = height;
        _traceSpacing = spacing;
    }

    /** Set the scaling factor from traces time to coordinates
     */
    public void setTimeScale (double scale) {
        // FIXME: rescale existing traces
        _timeScale = scale;
    }

    /**
     * Set the trace model and display all of its data
     */
    public void setTraceModel (TraceModel model) {
        _model = model;

        // FIXME
        // clear()

        drawModel();
    }

    /**
     * Set the trace pane. This is called by the TracePane.
     */
    public final void setTracePane (TracePane pane) {
        _pane = pane;
    }

    /**
     * Update the display of an existing trace element
     */
    public void updateTraceElement (TraceModel.Element elt) {
        // Figure out where to put it
        int index = elt.getTrace().getID();
        double y = _traceY + index * (_traceHeight + _traceSpacing);

        // Get the right figure
        // FIXME FIXME don't assume rectangle
        BasicFigure eltFigure = (BasicFigure)
            _renderer.getTraceElementRendering(elt);

        // Update its dimensions
        // FIXME This assumes a rectangle...
        Rectangle2D bounds = (Rectangle2D) eltFigure.getShape();

        bounds.setFrame(
                _traceX + (elt.startTime * _timeScale), y,
                (elt.stopTime - elt.startTime) * _timeScale,
                _traceHeight);
        eltFigure.setShape(bounds);
    }
        
}




