package uk.ac.starlink.topcat.join;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Component that allows the user to select table names for use with
 * the CDS X-Match service.
 *
 * <p>Currently extremely basic.
 * There should be some kind of way to select known VizieR tables.
 *
 * @author   Mark Taylor
 * @since    15 May 2014
 */
public class CdsTableSelector extends JPanel {

    private final JComboBox nameSelector_;

    /**
     * Constructor.
     */
    public CdsTableSelector() {
        nameSelector_ = new JComboBox();
        nameSelector_.addItem( "SIMBAD" );
        nameSelector_.setEditable( true );
        nameSelector_.setSelectedItem( null );

        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( new JLabel( "VizieR Table Name: " ) );
        add( nameSelector_ );
    }

    /**
     * Returns the human-readable name of a selected table.
     *
     * @return  currently selected table name
     */
    public String getTableName() {
        return (String) nameSelector_.getSelectedItem();
    }
}
