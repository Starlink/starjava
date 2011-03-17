package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
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
public class UwsJobPanel extends JPanel {

    private final DefaultListModel listModel_;
    private final JList jlist_;
    private final DetailPanel detail_;
    private final Map<UwsJob,Runnable> phaseWatcherMap_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public UwsJobPanel() {
        super( new BorderLayout() );
        listModel_ = new DefaultListModel();
        phaseWatcherMap_ = new HashMap<UwsJob,Runnable>();

        /* Set up JList of jobs. */
        jlist_ = new JList( listModel_ );
        jlist_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        jlist_.setCellRenderer( new UwsJobCellRenderer() );

        /* Set up detail panel. */
        detail_ = new DetailPanel();

        /* Fix it so the detail panel responds to selections in the list. */
        jlist_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                detail_.setJob( (UwsJob) jlist_.getSelectedValue() );
            }
        } );

        /* Arrange them in this component. */
        JPanel listContainer = new JPanel( new BorderLayout() );
        listContainer.add( new JScrollPane( jlist_ ), BorderLayout.CENTER );
        listContainer.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "Job List" ) );
        JPanel detailContainer = new JPanel( new BorderLayout() );
        detailContainer.add( new JScrollPane( detail_ ), BorderLayout.CENTER );
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
     * Signals that the state of a job may have changed and should be
     * reflected in the UUI.
     *
     * @param   job   job to update
     */
    private void updateJob( UwsJob job ) {
        int iJob = listModel_.indexOf( job );
        if ( iJob >= 0 ) {
            listModel_.set( iJob, job );
        }
        if ( detail_.getJob() == job ) {
            detail_.update();
        }
    }

    /**
     * Panel which displays the details for a single UWS job.
     */
    private class DetailPanel extends JPanel {

        private final JTextField urlField_;
        private final JLabel phaseLabel_;
        private final Action abortAction_;
        private final Action deleteAction_;
        private UwsJob job_;

        /**
         * Constructor.
         */
        public DetailPanel() {
            super( new BorderLayout() );
            JComponent main = Box.createVerticalBox();
            add( main, BorderLayout.NORTH );

            JComponent urlLine = Box.createHorizontalBox();
            urlLine.add( new JLabel( "URL: " ) );
            urlField_ = new JTextField();
            urlField_.setEditable( false );
            urlLine.add( urlField_ );
            main.add( urlLine );

            JComponent phaseLine = Box.createHorizontalBox();
            phaseLine.add( new JLabel( "Phase: " ) );
            phaseLabel_ = new JLabel();
            phaseLine.add( phaseLabel_ );
            phaseLine.add( Box.createHorizontalGlue() );
            main.add( phaseLine );

            deleteAction_ = new AbstractAction( "Delete" ) {
                public void actionPerformed( ActionEvent evt ) {
                    final UwsJob job = job_;
                    removeJob( job );
                    new Thread() {
                        public void run() {
                            job.attemptDelete();
                        }
                    }.start();
                }
            };

            abortAction_ = new AbstractAction( "Abort" ) {
                public void actionPerformed( ActionEvent evt ) {
                    final UwsJob job = job_;
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
            };

            JComponent controlLine = Box.createHorizontalBox();
            controlLine.add( new JLabel( "Actions: " ) );
            controlLine.add( new JButton( abortAction_ ) );
            controlLine.add( Box.createHorizontalStrut( 10 ) );
            controlLine.add( new JButton( deleteAction_ ) );
            controlLine.add( Box.createHorizontalGlue() );
            main.add( controlLine );
        }

        /**
         * Returns the job currently displayed.  May be null.
         *
         * @return   displayed job
         */
        public UwsJob getJob() {
            return job_;
        }

        /**
         * Sets the job to be displayed.  May be null.
         *
         * @param   job  job to display
         */
        public void setJob( UwsJob job ) {
            job_ = job;
            update();
        }

        /**
         * Ensures that the GUI is up to date.
         */
        public void update() {
            urlField_.setText( job_ == null ? null
                                            : job_.getJobUrl().toString() );
            urlField_.setCaretPosition( 0 );
            phaseLabel_.setText( job_ == null ? null : job_.getLastPhase() );
            deleteAction_.setEnabled( job_ != null );
            abortAction_.setEnabled( job_ != null &&
                                     UwsStage.forPhase( job_.getLastPhase() )
                                         != UwsStage.FINISHED );
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
