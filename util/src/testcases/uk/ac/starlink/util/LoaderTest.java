package uk.ac.starlink.util;

public class LoaderTest extends TestCase {

    public LoaderTest() {
        super( null );
    }

    public LoaderTest( String name ) {
        super( name );
    }

    public void testLoader() {
        assertNull( Loader.getClassInstance( "no.class.here.mate",
                                             TestCase.class ) );
        assertNull( Loader.getClassInstance( getClass().getName(),
                                             Comparable.class ) );
        assertEquals( LoaderTest.class,
                      Loader.getClassInstance( getClass().getName(),
                                               TestCase.class ).getClass() );
    }

}
