package uk.ac.starlink.parquet;

import java.io.File;
import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLUtils;

public class ExampleTest extends TestCase {
 
    public void testExample() throws IOException {
        StarTable ex = readTestTable( "example.parquet" );
        ex = Tables.randomTable( ex );
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
    }


    private void assertColumnLike( ColumnInfo cinfo,
                                   String name, Class<?> clazz ) {
        assertEquals( name, cinfo.getName() );
        assertEquals( clazz, cinfo.getContentClass() );
    }

    private StarTable readTestTable( String fname ) throws IOException {
        File file =
            URLUtils
           .urlToFile( ExampleTest.class.getResource( fname ).toString() );
        DataSource datsrc = new FileDataSource( file );
        return new ParquetTableBuilder().makeStarTable( datsrc, false, null );
    }
}
