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

import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;

/**
 * Abstract interface for inter-application messaging requirements of SPLAT.
 * This can be implemented by SAMP or PLASTIC (or others). PLASTIC
 * support is now withdrawn, so this keep for future flexibility.
 *
 * @author   Mark Taylor
 * @version  $Id$
 */
public interface SplatCommunicator
{

    /**
     * Initialises this object to work with a given SplatBrowser.
     * Must be called before other methods except as noted.
     *
     * @param   browser  SPLAT browser on behalf of which this is working
     */
    void setBrowser( SplatBrowser browser );

    /**
     * Must be called before any of the Actions provided by this object
     * are used.  May initiate communication with the messaging system etc.
     *
     * @return  true iff communication with the hub has been established
     */
    boolean setActive();

    /**
     * Returns the name of the protocol over which this object is implemented.
     *
     * @return   protocol name
     */
    String getProtocolName();

    /**
     * Attempts to start a messaging hub suitable for use with this object.
     * May be called before {@link #setBrowser} and {@link #setActive}.
     *
     * @param  external  true to run hub in external JVM,
     *                   false to run it in the current one
     */
    void startHub( boolean external ) throws IOException;

    /**
     * Returns an action which will display a SPLAT window giving 
     * information and perhaps providing configuration capabilities
     * for this communicator.  May return null if nothing suitable
     * is implemented.
     */
    Action getWindowAction();

    /**
     * Returns a list of actions suitable for insertion in a general purpose
     * menu associated with interoperability functionality
     * (register/unregister etc).
     *
     * @return   action list
     */
    Action[] getInteropActions();

    /**
     * Returns an object that can send a table from SPLAT 
     * to other applications.
     *
     * @param   browser  browser window which knows what table to transmit
     * @return  table transmitter
     */
    Transmitter createTableTransmitter( SSAQueryBrowser browser );

    /**
     * Returns an object that can send a spectrum from SPLAT
     * to other applications.
     *
     * @param  specList  list of spectra
     * @return  spectrum transmitter
     */ 
    Transmitter createSpecTransmitter( JList specList );
    
    /**
     * Returns an object that can send a Binary FITS table from SPLAT
     * to other applications.
     *
     * @param  specList  list of spectra
     * @return  spectrum transmitter
     */ 
    EventEnabledTransmitter createBinFITSTableTransmitter( JList specList );
    
    /**
     * Returns an object that can send a Binary FITS table from SPLAT
     * to other applications.
     *
     * @param  ssaQueryBrowser  SSA Query Browser instance
     * @return  spectrum transmitter
     */ 
    EventEnabledTransmitter createBinFITSTableTransmitter( SSAQueryBrowser ssaQueryBrowser );
    
    /**
     * Returns an object that can send a VOTable from SPLAT
     * to other applications.
     *
     * @param  specList  list of spectra
     * @return  spectrum transmitter
     */ 
    EventEnabledTransmitter createVOTableTransmitter( JList specList );
    
    /**
     * Returns an object that can send a VOTable from SPLAT
     * to other applications.
     *
     * @param  ssaQueryBrowser  SSA Query Browser instance
     * @return  spectrum transmitter
     */ 
    EventEnabledTransmitter createVOTableTransmitter( SSAQueryBrowser ssaQueryBrowser );
}
