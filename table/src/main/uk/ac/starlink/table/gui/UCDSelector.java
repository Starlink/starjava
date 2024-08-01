package uk.ac.starlink.table.gui;

import java.util.Iterator;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import uk.ac.starlink.table.UCD;

/**
 * A component which enables selection of a UCD by the user.
 * This currently consists of an editable JComboBox which permits selection
 * of one of the known UCDs or user entry of the ID of a new one.
 * The description of the UCD, if known, is displayed near it too.
 * At some point this component may get redesigned to permit hierarchical
 * UCD browsing or better display of the UCD descriptions.
 *
 * @author   Mark Taylor (Starlink)
 */
public class UCDSelector extends JPanel implements ItemListener {

    private JComboBox<String> comboBox;
    private JLabel descriptionLabel;

    /**
     * Constructs a new UCDSelector.
     */
    @SuppressWarnings("this-escape")
    public UCDSelector() {

        /* Construct the label used for showing descriptions. */
        descriptionLabel = new JLabel();

        /* Construct the JComboBox widget. */
        comboBox = new JComboBox<>();
        comboBox.setEditable( true );
        comboBox.addItem( null );
        for ( Iterator<UCD> it = UCD.getUCDs(); it.hasNext(); ) {
            comboBox.addItem( it.next().getID() );
        }
        comboBox.addItemListener( this );
        comboBox.setSelectedItem( "" );
        comboBox.setSelectedItem( null );

        /* Arrange the components. */
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        comboBox.setAlignmentX( SwingConstants.LEFT );
        descriptionLabel.setAlignmentX( SwingConstants.LEFT );
        add( comboBox );
        add( descriptionLabel );
    }

    /**
     * Returns the UCD ID selected by the user, or <code>null</code> if none
     * is selected.  Since the combobox is editable, this is not guaranteed
     * to be the ID of an existing UCD.  You can use the 
     * {@link uk.ac.starlink.table.UCD#getUCD} method to try to turn this
     * ID into a UCD.
     */
    public String getID() {
        return (String) comboBox.getSelectedItem();
    }

    /**
     * Sets the UCD ID currently entered in the selector to a given string.
     *
     * @param   id  UCD identifier
     */
    public void setID( String id ) {
        comboBox.setSelectedItem( id );
    }

    public void itemStateChanged( ItemEvent evt ) {

        /* Deal with selection events from this selector's comboBox - 
         * bail out if we have somehow been called otherwise. */
        if ( evt.getSource() != comboBox ) {
            return;
        }

        /* Change the state of the description label to reflect the new
         * selection. */
        String desc = "no UCD";
        String id = getID();
        if ( id != null && id.trim().length() > 0 ) {
            UCD ucd = UCD.getUCD( id );
            if ( ucd != null ) {
                desc = ucd.getDescription();
            }
            else {
                desc = "unknown UCD";
            }
        }
        descriptionLabel.setText( desc );
    }
}
