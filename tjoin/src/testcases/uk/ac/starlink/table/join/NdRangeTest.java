package uk.ac.starlink.table.join;

import junit.framework.TestCase;

public class NdRangeTest extends TestCase {

    private static final float NULL = Float.NaN;

    public NdRangeTest( String name ) {
        super( name );
    }

    public void testConstructor() {
        try {
            new NdRange( new Comparable[ 2 ], new Comparable[ 3 ] );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            new NdRange( new String[] { "Z", }, new String[] { "A", } );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testIsBounded() {
        for ( int idim = 0; idim < 4; idim++ ) {
            assertTrue( ! new NdRange( idim ).isBounded() );
            assertTrue( ! new NdRange( new Comparable[ idim ],
                                     new Comparable[ idim ] ).isBounded() );
        }

        assertTrue( new NdRange( new Comparable[] { null, null,
                                                    new Long( 1L ), },
                               new Comparable[ 3 ] ).isBounded() );
        assertTrue( new NdRange( new Comparable[ 2 ],
                                 new String[] { "a", "b", } ).isBounded() );
    }

    public void testMinMax() {
        assertEquals( "C", NdRange.min( "C", null, false ) );
        assertEquals( "C", NdRange.min( null, "C", false ) );
        assertEquals( "C", NdRange.min( "C", "X", false ) );
        assertEquals( "C", NdRange.min( "X", "C", false ) );
        assertNull( NdRange.min( "C", null, true ) );
        assertNull( NdRange.min( null, "C", true ) );
        assertEquals( "C", NdRange.min( "C", "X", true ) );
        assertEquals( "C", NdRange.min( "X", "C", true ) );

        assertEquals( "C", NdRange.max( "C", null, false ) );
        assertEquals( "C", NdRange.max( null, "C", false ) );
        assertEquals( "X", NdRange.max( "C", "X", false ) );
        assertEquals( "X", NdRange.max( "X", "C", false ) );
        assertNull( NdRange.max( "C", null, true ) );
        assertNull( NdRange.max( null, "C", true ) );
        assertEquals( "X", NdRange.max( "C", "X", true ) );
        assertEquals( "X", NdRange.max( "X", "C", true ) );

        assertEquals(
            new Integer( 3 ),
            NdRange.min( new Integer( 3 ), new Double( Math.PI ),
                       false ) );
        assertEquals(
            new Short( (short) 3 ),
            NdRange.max( new Short( (short) 3 ), new Float( (float) Math.E ),
                       true ) );
    }

    public void testInside() {
        assertTrue( new NdRange( 2 ).isInside( n2( 1, 20 ) ) );
        assertTrue( new NdRange( n2( 0, 0 ), n2( NULL, NULL ) )
                   .isInside( n2( 1, 20 ) ) );
        assertTrue( ! new NdRange( n2( NULL, NULL ), n2( 0, 0 ) )
                     .isInside( n2( 1, 20 ) ) );
        assertTrue( new NdRange( n2( NULL, 0 ), n2( NULL, 100 ) )
                   .isInside( n2( 1, 20 ) ) );
        assertTrue( ! new NdRange( n2( NULL, 0 ), n2( NULL, 10 ) )
                     .isInside( n2( 1, 20 ) ) );

        assertTrue( new NdRange( n2( 0, 0 ), n2( 1, 20 ) )
                   .isInside( n2( 1, 20 ) ) );
        assertTrue( ! new NdRange( n2( 0, 0 ), n2( 1, 20 ) )
                     .isInside( n2( -1, 20 ) ) );
    }

    public void testIntersection() {
        NdRange small = new NdRange( n2( 10, 20 ), n2( 11, 21 ) );
        NdRange big = new NdRange( n2( 5, 15 ), n2( 15, 25 ) );
        assertEquals( small, NdRange.intersection( small, big ) );
        assertEquals( small, NdRange.intersection( big, small ) );

        assertEquals( small, NdRange.intersection( small, small ) );
        assertEquals( big, NdRange.intersection( big, big ) );

        NdRange small1 = new NdRange( n2( 10, NULL ), n2( 11, NULL ) );
        NdRange big1 = new NdRange( n2( 5, NULL ), n2( 15, NULL ) );
        assertEquals( small1, NdRange.intersection( small1, big1 ) );
        assertEquals( small1, NdRange.intersection( big1, small1 ) );

        assertEquals( small, NdRange.intersection( small, small ) );
        assertEquals( big1, NdRange.intersection( big1, big1 ) );

        assertEquals(
            new NdRange( n2( 10, 100 ), n2( 20, 110 ) ),
            NdRange
           .intersection( new NdRange( n2( 0, 90 ), n2( 20, 110 ) ),
                          new NdRange( n2( 10, 100 ), n2( 30, 120 ) ) ) );

        assertNull(
            NdRange
           .intersection( new NdRange( n2( 10, 20 ), n2( 12, 22 ) ),
                          new NdRange( n2( 110, 120 ), n2( 112, 122 ) ) ) );
        assertNull(
            NdRange
           .intersection( new NdRange( n2( 10, 20 ), n2( 12, 22 ) ),
                          new NdRange( n2( 110, 20 ), n2( 112, 22 ) ) ) );
        assertNull(
            NdRange.intersection( new NdRange( n2( 10, NULL ), n2( 12, NULL ) ),
                                  new NdRange( n2( 110, NULL ),
                                               n2( 112, NULL ) ) ) );
    }

    public void testUnion() {
        NdRange small = new NdRange( n2( 10, 20 ), n2( 11, 21 ) );
        NdRange big = new NdRange( n2( 5, 15 ), n2( 15, 25 ) );
        assertEquals( big, NdRange.union( big, small ) );
        assertEquals( big, NdRange.union( small, big ) );

        assertEquals( small, NdRange.union( small, small ) );
        assertEquals( big, NdRange.union( big, big ) );

        NdRange bigger = new NdRange( n2( 5, NULL ), n2( 15, 25 ) );
        assertEquals( bigger, NdRange.union( small, bigger ) );
        assertEquals( bigger, NdRange.union( small, bigger ) );

        assertEquals(
            new NdRange( n2( 0, 90 ), n2( 30, 120 ) ),
            NdRange.union( new NdRange( n2( 0, 90 ), n2( 20, 110 ) ),
                           new NdRange( n2( 10, 100 ), n2( 30, 120 ) ) ) );
    }

    private static Comparable n( float val ) {
        return Float.isNaN( val ) ? null : new Float( val );
    }

    private static Comparable[] n2( float val1, float val2 ) {
        return new Comparable[] {
            ( Float.isNaN( val1 ) ? null : new Float( val1 ) ),
            ( Float.isNaN( val2 ) ? null : new Double( val2 ) ),
        };
    }
}
