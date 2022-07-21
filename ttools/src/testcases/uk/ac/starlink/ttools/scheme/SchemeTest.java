package uk.ac.starlink.ttools.scheme;

import java.io.IOException;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.filter.ArgException;
import uk.ac.starlink.util.LogUtils;

public class SchemeTest extends TestCase {

    public SchemeTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testScheme() throws IOException {
        StarTableFactory tfact = new StarTableFactory( false );
        tfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );

        tfact.addScheme( new AttractorScheme() );
        tfact.addScheme( new AttractorScheme() );
        tfact.addScheme( new SkySimScheme() );

        tryScheme( tfact, ":loop:10", 1, 10 );

        tryScheme( tfact,
                   ":class:" + AttractorScheme.class.getName() + ":10,rampe",
                   3, 10 );

        tryScheme( tfact, ":skysim:1e3", 7, 1000 );

        tryScheme( tfact, ":attractor:99,clifford", 2, 99 );
        tryScheme( tfact, ":attractor:101,rampe", 3, 101 );
    }

    public void testSkySim() throws IOException, ArgException {
        SkySimData simData = new SkySimScheme().readSimData();
        StarTable t10 = SkySimScheme.createBasicTable( simData, 10 );
        StarTable f10 = SkySimScheme.filterTable( t10 );
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
