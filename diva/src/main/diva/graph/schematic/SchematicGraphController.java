/*
 * $Id: SchematicGraphController.java,v 1.14 2001/07/22 22:01:25 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.graph.*;
import diva.graph.basic.*;
import diva.graph.layout.*;

import diva.canvas.CanvasUtilities;
import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.Site;

import diva.canvas.connector.*;

import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.CompositeInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.SelectionDragger;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.LabelFigure;

import diva.util.Filter;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.SwingConstants;

/**
 * A graph controller for dealing with schematic graphs.  Here there are
 * two kinds of nodes, "entity" nodes and "port" nodes.  Entities contain
 * the ports, and are connected by them
 *
 * @author 	Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version	$Revision: 1.14 $
 * @rating      Red
 */
public class SchematicGraphController extends AbstractGraphController {
    /**
     * The global count for the default node/edge creation.
     */
    private int _globalCount = 0;

    /** The selection interactor for drag-selecting nodes
     */
    private SelectionDragger _selectionDragger;

    /** The interactor that interactively creates edges
     */
    private EdgeCreator _edgeCreator;

    /** The entity controller
     */
    private BasicNodeController _entityController;

    /** The port controller
     */
    private BasicNodeController _portController;

    /** The link controller
     */
    private BasicEdgeController _linkController;

    /** The filter for control operations
     */
    private MouseFilter _controlFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    /** The application that is using this graph controller.
     */
    diva.gui.Application _application;

    /**
     * Create a new Graph controllers with node and edge controllers
     * suitable for a simple schematic editor.
     */
    public SchematicGraphController (diva.gui.Application application) {
	_application = application;

	BasicNodeRenderer nodeRenderer = new BasicNodeRenderer(this);
	nodeRenderer.setNodeShape(new Rectangle2D.Double(0.0, 0.0,
							 15.0, 15.0));
	nodeRenderer.setCompositeShape(new Rectangle2D.Double(0.0, 0.0,
							      200.0, 200.0));
	nodeRenderer.setNodeFill(Color.black);

	_entityController = new BasicNodeController(this);	
	_entityController.setNodeRenderer(nodeRenderer);

	_portController = new BasicNodeController(this);
	// The ports are terminals with a fixed attachment site and a fixed 
	// direction.
	_portController.setNodeRenderer(new NodeRenderer() {
	    public Figure render(Object n) {
		Rectangle2D shape = 
		    new Rectangle2D.Double(0.0, 0.0, 15.0, 15.0);
		Figure figure = new BasicFigure(shape, Color.black);
		Site site = new PerimeterSite(figure, 0);
		site.setNormal(CanvasUtilities.getNormal(SwingConstants.EAST));
		return new TerminalFigure(figure, 
					  new FixedNormalSite(site));
	    }
	});
	
	_portController.setNodeInteractor(new CompositeInteractor());
	
	_linkController = new BasicEdgeController(this);
	_linkController.setEdgeRenderer(new ManhattanEdgeRenderer());
			
	addGraphViewListener(new IncrementalLayoutListener(new IncrLayoutAdapter(new AbstractGlobalLayout(new BasicLayoutTarget(this)) {
	    public void layout(Object node) {
		GraphModel model = getLayoutTarget().getGraphModel();
		if(!model.isComposite(node) || node == model.getRoot()) 
		    return;
		Iterator nodes = model.nodes(node);
		int count = 0;
		while(nodes.hasNext()) {
		    nodes.next();
		    count++;
		}
		CompositeFigure figure = 
		    (CompositeFigure)getLayoutTarget().getVisualObject(node);
		
		nodes = model.nodes(node);
		int number = 0;
		while(nodes.hasNext()) {
		    number ++;
		    Object port = nodes.next();
		    // If there is no figure, then ignore this port.  This may
		    // happen if the port hasn't been rendered yet.
		    if(getLayoutTarget().getVisualObject(port) == null) 
			continue;
		    Rectangle2D portBounds = 
			getLayoutTarget().getBounds(port);
		    BoundsSite site = 
			new BoundsSite(figure.getBackgroundFigure(), 0, 
				       SwingConstants.EAST, 
				       100.0 * number / (count+1));
		    getLayoutTarget().translate(port, 
					site.getX() - portBounds.getX(),
					site.getY() - portBounds.getY());
		}		    
	    }
	}), new Filter() {
	    public boolean accept(Object o) {
		GraphModel model = getGraphModel();
		if(model.isNode(o)) {
		    Object parent = model.getParent(o);
		    return parent != model.getRoot();
		} else {
		    return false;
		}
	    }
	}));
    }

    /**
     * Initialize all interaction on the graph pane. This method
     * is called by the setGraphPane() method of the superclass.
     * This initialization cannot be done in the constructor because
     * the controller does not yet have a reference to its pane
     * at that time.
     */
    protected void initializeInteraction () {
        GraphPane pane = getGraphPane();

        // Create and set up the selection dragger
        _selectionDragger = new SelectionDragger(pane);
        _selectionDragger.addSelectionInteractor(
            (SelectionInteractor)getLinkController().getEdgeInteractor());
	_selectionDragger.addSelectionInteractor(
	    (SelectionInteractor)getEntityController().getNodeInteractor());
	
        // Create the interactor that drags new edges.
	_edgeCreator = new EdgeCreator(this) {
	    public Object createEdge() {	
		Object semanticObject = Integer.valueOf(_globalCount++);
		BasicGraphModel bgm = (BasicGraphModel)getGraphModel();
	        return bgm.createEdge(semanticObject);
	    }
	};
	_edgeCreator.setMouseFilter(_controlFilter);
	((CompositeInteractor)getPortController().getNodeInteractor()).addInteractor(_edgeCreator);
    }

    /** Return the entity controller.
     */
    public BasicNodeController getEntityController() {
	return _entityController;
    }

    /** Return the port controller.
     */
    public BasicNodeController getPortController() {
	return _portController;
    }

    /** Return the link controller.
     */
    public BasicEdgeController getLinkController() {
	return _linkController;
    }

    /**
     * Given an edge, return the controller associated with that
     * edge.
     */
    public EdgeController getEdgeController(Object edge) {
	return _linkController;
    }
  
    /**
     * Given an node, return the controller associated with that
     * node.
     */
    public NodeController getNodeController(Object node) {
	if(getGraphModel().isComposite(node)) 
	    return _entityController;
	else
	    return _portController;
    }

    /** A class that renders edges as ManhattanEdges.
     */
    public class ManhattanEdgeRenderer implements EdgeRenderer {
	/**
         * Render a visual representation of the given edge.
         */
        public Connector render(Object edge, Site tailSite, Site headSite) {
            AbstractConnector c = new ManhattanConnector(tailSite, headSite);
	    c.setLabelFigure(new LabelFigure("edge"));
	    return c;
        }
    }
}


