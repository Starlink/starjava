/*
 * ESO Archive
 *
 * $Id: CatalogNavigator.java,v 1.45 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/02  Created
 */

package jsky.catalog.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.HTMLQueryResultHandler;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.URLQueryResult;
import jsky.html.HTMLViewerFrame;
import jsky.html.HTMLViewerInternalFrame;
import jsky.util.FileUtil;
import jsky.util.I18N;
import jsky.util.Preferences;
import jsky.util.PrintableWithDialog;
import jsky.util.Saveable;
import jsky.util.SaveableAsHTML;
import jsky.util.SwingWorker;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GenericToolBarTarget;
import jsky.util.gui.ProgressException;
import jsky.util.gui.ProgressPanel;

/**
 * Used to navigate the catalog hierarchy. This class displays a tree of catalogs in one
 * panel and the interface for searching the catalog, or the query results in the other panel.
 * <p>
 * The tree display is based on a top level catalog directory. The details must be defined
 * in a derived class.
 */
public abstract class CatalogNavigator extends JPanel
        implements QueryResultDisplay, GenericToolBarTarget,
        HTMLQueryResultHandler {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogNavigator.class);

    /** The top level parent frame (or internal frame) used to close the window */
    private Component _parent;

    /** True if this is the main application window (enables exit menu item) */
    private static boolean _mainWindowFlag = false;

    /** Set this to the JDesktopPane, if using internal frames. */
    private JDesktopPane _desktop = null;

    /** Set to true to query catalogs automatically when selected */
    private boolean _autoQuery = false;

    /** Displays the catalog tree and the catalog query widgets */
    private JPanel _queryPanel;

    /** Displays query results, such as tabular data. */
    private JPanel _resultPanel;

    /** Tree displaying catalog hierarchy */
    private CatalogTree _catalogTree;

    /** Query panel currently being displayed */
    private JComponent _queryComponent;

    /** Result panel currently being displayed */
    private JComponent _resultComponent;

    /** The original URL for the display component's data (for history list) */
    private URL _origURL;

    /** reuse file chooser widget */
    private static JFileChooser _fileChooser;

    /** Panel used to display download progress information */
    private ProgressPanel _progressPanel;

    /** list of listeners for change events */
    private EventListenerList _listenerList = new EventListenerList();

    /** Stack of CatalogHistoryItems, used to go back to a previous panel */
    private Stack _backStack = new Stack();

    /** Stack of CatalogHistoryItems, used to go forward to the next panel */
    private Stack _forwStack = new Stack();

    /** Set when the back or forward actions are active to avoid the normal history stack handling */
    private boolean _noStack = false;

    /** Saved query result (set in background thread) */
    private QueryResult _queryResult;

    /** Optional object to use to plot table data */
    private TablePlotter _plotter;

    /** Utility object used to control background thread */
    private SwingWorker _worker;

    /** Top level window (or internal frame) for viewing an HTML page */
    private Component _htmlViewerFrame;

    /** Hash table associating each panel with a tree node */
    private Hashtable _panelTreeNodeTable = new Hashtable(10);

    /** List of CatalogHistoryItem, for previously viewed catalogs or query results. */
    private LinkedList _historyList;

    /** Base filename for serialization of the history list */
    private static final String HISTORY_LIST_NAME = "catalogHistoryList";

    /** Max number of items in the history list */
    private int _maxHistoryItems = 20;

    /** Maps query components to their corresponding result components */
    private Hashtable _queryResultComponentMap = new Hashtable();

    /** The pane dividing the catalog tree and the query panel */
    private JSplitPane _querySplitPane;

    /** The pane dividing the query and the results panel */
    private JSplitPane _resultSplitPane;

    /** Action to use for the "Open..." menu and toolbar items */
    private AbstractAction _openAction = new AbstractAction(_I18N.getString("open")) {

        public void actionPerformed(ActionEvent evt) {
            open();
        }
    };

    /** Action to use for the "Save as..." menu and toolbar items */
    private AbstractAction _saveAsAction = new AbstractAction(_I18N.getString("saveAs")) {

        public void actionPerformed(ActionEvent evt) {
            saveAs();
        }
    };

    /** Action to use for the "Save With Image..." menu and toolbar items */
    private AbstractAction _saveWithImageAction = new AbstractAction(_I18N.getString("saveCatalogWithImage")) {

        public void actionPerformed(ActionEvent evt) {
            saveWithImage();
        }
    };

    /** Action to use for the "Save as HTML..." menu and toolbar items */
    private AbstractAction _saveAsHTMLAction = new AbstractAction(_I18N.getString("saveAsHTML")) {

        public void actionPerformed(ActionEvent evt) {
            saveAsHTML();
        }
    };

    /** Action to use for the "Print..." menu and toolbar items */
    private AbstractAction _printAction = new AbstractAction(_I18N.getString("print") + "...") {

        public void actionPerformed(ActionEvent evt) {
            print();
        }
    };

    /** Action to use for the "Back" menu and toolbar items */
    private AbstractAction _backAction = new AbstractAction(_I18N.getString("back")) {

        public void actionPerformed(ActionEvent evt) {
            back();
        }
    };

    /** Action to use for the "Forward" menu and toolbar items */
    private AbstractAction _forwAction = new AbstractAction(_I18N.getString("forward")) {

        public void actionPerformed(ActionEvent evt) {
            forward();
        }
    };

    /** Action to use for the "Add Row" menu item */
    private AbstractAction _addRowAction = new AbstractAction(_I18N.getString("addRow")) {

        public void actionPerformed(ActionEvent evt) {
            addRow();
        }
    };

    /** Action to use for the "Delete Rows..." menu item */
    private AbstractAction _deleteSelectedRowsAction = new AbstractAction(_I18N.getString("deleteSelectedRows")) {

        public void actionPerformed(ActionEvent evt) {
            deleteSelectedRows();
        }
    };


    /**
     * Construct a CatalogNavigator using the given CatalogTree widget
     * (Call setQueryResult to set the root catalog to display).
     *
     * @param parent the parent component
     * @param catalogTree a CatalogTree (normally a subclass of CatalogTree
     *                    that knows about certain types of catalogs)
     */
    public CatalogNavigator(Component parent, CatalogTree catalogTree) {
        _parent = parent;
        setLayout(new BorderLayout());
        _catalogTree = catalogTree;
        catalogTree.setQueryResultDisplay(this);
        catalogTree.setHTMLQueryResultHandler(this);
        catalogTree.setPreferredSize(new Dimension(256, 0));

        _queryPanel = new JPanel();
        _queryPanel.setLayout(new BorderLayout());

        _resultPanel = new JPanel();
        _resultPanel.setLayout(new BorderLayout());

        _querySplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalogTree, _queryPanel);
        _querySplitPane.setOneTouchExpandable(true);

        _resultSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _querySplitPane, _resultPanel);
        _resultSplitPane.setOneTouchExpandable(true);
        _resultSplitPane.setDividerLocation(270);

        add(_resultSplitPane, BorderLayout.CENTER);

        // try to restore the history from the previous session
        loadHistory();

        // arrange to save the history list for the next session on exit
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                //addToHistory();
                saveHistory(true);
            }
        });
    }

    /**
     * Construct a CatalogNavigator using the given CatalogTree widget
     * and TablePlotter
     * (Call setQueryResult to set the root catalog to display).
     *
     * @param parent the parent component
     *
     * @param catalogTree a CatalogTree (normally a subclass of CatalogTree
     *                    that knows about certain types of catalogs)
     *
     * @param plotter the object to use to plot catalog table data
     *                (when the plot button is pressed)
     */
    public CatalogNavigator(Component parent, CatalogTree catalogTree, TablePlotter plotter) {
        this(parent, catalogTree);
        _plotter = plotter;
    }


    /** Return the object displaying the catalog tree */
    public CatalogTree getCatalogTree() {
        return _catalogTree;
    }


    /** The pane dividing the catalog tree and the query panel */
    protected JSplitPane getQuerySplitPane() {
        return _querySplitPane;
    }

    /** The pane dividing the query and the results panel */
    protected JSplitPane getResultSplitPane() {
        return _resultSplitPane;
    }

    /** Return the JDesktopPane, if using internal frames, otherwise null */
    public JDesktopPane getDesktop() {
        return _desktop;
    }

    /** Set the JDesktopPane to use for top level windows, if using internal frames */
    public void setDesktop(JDesktopPane desktop) {
        _desktop = desktop;
    }

    /** Set to true to query catalogs automatically when selected */
    public void setAutoQuery(boolean b) {
        _autoQuery = b;
    }

    /** Return the object used to plot table data, or null if none was defined. */
    public TablePlotter getPlotter() {
        return _plotter;
    }

    /** Set the object used to plot table data. */
    public void setPlotter(TablePlotter tp) {
        _plotter = tp;
    }

    /** Return the top level parent frame (or internal frame) for this window */
    public Component getParentFrame() {
        return _parent;
    }

    /**
     * Set the query or result component to display. The choice is made based on
     * which interfaces the component implements. If the component implements
     * QueryResultDisplay, it is considered a result component.
     */
    public void setComponent(JComponent component) {
        if (component instanceof QueryResultDisplay) {
            setResultComponent(component);
        }
        else {
            setQueryComponent(component);
	    
	    //System.out.println("XXX _autoQuery = " + _autoQuery  + ", component is a " + component.getClass());

            if ((component instanceof CatalogQueryTool)
                    && (_autoQuery || ((CatalogQueryTool) component).getCatalog().isLocal())) {
                ((CatalogQueryTool) component).search();
            }
        }
    }


    /** Set the query component to display */
    public void setQueryComponent(JComponent component) {
        if (component == null || component == _queryComponent)
            return;

        if (_queryComponent != null) {
            addToHistory();
            _queryPanel.remove(_queryComponent);
            _queryComponent = null;
        }

        _queryComponent = component;

        Catalog cat = _catalogTree.getSelectedNode();
        if (cat != null)
            _panelTreeNodeTable.put(_queryComponent, cat);

        _queryPanel.add(_queryComponent, BorderLayout.CENTER);

        // restore the query result corresponding to this catalog, if known
        Object resultComp = _queryResultComponentMap.get(_queryComponent);
        if (resultComp == null)
            setResultComponent(new EmptyPanel());
        else
            setResultComponent((JComponent) resultComp);
        update();
    }

    /** Return the panel currently being displayed */
    public JComponent getQueryComponent() {
        return _queryComponent;
    }


    /** Set the result component to display */
    public void setResultComponent(JComponent component) {
        if (component == null || component == _resultComponent)
            return;

        if (_resultComponent != null) {
            if (_resultComponent instanceof TableDisplayTool) {
                // if we're not reusing the current table window, tell it to hide any related popup
                // windows before replacing it (It might be needed again later though, if the user
                // goes back to it).
                //((TableDisplayTool)_resultComponent).hidePopups();
            }
            _resultPanel.remove(_resultComponent);
            _resultComponent = null;
        }

        _resultComponent = component;
        if (_queryComponent != null)
            _queryResultComponentMap.put(_queryComponent, _resultComponent);
        _resultPanel.add(_resultComponent, BorderLayout.CENTER);
        update();
        _resultComponentChanged();

        // try to display the right amount of the query window
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                _resultSplitPane.resetToPreferredSizes();
            }
        });
    }

    /** Return the panel currently being displayed */
    public JComponent getResultComponent() {
        return _resultComponent;
    }


    /** Called whenever the display component is changed */
    protected void _resultComponentChanged() {
        // set the state of the "Save As..." menu item
        _saveAsAction.setEnabled(_resultComponent instanceof Saveable);
        _printAction.setEnabled(_resultComponent instanceof PrintableWithDialog);

        boolean isTable = (_resultComponent instanceof TableDisplayTool);
        _saveWithImageAction.setEnabled(isTable);
        _deleteSelectedRowsAction.setEnabled(isTable);
        _addRowAction.setEnabled(isTable);

        fireChange(new ChangeEvent(this));
    }


    /**
     * Register to receive change events from this object whenever a new
     * query result is displayed.
     */
    public void addChangeListener(ChangeListener l) {
        _listenerList.add(ChangeListener.class, l);
    }


    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        _listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Notify any listeners that a new query result is being displayed.
     */
    protected void fireChange(ChangeEvent e) {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(e);
            }
        }
    }

    /**
     * Add the current catalog to the history stack, removing duplicates.
     */
    protected void addToHistory() {
        if (_queryComponent == null)
            return;

        CatalogHistoryItem historyItem = makeCatalogHistoryItem();
        if (historyItem == null)
            return;

        if (!_noStack) {
            _backStack.push(historyItem);
            _backAction.setEnabled(true);
            if (_forwStack.size() != 0) {
                cleanupHistoryStack(_forwStack);
                _forwStack.clear();
                _forwAction.setEnabled(false);
            }
        }
        addToHistory(historyItem);
    }

    /**
     * Add the given item to the history stack, removing duplicates.
     */
    protected void addToHistory(CatalogHistoryItem historyItem) {

        // remove duplicates from history list
        ListIterator it = ((LinkedList) _historyList.clone()).listIterator(0);
        for (int i = 0; it.hasNext(); i++) {
            CatalogHistoryItem item = (CatalogHistoryItem) it.next();
            if (item.name.equals(historyItem.name))
                _historyList.remove(i);
        }

        _historyList.addFirst(historyItem);
        if (_historyList.size() > _maxHistoryItems)
            _historyList.removeLast();
    }


    /** Return a new CatalogHistoryItem for the currently displayed catalog. */
    protected CatalogHistoryItem makeCatalogHistoryItem() {
        String s = _queryComponent.getName();
        if (s != null) {
            return new CatalogHistoryItem(s, _origURL, _queryComponent);
        }
        return null;
    }

    /** Return the max number of items in the history list. */
    public int getMaxHistoryItems() {
        return _maxHistoryItems;
    }

    /** Set the max number of items in the history list. */
    public void setMaxHistoryItems(int n) {
        _maxHistoryItems = n;
    }

    /** Add history items (for previously displayed components) to the given menu */
    public void addHistoryMenuItems(JMenu menu) {
        ListIterator it = _historyList.listIterator(0);
        while (it.hasNext()) {
            menu.add((CatalogHistoryItem) it.next());
        }
    }


    /**
     * This method is called after the history list is deserialized to remove any
     * items in the list that can't be accessed.
     */
    protected void cleanupHistoryList() {
        ListIterator it = _historyList.listIterator(0);
        while (it.hasNext()) {
            CatalogHistoryItem item = (CatalogHistoryItem) it.next();
            if (item.getURLStr() == null)
                it.remove();
        }
    }


    /**
     * This method may be redefined in subclasses to do cleanup work before components are
     * removed from the given history stack (_backStack or _forwStack).
     */
    protected void cleanupHistoryStack(Stack stack) {
        unplot(stack);
    }


    /**
     * Merge the _historyList with current serialized version (another instance
     * may have written it since we read it last).
     */
    protected LinkedList mergeHistoryList() {
        LinkedList savedHistory = _historyList;
        loadHistory();

        // Go through the list in reverse, since addToHistory inserts at the start of the list
        ListIterator it = savedHistory.listIterator(savedHistory.size() - 1);
        while (it.hasPrevious()) {
            addToHistory((CatalogHistoryItem) it.previous());
        }
        return _historyList;
    }

    /**
     * Add the current URL to the history list
     */
    protected void clearHistory() {
        _historyList = new LinkedList();
        _backAction.setEnabled(false);
        _backStack.clear();
        _forwAction.setEnabled(false);
        _forwStack.clear();
        saveHistory(false);
    }

    /**
     * Save the current history list to a file.
     *
     * @param merge if true, merge the list with the existing list on disk.
     */
    protected void saveHistory(boolean merge) {
        try {
            LinkedList l;
            if (merge)
                l = mergeHistoryList();
            else
                l = _historyList;
            Preferences.getPreferences().serialize(HISTORY_LIST_NAME, l);
        }
        catch (Exception e) {
        }
    }

    /** Try to load the history list from a file, and create an empty list if that fails. */
    protected void loadHistory() {
        try {
            _historyList = (LinkedList) Preferences.getPreferences().deserialize(HISTORY_LIST_NAME);
            cleanupHistoryList();
        }
        catch (Exception e) {
            _historyList = new LinkedList();
        }
    }

    /**
     * Set the original URL for the current catalog or table.
     *
     * @param url the URL of the catalog, table or FITS file
     */
    public void setOrigURL(URL url) {
        _origURL = url;
    }


    /**
     * Remove any plot symbols or graphics managed by any of the display
     * components in the given stack
     */
    protected void unplot(Stack stack) {
        // Unplot any catalog symbols before loosing the information
        int n = stack.size();
        for (int i = 0; i < n; i++) {
            CatalogHistoryItem item = (CatalogHistoryItem) (stack.get(i));
            Object resultComp = _queryResultComponentMap.get(item.queryComponent);
            if (resultComp instanceof TableDisplayTool) {
                ((TableDisplayTool) resultComp).unplot();
            }
        }
    }


    /** Remove any plot symbols or graphics managed by any of the display components */
    public void unplot() {
        Enumeration e = _queryResultComponentMap.elements();
        while (e.hasMoreElements()) {
            JComponent comp = (JComponent) e.nextElement();
            if (comp instanceof TableDisplayTool) {
                ((TableDisplayTool) comp).unplot();
            }
        }
    }


    /** Update the layout after a new component has been inserted */
    protected void update() {
        //updateTreeSelection();
        _queryPanel.revalidate();
        _resultPanel.revalidate();
        _parent.repaint();
    }


    /**
     * Select the node in the catalog directory tree corresponding to the current
     * display component
     */
    protected void updateTreeSelection() {
        if (_queryComponent instanceof CatalogQueryTool) {
            _catalogTree.selectNode(((CatalogQueryTool) _queryComponent).getCatalog());
            _updateTitle(((CatalogQueryTool) _queryComponent).getCatalog());
        }
        else if (_queryComponent instanceof TableDisplayTool) {
            _catalogTree.selectNode(((TableDisplayTool) _queryComponent).getTable());
        }
    }


    public QueryResult getQueryResult() {
        return _queryResult;
    }

    /**
     * Display the given query result.
     */
    public void setQueryResult(QueryResult queryResult) {
        if (queryResult == null) {
            return;
        }

        if (_worker != null) {
            // shouldn't happen if user interface disables it
            DialogUtil.error(_I18N.getString("queryInProgress"));
            return;
        }

        // result is a URL, get the data in a background thread
        _queryResult = queryResult;

        // Use a background thread for remote catalog access only
        boolean isLocal = true;
        if (queryResult instanceof URLQueryResult) {
            URLQueryResult uqr = (URLQueryResult) queryResult;
            URL url = uqr.getURL();
            isLocal = (url.getProtocol().equals("file"));
        }
	else if (queryResult instanceof Catalog) {
            isLocal = ((Catalog)queryResult).isLocal();
	}

        if (isLocal) {
            // Its not a URL, so do it in the foreground
            setComponent(makeQueryResultComponent(queryResult));
        }
        else {
            // remote catalog: run in a separate thread, so the user can monitor progress
            makeProgressPanel();

            _worker = new SwingWorker() {

                JComponent component;

                public Object construct() {
                    component = makeQueryResultComponent();
                    return component;
                }

                public void finished() {
                    _worker = null;
                    _progressPanel.stop();
                    setComponent(component);
                }
            };
            _worker.start();
        }
    }

    /** Update the frame's title to display the name of the given catalog */
    private void _updateTitle(Catalog catalog) {
        String title = _I18N.getString("catalogNavigator");
        String s = catalog.getTitle();
        if (s != null && s.length() > 0)
            title += " - " + s;
        if (_parent != null) {
            if (_parent instanceof JFrame)
                ((JFrame) _parent).setTitle(title);
            else
                ((JInternalFrame) _parent).setTitle(title);
        }
    }


    /**
     * If it does not already exist, make the panel used to display
     * the progress of network access.
     */
    protected void makeProgressPanel() {
        if (_progressPanel == null) {
            _progressPanel = ProgressPanel.makeProgressPanel(_I18N.getString("accessingCatalogServer"), _parent);
            _progressPanel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (_worker != null) {
                        _worker.interrupt();
                        _worker = null;
                    }
                }
            });
        }
    }


    /** Create and return a component displaying the given query result */
    protected JComponent makeQueryResultComponent() {
        return makeQueryResultComponent(_queryResult);
    }


    /**
     * Create and return a JComponent displaying the given query result.
     */
    protected JComponent makeQueryResultComponent(QueryResult queryResult) {
        _origURL = null;
        try {
            // See if there is a user interface handler for the query result
            if (queryResult instanceof CatalogUIHandler) {
                JComponent c = ((CatalogUIHandler) queryResult).makeComponent(this);
                if (c != null)
                    return c;
            }

            // No UI handler, return the default component for the query result
            if (queryResult instanceof CatalogDirectory) {
                return makeCatalogDirectoryComponent((CatalogDirectory) queryResult);
            }
            if (queryResult instanceof TableQueryResult) {
                return makeTableQueryResultComponent((TableQueryResult) queryResult);
            }
            if (queryResult instanceof Catalog) {
                return makeCatalogComponent((Catalog) queryResult);
            }
            if (queryResult instanceof URLQueryResult) {
                URL url = ((URLQueryResult) queryResult).getURL();
                return makeURLComponent(url);
            }
        }
        catch (Exception e) {
            if (_progressPanel != null)
                _progressPanel.stop();
            DialogUtil.error(e);
        }
        return new EmptyPanel();
    }


    /**
     * Return a new JComponent displaying the contents of the given catalog directory
     */
    protected JComponent makeCatalogDirectoryComponent(CatalogDirectory catalogDirectory) {
        // get the number of catalogs in the directory
        int numCatalogs = catalogDirectory.getNumCatalogs();
        if (numCatalogs == 0)
            return makeCatalogComponent(catalogDirectory);
        if (numCatalogs == 1)
            return makeCatalogComponent(catalogDirectory.getCatalog(0));

        return new EmptyPanel();
    }


    /**
     * Return a new JComponent displaying the contents of the given table query result.
     */
    protected JComponent makeTableQueryResultComponent(TableQueryResult tableQueryResult) {

        if (_resultComponent instanceof TableDisplayTool) {
            TableDisplayTool tdt = (TableDisplayTool) _resultComponent;
            if (tdt.getTable().getName().equals(tableQueryResult.getName())) {
                tdt.setQueryResult(tableQueryResult);
                return tdt;
            }
        }

        TableDisplayTool t = new TableDisplayTool(tableQueryResult, this, _plotter);

        // add a popup menu to the table
        makeTablePopupMenu(t);

        return t;
    }


    /** Add a popup menu to the given TableDisplayTool */
    protected void makeTablePopupMenu(TableDisplayTool t) {
        final JPopupMenu m = new JPopupMenu();
        m.add(_addRowAction);
        m.add(_deleteSelectedRowsAction);
        t.getTableDisplay().getTable().addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }


    /**
     * Return a new JComponent displaying the contents of (or the interface for searching)
     * the given catalog
     */
    protected JComponent makeCatalogComponent(Catalog catalog) {
        // catalog may contain multiple tables and implement the CatalogDirectory interface
        if (catalog instanceof CatalogDirectory) {
            CatalogDirectory catalogDirectory = (CatalogDirectory) catalog;
            int numCatalogs = catalogDirectory.getNumCatalogs();
            if (numCatalogs == 1) {
                Catalog c = catalogDirectory.getCatalog(0);
                if (c instanceof TableQueryResult) {
                    return makeTableQueryResultComponent((TableQueryResult) c);
                }
                else {
                    DialogUtil.error(_I18N.getString("subCatalogError") + ": " + c);
                    return new EmptyPanel();
                }
            }
            else if (numCatalogs > 1) {
                return makeTableQueryResultComponent(catalogDirectory.getCatalogList());
            }
        }

        if (catalog instanceof TableQueryResult)
            return makeTableQueryResultComponent((TableQueryResult) catalog);

        // Default to normal catalog query component
        CatalogQueryTool panel = makeCatalogQueryTool(catalog);
        return panel;
    }


    /** Make a panel for querying a catalog */
    protected CatalogQueryTool makeCatalogQueryTool(Catalog catalog) {
        return new CatalogQueryTool(catalog, this);
    }


    /**
     * Return a new JComponent displaying the contents of the given URL.
     */
    protected JComponent makeURLComponent(URL url) throws IOException {
        try {
            URLConnection connection;
            if (url.getProtocol().equals("file")) {
                connection = url.openConnection();
            }
            else {
                connection = _progressPanel.openConnection(url);
            }

            if (connection == null)
                return _queryComponent;

            int contentLength = connection.getContentLength();
            String contentType = connection.getContentType();
            if (contentType == null)
                contentType = "unknown";
            return makeURLComponent(url, contentType);
        }
        catch (ProgressException e) {
            // ignore: user pressed the stop button in the progress panel
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        if (_resultComponent != null) {
            return _resultComponent;
        }
        return new EmptyPanel();
    }


    /**
     * Return a new JComponent displaying the contents of the given URL.
     */
    protected JComponent makeURLComponent(URL url, String contentType) throws IOException {
        String filename = url.getFile();

        if (contentType.equals("text/html") || filename.endsWith(".html")) {
            displayHTMLPage(url);
            return _resultComponent;
        }

        if (contentType.equals("text/plain")) {
            displayPlainText(url);
            return _resultComponent;
        }

        // If it is not one of the known content types, call a method that may be
        // redefined in a derived class to handle that type
        return makeUnknownURLComponent(url, contentType);
    }


    /* XXX Attempt to show a URL in the default web browser and return true if successful.
    protected boolean displayHTMLPageWithDefaultBrowser(URL url) {
	//  XXX (a) JNLP only works when in a Java WebStart client...
	try {
	    // Lookup the javax.jnlp.BasicService object
	    BasicService bs = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
	    // Invoke the showDocument method
	    return bs.showDocument(url);
	} catch(UnavailableServiceException e) {
	    e.printStackTrace();
	    // Service is not supported
	    return false;
	}

	// XXX (b) works only if netscape is installed and in the exec path (not likely under Windows)

	// for convenience, try to load the HTML page in netscape first, and if
	// that fails, use a Java based HTML viewer
	try {
	    String[] cmd = new String[] {
		"netscape", "-remote", "openURL(" + url.toString() + ",new-window)"
	    };
	    Process process = Runtime.getRuntime().exec(cmd);
	    process.waitFor();
	    InputStream stderr = process.getErrorStream();
	    if (stderr.available() > 3)
		throw new RuntimeException("netscape not running");
	}
	catch(Exception e) {
	    return new HTMLResultViewer(_parent, url);
	}

	return false;
    }
    XXX */


    /**
     * Display the given HTML URL in a popup window containing a JEditorPane.
     */
    public void displayHTMLPage(URL url) {
        //if (displayHTMLPageWithDefaultBrowser(url))
        //    return;

        if (_htmlViewerFrame != null) {
            if (_htmlViewerFrame instanceof HTMLViewerFrame) {
                ((HTMLViewerFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
                ((HTMLViewerFrame) _htmlViewerFrame).setState(Frame.NORMAL);
                _htmlViewerFrame.setVisible(true);
            }
            else if (_htmlViewerFrame instanceof HTMLViewerInternalFrame) {
                ((HTMLViewerInternalFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
                _htmlViewerFrame.setVisible(true);
            }
            return;
        }

        if (_desktop != null) {
            _htmlViewerFrame = new HTMLViewerInternalFrame();
            ((HTMLViewerInternalFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
            _desktop.add(_htmlViewerFrame, JLayeredPane.DEFAULT_LAYER);
            _desktop.moveToFront(_htmlViewerFrame);
        }
        else {
            _htmlViewerFrame = new HTMLViewerFrame();
            ((HTMLViewerFrame) _htmlViewerFrame).getHTMLViewer().setPage(url);
        }
    }

    /**
     * Display the text pointed to by the given URL.
     */
    public void displayPlainText(URL url) {
	try{
	    String msg = FileUtil.getURL(url);
            if (_progressPanel != null)
                _progressPanel.stop();
	    if (msg.length() < 256) 
		DialogUtil.error(msg);
	    else
		displayHTMLPage(url);
	}
	catch(IOException e) {
	    DialogUtil.error(e);
	}
    }


    /**
     * Return a new JComponent displaying the contents of the given URL.
     * A null return value causes an empty panel to be displayed.
     * Returning the current component (_resultComponent) will cause no change.
     * This should be done if the URL is displayed in a separate window.
     */
    protected JComponent makeUnknownURLComponent(URL url, String contentType) {
        if (_resultComponent != null)
            return _resultComponent;
        return new EmptyPanel();
    }


    /**
     * Display a file chooser to select a local catalog file to open
     */
    public void open() {
        if (_fileChooser == null) {
            _fileChooser = makeFileChooser();
        }
        int option = _fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
            open(_fileChooser.getSelectedFile().getAbsolutePath());
        }
    }


    /**
     * Create and return a new file chooser to be used to select a local catalog file
     * to open.
     */
    protected JFileChooser makeFileChooser() {
        JFileChooser _fileChooser = new JFileChooser(new File("."));
        return _fileChooser;
    }


    /**
     * Open the given file or URL
     */
    public void open(String fileOrUrl) {
        try {
            setQueryComponent(new EmptyPanel());
            URL url = FileUtil.makeURL(null, fileOrUrl);
            URLQueryResult _queryResult = new URLQueryResult(url);

            String filename = url.getFile();
            if (filename.endsWith(".xml"))
                _catalogTree.setQueryResult(_queryResult);
            else
                setQueryResult(_queryResult);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Exit the application with the given status.
     */
    public void exit() {
        System.exit(0);
    }


    /**
     * Close the window
     */
    public void close() {
        if (_parent != null)
            _parent.setVisible(false);
        /*
	if (_parent instanceof JFrame)
	    ((JFrame)_parent).dispose();
	else if (_parent instanceof JInternalFrame)
	    ((JInternalFrame)_parent).dispose();
	*/
    }


    /**
     * Go back to the previous component in the history list
     */
    public void back() {
        if (_backStack.size() == 0)
            return;

        if (_queryComponent != null) {
            _queryPanel.remove(_queryComponent);
            URL url = _origURL;  // save and restore this
            CatalogHistoryItem item = makeCatalogHistoryItem();
            _origURL = url;
            if (item != null) {
                _forwStack.push(item);
                _forwAction.setEnabled(true);
            }
        }

        CatalogHistoryItem historyItem = (CatalogHistoryItem) _backStack.pop();
        if (_backStack.size() == 0)
            _backAction.setEnabled(false);

        CatalogNavigatorMenuBar.setCurrentCatalogNavigator(this);
        _noStack = true;
        try {
            historyItem.actionPerformed(null);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        _noStack = false;

        // select the related tree node
        if (historyItem.queryComponent != null) {
            Catalog cat = (Catalog)_panelTreeNodeTable.get(historyItem.queryComponent);
            if (cat != null)
                _catalogTree.selectNode(cat);
        }

        update();
    }


    /**
     * Go forward to the next component in the history list
     */
    public void forward() {
        if (_forwStack.size() == 0)
            return;

        if (_queryComponent != null) {
            _queryPanel.remove(_queryComponent);
            URL url = _origURL;  // save and restore this
            CatalogHistoryItem item = makeCatalogHistoryItem();
            _origURL = url;
            if (item != null) {
                _backStack.push(item);
                _backAction.setEnabled(true);
            }
        }

        CatalogHistoryItem historyItem = (CatalogHistoryItem) _forwStack.pop();
        if (_forwStack.size() == 0)
            _forwAction.setEnabled(false);

        CatalogNavigatorMenuBar.setCurrentCatalogNavigator(this);
        _noStack = true;
        try {
            historyItem.actionPerformed(null);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        _noStack = false;


        // select the related tree node
        if (historyItem.queryComponent != null) {
            Catalog cat = (Catalog)_panelTreeNodeTable.get(historyItem.queryComponent);
            if (cat != null)
                _catalogTree.selectNode(cat);
        }

        update();
    }


    // These are for the GenericToolBarTarget interface
    public AbstractAction getOpenAction() {
        return _openAction;
    }

    public AbstractAction getSaveAsAction() {
        return _saveAsAction;
    }

    public AbstractAction getSaveAsHTMLAction() {
        return _saveAsHTMLAction;
    }

    public AbstractAction getSaveWithImageAction() {
        return _saveWithImageAction;
    }

    public AbstractAction getPrintAction() {
        return _printAction;
    }

    public AbstractAction getBackAction() {
        return _backAction;
    }

    public AbstractAction getForwAction() {
        return _forwAction;
    }

    public AbstractAction getAddRowAction() {
        return _addRowAction;
    }

    public AbstractAction getDeleteSelectedRowsAction() {
        return _deleteSelectedRowsAction;
    }

    /**
     * Return the top level parent frame (or internal frame) for this window
     */
    public Component getRootComponent() {
        return _parent;
    }


    /**
     * Display a dialog to enter a URL to display
     */
    public void openURL() {
        String urlStr = DialogUtil.input(_I18N.getString("enterURLDisplay") + ":");
        if (urlStr != null) {
            URL url = null;
            try {
                url = new URL(urlStr);
            }
            catch (Exception e) {
                DialogUtil.error(e);
                return;
            }
            setQueryResult(new URLQueryResult(url));
        }
    }

    /**
     * Clear the display.
     */
    public void clear() {
        setQueryComponent(new EmptyPanel());
        _origURL = null;
    }


    /**
     * Pop up a dialog to ask the user for a file name, and then save the current query result
     * to the selected file.
     */
    public void saveAs() {
        if (_resultComponent instanceof Saveable) {
            if (_fileChooser == null) {
                _fileChooser = makeFileChooser();
            }
            int option = _fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
                saveAs(_fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
        else {
            DialogUtil.error(_I18N.getString("saveNotSupportedForObjType"));
        }
    }

    /**
     * Save the current query result to the selected file.
     */
    public void saveAs(String filename) {
        if (_resultComponent instanceof Saveable) {
            try {
                ((Saveable) _resultComponent).saveAs(filename);
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
        else {
            DialogUtil.error(_I18N.getString("saveNotSupportedForObjType"));
        }
    }

    /**
     * Save the current table as a FITS table in the current FITS image
     * (Should be defined in a derived class).
     */
    public void saveWithImage() {
    }


    /**
     * Pop up a dialog to ask the user for a file name, and then save the current query result
     * to the selected file in HTML format.
     */
    public void saveAsHTML() {
        if (_resultComponent instanceof SaveableAsHTML) {
            if (_fileChooser == null) {
                _fileChooser = makeFileChooser();
            }
            int option = _fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION && _fileChooser.getSelectedFile() != null) {
                saveAsHTML(_fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
        else {
            DialogUtil.error(_I18N.getString("htmlOutputNotSupportedForObjType"));
        }
    }

    /**
     * Save the current query result to the selected file in HTML format.
     */
    public void saveAsHTML(String filename) {
        if (_resultComponent instanceof SaveableAsHTML) {
            try {
                ((SaveableAsHTML) _resultComponent).saveAsHTML(filename);
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
        else {
            DialogUtil.error(_I18N.getString("htmlOutputNotSupportedForObjType"));
        }
    }

    /**
     * Pop up a dialog for printing the query results.
     */
    public void print() {
        if (_resultComponent instanceof PrintableWithDialog) {
            try {
                ((PrintableWithDialog) _resultComponent).print();
            }
            catch (Exception e) {
                DialogUtil.error(e);
            }
        }
        else {
            DialogUtil.error(_I18N.getString("printingNotSupportedForObjType"));
        }
    }

    /**
     * If a table is being displayed, add an empty row in the table.
     */
    public void addRow() {
        if (_resultComponent instanceof TableDisplayTool) {
            ((TableDisplayTool) _resultComponent).addRow();
        }
    }

    /**
     * If a table is being displayed, delete the selected rows.
     */
    public void deleteSelectedRows() {
        if (_resultComponent instanceof TableDisplayTool) {
            ((TableDisplayTool) _resultComponent).deleteSelectedRows();
        }
    }

    /**
     * Set the editable state of the cells in the displayed table.
     */
    public void setTableCellsEditable(boolean b) {
        if (_resultComponent instanceof TableDisplayTool) {
            ((TableDisplayTool) _resultComponent).setTableCellsEditable(b);
        }
    }

    /** Return true if this is the main application window (enables exit menu item) */
    public boolean isMainWindow() {
        return _mainWindowFlag;
    }

    /** Set to true if this is the main application window (enables exit menu item) */
    public static void setMainWindow(boolean b) {
        _mainWindowFlag = b;
    }


    /** Used to identify an empty query or result panel */
    public class EmptyPanel extends JPanel implements QueryResultDisplay {

        public void setQueryResult(QueryResult queryResult) {
            throw new RuntimeException(_I18N.getString("queryResultDisplayError"));
        }
    }

    /** Return the panel used to display download progress information */
    protected ProgressPanel getProgressPanel() {
        return _progressPanel;
    }

    /** Return the stack of CatalogHistoryItems, used to go back to a previous panel */
    protected Stack getBackStack() {
        return _backStack;
    }

    /** Return the stack of CatalogHistoryItems, used to go forward to the next panel */
    protected Stack getForwStack() {
        return _forwStack;
    }

    /** List of CatalogHistoryItem, for previously viewed catalogs or query results. */
    protected LinkedList getHistoryList() {
        return _historyList;
    }

}

