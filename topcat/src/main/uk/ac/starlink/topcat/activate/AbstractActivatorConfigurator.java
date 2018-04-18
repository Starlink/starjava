package uk.ac.starlink.topcat.activate;

import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import uk.ac.starlink.topcat.ActionForwarder;

/**
 * Skeleton implementation of ActivatorConfigurator.
 *
 * @author   Mark Taylor
 * @since    29 Jan 2018
 */
public abstract class AbstractActivatorConfigurator
                      implements ActivatorConfigurator {

    private final JComponent panel_;
    private final ActionForwarder forwarder_;

    /**
     * Constructor.
     *
     * @param  panel  configuration panel
     */
    protected AbstractActivatorConfigurator( JComponent panel ) {
        panel_ = panel;
        forwarder_ = new ActionForwarder();
    }

    /**
     * Returns the panel supplied at construction time.
     */
    public JComponent getPanel() {
        return panel_;
    }

    public void addActionListener( ActionListener l ) {
        forwarder_.addActionListener( l );
    }

    public void removeActionListener( ActionListener l ) {
        forwarder_.removeActionListener( l );
    }

    /**
     * Returns the action forwarder which should be added as a listener
     * to any GUI components that can affect the configuration.
     *
     * @return  forwarder
     */
    protected ActionForwarder getActionForwarder() {
        return forwarder_;
    }

    /**
     * Utility method to add an uneditable JTextField for displaying text.
     * This is quite like a JLabel, but the text can be cut'n'pasted.
     *
     * @return  new text field
     */
    public static JTextField createDisplayField() {
        JTextField field = new JTextField() {
            public void setText( String txt ) {
                super.setText( txt );
                super.setCaretPosition( 0 );
            }
        };
        field.setEditable( false );
        field.setBorder( BorderFactory.createEmptyBorder() );
        return field;
    }

    /**
     * Utility method to add a JLabel for displaying text.
     *
     * @return  new label
     */
    public static JLabel createDisplayLabel() {
        JLabel label = new JLabel();
        label.setFont( UIManager.getFont( "TextField.font" ) );
        return label;
    }
}
