/*
 * $Id: MultiWindowApplication.java,v 1.2 2001/07/22 22:01:31 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui;

import diva.resource.RelativeBundle;
import diva.resource.DefaultBundle;

import diva.gui.toolbox.ListDataModel;

import java.awt.Insets;
import java.awt.datatransfer.Clipboard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import javax.swing.Action;
import javax.swing.Icon;
//import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

/**
 * An application that manages a group of toplevel frames for each document.
 * These toplevel frames are raised and lowered together.
 *
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public abstract class MultiWindowApplication extends AbstractApplication {
    /** A mapping from content panes to views
     */
    private HashMap _viewMap = new HashMap();

    private MultiWindowListener _multiWindowListener = 
	new MultiWindowListener();

    /** Create a MultiWindow application based on the given context.
     */
    public MultiWindowApplication(AppContext context) {
        super(context);
    }

     /** Create a view to display the given document.  The document
     * should already be added to the application. After calling this
     * method, most callers should set this view to be the current
     * view.
     */
    public abstract View createView (Document d);

    public void addView(View v) {
        super.addView(v);
	
	AppContext context;
	if(v instanceof ContextView) {
	    context = ((ContextView)v).getContext();
	} else {
	    // FIXME
	    throw new UnsupportedOperationException("MultiWindowApplication " +
		"can currently only handle ContextViews");
	}
	// Make the context visible.
	context.setVisible(true);
	
	// FIXME: addContextListener??
	((Window)context).addWindowListener(_multiWindowListener);

	// Yuk we need hash tables to map components to views ek
        _viewMap.put(context, v);
    }

    /**
     * Remove the given view.
     */
    public void removeView(View v) {
        super.removeView(v);
	
	System.out.println("closing...");
	AppContext context;
	if(v instanceof ContextView) {
	    context = ((ContextView)v).getContext();
	} else {
	    // FIXME
	    throw new UnsupportedOperationException(
						    "MultiWindowApplication " +
						    "can currently only handle ContextViews");
	}
	// Make the context visible.
	context.setVisible(false);

	// Remove the display.
	JComponent pane = (JComponent) v.getComponent();
	_viewMap.remove(pane);

	// If there are no views left, then we are done.
	if(viewList().size() == 0) {
	    System.exit(0);
	}
    }	

    /** Get the Document displayed by the given component.
     */
    public View getView (Window w) {
        return (View) _viewMap.get(w);
    }


    // When a view is selected, find the other views for that document and
    // bring their frames to the front.
    private class MultiWindowListener extends WindowAdapter {
        public void windowActivated(WindowEvent e) {
	    Window window = e.getWindow();
            View view = getView(window);
            // FIXME: for some reason, closing
            //        a view also causes that view
            //        to be selected after it is
            //        closed?
            if(viewList().contains(view)) {
                // Prevent recursion
                if (getCurrentView() != view) {
                    setCurrentView(view);
                }
            }
        }

        public void windowClosing(WindowEvent e) {
            Window window = e.getWindow();
            View view = getView(window);
            // FIXME: avoid circular loop with the
            // removeDocument method (if the
            // file is closed from the menu,
            // rather than by clicking the X in
            // the internal pane
            if(viewList().contains(view)) {
                closeView(view);
                //workaround for combobox model bug
                setCurrentView(getCurrentView());
            }
        }
    }
}


