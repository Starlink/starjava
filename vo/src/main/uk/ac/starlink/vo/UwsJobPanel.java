package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Panel which displays the details for a single UWS job.
 *
 * @author   Mark Taylor
 * @since    17 Mar 2011
 */
public class UwsJobPanel extends JPanel {

    private final ValueField urlField_;
    private final ValueField phaseField_;
    private final ValueField idField_;
    private final ValueField runField_;
    private final ValueField ownerField_;
    private final ValueField durationField_;
    private final ValueField startField_;
    private final ValueField endField_;
    private final ValueField destructionField_;
    private UwsJob job_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public UwsJobPanel() {
        super( new BorderLayout() );
        JComponent main = Box.createVerticalBox();
        add( main, BorderLayout.NORTH );

        urlField_ = new ValueField();
        main.add( createTitledField( "URL", urlField_ ) );
        phaseField_ = new ValueField();
        main.add( createTitledField( "Phase", phaseField_ ) );
        idField_ = new ValueField();
        main.add( createTitledField( "Job ID", idField_ ) );
        runField_ = new ValueField();
        main.add( createTitledField( "Run ID", runField_ ) );
        ownerField_ = new ValueField();
        main.add( createTitledField( "Owner ID", ownerField_ ) );
        durationField_ = new ValueField();
        main.add( createTitledField( "Max Duration", durationField_ ) );
        startField_ = new ValueField();
        main.add( createTitledField( "Start Time", startField_ ) );
        endField_ = new ValueField();
        main.add( createTitledField( "End Time", endField_ ) );
        destructionField_ = new ValueField();
        main.add( createTitledField( "Destruction Time", destructionField_ ) );
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
        if ( job_ == null ) {
            urlField_.setText( null );
            setJobInfo( null );
        }
        else {
            urlField_.setText( job.getJobUrl().toString() );
        }
        updatePhase();
    }

    /**
     * Ensures that the GUI is up to date.
     */
    public void updatePhase() {
        phaseField_.setText( job_ == null ? null : job_.getLastPhase() );
        if ( job_ == null ) {
            setJobInfo( null );
        }
        else {
            updateJobInfo( job_ );
        }
    }

    /**
     * Triggers an asynchronous read of the job state from the
     * server followed by a corresponding update of the GUI.
     * May be called from any thread. 
     *
     * @param   job   job whose details are to be acquired
     */
    private void updateJobInfo( final UwsJob job ) {
        final URL url = job.getJobUrl();
        Thread jobReader = new Thread( "Read job " + job ) {
            public void run() {
                UwsJobInfo info;
                try {
                    info = job.readJob();
                }
                catch ( Exception e ) {
                    logger_.warning( "Couldn't get job details: " + e );
                    info = null;
                }
                final UwsJobInfo jobInfo = info;
                if ( jobInfo != null && job == job_ ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( job == job_ ) {
                                setJobInfo( jobInfo );
                            }
                        }
                    } );
                }
            }
        };
        jobReader.setDaemon( true );
        jobReader.start();
    }

    /**
     * Updates the display with a given job info object, which may be null.
     *
     * @param  jobInfo  job information
     */
    private void setJobInfo( UwsJobInfo jobInfo ) {
        if ( jobInfo == null ) {
            idField_.setText( null );
            runField_.setText( null );
            ownerField_.setText( null );
            durationField_.setText( null );
            startField_.setText( null );
            endField_.setText( null );
            destructionField_.setText( null );
        }
        else {
            idField_.setText( jobInfo.getJobId() );
            runField_.setText( jobInfo.getRunId() );
            ownerField_.setText( jobInfo.getOwnerId() );
            String duration = jobInfo.getExecutionDuration();
            if ( duration != null && duration.matches( "\\s*0+\\s*" ) ) {
                duration = "unlimited";
            }
            durationField_.setText( duration );
            startField_.setText( jobInfo.getStartTime() );
            endField_.setText( jobInfo.getEndTime() );
            destructionField_.setText( jobInfo.getDestruction() );
        }
    }

    /**
     * Creates and returns a component which includes a title and a
     * given component.
     *
     * @param  title  title text
     * @param  field  component to entitle
     */
    private static JComponent createTitledField( String title,
                                                 JComponent field ) {
        JComponent line = Box.createHorizontalBox();
        line.add( new JLabel( title + ": " ) );
        line.add( field );
        return line;
    }

    /**
     * Utility class for display of textual information.
     */
    private static class ValueField extends Box {
        private final JTextField textField_;

        /**
         * Constructor.
         */
        public ValueField() {
            super( BoxLayout.X_AXIS );
            textField_ = new JTextField();
            textField_.setEditable( false );
            textField_.setBorder( BorderFactory.createEmptyBorder() );
            add( textField_ );
        }

        /**
         * Sets the text displayed by this field.
         *
         * @param  txt  text
         */
        public void setText( String txt ) {
            textField_.setText( txt );
            textField_.setCaretPosition( 0 );
        }
    }
}
