/*
 * ESO Archive
 *
 * $Id: ProgressBarFilterInputStream.java,v 1.8 2002/08/04 21:48:51 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/24  Created
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

import jsky.util.FileUtil;
import jsky.util.Logger;
import jsky.util.SwingWorker;

import java.io.FilterInputStream;
import javax.swing.JTextField;
import javax.swing.DefaultBoundedRangeModel;
import java.io.InputStream;
import java.net.URL;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;


/**
 * Monitors reading from a given stream or URL and updates a given progress
 * bar and text field to show the amount of data read so far.
 */
public class ProgressBarFilterInputStream extends FilterInputStream {

    /** The progress bar to use */
    protected ProgressBarUtil progressBar;

    /** Text field used to display status information */
    JTextField statusField;

    /** The model for the progress bar */
    protected DefaultBoundedRangeModel model;

    /** The number of bytes read so far */
    protected int nread = 0;

    /** The size of the data in bytes, if known, otherwise 0 */
    protected int size = 0;

    /** Time in ms of last update (used to slow down text field updates) */
    protected long updateTime = 0L;

    /** Set this to interrupt the reading and throw an exception */
    protected boolean interrupted = false;


    /**
     * Constructs an object to monitor the progress of an input stream
     * using a given progress bar and text field.
     *
     * @param progressBar the progress bar to use
     * @param statusField text field used to display status information
     * @param in the input stream to be monitored
     * @param size the size in bytes of the date to be read, or 0 if not known
     */
    public ProgressBarFilterInputStream(ProgressBarUtil progressBar, JTextField statusField, InputStream in, int size) {
        super(in);
        this.progressBar = progressBar;
        this.statusField = statusField;
        setSize(size);
    }

