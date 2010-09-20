package uk.ac.starlink.table.gui;

import javax.swing.Icon;
import junit.framework.TestCase;

public class IconTest extends TestCase {

    public void testIcons() {
        assert24( new LocationTableLoadDialog().getIcon() );
        assert24( new SQLTableLoadDialog().getIcon() );
        assert24( new FileChooserTableLoadDialog().getIcon() );
        assert24( new FilestoreTableLoadDialog().getIcon() );
        assert24( SystemBrowser.getSystemBrowserIcon() );
    }

    private void assert24( Icon icon ) {
        assertEquals( 24, icon.getIconWidth() );
        assertEquals( 24, icon.getIconHeight() );
    }
}
