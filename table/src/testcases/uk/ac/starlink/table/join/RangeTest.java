package uk.ac.starlink.table.join;

import junit.framework.TestCase;

public class RangeTest extends TestCase {

    private static final float NULL = Float.NaN;

    public RangeTest( String name ) {
        super( name );
    }

    public void testConstructor() {
        try {
            new Range( new Comparable[ 2 ], new Comparable[ 3 ] );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
        try {
            new Range( new String[] { "Z", }, new String[] { "A", } );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testIsBounded() {
        for ( int idim = 0; idim < 4; idim++ ) {
            assertTrue( ! new Range( idim ).isBounded() );
            assertTrue( ! new Range( new Comparable[ idim ],
                                     new Comparable[ idim ] ).isBounded() );
        }

        assertTrue( new Range( new Comparable[] { null, null, new Long( 1L ), },
                               new Comparable[ 3 ] ).isBounded() );
        assertTrue( new Range( new Comparable[ 2 ],
                               new String[] { "a", "b", } ).isBounded() );
    }

    public void testMinMax() {
        assertEquals( "C", Range.min( "C", null, false ) );
        assertEquals( "C", Range.min( null, "C", false ) );
        assertEquals( "C", Range.min( "C", "X", false ) );
        assertEquals( "C", Range.min( "X", "C", false ) );
        assertNull( Range.min( "C", null, true ) );
        assertNull( Range.min( null, "C", true ) );
        assertEquals( "C", Range.min( "C", "X", true ) );
        assertEquals( "C", Range.min( "X", "C", true ) );

        assertEquals( "C", Range.max( "C", null, false ) );
        assertEquals( "C", Range.max( null, "C", false ) );
        assertEquals( "X", Range.max( "C", "X", false ) );
        assertEquals( "X", Range.max( "X", "C", false ) );
        assertNull( Range.max( "C", null, true ) );
        assertNull( Range.max( null, "C", true ) );
        assertEquals( "X", Range.max( "C", "X", true ) );
        assertEquals( "X", Range.max( "X", "C", true ) );

        assertEquals(
            new Integer( 3 ),
            Range.min( new Integer( 3 ), new Double( Math.PI ),
                       false ) );
        assertEquals(
            new Short( (short) 3 ),
            Range.max( new Short( (short) 3 ), new Float( (float) Math.E ),
                       true ) );
    }

    public void testInside() {
        assertTrue( new Range( 2 ).isInside( n2( 1, 20 ) ) );
        assertTrue( new Range( n2( 0, 0 ), n2( NULL, NULL ) )
                   .isInside( n2( 1, 20 ) ) );
        assertTrue( ! new Range( n2( NULL, NULL ), n2( 0, 0 ) )
                     .isInside( n2( 1, 20 ) ) );
        assertTrue( new Range( n2( NULL, 0 ), n2( NULL, 100 ) )
                   .isInside( n2( 1, 20 ) ) );
        assertTrue( ! new Range( n2( NULL, 0 ), n2( NULL, 10 ) )
                     .isInside( n2( 1, 20 ) ) );

        assertTrue( new Range( n2( 0, 0 ), n2( 1, 20 ) )
                   .isInside( n2( 1, 20 ) ) );
        assertTrue( ! new Range( n2( 0, 0 ), n2( 1, 20 ) )
                     .isInside( n2( -1, 20 ) ) );
    }

    public void testIntersection() {
        Range small = new Range( n2( 10, 20 ), n2( 11, 21 ) );
        Range big = new Range( n2( 5, 15 ), n2( 15, 25 ) );
        assertEquals( small, Range.intersection( small, big ) );
        assertEquals( small, Range.intersection( big, small ) );

        assertEquals( small, Range.intersection( small, small ) );
        assertEquals( big, Range.intersection( big, big ) );

        Range small1 = new Range( n2( 10, NULL ), n2( 11, NULL ) );
        Range big1 = new Range( n2( 5, NULL ), n2( 15, NULL ) );
        assertEquals( small1, Range.intersection( small1, big1 ) );
        assertEquals( small1, Range.intersection( big1, small1 ) );

        assertEquals( small, Range.intersection( small, small ) );
        assertEquals( big1, Range.intersection( big1, big1 ) );

        assertEquals(
            new Range( n2( 10, 100 ), n2( 20, 110 ) ),
            Range.intersection( new Range( n2( 0, 90 ), n2( 20, 110 ) ),
                                new Range( n2( 10, 100 ), n2( 30, 120 ) ) ) );

        assertNull(
            Range.intersection( new Range( n2( 10, 20 ), n2( 12, 22 ) ),
                                new Range( n2( 110, 120 ), n2( 112, 122 ) ) ) );
        assertNull(
            Range.intersection( new Range( n2( 10, 20 ), n2( 12, 22 ) ),
                                new Range( n2( 110, 20 ), n2( 112, 22 ) ) ) );
        assertNull(
            Range.intersection( new Range( n2( 10, NULL ), n2( 12, NULL ) ),
                                new Range( n2( 110, NULL ),
                                           n2( 112, NULL ) ) ) );
    }

    public void testUnion() {
        Range small = new Range( n2( 10, 20 ), n2( 11, 21 ) );
        Range big = new Range( n2( 5, 15 ), n2( 15, 25 ) );
        assertEquals( big, Range.union( big, small ) );
        assertEquals( big, Range.union( small, big ) );

        assertEquals( small, Range.union( small, small ) );
        assertEquals( big, Range.union( big, big ) );

        Range bigger = new Range( n2( 5, NULL ), n2( 15, 25 ) );
        assertEquals( bigger, Range.union( small, bigger ) );
        assertEquals( bigger, Range.union( small, bigger ) );

        assertEquals(
            new Range( n2( 0, 90 ), n2( 30, 120 ) ),
            Range.union( new Range( n2( 0, 90 ), n2( 20, 110 ) ),
                         new Range( n2( 10, 100 ), n2( 30, 120 ) ) ) );
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