    /**
     * Constructs an object to monitor the progress of an input stream
     * using a given progress bar and text field.
     *
     * @param progressBar the progress bar to use
     * @param statusField text field used to display status information
     * @param url the URL to read
     */
    public ProgressBarFilterInputStream(ProgressBarUtil progressBar, JTextField statusField, URL url) {
        super(FileUtil.makeURLStream(url));
        this.progressBar = progressBar;
        this.statusField = statusField;

        progressBar.startAnimation();
        statusField.setText("Connect: Host " + url.getHost());
        try {
            int size = url.openConnection().getContentLength();
            progressBar.stopAnimation();
            setSize(size);
        }
        catch (Exception e) {
            statusField.setText(e.getMessage());
            progressBar.stopAnimation();
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        statusField.setText("Connected to Host " + url.getHost());
    }


    /**
     * Constructs an object to monitor the progress of an input stream
     * using a given StatusPanel object.
     *
     * @param statusPanel used to display status information
     * @param url the URL to read
     */
    public ProgressBarFilterInputStream(StatusPanel statusPanel, URL url) {
        this(statusPanel.getProgressBar(), statusPanel.getTextField(), url);
    }


    /**
     * Interrupt the reading (causes the next read() to throw an exception).
     * This is normally called when a Stop or Cancel button is pushed.
     */
    public void interrupt() {
        interrupted = true;
        progressBar.stopAnimation();
        progressBar.setStringPainted(false);
        if (model != null)
            model.setValue(0);
        statusField.setText("Reading interrupted.");
    }

    /** Return true if reading was interrupted */
    public boolean isInterrupted() {
        return interrupted;
    }

    /** Throw an exception if interrupt() was called on this stream. */
    public void checkForInterrupt() throws IOException {
        if (interrupted) {
            throw new ProgressException("Reading interrupted");
        }
    }

    /** Set the size of the data to read */
    public void setSize(final int size) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setSize(size);
                }
            });
            return;
        }

        this.size = size;
        if (size <= 0) {
            model = null;
            progressBar.startAnimation();
            progressBar.setStringPainted(false);
        }
        else {
            progressBar.stopAnimation();
            model = new DefaultBoundedRangeModel(0, 0, 0, size);
            progressBar.setModel(model);
            progressBar.setStringPainted(true);
        }
    }


    /**
     * Set the number of bytes that have been read, update the display (but not
     * too often) and check for interrupt requests.
     */
    protected void setNumBytesRead(final int n) {
        nread = n;

        // delay update to improve performance
        long t = System.currentTimeMillis();
        if ((t - updateTime) > 200) {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        setNumBytesRead(n);
                    }
                });
                return;
            }

            if (model != null) {
                progressBar.stopAnimation();
                model.setValue(nread);
            }
            statusField.setText("Reading File: " + nread + " bytes");
            updateTime = t;
        }
    }


    /** Reset the progress bar to the idle state */
    public void clear() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    clear();
                }
            });
            return;
        }

        nread = 0;
        updateTime = 0L;

        progressBar.stopAnimation();
        progressBar.setStringPainted(false);
        statusField.setText("Document Done");
        if (model != null)
            model.setValue(0);
    }


    /** Return the size of the data to read */
    public int getSize() {
        return size;
    }


    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress bar after the read.
     */
    public int read() throws IOException {
        checkForInterrupt();
        int c = in.read();
        if (c >= 0)
            setNumBytesRead(nread + 1);
        else
            clear();

        return c;
    }


    /**
     * Overrides <code>FilterInputStream.read</code>
     * to update the progress bar after the read.
     */
    public int read(byte b[], int off, int len) throws IOException {
        checkForInterrupt();
        int nr = in.read(b, off, len);
        if (nr > 0)
            setNumBytesRead(nread + nr);
        else if (nr == -1)
            clear();
        return nr;
    }


    /**
     * Overrides <code>FilterInputStream.skip</code>
     * to update the progress bar after the skip.
     */
    public long skip(long n) throws IOException {
        checkForInterrupt();
        long nr = in.skip(n);
        if (nr > 0)
            setNumBytesRead(nread + (int) nr);
        return nr;
    }


    /**
     * Overrides <code>FilterInputStream.close</code>
     * to close the progress bar as well as the stream.
     */
    public void close() throws IOException {
        in.close();
        clear();
    }


    /**
     * Overrides <code>FilterInputStream.reset</code>
     * to reset the progress bar as well as the stream.
     */
    public synchronized void reset() throws IOException {
        in.reset();
        clear();
    }

    /** Test main */
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        JPanel top = new JPanel();
        final StatusPanel statusPanel = new StatusPanel();
        final ProgressBarUtil progressBarUtil = statusPanel.getProgressBar();
        top.setLayout(new BorderLayout());
        top.add(statusPanel, BorderLayout.NORTH);
        frame.getContentPane().add(top, BorderLayout.NORTH);

        JPanel bot = new JPanel();
        final JButton busyButton = new JButton("Look Busy");
        final JButton startButton = new JButton("Start Reading");
        final JButton stopButton = new JButton("Stop");
        bot.add(busyButton);
        bot.add(startButton);
        bot.add(stopButton);
        frame.getContentPane().add(bot, BorderLayout.SOUTH);

        // local test class
        class TestListener implements ActionListener {

            ProgressBarFilterInputStream stream;
            URL url;

            public TestListener() {
                try {
                    url = new URL("file:/etc/hosts");
                }
                catch (Exception e) {
                    DialogUtil.error(e);
                    System.exit(1);
                }
            }

            public void actionPerformed(ActionEvent e) {
                Object w = e.getSource();
                if (w == busyButton) {
                    if (stream != null) {
                        stream.interrupt();
                        stream = null;
                    }
                    progressBarUtil.startAnimation();
                }
                else if (w == startButton) {
                    if (stream != null) {
                        stream.interrupt();
                        stream = null;
                    }
                    stream = new ProgressBarFilterInputStream(statusPanel, url);
                    SwingWorker worker = new SwingWorker() {

                        public Object construct() {
                            while (true) {
                                try {
                                    //thread.sleep(1);
                                    if (stream.read() == -1)
                                        return null;
                                }
                                catch (Exception ex) {
                                    return null;
                                }
                            }
                        }
                    };
                    worker.start();
                }
                else if (w == stopButton) {
                    if (stream != null) {
                        stream.interrupt();
                        stream = null;
                    }
                    else {
                        progressBarUtil.stopAnimation();
                    }
                }
            }
        }
        TestListener tl = new TestListener();
        busyButton.addActionListener(tl);
        startButton.addActionListener(tl);
        stopButton.addActionListener(tl);

        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}
