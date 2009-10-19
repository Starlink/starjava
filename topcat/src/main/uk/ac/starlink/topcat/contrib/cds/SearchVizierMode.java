package uk.ac.starlink.topcat.contrib.cds;

import cds.vizier.VizieRCatalog;
import cds.vizier.VizieRQueryInterface;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ArrayTableSorter;

/**
 * Abstract VizierMode which presents a list of catalogues as selected
 * in some way by other GUI actions of the user.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public abstract class SearchVizierMode implements VizierMode {

    private final String name_;
    private final VizieRQueryInterface vqi_;
    private final VizierTableLoadDialog tld_;
    private final boolean useSplit_;
    private final ArrayTableModel tModel_;
    private final JTable table_;
    private final JScrollPane tScroller_;
    private final Action startSearchAction_;
    private final Action cancelSearchAction_;
    private Component panel_;
    private SearchWorker searchWorker_;

    /**
     * Constructor.
     *
     * @param   name  mode name
     * @param   vqi  vizier query interface
     * @param   tld  controlling load dialogue instance
     * @param   useSplit  true to use a JSplitPane to separate query panel
     *          from catalogue display table; false to use a fixed layout
     */
    public SearchVizierMode( String name, VizieRQueryInterface vqi,
                             VizierTableLoadDialog tld, boolean useSplit ) {
        name_ = name;
        vqi_ = vqi;
        tld_ = tld;
        useSplit_ = useSplit;
        tModel_ = new ArrayTableModel();
        tModel_.setColumns( createCatalogColumns() );
        table_ = new JTable( tModel_ );
        table_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        tScroller_ = new JScrollPane( table_,
                                      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        ArrayTableSorter sorter = new ArrayTableSorter( tModel_ );
        sorter.install( table_.getTableHeader() );
        sorter.setSorting( 0, false );

        startSearchAction_ = new AbstractAction( "Search Catalogues" ) {
            public void actionPerformed( ActionEvent evt ) {
                SearchWorker worker =
                    new SearchWorker( tld_.getTarget(),
                                      tld_.getRadius() + " deg",
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
            panel_ = createComponent( createSearchComponent() );
        }
        return panel_;
    }

    public JTable getQueryableTable() {
        return table_;
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
        searchWorker_ = worker;
        updateActions();
    }

    /**
     * Updates the enabledness of various components of this GUI based on
     * its current state.
     */
    private void updateActions() {
        startSearchAction_.setEnabled( searchWorker_ == null &&
                                       tld_.hasTarget() );
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
     * Obtains a VizieRCatalog object from one of the data items in the
     * table used by this object.
     *
     * @param  item  data item in suitable array table
     * @return  VizieRCatalog object
     */
    private static VizieRCatalog getCatalog( Object item ) {
        return ((CatalogQueryable) item).catalog_;
    }

    /**
     * Returns the columns for display of CatalogQueryable objects.
     *
     * @return   column list
     */
    private static ArrayTableColumn[] createCatalogColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return getCatalog( item ).getName();
                }
            },
            new ArrayTableColumn( "Category", String.class ) {
                public Object getValue( Object item ) {
                    return getCatalog( item ).getCategory();
                }
            },
            new ArrayTableColumn( "Density", Integer.class ) {
                public Object getValue( Object item ) {
                    return new Integer( getCatalog( item ).getDensity() );
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getCatalog( item ).getDesc();
                }
            },
        };
    }

    /**
     * Adapter class to present a VizieRCatalog as a Queryable.
     */
    private static class CatalogQueryable implements Queryable {
        private final VizieRCatalog catalog_;

        /**
         * Constructor.
         *
         * @param   catalog  VizieRCatalog object
         */
        CatalogQueryable( VizieRCatalog catalog ) {
            catalog_ = catalog;
        }

        public String getQuerySource() {
            return catalog_.getName();
        }

        public String getQueryId() {
            return catalog_.getName().replace( '/', '.' );
        }
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
            final CatalogQueryable[] qcats = loadCatalogs();
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        setSearchWorker( null );
                        tModel_.setItems( qcats );
                        tScroller_.getVerticalScrollBar().setValue( 0 );
                        StarJTable.configureColumnWidths( table_, 600, 1000 );
                        table_.setAutoResizeMode( JTable
                                                 .AUTO_RESIZE_ALL_COLUMNS );
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
         * Queries VizieR for catalogues and returns the result.
         *
         * @return   array of known queryables
         */
        private CatalogQueryable[] loadCatalogs() { 
            VizieRCatalog[] cats =
                (VizieRCatalog[]) 
                vqi_.queryVizieR( target_, radius_, null, null, queryArgs_ )
                    .toArray( new VizieRCatalog[ 0 ] );
            CatalogQueryable[] qcats = new CatalogQueryable[ cats.length ];
            for ( int i = 0; i < cats.length; i++ ) {
                qcats[ i ] = new CatalogQueryable( cats[ i ] );
            }
            return qcats;
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
