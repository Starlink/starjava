package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A component which contains a list of (name, component) pairs.
 * The components will typically be text fields or other things 
 * into which the user can enter input; the names labels for these.
 * Alignment is taken care of.
 * This class is a convenience for a number of widgets which need
 * to do the same sort of thing.
 *
 * @author   Mark Taylor (Starlink)
 */
public class LabelledComponentStack extends JPanel {

    private Font inputFont = new JTextField().getFont();
    private GridBagLayout layer = new GridBagLayout();
    private GridBagConstraints cons = new GridBagConstraints();

    /**
     * Constructs a new stack of input fields.
     */
    public LabelledComponentStack() {
        setLayout( layer );
        cons.gridy = 0;
    }

    /**
     * Adds a (name,component) pair with an optional padding string.
     *
     * @param  name  the component label, which gets displayed as a JLabel
     * @param  pad   an extra string which is aligned between the
     *         name and component
     * @param  comp  the component 
     */
    public void addLine( String name, String pad, Component comp ) {

        /* Add the name. */
        Component nameComp = new JLabel( name + ":  " );
        cons.gridx = 0;
        cons.anchor = GridBagConstraints.EAST;
        layer.setConstraints( nameComp, cons );
        add( nameComp );

        /* Add the padding string, if there is one. */
        Component padComp = null;
        if ( pad != null ) {
           padComp = new JLabel( pad + "  " );
           padComp.setFont( inputFont );
           cons.gridx = 1;
           cons.anchor = GridBagConstraints.EAST;
           layer.setConstraints( padComp, cons );
           add( padComp );
        }

        /* Add the query component. */
        cons.gridx = 2;
        cons.anchor = GridBagConstraints.WEST;
        layer.setConstraints( comp, cons );
        add( comp );
        
        /* Bump line index. */
        cons.gridy++;
    }
   
    /**
     * Adds a (name,component) pair.
     *
     * @param  name  the component label, which gets displayed as a JLabel
     * @param  comp  the component 
     */
    public void addLine( String name, Component comp ) {
        addLine( name, null, comp );
    }

    /**
     * Adds a (name,value) pair.  The value is a string which will be
     * presented as a JLabel or something.
     */
    public void addLine( String name, String value ) {
        addLine( name, new JLabel( value ) );
    }

    /**
     * Returns the font used for dialog boxes.
     *
     * @return font
     */
    public Font getInputFont() {
        return inputFont;
    }

}
