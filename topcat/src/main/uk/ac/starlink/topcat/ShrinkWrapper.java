package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JPanel;

/**
 * Holds a component so that its maximum size is the same as its
 * preferred size.  This is useful for components which get stretched
 * in an unwelcome way, for instance a JComboBox in a BoxLayout.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2005
 */
public class ShrinkWrapper extends JPanel {

    private final Component component_;

    /**
     * Constructs a shrink wrapper.
     *
     * @param  component  the component to be wrapped
     */
    public ShrinkWrapper( Component component ) {
        super( new FlowLayout( FlowLayout.CENTER, 0, 0 ) );
        component_ = component;
        add( component );
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
