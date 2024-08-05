package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLUtils;

/**
 * Panel used as part of the TAP load dialogue to resume execution of or
 * recover results from previously submitted asynchronous TAP queries.
 *
 * @author   Mark Taylor
 * @since    17 Mar 2011
 */
class ResumeTapQueryPanel extends JPanel {

    private final TapTableLoadDialog tld_;
    private final UwsJobPanel jobPanel_;
    private final JComponent mainBox_;
    private final JTextField urlField_;
    private final Action resumeAct_;
    private final Action loadResultAct_;
    private final Timer phaseTimer_;
    private Thread fetcher_;

    /**
     * Constructor.
     *
     * @param  tld  load dialogue of which this forms part
     */
    public ResumeTapQueryPanel( TapTableLoadDialog tld ) {
        super( new BorderLayout() );
        tld_ = tld;

        /* Set up components for the user to enter a URL corresponding
         * to a running TAP query. */
        urlField_ = new JTextField();
        jobPanel_ = new UwsJobPanel( true );
        Action clearAct = new AbstractAction( "Clear" ) {
            public void actionPerformed( ActionEvent evt ) {
                setStatus( "" );
                urlField_.setText( "" );
            }
        };
        final Action jobAct = new AbstractAction( "View" ) {
            public void actionPerformed( ActionEvent evt ) {
                final String urlTxt = urlField_.getText();
                final UwsJob job;
                try {
                    viewJob( new UwsJob( URLUtils.newURL( urlTxt ) ) );
                }
                catch ( MalformedURLException e ) {
                    assert false;
                    return;
                }
            }
        };
        urlField_.addActionListener( jobAct );
        urlField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                String txt = urlField_.getText();
                if ( txt != null && txt.trim().length() > 0 ) {
                    try {
                        URLUtils.newURL( urlField_.getText() );
                        jobAct.setEnabled( true );
                        return;
                    }
                    catch ( MalformedURLException e ) {
                    }
                }
                jobAct.setEnabled( false );
            }
        } );

        /* Set up actions which cause the currently displayed job to be
         * resumed.  These work using a slightly devious callback because
         * of the way that the TableLoadDialogue framework is set up.
         * They invoke the submit action on the parent TableLoadDialogue,
         * which in turn causes this class's createTableLoader method to
         * get called. */
        final ActionEvent dummyEvt = new ActionEvent( this, 0, "resume" );
        resumeAct_ = new AbstractAction( "Resume" ) {
            public void actionPerformed( ActionEvent evt ) {
                tld_.getSubmitAction().actionPerformed( dummyEvt );
                urlField_.setText( "" );
                setJob( null );
            }
        };
        loadResultAct_ = new AbstractAction( "Load Result" ) {
            public void actionPerformed( ActionEvent evt ) {
                tld_.getSubmitAction().actionPerformed( dummyEvt );
                urlField_.setText( "" );
                setJob( null );
            }
        };

        /* Set up a timer which will periodically update the status
         * of any job displayed in this panel. */
        phaseTimer_ = new Timer( 4000, new ActionListener() {
            private volatile boolean isRunning;
            private UwsJob lastJob;
            public void actionPerformed( ActionEvent evt ) {
                final UwsJob job = jobPanel_.getJob();
                if ( job != lastJob ) {
                    isRunning = false;
                    lastJob = job;
                }
                if ( job != null ) {
                    new Thread( "UWS job phase reader " + job.getJobUrl() ) {
                        public void run() {
                            if ( jobPanel_.getJob() == job && ! isRunning ) {
                                String errMsg = null;
                                isRunning = true;
                                UwsJobInfo info = null;
                                try {
                                    info = job.readInfo();
                                }
                                catch ( FileNotFoundException e ) {
                                    errMsg = "Job has been deleted";
                                }
                                catch ( IOException e ) {
                                    errMsg = e.toString();
                                }
                                finally {
                                    isRunning = false;
                                }
                                final UwsJobInfo info0 = info;
                                final String errMsg0 = errMsg;
                                SwingUtilities.invokeLater( new Runnable() {
                                    public void run() {
                                        if ( errMsg0 != null ) {
                                            setStatus( errMsg0 );
                                            setJob( null );
                                        }
                                        else {
                                            setJobState( job, info0 );
                                        }
                                    }
                                } );
                            }
                        }
                    }.start();
                }
            }
        } );
        phaseTimer_.setRepeats( true );
        phaseTimer_.setInitialDelay( 0 );
        phaseTimer_.setCoalesce( true );
        jobPanel_.setJob( null );

        /* Place components. */
        JComponent urlBox = Box.createVerticalBox();
        JComponent txtLine = Box.createHorizontalBox();
        txtLine.add( new JLabel( "Enter URL of "
                               + "previously submitted TAP Job" ) );
        txtLine.add( Box.createHorizontalGlue() );
        urlBox.add( txtLine );
        urlBox.add( Box.createVerticalStrut( 5 ) );
        JComponent entryLine = Box.createHorizontalBox();
        entryLine.add( new JLabel( "URL: " ) );
        entryLine.add( urlField_ );
        urlBox.add( entryLine );
        urlBox.add( Box.createVerticalStrut( 5 ) );
        JComponent ubuttLine = Box.createHorizontalBox();
        ubuttLine.add( Box.createHorizontalGlue() );
        ubuttLine.add( new JButton( clearAct ) );
        ubuttLine.add( Box.createHorizontalStrut( 5 ) );
        ubuttLine.add( new JButton( jobAct ) );
        urlBox.add( ubuttLine );
        JComponent controlBox = Box.createHorizontalBox();
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( resumeAct_ ) );
        controlBox.add( Box.createHorizontalStrut( 10 ) );
        controlBox.add( new JButton( loadResultAct_ ) );
        controlBox.add( Box.createHorizontalGlue() );
        mainBox_ = new JPanel( new BorderLayout() );
        mainBox_.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Previously Submitted Job" ) );
        urlBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        controlBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        add( urlBox, BorderLayout.NORTH );
        add( mainBox_, BorderLayout.CENTER );
        add( controlBox, BorderLayout.SOUTH );

        /* Initialise state. */
        setStatus( null );
    }

    /**
     * This method is called to perform table loading based on any
     * TAP query which is currently visible in this panel.
     *
     * @return  table loader based on current state
     */
    public TableLoader createTableLoader() {
        final boolean deleteOnExit = false;

        /* Prepare a TAP query based on the currently visible job, if any. */
        final UwsJob job = jobPanel_.getJob();
        if ( job == null ) {
            return null;
        }
        UwsJobInfo info = job.getLastInfo();
        String phase = info == null ? null : info.getPhase();
        UwsStage stage = UwsStage.forPhase( phase );
        final String summary = "Resumed TAP Query";

        /* If the query is completed, return a loader which loads the table
         * result right away. */
        if ( "COMPLETED".equals( phase ) ) {
            if ( deleteOnExit ) {
                job.setDeleteOnExit( true );
            }
            return new TableLoader() {
                public TableSequence loadTables( StarTableFactory tfact )
                        throws IOException {
                    StarTable table =
                        TapQuery.getResult( job, tld_.getContentCoding(),
                                            tfact.getStoragePolicy() );
                    return Tables.singleTableSequence( table );
                }
                public String getLabel() {
                    return summary;
                }
            };
        }

        /* If the query has not completed yet, return a loader which will
         * wait for completion and then load the result.  While running,
         * the query will be visible in the Running Jobs tab.
         * The query will be started first if necessary. */
        else if ( stage == UwsStage.UNSTARTED || stage == UwsStage.RUNNING ) {
            final boolean unstarted = stage == UwsStage.UNSTARTED;
            job.setDeleteOnExit( deleteOnExit );
            return new TableLoader() {
                public TableSequence loadTables( StarTableFactory tfact )
                        throws IOException {
                    if ( unstarted ) {
                        job.start();
                    }
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            tld_.addRunningQuery( job );
                        }
                    } );
                    StarTable table;
                    try {
                       table = TapQuery
                              .waitForResult( job, tld_.getContentCoding(),
                                              tfact.getStoragePolicy(), 4000 );
                    }
                    catch ( InterruptedException e ) {
                        throw (IOException)
                              new InterruptedIOException( "Interrupted" )
                             .initCause( e );
                    }
                    return Tables.singleTableSequence( table );
                }
                public String getLabel() {
                    return summary;
                }
            };
        }

        /* In any other state, or in absence of a job or phase, we can't
         * load anything. */
        else {

            /* This shouldn't happen, since the relevant actions are
             * disabled. */
            assert false;
            return null;
        }
    }

    /**
     * Reloads information from the server about the currently displayed
     * job, if any.
     */
    public void reload() {
        String urlTxt = urlField_.getText();
        if ( urlTxt != null ) {
            try {
                viewJob( new UwsJob( URLUtils.newURL( urlTxt ) ) );
            }
            catch ( MalformedURLException e ) {
            }
        }
    }

    /**
     * Causes a given UWS job to have its details retrieved and displayed.
     * The details will be updated periodically.
     *
     * @param  job  job to display
     */
    private void viewJob( final UwsJob job ) {
        String urlTxt = job.getJobUrl().toString();
        setStatus( "Examining Job " + urlTxt + "..." );
        fetcher_ = new Thread( "read UWS phase " + urlTxt ) {
            public void run() {
                final Thread fetcher = this;
                final UwsJobInfo info;
                try {
                    info = job.readInfo();
                }
                catch ( IOException e ) {
                    final String msg = e instanceof FileNotFoundException
                                     ? "No such job"
                                     : e.toString();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( fetcher_ == fetcher ) {
                                setStatus( "Job not available: " + msg );
                            }
                        }
                    } );
                    return;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( fetcher == fetcher_ ) {
                            setStatus( null );
                            setJob( job );
                        }
                    }
                } );
            }
        };
        fetcher_.setDaemon( true );
        fetcher_.start();
    }

    /**
     * Updates the state of the GUI displaying the job.  The job display
     * itself and any relevant actions are updated.
     *
     * @param   job  job
     * @param   info   job state
     */
    private void setJobState( UwsJob job, UwsJobInfo info ) {
        if ( job == jobPanel_.getJob() ) {
            jobPanel_.setJobInfo( info );
            String phase = info == null ? null : info.getPhase();
            UwsStage stage = UwsStage.forPhase( phase );
            if ( stage == UwsStage.FINISHED ) {
                phaseTimer_.stop();
                resumeAct_.setEnabled( false );
                if ( "COMPLETED".equals( phase ) ) {
                    loadResultAct_.setEnabled( true );
                }
            }
            else if ( stage == UwsStage.UNSTARTED ||
                      stage == UwsStage.RUNNING ||
                      stage == UwsStage.UNKNOWN ) {
                resumeAct_.setEnabled( true );
                loadResultAct_.setEnabled( false );
            }
            else {
                loadResultAct_.setEnabled( false );
                resumeAct_.setEnabled( false );
            }
        }
    }

    /**
     * Displays either an error message or information about the current job.
     * If <code>msg</code> is non-null, it is displayed like an error,
     * otherwise the current job is visible.
     *
     * @param  msg   error-type message, or null to show job status
     */
    private void setStatus( String msg ) {
        mainBox_.removeAll();
        if ( msg == null ) {
            mainBox_.add( jobPanel_, BorderLayout.CENTER );
        }
        else {
            JComponent errBox = Box.createHorizontalBox();
            errBox.add( Box.createHorizontalGlue() );
            errBox.add( new JLabel( msg ) );
            errBox.add( Box.createHorizontalGlue() );
            mainBox_.add( errBox, BorderLayout.CENTER );
        }
        mainBox_.revalidate();
        mainBox_.repaint();
    }

    /**
     * Sets the current job, and updates the GUI accordingly.
     *
     * @param  job  new job to display; may be null
     */
    private void setJob( UwsJob job ) {
        if ( jobPanel_.getJob() != null ) {
            phaseTimer_.stop();
        }
        jobPanel_.setJob( job );
        UwsJobInfo info = job == null ? null : job.getLastInfo();
        setJobState( job, info );
        if ( jobPanel_.getJob() != null ) {
            phaseTimer_.start();
        }
    }
}
