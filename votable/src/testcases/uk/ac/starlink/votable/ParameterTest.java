package uk.ac.starlink.votable;

import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

public class ParameterTest extends TestCase implements TableSink {

    DataSource votsrc = 
        DataSource.makeDataSource( getClass().getResource( "docexample.xml" ) );
    boolean gotMeta;
    boolean gotRows;
    boolean gotEnd;

    public ParameterTest( String name ) {
        super( name );
    }

    public void testRead() throws IOException {
        checkParams( new VOTableBuilder().makeStarTable( votsrc, false ) );
        checkParams( new VOTableBuilder().makeStarTable( votsrc, true ) );
    }

    public void testStream() throws IOException {
        assertTrue( ! ( gotMeta || gotRows || gotEnd ) );
        new VOTableBuilder().streamStarTable( votsrc.getInputStream(),
                                              this, null );
        assertTrue( gotMeta && gotRows && gotEnd );
    }

    private void checkParams( StarTable table ) {
        List params = table.getParameters();
        assertEquals( 3, params.size() );
        DescribedValue param0 = (DescribedValue) params.get( 0 );
        DescribedValue param1 = (DescribedValue) params.get( 1 );
        DescribedValue param2 = (DescribedValue) params.get( 2 );
        assertEquals( "Description", param0.getInfo().getName() );
        assertEquals( "Some bright stars", param0.getValue() );
        assertEquals( "Observer", param1.getInfo().getName() );
        assertEquals( "William Herschel", param1.getValue() );
        assertEquals( "Editor", param2.getInfo().getName() );
        assertEquals( "Mark Taylor", param2.getValue() );
    }

    /*
     * TableSink implementation.
     */
    public void acceptMetadata( StarTable meta ) {
        checkParams( meta );
        gotMeta = true;
    }
    public void acceptRow( Object[] row ) {
        gotRows = true;
    }
    public void endRows() {
        gotEnd = true;
    }
}
