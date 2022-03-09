package uk.ac.starlink.topcat.interop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.gui.UniformCallActionManager;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.MessageTrackerHubConnector;
import org.astrogrid.samp.gui.SendActionManager;
import org.astrogrid.samp.gui.SysTray;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.SubsetWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.plot.DensityWindow;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryPanel;

/**
 * TopcatCommunicator implementation based on SAMP.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2008
 */
public class SampCommunicator implements TopcatCommunicator {

    private final ClientProfile clientProfile_;
    private final GuiHubConnector hubConnector_;
    private final TopcatSampControl sampControl_;
    private final Transmitter tableTransmitter_;
    private final Action stopHubAct_;
    private Hub internalHub_;
    private int imageCount_;
    private static final Logger logger_ =
        Logger.getLogger( SampCommunicator.class.getName() );

    /**
     * Constructor.
     *
     * @param   controlWindow   TOPCAT control window
     */
    public SampCommunicator( ControlWindow controlWindow ) throws IOException {
        clientProfile_ = TopcatServer.getInstance().getProfile();
        hubConnector_ = new MessageTrackerHubConnector( clientProfile_ );
        sampControl_ = new TopcatSampControl( hubConnector_, controlWindow );
        tableTransmitter_ =
            new TableSendActionManager( hubConnector_, sampControl_ );
        stopHubAct_ = new AbstractAction( "Stop Internal Hub",
                                          ResourceIcon.NO_HUB ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( internalHub_ != null ) {
                    final Hub condemnedHub = internalHub_;
                    internalHub_ = null;
                    stopHubAct_.setEnabled( internalHub_ != null );
                    Thread shutter = new Thread( "Hub shutdown" ) {
                        public void run() {
                            condemnedHub.shutdown();
                        }
                    };
                    shutter.setDaemon( true );
                    shutter.start();
                }
            }
        };
        stopHubAct_.putValue( Action.SHORT_DESCRIPTION,
                              "Shuts down the internal SAMP hub running "
                            + "within TOPCAT" );
        stopHubAct_.setEnabled( internalHub_ != null );
    }

    public String getProtocolName() {
        return "SAMP";
    }

    public boolean setActive() {
        hubConnector_.setActive( true );
        hubConnector_.setAutoconnect( 5 );
        try {
            return hubConnector_.getConnection() != null;
        }
        catch ( IOException e ) {
            logger_.warning( "SAMP connection attempt failed: " + e );
            return false;
        }
    }

    public Transmitter getTableTransmitter() {
        return tableTransmitter_;
    }

    public Transmitter createImageTransmitter( DensityWindow densWin ) {
        return adaptTransmitter( new DensityImageSendActionManager( densWin ) );
    }

    public Transmitter createSubsetTransmitter( TopcatModel tcModel,
                                                SubsetWindow subWin ) {
        return adaptTransmitter( new SubsetSendActionManager( tcModel,
                                                              subWin ) );
    }

    public Transmitter createResourceListTransmitter( RegistryPanel regPanel,
                                                      String resourceType ) {
        String mtype = ( resourceType == null || resourceType.length() == 0 )
                     ? "voresource.loadlist"
                     : "voresource.loadlist." + resourceType;
        return adaptTransmitter( new ResourceListSendActionManager( regPanel,
                                                                    mtype ) );
    }

    public SkyPointActivity createSkyPointActivity() {
        final SendManager pointSender =
            new SendManager( hubConnector_, "coord.pointAt.sky" );
        return new SkyPointActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return pointSender.getComboBoxModel();
            }
            public void pointAtSky( double ra, double dec ) throws IOException {
                Message msg = new Message( "coord.pointAt.sky" );
                msg.addParam( "ra", SampUtils.encodeFloat( ra ) );
                msg.addParam( "dec", SampUtils.encodeFloat( dec ) );
                pointSender.notify( msg );
            }
        };
    }

    public RowActivity createRowActivity() {
        final SendManager rowSender =
            new SendManager( hubConnector_, "table.highlight.row" );
        return new RowActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return rowSender.getComboBoxModel();
            }
            public void highlightRow( TopcatModel tcModel, long lrow )
                    throws IOException {
                Map<?,?> msg = sampControl_.createRowMessage( tcModel, lrow );
                if ( msg != null ) {
                    rowSender.notify( msg );
                }
            }
        };
    }

    public SubsetActivity createSubsetActivity() {
        final SendManager subsetSender =
            new SendManager( hubConnector_, "table.select.rowList" );
        return new SubsetActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return subsetSender.getComboBoxModel();
            }
            public void selectSubset( TopcatModel tcModel, RowSubset rset )
                    throws IOException {
                Map<?,?> msg =
                    sampControl_.createSubsetMessage( tcModel, rset );
                if ( msg != null ) {
                    subsetSender.call( msg );
                }
            }
        };
    }

    public SpectrumActivity createSpectrumActivity() {
        final SendManager spectrumSender =
            new SendManager( hubConnector_, "spectrum.load.ssa-generic" );
        return new SpectrumActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return spectrumSender.getComboBoxModel();
            }
            public void displaySpectrum( String location, Map<?,?> metadata )
                    throws IOException {
                Message msg = new Message( "spectrum.load.ssa-generic" );
                msg.addParam( "url", location );
                msg.addParam( "meta", sanitizeMap( metadata ) );
                spectrumSender.call( msg );
            }
        };
    }

    public ImageActivity createImageActivity() {
        return new SampImageActivity( hubConnector_ );
    }

    public Action[] getInteropActions() {
        return new Action[] {
            stopHubAct_,
        };
    }

    public void startHub( boolean external ) throws IOException {
        if ( external ) {
            Hub.runExternalHub( HubServiceMode.MESSAGE_GUI );
        }
        else {
            internalHub_ = Hub.runHub( SysTray.getInstance().isSupported()
                                     ? HubServiceMode.CLIENT_GUI
                                     : HubServiceMode.NO_GUI );
            stopHubAct_.setEnabled( internalHub_ != null );
        }
    }

    public void maybeStartHub() throws IOException {
        if ( ! clientProfile_.isHubRunning() ) {
            logger_.info( "No SAMP hub running; attempting to start one" );
            startHub( false );
        }
    }

    public Action createWindowAction( final Component parent ) {
        return new BasicAction( "SAMP Status", ResourceIcon.SAMP,
                                "Show window displaying SAMP inter-tool "
                              + "messaging status and configuration" ) {
            private SampWindow sampWindow_;
            public void actionPerformed( ActionEvent evt ) {
                if ( sampWindow_ == null ) {
                    sampWindow_ = new SampWindow( parent, hubConnector_ );
                }
                sampWindow_.makeVisible();
            }
        };
    }

    public boolean isConnected() {
        return hubConnector_.isConnected();
    }

    public void addConnectionListener( ChangeListener listener ) {
        hubConnector_.addConnectionListener( listener );
    }

    /**
     * Returns the SAMP HubConnector used by this object.
     *
     * @return   connector
     */
    public GuiHubConnector getConnector() {
        return hubConnector_;
    }

    /**
     * Returns the TopcatSampControl object used by this object.
     *
     * @return samp control
     */
    public TopcatSampControl getSampControl() {
        return sampControl_;
    }

    /**
     * Turns a SAMP action manager into a Transmitter.
     *
     * @param  sender  SAMP sender
     * @return  Transmitter facade
     */
    private static Transmitter
            adaptTransmitter( final UniformCallActionManager sender ) {
        return new Transmitter() {
            public Action getBroadcastAction() {
                return sender.getBroadcastAction();
            }
            public JMenu createSendMenu() {
                return sender.createSendMenu();
            }
            public void setEnabled( boolean isEnabled ) {
                sender.setEnabled( isEnabled );
            }
        };
    }

    /**
     * SendActionManager for sending subsets as row selections from the 
     * subset window.
     */
    private class SubsetSendActionManager extends UniformCallActionManager {
        private final TopcatModel tcModel_;
        private final SubsetWindow subWin_;

        /**
         * Constructor.
         *
         * @param   tcModel  table
         * @param   subWin   subset window
         */
        SubsetSendActionManager( TopcatModel tcModel, SubsetWindow subWin ) {
            super( subWin, hubConnector_, "table.select.rowList",
                   "Row Subset" );
            tcModel_ = tcModel;
            subWin_ = subWin;
        }

        protected Map<?,?> createMessage() throws IOException {
            RowSubset rset = subWin_.getSelectedSubset();
            return sampControl_.createSubsetMessage( tcModel_, rset );
        }
    }

    /**
     * SendActionManager for sending FITS images from the density plot window.
     */
    private class DensityImageSendActionManager
                  extends UniformCallActionManager {
        private final DensityWindow densWin_;

        /**
         * Constructor.
         *
         * @param   densWin  density plot window
         */
        DensityImageSendActionManager( DensityWindow densWin ) {
            super( densWin, hubConnector_, "image.load.fits", "FITS Image" );
            densWin_ = densWin;
        }

        protected Map<?,?> createMessage() throws IOException {

            /* Write the FITS image to a byte array. */
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            BufferedOutputStream bufout = new BufferedOutputStream( bout );
            densWin_.exportFits( bufout );
            bufout.flush();
            final byte[] fitsdata = bout.toByteArray();
            ServerResource resource = new ServerResource() {
                public String getContentType() {
                    return "image/fits";
                }
                public long getContentLength() {
                    return fitsdata.length;
                }
                public void writeBody( OutputStream out ) throws IOException {
                    out.write( fitsdata );
                }
            };

            /* Make it available from the server.
             * But expire it after some amount of time, as this may be 
             * a large-ish amount of memory.
             * Is this a good solution? */
            final TopcatServer server = TopcatServer.getInstance();
            final URL iurl = server.addResource( "density.fits", resource );
            new Timer( true ).schedule( new TimerTask() {
                public void run() {
                    server.removeResource( iurl );
                }
            }, 60000 );

            /* Construct and return the message. */
            String iid = "density-" + ++imageCount_;
            return new Message( "image.load.fits" )
                  .addParam( "url", iurl.toString() )
                  .addParam( "image-id", iid );
        }
    }

    /**
     * SendActionManager implementation for transmitting
     * voresource.loadlist messages.
     */
    private class ResourceListSendActionManager
                  extends UniformCallActionManager {
        private final RegistryPanel regPanel_;
        private final String mtype_;

        /**
         * Constructor.
         *
         * @param  regPanel   panel whose displayed resources will be sent
         * @param  mtype   exact MType of message to send
         */
        ResourceListSendActionManager( RegistryPanel regPanel,
                                       String mtype ) {
            super( regPanel, hubConnector_, mtype, "Resource List" );
            mtype_ = mtype;
            regPanel_ = regPanel;
        }

        protected Map<?,?> createMessage() {
            Map<String,String> idMap = new LinkedHashMap<String,String>();
            RegResource[] resources = regPanel_.getResources();
            for ( int ir = 0; ir < resources.length; ir++ ) {
                idMap.put( resources[ ir ].getIdentifier(), "" );
            }
            return new Message( mtype_ )
                  .addParam( "ids", idMap );
        }
    }

    public JComponent createInfoPanel() {
        JComponent panel = new JPanel( new BorderLayout() );
        Box box = Box.createHorizontalBox();
        box.add( Box.createHorizontalStrut( 5 ) );
        int boxHeight = 20;

        /* Add message tracker panel. */
        if ( hubConnector_ instanceof MessageTrackerHubConnector ) {
            JComponent mBox = ((MessageTrackerHubConnector) hubConnector_)
                             .createMessageBox( boxHeight );
            JLabel mLabel = new JLabel( "Messages: " );
            mLabel.setLabelFor( mBox );
            box.add( mLabel );
            box.add( mBox );
            box.add( Box.createHorizontalStrut( 10 ) );
        }

        /* Add client tracker panel. */
        JComponent cBox = hubConnector_.createClientBox( false, boxHeight );
        JLabel cLabel = new JLabel( "Clients: " );
        cLabel.setLabelFor( cBox );
        box.add( cLabel );
        box.add( cBox );
        box.add( Box.createHorizontalStrut( 10 ) );
 
        /* Add reg/unreg button/indicator. */
        final JButton regButton =
            new JButton( hubConnector_
                        .createRegisterOrHubAction( panel, null ) );
        ChangeListener connListener = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                regButton.setText( null );
                regButton.setIcon( hubConnector_.isConnected()
                                       ? ResourceIcon.CONNECT
                                       : ResourceIcon.DISCONNECT );
            }
        };
        hubConnector_.addConnectionListener( connListener );
        connListener.stateChanged( null );
        regButton.setBorder( BorderFactory.createEmptyBorder() );
        box.add( regButton );
        box.add( Box.createHorizontalStrut( 5 ) );

        /* Wrap and return. */
        panel.setBorder( AuxWindow.makeTitledBorder( getProtocolName() ) );
        panel.add( box, BorderLayout.CENTER );
        return panel;
    }

    /**
     * Makes sure that a map is SAMP-friendly.
     * Any entries which are not are simply discarded.
     */
    public static Map<String,String> sanitizeMap( Map<?,?> map ) {

        /* Retain only entries which are String->String mappings.
         * This is more restrictive than strictly necessary, but it's 
         * easy to do, and it's unlikely (impossible?) that other 
         * legitimate types of entry (String->List or ->Map will be present. */
        Map<String,String> okMap = new LinkedHashMap<String,String>();
        for ( Map.Entry<?,?> entry : map.entrySet() ) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if ( key instanceof String ) {
                String skey = (String) key;
                if ( value instanceof String ) {
                    okMap.put( skey, (String) value );
                }
                else if ( value instanceof Number ) {
                    okMap.put( skey, value.toString() );
                }
                else if ( value instanceof Boolean ) {
                    okMap.put( skey,
                               SampUtils.encodeBoolean( ((Boolean) value)
                                                       .booleanValue() ) );
                }
            }
        }
        return okMap;
    }
}
