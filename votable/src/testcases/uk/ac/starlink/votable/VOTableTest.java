package uk.ac.starlink.votable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.TestCase;

public class VOTableTest extends TestCase {

    public VOTableTest( String name ) {
        super( name );
    }

    public void testTable() throws SAXException, IOException {
        checkTable( StoragePolicy.PREFER_MEMORY );
        checkTable( StoragePolicy.PREFER_DISK );
        checkTable( StoragePolicy.DISCARD );
    }

    private void checkTable( StoragePolicy policy ) 
            throws SAXException, IOException {
        URL votloc = getClass().getResource( "docexample.xml" );
        VOElement vot = new VOElementFactory( policy ).makeVOElement( votloc );

        VOElement defs = vot.getChildByName( "DEFINITIONS" );
        VOElement coosys = defs.getChildByName( "COOSYS" );
        assertEquals( "2000.", coosys.getAttribute( "equinox" ) );
        assertEquals( "myJ2000", coosys.getID() );
        assertEquals( "Absent", coosys.getAttribute( "nope", "Absent" ) );

        VOElement res = vot.getChildByName( "RESOURCE" );
        ParamElement param = (ParamElement) res.getChildByName( "PARAM" );
        String pdesc = param.getDescription();
        assertTrue( pdesc.startsWith( "This parameter is designed" ) );
        String pval = param.getValue();
        String pobj = (String) param.getObject();
        assertEquals( pval, pobj );
 
        TableElement tab = (TableElement) res.getChildrenByName( "TABLE" )[ 0 ];
        int ncol = tab.getColumnCount();
        assertEquals( 4, ncol );
        long nrow = tab.getRowCount();

        if ( policy == StoragePolicy.DISCARD ) {
            assertEquals( 0L, nrow );
        }
        else {
            assertEquals( 3L, nrow );

            VOStarTable stab = new VOStarTable( tab );
            assertEquals( tab.getRowCount(), stab.getRowCount() );
            assertEquals( tab.getColumnCount(), stab.getColumnCount() );
            RowSequence rseq = stab.getRowSequence();
            RowStepper rstep = tab.getData().getRowStepper();
            List rows = new ArrayList();
            for ( int ir = 0; ir < nrow; ir++ ) {
                assertTrue( rseq.hasNext() );
                rseq.next();
                Object[] row = rstep.nextRow();
                assertNotNull( row );
                assertEquals( ncol, row.length );
                for ( int ic = 0; ic < ncol; ic++ ) {
                    if ( row[ ic ] == null ||
                         row[ ic ].getClass().getComponentType() == null ) {
                        assertEquals( rseq.getCell( ic ), row[ ic ] );
                    }
                    else if ( Array.getLength( row[ ic ] ) == 1 ) {
                        assertEquals( rseq.getCell( ic ), 
                                      Array.get( row[ ic ], 0 ) );
                    }
                }
                rows.add( row );
            }
            assertNull( rstep.nextRow() );
            assertTrue( ! rseq.hasNext() );

            assertEquals( "Procyon",
                          ((String) ((Object[]) rows.get( 0 ))[ 0 ]).trim() );
            assertEquals( "Vega",
                          ((String) ((Object[]) rows.get( 1 ))[ 0 ]).trim() );
            assertEquals( 12, 
                          ((int[]) ((Object[]) rows.get( 0 ))[ 3 ]).length );
            assertEquals( 6,
                          ((int[]) ((Object[]) rows.get( 1 ))[ 3 ]).length );

            Object[] row2 = (Object[]) rows.get( 2 );
            assertEquals( ncol, row2.length );
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object cell = row2[ icol ];
                assertTrue( cell == null ||
                            cell instanceof Number &&
                                Float.isNaN( ((Number) cell).floatValue() ) ||
                            cell instanceof String &&
                                ((String) cell).length() == 0 );
            }

            DescribedValue parameter =
                stab.getParameterByName( param.getName() );
            assertTrue( stab.getParameters().contains( parameter ) );
            ValueInfo pinfo = parameter.getInfo();
            assertEquals( param.getValue(), parameter.getValue() );
            assertEquals( param.getName(), pinfo.getName() );
            assertEquals( String.class, pinfo.getContentClass() );
            assertEquals( param.getDescription(), pinfo.getDescription() );
        }
    }

}
