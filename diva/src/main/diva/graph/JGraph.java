/*
 * $Id: JGraph.java,v 1.18 2000/07/09 19:57:12 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.graph;

import diva.graph.*;
import diva.canvas.*;
import diva.canvas.event.*;
import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;

/**
 * A graph widget analagous to java.swing.JTree.
 * JGraph contains a GraphModel and can be customized
 * to render it in an application-specific way through
 * its GraphPane member variable.
 *
 * @see GraphModel
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.18 $
 */
public class JGraph extends JCanvas {


    /**
     * Construct a new JGraph with the given graph pane.
     */
    public JGraph(GraphPane pane) {
	super(pane);
    }

    /**
     * Return the canvas pane, which is of type
     * GraphPane.
     */
    public GraphPane getGraphPane() {
        return (GraphPane)getCanvasPane();
    }

    /**
     * Set the graph pane of this widget.
     */
    public void setGraphPane(GraphPane p) {
        setCanvasPane(p);
    }
}

