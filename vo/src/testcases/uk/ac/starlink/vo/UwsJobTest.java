package uk.ac.starlink.vo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import junit.framework.TestCase;

public class UwsJobTest extends TestCase {

    public void testTextPlain() throws IOException {
        String utf8 = "UTF-8";
        checkStringBytes( "tim", UwsJob.toTextPlain( "tim", utf8 ) );
        checkStringBytes( "tim tam\r\nchallenge",
                          UwsJob.toTextPlain( "tim tam\nchallenge", utf8 ) );
        checkStringBytes( "\r\none\r\ntwo\r\nthree\r\n",
                          UwsJob.toTextPlain( "\rone\ntwo\r\nthree\n", utf8 ) );
        checkStringBytes( "SELECT\r\n\r\nFROM",
                          UwsJob.toTextPlain( "SELECT\n\nFROM", utf8 ) );
    }

    public void testToPostedBytes() {
        Map map = new LinkedHashMap();
        map.put( "REQUEST", "doQuery" );
        checkStringBytes( "REQUEST=doQuery", UwsJob.toPostedBytes( map ) );
        map.put( "ADQL", "SELECT * FROM table" );
        checkStringBytes( "REQUEST=doQuery&ADQL=SELECT+*+FROM+table",
                          UwsJob.toPostedBytes( map ) );
    }

    public void testWriteHttpLine() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        UwsJob.writeHttpLine( bos, "Content-Type: text/x-votable+xml" );
        assertEquals( "Content-Type: text/x-votable+xml\r\n",
                      new String( bos.toByteArray(), "UTF-8" ) );
    }

    private void checkStringBytes( String str, byte[] bytes ) {
        assertEquals( str.length(), bytes.length );
        for ( int i = 0; i < bytes.length; i++ ) {
            assertEquals( str.charAt( i ), (char) bytes[ i ] );
        }
    }
}
