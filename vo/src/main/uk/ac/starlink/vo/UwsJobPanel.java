package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

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
    private final ValueField errorField_;
    private final JComponent paramPanel_;
    private UwsJob job_;

    /**
     * Constructor.
     *
     * @param  includeUrl   true to include the job URL field in the display
     */
    @SuppressWarnings("this-escape")
    public UwsJobPanel( boolean includeUrl ) {
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
        errorField_ = new ValueField( true );
        paramPanel_ = Box.createVerticalBox();

        Stack stack = new Stack();
        if ( includeUrl ) {
            stack.addItem( "URL", urlField_ );
        }
        stack.addItem( "Phase", phaseField_ );
        stack.addItem( "Job ID", idField_ );
        stack.addItem( "Run ID", runField_ );
        stack.addItem( "Owner ID", ownerField_ );
        stack.addItem( "Max Duration", durationField_ );
        stack.addItem( "Start Time", startField_ );
        stack.addItem( "End Time", endField_ );
        stack.addItem( "Destruction Time", destructionField_ );
        stack.addItem( "Error", errorField_ );
        stack.addItem( "Parameters", paramPanel_ );
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
        }
        else {
            urlField_.setText( job.getJobUrl().toString() );
        }
        setJobInfo( job == null ? null : job.getLastInfo() );
    }

    /**
     * Updates the display with a given job info object, which may be null.
     *
     * @param  jobInfo  job information
     */
    public void setJobInfo( UwsJobInfo jobInfo ) {
        if ( jobInfo == null ) {
            phaseField_.setText( null );
            idField_.setText( null );
            runField_.setText( null );
            ownerField_.setText( null );
            durationField_.setText( null );
            startField_.setText( null );
            endField_.setText( null );
            destructionField_.setText( null );
            errorField_.setText( null );
            paramPanel_.removeAll();
        }
        else {
            phaseField_.setText( jobInfo.getPhase() );
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
            if ( params == null ) {
                params = new UwsJobInfo.Parameter[ 0 ];
            }
            Stack pStack = new Stack();
            pStack.setBorder( params.length > 0
                            ? BorderFactory.createEtchedBorder()
                            : BorderFactory.createEmptyBorder() );
            for ( int ip = 0; ip < params.length; ip++ ) {
                UwsJobInfo.Parameter param = params[ ip ];
                ValueField pField = new ValueField( true );
                pField.setText( param.getValue() );
                pField.setEmph( param.isByReference() );
                pStack.addItem( param.getId(), pField );
            }
            paramPanel_.removeAll();
            paramPanel_.add( pStack );

            UwsJobInfo.Error error = jobInfo.getError();
            errorField_.setText( error == null ? null : error.getMessage() );
        }
        revalidate();
    }

    /**
     * Override to a no-op.
     *
     * <p>I don't understand why, but if I don't do this, when the component
     * is in a JScrollPane, every time it's refreshed (setJobInfo) it
     * jerkily scrolls to the bottom of the panel.
     * Possibly something to do with the hated GridBagLayout.
     */
    @Override
    public void scrollRectToVisible( Rectangle rect ) {
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
     * Utility component to lay out two columns of components aligned.
     */
    private static class Stack extends JPanel {
        private final GridBagLayout layer_;
        private final GridBagConstraints cons_;

        /**
         * Constructor.
         */
        public Stack() {
            super( new GridBagLayout() );
            layer_ = (GridBagLayout) getLayout();
            cons_ = new GridBagConstraints();
            cons_.gridy = 0;
        }

        /**
         * Adds a labelled item to this panel.
         *
         * @param  title  label string
         * @param  field  item to display under label
         */
        public void addItem( String title, JComponent field ) {
            if ( cons_.gridy > 0 ) {
                cons_.gridx = 0;
                Component strut = Box.createVerticalStrut( 4 );
                layer_.setConstraints( strut, cons_ );
                add( strut );
                cons_.gridy++;
            }

            JComponent titleLabel = new JLabel( title + ": " );
            GridBagConstraints cons1 = (GridBagConstraints) cons_.clone();
            cons1.gridx = 0;
            cons1.anchor = GridBagConstraints.NORTHWEST;
            layer_.setConstraints( titleLabel, cons1 );
            add( titleLabel );

            GridBagConstraints cons2 = (GridBagConstraints) cons_.clone();
            cons2.gridx = 1;
            cons2.anchor = GridBagConstraints.NORTHWEST;
            cons2.weightx = 1.0;
            cons2.fill = GridBagConstraints.HORIZONTAL;
            cons2.gridwidth = GridBagConstraints.REMAINDER;
            layer_.setConstraints( field, cons2 );
            add( field );

            cons_.gridy++;
        }
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
            if ( multiLine ) {
                textField_.setOpaque( false );
            }
            else {
                textField_.setBorder( BorderFactory.createEmptyBorder() );
            }
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
