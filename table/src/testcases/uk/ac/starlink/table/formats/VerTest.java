package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;

public class VerTest extends TestCase {

    public void testSamples() throws IOException {
        VerTableBuilder builder = new VerTableBuilder();
        StoragePolicy storage = StoragePolicy.PREFER_MEMORY;
        for ( String fname :
              new String[] { "gaskell.ver", "rosetta.ver", "damit.ver" } ) {
            URL url = VerTest.class.getResource( fname );
            URL url2 = URLUtils.resolveLocation( url, "#2" );
            DataSource datsrc = new URLDataSource( url );
            DataSource datsrc2 = new URLDataSource( url2 );
            TableSequence tseq = builder.makeStarTables( datsrc, storage );
            StarTable t1a = tseq.nextTable();
            StarTable t2a = tseq.nextTable();
            assertNull( tseq.nextTable() );
            checkVertexFaceTables( t1a, t2a );
            StarTable t1b = builder.makeStarTable( datsrc, true, storage );
            StarTable t2b = builder.makeStarTable( datsrc2, true, storage );
            checkVertexFaceTables( t1a, t2a );
        }
    }

    private void checkVertexFaceTables( StarTable t1, StarTable t2 )
            throws IOException {
        assertEquals( 12, t1.getRowCount() );
        assertEquals( 20, t2.getRowCount() );

        // These regression tests may need to be changed if the output
        // format of the input handler changes.
        assertEquals( 1080574087, Tables.checksumData( t1 ) );
        assertEquals( 1175028969, Tables.checksumData( t2 ) );
    }
}
