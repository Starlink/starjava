/*
 * $Id: TracePane.java,v 1.5 2002/05/16 21:20:06 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.trace;

import diva.canvas.GraphicsPane;

/**
 * A pane that displays a trace diagram.
 *
 * @author 	John Reekie (johnr@eecs.berkeley.edu)
 * @version	$Revision: 1.5 $
 * @rating Red
 */
public class TracePane extends GraphicsPane {

    /** The view
     */
    private TraceView _view;

    /** The controller
     */
    private TraceController _controller;

    /** Create a new trace pane with a default view, model,
     * and controller.
     */
    public TracePane () {
        this(   new TraceModel(),
                new TraceView(),
                new TraceController());
    }

    /** Create a new trace pane with the given model, and a default
     * view and controller.
     */
    public TracePane (TraceModel model) {
        this(   model,
                new TraceView(),
                new TraceController());
    }

    /** Create a new trace pane with the given model and controller.
     */
    public TracePane (TraceModel model, TraceController controller) {
        this(   model,
                new TraceView(),
                controller);
    }

    /** Create a new trace pane with the given model,
     * view and controller.
     */
    public TracePane (TraceModel model,
            TraceView view,
            TraceController controller) {

        // Set up the view
        _view = view;
        _view.setTracePane(this);
        _view.setTraceController(controller);

        // Set up the controller
        _controller = controller;
        _controller.setTraceView(view);

        //Set up the model
        setTraceModel(model);
    }

    /** Get the trace controller
     */
    public TraceController getTraceController () {
        return _controller;
    }

    /** Get the trace model
     */
    public TraceModel getTraceModel () {
        return _view.getTraceModel();
    }

    /** Get the trace view
     */
    public TraceView getTraceView () {
        return _view;
    }

    /** Set the trace model. Any existing data in the view will be
     * removed, and the new data loaded into the model.
     */
    public void setTraceModel (TraceModel model) {
        // Inform the view
        _view.setTraceModel(model);

        // Inform the controller. It will subscribe to the model
        // and cause the view to be updated.
        _controller.setTraceModel(model);
    }
}


