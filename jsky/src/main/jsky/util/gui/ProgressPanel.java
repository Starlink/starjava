/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ProgressPanel.java,v 1.7 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jsky.util.Resources;
import jsky.util.StatusLogger;


/**
 * A panel to display while a download or other background operation is in
 * progress.
 * <p>
 * This class is designed to be usable from any threadand all GUI access is done
 * synchronously in the event dispatching thread.
 *
 * @version $Revision: 1.7 $
 * @author Allan Brighton
 */
public class ProgressPanel extends JPanel implements ActionListener, StatusLogger {

    /** Parent of this window (frame or internal frame), used to close the window */
    protected Component parent;

    /** The title string*/
    protected String title;

    /** Displays the title */
    protected JLabel titleLabel;

    /** Displays the active GIF icon */
    protected JLabel iconLabel;

    /** Button to interrupt the task */
    protected JButton stopButton;

    /** Displays the progress bar and status text */
    protected StatusPanel statusPanel;

    /** If set, this is the current input stream being monitored */
    protected ProgressBarFilterInputStream loggedInputStream;

    /** Set to true if the stop button was pressed */
    protected boolean interrupted = false;

    /** Used to create a new progress panel in the event dispatching thread */
    protected static ProgressPanel newPanel;


    /**
     * Initialize a progress panel with the given title string.
     *
     * @param parent the parent frame or internal frame, used to close the window
     * @param the title string
     */
    public ProgressPanel(Component parent, String title) {
        this.parent = parent;
        this.title = title;
        init();
    }

    /** Default constructor */
    public ProgressPanel() {
        this(null, "Download in Progress...");
    }


