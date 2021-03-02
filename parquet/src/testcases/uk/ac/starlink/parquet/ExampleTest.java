package uk.ac.starlink.parquet;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLUtils;

public class ExampleTest extends TestCase {

    static {
        Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        ParquetUtil.silenceLog4j();
    }
 
    public void testExample() throws IOException {
        File file =
            URLUtils
           .urlToFile( ExampleTest.class.getResource( "example.parquet" )
           .toString() );
        IOSupplier<ParquetFileReader> pfrSupplier = getPfrSupplier( file );
        checkExample( new SequentialParquetStarTable( pfrSupplier ) );
        checkExample( new CachedParquetStarTable( pfrSupplier, 2 ) );
    }
 
    private void checkExample( ParquetStarTable pex ) throws IOException {
        StarTable ex = Tables.randomTable( pex );
        assertEquals( 3, ex.getRowCount() );
        assertEquals( 4, ex.getColumnCount() );
        assertColumnLike( ex.getColumnInfo( 0 ), "ints", Long.class );
        assertColumnLike( ex.getColumnInfo( 1 ), "fps", Double.class );
        assertColumnLike( ex.getColumnInfo( 2 ), "logs", Boolean.class );
        assertColumnLike( ex.getColumnInfo( 3 ), "strs", String.class );
        assertArrayEquals( ex.getRow( 0 ), new Object[] { 
            new Long( 1 ), new Double( 2.5 ), Boolean.TRUE, "foo",
        } );
        assertArrayEquals( ex.getRow( 1 ), new Object[] {
            new Long( 2 ), Double.valueOf( Double.NaN ), null, null,
        } );
        assertArrayEquals( ex.getRow( 2 ), new Object[] {
            new Long( 3 ), new Double( 99 ), Boolean.FALSE, "baz",
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
