package uk.ac.starlink.hapi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ComboBoxBumper;

/**
 * GUI component that displays metadata from available HAPI services.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class HapiBrowser extends JPanel {

    private final Supplier<ContentCoding> codingSupplier_;
    private final Consumer<URL> docUrlHandler_;
    private final JComboBox<String> nameSelector_;
    private final JTextField urlField_;
    private final SpinnerNumberModel chunklimitModel_;
    private final JComponent serviceBox_;
    private final Map<HapiService,ServicePanel> serviceMap_;
    private final PropertyChangeListener pcForwarder_;
    private HapiService service_;
    private String format_;
    private boolean includeHeader_;
    private ServerMeta[] servers_;

    /** Bean property that is updated when GUI state may have changed URL. */
    public static final String HAPISOURCE_PROP = "hapiSource";

    /** Maximum chunk count - set it too high and risk annoying services. */
    private static final int MAX_CHUNKLIMIT = 50;

    /**
     * Constructor.
     */
    public HapiBrowser() {
        this( null, null );
    }

    /**
     * Constructor with configurable content coding.
     *
     * @param  codingSupplier  supplier for content-coding,
     *                         or null for default
     * @param  docUrlHandler  handler for documentation URLs,
     *                        typically displays in a browser;
     *                        may be null
     */
    public HapiBrowser( Supplier<ContentCoding> codingSupplier,
                        Consumer<URL> docUrlHandler ) {
        super( new BorderLayout() );
        codingSupplier_ = codingSupplier;
        docUrlHandler_ = docUrlHandler;
        servers_ = new ServerMeta[ 0 ];
        serviceMap_ = new HashMap<HapiService,ServicePanel>();
        serviceBox_ = new JPanel( new BorderLayout() );

        urlField_ = new JTextField();
        urlField_.addActionListener( evt -> {
            setServerUrl( urlField_.getText() );
        } );

        nameSelector_ = new JComboBox<String>();
        nameSelector_.addItemListener( evt -> {
            int isel = nameSelector_.getSelectedIndex();
            if ( isel >= 0 ) {
                String name = nameSelector_.getItemAt( isel );
                for ( ServerMeta server : servers_ ) {
                    if ( name.equals( server.getName() ) ) {
                        String url = server.getUrl();
                        urlField_.setText( url );
                        setServerUrl( url );
                    }
                }
            }
        } );

        chunklimitModel_ = new SpinnerNumberModel( 1, 1, MAX_CHUNKLIMIT, 1 );

        pcForwarder_ = new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                String propName = evt.getPropertyName();
                if ( HAPISOURCE_PROP.equals( propName ) ) {
                    HapiBrowser.this.firePropertyChange( propName,
                                                         evt.getOldValue(),
                                                         evt.getNewValue() );
                }
            }
        };

        setServerUrl( null );

        LabelledComponentStack stack = new LabelledComponentStack();
        Box selectorBox = Box.createHorizontalBox();
        selectorBox.add( nameSelector_ );
        selectorBox.add( Box.createHorizontalStrut( 5 ) );
        selectorBox.add( new ComboBoxBumper( nameSelector_ ) );
        stack.addLine( "HAPI Server", selectorBox );
        stack.addLine( "HAPI URL", urlField_ );
        stack.addLine( "Chunk Limit", new JSpinner( chunklimitModel_ ) );
        stack.setBorder( createTitledBorder( "Service Selection" ) );
        add( stack, BorderLayout.NORTH );
        add( serviceBox_, BorderLayout.CENTER ); 
        setPreferredSize( new Dimension( 600, 560 ) );
    }

    /**
     * Asynchronously populates the list of servers.
     */
    public void init() {
        new Thread( () -> {
            ServerMeta[] servers = ServerMeta.getServers();
            SwingUtilities.invokeLater( () -> {
                setServers( servers );
            } );
        }, "HAPI default service list loader" ).start();
    }

    /**
     * Sets the list of servers.
     *
     * @param  servers  server list
     */
    public void setServers( ServerMeta[] servers ) {
        servers_ = servers;
        nameSelector_.removeAllItems();
        for ( ServerMeta server : servers ) {
            nameSelector_.addItem( server.getName() );
        }
        if ( nameSelector_.getSelectedItem() == null &&
             nameSelector_.getItemCount() > 0 ) {
            nameSelector_.setSelectedItem( nameSelector_.getItemAt( 0 ) );
        }
    }

    /**
     * Explicitly sets the data format to request.
     * If null, the format is automatically chosen.
     *
     * @param  format  HAPI data format, one of "csv" or "binary"
     */
    public void setFormat( String format ) {
        format_ = format;
    }

    /**
     * Sets whether the metadata should be requested and interpreted
     * with the data or whether it should be reused from the initial
     * metadata request.
     *
     * @param  includeHeader  true to include header with data requests
     */
    public void setIncludeHeader( boolean includeHeader ) {
        includeHeader_ = includeHeader;
    }

    /**
     * Returns a HapiSource that will acquire a table
     * corresponding to the the current state of the GUI.
     *
     * @return  hapiSource or null
     */
    public HapiSource getHapiSource() {
        ServicePanel servicePanel = getServicePanel();
        return servicePanel == null ? null : servicePanel.getHapiSource();
    }

    /**
     * Returns the currently selected service.
     *
     * @return  service
     */
    public HapiService getService() {
        return service_;
    }

    /**
     * Returns the maximum number of chunks into which a request will be split
     * to return the data.  If more chunks than this value are required,
     *
     * @return   chunk limit specified in GUI
     */
    public int getChunkLimit() {
        return chunklimitModel_.getNumber().intValue();
    }

    /**
     * Returns the currently visible service panel.
     *
     * @return  service panel, may be null
     */
    public ServicePanel getServicePanel() {
        return serviceMap_.get( service_ );
    }

    /**
     * Updates the currently selected service.
     *
     * @param  serverUrl  base URL of HAPI service
     */
    private void setServerUrl( String serverUrl ) {
        HapiService service = null;
        if ( serverUrl == null ) {
            service = null;
        }
        else {
            try {
                service = new HapiService( serverUrl, codingSupplier_ );
            }
            catch ( MalformedURLException e ) {
                service = null;
            }
        }
        if ( ! Objects.equals( service, service_ ) ) {
            ServicePanel oldServicePanel = serviceMap_.get( service_ );
            HapiSource oldHapiSrc = getHapiSource();
            if ( oldServicePanel != null ) {
                oldServicePanel.removePropertyChangeListener( pcForwarder_ );
            }
            service_ = service;
            final HapiService service0 = service;
            serviceMap_.computeIfAbsent( service, u -> {
                ServicePanel sp = new ServicePanel( service0, docUrlHandler_ );
                sp.setFormatSupplier( () -> format_ );
                sp.setHeaderInclusion( () -> includeHeader_ );
                return sp;
            } );
            ServicePanel servicePanel = serviceMap_.get( service );
            HapiSource hapiSrc = getHapiSource();
            if ( servicePanel != null ) {
                if ( oldServicePanel != null ) {
                    servicePanel.getDateRangePanel()
                                .configureFromTemplate( oldServicePanel
                                                       .getDateRangePanel() );
                }
                servicePanel.addPropertyChangeListener( pcForwarder_ );
            }
            serviceBox_.removeAll();
            serviceBox_.add( servicePanel, BorderLayout.CENTER );
            serviceBox_.revalidate();
            serviceBox_.repaint();
            if ( ! Objects.equals( oldHapiSrc, hapiSrc ) ) {
                firePropertyChange( HAPISOURCE_PROP, oldHapiSrc, hapiSrc );
            }
        }
    }

    /**
     * Creates a standard border with a title.
     *
     * @param  txt  title text
     * @return  border
     */
    static Border createTitledBorder( String txt ) {
        return BorderFactory
              .createTitledBorder( BorderFactory
                                  .createLineBorder( Color.BLACK ),
                                   txt );
    }

    /**
     * Posts an instance of this browser.
     */
    public static void main( String[] args ) {
        HapiBrowser browser = new HapiBrowser();
        JFrame frame = new JFrame();
        frame.getContentPane().add( browser );
        frame.pack();
        frame.setVisible( true );
        browser.init();
    }
}
