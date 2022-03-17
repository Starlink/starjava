package uk.ac.starlink.fits;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.storage.ListRowStore;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;

public class DemoTableTest extends TestCase {

    final Logger logger_;
    final DataSource demoSrc_;

    public DemoTableTest() {
        logger_ = Logger.getLogger( "uk.ac.starlink" );
        logger_.setLevel( Level.WARNING );
        demoSrc_ = new URLDataSource( getClass().getResource( "tables.fit" ) );
    }

    public void testDemoAscii() throws IOException {
        int offset = 0;
        InputStream in = demoSrc_.getInputStream();
        FitsHeader hdr0 = FitsUtil.readHeader( in );
        assertEquals( 1, hdr0.getHeaderBlockCount() );
        assertEquals( 0, hdr0.getDataBlockCount() );
        offset += hdr0.getHeaderByteCount();
        FitsHeader hdr1 = FitsUtil.readHeader( in );
        offset += hdr1.getHeaderByteCount();
        assertEquals( 2, hdr1.getHeaderBlockCount() );
        assertEquals( 5, hdr1.getDataBlockCount() );
        assertEquals( "J_ApJ_504_113_table3", hdr1.getStringValue( "EXTNAME" ));
        InputFactory infact1 =
            InputFactory.createFactory( demoSrc_, offset,
                                        hdr1.getDataByteCount() );
        offset += hdr1.getDataByteCount();
        checkAsciiTable( AsciiTableStarTable.createTable( hdr1, infact1 ) );
        ListRowStore store1 = new ListRowStore();
        AsciiTableStarTable.streamStarTable( hdr1, infact1.createInput( true ),
                                             store1 );
        checkAsciiTable( store1.getStarTable() );
    }

    public void testDemoBinary() throws IOException {
        InputStream in = demoSrc_.getInputStream();
        long offset = FitsUtil.skipHDUs( in, 2 );
        FitsHeader hdr = FitsUtil.readHeader( in );
        assertEquals( 1, hdr.getHeaderBlockCount() );
        assertEquals( 4, hdr.getDataBlockCount() );
        assertEquals( 1, hdr.getRequiredIntValue( "EXTVER" ) );
        assertEquals( 1L, hdr.getRequiredLongValue( "EXTVER" ) );
        assertEquals( Integer.valueOf( 1 ), hdr.getValue( "EXTVER" ) );
        assertEquals( Double.valueOf( 1.0 ), hdr.getDoubleValue( "EXTVER" ) );
        assertNull( hdr.getIntValue( "NOPE" ) );
        assertNull( hdr.getLongValue( "NOPE" ) );
        assertNull( hdr.getStringValue( "NOPE" ) );
        assertEquals( "AIPS CC", hdr.getStringValue( "EXTNAME" ) );
        InputFactory inputFact =
            InputFactory.createFactory( demoSrc_,
                                        offset + hdr.getHeaderByteCount(),
                                        hdr.getDataByteCount() );
        checkBintable( BintableStarTable
                      .createTable( hdr, inputFact,
                                    AbstractWideFits.DEFAULT ) );
        ListRowStore store = new ListRowStore();
        BintableStarTable.streamStarTable( hdr, inputFact.createInput( true ),
                                           (WideFits) null, store );
        checkBintable( store.getStarTable() );
    }

    private void checkAsciiTable( StarTable table ) throws IOException {
        Tables.checkTable( table );
        table = Tables.randomTable( table );
        assertEquals( 13, table.getColumnCount() );
        assertEquals( 138, table.getRowCount() );
        assertEquals( Integer.valueOf( 96 ), table.getCell( 3, 3 ) );
        assertArrayEquals( new Object[] {
            Double.valueOf( 0.0556 ),
            "00 42 41.58",
            "+40 51 56.9",
            Integer.valueOf( 72 ),
            "00 42 41.577",
            "+40 51 56.89",
            Float.valueOf( 24.5f ),
            null,
            Float.valueOf( 0.2f ),
            Float.valueOf( 25.43f ),
            ":",
            Float.valueOf( 0.26f ),
            null,
        }, squashBlanks( table.getRow( 23 ) ) );
    }

    private void checkBintable( StarTable table ) throws IOException {
        Tables.checkTable( table );
        table = Tables.randomTable( table );
        assertEquals( 3, table.getColumnCount() );
        assertEquals( 881, table.getRowCount() );
        assertEquals( 0.021926f, ((Float) table.getCell( 23, 0 )).floatValue(),
                      0.000001 );
        assertEquals( -1e-5f, ((Float) table.getCell( 23, 1 )).floatValue(),
                      1e-10 );
        assertEquals( -5.83333e-6, ((Float) table.getCell( 23, 2)).floatValue(),
                      1e-10 );
    }

    private static Object[] squashBlanks( Object[] row ) {
        int n = row.length;
        Object[] row1 = new Object[ n ];
        for ( int i = 0; i < n; i++ ) {
            row1[ i ] = Tables.isBlank( row[ i ] ) ? null : row[ i ];
        }
        return row1;
    }
}
