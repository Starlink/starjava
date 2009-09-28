package uk.ac.starlink.vo;

import java.awt.Component;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Table load dialogue abstract superclass for spatial DAL-like queries.
 *
 * @author   Mark Taylor
 * @since    22 Sep 2009
 */
public abstract class DalTableLoadDialog
        extends RegistryServiceTableLoadDialog {

    private final SkyPositionEntry skyEntry_;
    private final JTextField urlField_;
    private final String name_;
    private final boolean autoQuery_;
    private boolean setup_;

    /**
     * Constructor.
     *
     * @param  name  dialogue name
     * @param  description  dialogue description
     * @param  queryFactory  source of registry query definition
     * @param  showCapabilities  true to display the capabilities JTable as
     *         well as the Resource one; sensible if resource:capabilities
     *         relationship may not be 1:1
     * @param  autoQuery  populate service table with full registry query
     *         on initial display
     */
    protected DalTableLoadDialog( String name, String description,
                                  RegistryQueryFactory queryFactory,
                                  boolean showCapabilities,
                                  boolean autoQuery ) {
        super( name, description, queryFactory, showCapabilities );
        name_ = name;
        autoQuery_ = autoQuery;

        /* Add a field for holding the service URL.  This will typically be
         * populated by selecting an entry from the result of the displayed
         * registry search, but it may alternatively be filled by the
         * user typing (or cut'n'pasting) into it. */
        urlField_ = new JTextField();
        JComponent urlBox = Box.createHorizontalBox();
        urlBox.add( new JLabel( name + " URL: " ) );
        urlBox.add( urlField_ );
        getControlBox().add( urlBox );
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
        final Action okAction = getOkAction();
        okAction.setEnabled( false );
        urlField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                boolean hasUrl = false;
                String txt = urlField_.getText();
                if ( txt != null && txt.trim().length() > 0 ) {
                    try {
                        new URL( urlField_.getText() );
                        hasUrl = true;
                    }
                    catch ( MalformedURLException e ) {
                    }
                }
                okAction.setEnabled( hasUrl );
            }
        } );

        /* Add a spatial position selector component. */
        skyEntry_ = new SkyPositionEntry( "J2000" );
        skyEntry_.addActionListener( getOkAction() );
        getControlBox().add( skyEntry_ );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        urlField_.setEnabled( enabled );
        skyEntry_.setEnabled( enabled );
    }

    public JDialog createDialog( Component parent ) {
        if ( ! setup_ ) {
            setup_ = true;
            if ( autoQuery_ ) {
                getRegistryPanel().performAutoQuery( "Locating all known "
                                                   + name_ + " services" );
            }
        }
        return super.createDialog( parent );
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
     * Performs syntactic checks on a string which should be a URL, and
     * throws an informative RuntimeException if it is not.
     *
     * @param  url   string to test for URL-ness
     */
    public void checkUrl( String url ) {
        if ( url == null || url.trim().length() == 0 ) {
            throw new IllegalArgumentException( "No " + name_
                                              + " service selected" );
        }
        else {
            try {
                new URL( url );
            }
            catch ( MalformedURLException e ) {
                throw new IllegalArgumentException( "Bad " + name_
                                                  + " service URL" );
            }
        }
    }

    /**
     * Returns the SkyPositionEntry component used by this dialog.
     *
     * @return  sky position entry
     */
    public SkyPositionEntry getSkyEntry() {
        return skyEntry_;
    }

    /**
     * Returns a short string summarising the current query.
     *
     * @param  serviceUrl  service URL for the query to be labelled
     *         - may or may not be that of the currently selected
     *         resource/capability
     * @param  sizeDeg   size in degrees of the spatial query to be labelled
     * @return  query label
     */
    public String getQuerySummary( String serviceUrl, double sizeDeg ) {

        /* Get the name of the astronomical object being searched around,
         * if any. */
        String objName = getSkyEntry().getResolveField().getText();

        /* Get the short name for the registry resource providing the query
         * capability, if there is one. */
        RegistryPanel regPanel = getRegistryPanel();
        RegResource[] resources = regPanel.getSelectedResources();
        RegCapabilityInterface[] caps = regPanel.getSelectedCapabilities();
        String shortName = null;
        if ( resources.length == 1 && caps.length == 1 &&
             serviceUrl.equals( caps[ 0 ].getAccessUrl() ) ) {
            shortName = resources[ 0 ].getShortName();
            if ( shortName != null ) {
                shortName = shortName.replace( '/', '_' );
            }
        }
        if ( shortName == null || shortName.trim().length() == 0 ) {
            shortName = name_.replaceFirst( " .*", "" );
        }

        /* Get a short string summarising the query spatial extent. */
        String size;
        if ( sizeDeg > 0 ) {
            if ( sizeDeg > 1 ) {
                size = ((int) sizeDeg) + "d";
            }
            else if ( sizeDeg * 60 >= 1 ) {
                size = ((int) (sizeDeg * 60)) + "m";
            }
            else {
                size = ((int) (sizeDeg * 60 * 60)) + "s";
            }
        }
        else {
            size = null;
        }

        /* Combine and return the known information. */
        StringBuffer sbuf = new StringBuffer();
        if ( objName != null && objName.trim().length() > 0 ) {
            sbuf.append( objName )
                .append( '-' );
        }
        sbuf.append( shortName );
        if ( size != null ) {
            sbuf.append( '-' );
            sbuf.append( size );
        }
        return sbuf.toString();
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
     * object representing a cone search.
     *
     * @param   resource   cone search resource
     * @param   cap   cone search capability interface
     */
    public DescribedValue[] getMetadata( RegResource resource,
                                         RegCapabilityInterface cap ) {
        List metadata = new ArrayList();
        addMetadatum( metadata, resource.getShortName(),
                      "Service short name",
                      "Short name for " + name_ + " service" );
        addMetadatum( metadata, resource.getTitle(),
                      "Service title",
                      name_ + " service title" );
        addMetadatum( metadata, resource.getIdentifier(),
                      "Identifier",
                      "Unique resource registry identifier" );
        addMetadatum( metadata, resource.getPublisher(),
                      "Service publisher",
                      "Publisher for " + name_ + " service" );
        addMetadatum( metadata, resource.getReferenceUrl(),
                      "Service reference URL",
                      "Descriptive URL for search resource" );
        addMetadatum( metadata, resource.getContact(),
                      "Contact person",
                      "Individual to contact about this service" );
        return (DescribedValue[]) metadata.toArray( new DescribedValue[ 0 ] );
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
    private static void addMetadatum( List metadata, String value, String name,
                                      String description ) {
        if ( value != null && value.trim().length() > 0 ) {
            ValueInfo info = new DefaultValueInfo( name, String.class,
                                                   description );
            metadata.add( new DescribedValue( info, value ) );
        }
    }
}
