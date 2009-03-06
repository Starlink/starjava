/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     04-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SplatBrowser;

/**
 * Partial implementation of Communicator, providing some common features.
 *
 * @author Mark Taylor
 * @version $Id$
 */
public abstract class AbstractCommunicator implements SplatCommunicator {

    /** Protocol name. */
    private String name;

    /** Splat window on behalf of which this communicator is working. */
    protected SplatBrowser browser;

    /** Number of seconds between autoconnect attempts, if applicable. */
    public static int AUTOCONNECT_SECS = 5;

    /**
     * Constructor.
     *
     * @param  name  protocol name
     */
    protected AbstractCommunicator( String name )
    {
        this.name = name;
    }

    public String getProtocolName()
    {
        return name;
    }

    public void setBrowser( SplatBrowser browser )
    {
        this.browser = browser;
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
     *                 {@link SpecDataFactory}.
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
}
