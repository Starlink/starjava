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
            assertTrue( ! bh.isBad( array1, 0 ) );
            Number tnum = bh.makeNumber( array1, 0 );
            assertEquals( tnum.getClass(), bh.getBadValue().getClass() );
            assertEquals( tnum.intValue(), 0 );
            bh.putBad( array1, 0 );
            assertTrue( bh.isBad( array1, 0 ) );
            assertTrue( bh.makeNumber( array1, 0 ) == null );
        }
    }
}
