/*
 * $Id: GraphEditor.java,v 1.5 2001/07/22 22:01:25 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.graph.*;
import diva.graph.basic.*;
import diva.graph.toolbox.*;

//import diva.graph.layout.GlobalLayout;
//import diva.graph.layout.LevelLayout;
//import diva.graph.layout.RandomLayout;
//import diva.graph.layout.LayoutTarget;

import diva.gui.AbstractApplication;
import diva.gui.MDIContext;
import diva.gui.View;
import diva.gui.AppContext;
import diva.gui.ApplicationContext;
import diva.gui.DefaultStoragePolicy;
import diva.gui.DesktopContext;
import diva.gui.DefaultActions;
import diva.gui.Document;
import diva.gui.DocumentFactory;
import diva.gui.GUIUtilities;
import diva.gui.MDIApplication;
import diva.gui.StoragePolicy;
import diva.gui.ViewAdapter;
import diva.gui.ViewEvent;
import diva.gui.toolbox.JShadePane;
import diva.gui.toolbox.JPalette;

import diva.resource.DefaultBundle;
import diva.resource.RelativeBundle;

import java.awt.Image;
import java.awt.FlowLayout;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.DataOutputStream;

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import javax.swing.filechooser.FileFilter;

/**
 * A graph editor for non-hierarchical graphs. This is a complete
 * graph-editing application that includes automatic layout, load from
 * and save to XML, editable properties, and so on. It is intended as
 * an example application that you can use to understand the
 * functionality of the diva.gui application framework, as well as a
 * basis for building a customized graph editor for your own
 * application.
 *
 * <P> This class subclasses diva.gui.MDIApplication. As such, it
 * implements a straight-forward one-to-one mapping from documents
 * to views. (If you need more complex document-view mappings,
 * you will need to subclass AbstractApplication and implement the mappings
 * yourself.) Subclasses of MDIApplication need to call setAppContext()
 * with an instance of MDIFrame (in this case, we use DesktopContext). They
 * also need to implement at least these methods:
 *
 * <ul>
 * <li> public JComponent createView(Document d)
 * <p> Given an implementation of diva.gui.Document, create a suitable
 * widget and return it. The MDIApplication superclass will take care
 * adding it to the MDIFrame.
 *
 * <P><li> public String getTitle ()
 * <P> Return the title of the application -- this is shown in the
 * title bar of the AppContext and other places.
 *
 * <P><li> public void redisplay (Document d, JComponent c);
 * <p> Redisplay the document after it is mapped to the screen.
 * This only needs to be implemented by applications that need to
 * know the size of the component to draw properly (for example,
 * graph layout).
 * 
 * </ul>
 *
 * <p> In addition to implementing diva.gui.Application, the only other
 * interface that must be implemented to produce a complete
 * function application is diva.gui.Document. See the CanvasDocument
 * class.
 *
 * <p> 
 * Please also read the method documentation for this class, as well
 * as the source code. Apart from providing implementation of the 
 * methods described above, it also initializes menubars, toolbars,
 * and creates the Action objects that are executed in response to
 * toolbar and menu commands. 
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class GraphEditor extends MDIApplication {

    /** The layout selection combobox
     */
    private JComboBox _layoutComboBox;

    /** The layout engine
     */
    //    private GlobalLayout _globalLayout;

    /** Construct a new graph editing application. The application
     * will not have any open graph documents, until they are opened
     * by getting the "Open" action an invoking its actionPerformed()
     * method.
     */
    public GraphEditor (DesktopContext context) {
        super(context);

        // Create and initialize the storage policy
	try {
	    DefaultStoragePolicy storage = new DefaultStoragePolicy();
	    setStoragePolicy(storage);
	    FileFilter ff = new FileFilter() {
		public boolean accept (File file) {
		    return GUIUtilities.getFileExtension(file).
		    toLowerCase().equals("xml");
		}
		public String getDescription () {
		    return "XML files";
		}
	    };
	    JFileChooser fc;      
	    fc = storage.getOpenFileChooser();
	    fc.addChoosableFileFilter(ff);
	    fc.setFileFilter(ff);
	    
	    fc = storage.getSaveFileChooser();
	    fc.addChoosableFileFilter(ff);
	    fc.setFileFilter(ff);
	} catch (SecurityException ex) {
	    // FIXME: create a new "NoStoragePolicy"
	}
        setDocumentFactory(new GraphDocument.Factory());

        // Initialize the menubar, toolbar, and palettes
        initializeMenuBar(context.getJMenuBar());
	JPanel toolBarPane = context.getToolBarPane();
	toolBarPane.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
        initializeToolBar(context.getJToolBar());
        initializePalette();

        Icon icon = getResources().getImageIcon("GraphIconImage");
	Image iconImage = getResources().getImage("GraphIconImage");
        context.setFrameIcon(icon);
        context.setIconImage(iconImage);

	context.setVisible(true);
        // Experimental -- doesn't work... open a file
        // getAction("open").actionPerformed(null);
    }

    /** Create a view on the given document.
     */
    public View createView (Document d) {
        return new GraphView(d);
    }

    /** Get the title of this application
     */
    public String getTitle() {
        return "Diva schematic editor";
    }

    /** Initialize the palette in the.
     */
    public void initializePalette () {
        JShadePane s = (JShadePane) ((DesktopContext) getAppContext()).getPalettePane();
        
        RelativeBundle resources = getResources();
        Icon newIcon = resources.getImageIcon("NewImage");
        Icon openIcon = resources.getImageIcon("OpenImage");
        Icon saveIcon = resources.getImageIcon("SaveImage");
        
        JPalette p1 = new JPalette();
        p1.removeAll();
        p1.addIcon(newIcon, "foo");
        p1.addIcon(openIcon, "bar");
        p1.addIcon(saveIcon, "baz");
        s.addShade("Test1", newIcon, p1, "new group -- cool icons!");
        
        JPalette p2 = new JPalette();
        JPalette p3 = new JPalette();
        s.addShade("Test2", openIcon,new JPalette(),"open group -- disabled!");

        s.addShade("Test3", saveIcon,new JPalette(),"save group -- boring...");
        s.setEnabledAt(1, false);
    }
    
    /** Initialize the given menubar. Currently, all strings are
     * hard-wired, but maybe we should be getting them out of the
     * ApplicationResources.
     */
    public void initializeMenuBar (JMenuBar mb) {
        Action action;
        JMenuItem item;

        // Create the File menu
        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic('F');
        mb.add(menuFile);

        action = DefaultActions.newAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'N', 
				 "Create a new graph document");

        action = DefaultActions.openAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'O', 
				 "Open a graph document");

        action = DefaultActions.closeAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'C', 
				 "Close the current graph document");

        menuFile.addSeparator();

        action = DefaultActions.saveAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'S', 
				 "Save the current graph document");

        action = DefaultActions.saveAsAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'A', 
                "Save the current graph document to a different file");

        menuFile.addSeparator();

        action = DefaultActions.exitAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'X', 
				 "Exit from the graph editor");

        // Create the Devel menu
        JMenu menuDevel = new JMenu("Devel");
        menuDevel.setMnemonic('D');
        mb.add(menuDevel);

        action = new AbstractAction ("Print document info") {
            public void actionPerformed(ActionEvent e) {
                View v = getCurrentView();
                if (v == null) {
                    System.out.println("Graph document is null");
                } else {
                    System.out.println(v.toString());
                }
            }
        };
        addAction(action);
        GUIUtilities.addMenuItem(menuDevel, action, 'P', 
				 "Print current document info");
    }

    /** Initialize the given toolbar. Image icons will be obtained
     * from the ApplicationResources object and added to the
     * actions. Note that the image icons are not added to the actions
     * -- if we did that, the icons would appear in the menus, which I
     * suppose is a neat trick but completely useless.
     */
    public void initializeToolBar (JToolBar tb) {
        Action action;
        RelativeBundle resources = getResources();

        // Conventional new/open/save buttons
        action = getAction("New");
        GUIUtilities.addToolBarButton(tb, action, null,
				      resources.getImageIcon("NewImage"));

        action = getAction("Open");
        GUIUtilities.addToolBarButton(tb, action, null, 
				      resources.getImageIcon("OpenImage"));

        action = getAction("Save");
        GUIUtilities.addToolBarButton(tb, action, null, 
				      resources.getImageIcon("SaveImage"));

        //tb.addSeparator();

        // Layout combobox
	/*        _layoutComboBox = new JComboBox();
        String dflt = "Random layout";
        _layoutComboBox.addItem(dflt);
        _layoutComboBox.addItem("Levelized layout");
        _layoutComboBox.setSelectedItem(dflt);
        _layoutComboBox.setMaximumSize(_layoutComboBox.getMinimumSize());
        _layoutComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged (ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    GraphDocument d = (GraphDocument) getCurrentDocument();
                    JGraph jg = (JGraph) getView(d);
                    redoLayout(jg, (String) e.getItem());
                }
            }
        });
        tb.add(_layoutComboBox);
	*/
        //tb.addSeparator();
    }

    /** Create and run a new graph application
     */
    public static void main (String argv[]) {
	new GraphEditor(new DesktopContext(new ApplicationContext()));
    }

    /** Redo the layout of the given JGraph.
     
    public void redoLayout (JGraph jgraph, String type) {
        GraphController controller = jgraph.getGraphPane().getGraphController();
        LayoutTarget target = new BasicLayoutTarget(controller);
        Graph graph = controller.getGraph();
        GlobalLayout layout;

        if (type.equals("Random layout")) {
            layout = new RandomLayout();
        } else {
            layout = new LevelLayout(); 
        }
        // Perform the layout and repaint
        try {
            layout.layout(target, graph);
        } catch (Exception e) {
            showError("layout", e);
        }
        jgraph.repaint();
    }
    */
}



