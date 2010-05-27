package uk.ac.starlink.astrogrid.protocols.ivo;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import uk.ac.starlink.astrogrid.AcrConnection;

/**
 * URL connection used for MySpace I/O using the ACR.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2006
 */
public class IvoURLConnection extends URLConnection {

    private final AcrConnection aconn_;
    private InputStream inStrm_;
    private OutputStream outStrm_;

    /**
     * Constructor.
     *
     * @param   aconn  live connection to an ACR server
     * @param   url  ivo:-type URL as used by ACR MySpace communication
     *          locating the resource
     */
    public IvoURLConnection( AcrConnection aconn, URL url ) {
        super( url );
        aconn_ = aconn;
    }

    public void connect() throws IOException {
        if ( connected ) {
            return;
        }
        else if ( doInput && doOutput ) {
            throw new IOException( "Can't open ivo: URL "
                                 + "for both input and output" );
        }
        else if ( doInput ) {
            inStrm_ = createInputStream();
            connected = true;
        }
        else if ( doOutput ) {
            outStrm_ = createOutputStream();
            connected = true;
        }
        else {
            assert false;
        }
    }

    public InputStream getInputStream() throws IOException {
        connect();
        return inStrm_;
    }

    public OutputStream getOutputStream() throws IOException {
        connect();
        return outStrm_;
    }

    /**
     * Constructs an input stream which reads from the resource located
     * by this connection.
     *
     * @return  input stream
     */
    private InputStream createInputStream() throws IOException {

        /* Get the HTTP URL corresponding to this resource from the ACR. */
        String inLoc = aconn_.execute( "astrogrid.myspace.getReadContentURL",
                                       new String[] { url.toString() } )
                             .toString();

        /* Wrap it in a filter to work around that problem about lying
         * markSupported methods.  Return the result. */
        return new FilterInputStream( new URL( inLoc ).openStream() ) {
            public boolean markSupported() {
                return false;
            }
        };
    }

    /**
     * Constructs an output stream which writes to the resource located
     * by this connection.
     *
     * @return  output stream
     */
    private OutputStream createOutputStream() throws IOException {
        return aconn_.getOutputStream( url.toString() );
    }
}
