package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.util.IOUtils;

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
    private final JComponent paramPanel_;
    private final JComponent errorPanel_;
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
        phaseField_ = new ValueField();
        idField_ = new ValueField();
        runField_ = new ValueField();
        ownerField_ = new ValueField();
        durationField_ = new ValueField();
        startField_ = new ValueField();
        endField_ = new ValueField();
        destructionField_ = new ValueField();
        paramPanel_ = Box.createVerticalBox();
        errorPanel_ = Box.createVerticalBox();

        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "URL", urlField_ );
        stack.addLine( "Phase", phaseField_ );
        stack.addLine( "Job ID", idField_ );
        stack.addLine( "Run ID", runField_ );
        stack.addLine( "Owner ID", ownerField_ );
        stack.addLine( "Max Duration", durationField_ );
        stack.addLine( "Start Time", startField_ );
        stack.addLine( "End Time", endField_ );
        stack.addLine( "Destruction Time", destructionField_ );
        stack.addLine( "Parameters", paramPanel_ );
        stack.addLine( "Errors", errorPanel_ );
        main.add( stack );
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
            setJobInfo( null, null );
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
            setJobInfo( null, null );
        }
        else {
            updateJobInfo( job_ );
        }
        revalidate();
    }

    /**
     * Turns the detail message from an error (from the job/error resource)
     * into a string for display.  May be overridden by concrete subclasses.
     *
     * @param  errDetail  error detail content in bytes
     * @return  error detail string for display
     */
    protected String formatErrorDetail( byte[] errDetail ) {
        try {
            Reader rdr = new BufferedReader(
                             new InputStreamReader(
                                 new ByteArrayInputStream( errDetail ) ) );
            StringBuffer sbuf = new StringBuffer();
            for ( char c; ( c = (char) rdr.read() ) >= 0; ) {
                sbuf.append( c );
            }
            return sbuf.toString();
        }
        catch ( IOException e ) {
            logger_.warning( "Unexpected string processing error: " + e );
            return "";
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
                    UwsJobInfo.Error error = jobInfo.getError();
                    byte[] errDetail = null;
                    if ( error != null && error.hasDetail() ) {
                        try {
                            ByteArrayOutputStream bout =
                                new ByteArrayOutputStream();
                            IOUtils.copy( new URL( url + "/error" )
                                         .openStream(), bout );
                            errDetail = bout.toByteArray();
                        }
                        catch ( IOException e ) {
                            logger_.warning( "Couldn't get error details: "
                                           + e );
                        }
                    }
                    final byte[] errDetail1 = errDetail;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( job == job_ ) {
                                setJobInfo( jobInfo, errDetail1 );
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
     * @param  bytes containing additional error detail message if any,
     *         as retrieved from the job-id/error resource
     */
    private void setJobInfo( UwsJobInfo jobInfo, byte[] errDetail ) {
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

            UwsJobInfo.Parameter[] params = jobInfo.getParameters();
            JComponent pBox = Box.createVerticalBox();
            for ( int ip = 0; ip < params.length; ip++ ) {
                UwsJobInfo.Parameter param = params[ ip ];
                ValueField pField = new ValueField( true );
                pField.setText( param.getValue() );
                pField.setEmph( param.isByReference() );
                pBox.add( createTitledField( param.getId(), pField ) );
            }
            paramPanel_.removeAll();
            paramPanel_.add( pBox );

            UwsJobInfo.Error error = jobInfo.getError();
            JComponent eBox = Box.createVerticalBox();
            if ( error != null ) {
                ValueField messageField = new ValueField( true );
                messageField.setText( error.getMessage() );
                eBox.add( createTitledField( "Message", messageField ) );
                if ( errDetail != null && errDetail.length > 0 ) {
                    ValueField detailField = new ValueField( true );
                    detailField.setText( formatErrorDetail( errDetail ) );
                    eBox.add( createTitledField( "Detail", detailField ) );
                }
            }
            errorPanel_.removeAll();
            errorPanel_.add( eBox );
        }
        revalidate();
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
        JComponent box = Box.createVerticalBox();
        JComponent titleLine = Box.createHorizontalBox();
        titleLine.add( new JLabel( title + ": " ) );
        titleLine.add( Box.createHorizontalGlue() );
        box.add( titleLine );
        JComponent fieldLine = Box.createHorizontalBox();
        fieldLine.add( Box.createHorizontalStrut( 20 ) );
        fieldLine.add( field );
        box.add( fieldLine );
        return box;
    }

    /**
     * Utility class for display of textual information.
     */
    private static class ValueField extends Box {
        private final JTextComponent textField_;
        private Font baseFont_;

        /**
         * Constructs a single-line field.
         */
        public ValueField() {
            this( false );
        }

        /**
         * Constructs a single- or multi-line field.
         *
         * @param  multiLine  true for multiple lines
         */
        public ValueField( boolean multiLine ) {
            super( BoxLayout.X_AXIS );
            textField_ = multiLine ? new JTextArea() : new JTextField();
            textField_.setEditable( false );
            textField_.setBorder( BorderFactory.createEmptyBorder() );
            textField_.setOpaque( false );
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

        /**
         * Sets whether this text should be emphasised (perhaps italicised).
         *
         * @param  emph  true for emphasis
         */
        public void setEmph( boolean emph ) {
            if ( baseFont_ == null ) {
                baseFont_ = textField_.getFont();
            }
            textField_.setFont( emph ? baseFont_.deriveFont( Font.ITALIC )
                                     : baseFont_ );
        }
    }
}
