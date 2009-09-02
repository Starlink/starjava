package uk.ac.starlink.votable;

import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;

public class ParameterTest extends TestCase implements TableSink {

    DataSource votsrc = 
        DataSource.makeDataSource( getClass().getResource( "docexample.xml" ) );
    StoragePolicy policy = StoragePolicy.PREFER_MEMORY;
    boolean gotMeta;
    boolean gotRows;
    boolean gotEnd;

    public ParameterTest( String name ) {
        super( name );
    }

    public void testRead() throws IOException {
        checkReadParams( new VOTableBuilder()
                        .makeStarTable( votsrc, false, policy ) );
        checkReadParams( new VOTableBuilder()
                        .makeStarTable( votsrc, true, policy ) );
    }

    public void testDOM() throws IOException, SAXException {
        VOElement top = new VOElementFactory( policy )
                       .makeVOElement( votsrc.getURL() );
        TableElement vot = (TableElement) 
                           top.getElementsByVOTagName( "TABLE" ).item( 0 );
        StarTable st = new VOStarTable( vot );
        checkReadParams( st );
        checkWriteParams( st );
        checkWriteParams( new VOStarTable( vot ) );
    }

    public void testStream() throws IOException {
        assertTrue( ! ( gotMeta || gotRows || gotEnd ) );
        new VOTableBuilder().streamStarTable( votsrc.getInputStream(),
                                              this, null );
        assertTrue( gotMeta && gotRows && gotEnd );
    }

    private void checkReadParams( StarTable table ) {
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

    private void checkWriteParams( StarTable table ) {
        ValueInfo hatInfo = new DefaultValueInfo( "Hat", String.class );
        DescribedValue hatParam = new DescribedValue( hatInfo, "Panama" );
        table.setParameter( hatParam );
        assertEquals( "Panama", table.getParameterByName( "Hat" ).getValue() );
    }

    /*
     * TableSink implementation.
     */
    public void acceptMetadata( StarTable meta ) {
        checkReadParams( meta );
        gotMeta = true;
    }
    public void acceptRow( Object[] row ) {
        gotRows = true;
    }
    public void endRows() {
        gotEnd = true;
    }
}
