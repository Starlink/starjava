package uk.ac.starlink.vo;

import junit.framework.TestCase;

public class ResolverTest extends TestCase {

    public ResolverTest( String name ) {
        super( name );
    }

    public void testPosition() throws ResolverException {
        if ( AxisOK.isOK() ) {
            ResolverInfo info = ResolverInfo.resolve( "fomalhaut" );
            assertEquals( 344.412, info.getRaDegrees(), 0.001 );
            assertEquals( -29.622, info.getDecDegrees(), 0.001 );
        }
    }
}
