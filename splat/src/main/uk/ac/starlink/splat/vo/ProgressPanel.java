/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * Copyright 2007 Particle Physics and Astronomy Research Council.
 * Copyright 2009 Science and Technology Facilities Council.
 *
 * History:
 *    12-JAN-2007 (Peter W. Draper):
 *       Original version based on Jsky ProgressPanel.
 */

package uk.ac.starlink.splat.vo;

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

import jsky.util.gui.StatusPanel;

/**
 * A panel to display while a download or other background operation is in
 * progress.
 * <p>
 * This class is designed to be usable from any threadand all GUI access is
 * done synchronously in the event dispatching thread.
 *
 * @version $Revision$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class ProgressPanel
    extends JPanel
    implements ActionListener
{
    /** The title string */
    protected String title;

    /** Displays the title */
    protected JLabel titleLabel;

    /** Button to interrupt the task */
    protected JButton stopButton;

    /** Displays the progress bar and status text */
    protected StatusPanel statusPanel;

    /** Set to true if the stop button was pressed */
    protected boolean interrupted = false;

    /** Used to create a new progress panel in the event dispatching thread */
    protected static ProgressPanel newPanel;


    /**
     * Initialize a progress panel with the given title string.
     *
     * @param the title string
     */
    public ProgressPanel( String title ) 
    {
        this.title = title;
        init();
    }

    /**
     * Initialize the progress panel. This method may be called from any
     * thread, but will always run in the event dispatching thread.
     */
    protected void init() 
    {
        //  Make sure this is done in the event dispatch thread.
        if ( !SwingUtilities.isEventDispatchThread() ) {
            invokeAndWait( new Runnable() 
                {
                    public void run() 
                    {
                        init();
                    }
                });
            return;
        }

        setLayout( new BorderLayout() );
        setBorder( BorderFactory.createEtchedBorder() );
        JPanel top = new JPanel();
        top.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        top.setLayout( new BorderLayout() );
        titleLabel = new JLabel( title, SwingConstants.CENTER );
        titleLabel.setForeground( Color.black );
        top.add( titleLabel, BorderLayout.WEST );

        stopButton = new JButton( "Stop" );
        stopButton.addActionListener( this );
        top.add( stopButton, BorderLayout.EAST );

        statusPanel = new StatusPanel();
        statusPanel.getTextField().setColumns( 25 );

        add( top, BorderLayout.NORTH );
        add( statusPanel, BorderLayout.SOUTH );
    }


    /** 
     * Run the given Runnable synchronously in the event dispatching thread. 
     */
    protected static void invokeAndWait( Runnable r ) 
    {
        try {
            SwingUtilities.invokeAndWait( r );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the title string.
     */
    public void setTitle( final String title ) 
    {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            SwingUtilities.invokeLater( new Runnable() 
                {
                    public void run() 
                    {
                        setTitle( title );
                    }
                });
            return;
        }
        this.title = title;
        titleLabel.setText( title );
    }

    /** 
     * Log or display the given message.
     */
    public void logMessage( final String msg ) 
    {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            SwingUtilities.invokeLater( new Runnable() 
                {
                    public void run() 
                    {
                        statusPanel.setText( msg );
                    }
                });
            return;
        }
        statusPanel.setText( msg );
    }

    /** 
     * Set the status text to display. 
     */
    public void setText( final String s ) 
    {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            SwingUtilities.invokeLater( new Runnable() 
                {
                    public void run() 
                    {
                        statusPanel.setText( s );
                    }
                });
            return;
        }
        statusPanel.setText( s );
    }


    /** 
     * Add a listener to be called when the user presses the stop button. 
     */
    public void addActionListener( ActionListener l ) 
    {
        stopButton.addActionListener( l );
    }

    /**
     * Called when the Stop button is pressed.
     */
    public void actionPerformed( ActionEvent e )
    {
        interrupted = true;
        stop();
    }

    /**
     * Return true if the stop button was pressed.
     */
    public boolean isInterrupted()
    {
        return interrupted;
    }

    /**
     * Display the progress panel. This method may be called from any
     * thread, but will always run in the event dispatching thread.
     */
    public void start()
    {
        //  Make sure this is done in the event dispatch thread.
        if ( !SwingUtilities.isEventDispatchThread() ) {
            SwingUtilities.invokeLater( new Runnable()
                {
                    public void run()
                    {
                        start();
                    }
                });
            return;
        }
        interrupted = false;
        statusPanel.getProgressBar().startAnimation();
    }


    /**
     * Stop displaying the progress panel. This method may be called
     * from any thread, but will always run in the event dispatching
     * thread.
     */
    public void stop()
    {
        //  Make sure this is done in the event dispatch thread.
        if ( !SwingUtilities.isEventDispatchThread() ) {
            SwingUtilities.invokeLater( new Runnable()
                {
                    public void run()
                    {
                        stop();
                    }
                });
            return;
        }

        statusPanel.interrupt();
        statusPanel.getProgressBar().stopAnimation();
        statusPanel.getProgressBar().setStringPainted( false );
        statusPanel.getProgressBar().setValue( 0 );

        stopButton.setText( "Done" );
        stopButton.setEnabled( false );
    }


    /**
     * Set the percent done. A 0 value resets the bar and hides the percent
     * value.
     */
    public void setProgress( final int percent )
    {
        if ( !SwingUtilities.isEventDispatchThread() ) {
            SwingUtilities.invokeLater( new Runnable()
                {
                    public void run()
                    {
                        statusPanel.setProgress( percent );
                    }
                });
            return;
        }
        statusPanel.setProgress(percent);
    }
}
