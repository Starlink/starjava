package uk.ac.starlink.ttools.example;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.URLDataSource;

public class TwomassTest extends TestCase {

    public void testRead() throws IOException {
        URL url = TwomassTest.class.getResource( "psc_aaa.tail10.gz" );
        StoragePolicy storage = StoragePolicy.PREFER_MEMORY;
        StarTable table =
            new TwoMassPscTableBuilder()
           .makeStarTable( new URLDataSource( url ), true, storage );
        assertTrue( ! table.isRandom() );
        table = storage.copyTable( table );
        assertEquals( 10, table.getRowCount() );
        assertEquals( 60, table.getColumnCount() );
        assertEquals( "ra", table.getColumnInfo( 0 ).getName() );
        assertEquals( "coadd", table.getColumnInfo( 59 ).getName() );
        assertEquals( Short.valueOf( (short) 127 ), table.getCell( 0, 59 ) );
        assertEquals( Short.valueOf( (short) 115 ), table.getCell( 9, 59 ) );
    }
}
