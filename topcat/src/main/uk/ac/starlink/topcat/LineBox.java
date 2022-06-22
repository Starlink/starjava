package uk.ac.starlink.topcat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Layout utility component that places components in a horizontal Box.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 * @see      javax.swing.Box
 */
public class LineBox extends JPanel {

    private final JLabel jlabel_;
    private final JComponent comp_;

    /**
     * Constructs a box with a label, body component,
     * and optional small vertical gap below it.
     *
     * @param  label   text label
     * @param  comp    arbitrary component
     * @param  postGap  true for trailing vertical pad
     */
    public LineBox( String label, JComponent comp, boolean postGap ) {
        comp_ = comp;
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        jlabel_ = label == null ? null : new JLabel( label + ": " );
        if ( jlabel_ != null ) {
            add( jlabel_ );
        }
        if ( comp != null ) {
            add( comp );
        }
        add( Box.createHorizontalGlue() );
        if ( postGap ) {
            setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ) );
        }
    }

    /**
     * Constructs a box with a label, body component and no trailing gap.
     *
     * @param  label   text label
     * @param  comp    arbitrary component
     */
    public LineBox( String label, JComponent comp ) {
        this( label, comp, false );
    }

    /**
     * Constructs a box with just a body component.
     *
     * @param  comp   arbitrary component
     */
    public LineBox( JComponent comp ) {
        this( null, comp );
    }

    /**
     * Returns the JLabel component of this line box.
     *
     * @return  label component, may be null
     */
    public JLabel getLabel() {
        return jlabel_;
    }

    /**
     * Returns the labelled component part of this line box.
     *
     * @return  component presented at construction time, may be null
     */
    public JComponent getComponent() {
        return comp_;
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        if ( comp_ != null ) {
            comp_.setEnabled( isEnabled );
        }
        if ( jlabel_ != null ) {
            jlabel_.setEnabled( isEnabled );
        }
    }
}
