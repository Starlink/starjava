/*
 * $Id: JGraph.java,v 1.20 2001/07/24 06:34:51 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
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
 * JGraph functions as a container for an
 * instance of GraphPane, which is a multi-layer graphics
 * object containing (among other things) a layer upon which
 * graph elements are drawn and manipulated.
 *
 * @see GraphModel, GraphPane
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.20 $
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


