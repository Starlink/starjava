package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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

    private final Map<String,TapQueryPanel> tqMap_;
    private JTabbedPane tabber_;
    private JComponent tqContainer_;
    private TapQueryPanel tqPanel_;
    private CaretListener adqlListener_;

    /**
     * Constructor.
     */
    public TapTableLoadDialog() {
        super( "TAP", "Query remote databases using SQL-like language",
               Capability.TAP, false, false );
        tqMap_ = new HashMap<String,TapQueryPanel>();
        setIconUrl( TapTableLoadDialog.class.getResource( "tap.gif" ) );
    }

    protected Component createQueryComponent() {

        /* Prepare a panel to search the registry for TAP services. */
        final Component searchPanel = super.createQueryComponent();

        /* Prepare a tabbed panel to contain the components. */
        tabber_ = new JTabbedPane();
        tabber_.add( "Select Service", searchPanel );
        tqContainer_ = new JPanel( new BorderLayout() );
        String tqTitle = "Enter Query";
        tabber_.add( tqTitle, tqContainer_ );
        final int tqTabIndex = tabber_.getTabCount() - 1;

        /* Provide a button to move to the query tab.
         * Placing it near the service selector makes it more obvious that
         * that is what you need to do after selecting a TAP service. */
        final Action tqAct = new AbstractAction( tqTitle ) {
            public void actionPerformed( ActionEvent evt ) {
                tabber_.setSelectedIndex( tqTabIndex );
            }
        };
        tqAct.putValue( Action.SHORT_DESCRIPTION,
                        "Go to " + tqTitle
                      + " tab to prepare and execute TAP query" );
        Box buttLine = Box.createHorizontalBox();
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( tqAct ) );
        getControlBox().add( buttLine );

        /* Only enable the query tab if a valid service URL has been
         * selected. */
        tqAct.setEnabled( false );
        tabber_.setEnabledAt( tqTabIndex, false );
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
                tabber_.setEnabledAt( tqTabIndex, hasUrl );
                tqAct.setEnabled( hasUrl );
            }
        } );

        /* Arrange for the table query panel to get updated when it becomes
         * the visible tab. */
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                if ( tabber_.getSelectedIndex() == tqTabIndex ) {
                    setSelectedService( getServiceUrl() );
                }
                updateReady();
            }
        } );

        /* Arrange that the TAP query submit action's enabledness status
         * can be sensitive to the content of the ADQL entry field. */
        adqlListener_ = new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateReady();
            }
        };

        /* Return the tabbed pane which is the main query component. */
        return tabber_;
    }

    public TableLoader createTableLoader() {
        final URL serviceUrl = checkUrl( getServiceUrl() );
        final String adql = tqPanel_.getAdql();
        final String summary = TapQuery.summarizeAdqlQuery( serviceUrl, adql );
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                TapQuery query = TapQuery.createAdqlQuery( serviceUrl, adql );
                StarTable st;
                try {
                    st = query.execute( tfact, 4000, false );
                }
                catch ( InterruptedException e ) {
                    throw (IOException)
                          new InterruptedIOException( "Interrupted" )
                         .initCause( e );
                }
                List meta = st.getParameters();
                meta.addAll( Arrays.asList( query.getQueryMetadata() ) );
                meta.addAll( Arrays
                            .asList( getResourceMetadata( serviceUrl
                                                         .toString() ) ) );
                return Tables.singleTableSequence( st );
            }
            public String getLabel() {
                return summary;
            }
        };
    }

    public boolean isReady() {
        if ( tqPanel_ == null ||
             tabber_.getSelectedComponent() != tqContainer_ ) {
            return false;
        }
        else {
            String adql = tqPanel_.getAdql();
            return super.isReady() && adql != null && adql.trim().length() > 0;
        }
    }

    /**
     * Configure this dialogue to use a TAP service with a given service URL.
     *
     * @param  serviceUrl  service URL for TAP service
     */
    private void setSelectedService( String serviceUrl ) {

        /* We have to install a TapQueryPanel for this service in the 
         * appropriate tab of the tabbed pane.
         * First remove any previously installed query panel. */
        if ( tqPanel_ != null ) {
            tqContainer_.remove( tqPanel_ );
            tqPanel_.getAdqlPanel().removeCaretListener( adqlListener_ );
            tqPanel_ = null;
        }
        if ( serviceUrl != null ) {

            /* Construct, configure and cache a suitable query panel
             * if we haven't seen this service URL before now. */
            if ( ! tqMap_.containsKey( serviceUrl ) ) {
                TapQueryPanel tqPanel = new TapQueryPanel();
                tqPanel.setServiceHeading( getServiceHeading( serviceUrl ) );
                tqPanel.setServiceUrl( serviceUrl );
                tqMap_.put( serviceUrl, tqPanel );
            }

            /* Get the panel from the cache, now guaranteed present. */
            tqPanel_ = tqMap_.get( serviceUrl );

            /* Install ready for use. */
            tqPanel_.getAdqlPanel().addCaretListener( adqlListener_ );
            tqContainer_.add( tqPanel_, BorderLayout.CENTER );
        }
        updateReady();
    }

    /**
     * Returns a line of text describing the given service URL.
     * This is intended to be as human-readable as possible, and will be
     * taken from the currently selected resource if it appears to be
     * appropriate for the given URL.
     *
     * @param  serviceUrl  service URL of TAP service to find a heading for
     * @return  human-readable description of service
     */
    private String getServiceHeading( String serviceUrl ) {
        if ( serviceUrl == null && serviceUrl.trim().length() == 0 ) {
            return "";
        }
        RegistryPanel regPanel = getRegistryPanel();
        RegResource[] resources = regPanel.getSelectedResources();
        RegCapabilityInterface[] caps = regPanel.getSelectedCapabilities();
        if ( caps.length == 1 && resources.length == 1 ) {
            String acref = caps[ 0 ].getAccessUrl();
            if ( serviceUrl.equals( caps[ 0 ].getAccessUrl() ) ) {
                String heading = getResourceHeading( resources[ 0 ] );
                if ( heading != null && heading.trim().length() > 0 ) {
                    return heading;
                }
            }
        }
        return serviceUrl;
    }

    /**
     * Returns a line of text describing the given registry resource.
     * This is intended to be as human-readable as possible.
     * If the resource contains no appropriate fields however,
     * null may be returned.
     *
     * @param  resource  resourse to describe
     * @return  human-readable description of resource, or null
     */
    private static String getResourceHeading( RegResource resource ) {
        String title = resource.getTitle();
        if ( title != null && title.trim().length() > 0 ) {
            return title;
        }
        String shortName = resource.getShortName();
        if ( shortName != null && shortName.trim().length() > 0 ) {
            return shortName;
        }
        String ident = resource.getIdentifier();
        if ( ident != null && ident.trim().length() > 0 ) {
            return ident;
        }
        return null;
    }
}
