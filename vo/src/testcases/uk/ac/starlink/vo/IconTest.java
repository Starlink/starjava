package uk.ac.starlink.vo;

import javax.swing.Icon;
import junit.framework.TestCase;

public class IconTest extends TestCase {

    public void testIcons() {
        assert24( new ConeSearchDialog().getIcon() );
        assert24( new RegistryTableLoadDialog().getIcon() );
        assert24( new SiapTableLoadDialog().getIcon() );
        assert24( new SsapTableLoadDialog().getIcon() );
    }

    private void assert24( Icon icon ) {
        assertEquals( 24, icon.getIconWidth() );
        assertEquals( 24, icon.getIconHeight() );
    }
}
