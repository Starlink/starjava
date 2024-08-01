package uk.ac.starlink.vo;

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Panel displaying UWS jobs currently known about by the TAP load dialogue.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2011
 */
public class UwsJobListPanel extends JPanel {

    private final DefaultListModel<UwsJob> listModel_;
    private final JList<UwsJob> jlist_;
    private final UwsJobPanel detail_;
    private final JTextField urlField_;
    private final Action deleteAction_;
    private final Action abortAction_;
    private final JToggleButton.ToggleButtonModel delOnExitModel_;
    private final Map<UwsJob,UwsJob.JobWatcher> jobWatcherMap_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public UwsJobListPanel() {
        super( new BorderLayout() );
        listModel_ = new DefaultListModel<UwsJob>();
        jobWatcherMap_ = new HashMap<UwsJob,UwsJob.JobWatcher>();

        /* Set up JList of jobs. */
        jlist_ = new JList<UwsJob>( listModel_ );
        jlist_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        jlist_.setCellRenderer( new UwsJobCellRenderer() );

        /* Job display components. */
        detail_ = new UwsJobPanel( false );
        urlField_ = new JTextField();
        urlField_.setEditable( false );

        /* Fix it so the detail panel responds to selections in the list. */
        jlist_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                UwsJob job = jlist_.getSelectedValue();
                detail_.setJob( job );
                urlField_.setText( job == null ? null
                                               : job.getJobUrl().toString() );
                updateActions();
            }
        } );

        /* Set up actions for job control. */
        deleteAction_ = new AbstractAction( "Delete" ) {
            public void actionPerformed( ActionEvent evt ) {
                final UwsJob job = detail_.getJob();
                if ( job != null ) {
                    removeJob( job );
                    new Thread() {
                        public void run() {
                            job.attemptDelete();
                        }
                    }.start();
                }
                else {
                    assert false;
                }
            }
        };
        abortAction_ = new AbstractAction( "Abort" ) {
            public void actionPerformed( ActionEvent evt ) {
                final UwsJob job = detail_.getJob();
                if ( job != null ) {
                    new Thread() {
                        public void run() {
                            try {
                                job.postPhase( "ABORT" );
                            }
                            catch ( IOException e ) {
                                logger_.warning( "ABORT failed for job "
                                               + job );
                            }
                        }
                    }.start();
                }
                else {
                    assert false;
                }
            }
        };
        delOnExitModel_ = new JToggleButton.ToggleButtonModel();
        delOnExitModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                boolean delOnExit = delOnExitModel_.isSelected();
                UwsJob job = detail_.getJob();
                if ( job != null ) {
                    if ( job.getDeleteOnExit() != delOnExit ) {
                        job.setDeleteOnExit( delOnExit );
                    }
                }
            }
        } );
        JToggleButton delOnExitButton = new JCheckBox( "Delete On Exit" );
        delOnExitButton.setModel( delOnExitModel_ );

        /* Arrange the controls. */
        JComponent controlLine = Box.createHorizontalBox();
        controlLine.add( Box.createHorizontalGlue() );
        controlLine.add( new JButton( abortAction_ ) );
        controlLine.add( Box.createHorizontalStrut( 10 ) );
        controlLine.add( new JButton( deleteAction_ ) );
        controlLine.add( Box.createHorizontalStrut( 20 ) );
        controlLine.add( delOnExitButton );
        controlLine.add( Box.createHorizontalGlue() );
        updateActions();

        /* Place the subcomponents. */
        JPanel listContainer = new JPanel( new BorderLayout() );
        listContainer.add( new JScrollPane( jlist_ ), BorderLayout.CENTER );
        listContainer.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Job List" ) );
        JComponent urlLine = Box.createHorizontalBox();
        urlLine.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ) );
        urlLine.add( new JLabel( "URL: " ) );
        urlLine.add( urlField_ );
        controlLine.setBorder( BorderFactory.createEmptyBorder( 5, 0, 5, 0 ) );
        JPanel detailContainer = new JPanel( new BorderLayout() );
        detailContainer.add( new JScrollPane( detail_ ), BorderLayout.CENTER );
        detailContainer.add( urlLine, BorderLayout.NORTH );
        detailContainer.add( controlLine, BorderLayout.SOUTH );
        detailContainer.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Job Details" ) );
        JSplitPane splitter =
            new JSplitPane( JSplitPane.VERTICAL_SPLIT,
                            listContainer, detailContainer );
        add( splitter, BorderLayout.CENTER );
    }

    /**
     * Adds a new job to the list.
     *
     * @param  job   job to add
     * @param  select  true iff the new job should be automatically selected
     */
    public void addJob( UwsJob job, boolean select ) {
        listModel_.addElement( job );
        if ( select ) {
            jlist_.setSelectedValue( job, true );
        }
        UwsJob.JobWatcher watcher = new UwsJob.JobWatcher() {
            public void jobUpdated( final UwsJob uj, UwsJobInfo info ) {
                scheduleUpdateJobInfo( uj, info );
            }
        };
        jobWatcherMap_.put( job, watcher );
        job.addJobWatcher( watcher );
    }

    /**
     * Removes a previously-added job from the list.
     * It's harmless to call it on jobs which are not present.
     *
     * @param  job  job to remove
     */
    public void removeJob( UwsJob job ) {
        if ( listModel_.removeElement( job ) ) {
            UwsJob.JobWatcher watcher = jobWatcherMap_.remove( job );
            job.removeJobWatcher( watcher );
        }
        if ( detail_.getJob() == job ) {
            detail_.setJob( null );
        }
    }

    /**
     * Returns an array of jobs currently visible in this panel.
     *
     * @return  visible jobs
     */
    public UwsJob[] getJobs() {
        List<UwsJob> jobList = new ArrayList<UwsJob>();
        for ( int i = 0; i < listModel_.getSize(); i++ ) {
            jobList.add( listModel_.getElementAt( i ) );
        }
        return jobList.toArray( new UwsJob[ 0 ] );
    }

    /**
     * Reload job detail information from the server for the currently
     * displayed job.
     */
    public void reload() {
        final UwsJob job = detail_.getJob();
        if ( job != null ) {
            Thread phaser = new Thread( "UWS Phase reader" ) {
                public void run() {
                    final UwsJobInfo info;
                    try {
                        info = job.readInfo();
                    }
                    catch ( IOException e ) {
                        logger_.warning( "Phase read fail for UWS job "
                                       + job.getJobUrl() + ": " + e );
                        return;
                    }
                    scheduleUpdateJobInfo( job, info );
                }
            };
            phaser.setDaemon( true );
            phaser.start();
        }
    }

    /**
     * Signals that the state of a job may have changed and should be
     * reflected in the UI.  This method may be called from any thread.
     *
     * @param   job   job to update
     * @param   info   new job status
     */
    private void scheduleUpdateJobInfo( final UwsJob job,
                                        final UwsJobInfo info) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if ( detail_.getJob() == job ) {
                    detail_.setJobInfo( info );
                    updateActions();
                }
                int ijob = listModel_.indexOf( job );
                if ( ijob >= 0 ) {
                    ListDataEvent evt =
                        new ListDataEvent( UwsJobListPanel.this,
                                           ListDataEvent.CONTENTS_CHANGED,
                                           ijob, ijob );
                    for ( ListDataListener l :
                          listModel_.getListDataListeners() ) {
                        l.contentsChanged( evt );
                    }
                }
            }
        } );
    }

    private void updateActions() {
        UwsJob job = detail_.getJob();
        if ( job == null ) {
            deleteAction_.setEnabled( false );
            abortAction_.setEnabled( false );
            delOnExitModel_.setEnabled( false );
            delOnExitModel_.setSelected( false );
        }
        else {
            UwsJobInfo info = job.getLastInfo();
            boolean isFinished =
                info != null &&
                UwsStage.forPhase( info.getPhase() ) == UwsStage.FINISHED;
            deleteAction_.setEnabled( true );
            abortAction_.setEnabled( ! isFinished );
            delOnExitModel_.setEnabled( true );
            delOnExitModel_.setSelected( job.getDeleteOnExit() );
        }
    }

    /**
     * Custom cell renderer implementation for UwsJob objects.
     */
    private static class UwsJobCellRenderer extends DefaultListCellRenderer {
        private Color basicColor_;
        private Color finishedColor_;
        public Component getListCellRendererComponent( JList<?> jlist,
                                                       Object value, int ix,
                                                       boolean isSel,
                                                       boolean hasFocus ) {
            if ( basicColor_ == null ) {
                basicColor_ = getForeground();
                finishedColor_ = Color.GRAY;
            }
            super.getListCellRendererComponent( jlist, value, ix, isSel,
                                                hasFocus );
            if ( value instanceof UwsJob ) {
                UwsJob uwsJob = (UwsJob) value;
                UwsJobInfo info = uwsJob == null ? null : uwsJob.getLastInfo();
                String phase = info == null ? null : info.getPhase();
                UwsStage stage = UwsStage.forPhase( phase );
                setText( phase + ":  " + uwsJob.getJobUrl() );
                setForeground( stage == UwsStage.FINISHED
                             ? finishedColor_
                             : basicColor_ );
            }
            return this;
        }
    }
}
