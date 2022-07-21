package uk.ac.starlink.topcat.func;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class FuncTest extends TestCase {

    static {
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.ast" ).setLevel( Level.SEVERE );
    }

    public FuncTest( String name ) {
        super( name );
    }

    public void testJELClasses() {
        checkClassesLookOK( TopcatJELUtils.getStaticClasses()
                                          .toArray( new Class[ 0 ] ) );
        checkClassesLookOK( TopcatJELUtils.getActivationStaticClasses()
                                          .toArray( new Class[ 0 ] ) );
    }

    public void checkClassesLookOK( Class[] classes ) {
        for ( int i = 0; i < classes.length; i++ ) {
            Class clazz = classes[ i ];

            /* Check there's one private no-arg constructor to prevent
             * instantiation (not really essential, but good practice). */
            Constructor[] constructors = clazz.getDeclaredConstructors();
            assertEquals( 1, constructors.length );
            Constructor pcons = constructors[ 0 ];
            assertEquals( 0, pcons.getParameterTypes().length );
            assertTrue( Modifier.isPrivate( pcons.getModifiers() ) );

            /* Check there are no non-static members (would probably indicate
             * missing the 'static' modifier by accident). */
            Field[] fields = clazz.getDeclaredFields();
            for ( int j = 0; j < fields.length; j++ ) {
                assertTrue( Modifier.isStatic( fields[ j ].getModifiers() ) );
            }
            Method[] methods = clazz.getDeclaredMethods();
            for ( int j = 0; j < methods.length; j++ ) {
                assertTrue( Modifier.isStatic( methods[ j ].getModifiers() ) );
            }
        }
    }
}
