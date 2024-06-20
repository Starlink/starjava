package uk.ac.starlink.ast;

import uk.ac.starlink.util.TestCase;

public class KeyMapTest extends TestCase {

    public KeyMapTest( String name ) {
        super( name );
    }

    public void testMap() {
        KeyMap map = new KeyMap();

        map.mapPut0I( "VII", 8, null );
        assertEquals( "VII", map.mapKey( 0 ) );
        map.mapPut0I( "XIX", 19, "" );
        map.mapPut0D( "pi", Math.PI, "tits!" );
        map.mapPut0D( "e", Math.E, null );

        assertEquals( 8, map.mapGet0I( "VII" ).intValue() );
        assertEquals( 19, map.mapGet0I( "XIX" ).intValue() );
        assertEquals( Math.PI, map.mapGet0D( "pi" ).doubleValue() );
        assertEquals( Math.E, map.mapGet0D( "e" ).doubleValue() );

        assertEquals( KeyMap.AST__INTTYPE, map.mapType( "XIX" ) );
        assertEquals( KeyMap.AST__DOUBLETYPE, map.mapType( "pi" ) );

        assertEquals( 4, map.mapSize() );
        assertTrue( map.mapHasKey( "XIX" ) );
        assertEquals( 1, map.mapLength( "XIX" ) );
        map.mapRemove( "XIX" );
        assertTrue( ! map.mapHasKey( "XIX" ) );
        assertEquals( 0, map.mapLength( "XIX" ) );
        assertEquals( 3, map.mapSize() );
        assertNull( map.mapGet0I( "XIX" ) );

        String unkey = "Sir Not-Appearing-In-This-Map";
        assertTrue( ! map.mapHasKey( unkey ) );
        assertNull( map.mapGet0D( unkey ) );
        assertNull( map.mapGet0I( unkey ) );
        assertNull( map.mapGet0C( unkey ) );
        assertNull( map.mapGet0A( unkey ) );
        assertNull( map.mapGet1D( unkey ) );
        assertNull( map.mapGet1I( unkey ) );
        assertNull( map.mapGet1C( unkey, 1 ) );
        assertNull( map.mapGet1A( unkey ) );
        map.mapRemove( unkey );
        assertNull( map.mapGet0D( unkey ) );
        assertNull( map.mapGet0I( unkey ) );
        assertNull( map.mapGet0C( unkey ) );
        assertNull( map.mapGet0A( unkey ) );
        assertNull( map.mapGet1D( unkey ) );
        assertNull( map.mapGet1I( unkey ) );
        assertNull( map.mapGet1C( unkey, 10 ) );
        assertNull( map.mapGet1A( unkey ) );
        assertEquals( 0, map.mapLength( unkey ) );
    }

    public void testStrings() {
        KeyMap map = new KeyMap();

        map.mapPut0C( "Mark", "Beauchamp", null );
        assertEquals( "Beauchamp", map.mapGet0C( "Mark" ) );
    }

    public void testObjects() {
        KeyMap map = new KeyMap();

        AstObject ao = new UnitMap( 199 );
        map.mapPut0A( "AO", ao, "" );
        AstObject twin = map.mapGet0A( "AO" );
        assertTrue( ao != twin );
        assertTrue( ao.sameObject( twin ) );
    }

    public void testVectors() {
        KeyMap map = new KeyMap();

        AstObject o1 = new Frame( 1 );
        AstObject o2 = new Frame( 2 );
        map.mapPut1A( "A", new AstObject[] { o1, o2 },
                      null );
        assertEquals( 2, map.mapLength( "A" ) );
        assertArrayEquals( new AstObject[] { o1, o2 }, map.mapGet1A( "A" ) );
        assertEquals( o1, map.mapGet0A( "A" ) );

        map.mapPut1C( "C", new String[] { "one", "two", "three" },
                      "strings" );
        assertEquals( 3, map.mapLength( "C" ) );
        assertArrayEquals( new String[] { "one", "two", "three" },
                           map.mapGet1C( "C", 10 ) );
        assertArrayEquals( new String[] { "one", "two", "thr" },
                           map.mapGet1C( "C", 3 ) );
        assertEquals( "one", map.mapGet0C( "C" ) );

        map.mapPut1D( "D", new double[] { 1., 2., 3., 4. },
                      "" );
        assertEquals( 4, map.mapLength( "D" ) );
        assertArrayEquals( new double[] { 1., 2., 3., 4. },
                           map.mapGet1D( "D" ) );
        assertEquals( Double.valueOf( 1.0 ), map.mapGet0D( "D" ) );

        map.mapPut1I( "I", new int[] { 1, 2, 3, 4, 5 },
                      " n-n-numbers " );
        assertEquals( 5, map.mapLength( "I" ) );
        assertArrayEquals( new int[] { 1, 2, 3, 4, 5 },
                           map.mapGet1I( "I" ) );
        assertEquals( Integer.valueOf( 1 ), map.mapGet0I( "I" ) );
    }

    public void testErrors() {
        KeyMap map = new KeyMap();

        try {
            map.mapPut0I( null, 1, "" );
            fail();
        }
        catch ( NullPointerException e ) {}

        try {
            map.mapPut0D( null, 1.0, "" );
            fail();
        }
        catch ( NullPointerException e ) {}

        try {
            map.mapPut0C( null, "X", "" );
            fail();
        }
        catch ( NullPointerException e ) {}

        try {
            map.mapPut0A( null, new UnitMap( 1 ), "" );
            fail();
        }
        catch ( NullPointerException e ) {}
    }
}
