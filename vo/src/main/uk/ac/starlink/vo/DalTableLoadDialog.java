package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;

/**
 * Table load dialogue abstract superclass for registry-based DAL-like queries.
 *
 * @author   Mark Taylor
 * @since    22 Sep 2009
 */
public abstract class DalTableLoadDialog
        extends RegistryServiceTableLoadDialog {

    private final String protoName_;
    private final Capability capability_;
    private final boolean autoQuery_;
    private final RegistryQueryFactory queryFactory_;
    private JTextField urlField_;
    private JComponent urlBox_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  protoName   short name (perhaps acronym) for protocol
     * @param  description  dialogue description
     * @param  capability   service capability type
     * @param  showCapabilities  true to display the capabilities JTable as
     *         well as the Resource one; sensible if resource:capabilities
     *         relationship may not be 1:1
     * @param  autoQuery  populate service table with full registry query
     *         on initial display
     */
    @SuppressWarnings("this-escape")
    protected DalTableLoadDialog( String name, String protoName,
                                  String description, Capability capability,
                                  boolean showCapabilities,
                                  boolean autoQuery ) {
        super( name, protoName, description,
               new KeywordServiceQueryFactory( capability ), showCapabilities );
        protoName_ = protoName;
        capability_ = capability;
        autoQuery_ = autoQuery;
        queryFactory_ = (KeywordServiceQueryFactory) getQueryFactory();
    }

    protected Component createQueryComponent() {
        JPanel queryPanel = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                urlField_.setEnabled( enabled );
            }
        };
        queryPanel.add( super.createQueryComponent(), BorderLayout.CENTER );

        /* Add a field for holding the service URL.  This will typically be
         * populated by selecting an entry from the result of the displayed
         * registry search, but it may alternatively be filled by the
         * user typing (or cut'n'pasting) into it. */
        urlField_ = new JTextField();
        urlBox_ = Box.createHorizontalBox();
        urlBox_.add( new JLabel( protoName_ + " URL: " ) );
        urlBox_.add( urlField_ );
        getControlBox().add( urlBox_ );
        getControlBox().add( Box.createVerticalStrut( 5 ) );

        /* Fix it so that a resource selection populates the service
         * URL field. */
        final RegistryPanel regPanel = getRegistryPanel();
        ListSelectionListener selListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegCapabilityInterface[] caps =
                    regPanel.getSelectedCapabilities();
                urlField_.setText( caps.length == 1 ? caps[ 0 ].getAccessUrl()
                                                    : null );
                urlField_.setCaretPosition( 0 );
            }
        };
        regPanel.getResourceSelectionModel()
                .addListSelectionListener( selListener );
        regPanel.getCapabilitySelectionModel()
                .addListSelectionListener( selListener );

        /* Only enable the query submission when the service URL field
         * contains a syntactically valid URL. */
        urlField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateReady();
            }
        } );
        updateReady();

        /* Fix it so that the submit action is activated on double clicks
         * and so on. */
        regPanel.addActionListener( getSubmitAction() );

        /* Menus. */
        List<JMenu> menuList =
            new ArrayList<JMenu>( Arrays.asList( getMenus() ) );
        JMenu regMenu = new JMenu( "Registry" );
        regMenu.setMnemonic( KeyEvent.VK_R );
        for ( JMenuItem item : Arrays.asList( getRegistryMenuItems() ) ) {
            regMenu.add( item );
        }
        menuList.add( regMenu );
        setMenus( menuList.toArray( new JMenu[ 0 ] ) );

        /* Either initiate an automatic query, or provide some user-visible
         * advice about what to do now. */
        if ( autoQuery_ ) {
            regPanel.performAutoQuery( "Searching registry for all known "
                                     + protoName_ + " services" );
        }
        else {
            regPanel.displayAdviceMessage( new String[] {
                "Query registry for " + protoName_ + " services:",
                "enter keywords like \"2mass qso\" and click "
                + getRegistryPanel().getSubmitQueryAction()
                                    .getValue( Action.NAME )
                + ".",
                " ",
                "Alternatively, enter " + protoName_ + " URL in field below.",
            } );
        }
        return queryPanel;
    }

    public boolean isReady() {
        String txt = urlField_.getText();
        if ( txt == null || txt.trim().length() == 0 ) {
            return false;
        }
        else {
            try {
                new URL( txt );
                return true;
            }
            catch ( MalformedURLException e ) {
                return false;
            }
        }
    }

    /**
     * Returns the current contents of the service URL field.
     *
     * @return  currently filled in query service URL, if any
     */
    public String getServiceUrl() {
        return urlField_.getText();
    }

    /**
     * Sets the current contents of the service URL field.
     *
     * @param url  value to insert into service URL field
     */
    public void setServiceUrl( String url ) {
        urlField_.setText( url );
    }

    /**
     * Returns the text field into which the user can enter the service URL.
     *
     * @return  url field
     */
    public JTextField getServiceUrlField() {
        return urlField_;
    }

    /**
     * Returns the component in which the URL selector is located.
     *
     * @return  URL selector container
     */
    public JComponent getServiceUrlBox() {
        return urlBox_;
    }

    /**
     * Performs syntactic checks on a string which should be a URL, and
     * throws an informative RuntimeException if it is not.
     *
     * @param  url   string to test for URL-ness
     * @return  the url as URL if correct, never null
     */
    public URL checkUrl( String url ) {
        if ( url == null || url.trim().length() == 0 ) {
            throw new IllegalArgumentException( "No " + protoName_
                                              + " service selected" );
        }
        else {
            try {
                return new URL( url );
            }
            catch ( MalformedURLException e ) {
                throw new IllegalArgumentException( "Bad " + protoName_
                                                  + " service URL" );
            }
        }
    }

    /**
     * Returns an array of metadata items describing the resource being queried.
     *
     * @param  serviceUrl  service URL of query
     *         - may or may not be that of the currently selected
     *         resource/capability
     * @return  metadata array
     */
    public DescribedValue[] getResourceMetadata( String serviceUrl ) {
        RegistryPanel regPanel = getRegistryPanel();
        RegResource[] resources = regPanel.getSelectedResources();
        RegCapabilityInterface[] caps = regPanel.getSelectedCapabilities();
        return ( resources.length == 1 && caps.length == 1 &&
                 serviceUrl.equals( caps[ 0 ].getAccessUrl() ) )
             ? getMetadata( resources[ 0 ], caps[ 0 ] )
             : new DescribedValue[ 0 ];
    }

    /**
     * Returns a list of described values for the resource
     * object representing a DAL service.
     *
     * @param   resource   DAL resource
     * @param   cap   DAL capability interface
     */
    public DescribedValue[] getMetadata( RegResource resource,
                                         RegCapabilityInterface cap ) {
        List<DescribedValue> metadata = new ArrayList<DescribedValue>();
        addMetadatum( metadata, resource.getShortName(),
                      "Service short name",
                      "Short name for " + protoName_ + " service" );
        addMetadatum( metadata, resource.getTitle(),
                      "Service title",
                      protoName_ + " service title" );
        addMetadatum( metadata, resource.getIdentifier(),
                      "Identifier",
                      "Unique resource registry identifier" );
        addMetadatum( metadata, resource.getPublisher(),
                      "Service publisher",
                      "Publisher for " + protoName_ + " service" );
        addMetadatum( metadata, resource.getReferenceUrl(),
                      "Service reference URL",
                      "Descriptive URL for search resource" );
        addMetadatum( metadata, resource.getContact(),
                      "Contact person",
                      "Individual to contact about this service" );
        return metadata.toArray( new DescribedValue[ 0 ] );
    }

    public RegCapabilityInterface[] getCapabilities( RegResource resource ) {
        RegistryProtocol regProto =
            getQueryFactory().getRegistrySelector().getModel().getProtocol();
        RegCapabilityInterface[] caps = super.getCapabilities( resource );
        List<RegCapabilityInterface> capList =
            new ArrayList<RegCapabilityInterface>();
        for ( int i = 0; i < caps.length; i++ ) {
            if ( regProto.hasCapability( capability_, caps[ i ] ) ) {
                capList.add( caps[ i ] );
            }
        }
        return capList.toArray( new RegCapabilityInterface[ 0 ] );
    }

    /**
     * Adds a DescribedValue to a list of them, based on given values and
     * characteristics.  If the given value is blank, it is not added.
     *
     * @param  metadata  list of DescribedValue objects
     * @param  value     the value of the object to add
     * @param  name      the name of the object
     * @param  description  the description of the object
     */
    private static void addMetadatum( List<DescribedValue> metadata,
                                      String value, String name,
                                      String description ) {
        if ( value != null && value.trim().length() > 0 ) {
            ValueInfo info = new DefaultValueInfo( name, String.class,
                                                   description );
            metadata.add( new DescribedValue( info, value ) );
        }
    }
}
