package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;

public class GaiaTest extends TestCase {

    public GaiaTest( String name ) {
        super( name );
    }

    public void testGaia() throws IOException {
        if ( PlasticUtils.isHubRunning() &&
             new File( System.getProperty( "user.home" ), ".gaia-cookie" )
                .exists() ) {
            List resultList = new GaiaMd5( "expr 100 + 23" ).execute();
            Object result = Integer.valueOf( 123 );
            System.out.println( resultList );
            for ( Iterator it = resultList.iterator(); it.hasNext(); ) {
                assertEquals( result, it.next() );
            }
        }
    }
}
