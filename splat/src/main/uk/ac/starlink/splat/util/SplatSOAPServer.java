// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    17-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.util;

import java.net.URL;
import java.io.IOException;

import org.w3c.dom.Element;

import uk.ac.starlink.soap.AppHttpSOAPServer;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.PlotControlFrame;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.NDXSpecDataImpl;

/**
 * Implements the SOAP web services offered by the SPLAT
 * application. There is only one instance of this class for the SPLAT
 * application, but it does not come into existance until the
 * {@link #getInstance()} method is invoked. 
 * <p>
 * Current (unlike the IPC socket remote services) you should always
 * set the port number in use (the default of 8081 isn't suitable for
 * applications on a general user machine).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SplatSOAPServer
{
    /** 
     * The application HTTP/SOAP server 
     */
    private AppHttpSOAPServer server = null;

    /**
     * The instance of SplatBrowser that we're attached to.
     */
    private SplatBrowser browserMain = null;

    /** 
     * The port number for the HTTP server. Always re-define this. 
     */
    private int portNumber = 8081;

    /**
     * The instance.
     */
    private static SplatSOAPServer instance = null;

    /**
     * Get the instance. Uses lazy instantiation so object does not
     * exist until the first invocation of this method. Make sure 
     * that the SplatBrowser to be used is set before making any of
     * of this reference {@link #setSplatBrowser()}.
     */
    public static SplatSOAPServer getInstance()
    {
        if ( instance == null ) {
            try {
                instance = new SplatSOAPServer();
            }
            catch (IOException e) {
                e.printStackTrace();
                instance = null;
            }
        }
        return instance;
    }

    /**
     * Listen for remote RPC SOAP requests.
     */
    private SplatSOAPServer()
        throws IOException
    {
        //  Do nothing.
    }

    /**
     * Set the instance of SplatBrowser that we're attached to.
     */
    public void setSplatBrowser( SplatBrowser browserMain )
    {
        this.browserMain = browserMain;
    }

    /**
     * Get the instance of SplatBrowser that we're attached to.
     */
    public SplatBrowser getSplatBrowser()
    {
        return browserMain;
    }

    /**
     * Set the port number of the HTTP server.
     */
    public void setPortNumber( int portNum ) 
    {
        portNumber = portNum;
    }

    /**
     * Return the port number being used by this application.
     */
    public int getPortNumber()
    {
        return portNumber;
    }

    /**
     * Start the remote services. Do not forget to use this method as
     * no services are available until after it is invoked. 
     */
    public void start()
    {
        //  Create the HTTP/SOAP server. Need our local description to
        //  define the SOAP services offered (by this class). 
        URL deployURL = SplatSOAPServer.class.getResource( "deploy.wsdd" );
        System.out.println( deployURL );
        try {
            server = new AppHttpSOAPServer( portNumber );
            server.start();
            server.addSOAPService( deployURL );
            System.out.println( "port = " + portNumber );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start SPLAT SOAP services");
        }
    }

    /**
     * Stop the remote control services.
     */
    public void stop()
    {
        if ( server != null ) {
            try {
                server.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//
// Define the actual services, these are mediated through a static
// class SOAPServices.
//
    /**
     * Display a spectrum by name.
     *
     * @param specspec the spectrum specification
     */
    public boolean displaySpectrum( String specspec )
    {
        PlotControlFrame plot = browserMain.displaySpectrum( specspec );
        return (plot == null);
    }


    /**
     * Accept an NDX as Element description and display it.
     */
    public void displayNDX( Element ndxElement )
    {
        try {
            NDXSpecDataImpl impl = new NDXSpecDataImpl( ndxElement );
            SpecData spectrum = new SpecData( impl );
            browserMain.addSpectrum( spectrum );
            browserMain.displaySpectrum( spectrum );
        }
        catch( SplatException e ) {
            e.printStackTrace();
        }
    }
}
