package uk.ac.starlink.vo;

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Panel displaying UWS jobs currently known about by the TAP load dialogue.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2011
 */
public class UwsJobListPanel extends JPanel {

    private final DefaultListModel listModel_;
    private final JList jlist_;
    private final UwsJobPanel detail_;
    private final Action deleteAction_;
    private final Action abortAction_;
    private final JToggleButton.ToggleButtonModel delOnExitModel_;
    private final Map<UwsJob,Runnable> phaseWatcherMap_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public UwsJobListPanel() {
        super( new BorderLayout() );
        listModel_ = new DefaultListModel();
        phaseWatcherMap_ = new HashMap<UwsJob,Runnable>();

        /* Set up JList of jobs. */
        jlist_ = new JList( listModel_ );
        jlist_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        jlist_.setCellRenderer( new UwsJobCellRenderer() );

        /* Set up detail panel. */
        detail_ = new UwsJobPanel();

        /* Fix it so the detail panel responds to selections in the list. */
        jlist_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                detail_.setJob( (UwsJob) jlist_.getSelectedValue() );
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
        JPanel detailContainer = new JPanel( new BorderLayout() );
        detailContainer.add( new JScrollPane( detail_ ), BorderLayout.CENTER );
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
    public void addJob( final UwsJob job, boolean select ) {
        listModel_.addElement( job );
        if ( select ) {
            jlist_.setSelectedValue( job, true );
        }
        Runnable watcher = new Runnable() {
            public void run() {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        updateJob( job );
                        if ( job == detail_.getJob() ) {
                            updateActions();
                        }
                    }
                } );
            }
        };
        phaseWatcherMap_.put( job, watcher );
        job.addPhaseWatcher( watcher );
    }

    /**
     * Removes a previously-added job from the list.
     * It's harmless to call it on jobs which are not present.
     *
     * @param  job  job to remove
     */
    public void removeJob( UwsJob job ) {
        if ( listModel_.removeElement( job ) ) {
            Runnable watcher = phaseWatcherMap_.remove( job );
            job.removePhaseWatcher( watcher );
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
            Object item = listModel_.getElementAt( i );
            if ( item instanceof UwsJob ) {
                jobList.add( (UwsJob) item );
            }
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
                    try {
                        job.readPhase();
                    }
                    catch ( IOException e ) {
                        logger_.warning( "Phase read fail for UWS job "
                                       + job.getJobUrl() + ": " + e );
                        return;
                    }
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            updateJob( job );
                        }
                    } );
                }
            };
            phaser.setDaemon( true );
            phaser.start();
        }
    }

    /**
     * Signals that the state of a job may have changed and should be
     * reflected in the UI.
     *
     * @param   job   job to update
     */
    private void updateJob( UwsJob job ) {
        int iJob = listModel_.indexOf( job );
        if ( iJob >= 0 ) {
            listModel_.set( iJob, job );
        }
        if ( detail_.getJob() == job ) {
            detail_.updatePhase();
        }
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
            deleteAction_.setEnabled( true );
            abortAction_.setEnabled( UwsStage.forPhase( job.getLastPhase() )
                                         != UwsStage.FINISHED );
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
        public Component getListCellRendererComponent( JList jlist,
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
                String phase = uwsJob.getLastPhase();
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
