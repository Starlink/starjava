package uk.ac.starlink.ast;

import uk.ac.starlink.util.TestCase;

public class AstroCoordsTest extends TestCase {

    public AstroCoordsTest( String name ) {
        super( name );
    }

    public void testEmptyCoords() {
        assertEquals( 0,
                      Stc.astroCoordsToKeyMap( new AstroCoords() ).mapSize() );

        AstroCoords ac = Stc.keyMapToAstroCoords( new KeyMap() );
        assertNull( ac.getName() );
        assertNull( ac.getValue() );
        assertNull( ac.getError() );
        assertNull( ac.getResolution() );
        assertNull( ac.getSize() );
        assertNull( ac.getPixSize() );
    }

    public void testCoords() {
        String[] name = new String[] { "X", "Y" };
        Region value = makeBox( 1 );
        Region error = makeBox( 2 );
        Region res = makeBox( 3 );
        Region size = makeBox( 4 );
        Region pixsz = makeBox( 5 );

        AstroCoords ac = new AstroCoords();
        ac.setName( new String[] { "X", "Y" } );
        ac.setValue( value );
        ac.setError( error );
        ac.setResolution( res );
        ac.setSize( size );
        ac.setPixSize( pixsz );

        assertArrayEquals( name, ac.getName() );
        assertSameShape( value, ac.getValue() );
        assertSameShape( error, ac.getError() );
        assertSameShape( res, ac.getResolution() );
        assertSameShape( size, ac.getSize() );
        assertSameShape( pixsz, ac.getPixSize() );

        AstroCoords ac0 = ac;
        KeyMap km = Stc.astroCoordsToKeyMap( ac );
        assertEquals( 6, km.mapSize() );
        assertEquals( 2,
                      km.mapLength( Stc.getAstConstantC( "AST__STCNAME" ) ) );
        assertEquals( 1, km.mapLength( Stc.getAstConstantC( "AST__STCRES" ) ) );
        assertEquals( 0, km.mapLength( "urrgh" ) );

        ac = Stc.keyMapToAstroCoords( km );
        assertTrue( ac != ac0 );

        assertArrayEquals( name, ac.getName() );
        assertSameShape( value, ac.getValue() );
        assertSameShape( error, ac.getError() );
        assertSameShape( res, ac.getResolution() );
        assertSameShape( size, ac.getSize() );
        assertSameShape( pixsz, ac.getPixSize() );

        assertArrayEquals( name, new AstroCoords( name ).getName() );
    }

    private static Region makeBox( double origin ) {
        return new Box( new Frame( 2 ), 1, 
                        new double[] { origin, origin + 1.0 },
                        new double[] { origin + 2.0, origin + 4.0 }, null );
    }

    private void assertSameShape( Region region1, Region region2 ) {
        int ov = region1.overlap( region2 );
        assertEquals( "Regions don't match (" + ov + ")",
                      Region.OVERLAP_SAME, ov );
    }
}
