package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Panel which displays the details for a single UWS job.
 *
 * @author   Mark Taylor
 * @since    17 Mar 2011
 */
public class UwsJobPanel extends JPanel {

    private final JTextField urlField_;
    private final JLabel phaseLabel_;
    private UwsJob job_;

    /**
     * Constructor.
     */
    public UwsJobPanel() {
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
        urlField_.setText( job_ == null ? null
                                        : job_.getJobUrl().toString() );
        urlField_.setCaretPosition( 0 );
        updatePhase();
    }

    /**
     * Ensures that the GUI is up to date.
     */
    public void updatePhase() {
        phaseLabel_.setText( job_ == null ? null : job_.getLastPhase() );
    }
}
