/*
 * $Id: BubbleGraphDemo.java,v 1.6 2001/07/22 22:01:20 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.graph.basic;

import diva.gui.AppContext;
import diva.gui.BasicFrame;
import diva.canvas.*;
import diva.graph.*;
import diva.graph.toolbox.DeletionListener;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Another graph demo.  This uses a different rendering strategy for
 * the same graph model as the BasicGraphDemo.  Nodes are rendered as
 * round bubble and edges are rendered as curved arcs.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Steve Neuendorffer  (neuendor@eecs.berkeley.edu) 
 * @version $Revision: 1.6 $
 * @rating Red
 */
public class BubbleGraphDemo {
    /**
     * Construct a new instance of graph demo, which does the work of
     * setting up the graphs and displaying itself.
     */
    public static void main(String argv[]) {
        new BubbleGraphDemo(new BasicFrame("Bubble Graph Demo"));
    }

    public BubbleGraphDemo(AppContext context) {
        JGraph jg = new JGraph(new BubblePane());
	context.getContentPane().add("Center", jg);

	ActionListener deletionListener = new DeletionListener();
        jg.registerKeyboardAction(deletionListener, "Delete",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        jg.setRequestFocusEnabled(true);

        context.setSize(600, 400);
        context.setVisible(true);
    }
}







