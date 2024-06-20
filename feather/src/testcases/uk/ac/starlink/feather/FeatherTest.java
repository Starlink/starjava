package uk.ac.starlink.feather;

import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.TestCase;

public class FeatherTest extends TestCase {

    /**
     * Small test.  Much more comprehensive unit testing in
     * uk.ac.starlink.table.FormatsTest.
     */
    public void testData() throws IOException {
        StarTable dataTable = readTable( "data.fea" );
        assertEquals( Boolean.TRUE, dataTable.getCell( 0, 0 ) );
        assertNull( dataTable.getCell( 1, 0 ) );
        assertEquals( Double.valueOf( 10.0 ), dataTable.getCell( 0, 1 ) );
        assertNull( dataTable.getCell( 1, 1 ) );
        assertEquals( "red", dataTable.getCell( 0, 4 ) );
        assertNull( dataTable.getCell( 1, 4 ) );
    }

    private FeatherStarTable readTable( String name ) throws IOException {
        URL url = getClass().getResource( name );
        StarTable table = new FeatherTableBuilder()
                         .makeStarTable( new URLDataSource( url ), true,
                                         StoragePolicy.PREFER_MEMORY );
        Tables.checkTable( table );
        return (FeatherStarTable) table;
    }
}
