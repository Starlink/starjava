/*
 * $Id: TraceRenderer.java,v 1.6 2002/05/16 21:20:06 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.trace;

import diva.canvas.CompositeFigure;
import diva.canvas.Figure;

import java.awt.geom.Rectangle2D;

/**
 * An interface that defined how traces are to be displayed.
 *
 * @author 	John Reekie (johnr@eecs.berkeley.edu)
 * @version	$Revision: 1.6 $
 * @rating      Red
 */
public interface TraceRenderer {

    /** Remove the mapping of a Trace to its rendering. This
     * enables dead traces to be garbage-collected.
     */
    public void forgetTrace (TraceModel.Trace trace);

    /** Remove the mapping of a Trace element to its rendering. This
     * is used by moving traces, to enable dead objects to be
     * garbage-collected.
     */
    public void forgetTraceElement (TraceModel.Element element);

    /** Get the renderering of a Trace element. If this renderer has
     * previously rendered the given element, then it must return
     * that renderering, otherwise it must return null. The implementation
     * can assume that the element is contained within a trace,
     * so that it has a unique ID within that trace.
     */
    public Figure getTraceElementRendering(TraceModel.Element element);

    /** Get the renderering of a Trace. If this rendered has
     * previously rendered the given trace, then it must return
     * that renderering, otherwise it must return null.
     */
    public CompositeFigure getTraceRendering(TraceModel.Trace trace);

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
            TraceModel.Trace trace, Rectangle2D bounds);

    /** Create a new Figure for an element of a trace. Any figure can
     * be used, but to be useful it must display itself so that
     * it marks the region given by its bounding box.
     */
    public Figure renderTraceElement (
            TraceModel.Element element, Rectangle2D bounds);

    /** Update the rendering of a trace. Typically this is used to
     * update elements such as labels that may have been created
     * by the renderer but which the client doesn't know how
     * to change.
     */
    public void updateTrace (TraceModel.Trace trace);

    /** Update the rendering of a trace element. Typically this is
     * used to update elements such as labels that may have been
     * created by the renderer but which the client doesn't know how
     * to change.
     */
    public void updateTraceElement (TraceModel.Element element);
}




