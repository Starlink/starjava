/*
 * ESO Archive
 *
 * $Id: CatalogQueryTool.java,v 1.15 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/02  Created
 */

package jsky.catalog.gui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jsky.catalog.Catalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.util.I18N;
import jsky.util.Interruptable;
import jsky.util.SwingWorker;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.GridBagUtil;
import jsky.util.gui.ProgressException;


/**
 * Displays a CatalogQueryPanel in a JScrollPane and implements a search() method.
 */
public class CatalogQueryTool extends JPanel
        implements ActionListener, Interruptable {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CatalogQueryTool.class);

    /** The catalog to use */
    private Catalog _catalog;

    /** Catalog title label */
    private JLabel _catalogTitleLabel;

    /** Panel containing labels and entries for searching the catalog */
    private CatalogQueryPanel _catalogQueryPanel;

    /** reuse file chooser widget */
    private JFileChooser _fileChooser;

    /** Used to display query results */
    private QueryResultDisplay _queryResultDisplay;

    /** Used to scroll the query options panel */
    private JScrollPane _scrollPane;

    /** Utility object used to control background thread */
    private SwingWorker _worker;


    /**
     * Create a CatalogQueryTool for searching the given catalog.
     *
     * @param catalog The catalog to use.
     */
    public CatalogQueryTool(Catalog catalog) {
        _catalog = catalog;
        _catalogTitleLabel = makeCatalogPanelLabel(catalog);
        _catalogQueryPanel = makeCatalogQueryPanel(catalog);
        _catalogQueryPanel.addActionListener(this);

        _scrollPane = new JScrollPane(_catalogQueryPanel);

        GridBagUtil layout = new GridBagUtil(this);
        layout.add(_catalogTitleLabel, 0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.NONE,
                GridBagConstraints.CENTER,
                new Insets(3, 0, 3, 0));
        layout.add(_scrollPane, 0, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.BOTH,
                GridBagConstraints.CENTER,
                new Insets(0, 0, 0, 0));
        layout.add(makeButtonPanel(), 0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER,
                new Insets(5, 0, 0, 5));
    }


    /**
     * Create a CatalogQueryTool for searching the given catalog.
     *
     * @param catalog The catalog to use.
     * @param queryResultDisplay object used to display query results
     */
    public CatalogQueryTool(Catalog catalog, QueryResultDisplay queryResultDisplay) {
        this(catalog);
        _queryResultDisplay = queryResultDisplay;
    }

    /** Make and return the catalog panel label */
    protected JLabel makeCatalogPanelLabel(Catalog catalog) {
        String title = catalog.toString();
        setName(title);
        return new JLabel(title, JLabel.CENTER);
    }


    /** Make and return the catalog query panel */
    protected CatalogQueryPanel makeCatalogQueryPanel(Catalog catalog) {
        return new CatalogQueryPanel(catalog, 2);
    }


    /** Make and return the button panel */
    protected JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton goButton = new JButton(_I18N.getString("query"));
        goButton.setToolTipText(_I18N.getString("startQuery"));
        goButton.addActionListener(this);
        buttonPanel.add(goButton);

        return buttonPanel;
    }


    /** Set the object used to diplay the result of a query */
    public void setQueryResultDisplay(QueryResultDisplay q) {
        _queryResultDisplay = q;
    }

    /** Return the object used to diplay the result of a query */
    public QueryResultDisplay getQueryResultDisplay() {
        return _queryResultDisplay;
    }

    /**
     * Stop the background loading thread if it is running
     */
    public void interrupt() {
        if (_worker != null) {
            _worker.interrupt();
        }
        _worker = null;
    }

    /**
     * Return the name of this component (based on the data being displayed)
     */
    public String getName() {
        if (_catalog != null)
            return _catalog.getName();
        return _I18N.getString("catalog");
    }


    /** Return the catalog for this object */
    public Catalog getCatalog() {
        return _catalog;
    }


    /** Return the panel containing labels and entries for searching the catalog */
    public CatalogQueryPanel getCatalogQueryPanel() {
        return _catalogQueryPanel;
    }


    /**
     * Called when return is typed in one of the query panel text fields
     * to start the query.
     */
    public void actionPerformed(ActionEvent ev) {
        search();
    }


    /**
     * Query the catalog based on the settings in the query panel and display
     * the results.
     */
    public void search() {
        if (_queryResultDisplay == null)
            return;

        // run in a separate thread, so the user can monitor progress and cancel it, if needed
        _worker = new SwingWorker() {

            public Object construct() {
                try {
                    QueryArgs queryArgs = _catalogQueryPanel.getQueryArgs();
                    QueryResult queryResult = _catalog.query(queryArgs);
                    return queryResult;
                }
                catch (Exception e) {
                    return e;
                }
            }

            public void finished() {
                _worker = null;
                Object o = getValue();
                if (o instanceof ProgressException) {
                    // user canceled operation (pressed Stop button in progress panel): ignore
                    return;
                }
                if (o instanceof Exception) {
                    DialogUtil.error((Exception) o);
                    return;
                }
                _queryResultDisplay.setQueryResult((QueryResult) o);
            }
        };
        _worker.start();
    }
}

