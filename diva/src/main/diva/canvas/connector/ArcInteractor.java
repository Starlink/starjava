/*
 * $Id: ArcInteractor.java,v 1.2 2002/01/29 04:06:34 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */
package diva.canvas.connector;

import diva.canvas.Figure;
import diva.canvas.CompositeFigure;
import diva.canvas.FigureContainer;
import diva.canvas.FigureDecorator;
import diva.canvas.CanvasPane;
import diva.canvas.CanvasUtilities;
import diva.canvas.Site;
import diva.canvas.TransformContext;

import diva.canvas.event.LayerEvent;
import diva.canvas.event.LayerEventMulticaster;
import diva.canvas.event.LayerListener;
import diva.canvas.event.LayerMotionListener;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.Manipulator;

import diva.util.java2d.ShapeUtilities;
import diva.util.Filter;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import java.util.ArrayList;
import java.util.Iterator;


/** An interactor for dragging either end of an arc connector and for
 *  for altering the shape of the arc by dragging a midpoint grab handle.
 *  This class is designed for use in conjunction with ArcManipulator.
 *
 * @version $Revision: 1.2 $
 * @author Edward A. Lee
 */
public class ArcInteractor extends ConnectorInteractor {

    /** Create a new interactor to be used with the given manipulator.
     */
    public ArcInteractor (ArcManipulator m) {
        super(m);
    }

    /** Fire a connector event to all connector listeners.
     */
    protected void fireConnectorEvent (int id) {
        // NOTE: The following cast is safe because the method that
        // creates grab handles in ArcManipulator ensures that the
        // connector is an instance of ArcConnector.
        ArcConnector connector = (ArcConnector)getConnector();
        Site site = getHandle().getSite();
        int end;
        if (site == connector.getTailSite()) {
            end = ConnectorEvent.TAIL_END;
        } else if (site == connector.getMidpointSite()) {
            end = ConnectorEvent.MIDPOINT;
        } else {
            // Default is head site.
            end = ConnectorEvent.HEAD_END;
        }
        ConnectorEvent event = new ConnectorEvent(
                id,
                connector.getLayer(),
                getTarget(),
                connector,
                end);
        _notifyConnectorListeners(event, id);
    }

    /** Respond to translation of the grab-handle. Move the
     *  grab-handle, and adjust the connector accordingly,
     *  snapping it to a suitable target if possible.
     */
    public void translate (LayerEvent e, double dx, double dy) {
        // NOTE: The following cast is safe because the method that
        // creates grab handles in ArcManipulator ensures that the
        // connector is an instance of ArcConnector.
        ArcConnector connector = (ArcConnector)getConnector();
        Site site = getHandle().getSite();

        // Process movement in one of the end manipulators
        if (site != connector.getMidpointSite()) {
            super.translate(e, dx, dy);

        } else {
	    // Process movement of the mid-point manipulator. The
	    // distance we want to tell the connector to move, is
	    // the difference between where it is now, and where
	    // we think it should be -- this is because of the
	    // inexactness of ArcConnector.translateMidpoint().
	    //
	    double targetX = getX() + dx;
	    double targetY = getY() + dy;
		
	    Point2D mid = connector.getArcMidpoint();
	    
	    double newdx = targetX - mid.getX();
	    double newdy = targetY - mid.getY();
		
	    // Apply a couple of limiting functions to this, to avoid
	    // "yoyo-ing"
	    if (newdx > 0 && dx < 0 || newdx < 0 && dx > 0) {
		newdx = 0;
	    }
	    if (newdy > 0 && dy < 0 || newdy < 0 && dy > 0) {
		newdy = 0;
	    }
	    double limit = 25.0;
	    if (newdx > limit) {
		newdx = limit;
	    } else if (newdx < -limit) {
		newdx = -limit;
	    }
	    if (newdy > limit) {
		newdy = limit;
	    } else if (newdy < -limit) {
		newdy = -limit;
	    }
	    // Tell the connector to move its midpoint
	    connector.translateMidpoint(newdx, newdy);
	    //connector.translateMidpoint(dx, dy); // This one is "open-loop"
	    connector.reroute();
	}
	fireConnectorEvent(ConnectorEvent.CONNECTOR_DRAGGED);
    }
}
