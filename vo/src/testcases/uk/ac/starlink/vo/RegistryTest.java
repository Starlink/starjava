package uk.ac.starlink.vo;

import junit.framework.TestCase;
import org.us_vo.www.SimpleResource;

public class RegistryTest extends TestCase {

    public RegistryTest( String name ) {
        super( name );
    }

    public void testContents() throws Exception {
        RegistryInterrogator queryer = new RegistryInterrogator();
        String query = "serviceType like 'CONE' and facility like 'HST'";
        SimpleResource[] data = queryer.getResources( query );
        assertTrue( data.length > 0 );
        SimpleResource r0 = data[ 0 ];
        assertEquals( "CONE", r0.getServiceType() );
        assertEquals( "HST", r0.getFacility() );
        ConeSearch coner = new ConeSearch( r0 );
    }
}
