/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: JSkyCatRemoteControl.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.app.jskycat;

import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.ImageChangeEvent;
import jsky.image.gui.BasicImageDisplay;
import jsky.image.gui.ImageGraphicsHandler;
import jsky.image.gui.MainImageDisplay;


/**
 * Implements a remote control socket interface for JSkyCat.
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class JSkyCatRemoteControl extends Thread
        implements ImageGraphicsHandler, ChangeListener {

    // The socket to listen on
    private ServerSocket _serverSocket;

    // The main image display
    private MainImageDisplay _imageDisplay;

    // The last line read from the client
    private String _line;

    // The reply from the last command
    private String _reply;

    // The status of the last command (0 is okay)
    private int _status = 0;

    // If true, wait for notification that the image has been redisplayed before
    // sending the reply
    private boolean _waitForImageDisplay = false;

    /**
     * Listen on the given port for remote commands to the given
     * JSkyCat instance.
     */
    public JSkyCatRemoteControl(int portNum, JSkyCat jskycat) throws IOException {
        _serverSocket = new ServerSocket(portNum);
        _imageDisplay = jskycat.getImageDisplay();

        // get notification when the image has been repainted (see below).
        _imageDisplay.addImageGraphicsHandler(this);

        // get notified when the cut levels change
        _imageDisplay.getImageProcessor().addChangeListener(this);

    }

    /**
     * Called when the image processor settings are changed.
     * Try to improve performance by not automatically scanning the image to find the
     * best cut levels (tell it that the user set the cut levels and they should
     * not be changed).
     */
    public void stateChanged(ChangeEvent e) {
        ImageChangeEvent ice = (ImageChangeEvent) e;
        if (ice.isNewCutLevels())
            _imageDisplay.getImageProcessor().setUserSetCutLevels(true);
    }


    /**
     * Called each time the image is repainted.  This feature is
     * (mis)used to get notification when the image has been
     * displayed, since the graphics handlers are called after the
     * image is painted.  This is used to delay the reply for image
     * display commands and avoid overwriting an image while the data
     * is being read. With tiling, this can still happen, but only if
     * the user is scrolling at the time.
     */
    public void drawImageGraphics(BasicImageDisplay imageDisplay, Graphics2D g) {
        _waitForImageDisplay = false;
    }


    /**
     * Start accepting client connections.
     */
    public void run() {
        while (true) {
            try {
                Socket connection = _serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                PrintWriter out = new PrintWriter(connection.getOutputStream());
                while ((_line = in.readLine()) != null) {
                    _waitForImageDisplay = false;
                    try {
                        // evaluate the command in the event dispatching thread
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                try {
                                    _reply = _evalCommand(_line);
                                    _status = 0;
                                }
                                catch (Exception e) {
                                    _reply = e.toString();
                                    _status = 1;
                                }
                            }
                        });

                        // wait for the image to be displayed, if needed
                        while (_waitForImageDisplay) {
                            Thread.yield();
                        }
                    }
                    catch (Exception e) {
                        _reply = e.toString();
                        _status = 1;
                    }

                    // Answer with two lines:
                    //  first line is status (0 is okay) followed by a space and the length of the reply
                    //  second line is the reply (if not empty) or an error message (if status is not 0)
                    int n = _reply.length();
                    out.println(_status + " " + n);
                    if (n != 0)
                        out.print(_reply);
                    out.flush();
                }
                connection.close();
                in.close();
                out.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }


    /**
     * Evaluate the given remote command and return a reply if necessary,
     * otherwise an empty string.
     * <p>
     * The syntax of the command string is a line containing one of the following:
     * <dl>
     *
     * <dd><code>config -file <em>fileOrURL</em></code></dd>
     * <dt>displays the given image file</dt>
     *
     * <dd><code>update</code></dd>
     * <dt>Update the currently displayed image (the image data may have changed, but not the size)</dt>
     *
     * <dd><code>wcscenter</code></dd>
     * <dt>returns a string in the format "hh:mm:ss dd:mm:ss J2000" describing the center of the image</dt>
     *
     * </dl>
     *
     * @param cmd the command to evaluate.
     * @return the result of the command, an empty string.
     * @throws IllegalArgumentException if the command is not valid
     */
    private String _evalCommand(String cmd) {
        if (cmd.equals("update")) {
            return _updateImage();
        }
        else if (cmd.startsWith("config -file ")) {
            return _showImage(cmd.substring(13));
        }
        else if (cmd.equals("wcscenter")) {
            return _wcsCenter();
        }
        else
            throw new IllegalArgumentException("Unknown remote command: " + cmd);
    }

    /**
     * Update the currently displayed image (the image data may have changed, but not the size)
     */
    private String _updateImage() {
        _imageDisplay.updateImageData();
        _waitForImageDisplay = true;
        return "";
    }

    /**
     * Display the given file or URL.
     */
    private String _showImage(String fileOrURL) {
        _imageDisplay.setFilename(fileOrURL);
        _waitForImageDisplay = true;
        return "";
    }


    /**
     * Return the WCS center of the current image
     */
    private String _wcsCenter() {
        if (!_imageDisplay.isWCS())
            throw new IllegalArgumentException("Image does not support WCS");
        WorldCoordinateConverter wcc = _imageDisplay.getWCS();
        WorldCoords pos = new WorldCoords(wcc.getWCSCenter(), wcc.getEquinox());
        return pos.toString();
    }
}


