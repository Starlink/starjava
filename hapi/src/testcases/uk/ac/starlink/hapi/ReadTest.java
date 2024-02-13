package uk.ac.starlink.hapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class ReadTest extends TestCase {

    public ReadTest() {
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testExample1() throws IOException {
        HapiTableBuilder builder = new HapiTableBuilder();
        StoragePolicy storage = StoragePolicy.getDefaultPolicy();
        for ( String f :
              new String[] { "example1-csv.hapi", "example1-bin.hapi" } ) {
            URL url = ReadTest.class.getResource( f );
            checkExample1( builder.makeStarTable( new URLDataSource( url ),
                                                  false, storage ) );
            RowStore rowStore = storage.makeRowStore();
            try ( InputStream in = url.openStream() ) {
                builder.streamStarTable( in, rowStore, null );
                checkExample1( rowStore.getStarTable() );
            }
        }

        checkExample2( builder.makeStarTable( datsrc( "example2.hapi" ),
                                                      false, storage ) );
    }

    private static DataSource datsrc( String f ) {
        return new URLDataSource( ReadTest.class.getResource( f ) );
    }

    private void checkExample1( StarTable t ) throws IOException {
        Tables.checkTable( t );
        t = Tables.randomTable( t );
        assertEquals( 4, t.getRowCount() );
        assertEquals( 14, t.getColumnCount() );
        ColumnInfo cinfo0 = t.getColumnInfo( 0 );
        assertArrayEquals( new DomainMapper[] { TimeMapper.ISO_8601 },
                           cinfo0.getDomainMappers() );
        assertEquals( String.class, cinfo0.getContentClass() );
        ColumnInfo cinfo10 = t.getColumnInfo( 10 );
        assertEquals( "B_NEC_Model", cinfo10.getName() );
        assertEquals( double[].class, cinfo10.getContentClass() );
        assertEquals( "CHAOS magnetic model", cinfo10.getDescription() );
        assertArrayEquals( new int[] { 3 }, cinfo10.getShape() );
        assertEquals( "nT", cinfo10.getUnitString() );

        assertEquals( -144.5455, getDouble( t.getCell( 3, 2 ) ), 1e-2 );
        assertEquals( -1.018, ((double[]) t.getCell( 1, 13 ))[ 1 ], 1e-2 );
    }

    private void checkExample2( StarTable t ) throws IOException {
        Tables.checkTable( t );
        t = Tables.randomTable( t );
        assertArrayEquals( new String[] { "F/F", "P/P", "P/F" },
                           t.getCell( 3, 8 ) );
        assertArrayEquals( new int[] { 0, 1, 2 },
                           t.getCell( 3, 9 ) );
        ColumnInfo specCinfo = t.getColumnInfo( 14 );
        assertEquals( "spectra", specCinfo.getName() );
        DescribedValue binsValue =
            specCinfo.getAuxData().stream()
           .filter( dval -> "frequency".equals( dval.getInfo().getName() ) )
           .findFirst().get();
        assertEquals( "Hz", binsValue.getInfo().getUnitString() );
        assertArrayEquals( new int[] { 10 }, binsValue.getInfo().getShape() );
        assertEquals( double[].class, binsValue.getInfo().getContentClass() );
        assertArrayEquals( new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, },
                           binsValue.getValue() );
    }

    private double getDouble( Object obj ) {
        return obj instanceof Number ? ((Number) obj).doubleValue()
                                     : Double.NaN;
    }
}
