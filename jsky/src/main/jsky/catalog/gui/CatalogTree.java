/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CatalogTree.java,v 1.12 2002/08/20 18:03:25 brighton Exp $
 */

package jsky.catalog.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.catalog.QueryResult;
import jsky.catalog.QueryResultHandler;
import jsky.catalog.URLQueryResult;
import jsky.catalog.astrocat.AstroCatConfig;
import jsky.util.I18N;
import jsky.util.SwingWorker;
import jsky.util.TclUtil;
import jsky.util.gui.BasicWindowMonitor;
import jsky.util.gui.ClipboardHelper;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ProgressPanel;
import javax.swing.SwingUtilities;


/**
 * Used to display a catalog hierarchy.
 */
public class CatalogTree extends JPanel 
    implements QueryResultDisplay, QueryResultHandler {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogTree.class);

    // The tree widget 
    private JTree _tree;

    // Set to true if tree node selections should be ignored 
    private boolean _ignoreSelection = false;

    // Optional handler, used to report HTML format results (errors) from HTTP servers 
    private HTMLQueryResultHandler _htmlQueryResultHandler;

    // Reference to object used to display leaf items, such as tables, etc. 
    private QueryResultDisplay _queryResultDisplay;

    // Reference to the object being displayed (should be a CatalogDirectory object) 
    private QueryResult _queryResult;

    // Reference to the top level catalog directory object passed to the constructor 
    private CatalogDirectory _rootCatDir;

    // Hash table associating each tree node that has been already selected with a query result 
    private Hashtable _treeNodeTable = new Hashtable(10);

    // Utility object used to control background thread 
    private CatalogLoader _loader;

    // Panel used to display status information 
    private ProgressPanel _progressPanel;

    // Popup menu for tree nodes 
    private JPopupMenu _nodeMenu;

    // Action to use for the "Cut" menu and toolbar items 
    private AbstractAction _cutAction = new AbstractAction(_I18N.getString("cut")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                cut();
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to use for the "Copy" menu and toolbar items 
    private AbstractAction _copyAction = new AbstractAction(_I18N.getString("copy")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                copy();
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to use for the "Paste" menu and toolbar items 
    private AbstractAction _pasteAction = new AbstractAction(_I18N.getString("paste")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                paste();
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };


    // Action to use for the "Move Up" menu and toolbar items 
    private AbstractAction _moveUpAction = new AbstractAction(_I18N.getString("moveUp")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                moveNode(true);
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to use for the "Move Down" menu and toolbar items 
    private AbstractAction _moveDownAction = new AbstractAction(_I18N.getString("moveDown")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                moveNode(false);
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to use for the "To Up" menu and toolbar items 
    private AbstractAction _toTopAction = new AbstractAction(_I18N.getString("toTop")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                moveNodeToEnd(true);
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    // Action to use for the "To Bottom" menu and toolbar items 
    private AbstractAction _toBottomAction = new AbstractAction(_I18N.getString("toBottom")) {
        public void actionPerformed(ActionEvent evt) {
            try {
                moveNodeToEnd(false);
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    /**
     * Create a CatalogTree and display the given catalog directory hierarchy
     */
    public CatalogTree(CatalogDirectory catDir) {
        setMinimumSize(new Dimension(250, 250));

	_rootCatDir = catDir;
        _tree = new JTree(_rootCatDir);
        _tree.setShowsRootHandles(true);
        _tree.setBackground(getBackground());

        // setup the tree node popup menus
        _nodeMenu = makeNodeMenu();
        _tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    _nodeMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    _nodeMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        _cutAction.setEnabled(false);
        _copyAction.setEnabled(false);
        _pasteAction.setEnabled(false);
        _toTopAction.setEnabled(false);
        _moveUpAction.setEnabled(false);
        _moveDownAction.setEnabled(false);
        _toBottomAction.setEnabled(false);

        // Enable tool tips.
        _tree.setCellRenderer(_getTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(_tree);

        // Listen for when the selection changes.
        _tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                try {
                    _nodeSelected();
		    updateEnabledStates();
                }
                catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(_tree);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 0));
        panel.add(scrollPane);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }



    /** Optional handler, used to report HTML format errors from servers */
    public void setHTMLQueryResultHandler(HTMLQueryResultHandler handler) {
        _htmlQueryResultHandler = handler;
    }



    /**
     * Return the renderer to use for tree cells (may be redefined
     * in a subclass. The default here returns an instance of
     * CatalogTreeRenderer.
     */
    private TreeCellRenderer _getTreeCellRenderer() {
        return new CatalogTreeCellRenderer();
    }


    /** Set the object used to display leaf items, such as tables, etc. */
    public void setQueryResultDisplay(QueryResultDisplay q) {
        _queryResultDisplay = q;
    }

    /** Return the object used to display leaf items, such as tables, etc. */
    public QueryResultDisplay getQueryResultDisplay() {
        return _queryResultDisplay;
    }


    /** Return the internal JTree */
    public JTree getTree() {
        return _tree;
    }


    /**
     * Display the tree structure of the given query result, which may be
     * either a CatalogDirectory or a URLQueryResult, where an HTTP get
     * of the URL returns a CatalogDirectory.
     */
    public void setQueryResult(QueryResult queryResult) {
        if (queryResult instanceof URLQueryResult) {
            _addURLQueryResult(null, ((URLQueryResult)queryResult).getURL());
        }
        else if (queryResult instanceof CatalogDirectory) {
            _queryResult = queryResult;
            _tree.setModel((CatalogDirectory)queryResult);
        }
	else {
	    throw new RuntimeException("Expected a CatalogDirectory object, not: " + _queryResult);
	}
    }


    /**
     * Return a reference to the QueryResult being displayed (in this case,
     * it should be a CatalogDirectory object of some kind).
     */
    public QueryResult getQueryResult() {
        return _queryResult;
    }


    // Do an HTTP GET on the given URL and add the result to the tree at the given node,
    // if applicable.
    private void _addURLQueryResult(Catalog node, URL url) {
        if (_progressPanel == null) {
            _progressPanel = ProgressPanel.makeProgressPanel(_I18N.getString("downloadingCatalogDesc"), this);
        }

        // run in a separate thread, so the user can monitor progress and cancel it, if needed
        if (_loader == null) {
            _loader = new CatalogLoader(node, url);
            _loader.start();
        }
    }


    // This method is called when a tree node is selected
    private void _nodeSelected() {
        if (_ignoreSelection)
            return;

        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null) {
            return;
        }
	//System.out.println("XXX selected path = " + TclUtil.makeList(selectedNode.getPath()));

        if (selectedNode == _rootCatDir) {
            if (_queryResultDisplay != null) 
                _queryResultDisplay.setQueryResult(null);
            return;
        }

	_addCatalog(selectedNode, selectedNode);
    }


    /** Select the given tree node */
    public void selectNode(Catalog node) {
        _ignoreSelection = true;
        try {
            _tree.setSelectionPath(new TreePath(node.getPath()));
        }
        catch (Exception e) {
            System.out.println(_I18N.getString("noTreeNodeSelectWarning"));
        }
        _ignoreSelection = false;
    }


    /** Return the currently selected tree node */
    public Catalog getSelectedNode() {
        return (Catalog)_tree.getLastSelectedPathComponent();
    }


    /**
     * Add the given query result to the tree at the currently selected node.
     */
    public void addQueryResult(QueryResult queryResult) {
        Catalog selectedNode = (Catalog)_tree.getLastSelectedPathComponent();
        if (selectedNode == null) {
            setQueryResult(queryResult);
        }
        else {
            _addQueryResult(selectedNode, queryResult);
        }
    }


    // Add the given query result to the tree at the given node.
    private void _addQueryResult(Catalog node, QueryResult queryResult) {
        if (queryResult instanceof URLQueryResult) {
            _addURLQueryResult(node, ((URLQueryResult) queryResult).getURL());
        }
        else if (queryResult instanceof CatalogDirectory) {
            _addCatalogDirectory(node, (CatalogDirectory)queryResult);
        }
        else if (queryResult instanceof Catalog) {
            _addCatalog(node, (Catalog)queryResult);
        }
    }


    // Add the given catalog directory to the tree at the given node.
    private void _addCatalogDirectory(Catalog node, CatalogDirectory catDir) {
        int numCatalogs = catDir.getNumCatalogs();
        if (numCatalogs > 0) {
	    CatalogDirectory parent = node.getParent();
	    if (parent != null) {
		parent.replaceCatalog(node, catDir);
		//selectNode(catDir);
		_tree.expandPath(new TreePath(catDir.getPath()));
	    }
	    else if (node instanceof CatalogDirectory) {
		// node must be the root node
		setQueryResult(catDir);
	    }
	    else {
		throw new RuntimeException("No parent node specified for: " + node.getName());
	    }
        }
        else {
            // might be a link to the catalog description
            _addCatalog(node, catDir);
        }
    }

    // Add the user interface for the given catalog to the query result display.
    private void _addCatalog(Catalog node, Catalog catalog) {
        if (_queryResultDisplay != null) {
            if (catalog.getNumParams() == 0) {
                // if there are no parameters, do the query to get to the actual catalog
                QueryResult queryResult;
                try {
                    queryResult = catalog.query(new BasicQueryArgs(catalog));
                }
                catch (Exception e) {
                    DialogUtil.error(e);
                    return;
                }
                _addQueryResult(node, queryResult);
            }
            else {
		_queryResultDisplay.setQueryResult(catalog);
	    }
        }
    }


    // This method is called after queryResultHandler.getQueryResult(URL)
    // returns (in the event handling thread) to display the contents of the given
    // catalog directory in the tree at the given node.
    private void _displayQueryResult(Catalog node, CatalogDirectory catDir) {
	QueryResult queryResult = catDir;
	if (catDir.getNumCatalogs() == 1)
	    queryResult = catDir.getCatalog(0);

	if (node == null) {
	    setQueryResult(queryResult);
	}
	else {
	    _addQueryResult(node, queryResult);
	}
    }



    // Make and return a popup menu for tree nodes
    private JPopupMenu makeNodeMenu() {
        JPopupMenu menu = new JPopupMenu();

        menu.add(_cutAction);
        menu.add(_copyAction);
        menu.add(_pasteAction);

        menu.addSeparator();
        menu.add(_toTopAction);
        menu.add(_moveUpAction);
        menu.add(_moveDownAction);
        menu.add(_toBottomAction);

        return menu;
    }


    /**
     * Return a menu item to reload the catalog config file and rebuild the
     * tree with the new data.
     */
    public JMenuItem makeReloadMenuItem() {
        JMenuItem menuItem = new JMenuItem(_I18N.getString("reloadConfigFile"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                reload();
            }
        });
        return menuItem;
    }


    /**
     * If there is a URL corresponding to the root node, read it again and
     * rebuild the tree (in case the file changed...).
     */
    public void reload() {
        _rootCatDir = _rootCatDir.reload();
        setQueryResult(_rootCatDir);
    }


    /**
     * This method is called in a background thread to get the contents of the
     * given URL and return a QueryResult object representing it.
     */
    public QueryResult getQueryResult(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        String contentType = connection.getContentType();
        if (contentType != null && contentType.equals("text/html")) {
	    // might be an HTML error from an HTTP server
	    _htmlQueryResultHandler.displayHTMLPage(url);
	}
	else {
	    String filename = url.getFile();
	    CatalogDirectory parentDir = _rootCatDir;
	    Catalog selectedNode = getSelectedNode();
	    if (selectedNode != null && selectedNode != _rootCatDir) {
		CatalogDirectory dir = selectedNode.getParent();
		if (dir != null)
		    parentDir = dir;
	    }
	    return parentDir.loadSubDir(url);
        }
        throw new RuntimeException(_I18N.getString("urlAccessError") + ": " + url.toString());
    }

    // This method is called when a tree node is selected to update the
    // enabled states of the actions defined in this class.
    private void updateEnabledStates() {
        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null)
            return;

        Catalog[] path = selectedNode.getPath();
        int pathLen = 0;
	if (path != null)
	    pathLen = path.length;
        boolean b = (pathLen == 2);

        _cutAction.setEnabled(b);
        _copyAction.setEnabled(true);
        _pasteAction.setEnabled(pathLen == 1 && ClipboardHelper.getClipboard() instanceof Catalog);

        _toTopAction.setEnabled(b);
        _moveUpAction.setEnabled(b);
        _moveDownAction.setEnabled(b);
        _toBottomAction.setEnabled(b);
    }


    /**
     * Cut the selected catalog to the clipboard.
     */
    public void cut() {
        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null)
            return;

	ClipboardHelper.setClipboard(selectedNode);
	_rootCatDir.removeCatalog(selectedNode);
	_rootCatDir.save();
    }


    /**
     * Copy the selected catalog to the clipboard.
     */
    public void copy() {
        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null)
            return;
	ClipboardHelper.setClipboard(selectedNode);
    }


    /**
     * Paste the selected catalog from the clipboard.
     */
    public void paste() {
        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null) {
	    return;
	}

        Object o = ClipboardHelper.getClipboard();
        if (o instanceof Catalog) {
	    Catalog cat = (Catalog)o;
	    _rootCatDir.addCatalog(cat);
	    _rootCatDir.save();
	}
	else {
            if (o == null)
                DialogUtil.error(_I18N.getString("noCatalogObjInClipboard"));
            else
                DialogUtil.error(_I18N.getString("noSuitableObjInClipboard"));
	}
    }


    /** Move the the selected catalog up or down in the tree. */
    public void moveNode(boolean up) {
        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null)
            return;

	_rootCatDir.moveCatalog(selectedNode, up);
	_rootCatDir.save();
    }


    /** Move the the selected catalog all the way up or down in the tree, as far as possible. */
    public void moveNodeToEnd(boolean up) {
        Catalog selectedNode = getSelectedNode();
        if (selectedNode == null)
            return;

	_rootCatDir.moveCatalogToEnd(selectedNode, up);
	_rootCatDir.save();
    }


    // access other toolbar actions
    public AbstractAction getCutAction() {
        return _cutAction;
    }

    public AbstractAction getCopyAction() {
        return _copyAction;
    }

    public AbstractAction getPasteAction() {
        return _pasteAction;
    }

    public AbstractAction getMoveUpAction() {
        return _moveUpAction;
    }

    public AbstractAction getMoveDownAction() {
        return _moveDownAction;
    }

    public AbstractAction getToTopAction() {
        return _toTopAction;
    }

    public AbstractAction getToBottomAction() {
        return _toBottomAction;
    }

    // This utility class is used to load a catalog file or URL in a background thread.
    private class CatalogLoader extends SwingWorker {

        private Catalog _node;
        private URL _url;

        /**
         * Construct a CatalogLoader with a reference to the given tree node and URL
         */
        public CatalogLoader(Catalog node, URL url) {
            _node = node;
            _url = url;
            _progressPanel.start();
        }

        /** Load the URL in the background thread */
        public Object construct() {
            try {
                return getQueryResult(_url);
            }
            catch (Exception e) {
                return e;
            }
        }

        /** Called in default thread after construct() is done */
        public void finished() {
            _progressPanel.stop();
            _loader = null;

            Object result = getValue();
            if (result instanceof Exception) {
                DialogUtil.error((Exception) result);
                return;
            }

            if (result == null || !(result instanceof QueryResult)) {
                DialogUtil.error(_I18N.getString("urlLoadError") + ": " + _url.toString());
                return;
            }

            if (result instanceof CatalogDirectory) {
		// A URL returned a catalog directory
                _displayQueryResult(_node, (CatalogDirectory)result);
            }
            else if (_queryResultDisplay != null) {
		// if the URL's content type was not recognized here, it may be something that the
		// QueryResultDisplay class knows how to display
                _queryResultDisplay.setQueryResult(new URLQueryResult(_url));
            }
        }
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("CatalogTree");

        CatalogDirectory catDir = AstroCatConfig.getConfigFile();
        CatalogTree catTree = new CatalogTree(catDir);

        frame.getContentPane().add(catTree, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

