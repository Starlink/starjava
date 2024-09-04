package uk.ac.starlink.parquet;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOSupplier;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLUtils;

public class ExampleTest extends TestCase {

    static {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
    }
 
    public void testExamples() throws IOException {
        String[] compressFormats = {
            "none",
            "gzip",
            "snappy",
            "lz4",
        //  "brotli",  // can't find codec
        //  "zstd",    // parquet-mr uses zstd-jni
        };
        for ( String cf : compressFormats ) {
            String fname = "example-" + cf + ".parquet";
            File file = URLUtils
                       .urlToFile( ExampleTest.class.getResource( fname )
                                  .toString() );
            readCompressedFile( file );
        }
    }

    private void readCompressedFile( File file ) throws IOException {
        IOSupplier<ParquetFileReader> pfrSupplier = getPfrSupplier( file );
        checkExample( new SequentialParquetStarTable( pfrSupplier ) );
        checkExample( new CachedParquetStarTable( pfrSupplier, 2 ) );
    }
 
    private void checkExample( ParquetStarTable pex ) throws IOException {
        StarTable ex = Tables.randomTable( pex );
        assertEquals( 3, ex.getRowCount() );
        assertEquals( 5, ex.getColumnCount() );
        assertColumnLike( ex.getColumnInfo( 0 ), "ints", Long.class );
        assertColumnLike( ex.getColumnInfo( 1 ), "fps", Double.class );
        assertColumnLike( ex.getColumnInfo( 2 ), "logs", Boolean.class );
        assertColumnLike( ex.getColumnInfo( 3 ), "strs", String.class );
        assertColumnLike( ex.getColumnInfo( 4 ), "iarrs", long[].class );
        assertArrayEquals( ex.getRow( 0 ), new Object[] { 
            Long.valueOf( 1 ), Double.valueOf( 2.5 ), Boolean.TRUE, "foo",
            new long[] { 11, 12, 13, 14 },
        } );
        assertArrayEquals( ex.getRow( 1 ), new Object[] {
            Long.valueOf( 2 ), Double.valueOf( Double.NaN ), null, null,
            new long[] { 21, 22, 23, 24 },
        } );
        assertArrayEquals( ex.getRow( 2 ), new Object[] {
            Long.valueOf( 3 ), Double.valueOf( 99 ), Boolean.FALSE, "baz",
            new long[] { 31, 32, 33, 34 },
        } );
        ex.close();
    }

    private void assertColumnLike( ColumnInfo cinfo,
                                   String name, Class<?> clazz ) {
        assertEquals( name, cinfo.getName() );
        assertEquals( clazz, cinfo.getContentClass() );
    }

    private IOSupplier<ParquetFileReader> getPfrSupplier( File file )
            throws IOException {
        InputFile ifile = HadoopInputFile.fromPath( new Path( file.getPath() ),
                                                    new Configuration() );
        return () -> ParquetFileReader.open( ifile );
    }
}
