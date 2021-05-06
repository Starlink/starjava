package uk.ac.starlink.ecsv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class EcsvTest extends TestCase {

    private static final StoragePolicy STORAGE = StoragePolicy.PREFER_MEMORY;

    public EcsvTest() {
        Logger.getLogger( "uk.ac.starlink.ecsv" ).setLevel( Level.WARNING );
    }

    public void testData2() throws IOException {
        StarTable table = readTable( "example2.ecsv" );
        assertEquals( 2, table.getRowCount() );
        assertEquals( 2, table.getColumnCount() );
        assertEquals( new Double( 4.0 ), table.getCell( 1, 0 ) );
        assertEquals( new Long( 3L ), table.getCell( 1, 1 ) );
    }

    private StarTable readTable( String name ) throws IOException {
        return readTable( new URLDataSource( getClass().getResource( name ) ) );
    }

    private StarTable readTable( DataSource datsrc ) throws IOException {
        StarTable table = new EcsvTableBuilder()
                         .makeStarTable( datsrc, true, STORAGE );
        assertFalse( table.isRandom() );
        table = STORAGE.copyTable( table );
        Tables.checkTable( table );
        return table;
    }

    public void testData1() throws IOException {
        StarTable table = readTable( "test1.ecsv" );
        table = Tables.randomTable( table );
        Tables.checkTable( table );
        assertEquals( 12, table.getColumnCount() );
        assertEquals( new Double( 1.5 ), table.getCell( 1, 11 ) );
        assertEquals( "mbt",
                      table.getParameterByName( "author" ).getValue() );
        StarTable t2 = roundTrip( table, EcsvTableWriter.SPACE_WRITER );
        assertSameTable( table, t2 );
        StarTable t3 = roundTrip( table, EcsvTableWriter.COMMA_WRITER );
        assertSameTable( table, t3 );
    }

    private void assertSameTable( StarTable t1, StarTable t2 ) {
        int ncol = t1.getColumnCount();
        int nrow = (int) t2.getRowCount();
        assertEquals( ncol, t2.getColumnCount() );
        assertEquals( nrow, t2.getRowCount() );
        int ndummy = 0;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo c1 = t1.getColumnInfo( ic );
            ColumnInfo c2 = t2.getColumnInfo( ic );
            assertEquals( c1.getName(), c2.getName() );
            assertEquals( c1.getContentClass(), c2.getContentClass() );
            assertEquals( c1.getUnitString(), c2.getUnitString() );
            assertEquals( c1.getUCD(), c2.getUCD() );
            assertEquals( c1.getXtype(), c2.getXtype() );
            Object dummy1 = c1.getAuxDatumValueByName( "dummy", String.class );
            Object dummy2 = c2.getAuxDatumValueByName( "dummy", String.class );
            assertEquals( dummy1, dummy2 );
            if ( dummy1 != null ) {
                ndummy++;
            }
        }
        assertEquals( 1, ndummy );

        assertEquals( t1.getParameters().size(), t2.getParameters().size() );
        for ( DescribedValue param1 : t1.getParameters() ) {
            String name = param1.getInfo().getName();
            DescribedValue param2 = t2.getParameterByName( name );
            Object pv1 = param1.getValue();
            Object pv2 = param2.getValue();
            if ( pv1.getClass().getComponentType() != null ) {
                assertArrayEquals( pv1, pv2 );
            }
            else {
                assertEquals( pv1, pv2 );
            }
        }
    }

    private StarTable roundTrip( StarTable table, EcsvTableWriter writer )
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeStarTable( table, bout );
        bout.close();
        return readTable( new ByteArrayDataSource( "x", bout.toByteArray() ) );
    }
}
