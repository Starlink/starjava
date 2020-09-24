package uk.ac.starlink.topcat;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

public class DemoTest extends TestCase {

    static {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public DemoTest( String name ) {
        super( name );
    }

    public void testDemoTables() throws IOException {
        StarTable[] demoTables = Driver.getDemoTables();
        int ix = 0;

        assertEquals( 17, demoTables[ ix ].getColumnCount() );
        assertEquals( 875, demoTables[ ix++ ].getRowCount() );

        // assertEquals( 14, demoTables[ ix ].getColumnCount() );
        // assertEquals( 384L, demoTables[ ix++ ].getRowCount() );

        // assertEquals( 13, demoTables[ ix ].getColumnCount() );
        // assertEquals( 120L, demoTables[ ix++ ].getRowCount() );

        assertEquals( 12, demoTables[ ix ].getColumnCount() );
        assertEquals( 110L, demoTables[ ix++ ].getRowCount() );

        // assertEquals( 3, demoTables[ ix ].getColumnCount() );
        // assertEquals( 881L, demoTables[ ix++ ].getRowCount() );

        assertEquals( demoTables.length, ix );
    }

    public void testExamples() throws IOException {
        StarTableFactory tfact = TopcatPreparation.createFactory();
        for ( LoadWindow.Example ex : LoadWindow.createExamples() ) {
            StarTable table = tfact.makeStarTable( ex.location_ );
            assertTrue( table.getRowCount() > 100 );
        }
    }
}
