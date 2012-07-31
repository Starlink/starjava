package uk.ac.starlink.ttools.cone;

import gov.fnal.eag.healpix.PixTools;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.vecmath.Vector3d;
import junit.framework.TestCase;
import uk.ac.starlink.ttools.func.Tilings;

public class SkyTilingTest extends TestCase {

    private final Random random_ = new Random( 1122334455L );

    public SkyTilingTest( String name ) {
        super( name );
    }

    public void testPixTools() {

        /* Known to fail in early versions of PixTools. */
        if ( false ) {
            System.err.println( "Skipping test due to suspected PixTools bug" );
        }
        else {
            int nside = 1 << 20;
            Vector3d pos = new Vector3d( -0.704, 0.580, 0.408 );
            double radius = 1.22E-7;
            List nestPixels =
                new PixTools().query_disc( nside, pos, radius, 1, 1 );
            List ringPixels =
                new PixTools().query_disc( nside, pos, radius, 0, 1 );
            assertEquals( ringPixels.size(), nestPixels.size() );
            for ( int i = 0; i < ringPixels.size(); i++ ) {
                long iring = ((Number) ringPixels.get( i )).longValue();
                long inest = new PixTools().ring2nest( nside, iring );
                ringPixels.set( i, new Long( inest ) );
            }
            Collections.sort( nestPixels );
            Collections.sort( ringPixels );
            assertEquals( nestPixels, ringPixels );
        }

        /* Actually I'm not certain these should be 1-element arrays - but
         * unless I've got very lucky with the vector position they should
         * be. */
        /* Following discussion with Nikolai Kouropatkine (Pixtools author) -
         * this is probably not a bug, but it would be nice if it behaved
         * otherwise (for performance reasons). */
        /* Version 30 April 2008 - behaves better than the pervious version did.
         * These queries now return 2 values rather than 4 values. */
        if ( false ) {
            System.err.println( "Skipping test due to known PixTools bug" );
        }
        else {

//          assertEquals( 1, // would be nice, but not available yet
//          assertTrue( 4 >= // 07-Jan-2008 version achieves this
//          assertTrue( 2 >= // 30-Apr-2008 version achieves this
            assertTrue( 4 >= // 28-Jul-2012 - reversion required by bugfix
                new PixTools().query_disc( 1024,
                                           new Vector3d( -0.704, 0.580, 0.408 ),
                                           1.22E-12,
                                           1, 1 ).size() );
//          assertEquals( 1, // would be nice, but not available yet
//          assertTrue( 4 >= // 07-Jan-2008 version achieves this
//          assertTrue( 2 >= // 30-Apr-2008 version achieves this
            assertTrue( 4 >= // 28-Jul-2012 - reversion required by bugfix
                new PixTools().query_disc( 1024,
                                           new Vector3d( -0.704, 0.580, 0.408 ),
                                           1.22E-12,
                                           0, 1 ).size() );
        }
    }

    public void testGetNSide() {

        /* Known to fail for early versions of PixTools. */
        double radius = 0.25 / 3600;
        for ( int i = 0; i < 20; i++ ) {
            long nside = new PixTools().GetNSide( radius * 60 * 60 );
            int l2n = (int) ( Math.log( nside ) / Math.log( 2 ) );
            assertEquals( (int) Math.pow( 2, l2n ), nside );
            radius *= 2;
        }
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
