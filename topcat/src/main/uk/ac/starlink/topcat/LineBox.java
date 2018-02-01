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

    /**
     * Constructs a box with a label, body component,
     * and optional small vertical gap below it.
     *
     * @param  label   text label
     * @param  comp    arbitrary component
     * @param  postGap  true for trailing vertical pad
     */
    public LineBox( String label, JComponent comp, boolean postGap ) {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        if ( label != null ) {
            add( new JLabel( label + ": " ) );
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
}
