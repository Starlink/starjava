package uk.ac.starlink.vo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.util.TestCase;

public class UpToDateTest extends TestCase {

    public static final String REG_WSDL = 
        "http://nvo.stsci.edu/voregistry/registry.asmx?WSDL";
    public static final String SESAME_WSDL =
        "http://cdsws.u-strasbg.fr/axis/services/Sesame?wsdl";

    public UpToDateTest( String name ) {
        super( name );
    }

    public void testWsdls() throws Exception {
        checkWsdl( "registry.wsdl", REG_WSDL );
        checkWsdl( "sesame.wsdl", SESAME_WSDL );
    }

    public void checkWsdl( String localName, String remoteName)
            throws Exception {
        URL localURL = getClass().getClassLoader().getResource( localName );
        URL remoteURL = new URL( remoteName );
        String[] localLines = getLines( localURL );
        String[] remoteLines = getLines( remoteURL );
        String msg = "Cached copy of registry WSDL is out of date; " +
                     "replace src/wsdl/registry.wsdl with " + REG_WSDL;
        assertArrayEquals( msg, localLines, remoteLines );
    }

    private String[] getLines( URL url ) throws IOException {
        List lineList = new ArrayList();
        BufferedReader in = 
            new BufferedReader( new InputStreamReader( url.openStream() ) );
        for ( String line; ( line = in.readLine() ) != null; ) {
            lineList.add( line );
        }
        return (String[]) lineList.toArray( new String[ 0 ] );
    }
}
