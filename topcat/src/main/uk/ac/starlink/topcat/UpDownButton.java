package uk.ac.starlink.topcat;

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
    UpDownButton() {
        setIcon( ResourceIcon.DOWN_TRIM );
        setSelectedIcon( ResourceIcon.UP_TRIM );
        setSelected( true );
    }
}
