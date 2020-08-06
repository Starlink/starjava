package uk.ac.starlink.ttools.scheme;

import java.io.IOException;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;

public class SchemeTest extends TestCase {

    public void testScheme() throws IOException {
        StarTableFactory tfact = new StarTableFactory( false );
        tfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );

        tryScheme( tfact, ":loop:10", 1, 10 );
    }

    private void tryScheme( StarTableFactory tfact, String txt,
                            int ncol, long nrow )
            throws IOException {
        StarTable table = tfact.makeStarTable( txt );
        assertEquals( ncol, table.getColumnCount() );
        assertEquals( nrow, table.getRowCount() );
        Tables.checkTable( table );
    }
}
