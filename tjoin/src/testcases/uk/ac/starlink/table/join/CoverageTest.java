package uk.ac.starlink.table.join;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import uk.ac.starlink.pal.AngleDR;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.ttools.func.CoordsRadians;
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

    public void testSkyGeom() {
        Random rnd = new Random( 8982323L );
        FixedSkyMatchEngine fixEngine =
            new FixedSkyMatchEngine( new CdsHealpixSkyPixellator(), 0.0 );
        ErrorSkyMatchEngine errEngine =
            new ErrorSkyMatchEngine( new CdsHealpixSkyPixellator(),
                                     ErrorSummation.SIMPLE,
                                     Math.toRadians( 0.1 ) );
        MatchEngine combEngine =
            new CombinedMatchEngine( new MatchEngine[] { fixEngine } );

        // A HEALPix level 6 pixel is about 1 degree.
        for ( double rdeg = 1./3600.; rdeg < 10; rdeg *= 2 ) {
            double r = Math.toRadians( rdeg );

            fixEngine.setSeparation( r );
            Function<double[],Coverage> fixFunc = radec -> {
                Coverage cov = fixEngine.createCoverageFactory().get();
                cov.extend( boxXy( radec[ 0 ], radec[ 1 ] ) );
                return cov;
            };
            exerciseSkyCoverage( fixFunc, r, rnd );

            Function<double[],Coverage> combFunc = radec -> {
                Coverage cov = combEngine.createCoverageFactory().get();
                cov.extend( boxXy( radec[ 0 ], radec[ 1 ] ) );
                return cov;
            };
            exerciseSkyCoverage( combFunc, r, rnd );

            errEngine.setScale( r );
            for ( double vf = 0.25; vf < 4.001; vf *= 2 ) {
                double err = r * vf;
                Function<double[],Coverage> errFunc = radec -> {
                    Coverage cov = errEngine.createCoverageFactory().get();
                    cov.extend( new Object[] { radec[ 0 ], radec[ 1 ], err } );
                    return cov;
                };
                if ( err < Math.toRadians( 10 ) ) {
                    exerciseSkyCoverage( errFunc, err, rnd );
                }
            }
        }
    }

    /**
     * @param  covFunc  maps (ra,dec) array to coverage
     * @param  r   radius of coverage region in radians
     * @param  rnd  random number generator
     */
    private void exerciseSkyCoverage( Function<double[],Coverage> covFunc,
                                      double r, Random rnd ) {

        // Check that points supposed to be inside the coverage really are.
        for ( int i = 0; i < 20; i++ ) {

            // Pick a random sky position.
            double ra = rnd.nextDouble() * 2 * Math.PI;
            double dec = rnd.nextDouble() * Math.PI - Math.PI / 2.;

            // Cover a disc of the requested radius round it.
            Coverage cov = covFunc.apply( new double[] { ra, dec } );
            Predicate<Object[]> test = cov.createTestFactory().get();
            int nOut = 0;
            int nt = 100;
            for ( int j = 0; j < nt; j++ ) {

                // Check that all the points on the edge of the disc
                // test as inside the coverage.
                double[] off1 = randomOffset( ra, dec, r, rnd );
                assertTrue( test.test( boxXy0( off1[ 0 ], off1[ 1 ] ) ) );

                // Check that a selection of points inside the disc
                // test as inside the coverage.
                double[] off2 = randomOffset( ra, dec, r * rnd.nextDouble(),
                                              rnd );
                assertTrue( test.test( boxXy0( off2[ 0 ], off2[ 1 ] ) ) );

                // Pick some points outside the disc and see if they
                // test as outside the coverage.  This doesn't have to be
                // true for correctness, but the coverage isn't doing useful
                // work if it doesn't exclude these at all.
                // Especially for large length scales many may not be
                // excluded though, so don't make the test too rigorous.
                double rfar = Math.max( Math.toRadians( 2.0 ), r * 8 );
                double[] off3 = randomOffset( ra, dec, rfar, rnd );
                if ( ! test.test( boxXy0( off3[ 0 ], off3[ 1 ] ) ) ) { 
                    nOut++;
                }
            }
            assertTrue( nOut + "/100", nOut > 0.5 * nt );
        }
    }

    private static Object[] boxXy( double x, double y ) {
        return new Object[] { Double.valueOf( x ), Double.valueOf( y ), };
    }

    private static Object[] boxXy0( double x, double y ) {
        return new Object[] { Double.valueOf( x ), Double.valueOf( y ),
                              Double.valueOf( 0.0 ) };
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

    /**
     * Displaces a point d radians along a great circle in a random direction.
     */
    private static double[] randomOffset( double lon, double lat,
                                          double d, Random rnd ) {

        // Random unit vector with magnitude of desired offset
        double tx = rnd.nextDouble() - 0.5;
        double ty = rnd.nextDouble() - 0.5;
        double tz = rnd.nextDouble() - 0.5;
        double tf = 1.0 / Math.sqrt( tx * tx + ty * ty + tz * tz );

        // Cross product with position to get random vector in normal plane.
        Pal pal = new Pal();
        double[] r0 = pal.Dcs2c( new AngleDR( lon, lat ) );
        double[] ax = pal.Dvxv( r0, new double[] { tf * tx, tf * ty, tf * tz });

        // Scale random vector so its magnitude is size of rotation in radians.
        double af = d / Math.sqrt( ax[ 0 ] * ax[ 0 ]
                                 + ax[ 1 ] * ax[ 1 ]
                                 + ax[ 2 ] * ax[ 2 ] );
        ax[ 0 ] *= af;
        ax[ 1 ] *= af;
        ax[ 2 ] *= af;

        // Turn it into a rotation matrix and rotate input position.
        double[][] rot = pal.Dav2m( ax );

        // Rotate input position and return checked result.
        double[] r1 = pal.Dmxv( rot, r0 );
        AngleDR pos1 = pal.Dcc2s( r1 );
        double lon1 = pos1.getAlpha();
        double lat1 = pos1.getDelta();
        assertEquals( d,
                      CoordsRadians
                     .skyDistanceRadians( lon, lat, lon1, lat1 ),
                      d * 1e-5 );
        return new double[] { lon1, lat1 };
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
