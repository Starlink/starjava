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

        Mapping[] maps = ((Frame) cfrm).decompose( null, null );
        assertEquals( Frame.class, f1.getClass() );
        assertEquals( Frame.class, f2.getClass() );
        assertEquals( f1, maps[ 0 ] );
        assertEquals( f2, maps[ 1 ] );

        Frame[] frms = ((CmpFrame) cfrm).decompose();
        assertEquals( f1, frms[ 0 ] );
        assertEquals( f2, frms[ 1 ] );
    }

    public void testMapping() {
        Mapping m1 = new UnitMap( 1 );
        m1.setID( "M1" );
        Mapping m2 = new UnitMap( 1 );
        m2.setID( "M2" );
        CmpMap cmap = new CmpMap( m1, m2, true );

        Mapping[] maps = cmap.decompose( null, null );
        assertEquals( m1, maps[ 0 ] );
        assertEquals( m2, maps[ 1 ] );
    }
}
