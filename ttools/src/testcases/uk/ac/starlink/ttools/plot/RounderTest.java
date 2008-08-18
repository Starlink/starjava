package uk.ac.starlink.ttools.plot;

import junit.framework.TestCase;

public class RounderTest extends TestCase {

    public RounderTest( String name ) {
        super( name );
    }

    public void testLinear() {
        checkRounder( Rounder.LINEAR, Math.PI );
    }

    public void testLog() {
        checkRounder( Rounder.LOG, Math.PI );
    }

    public void checkRounder( Rounder rounder, double base ) {
        for ( int i = 0; i < 999; i++ ) {
            double rv = rounder.round( base );
            assertTrue( rv != base );
            assertTrue( rounder.nextUp( base ) > base );
            assertTrue( rounder.nextDown( base ) < base );
        }

        double lastUp = base;
        for ( int i = 0; i < 32; i++ ) {
            double val = rounder.nextUp( lastUp );
            assertTrue( val > lastUp );
            lastUp = val;
        }

        double lastDown = base;
        for ( int i = 0; i < 32; i++ ) {
            double val = rounder.nextDown( lastDown );
            assertTrue( val < lastDown );
            lastDown = val;
        }
    }
}
