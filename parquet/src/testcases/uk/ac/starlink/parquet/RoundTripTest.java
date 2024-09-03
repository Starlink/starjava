package uk.ac.starlink.parquet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import junit.framework.TestCase;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TestTableScheme;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.FileDataSource;

public class RoundTripTest extends TestCase {

    private static final ValueInfo SOUP_INFO =
        new DefaultValueInfo( "SOUP", String.class, null );

    public void testRoundTrip() throws IOException {
        StarTable table = new TestTableScheme().createTable( "100,s" );
        String soup = "\u0411\u043e\u0440\u0449";
        table.getParameters().add( new DescribedValue( SOUP_INFO, soup ) );
        CompressionCodecName[] codecs = {
            null,
            CompressionCodecName.UNCOMPRESSED,
            CompressionCodecName.GZIP,
            CompressionCodecName.SNAPPY,
            CompressionCodecName.LZ4_RAW,
        };
        for ( CompressionCodecName codec : codecs ) {
            testRoundTrip( table, codec );
        }
        try {
            testRoundTrip( table, CompressionCodecName.LZ4 );
            fail();
        }
        catch ( RuntimeException e ) {
        }
    }

    private void testRoundTrip( StarTable table, CompressionCodecName codec )
            throws IOException {
        ParquetTableWriter writer = new ParquetTableWriter();
        writer.setCompressionCodec( codec );
        File f = File.createTempFile( "tmp-" + codec, "parquet" );
        f.deleteOnExit();
        try ( OutputStream out =
                  new BufferedOutputStream( new FileOutputStream( f ) ) ) {
            writer.writeStarTable( table, out );
        }
        StarTable table1 = new ParquetTableBuilder()
                          .makeStarTable( new FileDataSource( f ), false,
                           StoragePolicy.PREFER_MEMORY );
        assertSameTable( table, table1 );
        f.delete();
    }

    private void assertSameTable( StarTable t1, StarTable t2 ) {
        assertEquals( t1.getName(), t2.getName() );
        assertEquals( t1.getParameterByName( SOUP_INFO.getName() ).getValue(),
                      t2.getParameterByName( SOUP_INFO.getName() ).getValue() );
        assertEquals( Tables.tableToString( t1, "csv" ),
                      Tables.tableToString( t2, "csv" ) );
    }
}
