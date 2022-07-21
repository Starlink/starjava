package uk.ac.starlink.ttools.example;

import java.io.IOException;
import java.util.logging.Level;
import java.net.URL;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;

public class AllWiseTest extends TestCase {

    public void testRead() throws IOException {
        LogUtils.getLogger( "uk.ac.starlink.table.examples" )
                .setLevel( Level.WARNING );
        URL url = AllWiseTest.class
                 .getResource( "wise-allwise-cat-part01.tail10.gz" );
        DataSource datsrc = new URLDataSource( url );
        StoragePolicy storage = StoragePolicy.PREFER_MEMORY;

        assertEquals( 10,
                      new AllWiseTableBuilder.Count()
                     .makeStarTable( datsrc, true, storage ).getRowCount() );
        assertEquals( -1,
                      new AllWiseTableBuilder.NoCount()
                     .makeStarTable( datsrc, true, storage ).getRowCount() );

        StarTable table =
            new AllWiseTableBuilder()
           .makeStarTable( new URLDataSource( url ), true, storage );
        assertTrue( ! table.isRandom() );
        table = storage.copyTable( table );
        assertEquals( 10, table.getRowCount() );
        assertEquals( 298, table.getColumnCount() );
        ColumnInfo raInfo = table.getColumnInfo( 1 );
        ColumnInfo htmInfo = table.getColumnInfo( 297 );
        assertEquals( "ra", raInfo.getName() );
        assertEquals( "deg", raInfo.getUnitString() );
        assertEquals( "J2000 right ascension", raInfo.getDescription() );
        assertEquals( "htm20", htmInfo.getName() );
        assertEquals( "J131609.66-741424.8", table.getCell( 0, 0 ) );
        assertEquals( new Long( 9111974345772L ), table.getCell( 9, 297 ) );
    }
}
