/*
 * $Id: SimpleEdgeController.java,v 1.1 2002/08/19 07:12:32 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.tutorial;

import diva.graphx.GraphController;
import diva.graphx.EdgeController;
import diva.graphx.GraphModel;

import diva.canvas.Figure;
import diva.canvas.FigureContainer;
import diva.canvas.Site;
import diva.canvas.connector.AbstractConnector;
import diva.canvas.connector.ArcConnector;
import diva.canvas.connector.Connector;
import diva.canvas.connector.StraightConnector;
import diva.canvas.connector.Arrowhead;

/**
 * A simple implementation of the EdgeRenderer interface.
 * This renderer creates straight-line connectors with
 * an arrow at the head.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class SimpleEdgeController extends EdgeController {    

    /** Construct a new SimpleNodeController
     */
    public SimpleEdgeController (GraphController gc) {
	super(gc);
    }

    /**
     * Render a visual representation of the given edge.
     */
   public Connector renderEdge (Object edge,
				Object parent,
				FigureContainer container,
				Site tailSite, Site headSite) {
        AbstractConnector c;
        Figure tf = tailSite.getFigure();
        Figure hf = headSite.getFigure();

        //if the edge is a self loop, create an ArcConnector instead!
	// um... is this really right? -- hjr
        if((tf != null)&&(hf != null)&&(tf == hf)){
            c = new ArcConnector(tailSite, headSite);
        }
        else {
            c = new StraightConnector(tailSite, headSite);
        }
        Arrowhead arrow = new Arrowhead(
                headSite.getX(), headSite.getY(), headSite.getNormal());
        c.setHeadEnd(arrow);

	container.add(c);
        return c;
   }
}
