package uk.ac.starlink.table.gui;

import javax.swing.Icon;
import junit.framework.TestCase;

public class IconTest extends TestCase {

    public void testIcons() {
        assert24( new SQLReadDialog().getIcon() );
        assert24( new FileChooserLoader().getIcon() );
        assert24( new FilestoreTableLoadDialog().getIcon() );
    }

    private void assert24( Icon icon ) {
        assertEquals( 24, icon.getIconWidth() );
        assertEquals( 24, icon.getIconHeight() );
    }
}
