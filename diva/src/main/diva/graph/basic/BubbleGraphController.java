/*
 * $Id: BubbleGraphController.java,v 1.3 2001/07/22 22:01:20 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.basic;

import diva.graph.*;

import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.Site;

import diva.canvas.connector.AutonomousSite;
import diva.canvas.connector.CenterSite;
import diva.canvas.connector.PerimeterSite;
import diva.canvas.connector.PerimeterTarget;
import diva.canvas.connector.Connector;
import diva.canvas.connector.ArcConnector;
import diva.canvas.connector.ArcManipulator;
import diva.canvas.connector.ConnectorEvent;
import diva.canvas.connector.ConnectorListener;
import diva.canvas.connector.ConnectorTarget;

import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionDragger;

import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A controller for bubble-and-arc graph editors.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.3 $
 * @rating      Red
 */
public class BubbleGraphController extends BasicGraphController {
    /**
     * Create a new controller with default node and edge controllers.
     * Set the node renderer to a bubble renderer, and the edge renderer
     * to an arc renderer.
     */
    public BubbleGraphController () {
	NodeController nc = new BasicNodeController(this);
	nc.setNodeRenderer(new BubbleRenderer());
	setNodeController(nc);
	
	EdgeController ec = new BasicEdgeController(this);
	ec.setEdgeRenderer(new ArcRenderer());
	setEdgeController(ec);
    }


}


