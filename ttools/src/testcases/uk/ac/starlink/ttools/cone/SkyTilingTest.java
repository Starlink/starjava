package uk.ac.starlink.ttools.cone;

import gov.fnal.eag.healpix.PixTools;
import java.util.Random;
import javax.vecmath.Vector3d;
import junit.framework.TestCase;
import uk.ac.starlink.ttools.func.Tilings;

public class SkyTilingTest extends TestCase {

    private final Random random_ = new Random( 1122334455L );

    public SkyTilingTest( String name ) {
        super( name );
    }

    public void testHealpixNest() {
        if ( true ) {
            System.err.println( "Skipping query_disc nesting test" 
                              + " - known Pixtools bug" );
        }
        else {
            new PixTools().query_disc( 1048576,
                                       new Vector3d( -0.704, 0.580, 0.408 ),
                                       1.22E-7,
                                       1, 1 );
        }
    }

    public void testGetNSide() {
        if ( true ) {
            System.err.println( "Skipping GetNSide test "
                              + " - known Pixtools bug" );
        }
        else {
            double radius = 0.25 / 3600;
            for ( int i = 0; i < 20; i++ ) {
                long nside = new PixTools().GetNSide( radius * 60 * 60 );
                int l2n = (int) ( Math.log( nside ) / Math.log( 2 ) );
                assertEquals( (int) Math.pow( 2, l2n ), nside );
                radius *= 2;
            }
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
            long nside = Tilings.healpixNside( radius );
            checkTiling( new HealpixTiling( nside, true ), radius );
            checkTiling( new HealpixTiling( nside, false ), radius );
            radius *= 2;
        }
    }

    public void testPoints() {
        for ( int i = 0; i < 10; i++ ) {
            double ra = random_.nextDouble() * 360;
            double dec = ( random_.nextDouble() - 0.5 ) * 90;
            for ( int level = 2; level <= 20; level++ ) {
                int nside = (int) Math.pow( 2, level );
                assertEquals( (double) nside, Math.pow( 2, level ), 0.0 );
                assertEquals(
                    new HealpixTiling( nside, true ).getPositionTile( ra, dec ),
                    Tilings.healpixNestIndex( nside, ra, dec ) );
                assertEquals(
                    new HealpixTiling( nside, false ).getPositionTile( ra, dec),
                    Tilings.healpixRingIndex( nside, ra, dec ) );
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
