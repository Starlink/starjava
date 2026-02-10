package uk.ac.starlink.table;

import junit.framework.TestCase;

public class DomainTest extends TestCase {
    public void testTime() {
        double unixSec = 946684800;
        double tol = 0.001;
        assertEquals( unixSec,
                      TimeMapper.DECIMAL_YEAR.toUnixSeconds( 2000.0 ), tol );
        assertEquals( unixSec,
                      TimeMapper.MJD.toUnixSeconds( 51544.0 ), tol );
        assertEquals( unixSec,
                      TimeMapper.JD.toUnixSeconds( 2451544.5 ), tol );
        assertEquals( unixSec,
                      TimeMapper.UNIX_SECONDS.toUnixSeconds( unixSec ), tol );
        assertEquals( unixSec,
                      TimeMapper.ISO_8601
                                .toUnixSeconds( "2000-01-01T00:00:00" ), tol );
        assertEquals( unixSec,
                      TimeMapper.ISO_8601
                                .toUnixSeconds( "2000-001 00:00:00" ), tol );

        assertEquals( 100_000, TimeMapper.JD.toJd( 100_000 ), tol );
        assertEquals( 1_000_000_000,
                      TimeMapper.UNIX_SECONDS.toUnixSeconds( 1_000_000_000 ),
                      tol );
    }
}
