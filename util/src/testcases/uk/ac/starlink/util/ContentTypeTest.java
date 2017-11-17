package uk.ac.starlink.util;

import java.util.LinkedHashMap;
import java.util.Map;
import junit.framework.TestCase;

public class ContentTypeTest extends TestCase {

    public void testContentType() {
        ContentType dltype =
            ContentType.parseContentType( "APPLICATION / X-VOTABLE+XML"
                                        + "; content=datalink"
                                        + "; CHARSET=\"iso\\-8859\\-1\"" );
        assertTrue( dltype.matches( "application", "x-votable+xml" ) );
        assertTrue( dltype.matches( "application", "x-VOTABLE+xml" ) );
        assertFalse( dltype.matches( "text", "x-votable+xml" ) );
        assertFalse( dltype.matches( "application", "x-votable-xml" ) );
        assertEquals( "application", dltype.getType() );
        assertEquals( "x-votable+xml", dltype.getSubtype() );
        assertEquals( "datalink", dltype.getParameter( "content" ) );
        assertEquals( "datalink", dltype.getParameter( "Content" ) );
        assertEquals( "iso-8859-1", dltype.getParameter( "charset" ) );
        assertEquals( "iso-8859-1", dltype.getParameter( "CHARSET" ) );
        Map<String,String> pmap = new LinkedHashMap<String,String>();
        pmap.put( "charset", "iso-8859-1" );
        pmap.put( "content", "datalink" );
        assertEquals( pmap, dltype.getParameters() );
        assertEquals(
            "application/x-votable+xml; content=datalink; charset=iso-8859-1",
            dltype.toString() );

        ContentType wtype =
            ContentType
           .parseContentType( "text/quite.plain;author = \"M\\.B\\.T\\.\"" );
        assertEquals( "quite.plain", wtype.getSubtype() );
        assertEquals( "M.B.T.", wtype.getParameter( "author" ) );
        assertEquals( null, wtype.getParameter( "content" ) );
        assertEquals( "text/quite.plain; author=M.B.T.",
                      wtype.toString() );

        ContentType vtype =
            ContentType.parseContentType( " a / b;  c= \"X-\\\"Y\\\"-Z\"" );
        assertEquals( "X-\"Y\"-Z", vtype.getParameter( "c" ) );
        assertEquals( "a/b; c=\"X-\\\"Y\\\"-Z\"", vtype.toString() );

        ContentType qtype =
            ContentType.parseContentType( "a/2; q=\"X(Y)Z\"" );
        assertEquals( "X(Y)Z", qtype.getParameter( "q" ) );
        assertEquals( "a/2; q=\"X(Y)Z\"", qtype.toString() );

        assertNull( ContentType.parseContentType( "text-html" ) );
    }
}
