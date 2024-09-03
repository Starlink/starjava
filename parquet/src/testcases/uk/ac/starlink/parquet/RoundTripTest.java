package uk.ac.starlink.parquet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import junit.framework.TestCase;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.TestTableScheme;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.LogUtils;

public class RoundTripTest extends TestCase {

    private static final ValueInfo SOUP_INFO =
        new DefaultValueInfo( "SOUP", String.class, null );
    private final int icMeta_ = 1;

    public RoundTripTest() {
        LogUtils.getLogger( "uk.ac.starlink.parquet" )
                .setLevel( Level.WARNING );
    }

    public void testRoundTrip() throws IOException {
        StarTable table0 = new TestTableScheme().createTable( "100,s" );
        table0.setName( "test100" );
        String soup = "\u0411\u043e\u0440\u0449";
        table0.getParameters().add( new DescribedValue( SOUP_INFO, soup ) );
        ColumnInfo info1 = new ColumnInfo( table0.getColumnInfo( icMeta_ ) );
        info1.setUCD( "meta.code" );
        info1.setDescription( "This is a test column" );
        WrapperStarTable table = new WrapperStarTable( table0 ) {
            @Override
            public ColumnInfo getColumnInfo( int icol ) {
                return icol == icMeta_ ? info1 : super.getColumnInfo( icol );
            }
        };
        CompressionCodecName[] codecs = {
            null,
            CompressionCodecName.UNCOMPRESSED,
            CompressionCodecName.GZIP,
            CompressionCodecName.SNAPPY,
            CompressionCodecName.LZ4_RAW,
        };
        for ( CompressionCodecName codec : codecs ) {
            for ( boolean votmeta : new boolean[] { false, true } ) {
                testRoundTrip( table, codec, votmeta );
            }
        }
        try {
            testRoundTrip( table, CompressionCodecName.LZ4, false );
            fail();
        }
        catch ( RuntimeException e ) {
        }
    }

    private void testRoundTrip( StarTable table, CompressionCodecName codec,
                                boolean votmeta )
            throws IOException {
        ParquetTableWriter writer = new ParquetTableWriter();
        writer.setCompressionCodec( codec );
        writer.setVOTableMetadata( votmeta );
        File f = File.createTempFile( "tmp-" + codec, "parquet" );
        f.deleteOnExit();
        try ( OutputStream out =
                  new BufferedOutputStream( new FileOutputStream( f ) ) ) {
            writer.writeStarTable( table, out );
        }
        StarTable table1 = new ParquetTableBuilder()
                          .makeStarTable( new FileDataSource( f ), false,
                           StoragePolicy.PREFER_MEMORY );
        Tables.checkTable( table1 );
        assertSameTable( table, table1, votmeta );
        f.delete();
    }

    private void assertSameTable( StarTable t1, StarTable t2,
                                  boolean votmeta ) {
        assertEquals( Tables.tableToString( t1, "csv" ),
                      Tables.tableToString( t2, "csv" ) );

        assertEquals( t1.getName(), t2.getName() );
        assertEquals( t1.getParameterByName( SOUP_INFO.getName() ).getValue(),
                      t2.getParameterByName( SOUP_INFO.getName() ).getValue() );

        ColumnInfo cinfo1 = t1.getColumnInfo( icMeta_ );
        ColumnInfo cinfo2 = t2.getColumnInfo( icMeta_ );
        if ( votmeta ) {
            assertEquals( cinfo1.getUCD(), cinfo2.getUCD() );
            assertEquals( cinfo1.getDescription(), cinfo2.getDescription() );
        }
        assertNotNull( cinfo1.getUCD() );
        assertEquals( votmeta, cinfo2.getUCD() != null );
        assertNotNull( cinfo1.getDescription() );
        assertEquals( votmeta, cinfo2.getDescription() != null );
    }
}
