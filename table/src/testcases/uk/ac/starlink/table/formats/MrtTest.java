package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import junit.framework.TestCase;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;

public class MrtTest extends TestCase {

    public void testRead() throws IOException {
        String[] fnames = {
            "ajab4e1ct2-sub.mrt",
            "ajab4e9at3.mrt",
            "ajab5525t2-sub.mrt",
            "apjsab426bt3-sub.mrt",
            "apjsab4ea2t2-sub.mrt",
            "apjsab521at1-sub.mrt",
            "apjsab521at5.mrt",
            "apjsab530at1-sub.mrt",
            "datafileB1-sub.mrt",
        };
        MrtTableBuilder builder =
            new MrtTableBuilder( ErrorMode.FAIL, true, true );
        int nt = fnames.length;
        Map<String,StarTable> tmap = new LinkedHashMap<>();
        StarTable[] tables = new StarTable[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            String fname = fnames[ i ];
            StarTable table = readMrt( fname, builder );
            assertTrue( table.getColumnCount() > 2 );
            assertTrue( table.getRowCount() > 5 );
            Tables.checkTable( table );
            tmap.put( fname, table );
        }

        StarTable t1 = tmap.get( "ajab4e1ct2-sub.mrt" );
        assertEquals( "ajab4e1ct2_mrt.txt",
                      t1.getParameterByName( "filename" ).getValue() );
        assertEquals( "Pearson K.A.",
                      t1.getParameterByName( "Author" ).getValue() );
        assertEquals( 8, t1.getColumnCount() );
        assertEquals( 11, t1.getRowCount() );
        ColumnInfo t1c1 = t1.getColumnInfo( 1 );
        assertEquals( "Tmag", t1c1.getName() );
        assertEquals( "mag", t1c1.getUnitString() );

        StarTable t2 = tmap.get( "apjsab530at1-sub.mrt" );
        assertEquals( "NGC_1817", t2.getCell( 0, 20 ) );
        assertNull( t2.getCell( 1, 20 ) );

        StarTable t3 = tmap.get( "datafileB1-sub.mrt" );
        assertEquals( "+", t3.getCell( 1, 6 ) );
        assertEquals( "-", t3.getCell( 2, 6 ) );
    }

    private StarTable readMrt( String fname, MrtTableBuilder builder )
            throws IOException {
        StoragePolicy storage = StoragePolicy.PREFER_MEMORY;
        DataSource datsrc =
            new URLDataSource( MrtTest.class.getResource( fname ) );
        StarTable table = builder.makeStarTable( datsrc, true, storage );
        return storage.randomTable( table );
    }
}
