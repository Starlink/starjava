package uk.ac.starlink.votable;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.TestCase;

public class DOMTest extends TestCase {

    Document voDoc_;
    VOElement votEl_;
    TableElement tabEl_;

    public DOMTest( String name ) {
        super( name );
    }

    public void setUp() throws Exception {
        Document baseDoc = 
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                                  .newDocument();
        Element baseEl = baseDoc.createElement( "VOTABLE" );
        baseDoc.appendChild( baseEl );
        votEl_ = new VOElementFactory( StoragePolicy.PREFER_MEMORY )
                .makeVOElement( baseEl, null );
        voDoc_ = votEl_.getOwnerDocument();
        tabEl_ = (TableElement) 
                 votEl_.appendChild( voDoc_.createElement( "TABLE" ) );
    }

    public void testParse() throws IOException {
        FieldElement[] fields = makeFields( new String[] { "int", "float" } );
        for ( int i = 0; i < fields.length; i++ ) {
            tabEl_.appendChild( fields[ i ] );
        }

        VOElement tdEl = (VOElement) 
            tabEl_.appendChild( voDoc_.createElement( "DATA" ) )
                  .appendChild( voDoc_.createElement( "TABLEDATA" ) );

        tdEl.appendChild( makeRow( new String[] { "  1   ", "  1.  " } ) );
        tdEl.appendChild( makeRow( new String[] { "2", "2" } ) );
        tdEl.appendChild( makeRow( new String[] { "03", "+3e-00" } ) );
        tdEl.appendChild( makeRow( new String[] { " 0x2a ", " 42.000" } ) );
        tdEl.appendChild( makeRow( new String[] { "", "NaN" } ) );
        tdEl.appendChild( makeRow( new String[] { "urg", "" } ) );
        tdEl.appendChild( makeRow( new String[] { "1e1", "+Inf" } ) );
        tdEl.appendChild( makeRow( new String[] { "9999999999", "-Inf" } ) );

        final int BAD = Integer.MIN_VALUE;
        int[] intVals = new int[] { 1, 2, 3, 42, BAD, BAD, BAD, BAD };
        float[] floatVals = new float[] { 1.f, 2.f, 3.f, 42.f, Float.NaN,
                                          Float.NaN, Float.POSITIVE_INFINITY,
                                          Float.NEGATIVE_INFINITY };

        StarTable st = new VOStarTable( tabEl_ );
        int nrow = 8;
        assertTrue( st.isRandom() );
        assertEquals( nrow, st.getRowCount() );
        assertEquals( 2, st.getColumnCount() );

        for ( int irow = 0; irow < nrow; irow++ ) {
            Object[] row = st.getRow( (long) irow );
            int iv = intVals[ irow ];
            float fv = floatVals[ irow ];
            assertEquals( iv == BAD ? null : new Integer( iv ), 
                          row[ 0 ] );
            assertEquals( Float.isNaN( fv ) ? null : new Float( fv ), 
                          row[ 1 ] );
        }

    }

    FieldElement[] makeFields( String[] datatypes ) {
        int ncol = datatypes.length;
        FieldElement[] fields = new FieldElement[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            fields[ i ] = (FieldElement) voDoc_.createElement( "FIELD" );
            fields[ i ].setAttribute( "datatype", datatypes[ i ] );
        }
        return fields;
    }

    VOElement makeRow( String[] cells ) {
        int ncell = cells.length;
        VOElement rowEl = (VOElement) voDoc_.createElement( "TR" );
        for ( int i = 0; i < ncell; i++ ) {
            rowEl.appendChild( voDoc_.createElement( "TD" ) )
                 .appendChild( voDoc_.createTextNode( cells[ i ] ) );
        }
        return rowEl;
    }
}
