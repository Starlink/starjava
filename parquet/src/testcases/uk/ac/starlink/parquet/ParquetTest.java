package uk.ac.starlink.parquet;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Level;
import junit.framework.TestCase;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;

public class ParquetTest extends TestCase {

    static {
        LogUtils.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
    }

    public void testTimeMappers() {
        assertTrue( InputColumns.TIME_MAPPERS.keySet()
                   .containsAll( Arrays.asList( LogicalTypeAnnotation.TimeUnit
                                               .values() ) ) );
    }

    public void testTimeMapper() throws IOException {
        URL url = ParquetTest.class.getResource( "gold_vs_bitcoin.parquet" );
        StarTable table = new StarTableFactory( true )
                         .makeStarTable( new URLDataSource( url ) );
        int itcol = 0;
        ColumnInfo tinfo = table.getColumnInfo( itcol );
        assertEquals( 1, tinfo.getDomainMappers().length );
        TimeMapper tmapper = (TimeMapper) tinfo.getDomainMappers()[ 0 ];
        long t1micro = ((Long) table.getCell( 0, itcol )).longValue();
        double t1sec = tmapper.toUnixSeconds( table.getCell( 0, itcol ) );
        double t1year = t1sec / ( 365.25 * 24 * 3600 );
        assertTrue( t1micro > 1e15 && t1micro < 2e15 );
        assertEquals( 2024-1970, t1year, 1.0 );
    }

    public void testDump() throws IOException {
        URL url = ParquetTest.class.getResource( "example-zstd.parquet" );
        ParquetStarTable table = ParquetDump.readParquetTable( url.toString() );
        ParquetDump dump = new ParquetDump( table );
        for ( Function<ParquetDump,String> dumpFunc :
              ParquetDump.createDumpFunctionMap().values() ) {
            dumpFunc.apply( dump );
        }
    }
}
