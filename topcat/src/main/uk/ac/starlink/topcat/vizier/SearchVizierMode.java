package uk.ac.starlink.topcat.vizier;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
import org.xml.sax.helpers.DefaultHandler;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.util.URLUtils;
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
    private final VizierTableLoadDialog tld_; 
    private final boolean useSplit_;
    private final JTable table_;
    private final JScrollPane tScroller_;
    private final Action startSearchAction_;
    private final Action cancelSearchAction_;
    private final ToggleButtonModel includeSubModel_;
    private final ToggleButtonModel includeObsModel_;
    private ArrayTableModel<Queryable> tModel_;
    private ArrayTableSorter<Queryable> sorter_;
    private VizierInfo vizinfo_;
    private Component panel_;
    private Component searchComponent_;
    private SearchWorker searchWorker_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.vizier" );

    /**
     * Constructor.
     *
     * @param   name  mode name
     * @param   tld  controlling load dialogue instance
     * @param   useSplit  true to use a JSplitPane to separate query panel
     *          from catalogue display table; false to use a fixed layout
     */     
    public SearchVizierMode( String name, VizierTableLoadDialog tld,
                             boolean useSplit ) {
        name_ = name; 
        tld_ = tld;
        useSplit_ = useSplit;
        tModel_ = new ArrayTableModel<Queryable>( new Queryable[ 0 ] );
        tModel_.setColumns( createCatalogColumns( false ) );
        table_ = new JTable( tModel_ );
        table_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        tScroller_ =
            new JScrollPane( table_,
                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

        startSearchAction_ = new AbstractAction( "Search Catalogues" ) {
            public void actionPerformed( ActionEvent evt ) {
                SearchWorker worker =
                    new SearchWorker( tld_.getTarget(), tld_.getRadius(),
                                      getSearchArgs(), includeSubTables(),
                                      includeObsoletes() );
                setSearchWorker( worker );
                worker.start();
            }
        };
        cancelSearchAction_ = new AbstractAction( "Cancel Search" ) {
            public void actionPerformed( ActionEvent evt ) {
                setSearchWorker( null );
            }
        };
        includeSubModel_ =
            new ToggleButtonModel( "Sub-Table Details", null,
                                   "If selected, sub-tables as well as " +
                                   "top-level resources are queried " +
                                   "and listed" );
        includeObsModel_ =
            new ToggleButtonModel( "Include Obsolete Tables", null,
                                   "If selected, all will be shown; " +
                                   "otherwise, older versions of existing " +
                                   "VizieR tables will be omitted" );
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

    public void setVizierInfo( VizierInfo vizinfo ) {
        vizinfo_ = vizinfo;
    }

    /**
     * Returns the vizier info object.
     *
     * @return  vizinfo
     */
    public VizierInfo getVizierInfo() {
        return vizinfo_;
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
     * Indicates whether the search should report sub-tables or just top-level
     * resources.
     *
     * @return   false for just top-level resources, true for sub-tables as well
     */
    public boolean includeSubTables() {
        return includeSubModel_.isSelected();
    }

    /**
     * Indicates whether the search should report obsolete tables or just
     * the newest version of each.
     *
     * @return  false for just current versions, true for obsolete versions
     *          as well
     */
    public boolean includeObsoletes() {
        return includeObsModel_.isSelected();
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
                                        : worker.getProgressPanel() );
        searchWorker_ = worker;
        updateActions();
    }

    /**
     * Updates the enabledness of various components of this GUI based on
     * its current state.
     */
    private void updateActions() {
        boolean canSearch = searchWorker_ == null && tld_.hasTarget();
        includeSubModel_.setEnabled( canSearch );
        includeObsModel_.setEnabled( canSearch );
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
        JComponent optionLine = Box.createHorizontalBox();
        optionLine.add( includeSubModel_.createCheckBox() );
        optionLine.add( includeObsModel_.createCheckBox() );
        optionLine.add( Box.createHorizontalGlue() );
        JComponent buttonLine = Box.createHorizontalBox();
        buttonLine.add( Box.createHorizontalGlue() );
        buttonLine.add( new JButton( startSearchAction_ ) );
        buttonLine.add( Box.createHorizontalStrut( 10 ) );
        buttonLine.add( new JButton( cancelSearchAction_ ) );
        JComponent controlBox = Box.createVerticalBox();
        controlBox.add( optionLine );
        controlBox.add( buttonLine );
        controlBox.setBorder( BorderFactory.createEmptyBorder( 0, 5, 5, 5 ) );
        searchPanel.add( controlBox, BorderLayout.SOUTH );

        /* Place the mode-specific search component appropriately. */
        if ( useSplit_ ) {
            JSplitPane splitter =
                new JSplitPane( JSplitPane.VERTICAL_SPLIT, true,
                                searchPanel, tScroller_ );
            splitter.setResizeWeight( 0.4 );
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
    private static JComponent createProgressPanel( JProgressBar progBar ) {

        JComponent msgLine = Box.createHorizontalBox();
        msgLine.add( Box.createHorizontalGlue() );
        msgLine.add( new JLabel( "Locating suitable catalogues" ) );
        msgLine.add( Box.createHorizontalGlue() );

        JComponent progLine = Box.createHorizontalBox();
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
     * Attempts to search VizieR for a list of catalogues appropriate
     * for a given position.
     *
     * @param   target   central position in vizier format
     * @param   radius   search radius in degrees in vizier format
     * @param   queryArgs  any additional args ready for appending to
     *                     a vizier query URL
     */
    private void parseCatalogQuery( String target, String radius,
                                    String queryArgs, boolean includeSubTables,
                                    DefaultHandler catHandler )
            throws IOException, ParserConfigurationException, SAXException {
        StringBuffer ubuf = new StringBuffer()
            .append( getVizierInfo().getBaseUrl().toString() )
            .append( "?" );
        if ( includeSubTables ) {
            ubuf.append( "-meta=t" );
        }
        else {
            ubuf.append( "-meta" );
        }
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
        URL url = URLUtils.newURL( ubuf.toString() );
        logger_.info( url.toString() );
        SAXParserFactory spfact = SAXParserFactory.newInstance();
        spfact.setNamespaceAware( false );
        spfact.setValidating( false );
        SAXParser parser = spfact.newSAXParser();
        InputStream in = new BufferedInputStream( url.openStream() );
        try {
            parser.parse( in, catHandler );
        }
        finally {
            in.close();
        }
    }

    /**
     * Returns the columns for display of VizierCatalog objects.
     *
     * @param  includeSubTables  true if sub-tables will be represented in
     *           the table as well as catalogues
     * @return   column list
     */
    private static List<CatColumn<?>>
            createCatalogColumns( boolean includeSubTables ) {
        List<CatColumn<?>> colList = new ArrayList<CatColumn<?>>();
        colList.add( new CatColumn<String>( "Name", String.class ) {
            public String getValue( VizierCatalog vcat ) {
                return vcat.getName();
            }
        } );
        if ( includeSubTables ) {
            colList.add( new CatColumn<Integer>( "Tables", Integer.class ) {
                public Integer getValue( VizierCatalog vcat ) {
                    return vcat.getTableCount();
                }
            } );
            colList.add( new CatColumn<Long>( "Rows", Long.class ) {
                public Long getValue( VizierCatalog vcat ) {
                    return vcat.getRowCount();
                }
            } );
        }
        colList.add( new CatColumn<Integer>( "Popularity", Integer.class ) {
            public Integer getValue( VizierCatalog vcat ) {
                return vcat.getCpopu();
            }
        } );
        colList.add( new CatColumn<Integer>( "Density", Integer.class ) {
            public Integer getValue( VizierCatalog vcat ) {
                return vcat.getDensity();
            }
        } );
        colList.add( new CatColumn<String>( "Description", String.class ) {
            public String getValue( VizierCatalog vcat ) {
                return vcat.getDescription();
            }
        } );
        colList.add( new CatColumn<String>( "Wavelengths", String.class ) {
            public String getValue( VizierCatalog vcat ) {
                return concat( vcat.getLambdas() );
            }
        } );
        colList.add( new CatColumn<String>( "Astronomy", String.class ) {
            public String getValue( VizierCatalog vcat ) {
                return concat( vcat.getAstros() );
            }
        } );
        return colList;
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
        private final String target_;
        private final String radius_;
        private final String queryArgs_;
        private final JProgressBar progBar_;
        private final JComponent progPanel_;
        private final boolean includeSubTables_;
        private final boolean includeObsoletes_;
        private volatile boolean cancelled_;

        /**
         * Constructor.
         *
         * @param   target  VizieR target string,
         *                  may be empty for no spatial restriction
         * @param   radius  radius string in degrees,
         *                  may be empty
         * @param   queryArgs  other query arguments as a URL fragment string
         * @param   includeSubTables  true to include sub tables as separate
         *             entries, false for only resource entries
         * @param   includeObsoletes  true to include tables marked obsolete
         */
        SearchWorker( String target, String radius, String queryArgs,
                      boolean includeSubTables, boolean includeObsoletes ) {
            target_ = target;
            radius_ = radius;
            queryArgs_ = queryArgs;
            includeSubTables_ = includeSubTables;
            includeObsoletes_ = includeObsoletes;
            progBar_ = new JProgressBar();
            progBar_.setIndeterminate( true );
            progBar_.setStringPainted( true );
            progPanel_ = createProgressPanel( progBar_ );
        }

        public void run() {
            final VizierCatalog[] qcats = loadCatalogs();
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        setSearchWorker( null );
                        if ( sorter_ != null ) {
                            sorter_.uninstall( table_.getTableHeader() );
                        }
                        tModel_ = new ArrayTableModel<Queryable>
                                                     ( new Queryable[ 0 ] );
                        tModel_
                       .setColumns( createCatalogColumns( includeSubTables_ ) );
                        tModel_.setItems( qcats );
                        sorter_ = new ArrayTableSorter<Queryable>( tModel_ );
                        sorter_.install( table_.getTableHeader() );
                        sorter_.setSorting( 0, false );
                        table_.setModel( tModel_ );
                        tScroller_.getVerticalScrollBar().setValue( 0 );
                        StarJTable.configureColumnWidths( table_, 600, 1000 );
                    }
                }
            } );
        }

        /**
         * Returns a list of catalogues.
         * If a read error occurs, a warning dialogue is posted and an 
         * empty list returned.
         *
         * @return   requested catalogue list
         */
        private VizierCatalog[] loadCatalogs() {
            Throwable error;
            try {
                return attemptLoadCatalogs();
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
                    if ( isActive() ) {
                        ErrorDialog
                       .showError( vizinfo_.getParent(), "VizieR Error", error1,
                                   "Couldn't read catalog list from VizieR" );
                    }
                }
            } );
            return new VizierCatalog[ 0 ];
        }

        /**
         * Attempts to return a list of catalogues.
         *
         * @return  catalogue list
         */
        private VizierCatalog[] attemptLoadCatalogs()
                throws IOException, ParserConfigurationException, SAXException {
            final List<VizierCatalog> catList = new ArrayList<VizierCatalog>();
            progBar_.setString( "Found 0" );
            CatalogSaxHandler catHandler =
                    new CatalogSaxHandler( includeObsoletes_ ) {
                public void gotCatalog( VizierCatalog cat )
                        throws SAXException {
                    if ( cancelled_ ) {
                        throw new SAXException( "search cancelled" );
                    }
                    catList.add( cat );
                    progBar_.setString( "Found " + catList.size() );
                }
            };
            parseCatalogQuery( target_, radius_, queryArgs_, includeSubTables_,
                               catHandler );
            return catList.toArray( new VizierCatalog[ 0 ] );
        }

        /**
         * Frees resources if the result of this search will not be required.
         */
        public void cancel() {
            cancelled_ = true;
        }

        /**
         * Returns a component suitable for displaying while this worker
         * is active.
         *
         * @return   progress panel
         */
        public JComponent getProgressPanel() {
            return progPanel_;
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

    /**
     * Utility sub-class of ArrayTableColumn for use with VizierCatalogs.
     */
    private static abstract class CatColumn<C>
            extends ArrayTableColumn<VizierCatalog,C> {

        /**
         * Constructor.
         *
         * @param  name  column name
         * @param  clazz  column content class
         */
        CatColumn( String name, Class<C> clazz ) {
            super( name, clazz );
        }
    }
}
