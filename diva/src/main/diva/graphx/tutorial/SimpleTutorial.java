/*
 * $Id: SimpleTutorial.java,v 1.2 2002/08/19 07:12:35 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.graphx.tutorial;
import diva.graphx.GraphController;
import diva.graphx.GraphModel;

import diva.graphx.toolbox.BasicGraphModel;

import diva.canvas.JCanvas;
import diva.canvas.GraphicsPane;

import diva.gui.AppContext;
import diva.gui.BasicFrame;
import javax.swing.SwingUtilities;

/**
 * This is the most basic tutorial, popping up an empty graph
 * editing window.  Control-click to add nodes,
 * select a node and control-drag to create new edges.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class SimpleTutorial {
    /**
     * Pop up an empty graph editing window.
     */
    public static void main(String argv[]) {
        final AppContext context = new BasicFrame("Simple Tutorial");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {       
                new SimpleTutorial(context);
                context.setVisible(true);
            }
        });
    }

    public SimpleTutorial(AppContext context) {
	JCanvas canvas = new JCanvas();
	GraphModel model = new BasicGraphModel();
	GraphController gc = new
	    SimpleGraphController((GraphicsPane) canvas.getCanvasPane(),
				  model);
        // JGraph jg = new JGraph(new GraphPane(new SimpleGraphController(),
        //        new BasicGraphModel()));
        context.getContentPane().add("Center", canvas);
    }
}
