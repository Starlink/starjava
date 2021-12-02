package uk.ac.starlink.pds4;

import java.io.IOException;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class Pds4StarTableTest extends TestCase {

    public void testBinary() throws IOException {
        StarTable table =
            new Pds4TableBuilder()
           .makeStarTable( getDataSource( "Product_Table_Binary.xml" ),
                           false, StoragePolicy.PREFER_MEMORY );
        assertTrue( table.isRandom() );
        Tables.checkTable( table );
        assertEquals( 336, table.getRowCount() );
        assertEquals( 20, table.getColumnCount() );
        assertEquals( "BUTTERFLY_SWITCH_2",
                      table.getColumnInfo( 11 ).getName() );
        assertEquals( Short.class, table.getColumnInfo(13).getContentClass() );
        assertEquals( Double.class, table.getColumnInfo(17).getContentClass() );
        assertEquals( "second", table.getColumnInfo(0).getUnitString() );
        assertEquals( 0, ((Number) table.getCell( 86, 19 )).intValue() );
        assertEquals( 2048, ((Number) table.getCell( 87, 19 )).intValue() );
        assertEquals( 0, ((Number) table.getCell( 88, 19 )).intValue() );
        assertEquals( 0.0536, ((Number) table.getCell( 0, 8 )).doubleValue(),
                      0.00001 );
    }

    public void testCharacter() throws IOException {
        StarTable table =
            new Pds4TableBuilder()
           .makeStarTable( getDataSource( "Product_Table_Character.xml" ),
                           false, StoragePolicy.PREFER_MEMORY );
        assertTrue( table.isRandom() );
        Tables.checkTable( table );
        assertEquals( 23, table.getRowCount() );
        assertEquals( 10, table.getColumnCount() );
        assertEquals( "DV+", table.getColumnInfo( 4 ).getName() );
        assertEquals( 1.71, ((Number) table.getCell( 7, 4 )).doubleValue() );
        assertEquals( "SS1520900R6M1.IMG", table.getCell( 21, 9 ) );
    }

    public void testDelimited() throws IOException {
        StarTable table =
            new Pds4TableBuilder()
           .makeStarTable( getDataSource( "Product_Table_Delimited.xml" ),
                           false, StoragePolicy.PREFER_MEMORY );
        Tables.checkTable( table );
        assertFalse( table.isRandom() );
        table = Tables.randomTable( table );
        assertEquals( 3, table.getRowCount() );
        assertEquals( 13, table.getColumnCount() );
        assertEquals( "CHANNEL_NUMBER", table.getColumnInfo( 0 ).getName() );
        assertEquals( "SPECTRA_12", table.getColumnInfo( 12 ).getName() );
        assertEquals( 27, ((Number) table.getCell( 0, 0 )).intValue() );
        assertEquals( 10343, ((Number) table.getCell( 2, 12 )).intValue() );
    }

    public void testMulti() throws IOException {
        TableSequence tseq =
            new Pds4TableBuilder()
           .makeStarTables( getDataSource( "Product_Table_Multiple_Tables.xml"),
                            StoragePolicy.PREFER_MEMORY );
        StarTable t1 = tseq.nextTable();
        StarTable t2 = tseq.nextTable();
        assertNull( tseq.nextTable() );
        Tables.checkTable( t1 );
        Tables.checkTable( t2 );
        t1 = Tables.randomTable( t1 );
        t2 = Tables.randomTable( t2 );
        assertEquals( 10, t1.getColumnCount() );
        assertEquals( 10, t2.getColumnCount() );
        assertEquals( 23, t1.getRowCount() );
        assertEquals( 23, t2.getRowCount() );
        assertEquals( "SOL", t1.getColumnInfo( 0 ).getName() );
        assertEquals( "SOL", t2.getColumnInfo( 0 ).getName() );
        assertEquals( "SS091A990R6M1.IMG", t1.getCell( 0, 9 ) );
        assertEquals( "SS091AA00R6M1.IMG", t1.getCell( 4, 9 ) );
        assertEquals( "BLAH\"", t2.getCell( 4, 9 ) );
        assertEquals( "SS091AA00R6M1.IMG", t2.getCell( 5, 9 ) );
    }

    public void testVirs() throws IOException {
        DataSource datsrc = getDataSource( "virs_wavelengths.xml" );
        StoragePolicy storage = StoragePolicy.PREFER_MEMORY;
        Pds4TableBuilder builder = new Pds4TableBuilder();
        builder.setObservationalOnly( true );
        try {
            builder.makeStarTable( datsrc, false, storage );
            fail();
        }
        catch ( TableFormatException e ) {
            assertTrue( e.getMessage().indexOf( "No tables" ) >= 0 );
        }
        builder.setObservationalOnly( false );
        StarTable table = builder.makeStarTable( datsrc, false, storage );
        assertEquals( 105, table.getRowCount() );
        assertEquals( 2, table.getColumnCount() );
        Tables.checkTable( table );
        table = Tables.randomTable( table );
        assertEquals( 1, ((Number) table.getCell( 0, 0 )).intValue() );
        assertEquals( 303, ((Number) table.getCell( 0, 1 )).intValue() );
    }

    public void testArray() throws IOException {
        StarTable table =
            new Pds4TableBuilder()
           .makeStarTable( getDataSource( "gf1.lblx" ), false,
                           StoragePolicy.PREFER_MEMORY );
        assertEquals( 0, table.getColumnInfo( 1 ).getDomainMappers().length );
        DomainMapper[] tmappers = table.getColumnInfo( 0 ).getDomainMappers();
        assertEquals( 1, tmappers.length );
        TimeMapper tmapper = (TimeMapper) tmappers[ 0 ];
        Tables.checkTable( table );
        assertFalse( table.isRandom() );
        table = Tables.randomTable( table );
        assertArrayEquals( new int[] { 5 },
                           table.getColumnInfo( 6 ).getShape() );
        assertArrayEquals( new long[] { 2, 12, 22, 32, 42 },
                           table.getCell( 0, 7 ) );
        assertArrayEquals( new long[] { 2, 2, 2, 2, 2 },
                           table.getCell( 1, 7 ) );
        String time0 = (String) table.getCell( 0, 0 );
        assertEquals( "2019-08-06T01:00:00Z", time0 );
        // uk.ac.starlink.ttools.func.Times.isoToUnixSec("2019-08-06T01:00:00Z")
        assertEquals( 1565053200L, tmapper.toUnixSeconds( time0 ) );
    }

    private static DataSource getDataSource( String tname ) {
        return new URLDataSource( Pds4StarTableTest.class.getResource( tname ));
    }
}
