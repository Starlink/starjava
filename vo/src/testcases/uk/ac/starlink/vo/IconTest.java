package uk.ac.starlink.vo;

import javax.swing.Action;
import javax.swing.Icon;
import junit.framework.TestCase;

public class IconTest extends TestCase {

    public void testDialogIcons() {
        assert24( new ConeSearchDialog().getIcon() );
        assert24( new Ri1RegistryTableLoadDialog().getIcon() );
        assert24( new SiapTableLoadDialog().getIcon() );
        assert24( new SsapTableLoadDialog().getIcon() );
        assert24( new TapTableLoadDialog().getIcon() );
    }

    public void testEditActions() {
        for ( Action act : new TapQueryPanel( null ).getEditActions() ) {
            assertNotNull( act.getValue( Action.SHORT_DESCRIPTION ) );
            Icon icon = (Icon) act.getValue( Action.SMALL_ICON );
            assertNotNull( icon );
            assert24( icon );
        }
    }

    public void testTreeIcons() {
        assertNotNull( TapServiceTreeModel.serviceIcon_ );
        assertNotNull( TapServiceTreeModel.tableIcon_ );
    }

    private void assert24( Icon icon ) {
        assertEquals( 24, icon.getIconWidth() );
        assertEquals( 24, icon.getIconHeight() );
    }
}
