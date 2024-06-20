package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.table.DescribedValue;
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
        voDoc_ = new VODocument();
        votEl_ = (VOElement) 
                 voDoc_.appendChild( voDoc_.createElement( "VOTABLE" ) );
        tabEl_ = (TableElement) 
                 votEl_.appendChild( voDoc_.createElement( "RESOURCE" ) )
                       .appendChild( voDoc_.createElement( "TABLE" ) );
    }

    public void testLink() throws IOException {
        LinkElement linkEl = (LinkElement) 
                             tabEl_.getParent()
                            .appendChild( voDoc_.createElement( "LINK" ) );
        URL url = new URL( "http://www.starlink.ac.uk/stil/" );
        linkEl.setAttribute( "title", "STIL" );
        linkEl.setAttribute( "href", url.toString() );

        assertEquals( url, linkEl.getHref() );

        StarTable st = new VOStarTable( tabEl_ );
        DescribedValue dval = st.getParameterByName( "STIL" );
        assertEquals( URL.class, dval.getInfo().getContentClass() );
        assertEquals( url, dval.getValue() );
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
            assertEquals( iv == BAD ? null : Integer.valueOf( iv ), 
                          row[ 0 ] );
            assertEquals( Float.isNaN( fv ) ? null : Float.valueOf( fv ), 
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
