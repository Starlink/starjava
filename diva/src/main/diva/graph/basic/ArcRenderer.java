/*
 * $Id: ArcRenderer.java,v 1.3 2000/07/31 22:15:54 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.basic;

import diva.graph.*;

import diva.canvas.Site;
import diva.canvas.connector.Connector;
import diva.canvas.connector.ArcConnector;
import diva.canvas.connector.StraightConnector;
import diva.canvas.connector.Arrowhead;
import diva.canvas.toolbox.LabelFigure;

import java.awt.Font;

/**
 * An EdgeRenderer that draws arcs. To do so, it creates a new
 * instance of ArcConnector and initializes it.
 *
 * @author Edward A. Lee  (eal@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class ArcRenderer implements EdgeRenderer {
    /** Render a visual representation of the given edge.
     */
    public Connector render(Object edge, Site tailSite, Site headSite) {
        // FIXME: Find a way to set the curvature (the third argument).
        ArcConnector c = new ArcConnector(tailSite, headSite);
        Arrowhead arrow = new Arrowhead(
                headSite.getX(), headSite.getY(), headSite.getNormal());
	c.setHeadEnd(arrow);

        Object p = "edge";//edge.getProperty("label");
        String label = p == null ? "#" : (String) p;
	LabelFigure labelFigure = new LabelFigure(label);
        labelFigure.setStyle(Font.ITALIC);
        labelFigure.setSize(14);
        c.setLabelFigure(labelFigure);
        return c;
    }
}

