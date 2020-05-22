package uk.ac.starlink.ttools.cone;

import java.util.BitSet;
import java.util.Random;
import junit.framework.TestCase;
import uk.ac.starlink.ttools.func.Tilings;

public class SkyTilingTest extends TestCase {

    private final Random random_ = new Random( 1122334455L );

    public SkyTilingTest( String name ) {
        super( name );
    }

    public void testHtmTilings() {
        double radius = 0.1/3600;
        for ( int i = 0; i < 10; i++ ) {
            int level = Tilings.htmLevel( radius );
            checkTiling( new HtmTiling( level ), radius );
            radius *= 2;
        }
    }

    public void testHealpixTilings() {
        double radius = 0.25/3600;
        for ( int i = 0; i < 10; i++ ) {
            int k = Tilings.healpixK( radius );
            checkTiling( new HealpixTiling( k, true ), radius );
            checkTiling( new HealpixTiling( k, false ), radius );
            radius *= 2;
        }
    }

    public void testPoints() {
        for ( int i = 0; i < 10; i++ ) {
            double ra = random_.nextDouble() * 360;
            double dec = ( random_.nextDouble() - 0.5 ) * 90;
            for ( int level = 2; level <= 20; level++ ) {
                int k = level;
                assertEquals(
                    new HealpixTiling( k, true )
                       .getPositionTile( ra, dec ),
                    Tilings.healpixNestIndex( k, ra, dec ) );
                assertEquals(
                    new HealpixTiling( k, false )
                       .getPositionTile( ra, dec),
                    Tilings.healpixRingIndex( k, ra, dec ) );
                assertEquals(
                    new HtmTiling( level ).getPositionTile( ra, dec ),
                    Tilings.htmIndex( level, ra, dec ) );
            }
        }
    }

    /* Fails with PixTools versions before 2017-09-06. */
    public void testPoint3() {
        BitSet ringMap = new BitSet();
        BitSet nestMap = new BitSet();
        for ( int level = 0; level <= 2; level++ ) {
            int npix = 12 * (int) Math.pow( 4, level );
            int nside = 1 << level;
            double resDeg = Tilings.healpixResolution( level );
            for ( long ipix = 0; ipix < npix; ipix++ ) {
                long ipixNest = ipix;
                long ipixRing = Tilings.healpixNestToRing( level, ipixNest );
                double lat = Tilings.healpixNestLat( level, ipixNest );
                double lon = Tilings.healpixNestLon( level, ipixNest );
                lat -= resDeg * 0.0001;
                lon -= resDeg * 0.0001;
                assertEquals( ipixNest,
                              Tilings.healpixNestIndex( level, lon, lat ) );
                assertEquals( Tilings.healpixRingIndex( level, lon, lat ),
                              Tilings.healpixNestToRing( level, ipixNest ) );
                nestMap.set( (int) ipixNest );
                ringMap.set( (int) ipixRing );
            }
            BitSet fullMap = new BitSet( npix );
            fullMap.set( 0, npix );
            assertEquals( fullMap, ringMap );
            assertEquals( fullMap, nestMap );
        }
    }

    private void checkTiling( SkyTiling tiling, double angle ) {
        for ( int j = -2; j <= 2; j++ ) {
            double ra = random_.nextDouble() * 360;
            double dec = ( random_.nextDouble() - 0.5 ) * 90;
            long tileIndex = tiling.getPositionTile( ra, dec );
            for ( int i = 0; i < 10; i++ ) {
                double radius =
                    angle * Math.pow( 2.0, j )
                          * ( 0.5 + 0.5 * random_.nextDouble() );
                long[] lohi = tiling.getTileRange( ra, dec, radius );
                assertTrue( tileIndex >= lohi[ 0 ] );
                assertTrue( tileIndex <= lohi[ 1 ] );
            }
        }
    }
}
