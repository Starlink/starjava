package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.URL;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ShapedArray;
import uk.ac.starlink.util.TestCase;

public class VOTableTest extends TestCase {

    public VOTableTest( String name ) {
        super( name );
    }

    public void testTable() throws IOException, SAXException {
        URL votloc = getClass().getResource( "docexample.xml" );
        VOTable vot = new VOTable( votloc, true );

        VOElement defs = vot.getChildByName( "DEFINITIONS" );
        VOElement coosys = defs.getChildByName( "COOSYS" );
        assertEquals( "2000.", coosys.getAttribute( "equinox" ) );
        assertEquals( "myJ2000", coosys.getID() );
        assertEquals( "Absent", coosys.getAttribute( "nope", "Absent" ) );

        VOElement res = vot.getChildByName( "RESOURCE" );
        Param param = (Param) res.getChildByName( "PARAM" );
        String pdesc = param.getDescription();
        assertTrue( pdesc.startsWith( "This parameter is designed" ) );
        String pval = param.getValue();
        String pobj = (String) param.getObject();
        assertEquals( pval, pobj );
 
        Table tab = (Table) res.getChildrenByName( "TABLE" )[ 0 ];
        int ncol = tab.getColumnCount();
        int nrow = tab.getRowCount();
        assertEquals( 2, nrow );
        assertEquals( 4, ncol );

        Table tabclone = Table.makeTable( tab.getSource() );
        RandomVOTable rtab = new RandomVOTable( tabclone );
        assertEquals( tab.getRowCount(), rtab.getRowCount() );
        assertEquals( tab.getColumnCount(), rtab.getColumnCount() );
        for ( int ir = 0; ir < nrow; ir++ ) {
            assertTrue( tab.hasNextRow() );
            Object[] row = tab.nextRow();
            for ( int ic = 0; ic < ncol; ic++ ) {
                assertEquals( row[ ic ].getClass(), 
                              rtab.getValueAt( ir, ic ).getClass() );
            }
        }
        assertTrue( ! tab.hasNextRow() );

        long[] as = tab.getField( 3 ).getArraysize();
        assertArrayEquals( as, new long[] { 2, 3, -1 } );
        ShapedArray sa0 = (ShapedArray) rtab.getValueAt( 0, 3 );
        ShapedArray sa1 = (ShapedArray) rtab.getValueAt( 1, 3 );
        assertArrayEquals( sa0.getDims(), new int[] { 2, 3, 2 } );
        assertArrayEquals( sa1.getDims(), new int[] { 2, 3, 1 } );
    }
}
