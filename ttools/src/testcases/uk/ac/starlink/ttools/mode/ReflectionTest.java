package uk.ac.starlink.ttools.mode;

import junit.framework.TestCase;

public class ReflectionTest extends TestCase {

    public ReflectionTest( String name ) {
        super( name );
    }

    public void testTopcatMode() throws Exception {
        TopcatMode.TopcatLoader.reflect();
    }
}
