/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HTMLViewer.java,v 1.10 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.html;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import jsky.util.FileUtil;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GenericToolBarTarget;


/**
 * Used to associate handler classes with file suffixes
 * and mime-types. This information is used to determine which class
 * to use to display a file or URL returned from a catalog query.
 *
 * @version $Revision: 1.10 $
 * @author Allan Brighton
 */
public class HTMLViewer extends JPanel implements GenericToolBarTarget {

    /** The top level parent frame (or internal frame) used to close the window */
    protected Component parent;

    /** Used to display HTML */
    protected JEditorPane html;

    /** The URL of the currently displayed page */
    protected URL url;

    /** Reuse file chooser widget */
    protected static JFileChooser fileChooser;

    /** list of listeners for change events */
    protected EventListenerList listenerList = new EventListenerList();

    /** Stack of HTMLViewerHistoryItem, used to go back to a previous page */
    protected Stack backStack = new Stack();

    /** Stack of HTMLViewerHistoryItem, used to go forward to the next page */
    protected Stack forwStack = new Stack();

    /** Set when the back or forward actions are active to avoid the normal history stack handling */
    protected boolean noStack = false;

    /** List of HTMLViewerHistoryItem, for previously viewed catalogs or query results. */
    protected LinkedList historyList;

    /** Base filename for serialization of the history list */
    protected static final String HISTORY_LIST_NAME = "htmlViewerHistoryList";

    /** Max number of items in the history list */
    protected int maxHistoryItems = 20;

    /** Action to use for the "Open..." menu and toolbar items */
    protected AbstractAction openAction = new AbstractAction("Open") {

        public void actionPerformed(ActionEvent evt) {
            open();
        }
    };

    /** Action to use for the "Back" menu and toolbar items */
    protected AbstractAction backAction = new AbstractAction("Back") {

        public void actionPerformed(ActionEvent evt) {
            back();
        }
    };

    /** Action to use for the "Forward" menu and toolbar items */
    protected AbstractAction forwAction = new AbstractAction("Forward") {

        public void actionPerformed(ActionEvent evt) {
            forward();
        }
    };

    /** Action to use for the "Save as..." menu and toolbar items */
    protected AbstractAction saveAsAction = new AbstractAction("Save As") {

        public void actionPerformed(ActionEvent evt) {
            saveAs();
        }
    };

    /** Action to use for the "Print..." menu and toolbar items */
    protected AbstractAction printAction = new AbstractAction("Print...") {

        public void actionPerformed(ActionEvent evt) {
            print();
        }
    };


    /**
     * Create a window for displaying HTML.
     */
    public HTMLViewer() {
        html = new JEditorPane();
        html.setEditable(false);
        html.setContentType("text/html");
        html.addHyperlinkListener(createHyperLinkListener());

        JScrollPane scroller = new JScrollPane();
        JViewport vp = scroller.getViewport();
        vp.add(html);
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);

        // try to restore the history from the previous session
        loadHistory();

