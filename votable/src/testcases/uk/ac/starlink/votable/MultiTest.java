package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.TestCase;

public class MultiTest extends TestCase {

    public MultiTest() {
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testRW() throws IOException {
        StarTable[] tables = new StarTable[] {
            TableWriterTest.createTestTable( 1 ),
            TableWriterTest.createTestTable( 23 ),
            TableWriterTest.createTestTable( 109 ),
        };
        exerciseRW( new VOTableWriter(), new VOTableBuilder(), tables, 0 );
        exerciseRW( new FitsPlusTableWriter(), new FitsPlusTableBuilder(),
                    tables, 1 );
        exerciseRW( new FitsTableWriter(), new FitsTableBuilder(), tables, 1 );
    }

    public void exerciseRW( MultiStarTableWriter writer,
                            MultiTableBuilder reader,
                            StarTable[] tables, int offset )
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeStarTables( Tables.arrayTableSequence( tables ), bout );
        bout.close();
        byte[] buf = bout.toByteArray();
        StoragePolicy sp = StoragePolicy.PREFER_MEMORY;
        StarTable[] iTables = 
            Tables.tableArray(
                reader.makeStarTables( toDataSource( buf, null ), sp ) );
        for ( int itab = 0; itab < iTables.length; itab++ ) {
            assertEqualData( tables[ itab ], iTables[ itab ] );
            DataSource datsrc =
                toDataSource( buf, Integer.toString( itab + offset ) );
            assertEqualData( tables[ itab ],
                             reader.makeStarTable( datsrc, false, sp ) );
        }
    }

    private static DataSource toDataSource( byte[] buf, String pos ) {
        DataSource datsrc = new ByteArrayDataSource( "t", buf );
        datsrc.setPosition( pos );
        return datsrc;
    }

    private void assertEqualData( StarTable t1, StarTable t2 )
            throws IOException {
        assertEquals( t1.getRowCount(), t2.getRowCount() );
        assertEquals( t1.getColumnCount(), t2.getColumnCount() );
        RowSequence seq1 = t1.getRowSequence();
        RowSequence seq2 = t2.getRowSequence();
        while ( seq1.next() ) {
            assertTrue( seq2.next() );
            seq1.next();
            seq2.next();
            assertArrayEquals( seq1.getRow(), seq2.getRow() );
        }
        assertTrue( ! seq2.next() );
    }
}
