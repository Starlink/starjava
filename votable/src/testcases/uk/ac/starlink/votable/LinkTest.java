package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.net.URL;
import junit.framework.TestCase;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.LogUtils;

public class LinkTest extends TestCase {

    public LinkTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    public void testLink() throws Exception {
        ColumnInfo cinfo = new ColumnInfo( "NAME", String.class, "data" );
        
        ValueInfo linkInfo =
            new DefaultValueInfo( "pointer", URL.class, "URL link" );
        URL url = new URL( "http://www.rlyeh.mil/" );
        DescribedValue linkVal = 
            new DescribedValue( linkInfo, url );
        cinfo.setAuxDatum( linkVal );
        StarTable st = new RowListStarTable( new ColumnInfo[] { cinfo } );

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        new VOTableWriter( DataFormat.TABLEDATA, true )
            .writeStarTable( st, bOut );
        byte[] buf = bOut.toByteArray();

        VOElement vel = new VOElementFactory()
                       .makeVOElement( new ByteArrayInputStream( buf ), null );
        NodeList linkList = vel.getElementsByVOTagName( "LINK" );
        assertEquals( 1, linkList.getLength() );
        LinkElement link = (LinkElement) linkList.item( 0 );
        assertEquals( "pointer", link.getAttribute( "title" ) );
        assertEquals( "pointer", link.getHandle() );
        assertEquals( url, link.getHref() );
    }
}
