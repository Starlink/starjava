/*
 * $Id: BasicTraceRenderer.java,v 1.9 2002/05/16 21:20:05 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.trace;

import diva.canvas.CompositeFigure;
import diva.canvas.Figure;

import diva.canvas.toolbox.BasicRectangle;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A basic implementation of trace rendering.
 *
 * @author 	John Reekie (johnr@eecs.berkeley.edu)
 * @version	$Revision: 1.9 $
 * @rating      Red
 */
public class BasicTraceRenderer implements TraceRenderer {

    /** The mapping from traces to their renderings.
     */
    private HashMap _traceMap = new HashMap();

    /** The mappings from trace elements to their renderings.
     * Each element is another mapping from integer IDs to
     * figures.
     */
    private HashMap _elementMap = new HashMap();
 
    /** A mapping from ints to Colors. How lame can you go?
     */
    private Color _colorMap[] = {
        Color.red,
        Color.orange,
        Color.yellow,
        Color.green,
        Color.blue,
        Color.magenta,
        Color.gray,
        Color.black,
        Color.cyan,
        Color.pink
    };

    /** Clear all data from the renderer.
     */
    public void clear () {
        _traceMap = new HashMap();
        _elementMap = new HashMap();
    }

    /** Remove the mapping of a Trace to its rendering. This
     * enables dead traces to be garbage-collected.
     */
    public void forgetTrace (TraceModel.Trace trace) {
        _traceMap.remove(trace);
        _elementMap.remove(trace);
    }

    /** Remove the mapping of a Trace element to its rendering. This
     * is used by moving traces, to enable dead objects to be
     * garbage-collected.
     */
    public void forgetTraceElement (TraceModel.Element element) {
        ArrayList map = (ArrayList) _elementMap.get(element.getTrace());
        if (map != null) {
            map.remove(element.getID());
        }
    }       

    /** Get the renderering of a Trace element. If this renderer has
     * previously rendered the given element, then it returns
     * that renderering, otherwise it returns null. The element
     * must already be contained within a trace.
     */
    public Figure getTraceElementRendering(
            TraceModel.Element element) {
        ArrayList map = (ArrayList) _elementMap.get(element.getTrace());
        if (map != null) {
            return (Figure) map.get(element.getID());
        }
        return null;
    }

    /** Get the renderering of a Trace. If this renderer has
     * previously rendered the given trace, then it returns
     * that renderering, otherwise it returns null.
     */
    public CompositeFigure getTraceRendering(TraceModel.Trace trace) {
        return (CompositeFigure) _traceMap.get(trace);
    }

    /** Create a new Figure for a Trace. The Figure must be a
     * CompositeFigure, and must be able to treat its bounds
     * as the region in which the trace is to be displayed. The
     * figure is not expected to display the elements of the trace,
     * just the marker line or boundary. If the renderering requires
     * a label, then the renderer is expected to provide that label.
     * The renderer should not check if the trace has already
     * been rendered (this is for performance reasons).
     */
    public CompositeFigure renderTrace (
            TraceModel.Trace trace, Rectangle2D bounds) {

        //// FIXME remove outline for now
        //// Figure background = new BasicRectangle(bounds);
        Figure background = new BasicRectangle(bounds, null);

        CompositeFigure rendering = new CompositeFigure();
        rendering.add(background);
        _traceMap.put(trace,rendering);
        _elementMap.put(trace, new ArrayList());
        return rendering;
    }

    /** Create a new Figure for an element of a trace. Any figure can
     * be used, but to be useful it must display itself so that it
     * marks the region given by its bounding box. In this default
     * implementation, a rectangle is produced, with the color
     * produced by taking the integer value of the element's value.
     */
    public Figure renderTraceElement (
            TraceModel.Element element, Rectangle2D bounds) {
        Color color = _colorMap[element.intValue % _colorMap.length];
        Figure rendering = new BasicRectangle(bounds, color);
        ArrayList map = (ArrayList) _elementMap.get(element.getTrace());
        map.add(element.getID(), rendering);
        return rendering;
    }

    /** Update the rendering of a trace. Typically this is used to
     * update elements such as labels that may have been created
     * by the renderer but which the client doesn't know how
     * to change.
     */
    public void updateTrace (TraceModel.Trace trace) {
        // nothing
    }

    /** Update the rendering of a trace element. Typically this is
     * used to update elements such as labels that may have been
     * created by the renderer but which the client doesn't know how
     * to change.
     */
    public void updateTraceElement (TraceModel.Element element) {
       // nothing
    }
}





