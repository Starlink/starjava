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
        int ix = 0;

        assertEquals( 15, demoTables[ ix ].getColumnCount() );
        assertEquals( 875, demoTables[ ix++ ].getRowCount() );

        // assertEquals( 14, demoTables[ ix ].getColumnCount() );
        // assertEquals( 384L, demoTables[ ix++ ].getRowCount() );

        // assertEquals( 13, demoTables[ ix ].getColumnCount() );
        // assertEquals( 120L, demoTables[ ix++ ].getRowCount() );

        assertEquals( 10, demoTables[ ix ].getColumnCount() );
        assertEquals( 20L, demoTables[ ix++ ].getRowCount() );

        // assertEquals( 3, demoTables[ ix ].getColumnCount() );
        // assertEquals( 881L, demoTables[ ix++ ].getRowCount() );

        assertEquals( demoTables.length, ix );
    }
}
