package uk.ac.starlink.vo;

import java.net.ConnectException;
import junit.framework.TestCase;
import org.apache.axis.AxisFault;

public class ResolverNetTest extends TestCase {

    public ResolverNetTest( String name ) {
        super( name );
    }

    public void testPosition() throws ResolverException {
        try {
            ResolverInfo info = ResolverInfo.resolve( "fomalhaut" );
            assertEquals( 344.412, info.getRaDegrees(), 0.001 );
            assertEquals( -29.622, info.getDecDegrees(), 0.001 );
        }
        catch ( ResolverException e ) {
            if ( e.getCause() instanceof ConnectException ||
                 e.getCause() instanceof AxisFault ) {
                String msg = e.getCause().getMessage();
                if ( msg.indexOf( "timed out" ) > 0 ) {
                    System.err.println( "Connection to SIMBAD timed out" );
                    System.err.println( "SIMBAD service down?" );
                    return;
                }
            }                
            throw e;
        }
    }
}
