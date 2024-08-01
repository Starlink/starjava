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

    private final Font inputFont_;
    private final GridBagLayout layer_;
    private final GridBagConstraints cons_;
    private final List<JLabel> labels_;
    private final List<Component> fields_;

    /**
     * Constructs a new stack of input fields.
     */
    @SuppressWarnings("this-escape")
    public LabelledComponentStack() {
        inputFont_ = new JTextField().getFont();
        layer_ = new GridBagLayout();
        cons_ = new GridBagConstraints();
        labels_ = new ArrayList<JLabel>();
        fields_ = new ArrayList<Component>();
        setLayout( layer_ );
        cons_.gridy = 0;
    }

    /**
     * Adds a (name,component) pair with an optional padding string
     * and default xfill.
     *
     * @param  name  the component label, which gets displayed as a JLabel
     * @param  pad   an extra string which is aligned between the
     *         name and component
     * @param  comp  the component 
     */
    public void addLine( String name, String pad, Component comp ) {
        addLine( name, pad, comp, comp instanceof JTextField );
    }


    /**
     * Adds a (name,component) pair with an optional padding string and
     * explicit xfill.
     *
     * @param  name  the component label, which gets displayed as a JLabel
     * @param  pad   an extra string which is aligned between the
     *         name and component
     * @param  comp  the component 
     * @param  xfill  true iff the component should be stretched to the
     *                full available width
     */
    public void addLine( String name, String pad, Component comp,
                         boolean xfill ) {

        /* Add some vertical padding except for the first added line. */
        if ( cons_.gridy > 0 ) {
            cons_.gridx = 0;
            Component strut = Box.createVerticalStrut( 4 );
            layer_.setConstraints( strut, cons_ );
            add( strut );
            cons_.gridy++;
        }

        /* Add the name. */
        JLabel nameComp = new JLabel( name == null ? null : ( name + ":  " ) );
        GridBagConstraints cons1 = (GridBagConstraints) cons_.clone();
        cons1.gridx = 0;
        cons1.anchor = GridBagConstraints.EAST;
        layer_.setConstraints( nameComp, cons1 );
        add( nameComp );

        /* Add the padding string, if there is one. */
        Component padComp = null;
        if ( pad != null ) {
           GridBagConstraints cons2 = (GridBagConstraints) cons_.clone();
           padComp = new JLabel( pad + "  " );
           padComp.setFont( inputFont_ );
           cons2.gridx = 1;
           cons2.anchor = GridBagConstraints.EAST;
           layer_.setConstraints( padComp, cons2 );
           add( padComp );
        }

        /* Add the query component. */
        GridBagConstraints cons3 = (GridBagConstraints) cons_.clone();
        cons3.gridx = 2;
        cons3.anchor = GridBagConstraints.WEST;
        cons3.weightx = 1.0;
        cons3.fill = xfill ? GridBagConstraints.HORIZONTAL
                           : GridBagConstraints.NONE;
        cons3.gridwidth = GridBagConstraints.REMAINDER;
        layer_.setConstraints( comp, cons3 );
        add( comp );
        
        /* Bump line index. */
        cons_.gridy++;

        /* Record the components placed. */
        labels_.add( nameComp );
        fields_.add( comp );
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
        return inputFont_;
    }

    /**
     * Returns an array of labels which have been added.  Each corresponds
     * to the <code>name</code> argument of an <code>addLine</code> call.
     *
     * @return  an array of JLabels, one for each line
     */
    public JLabel[] getLabels() {
        return labels_.toArray( new JLabel[ 0 ] );
    }

    /**
     * Returns an array of the components which have been added.
     * Each corresponds to the <code>comp</code> argument
     * of an <code>addLine</code> call.
     *
     * @return   an array of field components, one for each line
     */
    public Component[] getFields() {
        return fields_.toArray( new Component[ 0 ] );
    }

}
