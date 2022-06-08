package uk.ac.starlink.table.join;

import java.util.function.Predicate;
import java.util.function.Supplier;
import uk.ac.starlink.util.TestCase;

public class CoverageTest extends TestCase {

    public void testCoverage() {
        assertFalse( Coverage.FULL.isEmpty() );
        Predicate<Object[]> fullTest = Coverage.FULL.createTestFactory().get();
        assertTrue( fullTest.test( boxXy( 0, 0 ) ) );
        assertTrue( fullTest.test( boxXy( -100, +1_000_000 ) ) );
    }

    public void testSkyCoverage() {
        BitsetMask mask3 = new BitsetMask( 3 );
        assertTrue( mask3.isEmpty() );
        mask3.addPixel( 3, 0 );
        assertEquals( 1./768., mask3.getSkyFraction() );
        mask3 = new BitsetMask( 3 );
        mask3.addPixel( 4, 0 );
        assertEquals( 1./768., mask3.getSkyFraction() );
        mask3 = new BitsetMask( 3 );
        mask3.addPixel( 2, 0 );
        mask3.addPixel( 2, 0 );
        assertEquals( 4./768., mask3.getSkyFraction() );
        mask3 = new BitsetMask( 3 );
        mask3.addPixel( 0, 11 );
        assertEquals( 1./12., mask3.getSkyFraction() );
        SkyCoverage.TupleDecoder degDecoder = (t, d) -> {
            d[ 0 ] = Math.toRadians( ((Number) t[ 0 ]).doubleValue() );
            d[ 1 ] = Math.toRadians( ((Number) t[ 1 ]).doubleValue() );
            return true;
        };
        SkyCoverage c1 =
                SkyCoverage.createFixedErrorCoverage( 0.0001, degDecoder );
        assertTrue( c1.isEmpty() );
        assertEquals( 0, c1.getMask().getSkyFraction() );
        c1.extend( boxXy( 0, 90 ) );
        assertFalse( c1.isEmpty() );
        assertEquals( 4. / (12L * ( 1 << 2 * BitsetMask.DEFAULT_ORDER ) ),
                      c1.getMask().getSkyFraction() );
        assertTrue( c1.createTestFactory().get().test( boxXy( 23, 89.9 ) ) );
        assertFalse( c1.createTestFactory().get().test( boxXy( 23, -6 ) ) );
        SkyCoverage c2 =
                SkyCoverage.createFixedErrorCoverage( 0.0001, degDecoder );
        c2.extend( boxXy( 0, -90 ) );
        assertEquals( 4. / (12L * ( 1 << 2 * BitsetMask.DEFAULT_ORDER ) ),
                      c2.getMask().getSkyFraction() );
        assertFalse( c2.createTestFactory().get().test( boxXy( 23, 89.9 ) ) );
        c2.union( c1 );
        assertEquals( 8. / (12L * ( 1 << 2 * BitsetMask.DEFAULT_ORDER ) ),
                      c2.getMask().getSkyFraction() );
        assertTrue( c2.createTestFactory().get().test( boxXy( 23, 89.9 ) ) );

        SkyCoverage c3 =
            SkyCoverage.createFixedErrorCoverage( 0.001, degDecoder );
        c3.extend( boxXy( 180, -10 ) );
        c3.extend( boxXy( 180, +10 ) );
        c2.intersection( c3 );
        assertTrue( c2.isEmpty() );
    }

    public void testCuboidCoverage() {
        CuboidCoverage c1 =
            CuboidCoverage.createFixedCartesianCoverage( 2, 0.5 );
        assertTrue( c1.isEmpty() );
        c1.extend( boxXy( 0, 4 ) );
        assertFalse( c1.isEmpty() );
        c1.extend( boxXy( 1, 8 ) );
        assertCuboidBounds( new double[] { -0.5, 3.5 },
                            new double[] { 1.5, 8.5 }, c1 );
        
        Predicate<Object[]> test1 = c1.createTestFactory().get();
        assertTrue( test1.test( boxXy( 0.5, 6 ) ) );
        assertTrue( test1.test( boxXy( -0.5, 6 ) ) );
        assertFalse( test1.test( boxXy( -0.51, 6 ) ) );
        assertTrue( test1.test( boxXy( 0, 4 ) ) );
        assertTrue( test1.test( boxXy( 1.5, 6 ) ) );
        assertFalse( test1.test( boxXy( 2, 6 ) ) );

        CuboidCoverage c2 =
            CuboidCoverage
           .createFixedCartesianCoverage( 2, new double[] { 1, 2 } );
        c2.extend( boxXy( 1, 1 ) );
        assertCuboidUnionBounds( c1, c2, 
                                 new double[] { -0.5, -1 },
                                 new double[] { 2, 8.5 } );
        assertCuboidIntersectionBounds( c1, c2, null, null );

        CuboidCoverage c3 =
            CuboidCoverage
           .createFixedCartesianCoverage( 2, new double[] { 1, 2 } );
        c3.extend( boxXy( 1, 2 ) );
        
        assertCuboidIntersectionBounds( c1, c3, new double[] { 0, 3.5 },
                                                new double[] { 1.5, 4 } );
    }

    private static Object[] boxXy( double x, double y ) {
        return new Object[] { Double.valueOf( x ), Double.valueOf( y ), };
    }

    private void assertCuboidUnionBounds( CuboidCoverage c1,
                                          CuboidCoverage c2,
                                          double[] mins, double[] maxs ) {
        CuboidCoverage u1 = new CopyCuboid( c1 );
        u1.union( c2 );
        assertCuboidBounds( mins, maxs, u1 );
        CuboidCoverage u2 = new CopyCuboid( c2 );
        u2.union( c1 );
        assertCuboidBounds( mins, maxs, u2 );
    }

    private void assertCuboidIntersectionBounds( CuboidCoverage c1,
                                                 CuboidCoverage c2,
                                                 double[] mins, double[] maxs ){
        CuboidCoverage inter1 = new CopyCuboid( c1 );
        inter1.intersection( c2 );
        assertCuboidBounds( mins, maxs, inter1 );
        CuboidCoverage inter2 = new CopyCuboid( c2 );
        inter2.intersection( c1 );
        assertCuboidBounds( mins, maxs, inter2 );
    }

    private void assertCuboidBounds( double[] mins, double[] maxs,
                                     CuboidCoverage cov ) {
        if ( mins == null && maxs == null ) {
            assertTrue( cov.isEmpty() );
        }
        else {
            assertArrayEquals( mins, cov.mins_ );
            assertArrayEquals( maxs, cov.maxs_ );
        }
    }

    private static class CopyCuboid extends CuboidCoverage {
        CopyCuboid( CuboidCoverage template ) {
            super( template.ndim_ );
            System.arraycopy( template.mins_, 0, mins_, 0, ndim_ );
            System.arraycopy( template.maxs_, 0, maxs_, 0, ndim_ );
        }
        public void extend( Object[] tuple ) {
            throw new UnsupportedOperationException();
        }
        public Supplier<Predicate<Object[]>> createTestFactory() {
            throw new UnsupportedOperationException();
        }
    }
}
