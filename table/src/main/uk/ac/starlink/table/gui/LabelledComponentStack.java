package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
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
    private List labels = new ArrayList();
    private List fields = new ArrayList();

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

        /* Add some vertical padding except for the first added line. */
        if ( cons.gridy > 0 ) {
            cons.gridx = 0;
            Component strut = Box.createVerticalStrut( 4 );
            layer.setConstraints( strut, cons );
            add( strut );
            cons.gridy++;
        }

        /* Add the name. */
        Component nameComp = new JLabel( name + ":  " );
        GridBagConstraints cons1 = (GridBagConstraints) cons.clone();
        cons1.gridx = 0;
        cons1.anchor = GridBagConstraints.EAST;
        layer.setConstraints( nameComp, cons1 );
        add( nameComp );

        /* Add the padding string, if there is one. */
        Component padComp = null;
        if ( pad != null ) {
           GridBagConstraints cons2 = (GridBagConstraints) cons.clone();
           padComp = new JLabel( pad + "  " );
           padComp.setFont( inputFont );
           cons2.gridx = 1;
           cons2.anchor = GridBagConstraints.EAST;
           layer.setConstraints( padComp, cons2 );
           add( padComp );
        }

        /* Add the query component. */
        GridBagConstraints cons3 = (GridBagConstraints) cons.clone();
        cons3.gridx = 2;
        cons3.anchor = GridBagConstraints.WEST;
        cons3.weightx = 1.0;

        /* JTextFields like a horizontal fill so that they can expand to
         * the size avaiable.  This isn't appropriate (it's ugly) for
         * most other components. */
        cons3.fill = ( comp instanceof JTextField ) 
                   ? GridBagConstraints.HORIZONTAL
                   : GridBagConstraints.NONE;
        cons3.gridwidth = GridBagConstraints.REMAINDER;
        layer.setConstraints( comp, cons3 );
        add( comp );
        
        /* Bump line index. */
        cons.gridy++;

        /* Record the components placed. */
        labels.add( nameComp );
        fields.add( comp );
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

    /**
     * Returns an array of labels which have been added.  Each corresponds
     * to the <tt>name</tt> argument of an <tt>addLine</tt> call.
     *
     * @return  an array of JLabels, one for each line
     */
    public JLabel[] getLabels() {
        return (JLabel[]) labels.toArray( new JLabel[ 0 ] );
    }

    /**
     * Returns an array of the components which have been added.
     * Each corresponds to the <tt>comp</tt> argument of an <tt>addLine</tt>
     * call.
     *
     * @return   an array of field components, one for each line
     */
    public Component[] getFields() {
        return (Component[]) fields.toArray( new Component[ 0 ] );
    }

}
