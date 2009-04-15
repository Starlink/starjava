/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     04-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import uk.ac.starlink.plastic.PlasticHub;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;

/**
 * Communicator implementation based on the PLASTIC protocol.
 *
 * @author Mark Taylor
 * @version  $Id$
 */
public class PlasticCommunicator
    implements SplatCommunicator
{

    /**
     * Object which does most of the work handling PLASTIC operations.
     */
    protected SplatPlastic plasticServer;

    /**
     * Splat window on behalf of which this communicator is working.
     */
    protected SplatBrowser browser;

    /**
     * Cached list of menu actions associated with this object.
     */
    private Action[] interopActions;

    /**
     * Number of seconds between autoconnect attempts, if applicable.
     */
    public static int AUTOCONNECT_SECS = 5;

    public String getProtocolName()
    {
        return "PLASTIC";
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
        this.browser = browser;
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
        plasticServer.setAutoRegister( AUTOCONNECT_SECS * 1000 );

        //  Return status.
        return isReg;
    }

    public Action getWindowAction()
    {
        return null;
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
                HelpFrame.getAction( "Help on interoperability", "interop" ),
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
     * Adds a spectrum to the browser given a name and type.
     * This invokes a suitable method on the SplatBrowser synchronously
     * on the event dispatch thread and returns a success flag.
     *
     * @param name the name (i.e. file specification) of the spectrum
     *             to add.
     * @param usertype index of the type of spectrum, 0 for default
     *                 based on file extension, otherwise this is an
     *                 index of the knownTypes array in
     *                 {@link uk.ac.starlink.splat.data.SpecDataFactory}.
     * @return  true  iff the load was successful
     */
    protected boolean addSpectrum( final String name, final int usertype )
    {
        if ( SwingUtilities.isEventDispatchThread() ) {
            throw new IllegalStateException(
                "Don't call from event dispatch thread" );
        }
        final Boolean[] result = new Boolean[ 1 ];
        try {
            SwingUtilities.invokeAndWait( new Runnable() {
                public void run() {
                    boolean success;
                    try {
                        browser.tryAddSpectrum( name, usertype );
                        success = true;
                    }
                    catch ( SplatException e ) {
                        success = false;
                    }
                    catch ( Throwable e ) {
                        e.printStackTrace();
                        success = false;
                    }
                    result[ 0 ] = Boolean.valueOf( success );
                }
            } );
        }
        catch ( InterruptedException e ) {
            result[ 0 ] = Boolean.FALSE;
        }
        catch ( InvocationTargetException e ) {
            result[ 0 ] = Boolean.FALSE;
        }
        assert result[ 0 ] != null;
        return result[ 0 ].booleanValue();
    }

    /**
     * Adds a spectrum to a browser given a spectral properties object.
     * This invokes a suitable method on the SplatBrowser synchronously
     * on the event dispatch thread and returns a success flag.
     *
     * @param props a container class for the spectrum properties, including
     *              the specification (i.e. file name etc.) of the spectrum
     * @return  true  iff the load was successful
     */
    protected boolean addSpectrum( final SpectrumIO.Props props )
    {
        if ( SwingUtilities.isEventDispatchThread() ) {
            throw new IllegalStateException(
                "Don't call from event dispatch thread" );
        }
        final Boolean[] result = new Boolean[ 1 ];
        try {
            SwingUtilities.invokeAndWait( new Runnable() {
                public void run() {
                    boolean success;
                    try {
                        browser.tryAddSpectrum( props );
                        success = true;
                    }
                    catch ( SplatException e ) {
                        success = false;
                    }
                    catch ( Throwable e ) {
                        e.printStackTrace();
                        success = false;
                    }
                    result[ 0 ] = Boolean.valueOf( success );
                }
            } );
        }
        catch ( InterruptedException e ) {
            result[ 0 ] = Boolean.FALSE;
        }
        catch ( InvocationTargetException e ) {
            result[ 0 ] = Boolean.FALSE;
        }
        assert result[ 0 ] != null;
        return result[ 0 ].booleanValue();
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
