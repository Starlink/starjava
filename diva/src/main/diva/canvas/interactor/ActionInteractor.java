/*
 * $Id: ActionInteractor.java,v 1.6 2001/07/22 22:00:36 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */
package diva.canvas.interactor;

import diva.canvas.event.*;

import javax.swing.Action;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;

/** 
 * An interactor that fires an Action when a mouse pressed event occurs.
 *
 * @version $Revision: 1.6 $
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 */
public class ActionInteractor extends AbstractInteractor {

    // The associated action.
    Action _action = null;

    /** Create a new interactor that will throw a NullPointerException
     *  when a mouse button is pressed.  (In some cases we have to set
     *  the action after creating it.)
     */
    public ActionInteractor() {
	setAction(null);
	setMouseFilter(MouseFilter.defaultFilter);
    }

    /** Create a new interactor that will activate the given action.
     */
    public ActionInteractor(Action action) {
	setAction(action);
	setMouseFilter(MouseFilter.defaultFilter);
    }

    /** Return the action associated with this interactor.
     */
    public Action getAction() {
	return _action;
    }

    /** Activate the action referenced by this interactor.  The source of 
     *  the ActionEvent is the layer event.
     */
    public void mousePressed (LayerEvent layerEvent) {
	ActionEvent event = new ActionEvent(layerEvent,
					    layerEvent.getID(),
					    "Pressed",
					    layerEvent.getModifiers());
	_action.actionPerformed(event);
    }

    /** Set the action associated with this interactor.
     */
    public void setAction(Action action) {
	_action = action;
    }
}


