package uk.ac.starlink.ecsv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TestTableScheme;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class EcsvTest extends TestCase {

    private static final StoragePolicy STORAGE = StoragePolicy.PREFER_MEMORY;

    public EcsvTest() {
        LogUtils.getLogger( "uk.ac.starlink.ecsv" ).setLevel( Level.SEVERE );
    }

    public void testData2() throws IOException {
        StarTable table = readTable( "example2.ecsv" );
        checkRoundTripSerialize( table );
        assertEquals( 2, table.getRowCount() );
        assertEquals( 2, table.getColumnCount() );
        assertEquals( Double.valueOf( 4.0 ), table.getCell( 1, 0 ) );
        assertEquals( Long.valueOf( 3L ), table.getCell( 1, 1 ) );
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
        table.setName( "DATA1" );
        table = Tables.randomTable( table );
        Tables.checkTable( table );
        checkRoundTripSerialize( table );
        assertEquals( 12, table.getColumnCount() );
        assertEquals( Double.valueOf( 1.5 ), table.getCell( 1, 11 ) );
        assertEquals( "mbt",
                      table.getParameterByName( "author" ).getValue() );
        StarTable t2 = roundTrip( table, EcsvTableWriter.SPACE_WRITER );
        assertSameTable( table, t2 );
        StarTable t3 = roundTrip( table, EcsvTableWriter.COMMA_WRITER );
        assertSameTable( table, t3 );
        assertEquals( 1, countDummy( table ) );
        assertEquals( 1, countDummy( t3 ) );
        Object[] blankRow = table.getRow( 2 );
        for ( int i = 0; i < table.getColumnCount(); i++ ) {
            assertTrue( blankRow[ i ] == null ||
                        table.getColumnInfo( i ).getContentClass()
                                                .equals( Boolean.class ) );
        }
    }

    public void testScheme() throws IOException {
        TestTableScheme tscheme = new TestTableScheme();
        checkRoundTripSerialize( tscheme.createTable( "100,*" ) );
    }

    private void checkRoundTripSerialize( StarTable table ) throws IOException {
        EcsvTableBuilder rdr = new EcsvTableBuilder();
        roundTripSerialize( table, EcsvTableWriter.SPACE_WRITER, rdr );
        roundTripSerialize( table, EcsvTableWriter.COMMA_WRITER, rdr );
    }

    private void roundTripSerialize( StarTable t0,
                                     StarTableWriter writer,
                                     TableBuilder reader )
            throws IOException {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        writer.writeStarTable( t0, out1 );
        out1.close();
        byte[] buf1 = out1.toByteArray();
        StarTableFactory tfact = new StarTableFactory();
        tfact.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
        StarTable t1 =
            tfact.makeStarTable( new ByteArrayInputStream( buf1 ), reader );
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        writer.writeStarTable( t1, out2 );
        out2.close();
        byte[] buf2 = out2.toByteArray();
        assertArrayEquals( buf1, buf2 );
    }

    public void testSubtypes() throws IOException {
        // Tables from APE6 ECSV 1.0 update
        // https://github.com/astropy/astropy-APEs/blob/master/APE6.rst
        StarTable s1 = readTable( "subtype1.ecsv" );
        StarTable s2 = readTable( "subtype2.ecsv" );
        StarTable s3 = readTable( "subtype3.ecsv" );

        ColumnInfo c1a = s1.getColumnInfo( 0 );
        assertEquals( double[].class, c1a.getContentClass() );
        assertArrayEquals( new int[] { 2, 3 }, c1a.getShape() );
        assertEquals( 3.0, ((double[]) s1.getCell( 0, 0 ))[ 3 ] );
        assertTrue( Double.isNaN( ((double[]) s1.getCell( 1, 0 ))[ 3 ] ) );

        ColumnInfo c2a = s2.getColumnInfo( 0 );
        assertEquals( long[].class, c2a.getContentClass() );
        assertArrayEquals( new int[] { -1 }, c2a.getShape() );
        assertArrayEquals( new long[] { 8, 9, 10 }, s2.getCell( 2, 0 ) );

        ColumnInfo c3a = s3.getColumnInfo( 0 );
        assertEquals( String.class, c3a.getContentClass() );
        assertEquals( "true", s3.getCell( 2, 0 ) );
    }

    private void assertSameTable( StarTable t1, StarTable t2 ) {
        int ncol = t1.getColumnCount();
        int nrow = (int) t2.getRowCount();
        assertEquals( ncol, t2.getColumnCount() );
        assertEquals( nrow, t2.getRowCount() );
        assertEquals( t1.getName(), t2.getName() );
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
        }

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

    private int countDummy( StarTable table ) {
        int ndummy = 0;
        for ( int ic = 0; ic < table.getColumnCount(); ic++ ) {
            ColumnInfo cinfo = table.getColumnInfo( ic );
            Object dummy = cinfo.getAuxDatumValueByName( "dummy", String.class);
            if ( dummy != null ) {
                ndummy++;
            }
        }
        return ndummy;
    }

    private StarTable roundTrip( StarTable table, EcsvTableWriter writer )
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        writer.writeStarTable( table, bout );
        bout.close();
        return readTable( new ByteArrayDataSource( "x", bout.toByteArray() ) );
    }
}
