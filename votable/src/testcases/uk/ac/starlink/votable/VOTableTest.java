package uk.ac.starlink.votable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.net.URL;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
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
        VOStarTable stab = new VOStarTable( tabclone );
        assertEquals( tab.getRowCount(), stab.getRowCount() );
        assertEquals( tab.getColumnCount(), stab.getColumnCount() );
        for ( int ir = 0; ir < nrow; ir++ ) {
            assertTrue( tab.hasNextRow() );
            assertTrue( stab.hasNext() );
            Object[] row = tab.nextRow();
            for ( int ic = 0; ic < ncol; ic++ ) {
                if ( row[ ic ].getClass().getComponentType() == null ) {
                    assertEquals( stab.getCell( (long) ir, ic ), row[ ic ] );
                }
                else if ( Array.getLength( row[ ic ] ) == 1 ) {
                    assertEquals( stab.getCell( (long) ir, ic ), 
                                  Array.get( row[ ic ], 0 ) );
                }
            }
        }
        assertTrue( ! tab.hasNextRow() );
        assertTrue( ! stab.hasNext() );

        DescribedValue parameter = stab.getParameterByName( param.getName() );
        assertTrue( stab.getParameters().contains( parameter ) );
        ValueInfo pinfo = parameter.getInfo();
        assertEquals( param.getValue(), parameter.getValue() );
        assertEquals( param.getName(), pinfo.getName() );
        assertEquals( String.class, pinfo.getContentClass() );
        assertEquals( param.getDescription(), pinfo.getDescription() );
    }

    
}
