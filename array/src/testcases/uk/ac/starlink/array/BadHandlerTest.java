package uk.ac.starlink.array;

import java.util.Iterator;
import junit.framework.TestCase;

public class BadHandlerTest extends TestCase {

    public BadHandlerTest( String name ) {
        super( name );
    }

    public void testBadHandler() {
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type type = (Type) it.next();
            BadHandler bh = BadHandler.getHandler( type,
                                                   type.defaultBadValue() );
            Object array1 = type.newArray( 1 );
            Object array2 = type.newArray( 2 );
            BadHandler.ArrayHandler ah = bh.arrayHandler( array2 );
            assertTrue( ! bh.isBad( array1, 0 ) );
            assertTrue( ! ah.isBad( 0 ) );
            Number tnum = bh.makeNumber( array1, 0 );
            assertEquals( tnum.getClass(), bh.getBadValue().getClass() );
            assertEquals( tnum.intValue(), 0 );
            bh.putBad( array1, 0 );
            ah.putBad( 0 );
            assertTrue( bh.isBad( array1, 0 ) );
            assertTrue( ah.isBad( 0 ) );
            assertTrue( ! ah.isBad( 1 ) );
            assertTrue( bh.makeNumber( array1, 0 ) == null );
        }
    }

    public void testNullBadHandler() {
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type type = (Type) it.next();
            BadHandler bh = BadHandler.getHandler( type, null );
            Object array1 = type.newArray( 1 );
            Object array2 = type.newArray( 2 );
            BadHandler.ArrayHandler ah = bh.arrayHandler( array2 );
            assertTrue( ! bh.isBad( array1, 0 ) );
            assertTrue( ! ah.isBad( 0 ) );
            Number tnum = bh.makeNumber( array1, 0 );
            assertEquals( tnum.intValue(), 0 );
            bh.putBad( array1, 0 );
            ah.putBad( 0 );
            if ( type.isFloating() ) {
                assertNotNull( bh.getBadValue() );
                assertTrue( bh.isBad( array1, 0 ) );
                assertTrue( ah.isBad( 0 ) );
                assertTrue( ! ah.isBad( 1 ) );
                assertNull( bh.makeNumber( array1, 0 ) );
            }
            else {
                assertNull( bh.getBadValue() );
                assertTrue( ! bh.isBad( array1, 0 ) );
                assertTrue( ! ah.isBad( 0 ) );
                assertTrue( ! ah.isBad( 0 ) );
                assertNotNull( bh.makeNumber( array1, 0 ) );
            }
        }
    }
}
