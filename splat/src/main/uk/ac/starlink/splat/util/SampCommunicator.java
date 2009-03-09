/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     04-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.AbstractMessageHandler;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.MessageHandler;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.MessageTrackerHubConnector;
import org.astrogrid.samp.xmlrpc.HubMode;
import org.astrogrid.samp.xmlrpc.HubRunner;
import org.astrogrid.samp.xmlrpc.XmlRpcKit;

import uk.ac.starlink.splat.iface.SampFrame;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;

/**
 * Communicator implementation based on the SAMP protocol.
 *
 * @author Mark Taylor
 * @version  $Id$
 */
public class SampCommunicator
    implements SplatCommunicator
{
    /** Handles communication with SAMP hub. */
    protected GuiHubConnector hubConnector;

    /** HTTP server used. */
    protected SplatHTTPServer server;

    /** Splat window on behalf of which this communicator is working. */
    protected SplatBrowser browser;

    /** Cached list of menu-type actions for this communicator. */
    private Action[] interopActions;

    /** Action for displaying SAMP control window. */
    private Action windowAction;

    /** Hub mode used for internal hub. */
    public static final HubMode INTERNAL_HUB_MODE = HubMode.NO_GUI;

    /** Hub mode used for external hub. */
    public static final HubMode EXTERNAL_HUB_MODE = HubMode.MESSAGE_GUI;

    /** Number of seconds between autoconnect attempts. */
    public static int AUTOCONNECT_SECS = 5;

    /** Logger. */
    private static Logger logger =
        Logger.getLogger( SampCommunicator.class.getName() );

    /**
     * Constructor.
     */
    public SampCommunicator()
        throws IOException
    {
        server = SplatHTTPServer.getInstance();
        ClientProfile profile = server.getSampProfile();
        hubConnector = new MessageTrackerHubConnector( profile );
        initConnector();
    }

    /**
     * Initialises the hub connection, including notifying the hub of
     * this client's metadata and subscriptions.
     */
    private void initConnector()
    {
        hubConnector.declareMetadata( createMetadata() );
        MessageHandler[] handlers = createMessageHandlers();
        for ( int i = 0; i < handlers.length; i++ ) {
            hubConnector.addMessageHandler( handlers[ i ] );
        } 
        Subscriptions subs = hubConnector.computeSubscriptions();
        hubConnector.declareSubscriptions( subs );
    }

    /**
     * Returns the metadata which describes SPLAT to the hub.
     */
    private Metadata createMetadata()
    {
        Metadata meta = new Metadata();
        meta.setName( decodeMeta( Utilities.getApplicationName() ) );
        meta.setDescriptionText( decodeMeta( Utilities.getFullDescription() ) );
        meta.setDocumentationUrl( decodeMeta( Utilities.getSupportURL() ) );
        meta.setIconUrl( server.getLogoURL().toString() );
        meta.put( "authors", decodeMeta( Utilities.getAuthors() ) );
        meta.put( "copyright", decodeMeta( Utilities.getCopyright() ) );
        meta.put( "splat.version", decodeMeta( Utilities.getReleaseVersion() ) );
        meta.put( "support.mail", decodeMeta( Utilities.getSupportEmail() ) );
        return meta;
    }

    /**
     * Massages the metadata text output from the various {@link #Utilities}
     * methods.  Those methods return HTML text, but for our purposes
     * we require plain text.
     */
    private static String decodeMeta( String in )
    {
        //  De-HTML-ise input string.  Ad hoc implementation just deals with
        //  those items currently known to appear in the inputs.
        return in.replaceAll( "&amp;", "&" )
                 .replaceAll( "&lt;", "<" )
                 .replaceAll( "&gt;", ">" )
                 .replaceAll( "<br */?>", "\n" );
    }

    /**
     * Returns an array of handlers for all the SAMP messages (MTypes)
     * which SPLAT is prepared to process (is subscrbed to).
     */
    private MessageHandler[] createMessageHandlers() {
        return new MessageHandler[] {
            new SpectrumLoadHandler(),
        };
    }

    public String getProtocolName()
    {
        return "SAMP";
    }

    public void startHub( boolean external )
        throws IOException
    {
        if ( external ) {
            HubRunner.runExternalHub( EXTERNAL_HUB_MODE );
        }
        else {
            HubRunner.runHub( INTERNAL_HUB_MODE, XmlRpcKit.INTERNAL );
        }
    }

    public void setBrowser( SplatBrowser browser )
    {
        this.browser = browser;
    }

    public boolean setActive()
    {
        hubConnector.setActive( true );
        hubConnector.setAutoconnect( AUTOCONNECT_SECS );
        try {
            return hubConnector.getConnection() != null;
        }
        catch (IOException e) {
            logger.warning( "SAMP connection failure: " + e );
            return false;
        }
    }

    public Action getWindowAction()
    {
        if ( windowAction == null ) {
            Icon sampIcon =
                new ImageIcon( ImageHolder.class.getResource( "samp.gif" ) );
            windowAction = new WindowAction( "SAMP", sampIcon );
        }
        return windowAction;
    }

    public Action[] getInteropActions() {
        if ( interopActions == null ) {
            interopActions = new Action[] {
            };
        }
        return interopActions;
    }

    public Transmitter createTableTransmitter( SSAQueryBrowser ssaBrowser )
    {
        return new TableSendActionManager( ssaBrowser, hubConnector );
    }

    public Transmitter createSpecTransmitter( JList specList )
    {
        return new SpectrumSendActionManager( specList, hubConnector );
    }

    /**
     * MessageHandler implementation for dealing with spectrum load MTypes.
     */
    private class SpectrumLoadHandler extends AbstractMessageHandler
    {
        SpectrumLoadHandler()
        {
            super( "spectrum.load.ssa-generic" );
        }

        /**
         * Invoked by SAMP to request SPLAT spectrum load.
         */
        public Map processCall( HubConnection connection, String senderId,
                                Message msg )
            throws Exception
        {
            //  Extract MType-specific parameters from message.
            String location = (String) msg.getRequiredParam( "url" );
            Map meta = (Map) msg.getParam( "meta" );

            //  Turn these into a properties object which SPLAT can process.
            SpectrumIO.Props props = SplatPlastic.getProps( location, meta );

            //  Attempt to load the spectrum synchronously on the 
            //  event dispatch thread.
            SpectrumAdder specAdder = new SpectrumAdder( props );
            SwingUtilities.invokeAndWait( specAdder );

            //  Throw an exception if there was one, else return null (success).
            Exception error = specAdder.getError();
            if ( error != null ) {
                throw error;
            }
            else {
                return null;
            }
        }
    }

    /**
     * Runnable inner class used for loading a spectrum on the 
     * event dispatch thread.
     */
    private class SpectrumAdder implements Runnable
    {
        SpectrumIO.Props props;
        Exception error;

        /**
         * Constructor.
         *
         * @param   props  defines where and what kind of spectrum to load
         */
        SpectrumAdder( SpectrumIO.Props props )
        {
            this.props = props;
        }

        /**
         * Attempts to load the spectrum and stores error on failure.
         */
        public void run()
        {
            try {
                browser.tryAddSpectrum( props );
            }
            catch (Exception e) {
                error = e;
            }
        }

        /**
         * Returns any error which was encountered during spectrum load
         * (run method).
         * 
         * @return   load error, or null on success
         */
        public Exception getError()
        {
            return error;
        }
    }

    /**
     * Action for displaying SAMP status window.
     */
    private class WindowAction extends AbstractAction
    {
        /** SAMP control window object. */
        private JFrame sampFrame = null;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon  action icon
         */
        WindowAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION,
                     "Show SAMP control window"
                   + " (application interoperability)" );
        }

        public void actionPerformed( ActionEvent ae )
        {
            if ( sampFrame == null ) {
                sampFrame = new SampFrame( hubConnector );
            }
            sampFrame.setVisible( true );
        }
    }
}
