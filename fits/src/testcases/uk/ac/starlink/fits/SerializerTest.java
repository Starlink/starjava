package uk.ac.starlink.fits;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import uk.ac.starlink.table.LoopStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;

public class SerializerTest extends TestCase {

    public SerializerTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.SEVERE );
    }

    public void testBadRowCount() throws IOException {
        StarTable t0 = new LoopStarTable( "v", 0, 10, 1, true );
        int rdiff = 2;
        long nrow0 = t0.getRowCount();
        assertTrue( nrow0 > 0 );
        StarTable t1 = new WrapperStarTable( t0 ) {
            @Override
            public long getRowCount() {
                return nrow0 - rdiff;
            }
        };
        StarTable t2 = new WrapperStarTable( t0 ) {
            @Override
            public long getRowCount() {
                return nrow0 + rdiff;
            }
        };
        boolean broken1;
        try {
            Tables.checkTable( t1 );
            broken1 = false;
        }
        catch ( AssertionError e ) {
            broken1 = true;
        }
        assertTrue( broken1 );
        StarTableWriter writer = new FitsTableWriter();
        TableBuilder reader = new FitsTableBuilder();
        StarTable t1r = roundTrip( t1, writer, reader );
        StarTable t2r = roundTrip( t2, writer, reader );
        Tables.checkTable( t1r );
        Tables.checkTable( t2r );

        // These assertions don't really matter, as long as the checks
        // above pass.
        assertEquals( nrow0, t1r.getRowCount() );
        assertEquals( nrow0, t2r.getRowCount() );
    }

    public static StarTable roundTrip( StarTable table,
                                       StarTableWriter outHandler,
                                       TableBuilder inHandler )
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        outHandler.writeStarTable( table, out );
        out.close();
        DataSource datsrc = new ByteArrayDataSource( "t", out.toByteArray() );
        return inHandler
              .makeStarTable( datsrc, false, StoragePolicy.PREFER_MEMORY );
    }
}
