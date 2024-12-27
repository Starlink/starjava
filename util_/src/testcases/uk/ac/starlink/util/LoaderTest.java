package uk.ac.starlink.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

public class LoaderTest extends TestCase {

    public LoaderTest() {
        super( null );
    }

    public LoaderTest( String name ) {
        super( name );
    }

    public void testLoader() {
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.SEVERE );
        assertNull( Loader.getClassInstance( "no.class.here.mate",
                                             TestCase.class ) );
        assertNull( Loader.getClassInstance( getClass().getName(),
                                             Comparable.class ) );
        assertEquals( LoaderTest.class,
                      Loader.getClassInstance( getClass().getName(),
                                               TestCase.class ).getClass() );
    }

    public void testCheckJ2se() throws ClassNotFoundException {
        Loader.checkJ2se();
    }

    public void testGetClassInstances() {
        System.setProperty( "util.loader.classes",
                            "java.util.HashMap:java.util.WeakHashMap" );
        List maps = Loader.getClassInstances( "util.loader.classes",
                                              Map.class );
        assertEquals( 2, maps.size() );
        assertEquals( HashMap.class, maps.get( 0 ).getClass() );
        assertEquals( WeakHashMap.class, maps.get( 1 ).getClass() );
    }


}
