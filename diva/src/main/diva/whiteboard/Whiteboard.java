/*
 * $Id: Whiteboard.java,v 1.60 2001/08/27 22:29:37 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;

import diva.sketch.JSketch;
import diva.sketch.SketchEvent;
import diva.sketch.SketchListener;
import diva.sketch.SketchModel;
import diva.sketch.SketchPane;
import diva.sketch.SketchWriter;
import diva.sketch.SketchParser;

import diva.gui.MDIApplication;
import diva.gui.MDIContext;
import diva.gui.Application;
import diva.gui.AppContext;
import diva.gui.ApplicationContext;
import diva.gui.BasicPage;
import diva.gui.DesktopContext;
import diva.gui.DefaultActions;
import diva.gui.Document;
import diva.gui.toolbox.ListDataModel;
import diva.gui.MultipageDocument;
import diva.gui.MultipageModel;
import diva.gui.DocumentFactory;
import diva.gui.ExtensionFileFilter;
import diva.gui.GUIUtilities;
import diva.gui.Page;
import diva.gui.StoragePolicy;
import diva.gui.ViewAdapter;
import diva.gui.ViewEvent;
import diva.gui.View;
import diva.gui.toolbox.JShadePane;
import diva.gui.toolbox.JPalette;
import diva.gui.toolbox.ListDataModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoManager;

//import com.sun.image.codec.jpeg.*;

/**
 * A digital whiteboard application that supports freeform sketching
 * and gestural command editing.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.60 $
 */
public class Whiteboard extends MDIApplication {
    /**
     * The width of the image on a button in the toolbar.
     */
    public final static int BUTTON_WIDTH = 25;
    /**
     * The height of the image on a button in the toolbar.
     */
    public final static int BUTTON_HEIGHT = 25;
    
    /** A copy of the storage policy reference
     */
    protected SketchStoragePolicy _storagePolicy;    

    /**
     * The following are strings that are used to name menu items, set
     * and retrieve the corresponding actions, set image icons.
     */
    private final static String NEW_PAGE = "New Page";
    private final static String SAVE_AS_JPEG = "Save as jpeg";    
    private final static String NEXT = "Next";
    private final static String PREVIOUS = "Previous";
    private final static String MENU_PEN_COLOR = "Pen Color";
    private final static String MENU_PEN_WIDTH = "Pen Width";
    private final static String SAVE_GESTURES = "Save Gestures";
    private final static String LOAD_GESTURES = "Load Gestures";
    private final static String GROUP = "Group Symbols";
    private final static String UNGROUP = "Ungroup Symbols";

    /*********************** Menu items *************************/
    protected JMenuItem _newPageMenuItem;
    protected JMenuItem _closeMenuItem;
    protected JMenuItem _saveMenuItem;
    protected JMenuItem _printMenuItem;
    protected JMenuItem _saveAsMenuItem;
    protected JMenuItem _saveAsGIFMenuItem;

    protected JMenuItem _undoMenuItem;
    protected JMenuItem _redoMenuItem;
    protected JMenuItem _nextMenuItem;
    protected JMenuItem _previousMenuItem;
    
    /*********************** Menu items *************************/

    /*********************** Tool Bar Buttons *************************/
    protected JButton _newPageButton;
    protected JButton _openButton;        
    protected JButton _saveButton;
    //    protected JButton _undoButton;
    //    protected JButton _redoButton;
    protected JButton _nextButton;
    protected JButton _previousButton;
    protected JPopupMenu _outlinePopupMenu = new JPopupMenu();
    protected JPopupMenu _fillPopupMenu = new JPopupMenu();
    protected JButton _outlineColorButton;
    protected JButton _fillColorButton;
    protected JPopupMenu _widthPopupMenu = new JPopupMenu();    
    protected JButton _widthButton;
    protected JButton _sketchModeButton;
    protected JButton _highlightModeButton;
    protected JButton _commandModeButton;    
    /*********************** Tool Bar Buttons *************************/

    protected WhiteboardState _whiteboardState = new WhiteboardState();
    protected UndoAdaptor _undoAdaptor = new UndoAdaptor();
    
    /**
     * Create and run a new whiteboard application.
     */
    public static void main (String argv[]) {
        DesktopContext dc = new DesktopContext(new ApplicationContext());
        Whiteboard wb = new Whiteboard(dc);
        wb.setVisible(true);
        wb.getDesktopContext().setMaximizeMode(true);
        System.out.println("DONE WITH MAIN");
    }
 