    /**
     * Initialize the progreass panel. This method may be called from any
     * thread, but will always run in the event dispatching thread.
     */
    protected void init() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            invokeAndWait(new Runnable() {

                public void run() {
                    init();
                }
            });
            return;
        }
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        JPanel top = new JPanel();
        top.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        top.setLayout(new BorderLayout());
        titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Color.black);
        top.add(titleLabel, BorderLayout.WEST);
        iconLabel = new JLabel(Resources.getIcon("TaskStatusOn.gif"));
        top.add(iconLabel, BorderLayout.EAST);

        JPanel center = new JPanel();
        stopButton = new JButton("Stop");
        stopButton.addActionListener(this);
        center.add(stopButton);
        top.add(center, BorderLayout.SOUTH);

        statusPanel = new StatusPanel();
        statusPanel.getTextField().setColumns(25);

        add(top, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);
    }


    /** Run the given Runnable synchronously in the event dispatching thread. */
    protected static void invokeAndWait(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the parent frame or internal frame, used to close the window
     */
    public void setParent(Component parent) {
        this.parent = parent;
    }

    /**
     * Set the title string.
     */
    public void setTitle(final String title) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setTitle(title);
                }
            });
            return;
        }
        this.title = title;
        titleLabel.setText(title);
    }

    /** Log or display the given message */
    public void logMessage(final String msg) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    statusPanel.setText(msg);
                }
            });
            return;
        }
        statusPanel.setText(msg);
    }

    /** Set the status text to display. */
    public void setText(final String s) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    statusPanel.setText(s);
                }
            });
            return;
        }
        statusPanel.setText(s);
    }


    /** Add a listener to be called when the user presses the stop button. */
    public void addActionListener(ActionListener l) {
        stopButton.addActionListener(l);
    }

    /** Return the status panel (displays the progress bar and text field). */
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    /**
     * Called when the Stop button is pressed.
     */
    public void actionPerformed(ActionEvent e) {
        interrupted = true;
        stop();
    }

    /** Return true if the stop button was pressed */
    public boolean isInterrupted() {
        return interrupted;
    }

    /**
     * Return a connection to the given URL and log messages before and after
     * opening the connection.
     */
    public URLConnection openConnection(URL url) throws IOException {
        start();
        URLConnection connection = statusPanel.openConnection(url);
        if (interrupted)
            throw new ProgressException("Interrupted");
        return connection;
    }


    /**
     * Display the progress panel. This method may be called from any
     * thread, but will always run in the event dispatching thread.
     */
    public void start() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    start();
                }
            });
            return;
        }

        interrupted = false;
        if (parent instanceof JFrame)
            ((JFrame) parent).setState(Frame.NORMAL);
        parent.setVisible(true);
        statusPanel.getProgressBar().startAnimation();
        BusyWin.setBusy(true, parent);
    }


    /**
     * Stop displaying the progress panel. This method may be called
     * from any thread, but will always run in the event dispatching
     * thread.
     */
    public void stop() {
        // make sure this is done in the event dispatch thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    stop();
                }
            });
            return;
        }

        BusyWin.setBusy(false, parent);
        if (loggedInputStream != null) {
            loggedInputStream.interrupt();
            loggedInputStream = null;
        }
        parent.setVisible(false);
        statusPanel.interrupt();
        statusPanel.getProgressBar().stopAnimation();
        statusPanel.setText("");
        statusPanel.getProgressBar().setStringPainted(false);
        statusPanel.getProgressBar().setValue(0);
    }


    /**
     * Make a ProgressPanel and frame (or internal frame) and return the panel.
     *
     * @param title the title string
     * @param window window to display the dialog over, may be null
     */
    public static ProgressPanel makeProgressPanel(final String title, final Component window) {
        if (!SwingUtilities.isEventDispatchThread()) {
            invokeAndWait(new Runnable() {

                public void run() {
                    newPanel = ProgressPanel.makeProgressPanel(title, window);
                }
            });
            return newPanel;
        }

        // get the parent frame so that the dialog won't be hidden behind it
        Frame parent = null;
        JDesktopPane desktop = DialogUtil.getDesktop();
        if (desktop != null)
            parent = SwingUtil.getFrame(desktop);
        else
            parent = SwingUtil.getFrame(window);

        ProgressPanelDialog f = new ProgressPanelDialog(title, parent);
        f.show();
        return f.getProgressPanel();
    }


    /**
     * Make a ProgressPanel and frame (or internal frame) and return the panel.
     *
     * @param title the title string
     */
    public static ProgressPanel makeProgressPanel(String title) {
        return makeProgressPanel(title, null);
    }


    /**
     * Make a ProgressPanel and frame (or internal frame) and return the panel.
     *
     * @param the title string
     */
    public static ProgressPanel makeProgressPanel() {
        return makeProgressPanel("Downloading data...");
    }

    /** Set the percent done. A 0 value resets the bar and hides the percent value. */
    public void setProgress(final int percent) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    statusPanel.setProgress(percent);
                }
            });
            return;
        }
        statusPanel.setProgress(percent);
    }


    /**
     * Return a input stream that will generate log messages showing
     * the progress of the read from the given stream.
     *
     * @param in the input stream to be monitored
     * @param size the size in bytes of the date to be read, or 0 if not known
     */
    public ProgressBarFilterInputStream getLoggedInputStream(InputStream in, int size) throws IOException {
        if (interrupted) {
            throw new ProgressException("Interrupted");
        }
        loggedInputStream = statusPanel.getLoggedInputStream(in, size);
        return loggedInputStream;
    }


    /**
     * Return an input stream to use for reading from the given URL
     * that will generate log messages showing the progress of the read.
     *
     * @param url the URL to read
     */
    public ProgressBarFilterInputStream getLoggedInputStream(URL url) throws IOException {
        if (interrupted) {
            throw new ProgressException("Interrupted");
        }
        loggedInputStream = statusPanel.getLoggedInputStream(url);
        return loggedInputStream;
    }

    /**
     * Stop logging reads from the input stream returned from an
     * earlier call to getLoggedInputStream().
     *
     * @param in an input stream returned from getLoggedInputStream()
     */
    public void stopLoggingInputStream(ProgressBarFilterInputStream in) throws IOException {
        loggedInputStream = null;
        statusPanel.stopLoggingInputStream(in);
    }
}

