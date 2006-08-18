package uk.ac.starlink.plastic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.votech.plastic.PlasticHubListener;

/**
 * Executes a remote control command in a running GAIA instance over
 * PLASTIC using the ivo://plastic.starlink.ac.uk/gaia/executeMd5 message.
 * This class is meant more as an example than as a usable item;
 * if the latter is required it should have better javadocs, error
 * handling and main() method, and should be moved somewhere other than
 * a testcases directory.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2006
 */
public class GaiaMd5 {

    private final String cmd_;
    private static final URI EXECUTE_MD5 = 
        PlasticUtils
       .createURI( "ivo://plastic.starlink.ac.uk/gaia/executeMd5" );
    private static final MessageDigest MD5_DIGEST;
    static {
        try {
            MD5_DIGEST = MessageDigest.getInstance( "MD5" );
        }
        catch ( NoSuchAlgorithmException e ) {
            throw (RuntimeException)
                  new UnsupportedOperationException( "Wot, no MD5?" )
                 .initCause( e );
        }
    }

    public GaiaMd5( String cmd ) {
        cmd_ = cmd;
    }

    public List execute() throws IOException {
        String checksum = getChecksum( getCookie(), cmd_ );
        List msgParams = Arrays.asList( new String[] { cmd_, checksum } );

        PlasticHubListener hub = PlasticUtils.getLocalHub();
        URI clientId = hub.registerNoCallBack( "GaiaMD5" );
        List results = null;
        try {
            Map map = hub.request( clientId, EXECUTE_MD5, msgParams );
            results = new ArrayList();
            for ( Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                results.add( entry.getValue() );
            }
        }
        finally {
            hub.unregister( clientId );
        }
        return results;
    }

    public static String getChecksum( byte[] cookie, String script ) {
        byte[] msg = new byte[ cookie.length + script.length() ];
        System.arraycopy( cookie, 0, msg, 0, cookie.length );
        for ( int i = 0; i < script.length(); i++ ) {
            msg[ i + cookie.length ] = (byte) script.charAt( i );
        }
        return md5sum( msg );
    }

    public static synchronized String md5sum( byte[] msg ) {
        MessageDigest digest = MD5_DIGEST;
        digest.reset();
        byte[] bsum = digest.digest( msg );
        assert bsum.length == 16;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < bsum.length; i++ ) {
            int ival = ( bsum[ i ] & 0xff );
            String hexdigs = Integer.toHexString( ival );
            if ( hexdigs.length() == 1 ) {
                hexdigs = "0" + hexdigs;
            }
            assert hexdigs.length() == 2;
            sbuf.append( hexdigs );
        }
        assert sbuf.length() == 32;
        return sbuf.toString();
    }

    public static byte[] getCookie() throws IOException {
        File file = new File( System.getProperty( "user.home" ), 
                              ".gaia-cookie" );
        InputStream in = new FileInputStream( file );
        byte[] buf = new byte[ 1024 ];
        int i = 0;
        try {
            for ( int c; ( c = in.read() ) >= 0 &&
                         i < buf.length &&
                         c != '\n'; ) {
                buf[ i++ ] = (byte) c;
            }
        }
        finally {
            in.close();
        }
        byte[] cookie = new byte[ i ];
        System.arraycopy( buf, 0, cookie, 0, i );
        return cookie;
    }

    public static void main( String[] args ) throws IOException {
        if ( args.length != 1 ) {
            throw new IllegalArgumentException( "usage: GaiaMd5 cmd" );
        }
        System.out.println( new GaiaMd5( args[ 0 ] ).execute() );
    }
}
