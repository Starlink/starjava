package uk.ac.starlink.topcat;

import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;

public class DemoTest extends TestCase {

    static {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public DemoTest( String name ) {
        super( name );
    }

    public void testDemoTables() {
        StarTable[] demoTables = Driver.getDemoTables();
        assertEquals( 4, demoTables.length );

        assertEquals( 14, demoTables[ 0 ].getColumnCount() );
        assertEquals( 384L, demoTables[ 0 ].getRowCount() );

        assertEquals( 13, demoTables[ 1 ].getColumnCount() );
        assertEquals( 120L, demoTables[ 1 ].getRowCount() );

        assertEquals( 9, demoTables[ 2 ].getColumnCount() );
        assertEquals( 20L, demoTables[ 2 ].getRowCount() );

        assertEquals( 3, demoTables[ 3 ].getColumnCount() );
        assertEquals( 881L, demoTables[ 3 ].getRowCount() );
    }
}
