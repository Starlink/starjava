package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

/**
 * Wraps a component so that it assumes the same size as its container.
 * Another installment in my gruelling battle against layout managers.
 *
 * @author   Mark Taylor
 * @since    1 Nov 2005
 */
public class SizeWrapper extends JPanel {

    /**
     * Constructor.
     *
     * @param  comp  component to wrap
     */
    @SuppressWarnings("this-escape")
    public SizeWrapper( Component comp ) {
        SpringLayout layer = new SpringLayout();
        setLayout( layer );
        add( comp );
        layer.putConstraint( SpringLayout.WEST, this, 0,
                             SpringLayout.WEST, comp );
        layer.putConstraint( SpringLayout.EAST, this, 0,
                             SpringLayout.EAST, comp );
        layer.putConstraint( SpringLayout.NORTH, this, 0,
                             SpringLayout.NORTH, comp );
        layer.putConstraint( SpringLayout.SOUTH, this, 0,
                             SpringLayout.SOUTH, comp );
    }
}
