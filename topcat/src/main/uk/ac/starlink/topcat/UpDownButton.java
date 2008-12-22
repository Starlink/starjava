package uk.ac.starlink.topcat;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

/**
 * A toggle button whose representation switches between an up arrow (true)
 * and a down arrow (false).
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class UpDownButton extends JRadioButton {

    /**
     * Constructs an UpDownButton with a default model.
     */
    UpDownButton() {
        setIcon( ResourceIcon.DOWN_TRIM );
        setSelectedIcon( ResourceIcon.UP_TRIM );
        setBorder( BorderFactory.createEmptyBorder() );
        setSelected( true );
    }

    /**
     * Constructs an UpDownButton with a given model.
     *
     * @param   model  toggle button model
     */
    UpDownButton( JToggleButton.ToggleButtonModel model ) {
        this();
        setModel( model );
    }
}
