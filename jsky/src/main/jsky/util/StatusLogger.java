/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: StatusLogger.java,v 1.5 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import jsky.util.gui.ProgressBarFilterInputStream;


/**
 * This defines an interface for logging status messages. In a user interface,
 * this can be implemented as a progress bar and test message.
 *
 * @see jsky.util.gui.StatusPanel
 *
 * @version $Revision: 1.5 $
 * @author Allan Brighton
 */
public abstract interface StatusLogger {

    /** Log or display the given message */
    public void logMessage(String msg);

    /** Set the percent done. */
    public void setProgress(int percent);

    /**
     * Return a connection to the given URL and log messages before and after
     * opening the connection.
     */
    public URLConnection openConnection(URL url) throws IOException;

    /**
     * Return a input stream that will generate log messages showing
     * the progress of the read from the given stream.
     *
     * @param in the input stream to be monitored
     * @param size the size in bytes of the date to be read, or 0 if not known
     */
    public ProgressBarFilterInputStream getLoggedInputStream(InputStream in, int size) throws IOException;

    /**
     * Return an input stream to use for reading from the given URL
     * that will generate log messages showing the progress of the read.
     *
     * @param url the URL to read
     */
    public ProgressBarFilterInputStream getLoggedInputStream(URL url) throws IOException;

    /**
     * Stop logging reads from the input stream returned from an
     * earlier call to getLoggedInputStream().
     *
     * @param in an input stream returned from getLoggedInputStream()
     */
    public void stopLoggingInputStream(ProgressBarFilterInputStream in) throws IOException;

}

