package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.TestCase;

public class ReferenceTest extends TestCase {

    URL url = getClass().getResource( "animals.xml" );

    public ReferenceTest( String name ) {
        super( name );
    }

    public void testDOM() throws IOException, SAXException {
        VOElement top = new VOElementFactory( StoragePolicy.PREFER_MEMORY )
                       .makeVOElement( url );
        NodeList tlist = top.getElementsByTagName( "TABLE" );
        assertEquals( 2, tlist.getLength() );
        TableElement t1 = (TableElement) tlist.item( 0 );
        TableElement t2 = (TableElement) tlist.item( 1 );
        assertEquals( t1.getFields().length, 6 );
        assertArrayEquals( t1.getFields(), t2.getFields() );
        assertEquals( 6, t1.getData().getRowCount() );
        assertEquals( 1, t2.getData().getRowCount() );
    }

    public void testStream() throws IOException, SAXException {
        VOTableBuilder builder = new VOTableBuilder();
        builder.streamStarTable( url.openStream(), new AnimalSink(), "0" );
        builder.streamStarTable( url.openStream(), new AnimalSink(), "1" );
        boolean done;
        try {
            builder.streamStarTable( url.openStream(), new AnimalSink(), "2" );
            done = true;
        }
        catch ( Throwable th ) {
            done = false;
        }
        assertTrue( ! done );
    }

    static class AnimalSink implements TableSink {
        long nrow;
        long irow;
        public void acceptMetadata( StarTable meta ) {
            assertEquals( 6, meta.getColumnCount() );
            nrow = meta.getRowCount();
        }
        public void acceptRow( Object[] row ) {
            irow++;
        }
        public void endRows() {
            assertTrue( irow == nrow || nrow == -1 );
            assertTrue( irow > 0 );
        }
    }
}
