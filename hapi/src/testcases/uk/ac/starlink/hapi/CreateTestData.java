package uk.ac.starlink.hapi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.util.IOUtils;

public class CreateTestData {

    public static void main( String[] args )
            throws IOException, URISyntaxException {
        for ( Map.Entry<String,String> entry : createFileMap().entrySet() ) {
            File file = new File( entry.getKey() );
            URL url = new URI( entry.getValue() ).toURL();
            System.out.println( file );
            try ( InputStream in = url.openStream();
                  OutputStream out = new FileOutputStream( file ) ) {
                IOUtils.copy( in, out );
            }
        }
    }

    private static Map<String,String> createFileMap() {
        Map<String,String> map = new LinkedHashMap<>();
        map.put( "example1-hdr.hapi",
                 "https://vires.services/hapi/info?dataset=GRACE_A_MAG" );
        map.put( "example1-bin.hapi",
                 "https://vires.services/hapi/data?dataset=GRACE_A_MAG" +
                 "&start=2010-01-01T00:00:00&stop=2010-01-01T00:00:10" +
                 "&format=binary&include=header" );
        map.put( "example1-csv.hapi",
                 "https://vires.services/hapi/data?dataset=GRACE_A_MAG" +
                 "&start=2010-01-01T00:00:00&stop=2010-01-01T00:00:10" +
                 "&format=csv&include=header" );
        // Attempting with format=binary fails (suspect service error).
        map.put( "example2.hapi",
                 "http://hapi-server.org/servers/TestData2.0/hapi/data?" +
                 "id=dataset1" +
                 "&time.min=1970-01-01Z&time.max=1970-01-01T00:00:11Z" +
                 "&include=header" );
        return map;
    }
}
