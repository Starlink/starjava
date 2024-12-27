package uk.ac.starlink.util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Provides a container for a component that extends its maximum vertical size.
 * This can be useful for aligning (e.g.) components in a horizontal Box.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2017
 */
public class TallWrapper extends JPanel {

    private final Component comp_;

    /**
     * Constructor.
     *
     * @param   comp  the component to be wrapped
     */
    @SuppressWarnings("this-escape")
    public TallWrapper( Component comp ) {
        super( new BorderLayout() );
        comp_ = comp;
        add( comp, BorderLayout.CENTER );
        setBorder( BorderFactory.createEmptyBorder() );
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension( comp_.getMaximumSize().width, Short.MAX_VALUE );
    }
}
