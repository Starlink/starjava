/*
 * $Id: DefaultDeletionPolicy.java,v 1.1 2002/09/21 00:08:15 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graphx.toolbox;

import diva.graphx.*;

import diva.canvas.interactor.SelectionModel;
import diva.canvas.Figure;
import diva.canvas.JCanvas;
import diva.canvas.GraphicsPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * A default implementation of a deletion policy. This policy disconnects
 * edges that are connected to a node that will be deleted.
 *
 * @author John Reekie
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class DefaultDeletionPolicy implements DeletionPolicy {

    /** Handle an edge that is connected at the head to a node that is about
     * to be deleted, but which is not itself in the selection.
     */
    public void connectedHead (GraphController controller,
			       GraphModel model,
			       Object edge,
			       Object node) {
	controller.setHead(edge, null);
    }

    /** Handle an edge that is connected at the tail to a node that is about
     * to be deleted, but which is not itself in the selection.
     */
    public void connectedTail (GraphController controller,
			       GraphModel model,
			       Object edge,
			       Object node) {
	controller.setTail(edge, null);
    }
}
