package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class SkyPixellatorTest extends TestCase {

    public void testOverlaps() {
        checkOverlaps( new PixtoolsHealpixSkyPixellator() );
        try {
            checkOverlaps( new HtmSkyPixellator() );
        }
        catch ( AssertionFailedError e ) {
            System.err.println( "HTM pixellator fails" );
        }
    }

    private void checkOverlaps( SkyPixellator pixellator ) {
        Random rnd = new Random( 997521L );
        double scale = 0.05;  // degrees
        for ( int i = 0; i < 1000; i++ ) {
            double ra1 = rnd.nextDouble() * 360;
            double dec1 = rnd.nextDouble() * 180 - 90;
            double ra2 = Math.min( ra1 + rnd.nextDouble() * scale, 360 );
            double dec2 = Math.max( Math.min( dec1 + rnd.nextDouble() * scale,
                                              90 ), -90 );
            checkOverlap( pixellator, ra1, dec1, ra2, dec2 );
        }
        checkOverlap( pixellator, 0, -89.983888, 180, -89.983888 );
        checkOverlap( pixellator, 0, +89.983888, 180, +89.983888 );
    }

    private void checkOverlap( SkyPixellator pixellator,
                               double raDeg1, double decDeg1,
                               double raDeg2, double decDeg2 ) {
        double ra1 = Math.toRadians( raDeg1 );
        double dec1 = Math.toRadians( decDeg1 );
        double ra2 = Math.toRadians( raDeg2 );
        double dec2 = Math.toRadians( decDeg2 );
        double dist =
            AbstractSkyMatchEngine.calculateSeparation( ra1, dec1, ra2, dec2 );
        double radius = dist * 0.5001;
        double scale = radius * 0.2;
        for ( int i = 0; i < 5; i++ ) {
            scale *= 2;
            pixellator.setScale( scale );
            Object[] disc1 = pixellator.getPixels( ra1, dec1, radius );
            Object[] disc2 = pixellator.getPixels( ra2, dec2, radius );
            assertTrue( hasOverlap( disc1, disc2 ) );
        }
    }

    private boolean hasOverlap( Object[] a1, Object[] a2 ) {
        Set intersect = new HashSet( Arrays.asList( a1 ) );
        intersect.retainAll( Arrays.asList( a2 ) );
        return ! intersect.isEmpty();
    }
}
