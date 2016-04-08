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
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.astrogrid.samp.ErrInfo;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.MessageHandler;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.MessageTrackerHubConnector;
import org.astrogrid.samp.gui.SysTray;
import org.astrogrid.samp.hub.Hub;
import org.astrogrid.samp.hub.HubServiceMode;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.SampFrame;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.SpectrumIO.SourceType;
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
        SpectrumLoadHandler loadHandler = new SpectrumLoadHandler();
        initConnector( new MessageHandler[] { loadHandler, } );
    }

    /**
     * Initialises the hub connection, including notifying the hub of
     * this client's metadata and subscriptions.
     */
    private void initConnector( MessageHandler[] handlers )
    {
        hubConnector.declareMetadata( createMetadata() );
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

    public String getProtocolName()
    {
        return "SAMP";
    }

    public void startHub( boolean external )
        throws IOException
    {
        if ( external ) {
            Hub.runHub( HubServiceMode.MESSAGE_GUI );
        }
        else {
            Hub.runHub( SysTray.getInstance().isSupported()
                            ? HubServiceMode.CLIENT_GUI
                            : HubServiceMode.NO_GUI );
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
            Icon sampOffIcon =
                new ImageIcon( ImageHolder.class.getResource( "samp.gif" ) );
            Icon sampOnIcon =
                new ImageIcon( ImageHolder.class.getResource( "sampgo.gif" ) );
            windowAction = 
                new WindowAction( "SAMP control", sampOffIcon, sampOnIcon );
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
    
    public EventEnabledTransmitter createBinFITSTableTransmitter( JList specList )
    {
        return new BinFITSTableSendActionManager( specList, hubConnector );
    }
    
    public EventEnabledTransmitter createBinFITSTableTransmitter( SSAQueryBrowser ssaQueryBrowser )
    {
        return new BinFITSTableSendActionManager( ssaQueryBrowser, hubConnector );
    }
    
    public EventEnabledTransmitter createVOTableTransmitter( JList specList )
    {
        return new VOTableSendActionManager( specList, hubConnector );
    }
    
    public EventEnabledTransmitter createVOTableTransmitter( SSAQueryBrowser ssaQueryBrowser )
    {
        return new VOTableSendActionManager( ssaQueryBrowser, hubConnector );
    }

    /**
     * MessageHandler implementation for dealing with spectrum load MTypes.
     */
    private class SpectrumLoadHandler
            implements MessageHandler, SpectrumIO.Watch
    {
        private final Subscriptions subs;
        private final Map propsMap;

        SpectrumLoadHandler()
        {
        	subs = new Subscriptions();
            subs.addMType( "spectrum.load.ssa-generic" );
            subs.addMType( "table.load.fits" );
            subs.addMType( "table.load.votable" );
            propsMap = Collections.synchronizedMap( new IdentityHashMap() );
        }

        public Map getSubscriptions()
        {
        	return subs;
        }

        /**
         * Invoked by SAMP to request SPLAT spectrum load, with no response
         * required.
         */
        public void receiveNotification( HubConnection connection,
                                         String senderId, Message message )
        {
        	SpectrumIO.Props props = createProps( message );
            SpectrumIO.getInstance().setWatcher( this );
            props.setType(7); //set type == guess
            loadSpectrum( props );
        }

        /**
         * Invoked by SAMP to request SPLAT spectrum load, with an
         * asynchronous response required.
         */
        public void receiveCall( HubConnection connection, String senderId,
                                 String msgId, Message message )
        {
        	SpectrumIO.Props props = createProps( message );
            propsMap.put( props, new MsgInfo( connection, msgId ) );
            SpectrumIO.getInstance().setWatcher( this );
            loadSpectrum( props );
        }

        /**
         * Invoked by SpectrumIO when a load has completed with success.
         */
        public void loadSucceeded( SpectrumIO.Props props )
        {
        	MsgInfo msginfo = (MsgInfo) propsMap.remove( props );
            if ( msginfo != null ) {
                Response response =
                    Response.createSuccessResponse( new HashMap() );
                msginfo.reply( response );
            }
            else {
                logger.info( "Orphaned SAMP spectrum load success?" );
            }
            if ( propsMap.size() == 0 ) {
                SpectrumIO.getInstance().setWatcher( null );
            }
        }

        /**
         * Invoked by SpectrumIO when a load has completed with failure.
         */
        public void loadFailed( SpectrumIO.Props props, Throwable error )
        {
        	MsgInfo msginfo = (MsgInfo) propsMap.remove( props );
            if ( msginfo != null ) {
                ErrInfo errInfo = new ErrInfo( error );
                errInfo.setErrortxt( "Spectrum load failed" );
                Response response =
                    Response.createErrorResponse( errInfo );
                msginfo.reply( response );
            }
            else {
                logger.info( "Orphaned SAMP spectrum load failure?" );
            }
            if ( propsMap.size() == 0 ) {
                SpectrumIO.getInstance().setWatcher( null );
            }
        }

        /**
         * Constructs a SpectrumIO.Props object corresponding to an
         * incoming SAMP spectrum load message.
         * 
         * @param   msg  spectrum load message
         * @return  new Props object
         */
        private SpectrumIO.Props createProps( Message msg )
        {
            //  Extract MType-specific parameters from message.
            String location = (String) msg.getRequiredParam( "url" );
            Map meta = (Map) msg.getParam( "meta" );
            String shortName = (String) msg.getParam( "name" );

            //  Turn these into a properties object which SPLAT can process.
            SpectrumIO.Props props = getProps( location, meta );
            if ( shortName != null && shortName.trim().length() > 0 ) {
                props.setShortName( shortName );
            }

            //  Table Mtypes lack SSA meta-data, attempt to handle this by
            //  using the standard SPLAT guessing for tables. This also makes
            //  sure we download using HTTP if necessary.
            if ( ! msg.getMType().equals( "spectrum.load.ssa-generic" ) ) {
                props.setType( SpecDataFactory.TABLE );
            }
            
            props.setSourceType(SourceType.SAMP);
            return props;
        }

        /**
         * Dispatches load of a spectrum defined by a Props object.
         *
         * @param  props   spectrum properties
         */
        private void loadSpectrum( SpectrumIO.Props props )
        {
            //  Attempt to load the spectrum asynchronously on the 
            //  event dispatch thread.
        	SpectrumAdder specAdder = new SpectrumAdder( props );
            SwingUtilities.invokeLater( specAdder );
        }
    }

    /**
     * Convert a spectrum specification (a URL and Map of SSAP metadata) into
     * a SpectrumIO.Prop instance.
     *
     * @param location URL of spectrum.
     * @param meta a key-value map of SSAP metadata that describes the
     *             spectrum to be accessed.
     */
    public static SpectrumIO.Props getProps( String location, Map meta )
    {
        SpectrumIO.Props props = new SpectrumIO.Props( location );
        if ( meta != null && meta.size() > 0 ) {
            SpecDataFactory specDataFactory = SpecDataFactory.getInstance();
            Set keys = meta.keySet();
            Iterator i = keys.iterator();
            String key;
            String value;
            String axes[];
            String units[];
            while( i.hasNext() ) {
                key = (String) i.next();
                value = String.valueOf( meta.get( key ) );
                key = key.toLowerCase();

                //  UTYPEs and UCDs, maybe UTYPES should be sdm:ssa.xxx.
                //  Many of the SSAP response UTYPEs don't seem documented yet.
                if ( key.equals( "vox:spectrum_format" ) ||
                     utypeMatches( key, "access.format" ) ) {
                    props.setType( specDataFactory.mimeToSPLATType( value ) );
                    //props.setObjectType(SpecDataFactory.mimeToObjectType(value));
                }
                else if ( key.equals( "vox:image_title" ) ||
                          utypeMatches( key, "target.name" ) ) {
                    props.setShortName( value );
                }
                else if ( key.equals( "vox:spectrum_axes" ) ) {
                    axes = value.split( "\\s" );
                    if ( axes.length > 0  ) {
                        props.setCoordColumn( axes[0] );
                        if ( axes.length > 1 ) {
                            props.setDataColumn( axes[1] );
                            if ( axes.length == 3 ) {
                                props.setErrorColumn( axes[2] );
                            }
                        }
                    }
                }
                else if ( utypeMatches( key, "Dataset.SpectralAxis" ) ) {
                    props.setCoordColumn( value );
                }
                else if ( utypeMatches( key, "Dataset.FluxAxis" ) ) {
                    props.setDataColumn( value );
                }
                else if ( key.equals( "vox:spectrum_units" ) ) {
                    units = value.split("\\s");
                    if ( units.length > 0  ) {
                        props.setCoordUnits( units[0] );
                        if ( units.length > 1 ) {
                            props.setDataUnits( units[1] );
                        }
                    }
                }
            }
        }
        return props;
    }

    /**
     * Determines whether a given map key corresponds to a utype string.
     *
     * @param   key  provided map key
     * @param   utype  UType to test against, without namespacing
     * @return  true iff they appear to match
     */
    private static boolean utypeMatches( String key, String utype )
    {
        // Not sure what the correct utype namespacing is, if anything;
        // be liberal about that, and about case sensitivity, for now.
        if ( key == null ) {
            return false;
        }
        String lKey = key.toLowerCase();
        String lUtype = utype.toLowerCase();
        return lKey.equals( lUtype )
            || lKey.endsWith( ":" + lUtype );
    }


    /**
     * Encapsulates information about a message which has been received.
     */
    private class MsgInfo
    {
        private final HubConnection connection;
        private final String msgId;

        /**
         * Constructor.
         *
         * @param  connection  connection on which the message was received
         * @param  msgId  message ID
         */
        MsgInfo( HubConnection connection, String msgId )
        {
            this.connection = connection;
            this.msgId = msgId;
        }

        /**
         * Sends a given response object to the appropriate destination
         * for this message.
         *
         * @param  response   reply to send back
         */
        void reply( Response response ) {
            try {
                connection.reply( msgId, response );
            }
            catch ( Throwable e ) {
                logger.info( "SAMP response failed: " + e );
            }
        }
    }

    /**
     * Runnable inner class used for loading a spectrum on the 
     * event dispatch thread.
     */
    private class SpectrumAdder implements Runnable
    {
        final SpectrumIO.Props props;

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
            SpectrumIO.getInstance()
                      .load( browser, true, new SpectrumIO.Props[] { props } );
        }
    }

    /**
     * Action for displaying SAMP status window.
     */
    private class WindowAction
        extends AbstractAction
        implements ChangeListener
    {
        /** SAMP control window object. */
        private JFrame sampFrame = null;

        /** Icon representing disconnected state. */
        private Icon offIcon;

        /** Icon representing connected state. */
        private Icon onIcon;

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  offIcon  action icon when disconnected
         * @param  onIcon  action icon when connected
         */
        WindowAction( String name, Icon offIcon, Icon onIcon )
        {
            super( name );
            this.offIcon = offIcon;
            this.onIcon = onIcon;
            putValue( SHORT_DESCRIPTION,
                     "Show SAMP control window"
                   + " (application interoperability)" );

            //  Change icon in accordance with connection status.
            hubConnector.addConnectionListener( this );
            stateChanged( null );
        }

        public void actionPerformed( ActionEvent ae )
        {
            if ( sampFrame == null ) {
                sampFrame = new SampFrame( hubConnector );
            }
            sampFrame.setVisible( true );
        }

        /**
         * Invoked when connection status has, or may have, changed.
         */
        public void stateChanged( ChangeEvent ce )
        {
            putValue( SMALL_ICON,
                      hubConnector.isConnected() ? onIcon : offIcon );
        }
    }
}