    /**
     * Construct a new whiteboard application. The application
     * will not have any open sketch documents, until they are opened
     * by getting the "Open" action and invoking its actionPerformed()
     * method.
     */
    public Whiteboard(DesktopContext context) {
        super(context);
        
        addViewListener(new WBAppViewListener());
        
        _whiteboardState.addStateListener(new WBStateListener());
            
        setDocumentFactory(new SketchDocument.Factory());

        // Initialize the menubar, toolbar, and palettes
        initializeMenuBar(context.getJMenuBar());
        initializeToolBar(context.getJToolBar());

        JSplitPane splitPane = context.getSplitPane();
        splitPane.remove(splitPane.getLeftComponent());
        //initializePalette();

        Icon icon = getResources().getImageIcon("GraphIconImage");
        Image iconImage = getResources().getImage("GraphIconImage");

        context.setFrameIcon(icon);
	//        getDesktopContext().setIconImage(iconImage);

        // Create and initialize the storage policy
        _storagePolicy = new SketchStoragePolicy();
        setStoragePolicy(_storagePolicy);

        JFileChooser fc;
        fc = _storagePolicy.getOpenFileChooser();
        FileFilter ff =
            new ExtensionFileFilter(SketchStoragePolicy.SML,
                    "Sketch Markup Language");
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);

        /*
        fc = _storagePolicy.getSaveFileChooser();
        FileFilter ff2 =
            new ExtensionFileFilter(SketchStoragePolicy.JPEG,
                    "JPEG Format");        
        fc.addChoosableFileFilter(ff2);
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);
        */
        
        // Set the exit action for the X in the right upper corner
        // of the frame.
        context.setExitAction(getAction(DefaultActions.EXIT));
        context.setTitle("Diva Whiteboard");
        getAction(DefaultActions.NEW).actionPerformed(null);
        context.setSize(540, 720);

        setClipboard(Toolkit.getDefaultToolkit().getSystemClipboard());

