/*
 * $Id: ContextGraphView.java,v 1.4 2001/07/23 04:11:23 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.gui.AbstractView;
import diva.gui.AppContext;
import diva.gui.ApplicationContext;
import diva.gui.ContextView;
import diva.gui.DefaultActions;
import diva.gui.Document;
import diva.gui.GUIUtilities;
import diva.gui.View;
import diva.gui.toolbox.JPalette;
import diva.gui.toolbox.JShadePane;
import diva.resource.RelativeBundle;
import diva.resource.DefaultBundle;

import diva.graph.JGraph;
import diva.graph.GraphPane;
import diva.graph.GraphController;
import diva.graph.toolbox.DeletionListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import diva.gui.toolbox.JStatusBar;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.JMenuItem;

/**
 * Steve, fix me!
 * 
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu
 * @version $Revision: 1.4 $
 */
public class ContextGraphView extends AbstractView implements ContextView {
    private JGraph _jgraph = null;
    private ApplicationContext _context = null;
    private MultiWindowGraphEditor _application;
    /** The toolbar pane
     */
    private transient JPanel _toolBarPane;
    private transient JToolBar _toolBar;

    /** The status bar
     */
    private transient JStatusBar _statusBar;



    public ContextGraphView(MultiWindowGraphEditor g, Document d) {
        super(d);
	_application = g;
    }

    public JComponent getComponent() {
        if(_jgraph == null) {
	    _updateView();
        }
        return _jgraph;
    }

    public AppContext getContext() {
        if(_jgraph == null) {
	    _updateView();
        }
 	return _context;
    }

    public String getTitle() {
        return "Graph";
    }

    public String getShortTitle() {
        return "Graph";
    }

    private void _updateView() {
	_context = new ApplicationContext();
	_context.setSize(800, 600);
	JMenuBar mb = new JMenuBar();
	_context.setJMenuBar(mb);
	_toolBar = new JToolBar();
	_application.initializeMenuBar(mb);
	_application.initializeToolBar(_toolBar);
        
	JSplitPane splitPane = new JSplitPane();
	_context.getContentPane().add(splitPane);

	// Initialize the palette.
	JShadePane shadePane = new JShadePane();
	splitPane.setLeftComponent(shadePane);
	JPalette p1 = new JPalette();
        JPalette p2 = new JPalette();
        JPalette p3 = new JPalette();
	shadePane.add(p1);
	shadePane.add(p2);
	shadePane.add(p3);
	
	GraphController controller =
	    new SchematicGraphController(getDocument().getApplication());
	_jgraph = new JGraph(new GraphPane(controller,
	    ((GraphDocument)getDocument()).getGraphModel()));
	new GraphDropTarget(_jgraph);
	
	ActionListener deletionListener = new DeletionListener();
	_jgraph.registerKeyboardAction(deletionListener, "Delete",
	    KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
	    JComponent.WHEN_IN_FOCUSED_WINDOW);
	_jgraph.setRequestFocusEnabled(true);

	splitPane.setRightComponent(_jgraph);
    }
}


