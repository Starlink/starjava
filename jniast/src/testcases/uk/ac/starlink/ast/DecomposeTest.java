package uk.ac.starlink.ast;

import junit.framework.TestCase;

public class DecomposeTest extends TestCase {

    public DecomposeTest( String name ) {
        super( name );
    }

    public void testFrame() {
        Frame f1 = new Frame( 1 );
        f1.setID( "F1" );
        Frame f2 = new Frame( 1 );
        f2.setID( "F2" );
        CmpFrame cfrm = new CmpFrame( f1, f2 );

        Mapping[] maps = cfrm.decompose( null, null );
        assertEquals( Frame.class, f1.getClass() );
        assertEquals( Frame.class, f2.getClass() );
        assertEquals( f1, maps[ 0 ] );
        assertEquals( f2, maps[ 1 ] );
    }
}
