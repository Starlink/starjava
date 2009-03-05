/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     04-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenu;

import uk.ac.starlink.plastic.PlasticHub;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;

/**
 * Communicator implementation based on the PLASTIC protocol.
 * This is more or less a facade over a contained {@link PlasticServer} object.
 *
 * @author Mark Taylor
 * @version  $Id$
 */
public class PlasticCommunicator
    extends AbstractCommunicator
{

    /**
     * Object which does most of the work handling PLASTIC operations.
     */
    private SplatPlastic plasticServer;

    /**
     * Cached list of menu actions associated with this object.
     */
    private Action[] interopActions;

    /**
     * Constructor.
     */
    public PlasticCommunicator()
    {
        super( "PLASTIC" );
    }

    public void startHub( boolean external )
        throws IOException
    {
        if ( external ) {
            PlasticUtils.startExternalHub( true );
        }
        else {
            PlasticHub.startHub( null, null );
        }
    }

    public void setBrowser( SplatBrowser browser )
    {
        plasticServer = new SplatPlastic( this );
        super.setBrowser( browser );
    }

    public boolean setActive()
    {

        //  Attempt registration and record success.
        boolean isReg;
        try {
            plasticServer.register();
            isReg = true;
        }
        catch (IOException e) {
            isReg = false;
        }

        //  Either way, arrange it so that we keep a look out for appearing
        //  hubs at time when we're not registered.
        plasticServer.setAutoRegister( 5000 );

        //  Return status.
        return isReg;
    }

    public Action[] getInteropActions()
    {
        if ( interopActions == null ) {
            interopActions = new Action[] {
                plasticServer.getRegisterAction( true ),
                plasticServer.getRegisterAction( false ),
                plasticServer.getHubStartAction( true ),
                plasticServer.getHubStartAction( false ),
                plasticServer.getHubWatchAction(),
            };
        }
        return interopActions;
    }

    public Transmitter createTableTransmitter( SSAQueryBrowser ssaBrowser )
    {
        return adaptTransmitter( new StarTableTransmitter( plasticServer,
                                                           ssaBrowser ) );
    }

    public Transmitter createSpecTransmitter( JList specList )
    {
        return adaptTransmitter( SpecTransmitter
                                .createSpectrumTransmitter( plasticServer,
                                                            specList ) );
    }

    /**
     * Turns a PlasticTransmitter object into a Transmitter object (facade).
     *
     * @param  plastTrans  input object
     * @return  facade over input object
     */
    private static Transmitter adaptTransmitter( final PlasticTransmitter
                                                       plasTrans )
    {
        return new Transmitter()
        {
            public Action getBroadcastAction()
            {
                return plasTrans.getBroadcastAction();
            }
            public JMenu createSendMenu()
            {
                return plasTrans.createSendMenu();
            }
            public void setEnabled( boolean isEnabled )
            {
                plasTrans.setEnabled( isEnabled );
            }
        };
    }
}
