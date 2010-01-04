/*
 * Copyright (C) 2009 Science and Technology Facilities Council.
 *
 *  History:
 *     19-NOV-2009 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import uk.ac.starlink.splat.util.Utilities;

/**
 * Frame for displaying a ProgressPanel, so that a long running process can be
 * monitored by stopping, starting and changing the title and message. Note
 * that the instance is never disposed and this may not appear unless the
 * process takes more than TIMER_DELAY to do the job.
 */
public class ProgressFrame
    extends JFrame
{
    private ProgressPanel progressPanel = new ProgressPanel( "Progress..." );
    private boolean makeVisible = false;
    private Timer timer = null;
    private static int TIMER_DELAY = 10000; // 10 seconds.

    /**
     * Create a top level window populated with a ProgressPanel.
     * Note this remains invisible until after a call to start().
     *
     * @param title the component title.
     */
    public ProgressFrame( String title )
    {
        initUI();
        initFrame( title );
    }

    protected void initUI()
    {
        getContentPane().setLayout( new BorderLayout() );

        JPanel mainPanel = new JPanel( new BorderLayout() );
        getContentPane().add( progressPanel, BorderLayout.CENTER );

        //  Just one action, so we want this on all the time.
        progressPanel.setDisableOnStop( false );
    }

    // Initialise frame properties. Note remains invisible until start().
    protected void initFrame( String title )
    {
        setTitle( Utilities.getTitle( title ) );
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        pack();
        makeVisible = false;
        setVisible( false );
    }

    /**
     * Stop all progress panel.
     */
    public void stop()
    {
        synchronized( this ) {
            makeVisible = false;
            progressPanel.stop();
            setVisible( false );
        }
    }

    /**
     * Start the progress panel.
     */
    public void start()
    {
        synchronized( this ) {
            if ( ! isVisible() ) {
                makeVisible = true;
                progressPanel.start();
                eventuallyMakeVisible();
            }
        }
    }

    /**
     * Make visible after a while, but only if not made invisible in the
     * intervening period.
     */
    protected void eventuallyMakeVisible()
    {
        //  In fact this timer run continually and will check makeVisible
        //  on the next timed event.
        if ( timer == null ) {
            timer =
                new Timer( TIMER_DELAY,
                    new ActionListener()
                    {
                        public void actionPerformed( ActionEvent e )
                        {
                            synchronized( this ) {
                                if ( makeVisible ) {
                                    setVisible( true );
                                    makeVisible = false;
                                }
                            }
                        }
                    });
            timer.start();
        }
        else {
            //  Reinitialise timer so that we get the full TIMER_DELAY each
            //  request for visibility. XXX may need a Timer per request if
            //  this means the frame never appears for multiple repeated
            //  requests.
            timer.restart();
        }
    }

    /**
     * Set the progress panel title.
     */
    public void setTitle( String title )
    {
        progressPanel.setTitle( title );
    }

    /**
     * Set the progress panel message.
     */
    public void setMessage( String message )
    {
        progressPanel.setText( message );
    }

    /**
     * Have we been told to stop? If so the controller class should arrange to
     * halt the related thread.
     */
    public boolean isInterrupted()
    {
        synchronized( this ) {
           return progressPanel.isInterrupted();
        }
    }
}
