/*
 * $Id: SimpleNodeController.java,v 1.2 2002/09/20 02:41:27 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.tutorial;

import diva.graphx.GraphController;
import diva.graphx.NodeController;
import diva.graphx.GraphModel;

import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.FigureContainer;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicRectangle;
import java.awt.Shape;
import java.awt.Paint;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.GeneralPath;

/**
 * A simple renderer for nodes.
 *
 * @author  John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating  Red
 */
public class SimpleNodeController extends NodeController {

    /** Construct a new SimpleNodeController
     */
    public SimpleNodeController (GraphController gc) {
	super(gc);
    }

    /**
     * Render the node and add it to the given parent
     */
    public Figure renderNode (Object node,
			      Object parent,
			      FigureContainer container,
			      double x, double y) {
	BasicRectangle f = new BasicRectangle(0.0,0.0,60.0,60.0, Color.orange);

	// FIXME why doesn't this work?
	f.setStrokePaint(Color.red);
	f.setLineWidth(2.0f);

	container.add(f);
	CanvasUtilities.translateTo(f, x, y);
	return f;
    }

    /**
     * Remove the visual representation of the given node.
     */
    public void unrenderNode (Object node,
				Figure figure) {
	getController().getSelectionModel().removeSelection(figure);
	((FigureContainer) figure.getParent()).remove(figure);
    }
}
