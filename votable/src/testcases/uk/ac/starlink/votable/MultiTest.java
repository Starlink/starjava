package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;

public class MultiTest extends TestCase {

    public MultiTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testRW() throws IOException {
        StarTable[] tables = new StarTable[] {
            createTestTable( 1 ),
            createTestTable( 23 ),
            createTestTable( 109 ),
        };
        exerciseRW( new VOTableWriter(), new VOTableBuilder(), tables, 0 );
        exerciseRW( new UnifiedFitsTableWriter(), new FitsPlusTableBuilder(),
                    tables, 1 );
        UnifiedFitsTableWriter basicWriter = new UnifiedFitsTableWriter();
        basicWriter.setPrimaryType( UnifiedFitsTableWriter.BASIC_PRIMARY_TYPE );
        exerciseRW( basicWriter, new FitsTableBuilder(), tables, 1 );
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
        assertEquals( tables.length, iTables.length );
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

    private static StarTable createTestTable( int nrow ) {
        int[] cd1 = new int[ nrow ];
        float[] cd2 = new float[ nrow ];
        String[] cd3 = new String[ nrow ];
        for ( int i = 0; i < nrow; i++ ) {
            int j = i + 1;
            cd1[ i ] = j;
            cd2[ i ] = (float) j;
            cd3[ i ] = "row " + j;
        }
        ColumnInfo ci1 = new ColumnInfo( "ints", Integer.class, null );
        ColumnInfo ci2 = new ColumnInfo( "floats", Float.class, null );
        ColumnInfo ci3 = new ColumnInfo( "strings", String.class, null );
        ColumnStarTable t0 = ColumnStarTable.makeTableWithRows( nrow );
        t0.addColumn( ArrayColumn.makeColumn( ci1, cd1 ) );
        t0.addColumn( ArrayColumn.makeColumn( ci2, cd2 ) );
        t0.addColumn( ArrayColumn.makeColumn( ci3, cd3 ) );
        return t0;
    }
}
