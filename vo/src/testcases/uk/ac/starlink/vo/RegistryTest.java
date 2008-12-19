package uk.ac.starlink.vo;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

public class RegistryTest extends TestCase {

    public RegistryTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
        Logger.getLogger( "org.apache.axis.utils" ).setLevel( Level.SEVERE );
    }

    public void testContents() throws Exception {
        RegistryQuery query =
            new RegistryQuery( RegistryQuery.AG_REG,
                               "capability/@standardID = '"
                             + RegCapabilityInterface.SSA_STDID + "'" );
        int nres = 0;
        int ncap = 0;
        for ( Iterator it = query.getQueryIterator(); it.hasNext(); ) {
            RegResource res = (RegResource) it.next();
            nres++;
            RegCapabilityInterface[] caps = res.getCapabilities();
            ncap += caps.length;
            for ( int ic = 0; ic < caps.length; ic++ ) {
                assert RegCapabilityInterface.SSA_STDID
                      .equals( caps[ ic ].getStandardId() );
            }
        }
        assertTrue( nres > 10 );
        assertTrue( ncap >= nres );
    }
}