        // arrange to save the history list for the next session on exit
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                //addToHistory();
                saveHistory(true);
            }
        });

        backAction.setEnabled(false);
        forwAction.setEnabled(false);
    }


    /**
     * Create a window for displaying HTML.
     *
     * @param parent the top level parent frame (or internal frame) used to close the window
     */
    public HTMLViewer(Component parent) {
        this();
        this.parent = parent;
    }

    /**
     * Create a window for displaying the given HTML URL.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param url A URL to display
     */
    public HTMLViewer(Component parent, URL url) {
        this(parent);
        setPage(url);
    }


    /** Used to display HTML */
    public JEditorPane getEditorPane() {
        return html;
    }

    /**
     * Display the given URL, which should have the content type text/html.
     *
     * @param url A URL to display
     */
    public void setPage(final URL url) {
        if (url == null)
            return;

        addToHistory();
        this.url = url;
        // This text should show while the translation is in progress
        //html.setText("<html><body><blink><em>Processing, please wait...</em></blink></body></html>");
        //html.repaint();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    html.setPage(url);
                    fireChange(new ChangeEvent(this));
                }
                catch (Exception ex) {
                    DialogUtil.error(ex);
                }
            }
        });
    }

    /**
     * Display the given HTML text.
     */
    public void setText(String text) {
        try {
            addToHistory();
            html.setText(text);
            this.url = null;
            fireChange(new ChangeEvent(this));
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /** Create and return a listener for HTML links. */
    protected HyperlinkListener createHyperLinkListener() {
        return new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e instanceof HTMLFrameHyperlinkEvent) {
                        ((HTMLDocument) html.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
                    }
                    else {
                        setPage(e.getURL());
                    }
                }
            }
        };
    }


    /**
     * Display a file chooser to select a local catalog file to open
     */
    public void open() {
        if (fileChooser == null) {
            fileChooser = makeFileChooser();
        }
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
            open(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * Create and return a new file chooser to be used to select a file to open.
     */
    protected JFileChooser makeFileChooser() {
        JFileChooser fileChooser = new JFileChooser(new File("."));
        return fileChooser;
    }


    /**
     * Open and display the given file or URL
     */
    public void open(String fileOrUrl) {
        URL url = FileUtil.makeURL(null, fileOrUrl);
        setPage(url);
    }

    /**
     * Display a dialog to enter a URL to display
     */
    public void openURL() {
        String urlStr = DialogUtil.input("Enter the World Wide Web location (URL) to display:");
        if (urlStr != null) {
            URL url = null;
            try {
                url = new URL(urlStr);
            }
            catch (Exception e) {
                DialogUtil.error(e);
                return;
            }
            setPage(url);
        }
    }


    /**
     * Close the window
     */
    public void close() {
        if (parent != null)
            parent.setVisible(false);
    }


    /**
     * Go back to the previous component in the history list
     */
    public void back() {
        if (backStack.size() == 0)
            return;

        if (url != null) {
            forwStack.push(makeHTMLViewerHistoryItem());
            forwAction.setEnabled(true);
        }

        HTMLViewerHistoryItem historyItem = (HTMLViewerHistoryItem) backStack.pop();
        if (backStack.size() == 0)
            backAction.setEnabled(false);

        HTMLViewerMenuBar.setCurrentHTMLViewer(this);
        noStack = true;
        try {
            historyItem.actionPerformed(null);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        noStack = false;
    }


    /**
     * Go forward to the next component in the history list
     */
    public void forward() {
        if (forwStack.size() == 0)
            return;

        if (url != null) {
            backStack.push(makeHTMLViewerHistoryItem());
            backAction.setEnabled(true);
        }

        HTMLViewerHistoryItem historyItem = (HTMLViewerHistoryItem) forwStack.pop();
        if (forwStack.size() == 0)
            forwAction.setEnabled(false);

        HTMLViewerMenuBar.setCurrentHTMLViewer(this);
        noStack = true;
        try {
            historyItem.actionPerformed(null);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
        noStack = false;
    }


    /**
     * Register to receive change events from this object whenever a new
     * HTML page is displayed.
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }


    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Notify any listeners that a new HTML page is being displayed.
     */
    protected void fireChange(ChangeEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(e);
            }
        }
    }

    /**
     * Add the current URL to the history stack, removing duplicates.
     */
    protected void addToHistory() {
        if (url == null)
            return;

        HTMLViewerHistoryItem historyItem = makeHTMLViewerHistoryItem();
        if (historyItem == null)
            return;

        if (!noStack) {
            backStack.push(historyItem);
            backAction.setEnabled(true);
            if (forwStack.size() != 0) {
                forwStack.clear();
                forwAction.setEnabled(false);
            }
        }
        addToHistory(historyItem);
    }

    /**
     * Add the given item to the history stack, removing duplicates.
     */
    protected void addToHistory(HTMLViewerHistoryItem historyItem) {

        // remove duplicates from history list
        ListIterator it = ((LinkedList) historyList.clone()).listIterator(0);
        for (int i = 0; it.hasNext(); i++) {
            HTMLViewerHistoryItem item = (HTMLViewerHistoryItem) it.next();
            if (item.title.equals(historyItem.title))
                historyList.remove(i);
        }

        historyList.addFirst(historyItem);
        if (historyList.size() > maxHistoryItems)
            historyList.removeLast();
    }

    /** Return a new HTMLViewerHistoryItem for the currently displayed catalog. */
    protected HTMLViewerHistoryItem makeHTMLViewerHistoryItem() {
        //HTMLDocument doc = (HTMLDocument)html.getDocument();
        //String title = (String)doc.getProperty(Document.TitleProperty);
        String title = url.toString();
        if (title != null) {
            return new HTMLViewerHistoryItem(title, url);
        }
        return null;
    }

    /** Return the max number of items in the history list. */
    public int getMaxHistoryItems() {
        return maxHistoryItems;
    }

    /** Set the max number of items in the history list. */
    public void setMaxHistoryItems(int n) {
        maxHistoryItems = n;
    }

    /** Add history items (for previously displayed components) to the given menu */
    public void addHistoryMenuItems(JMenu menu) {
        ListIterator it = historyList.listIterator(0);
        while (it.hasNext()) {
            menu.add((HTMLViewerHistoryItem) it.next());
        }
    }


    /**
     * This method is called after the history list is deserialized to remove any
     * items in the list that can't be accessed.
     */
    protected void cleanupHistoryList() {
        ListIterator it = historyList.listIterator(0);
        while (it.hasNext()) {
            HTMLViewerHistoryItem item = (HTMLViewerHistoryItem) it.next();
            if (item.getURLStr() == null)
                it.remove();
        }
    }


    /**
     * Merge the historyList with current serialized version (another instance
     * may have written it since we read it last).
     */
    protected LinkedList mergeHistoryList() {
        LinkedList savedHistory = historyList;
        loadHistory();

        // Go through the list in reverse, since addToHistory inserts at the start of the list
        ListIterator it = savedHistory.listIterator(savedHistory.size() - 1);
        while (it.hasPrevious()) {
            addToHistory((HTMLViewerHistoryItem) it.previous());
        }
        return historyList;
    }

    /**
     * Add the current URL to the history list
     */
    protected void clearHistory() {
        historyList = new LinkedList();
        backAction.setEnabled(false);
        backStack.clear();
        forwAction.setEnabled(false);
        forwStack.clear();
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
                l = historyList;
            Preferences.getPreferences().serialize(HISTORY_LIST_NAME, l);
        }
        catch (Exception e) {
        }
    }

    /** Try to load the history list from a file, and create an empty list if that fails. */
    protected void loadHistory() {
        try {
            historyList = (LinkedList) Preferences.getPreferences().deserialize(HISTORY_LIST_NAME);
            cleanupHistoryList();
        }
        catch (Exception e) {
            historyList = new LinkedList();
        }
    }

    /**
     * Pop up a dialog to ask the user for a file name, and then save the current page
     * to the selected file.
     */
    public void saveAs() {
    }


    /**
     * Pop up a dialog for printing the current page.
     */
    public void print() {
    }


    // These are for the GenericToolBarTarget interface
    public AbstractAction getOpenAction() {
        return openAction;
    }

    public AbstractAction getSaveAsAction() {
        return saveAsAction;
    }

    public AbstractAction getPrintAction() {
        return printAction;
    }

    public AbstractAction getBackAction() {
        return backAction;
    }

    public AbstractAction getForwAction() {
        return forwAction;
    }
}


