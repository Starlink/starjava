package uk.ac.starlink.topcat.interop;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.event.ChangeListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.plastic.PlasticHub;
import uk.ac.starlink.plastic.PlasticListWindow;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.HelpAction;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.SubsetWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.plot.DensityWindow;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.vo.RegistryPanel;

/**
 * TopcatCommunicator which uses PLASTIC as the messaging protocol.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2008
 */
public class PlasticCommunicator implements TopcatCommunicator {

    private final ControlWindow control_;
    private final TopcatPlasticListener plasticServer_;
    private final Transmitter tableTransmitter_;

    /**
     * Constructor.
     *
     * @param  control  TOPCAT control window
     */
    public PlasticCommunicator( ControlWindow control ) {
        control_ = control;
        plasticServer_ = new TopcatPlasticListener( control );
        tableTransmitter_ =
            adaptTransmitter( plasticServer_.createTableTransmitter() );
    }

    public String getProtocolName() {
        return "PLASTIC";
    }

    public boolean setActive() {
        boolean isReg;
        try {
            plasticServer_.register();
            isReg = true;
        }
        catch ( IOException e ) {
            isReg = false;
        }
        plasticServer_.setAutoRegister( 5000 );
        return isReg;
    }

    public Action[] getInteropActions() {
        Action helpAct = new HelpAction( "interop", control_ );
        helpAct.putValue( Action.NAME, "Help on interoperability" );
        helpAct.putValue( Action.SHORT_DESCRIPTION,
                          "Show help on PLASTIC with details of "
                        + "supported messages" );
        return new Action[] {
            plasticServer_.getRegisterAction( true ),
            plasticServer_.getRegisterAction( false ),
            new HubWatchAction( control_, plasticServer_ ),
            plasticServer_.getHubStartAction( true ),
            plasticServer_.getHubStartAction( false ),
            helpAct,
        };
    }

    public Transmitter getTableTransmitter() {
        return tableTransmitter_;
    }

    public Transmitter createImageTransmitter( final DensityWindow densWin ) {
        PlasticTransmitter ptrans =
                new TopcatTransmitter( plasticServer_, MessageId.FITS_LOADIMAGE,
                                       "FITS image" ) {
            protected void transmit( PlasticHubListener hub, URI clientId,
                                     ApplicationItem app )
                    throws IOException {
                URI[] recipients = app == null ? null
                                               : new URI[] { app.getId() };
                transmitDensityFits( densWin, hub, clientId, recipients );
            }
        };
        return adaptTransmitter( ptrans );
    }

    public Transmitter createSubsetTransmitter( TopcatModel tcModel,
                                                SubsetWindow subSelector ) {
        return adaptTransmitter( plasticServer_
                                .createSubsetTransmitter( tcModel,
                                                          subSelector ) );
    }

    public Transmitter createResourceListTransmitter( RegistryPanel regPanel,
                                                      String resourceType ) {
        return new DisabledTransmitter( "Resource List" );
    }

    public void startHub( boolean external ) throws IOException {
        if ( external ) {
            PlasticUtils.startExternalHub( true );
        }
        else {
            PlasticHub.startHub( null, null );
        }
    }

    public void maybeStartHub() {
    }

