package uk.ac.starlink.topcat;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/**
 * Panel that presents name-value pairs to the user,
 * where the values are short or medium-length strings.
 * The strings may be wrapped over multiple screen lines,
 * but will not be scrolled.
 *
 * @author   Mark Taylor
 * @since    30 Apr 2019
 */
public class TextItemPanel extends JPanel {

    private final GridBagLayout layer_;
    private int iline_;

    @SuppressWarnings("this-escape")
    public TextItemPanel() {
        layer_ = new GridBagLayout();
        setLayout( layer_ );
    }

    /**
     * Adds a name-value pair.
     *
     * @param  name  presentation name
     * @param  text  text value
     */
    public void addItem( String name, String text ) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridy = iline_++;
        cons.gridx = 0;
        cons.anchor = GridBagConstraints.NORTHWEST;
        cons.fill = GridBagConstraints.NONE;
        cons.weightx = 0;
        cons.weighty = 1;
        cons.insets = new Insets( 2, 5, 2, 5 );
        JLabel label = new JLabel( name + ":" );
        layer_.setConstraints( label, cons );
        add( label );
        cons.gridx = 1;
        cons.anchor = GridBagConstraints.WEST;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.weightx = 1;
        cons.insets = new Insets( 2, 5, 2, 5 );
        JTextArea field = new JTextArea();
        field.setText( text );
        field.setCaretPosition( 0 );
        field.setEditable( false );
        field.setLineWrap( true );
        field.setWrapStyleWord( true );
        field.setBackground( UIManager
                            .getColor( "TextField.inactiveBackground" ) );
        field.setBorder( UIManager.getBorder( "TextField.border" ) );
        layer_.setConstraints( field, cons );
        add( field );
    }
}
