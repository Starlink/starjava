package uk.ac.starlink.vo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.swing.Action;
import javax.swing.Icon;
import junit.framework.TestCase;

public class ResourceTest extends TestCase {

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
        assertNotNull( ResourceIcon.NODE_SERVICE );
        assertNotNull( ResourceIcon.NODE_TABLE );
    }

    public void testResourceIcon() {
        for ( Field field : ResourceIcon.class.getDeclaredFields() ) {
            int mods = field.getModifiers();
            String name = field.getName();
            if ( Icon.class.isAssignableFrom( field.getType() ) &&
                 Modifier.isPublic( mods ) &&
                 Modifier.isStatic( mods ) &&
                 Modifier.isFinal( mods ) &&
                 name.equals( name.toUpperCase() ) ) {
                try {
                    assertNotNull( name, (Icon) field.get( null ) );
                }
                catch ( IllegalAccessException e ) {
                    fail();
                }
            }
        }
    }

    public void testHintsDocs() throws Exception {
        for ( AdqlVersion vers : AdqlVersion.values() ) {
            assertNotNull( HintPanel.getDocResource( vers ) );
        }
    }

    private void assert24( Icon icon ) {
        assertEquals( 24, icon.getIconWidth() );
        assertEquals( 24, icon.getIconHeight() );
    }
}
