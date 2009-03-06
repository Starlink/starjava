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
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;

import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.MessageTrackerHubConnector;
import org.astrogrid.samp.xmlrpc.HubMode;
import org.astrogrid.samp.xmlrpc.HubRunner;
import org.astrogrid.samp.xmlrpc.XmlRpcKit;

import uk.ac.starlink.splat.iface.SampFrame;
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
    extends AbstractCommunicator
{
    /** Handles communication with SAMP hub. */
    private GuiHubConnector hubConnector;

    /** Cached list of menu-type actions for this communicator. */
    private Action[] interopActions;

    /** Action for displaying SAMP control window. */
    private Action windowAction;

    /** Hub mode used for internal hub. */
    private static final HubMode INTERNAL_HUB_MODE = HubMode.NO_GUI;

    /** Hub mode used for external hub. */
    private static final HubMode EXTERNAL_HUB_MODE = HubMode.MESSAGE_GUI;

    /** Logger. */
    private static Logger logger =
        Logger.getLogger( SampCommunicator.class.getName() );

    /**
     * Constructor.
     */
    public SampCommunicator()
        throws IOException
    {
        super( "SAMP" );
        ClientProfile profile = SplatHTTPServer.getInstance().getSampProfile();
        hubConnector = new MessageTrackerHubConnector( profile );
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
        super.setBrowser( browser );
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