    public SkyPointActivity createSkyPointActivity() {
        final ComboBoxModel<?> selector =
            plasticServer_.createPlasticComboBoxModel( MessageId.SKY_POINT );
        return new SkyPointActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return selector;
            }
            public void pointAtSky( double ra, double dec ) throws IOException {
                Object item = selector.getSelectedItem();
                final URI[] recipients =
                    item instanceof ApplicationItem
                         ? new URI[] { ((ApplicationItem) item).getId() }
                         : null;
                plasticServer_.pointAt( ra, dec, recipients );
            }
        };
    }

    public RowActivity createRowActivity() {
        final ComboBoxModel<?> selector =
            plasticServer_
           .createPlasticComboBoxModel( MessageId.VOT_HIGHLIGHTOBJECT );
        return new RowActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return selector;
            }
            public void highlightRow( TopcatModel tcModel, long lrow )
                    throws IOException {
                Object item = selector.getSelectedItem();
                final URI[] recipients =
                    item instanceof ApplicationItem
                         ? new URI[] { ((ApplicationItem) item).getId() }
                         : null;
                plasticServer_.highlightRow( tcModel, lrow, recipients );
            }
        };
    }

    public SubsetActivity createSubsetActivity() {
        final ComboBoxModel<?> selector =
            plasticServer_
           .createPlasticComboBoxModel( MessageId.VOT_SHOWOBJECTS );
        return new SubsetActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return selector;
            }
            public void selectSubset( TopcatModel tcModel, RowSubset rset )
                    throws IOException {
                Object item = selector.getSelectedItem();
                final URI[] recipients =
                   item instanceof ApplicationItem
                        ? new URI[] { ((ApplicationItem) item).getId() }
                        : null;
                plasticServer_.transmitSubset( tcModel, rset, recipients );
            }
        };
    }

    public SpectrumActivity createSpectrumActivity() {
        final URI msgId = MessageId.SPECTRUM_LOADURL;
        final ComboBoxModel<?> selector =
            plasticServer_.createPlasticComboBoxModel( msgId );
        return new SpectrumActivity() {
            public ComboBoxModel<?> getTargetSelector() {
                return selector;
            }
            public void displaySpectrum( String location, Map<?,?> metadata )
                    throws IOException {
                Object item = selector.getSelectedItem();
                final URI[] recipients =
                    item instanceof ApplicationItem
                         ? new URI[] { ((ApplicationItem) item).getId() }
                         : null;
                List<Object> argList =
                    Arrays.asList( new Object[] { location, location,
                                                  metadata } );
                plasticServer_.register();
                PlasticHubListener hub = plasticServer_.getHub();
                URI plasticId = plasticServer_.getRegisteredId();
                Map<?,?> responses = ( recipients == null )
                    ? hub.request( plasticId, msgId, argList )
                    : hub.requestToSubset( plasticId, msgId, argList,
                                           Arrays.asList( recipients ) );
            }
        };
    }

    public ImageActivity createImageActivity() {
        return new PlasticImageActivity( plasticServer_ );
    }

    public JComponent createInfoPanel() {
        return null;
    }

    public Action createWindowAction( Component parent ) {
        return null;
    }

    public boolean isConnected() {
        return plasticServer_.getRegisteredId() != null;
    }

    public void addConnectionListener( ChangeListener listener ) {
        plasticServer_.getRegisterToggle().addChangeListener( listener );
    }

    /**
     * Turns a PlasticTransmitter into a Transmitter.
     *
     * @param   plasTrans  base transmitter object
     * @return   Transmitter facade
     */
    private static Transmitter adaptTransmitter( final PlasticTransmitter
                                                       plasTrans ) {
        return new Transmitter() {
            public Action getBroadcastAction() {
                return plasTrans.getBroadcastAction();
            }
            public JMenu createSendMenu() {
                return plasTrans.createSendMenu();
            }
            public void setEnabled( boolean isEnabled ) {
                plasTrans.setEnabled( isEnabled );
            }
        };
    }

    /**
     * Transmits the image currently plotted in the given DensityWindow 
     * a as a FITS file to PLASTIC listeners.
     *
     * @param  densWin  window holding plot
     * @param  hub  hub object
     * @param  plasticId  registration ID for this applicaition
     * @param  recipients  list of targets PLASTIC ids for this message;
     *         if null broadcast to all
     */
    private static void transmitDensityFits( final DensityWindow densWin,
                                             final PlasticHubListener hub,
                                             final URI plasticId,
                                             final URI[] recipients )
            throws IOException {

        /* Write the data as a FITS image to a temporary file preparatory
         * to broadcast. */
        final File tmpfile = File.createTempFile( "plastic", ".fits" );
        final String tmpUrl = URLUtils.makeFileURL( tmpfile ).toString();
        tmpfile.deleteOnExit();
        OutputStream ostrm =
            new BufferedOutputStream( new FileOutputStream( tmpfile ) );
        try {
            densWin.exportFits( ostrm );
        }
        catch ( IOException e ) {
            tmpfile.delete();
            throw e;
        }
        finally {
            ostrm.close();
        }

        /* Do the broadcast, synchronously so that we don't delete the
         * temporary file to early, but in another thread so we don't block
         * the GUI. */
        new Thread( "FITS broadcast" ) {
            public void run() {
                List<Object> argList =
                    Arrays.asList( new Object[] { tmpUrl, tmpUrl } );
                URI msgId = MessageId.FITS_LOADIMAGE;
                Map<?,?> responses = recipients == null
                    ? hub.request( plasticId, msgId, argList )
                    : hub.requestToSubset( plasticId, msgId, argList,
                                           Arrays.asList( recipients ) );
                tmpfile.delete();
            }
        }.start();
    }

    /**
     * Action which displays a window giving some information about 
     * the state of the PLASTIC hub.
     */     
    private static class HubWatchAction extends BasicAction {
        private final Component parent_;
        private final HubManager hubManager_;
        private JFrame hubWindow_;

        /**
         * Constructor.
         *
         * @param  parent  parent window
         * @param  hubManager  connection manager
         */
        HubWatchAction( Component parent, HubManager hubManager ) {
            super( "Show Registered Applications", null,
                   "Display applications registered with the PLASTIC hub" );
            parent_ = parent;
            hubManager_ = hubManager;
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( hubWindow_ == null ) {
                hubWindow_ = new PlasticListWindow( hubManager_
                                                   .getApplicationListModel() );
                hubWindow_.setTitle( "PLASTIC apps" );
                AuxWindow.positionAfter( parent_, hubWindow_ );
                hubWindow_.pack();
            }
            hubWindow_.setVisible( true );
        }
    }
}