        ThumbnailView tv = new ThumbnailView(this);
        context.setPalettePane(tv.getComponent());
    }

    public void addDocument(Document d) {
        super.addDocument(d);
        // add UndoAdaptor to the document
        ((MultipageDocument)d).getEditSupport().addUndoableEditListener(_undoAdaptor);
    }

    public boolean closeDocument(Document d){
        boolean val = super.closeDocument(d);
        // remove UndoAdaptor from the document
        ((MultipageDocument)d).getEditSupport().removeUndoableEditListener(_undoAdaptor);
        return val;
    }

    /**
     * Return a view for the given document.
     * Invoked by DefaultActions newAction and openAction.
     * Add a page listener to the multipage document and then
     * instantiate a whiteboard view object.
     */
    public View createView(Document d) {
        MultipageDocument doc = (MultipageDocument)d;
        doc.getMultipageModel().addPageListener(new WBPageListener());
        return new WhiteboardView(doc, _whiteboardState);
    }

    /**
     * Display the given document. The document should already be
     * added to the application. After calling this method, most
     * callers should set this document to be the current document.
     */
    public void displayDocument (Document d) {}

    protected DesktopContext getDesktopContext() {
        return (DesktopContext)getAppContext();
    }

    /**
     * Get the title of this application
     */
    public String getTitle() {
        return "Diva whiteboard";
    }

    public WhiteboardState getWhiteboardState (){
        return _whiteboardState;
    }
    
    /**
     * Initialize the given menubar. Currently, all strings are
     * hard-wired, but maybe we should be getting them out of the
     * ApplicationResources.
     */
    public void initializeMenuBar (JMenuBar mb) {
        initializeFileMenu(mb);
        initializeEditMenu(mb);
        initializeViewMenu(mb);
        initializeDebugMenu(mb);
        //initializeToolMenu(mb);        
    }

    /**
     * Create the Debug menu with the following items:
     * <ol>
     * <li> Save Gestures </li>
     * <li> Load Gestures </li>
     * </ol>
     */
    protected void initializeDebugMenu(JMenuBar mb){
        Action action;
        JMenuItem item;
        
        JMenu menuDebug = new JMenu("Debug");
        menuDebug.setMnemonic('D');
        mb.add(menuDebug);

        action = new AbstractAction(SAVE_GESTURES){
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
                int result = fc.showSaveDialog(getAppContext().makeComponent());
                if(result == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fc.getSelectedFile();
                    WhiteboardView wbv = (WhiteboardView)getCurrentView();
                    //assume wbv != null, otherwise this shouldn't have been active
                    SketchModel gestures = wbv.getGestureModel();
                    SketchWriter sw = new SketchWriter();
                    try {
                        sw.writeModel(gestures, new FileWriter(chosenFile));
                    }
                    catch(IOException ex) {
                        System.out.println(ex.toString());
                    }
                }
            }
        };
        addAction(action);
        GUIUtilities.addMenuItem(menuDebug, action, 's', "Save gestures to a file for debugging");
        
        action = new AbstractAction(LOAD_GESTURES){
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.dir")));                
                int result = fc.showOpenDialog(getAppContext().makeComponent());
                if(result == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fc.getSelectedFile();
                    WhiteboardView wbv = (WhiteboardView)getCurrentView();
                    //assume wbv != null, otherwise this shouldn't have been active
                    SketchModel gestures = wbv.getGestureModel();
                    SketchParser sr = new SketchParser();
                    try {
                        SketchModel model = (SketchModel)sr.parse(new FileReader(chosenFile));
                        wbv.playGestureModel(model);
                    }
                    catch(Exception ex) {
                        System.out.println(ex.toString());
                    }
                }
            }
        };
        addAction(action);
        GUIUtilities.addMenuItem(menuDebug, action, 's', "Save gestures to a file for debugging");
        
    }

    /**
     * Create the Edit menu with the following items:
     * <ol>
     * <li> Cut </li>
     * <li> Copy </li>
     * <li> Paste </li>
     * </ol>
     */
    protected void initializeEditMenu(JMenuBar mb){
        Action action;
        JMenuItem item;
        
        JMenu menuEdit = new JMenu("Edit");
        menuEdit.setMnemonic('E');
        mb.add(menuEdit);

        action = new UndoAction();
        addAction(action);
        _undoMenuItem =
            GUIUtilities.addMenuItem(menuEdit, action, 'z', "Undo");

        action = new RedoAction();
        addAction(action);
        _redoMenuItem =
            GUIUtilities.addMenuItem(menuEdit, action, 'r', "Redo");

        menuEdit.addSeparator();
                    
        action = DefaultActions.cutAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuEdit, action, 't', "Cut the selected strokes");

        action = DefaultActions.copyAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuEdit, action, 'c', "Copy the selected strokes");

        action = DefaultActions.pasteAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuEdit, action, 'p', "Paste strokes from the clipboard");

        menuEdit.addSeparator();

        action = new AbstractAction(GROUP){
            public void actionPerformed(ActionEvent e) {
                WhiteboardView wbv = (WhiteboardView)getCurrentView();
                wbv.groupSelected();
            }
        };
        addAction(action);
        GUIUtilities.addMenuItem(menuEdit, action, 'g', "Group a set of symbols");

        action = new AbstractAction(UNGROUP){
            public void actionPerformed(ActionEvent e) {
                WhiteboardView wbv = (WhiteboardView)getCurrentView();
                wbv.ungroupSelected();
            }
        };
        addAction(action);
        GUIUtilities.addMenuItem(menuEdit, action, 'g', "Ungroup a set of symbols");
    }
    
    /**
     * Create the File menu with the following items:
     * <ol>
     * <li> New </li>
     * <li> Open </li>
     * <li> Close </li>
     * <li> Save </li>
     * <li> Save As </li>
     * <li> Print </li>
     * <li> Exit </li>
     * </ol>
     */
    protected void initializeFileMenu(JMenuBar mb){
        Action action;
        JMenuItem item;
        
        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic('F');
        mb.add(menuFile);

        action = DefaultActions.newAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'N', "Create a new sketch document");

        action = new AbstractAction(NEW_PAGE){
            public void actionPerformed(ActionEvent e) {
                MultipageDocument d = (MultipageDocument) getCurrentView().getDocument();
                MultipageModel mpm = d.getMultipageModel();
                int num = mpm.getPageCount();
                Page curPage = mpm.getCurrentPage();
                String pageName = "Page " + (num+1);
                Page page = new BasicPage(mpm, pageName, new SketchModel());
                mpm.addPage(page);
                mpm.setCurrentPage(page);

                //create an edit
                WhiteboardEdits.NewPageEdit edit =
                new WhiteboardEdits.NewPageEdit(mpm, page, curPage);
                d.getEditSupport().postEdit(edit);
            }
        };
        addAction(action);
        _newPageMenuItem = GUIUtilities.addMenuItem(menuFile, action, 'P',
                "Create a new page in the current document");

        action = DefaultActions.openAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, "Open...", action, 'O',
                "Open a sketch document", true);

        action = DefaultActions.closeAction(this);
        addAction(action);
        _closeMenuItem = GUIUtilities.addMenuItem(menuFile, action, 'C',
                "Close the current sketch document");

        menuFile.addSeparator();

        action = DefaultActions.saveAction(this);
        addAction(action);
        _saveMenuItem = GUIUtilities.addMenuItem(menuFile, action, 'S',
                "Save the current sketch document");


        action = DefaultActions.saveAsAction(this);
        addAction(action);
        _saveAsMenuItem = GUIUtilities.addMenuItem(menuFile, "Save As...",
                action, 'A', 
                "Save the current sketch document to a different file", false);

        action = new SaveAsGIFAction();
        addAction(action);
        _saveAsGIFMenuItem = GUIUtilities.addMenuItem(menuFile, action, 'G', 
                "Save the current sketch document in GIF format");
        
        menuFile.addSeparator();

        action = DefaultActions.printAction(this);
        addAction(action);
        _printMenuItem = GUIUtilities.addMenuItem(menuFile, "Print...",
                action, 'P',
                "Print the current sketch document", false);
        menuFile.addSeparator();
        
        action = DefaultActions.exitAction(this);
        addAction(action);
        GUIUtilities.addMenuItem(menuFile, action, 'X', "Exit from whiteboard");
    }

    /**
     * Create the View menu with the following items:
     * <ol>
     * <li> Next Page </li>
     * <li> Previous Page </li>
     * </ol>
     */
    protected void initializeViewMenu(JMenuBar mb){
        Action action;
        JMenu menuView = new JMenu("View");
        menuView.setMnemonic('V');
        mb.add(menuView);

        action = new AbstractAction(PREVIOUS) {
            public void actionPerformed(ActionEvent e) {
                WhiteboardView wbv = (WhiteboardView)getCurrentView();
                wbv.previousPage();
            }
        };
        addAction(action);
        _previousMenuItem = GUIUtilities.addMenuItem(menuView, "Previous Page", action,
                KeyEvent.VK_PAGE_DOWN, "Previous Page", false);

        action = new AbstractAction(NEXT) {
            public void actionPerformed(ActionEvent e) {
                WhiteboardView wbv = (WhiteboardView)getCurrentView();
                wbv.nextPage();
            }
        };
        addAction(action);
        _nextMenuItem = GUIUtilities.addMenuItem(menuView, "Next Page", action,
                    KeyEvent.VK_PAGE_UP, "Next Page", false);
    }
    
    /**
     * Initialize the given toolbar. Image icons will be obtained
     * from the ApplicationResources object and added to the
     * actions. Note that the image icons are not added to the actions
     * -- if we did that, the icons would appear in the menus, which I
     * suppose is a neat trick but completely useless.
     */
    protected void initializeToolBar (JToolBar tb) {
        tb.setName("Toolbar");
        Action action;
        int offset = 3; // offset from the border of a button
        
        // Conventional new/open/save buttons
        action = getAction(DefaultActions.NEW);
        GUIUtilities.addToolBarButton(tb, action, null,
                getResources().getImageIcon("NewImage"));
                
        action = getAction(NEW_PAGE);
        _newPageButton = GUIUtilities.addToolBarButton(tb, action, null,
                getResources().getImageIcon("NewPageImage"));

        action = getAction(DefaultActions.OPEN);
        _openButton = GUIUtilities.addToolBarButton(tb, action, null,
                getResources().getImageIcon("OpenImage"));

        action = getAction(DefaultActions.SAVE);
        _saveButton = GUIUtilities.addToolBarButton(tb, action, null,
                getResources().getImageIcon("SaveImage"));

        action = getAction(PREVIOUS);
        _previousButton = GUIUtilities.addToolBarButton(tb, action, null,
                getResources().getImageIcon("PreviousImage"), false);
        _previousButton.setMargin(new Insets(offset, 0, offset, 0));

        action = getAction(NEXT);
        _nextButton = GUIUtilities.addToolBarButton(tb, action, null,
                getResources().getImageIcon("NextImage"), false);
        _nextButton.setMargin(new Insets(offset, 0, offset, 0));        

        initializeToolbarPenColorMenu(tb);
        initializeToolbarPenWidthMenu(tb);

        action = new AbstractAction(WhiteboardState.SKETCH_MODE){
            public void actionPerformed(ActionEvent e) {
                _whiteboardState.setMode(WhiteboardState.SKETCH_MODE);
            }
        };
        addAction(action);
        _sketchModeButton =
            GUIUtilities.addToolBarButton(tb, action, null,
                    getResources().getImageIcon("SketchModeImage"), false);
        _sketchModeButton.setMargin(new Insets(offset, 0, offset, 0));

        action = new AbstractAction(WhiteboardState.HIGHLIGHT_MODE){
            public void actionPerformed(ActionEvent e) {
                _whiteboardState.setMode(WhiteboardState.HIGHLIGHT_MODE);
            }
        };
        addAction(action);
        _highlightModeButton =
            GUIUtilities.addToolBarButton(tb, action, null,
                    getResources().getImageIcon("HighlightModeImage"), false);
        _highlightModeButton.setMargin(new Insets(offset, 0, offset, 0));
        
        action = new AbstractAction(WhiteboardState.COMMAND_MODE){
            public void actionPerformed(ActionEvent e) {
                _whiteboardState.setMode(WhiteboardState.COMMAND_MODE);
            }
        };
        _commandModeButton =
            GUIUtilities.addToolBarButton(tb, action, null,
                    getResources().getImageIcon("CommandModeImage"), true);
        _commandModeButton.setMargin(new Insets(offset, 0, offset, 0));

    }

    /**
     * Adds a button to the toolbar which allows the user to change
     * the color of the pen ink.  When the button is pressed, a menu
     * will show up, and the menu provides 4 basic colors for
     * convenience.  The last item in the menu is "custom...".
     * This will bring up a color palette and give the user a lot more
     * color choices.
     */
    protected void initializeToolbarPenColorMenu(JToolBar tb){
        Action action;
        
        action = new AbstractAction (WhiteboardState.PEN_COLOR){
            public void actionPerformed(ActionEvent e) {
                _outlinePopupMenu.show(_outlineColorButton,
                        0, _outlineColorButton.getHeight());
            }
        };
        addAction(action);        
        _outlineColorButton = GUIUtilities.addToolBarButton(tb, action, "Pen Color",
                new ColorIcon(Color.black));
        _outlineColorButton.setDisabledIcon(new ColorIcon(new Color(0.3f,0.3f,0.3f,0.5f)));
        
        JMenuItem item;
        item = _outlinePopupMenu.add(new SetOutlineAction(Color.red));
        item.setIcon(new ColorIcon(Color.red));
        item = _outlinePopupMenu.add(new SetOutlineAction(Color.orange));
        item.setIcon(new ColorIcon(Color.orange));
        item = _outlinePopupMenu.add(new SetOutlineAction(Color.yellow));
        item.setIcon(new ColorIcon(Color.yellow));
        item = _outlinePopupMenu.add(new SetOutlineAction(Color.green));
        item.setIcon(new ColorIcon(Color.green));
        item = _outlinePopupMenu.add(new SetOutlineAction(Color.blue));
        item.setIcon(new ColorIcon(Color.blue));
        item = _outlinePopupMenu.add(new SetOutlineAction(Color.black));
        item.setIcon(new ColorIcon(Color.black));                
        
        item = _outlinePopupMenu.add(new AbstractAction() {//custom option
            public void actionPerformed(ActionEvent e){
                //                _colorPopupMenu.setVisible(false);
                Color oldColor = _whiteboardState.getPenColor();
                _outlinePopupMenu.setVisible(false);
                Color newColor =
                JColorChooser.showDialog(getDesktopContext().makeComponent(),
                        "Pen Color", Color.white);
                _whiteboardState.setPenColor(newColor);
            }
        });
        item.setText("Custom...");


        action = new AbstractAction (WhiteboardState.FILL_COLOR){
            public void actionPerformed(ActionEvent e) {
                _fillPopupMenu.show(_fillColorButton,
                        0, _fillColorButton.getHeight());
            }
        };
        addAction(action);        
        _fillColorButton = GUIUtilities.addToolBarButton(tb, action, "Fill Color",
                new ColorIcon(null));
        _fillColorButton.setDisabledIcon(new ColorIcon(new Color(0.3f,0.3f,0.3f,0.5f)));
        
        item = _fillPopupMenu.add(new SetFillAction(Color.red));
        item.setIcon(new ColorIcon(Color.red));
        item = _fillPopupMenu.add(new SetFillAction(Color.orange));
        item.setIcon(new ColorIcon(Color.orange));
        item = _fillPopupMenu.add(new SetFillAction(Color.yellow));
        item.setIcon(new ColorIcon(Color.yellow));
        item = _fillPopupMenu.add(new SetFillAction(Color.green));
        item.setIcon(new ColorIcon(Color.green));
        item = _fillPopupMenu.add(new SetFillAction(Color.blue));
        item.setIcon(new ColorIcon(Color.blue));
        item = _fillPopupMenu.add(new SetFillAction(Color.black));
        item.setIcon(new ColorIcon(Color.black));                
        item = _fillPopupMenu.add(new SetFillAction(Color.white));
        item.setIcon(new ColorIcon(Color.white));                
        item = _fillPopupMenu.add(new SetFillAction(null));
        item.setIcon(new ColorIcon(null));                
        
        item = _fillPopupMenu.add(new AbstractAction() {//custom option
            public void actionPerformed(ActionEvent e){
                _fillPopupMenu.setVisible(false);
                Color newColor =
                JColorChooser.showDialog(getDesktopContext().makeComponent(),
                        "Pen Color", Color.white);
                _whiteboardState.setFillColor(newColor);
            }
        });
        item.setText("Custom...");
    }

    /**
     * Adds a button to the toolbar which allows the user to change
     * the thickness of the pen.  When the button is pressed, a menu
     * will show up, and the menu provides 5 choices of line widths
     * for convenience.  User can also perform this task through the
     * menu.
     */
    protected void initializeToolbarPenWidthMenu(JToolBar tb){
        Action action;
        
        action = new AbstractAction (WhiteboardState.PEN_WIDTH){
            public void actionPerformed(ActionEvent e) {
                _widthPopupMenu.show(_widthButton,
                        0, _widthButton.getHeight());
            }
        };
        
        addAction(action);        
        _widthButton =
            GUIUtilities.addToolBarButton(tb, action, "Pen Width", new WidthIcon(2));
        _widthButton.setDisabledIcon(new GrayedOutWidthIcon());
        JMenuItem item;
        item = _widthPopupMenu.add(new SetWidthAction(1f));
        item.setIcon(new WidthIcon(1f));
        item = _widthPopupMenu.add(new SetWidthAction(2f));
        item.setIcon(new WidthIcon(2f));
        item = _widthPopupMenu.add(new SetWidthAction(3f));
        item.setIcon(new WidthIcon(3f));
        item = _widthPopupMenu.add(new SetWidthAction(4f));
        item.setIcon(new WidthIcon(4f));
        item = _widthPopupMenu.add(new SetWidthAction(8f));
        item.setIcon(new WidthIcon(8f));
        item = _widthPopupMenu.add(new SetWidthAction(12f));
        item.setIcon(new WidthIcon(12f));        
        
        item = _widthPopupMenu.add(new AbstractAction() {
            public void actionPerformed(ActionEvent e){
                _widthPopupMenu.setVisible(false);
            }
        });
    }
    
    /**
     * Set the given view to be the current view, and
     * update the toolbar widgets.
     *
    public void setCurrentView (View v) {
        super.setCurrentView(v);
    }
    */

    /**
     * A color icon paints a rectangle using the color set in the
     * constructor.
     */
    private class ColorIcon implements Icon {
        /**
         * Color used to paint on the icon.
         */
        Color _color;
        /**
         * Border offset.
         */
        int _offset = 2;
        
        public ColorIcon(Color c){
            _color = c;
        }

        public void paintIcon (Component c, Graphics g, int x, int y) {
            if(_color == null) {
                g.setColor(Color.black);
                g.drawRect(x+_offset, y+_offset,
                        getIconWidth()-_offset, getIconHeight()-_offset);
            }
            else {
                g.setColor(_color);
                g.fillRect (x+_offset, y+_offset,
                        getIconWidth()-_offset, getIconHeight()-_offset);
            }
        }
        public int getIconWidth() {
            return BUTTON_WIDTH;
        }

        public int getIconHeight() { 
            return BUTTON_HEIGHT;
        }

        public Color getColor(){
            return _color;
        }
    }

    /**
     * When this action is called, it sets the pen color and close the
     * popup menu for choosing colors.
     */
    private class SetOutlineAction extends AbstractAction {
        Color _color;
        
        public SetOutlineAction(Color c) {
            _color = c;
        }

        public void actionPerformed(ActionEvent e){
            _whiteboardState.setPenColor(_color);
            _outlinePopupMenu.setVisible(false);
        }
    }

    /**
     * When this action is called, it sets the pen color and close the
     * popup menu for choosing colors.
     */
    private class SetFillAction extends AbstractAction {
        Color _color;
        
        public SetFillAction(Color c) {
            _color = c;
        }

        public void actionPerformed(ActionEvent e){
            _whiteboardState.setFillColor(_color);
            _fillPopupMenu.setVisible(false);
        }
    }
    
    /**
     * When this action is called, it sets the pen width and close the
     * popup menu for selecting pen widths.
     */
    private class SetWidthAction extends AbstractAction {
        /**
         * The pen width.
         */
        float _width;
        
        public SetWidthAction(float w) {
            _width = w;
        }

        public void actionPerformed(ActionEvent e){
            _whiteboardState.setPenWidth(_width);
            _widthPopupMenu.setVisible(false);
        }
    }
    
    /**
     * A line width icon paints a line using the thickness set in the
     * constructor.
     */
    private class WidthIcon implements Icon {
        /**
         * The pen width.
         */
        float _width;
        /**
         * Paint lines with this color.
         */
        Color _color = Color.black;
        /**
         * Border offset.
         */
        int _offset = 2;
        /**
         * Y offset for painting the string of line width.
         */
        int _yoffset;

        /**
         * The font used for labeling the point size.
         */
        Font _font = new Font("Serif", Font.PLAIN, 10);

        public WidthIcon(float w){
            _width = w;
            _yoffset = getIconHeight()/2;
        }

        public void paintIcon (Component c, Graphics g, int x, int y) {
            g.setColor(_color);
            g.setFont(_font);
            g.drawString(String.valueOf(_width), x, y+_yoffset);
            int xstart = x + _offset*5;
            int ystart = _yoffset - (int)_width/2;
            g.fillRect(xstart, ystart, getIconWidth()-_offset, (int)_width);
        }

        public int getIconWidth() {
            return BUTTON_WIDTH;
        }

        public int getIconHeight() { 
            return BUTTON_HEIGHT;
        }
    }    

    /**
     * Listens to the whiteboard state and updates the widgets
     * accordingly.
     */
    protected class WBStateListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e){
            String propertyName = e.getPropertyName();
            if(propertyName.equals(WhiteboardState.PEN_COLOR)){
                //button state
                Color c = (Color)e.getNewValue();
                _outlineColorButton.setIcon(new ColorIcon(c));
            }
            else if(propertyName.equals(WhiteboardState.FILL_COLOR)){
                //button state
                Color c = (Color)e.getNewValue();
                _fillColorButton.setIcon(new ColorIcon(c));
            }
            else if(propertyName.equals(WhiteboardState.PEN_WIDTH)){
                //button state
                float w = ((Float)e.getNewValue()).floatValue();
                _widthButton.setIcon(new WidthIcon(w));
            }
            else if(propertyName.equals(WhiteboardState.MODE)){
                String mode = (String)e.getNewValue();
                //button state
                if(mode.equals(WhiteboardState.COMMAND_MODE)){
                    _commandModeButton.setEnabled(false);
                    _highlightModeButton.setEnabled(true);
                    _sketchModeButton.setEnabled(true);
                }
                else if(mode.equals(WhiteboardState.HIGHLIGHT_MODE)){
                    _commandModeButton.setEnabled(true);
                    _highlightModeButton.setEnabled(false);
                    _sketchModeButton.setEnabled(true);
                }
                else { //sketch mode
                    _commandModeButton.setEnabled(true);
                    _highlightModeButton.setEnabled(true);
                    _sketchModeButton.setEnabled(false);
                }
            }
            else {
                System.err.println("Unknown UI property: " + propertyName);
            }
        }
    }

    public class WBAppViewListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e){
            ListDataModel ldm = (ListDataModel)e.getSource();//views
            _newPageMenuItem.setEnabled(true);
            _newPageButton.setEnabled(true);
            
            _closeMenuItem.setEnabled(true);
            _saveMenuItem.setEnabled(true);
            _printMenuItem.setEnabled(true);
            _saveAsMenuItem.setEnabled(true);
            _saveAsGIFMenuItem.setEnabled(true);
            _saveButton.setEnabled(true);

            if(ldm.getSize()==1){//first document opened, send events to WBV
                _whiteboardState.setPenColor(_whiteboardState.getPenColor());
                _whiteboardState.setPenWidth(_whiteboardState.getPenWidth());
                _whiteboardState.setMode(_whiteboardState.getMode());
                _outlineColorButton.setEnabled(true);
                _fillColorButton.setEnabled(true);
                _widthButton.setEnabled(true);
            }
        }

        public void intervalRemoved(ListDataEvent e){
            ListDataModel ldm = (ListDataModel)e.getSource();//views
            if(ldm.getSize()==0){
                disableAll();
            }
        }

        /**
         * Invoked when the current document has been switched.
         * AR: should I check if this document is being viewed
         * currently?         
         */
        public void contentsChanged(ListDataEvent e){
            ListDataModel ldm = (ListDataModel)e.getSource();//views
            View view = (View)ldm.getSelectedItem();
            if(view != null){
                MultipageDocument doc =
                    (MultipageDocument)view.getDocument();
                MultipageModel model = doc.getMultipageModel();
                int curPageIndex = model.indexOf(model.getCurrentPage());
                updatePageWidgets(curPageIndex, model.getPageCount());
                refreshUndoRedo();
            }
        }
        
        private void disableAll(){
            _newPageMenuItem.setEnabled(false);
            _newPageButton.setEnabled(false);
            
            _closeMenuItem.setEnabled(false);
            _saveMenuItem.setEnabled(false);
            _printMenuItem.setEnabled(false);
            _saveAsMenuItem.setEnabled(false);
            _saveAsGIFMenuItem.setEnabled(false);
            _saveButton.setEnabled(false);
            
            _previousButton.setEnabled(false);
            _previousMenuItem.setEnabled(false);
            _nextButton.setEnabled(false);
            _nextMenuItem.setEnabled(false);

            _outlineColorButton.setEnabled(false);
            _fillColorButton.setEnabled(false);
            _widthButton.setEnabled(false);

            _commandModeButton.setEnabled(false);
            _sketchModeButton.setEnabled(false);
            _highlightModeButton.setEnabled(false);
        }
    }


    public class WBPageListener implements ListDataListener {

        public void intervalAdded(ListDataEvent e){}

        public void intervalRemoved(ListDataEvent e){
            ListDataModel ldm = (ListDataModel)e.getSource();
            int curPageIndex = ldm.getIndexOf(ldm.getSelectedItem());
            updatePageWidgets(curPageIndex, ldm.getSize());
        }

        /**
         * Invoked when the current page has been switched
         * AR: should I check if this page is being viewed currently?
         */
        public void contentsChanged(ListDataEvent e){
            ListDataModel ldm = (ListDataModel)e.getSource();
            int curPageIndex = ldm.getIndexOf(ldm.getSelectedItem());
            updatePageWidgets(curPageIndex, ldm.getSize());            
        }
    }

    
    private void updatePageWidgets(int curPageIndex, int numPages){
        if(curPageIndex == numPages-1){//last page
            _nextMenuItem.setEnabled(false);
            _nextButton.setEnabled(false);
        }
        else {
            _nextMenuItem.setEnabled(true);
            _nextButton.setEnabled(true);
        }
        if(curPageIndex == 0){//first page
            _previousMenuItem.setEnabled(false);
            _previousButton.setEnabled(false);
        }
        else {
            _previousMenuItem.setEnabled(true);
            _previousButton.setEnabled(true);
        }
    }

    class GrayedOutWidthIcon implements Icon {
        private Color _color = new Color(0.3f, 0.3f, 0.3f, 0.5f);
        private Font _font = new Font("Serif", Font.PLAIN, 10);
        private int _offset = 2;
        private int _yoffset = BUTTON_HEIGHT/2;
        
        public int getIconHeight(){
            return BUTTON_HEIGHT;
        }
            
        public int getIconWidth(){
            return BUTTON_WIDTH;
        }
        
        public void paintIcon(Component c, Graphics g, int x, int y){
            float width = _whiteboardState.getPenWidth();
            g.setColor(_color);
            g.setFont(_font);
            g.drawString(String.valueOf(width), x, y+_yoffset);
            int xstart = x + _offset*5;
            int ystart = _yoffset - (int)width/2;
            g.fillRect(xstart, ystart, getIconWidth()-_offset, (int)width); 
        }
    }

    private class RedoAction extends AbstractAction {

        public RedoAction(){
            super("Redo");
        }
        
        public void actionPerformed(ActionEvent e){
            getCurrentView().getDocument().getUndoManager().redo();
            refreshUndoRedo();
        }
    }

    private class SaveAsGIFAction extends AbstractAction {

        public SaveAsGIFAction(){
            super("Export to GIF...");
        }
        
        public void actionPerformed(ActionEvent e){
            // Open a chooser dialog
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new File(((diva.gui.DefaultStoragePolicy)getStoragePolicy()).getDirectory()));
            FileFilter ff =
                new ExtensionFileFilter("GIF", "GIF Format");        
            fc.addChoosableFileFilter(ff);
            fc.setFileFilter(ff);
        
            int result = fc.showSaveDialog(getAppContext().makeComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosenFile = fc.getSelectedFile();
                if(chosenFile.exists()) {
                    // Query on overwrite
                    int opt = JOptionPane.showConfirmDialog(
                            getAppContext().makeComponent(), 
			    "File \"" + chosenFile.getName() + 
			    "\" exists. Overwrite?", "Overwrite file?",
                            JOptionPane.YES_NO_OPTION);
                    if (opt != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                try {
                    ((WhiteboardView)getCurrentView()).saveAsGIF(chosenFile);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }                   
            }
        }
    }
    
    
    private class UndoAction extends AbstractAction {

        public UndoAction(){
            super("Undo");
        }
        
        public void actionPerformed(ActionEvent e){
            //            System.out.println(getCurrentView().getDocument().getUndoManager());
            getCurrentView().getDocument().getUndoManager().undo();
            refreshUndoRedo();
        }
    }

    private void refreshUndoRedo(){
        UndoManager mgr = getCurrentView().getDocument().getUndoManager();

        _undoMenuItem.setText(mgr.getUndoPresentationName());
        _undoMenuItem.setEnabled(mgr.canUndo());
        
        _redoMenuItem.setText(mgr.getRedoPresentationName());
        _redoMenuItem.setEnabled(mgr.canRedo());
    }

    /**
     * Registered in the UndoableEditSupport of a document.  It
     * listens for edit events.  When an edit happens, it extracts
     * the edit from the event, adds it to the UndoManager of the
     * document, and refreshes the undo-related GUI widget state.
     */
    private class UndoAdaptor implements UndoableEditListener {
        public void undoableEditHappened(UndoableEditEvent evt) {
            UndoableEdit edit = evt.getEdit();
            UndoManager mgr = getCurrentView().getDocument().getUndoManager();
            mgr.addEdit(edit);
            refreshUndoRedo();
        }
    }    
}

/**
     * Save the current page of the specified document to the file in
     * JPEG format.
     *
    protected void saveAsJPEG(Document document, File f){
        MultipageDocument d = (MultipageDocument) document;
        Page page = d.getCurrentPage();
        SketchPane pane = (SketchPane)_sketchPanes.get(page);
        int w = getDesktopContext().makeComponent().getWidth();
        int h = getDesktopContext().makeComponent().getHeight();
        BufferedImage image =
            (BufferedImage)getDesktopContext().makeComponent().createImage(w, h);
        Graphics2D g = (Graphics2D)image.getGraphics();
        //write to the image
        pane.paint(g);
        // write it out in the format you want
        try{
            String orig = f.getName();
            String sub = orig.substring(0, orig.lastIndexOf('.'));
            String fileName = sub+"."+SketchStoragePolicy.JPEG;
            FileOutputStream out = new FileOutputStream(f);
            JPEGImageEncoder encoder =
                JPEGCodec.createJPEGEncoder(out); 
            encoder.encode(image);
            out.flush();
            out.close();                    
            System.out.println("Wrote JPEG image to file "+fileName);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        //dispose of the graphics content
        g.dispose();
    }
    */


