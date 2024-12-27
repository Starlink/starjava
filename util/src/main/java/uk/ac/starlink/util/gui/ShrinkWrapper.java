package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Provides a container for a component whose maximum size is the same as 
 * its preferred size.  This preferred size is the same as the preferred
 * size of the contained component.  This is useful for components 
 * which get stretched in an unwelcome way, for instance a JComboBox 
 * in a BoxLayout.
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
    @SuppressWarnings("this-escape")
    public ShrinkWrapper( Component component ) {
        super( new FlowLayout( FlowLayout.CENTER, 0, 0 ) );
        component_ = component;
        add( component );
        setBorder( BorderFactory.createEmptyBorder() );
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
