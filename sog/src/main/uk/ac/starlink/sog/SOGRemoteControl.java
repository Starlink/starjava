/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 */

// This is based on the JSky class JSkyCatRemoteControl. It is quite
// different in operation as remote control is now offered via SOAP
// services (using HTTP), rather than a simple socket based IPC
// mechanism.

package uk.ac.starlink.sog;

import java.awt.Graphics2D;
import java.io.IOException;
import java.net.URL;

import javax.media.jai.PlanarImage;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.ImageChangeEvent;
import jsky.image.gui.BasicImageDisplay;
import jsky.image.gui.ImageGraphicsHandler;

import org.w3c.dom.Element;

import uk.ac.starlink.soap.AppHttpSOAPServer;
import uk.ac.starlink.jaiutil.HDXImage;

/**
 * Implements the SOAP web services offered by the SOG
 * application. There is only one instance of this class for the SOG
 * application, but it does not come into existance until the
 * {@link #getInstance()} method is invoked. You should always
 * set the port number in use (the default of 8082 isn't suitable for
 * applications on a general user machine).
 *
 * @author Peter W. Draper
 * @author Allan Brighton
 * @version $Id$
 * @since 27-MAY-2002
 */
public class SOGRemoteControl
    implements ImageGraphicsHandler, ChangeListener
{
    /** The application HTTP/SOAP server */
    private AppHttpSOAPServer _server = null;

    /** The main image display */
    private SOGNavigatorImageDisplay _imageDisplay;

    /** If true, wait for notification that the image has been
     *  redisplayed before sending the reply */
    private boolean _waitForImageDisplay = false;

    /** The port number for the HTTP server. Always re-define this. */
    private int portNumber = 8082;

    /**
     * The instance.
     */
    private static SOGRemoteControl _instance = null;

    /**
     * Get the instance. Uses lazy instantiation so object does not
     * exist until the first invocation of this method.
     */
    public static SOGRemoteControl getInstance()
    {
        if ( _instance == null ) {
            try {
                _instance = new SOGRemoteControl();
            }
            catch (IOException e) {
                e.printStackTrace();
                _instance = null;
            }
        }
        return _instance;
    }

    /**
     * Listen on the given port for remote RPC SOAP requests.
     */
    private SOGRemoteControl()
        throws IOException
    {
        _imageDisplay = 
            (SOGNavigatorImageDisplay) SOG.getInstance().getImageDisplay();

        // get notification when the image has been repainted (see below).
        _imageDisplay.addImageGraphicsHandler( this );

        // get notified when the cut levels change
        _imageDisplay.getImageProcessor().addChangeListener( this );
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
     * Called when the image processor settings are changed.
     *
     * Try to improve performance by not automatically scanning the
     * image to find the best cut levels (tell it that the user set
     * the cut levels and they should not be changed).
     */
    public void stateChanged( ChangeEvent e )
    {
        ImageChangeEvent ice = (ImageChangeEvent)e;
        if ( ice.isNewCutLevels() ) {
            _imageDisplay.getImageProcessor().setUserSetCutLevels( true );
        }
    }

    /**
     * Called each time the image is repainted.  This feature is
     * (mis)used to get notification when the image has been displayed,
     * since the graphics handlers are called after the image is
     * painted.  This is used to delay the reply for image display
     * commands and avoid overwriting an image while the data is being
     * read. With tiling, this can still happen, but only if the user
     * is scrolling at the time.
     */
    public void drawImageGraphics( BasicImageDisplay imageDisplay,
                                   Graphics2D g )
    {
        _waitForImageDisplay = false;
    }

    /**
     * Start the remote control services. Do not forget to use this
     * method as no services are available until after it is invoked.
     */
    public void start()
    {
        //  Create the HTTP/SOAP server. Need our local description to
        //  define the SOAP services offered (by this class). 
        URL deployURL = SOGRemoteControl.class.getResource( "deploy.wsdd" );
        System.out.println( deployURL );
        try {
            _server = new AppHttpSOAPServer( portNumber );
            _server.start();
            _server.addSOAPService( deployURL );
            System.out.println( "port = " + portNumber );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( "Failed to start SOG SOAP services" );
        }
    }

    /**
     * Stop the remote control services.
     */
    public void stop()
    {
        if ( _server != null ) {
            try {
                _server.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Update the currently displayed image (the image data may have
     * changed, but not the size)
     */
    public String updateImage()
    {
        _imageDisplay.updateImageData();
        _waitForImageDisplay = true;
        return "";
    }

    /**
     * Display the given file or URL.
     */
    public String showImage( String fileOrURL )
    {
        _imageDisplay.setFilename( fileOrURL );
        _waitForImageDisplay = true;
        return "";
    }

    /**
     * Display the given DOM element as an NDX.
     */
    public String showNDX( Element element )
    {
        _imageDisplay.setNDX( element );
        _waitForImageDisplay = true;
        return "";
    }

    /**
     * Return the WCS center of the current image
     */
    public String wcsCenter()
    {
        if ( ! _imageDisplay.isWCS() ) {
            throw new IllegalArgumentException("Image does not support WCS");
        }
        WorldCoordinateConverter wcc = _imageDisplay.getWCS();
        WorldCoords pos = new WorldCoords( wcc.getWCSCenter(), 
                                           wcc.getEquinox() );
        return pos.toString();
    }
}
