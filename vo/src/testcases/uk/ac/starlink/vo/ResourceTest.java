package uk.ac.starlink.vo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.swing.Action;
import javax.swing.Icon;
import uk.ac.starlink.util.TestCase;

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
        assertNotNull( ResourceIcon.NODE_FUNCTION );
        assertNotNull( ResourceIcon.NODE_SIGNATURE );
        assertNotNull( ResourceIcon.NODE_DOC );
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

    public void testUdf() {
        FeatureTreeModel.Signature mocSig =
            FeatureTreeModel
           .createSignature( "moc_intersect(moc1 MOC, moc2 MOC) -> MOC" );
        assertEquals( "moc_intersect", mocSig.getName() );
        FeatureTreeModel.Arg[] args = mocSig.getArgs();
        assertEquals( 2, args.length );
        assertEquals( "moc1", args[ 0 ].getArgName() );
        assertEquals( "moc2", args[ 1 ].getArgName() );
        assertEquals( "MOC", args[ 0 ].getArgType() );
        assertEquals( "MOC", args[ 1 ].getArgType() );
        assertEquals( "MOC", mocSig.getReturnType() );
    }

    private void assert24( Icon icon ) {
        assertEquals( 24, icon.getIconWidth() );
        assertEquals( 24, icon.getIconHeight() );
    }
}
