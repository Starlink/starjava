package uk.ac.starlink.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LoaderTest {

    @Test
    public void testLoader() {
        LogUtils.getLogger( "uk.ac.starlink.util" ).setLevel( Level.SEVERE );
        assertNull( Loader.getClassInstance( "no.class.here.mate",
                                             TestCase.class ) );
        assertNull( Loader.getClassInstance( getClass().getName(),
                                             Comparable.class ) );
        //TODO: The original gerClassInstance of TestCase.class, why do we test the equality with LoaderTest.class?
        assertEquals( LoaderTest.class,
                      Loader.getClassInstance( getClass().getName(),
                                               LoaderTest.class ).getClass() );
    }

    @Test
    public void testCheckJ2se() throws ClassNotFoundException {
        Loader.checkJ2se();
    }

    @Test
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
