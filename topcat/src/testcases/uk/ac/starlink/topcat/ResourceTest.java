package uk.ac.starlink.topcat;

import java.io.FileNotFoundException;
import java.io.IOException;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

public class ResourceTest extends TestCase {

    public ResourceTest( String name ) {
        super( name );
    }

    public void testResourceIcon() throws FileNotFoundException {
        ResourceIcon.checkResourcesPresent();
    }

    public void testVersionString() {
        String version = AuxWindow.getVersion();
        assertTrue( version.matches( "^[0-9].*" ) );
    }

    public void testDemoTables() throws IOException {
        StarTable[] demoTables = Driver.getDemoTables();
        for ( int i = 0; i < demoTables.length; i++ ) {
            StarTable dt = demoTables[ i ];
            assertNotNull( dt );
            assertTrue( dt.isRandom() );
            assertTrue( dt.getRowCount() > 1 );
            assertTrue( dt.getColumnCount() > 1 );
            Tables.checkTable( dt );
        }
    }
}
