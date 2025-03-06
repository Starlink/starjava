package uk.ac.starlink.table;

import junit.framework.TestCase;

public class TablesTest extends TestCase {

    public void testParseCount() {
        assertEquals( 1_000_000, Tables.parseCount( "1000000" ) );
        assertEquals( 1_000_000, Tables.parseCount( "1_000_000" ) );
        assertEquals( 1_000_000, Tables.parseCount( "1e6" ) );
        assertEquals( (long) 1e12, Tables.parseCount( "1_000_000_000_000" ) );
        assertEquals( (long) 12e12, Tables.parseCount( "12e12" ) );
        assertEquals( (long) 1.2e13, Tables.parseCount( "1.2e13" ) );
        assertEquals( 256, Tables.parseCount( "0x100" ) );
        assertEquals( 0xDEADBEEFl, Tables.parseCount( "0xDead_Beef" ) );
        assertEquals( 0, Tables.parseCount( "0" ) );
        assertEquals( 0, Tables.parseCount( "00" ) );

        String[] bads = {
            "-1", "1 000 000", "1.5e-6", "1.1.1", "1.1.1e1", "2e-1", "0xHEAD",
        };
        for ( String txt : bads ) {
            try {
                Tables.parseCount( txt );
                fail( txt );
            }
            catch ( NumberFormatException e ) {
                // OK
            }
        }
    }

}
