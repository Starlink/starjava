/*
 * $Id: ArcManipulator.java,v 1.4 2000/05/02 00:43:18 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.connector;

import diva.canvas.Site;
import diva.canvas.Figure;
import diva.canvas.FigureContainer;
import diva.canvas.FigureDecorator;

import diva.canvas.event.LayerEvent;
import diva.canvas.event.LayerMotionListener;
import diva.canvas.event.LayerEventMulticaster;
import diva.canvas.event.LayerListener;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.GrabHandleFactory;
import diva.canvas.interactor.BasicGrabHandleFactory;

import diva.canvas.toolbox.BasicHighlighter;

/**
 * A manipulator for arc connectors. In addition to the grab handles
 * at the ends of the connector, it attaches a handle in the center
 * of the connector so that the connector can be reshaped.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public class ArcManipulator extends ConnectorManipulator {

    /**
     * Construct a new manipulator that uses rectangular grab-handles.
     */
    public ArcManipulator () {
        super();
    }

    /**
     * Construct a new manipulator using the given grab-handle factory. 
     */
    public ArcManipulator(GrabHandleFactory f) {
        super(f);
    }

    /** Create a new instance of this manipulator. The new
     * instance will have the same grab handle, and interaction role
     * for grab-handles, as this one.
     */
    public FigureDecorator newInstance (Figure f) {
        ArcManipulator m = new ArcManipulator();
        m.setGrabHandleFactory(this.getGrabHandleFactory());
        m.setHandleInteractor(this.getHandleInteractor());
        return m;
    }
}

