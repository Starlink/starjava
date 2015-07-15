package uk.ac.starlink.ttools.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.swing.Icon;
import junit.framework.TestCase;

public class IconTest extends TestCase {

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
}
