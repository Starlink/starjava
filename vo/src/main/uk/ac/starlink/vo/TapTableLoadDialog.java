package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoader;

/**
 * Load dialogue for TAP services.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 * @see <a href="http://www.ivoa.net/Documents/TAP/">IVOA TAP Recommendation</a>
 */
public class TapTableLoadDialog extends DalTableLoadDialog {

    private JEditorPane adqlPanel_;
    private TableSetPanel tmetaPanel_;
    private final Map<String,TableMeta[]> metaMap_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public TapTableLoadDialog() {
        super( "TAP", "Query remote databases using SQL-like language",
               Capability.TAP, false, false );
        metaMap_ = new HashMap<String,TableMeta[]>();
        setIconUrl( TapTableLoadDialog.class.getResource( "tap.gif" ) );
    }

    protected Component createQueryComponent() {

        /* Prepare a panel to search the registry for TAP services. */
        final Component searchPanel = super.createQueryComponent();
        JComponent queryPanel = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                searchPanel.setEnabled( enabled );
                adqlPanel_.setEnabled( enabled );
            }
        };

        /* Prepare a panel to contain user-entered ADQL text. */
        adqlPanel_ = new JEditorPane();
        adqlPanel_.setEditable( true );
        adqlPanel_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateReady();
            }
        } );
        updateReady();
        JComponent adqlScroller = new JScrollPane( adqlPanel_ );
        adqlScroller.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "ADQL Text" ) );

        /* Prepare a panel for table metadata display. */
        tmetaPanel_ = new TableSetPanel();
        tmetaPanel_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Table Metadata" ) );

        /* Put the components together in a window. */
        final JTabbedPane tabber = new JTabbedPane();
        tabber.add( "Search", searchPanel );
        final JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        splitter.setTopComponent( tmetaPanel_ );
        splitter.setBottomComponent( adqlScroller );
        splitter.setResizeWeight( 0.7 );
        tabber.add( "ADQL", splitter );
        final int adqlTabIndex = tabber.getTabCount() - 1;

        /* Provide a button to move to the query tab.
         * Placing it near the service selector makes it more obvious that
         * that is what you need to do after selecting a TAP service. */
        final Action adqlAct = new AbstractAction( "Enter Query" ) {
            public void actionPerformed( ActionEvent evt ) {
                tabber.setSelectedIndex( adqlTabIndex );
            }
        };
        adqlAct.putValue( Action.SHORT_DESCRIPTION,
                          "Go to ADQL tab and enter query text" );
        Box buttLine = Box.createHorizontalBox();
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( adqlAct ) );
        getControlBox().add( buttLine );

        /* Only enable the query tab if a valid service URL has been
         * selected. */
        adqlAct.setEnabled( false );
        tabber.setEnabledAt( adqlTabIndex, false );
        getServiceUrlField().addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                boolean hasUrl;
                try {
                    checkUrl( getServiceUrl() );
                    hasUrl = true;
                }
                catch ( RuntimeException e ) {
                    hasUrl = false;
                }
                tabber.setEnabledAt( adqlTabIndex, hasUrl );
                adqlAct.setEnabled( hasUrl );
            }
        } );

        /* Arrange for the table metadata to get updated when the tab is
         * switched to the ADQL tab. */
        tabber.addChangeListener( new ChangeListener() {
            private boolean shown = false;
            public void stateChanged( ChangeEvent evt ) {
                String selectedTab =
                    tabber.getTitleAt( tabber.getSelectedIndex() );
                if ( "ADQL".equals( selectedTab ) ) {
                    if ( ! shown ) {
                        splitter.setDividerLocation( 0.5 );
                    }
                    updateTableMetadata( getServiceUrl() );
                }
            }
        } );

        /* Prepare a menu to configure the table metadata column display. */
        List<JMenu> menuList =
            new ArrayList<JMenu>( Arrays.asList( super.getMenus() ) );
        JMenu colsMenu = tmetaPanel_.makeColumnDisplayMenu( "Display" );
        colsMenu.setMnemonic( KeyEvent.VK_D );
        menuList.add( colsMenu );
        setMenus( menuList.toArray( new JMenu[ 0 ] ) );
        return tabber;
    }

    public TableLoader createTableLoader() {
        String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        final TapQuery query =
            TapQuery.createAdqlQuery( serviceUrl, getAdql() );
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( query.getQueryMetadata() ) );
        metadata.addAll( Arrays.asList( getResourceMetadata( serviceUrl ) ) );
        final UwsJob job = query.getJob();
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                StarTable st;
                try {
                    st = query.execute( tfact, 4000 );
                }
                catch ( InterruptedException e ) {
                    throw (IOException)
                          new InterruptedIOException( "Interrupted" )
                         .initCause( e );
                }
                st.getParameters().addAll( metadata );
                return Tables.singleTableSequence( st );
            }
            public String getLabel() {
                return query.getSummary();
            }
        };
    }

    public boolean isReady() {
        if ( adqlPanel_ == null ) {
            return false;
        }
        else {
            String adql = getAdql();
            return super.isReady() && adql != null && adql.trim().length() > 0;
        }
    }

    /**
     * Returns the text currently entered in the ADQL text component.
     *
     * @return  adql text supplied by user
     */
    private String getAdql() {
        return adqlPanel_.getText();
    }

    /**
     * Arrange for the table metadata for a given TAP service to
     * get displayed in the table metadata browser.
     *
     * @param  serviceUrl  TAP base service URL
     */
    private void updateTableMetadata( final String serviceUrl ) {

        /* If the metadata for this service is already cached, use the
         * cached copy and install it in the GUI immediately. */ 
        TableMeta[] tmetas = metaMap_.get( serviceUrl );
        if ( tmetas != null ) {
            tmetaPanel_.setTables( tmetas );
        }
        else {

            /* Otherwise prepare the URL where we can find the TableSet
             * document. */
            URL url;
            try {
                url = new URL( serviceUrl );
            }
            catch ( MalformedURLException e ) {
                return;
            }
            final URL turl;
            try {
                turl = new URL( serviceUrl + "/tables" );
            }
            catch ( MalformedURLException e ) {
                throw new AssertionError( e );
            }

            /* Dispatch a request to acquire the table metadata from
             * the service. */
            tmetaPanel_.setTables( new TableMeta[ 0 ] );
            tmetaPanel_.showFetchProgressBar( "Fetching Table Metadata" );
            new Thread( "Table metadata fetcher" ) {
                public void run() {
                    final TableMeta[] tableMetas;
                    try {
                        logger_.info( "Reading table metadata from " + turl );
                        tableMetas = TableSetSaxHandler.readTableSet( turl );
                    }
                    catch ( final Exception e ) {
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                if ( serviceUrl.equals( getServiceUrl() ) ) {
                                    tmetaPanel_.showFetchFailure( turl, e );
                                }
                            }
                        } );
                        return;
                    }

                    /* On success, and assuming that the service URL has
                     * not been replaced by the user, install this information
                     * in the GUI. */
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            metaMap_.put( serviceUrl, tableMetas );
                            if ( serviceUrl.equals( getServiceUrl() ) ) {
                                tmetaPanel_.setTables( tableMetas );
                            }
                        }
                    } );
                }
            }.start();
        }
    }
}
