package uk.ac.starlink.table.join;

import gov.fnal.eag.healpix.PixTools;
import java.util.List;
import javax.vecmath.Vector3d;
import junit.framework.TestCase;

/**
 * All the tests in this class fail with versions of PixTools
 * prior to 2017-09-06.
 */
public class RingBugTest extends TestCase {

    public void testRingBug() {
        double PI = Math.PI;
        double scale = 0.0001 * PI;
        double decRad = PI / 18.0;
        double radius = scale * 4;
        PixTools pt = new PixTools();
        int nside = 1 << 2;
        int inclusive = 0;
        int nest = 0;
        Vector3d v1 = pt.Ang2Vec( PI/2 + decRad, -0.1 * scale );
        Vector3d v2 = pt.Ang2Vec( PI/2 + decRad, +0.1 * scale );
        List tiles1 = pt.query_disc( nside, v1, radius, nest, inclusive );
        List tiles2 = pt.query_disc( nside, v2, radius, nest, inclusive );
        assertEquals( 1, tiles1.size() );
        assertEquals( 1, tiles2.size() );

        // In PixTools 2012-07-28, tiles1 contains pixel 120 instead of 104.
        assertEquals( tiles2, tiles1 );

        long itile = pt.nest2ring( nside, pt.vect2pix_nest( nside, v2 ) );
        assertEquals( 104, itile );
        assertEquals( itile, ((Long) tiles1.get( 0 )).longValue() );
        assertEquals( itile, ((Long) tiles2.get( 0 )).longValue() );
    }

    public void testPoint4() {
        int nside = 2;
        PixTools pt = new PixTools();
        for ( double theta = 0; theta < Math.PI; theta += 0.1 ) {
            for ( double phi = -Math.PI; phi < Math.PI; phi += 0.1 ) {
                Vector3d v = pt.Ang2Vec( theta, phi );
                long irv1 = pt.vect2pix_ring( nside, v );
                long inv1 = pt.vect2pix_nest( nside, v );
                long irv2 = pt.nest2ring( nside, inv1 );

                long ira1 = pt.ang2pix_ring( nside, theta, phi );
                long ina1 = pt.ang2pix_nest( nside, theta, phi );
                long ira2 = pt.nest2ring( nside, ina1 );

                // This fails in some cases for 2012-07-28 PixTools version
                assertEquals( irv1, irv2 );
                assertEquals( ira1, ira2 );
            }
        }
    }

    public void testPoint5() {
        PixTools pt = new PixTools();
        double theta = 1.5;
        double phi = -0.04;
        Vector3d v = pt.Ang2Vec( theta, phi );
        int nside = 2;
        long inest = pt.vect2pix_nest( nside, v );
        long iring = pt.vect2pix_ring( nside, v );
        long iring2 = pt.nest2ring( nside, inest );

        // Specifically, this fails for 2012-07-28 PixTools version.
        assertEquals( iring, iring2 );
    }
}
