package uk.ac.starlink.topcat.vizier;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ArrayTableSorter;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Abstract VizierMode which presents a list of catalogues as selected
 * in some way by other GUI actions of the user.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public abstract class SearchVizierMode implements VizierMode {

    private final String name_;
    private final VizierInfo vizinfo_;
    private final VizierTableLoadDialog tld_; 
    private final boolean useSplit_;
    private final ArrayTableModel tModel_;
    private final JTable table_;
    private final JScrollPane tScroller_;
    private final Action startSearchAction_;
    private final Action cancelSearchAction_;
    private Component panel_;
    private Component searchComponent_;
    private SearchWorker searchWorker_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.vizier" );

    /**
     * Constructor.
     *
     * @param   name  mode name
     * @param   vizinfo  vizier query interface
     * @param   tld  controlling load dialogue instance
     * @param   useSplit  true to use a JSplitPane to separate query panel
     *          from catalogue display table; false to use a fixed layout
     */     
    public SearchVizierMode( String name, VizierInfo vizinfo,
                             VizierTableLoadDialog tld, boolean useSplit ) {
        name_ = name; 
        vizinfo_ = vizinfo;
        tld_ = tld;
        useSplit_ = useSplit;
        tModel_ = new ArrayTableModel();
        tModel_.setColumns( createCatalogColumns() );
        table_ = new JTable( tModel_ );
        table_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        tScroller_ =
            new JScrollPane( table_,
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        ArrayTableSorter sorter = new ArrayTableSorter( tModel_ );
        sorter.install( table_.getTableHeader() );
        sorter.setSorting( 0, false );

        startSearchAction_ = new AbstractAction( "Search Catalogues" ) {
            public void actionPerformed( ActionEvent evt ) {
                SearchWorker worker =
                    new SearchWorker( tld_.getTarget(), tld_.getRadius(),
                                      getSearchArgs() );
                setSearchWorker( worker );
                worker.start();
            }
        };
        cancelSearchAction_ = new AbstractAction( "Cancel Search" ) {
            public void actionPerformed( ActionEvent evt ) {
                setSearchWorker( null );
            }
        };
        tld_.addTargetActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateActions();
            }
        } );
        tld_.addTargetCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateActions();
            }
        } );
        updateActions();
    }

    /**
     * Constructs the GUI component which the user will fill in to
     * specify what catalogues they want to select from.
     * The setEnable() method on the returned component should ideally
     * enable/disable all GUI controls visible in it.
     *
     * @return   search component
     */
    protected abstract Component createSearchComponent();

    /**
     * Returns the arguments, based on the current state of the search
     * component, to pass to the VizieR server to search for available
     * catalogues.
     *
     * @return   URL fragment giving catalogue search arguments
     */
    protected abstract String getSearchArgs();

    public String getName() {
        return name_;
    }

    public Component getComponent() {
        if ( panel_ == null ) {
            searchComponent_ = createSearchComponent();
            panel_ = createComponent( searchComponent_ );
            updateActions();
        }
        return panel_;
    }

    public JTable getQueryableTable() {
        return table_;
    }

    /**
     * Returns the action which starts a search for catalogues.
     *
     * @return  start search action
     */
    public Action getSearchAction() {
        return startSearchAction_;
    }

    /**
     * Declares that a given SearchWorker object is now working on behalf
     * of this component to search for suitable catalogues.
     *
     * @param   worker  newly active search worker; may be null for no active
     *                  search
     */
    private void setSearchWorker( SearchWorker worker ) {
        if ( searchWorker_ != null ) {
            searchWorker_.cancel();
        }
        tScroller_.setViewportView( worker == null
                                        ? table_
                                        : createSearchProgressPanel() );
        searchWorker_ = worker;
        updateActions();
    }

    /**
     * Updates the enabledness of various components of this GUI based on
     * its current state.
     */
    private void updateActions() {
        boolean canSearch = searchWorker_ == null && tld_.hasTarget();
        startSearchAction_.setEnabled( canSearch );
        if ( searchComponent_ != null ) {
            searchComponent_.setEnabled( canSearch );
        }
        cancelSearchAction_.setEnabled( searchWorker_ != null );
    }

    /**
     * Used for lazy construction of the GUI component comprising this mode.
     *
     * @param   searchComponent  the mode-specific GUI component
     */
    private Component createComponent( Component searchComponent ) {

        /* Set up the container with the start/stop search buttons. */
        JComponent searchPanel = new JPanel( new BorderLayout() );
        searchPanel.add( searchComponent, BorderLayout.CENTER );
        JComponent buttonLine = Box.createHorizontalBox();
        buttonLine.add( Box.createHorizontalGlue() );
        buttonLine.add( new JButton( startSearchAction_ ) );
        buttonLine.add( Box.createHorizontalStrut( 10 ) );
        buttonLine.add( new JButton( cancelSearchAction_ ) );
        buttonLine.add( Box.createHorizontalGlue() );
        buttonLine.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        searchPanel.add( buttonLine, BorderLayout.SOUTH );

        /* Place the mode-specific search component appropriately. */
        if ( useSplit_ ) {
            JSplitPane splitter =
                new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
                                searchPanel, tScroller_ );
            splitter.setResizeWeight( 0.5 );
            return splitter;
        }
        else {
            JComponent panel = new JPanel( new BorderLayout() );
            panel.add( searchPanel, BorderLayout.NORTH );
            panel.add( tScroller_, BorderLayout.CENTER );
            return panel;
        }
    }

    /**
     * Returns a panel which can be used to indicate that a query for
     * catalogues is taking place.
     *
     * @return  search progress panel
     */
    private static JComponent createSearchProgressPanel() {
        JComponent msgLine = Box.createHorizontalBox();
        msgLine.add( Box.createHorizontalGlue() );
        msgLine.add( new JLabel( "Locating suitable catalogues" ) );
        msgLine.add( Box.createHorizontalGlue() );

        JComponent progLine = Box.createHorizontalBox();
        JProgressBar progBar = new JProgressBar();
        progBar.setIndeterminate( true );
        progLine.add( Box.createHorizontalGlue() );
        progLine.add( progBar );
        progLine.add( Box.createHorizontalGlue() );

        JComponent workBox = Box.createVerticalBox();
        workBox.add( Box.createVerticalGlue() );
        workBox.add( msgLine );
        workBox.add( Box.createVerticalStrut( 5 ) );
        workBox.add( progLine );
        workBox.add( Box.createVerticalGlue() );

        return workBox;
    }

    /**
     * Returns a list of catalogues appropriate for a given position.
     * If a read error occurs, a warning dialogue is posted and an 
     * empty list returned.
     *
     * @param   target   central position in vizier format
     * @param   radius   search radius in degrees in vizier format
     * @param   queryArgs  any additional args ready for appending to
     *                     a vizier query URL
     */
    private VizierCatalog[] loadCatalogs( String target, String radius,
                                          String queryArgs ) {
        Throwable error;
        try {
            return attemptLoadCatalogs( target, radius, queryArgs );
        }
        catch ( IOException e ) {
            error = e;
        }
        catch ( ParserConfigurationException e ) {
            error = e;
        }
        catch ( SAXException e ) {
            error = e;
        }
        assert error != null;
        final Throwable error1 = error;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                ErrorDialog
               .showError( vizinfo_.getParent(), "VizieR Error", error1,
                           "Couldn't read catalog list from VizieR" );
            }
        } );
        return new VizierCatalog[ 0 ];
    }

    /**
     * Attempts to search VizieR for a list of catalogues appropriate
     * for a given position.
     *
     * @param   target   central position in vizier format
     * @param   radius   search radius in degrees in vizier format
     * @param   queryArgs  any additional args ready for appending to
     *                     a vizier query URL
     */
    private VizierCatalog[] attemptLoadCatalogs( String target, String radius,
                                                 String queryArgs )
            throws IOException, ParserConfigurationException, SAXException {
        StringBuffer ubuf = new StringBuffer()
            .append( VizierInfo.VIZIER_BASE_URL )
            .append( "?-meta" );
        if ( target != null && target.trim().length() > 0 ) {
            ubuf.append( VizierTableLoadDialog.encodeArg( "-c", target ) );
        }
        if ( radius != null && radius.trim().length() > 0 ) {
            ubuf.append( VizierTableLoadDialog.encodeArg( "-c.r", radius ) );
            ubuf.append( VizierTableLoadDialog.encodeArg( "-c.u", "deg" ) );
        }
        if ( queryArgs != null && queryArgs.trim().length() > 0 ) {
            ubuf.append( queryArgs );
        }
        URL url = new URL( ubuf.toString() );
        logger_.info( url.toString() );
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        spfact.setNamespaceAware( false );
        spfact.setValidating( false );
        SAXParser parser = spfact.newSAXParser();
        CatalogSaxHandler catHandler = new CatalogSaxHandler();
        InputStream in = new BufferedInputStream( url.openStream() );
        try {
            parser.parse( in, catHandler );
        }
        finally {
            in.close();
        }
        return catHandler.getCatalogs();
    }

    /**
     * Returns the columns for display of VizierCatalog objects.
     *
     * @return   column list
     */
    private static ArrayTableColumn[] createCatalogColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return ((VizierCatalog) item).getName();
                }
            },
            new ArrayTableColumn( "Popularity", Integer.class ) {
                public Object getValue( Object item ) {
                    return ((VizierCatalog) item).getCpopu();
                }
            },
            new ArrayTableColumn( "Density", Integer.class ) {
                public Object getValue( Object item ) {
                    return ((VizierCatalog) item).getDensity();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return ((VizierCatalog) item).getDescription();
                }
            },
            new ArrayTableColumn( "Wavelengths", String.class ) {
                public Object getValue( Object item ) {
                    return concat( ((VizierCatalog) item).getLambdas() );
                }
            },
            new ArrayTableColumn( "Astronomy", String.class ) {
                public Object getValue( Object item ) {
                    return concat( ((VizierCatalog) item).getAstros() );
                }
            },
        };
    }

    /**
     * Concatenates strings together for display.
     *
     * @param   strings  array of input strings
     * @return  single string with comma-separated values
     */
    private static String concat( String[] strings ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < strings.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( strings[ i ] );
        }
        return sbuf.toString();
    }

    /**
     * Thread which performs the search to locate suitable catalogues
     * for interrogation, based on the user's preferences as filled in
     * to the mode-specific parts of this mode's GUI.
     */
    private class SearchWorker extends Thread {
        final String target_;
        final String radius_;
        final String queryArgs_;

        /**
         * Constructor.
         *
         * @param   target  VizieR target string,
         *                  may be empty for no spatial restriction
         * @param   radius  radius string in degrees,
         *                  may be empty
         * @param   queryArgs  other query arguments as a URL fragment string
         */
        SearchWorker( String target, String radius, String queryArgs ) {
            target_ = target;
            radius_ = radius;
            queryArgs_ = queryArgs;
        }

        public void run() {
            final VizierCatalog[] qcats =
                loadCatalogs( target_, radius_, queryArgs_ );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        setSearchWorker( null );
                        tModel_.setItems( qcats );
                        tScroller_.getVerticalScrollBar().setValue( 0 );
                        StarJTable.configureColumnWidths( table_, 600, 1000 );
                    }
                }
            } );
        }

        /**
         * Frees resources if the result of this search will not be required.
         */
        public void cancel() {
            // should interrupt VizieR query here?
        }

        /**
         * Indicates whether this worker is currently active on behalf of
         * the mode.  If not, it should not now nor ever affect the GUI further.
         *
         * @return   true  iff still active
         */
        boolean isActive() {
            return this == searchWorker_;
        }
    };
}
