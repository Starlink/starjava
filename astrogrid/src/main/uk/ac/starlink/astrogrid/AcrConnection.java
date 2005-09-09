package uk.ac.starlink.astrogrid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Connection to an ACR server.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2005
 */
class AcrConnection extends Connection {

    private final AcrBranch root_;
    private final XmlRpcClient client_;

    /** Location in the user's home directory of the ACR rendezvous file. */
    public static String ACR_FILE = ".astrogrid-desktop";

    /** Number of bytes per chunk used in HTTP streamed output. */
    public static int HTTP_CHUNK = 1024 * 1024;

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    /**
     * Constructor.
     *
     * @param  connector   connector instance which dispatched this connection,
     *                     if any
     */
    public AcrConnection( AcrConnector connector ) throws IOException {
        super( connector, new HashMap() );
        URL url;
        try {
            url = getServerURL();
        }
        catch ( FileNotFoundException e ) {
            String msg = "The ACR is does not appear to be running (no ~/"
                       + ACR_FILE +")\n"
                       + "To enable MySpace access, start the Astrogrid "
                       + "Desktop\n" 
                       + "(http://software.astrogrid.org/jnlp/"
                       + "astrogrid-desktop/astrogrid-desktop.jnlp)";
            throw (IOException) new IOException( msg ).initCause( e );
        }
        client_ = new XmlRpcClient( getServerURL() );
        String homeUri = (String) execute( "astrogrid.myspace.getHome", null );
        root_ = new AcrBranch( this, homeUri, homeUri, null );
    }

    public boolean isConnected() {
        /* Hmm, should perhaps do something smarter than this. */
        return true;
    }

    public Branch getRoot() {
        return root_;
    }

    public void logOut() {
        /* Perform no action - a logout here doesn't want to cause a logout
         * for the ACR server itself. */
    }

    /**
     * Executes an XML-RPC command.  If the result represents an array
     * of arguments, it will be an <code>Object[]</code> array.
     * If it represents a struct, it will be a <code>Map</code>.
     * An <code>IOException</code> will be thrown if anything goes wrong.
     *
     * @param   fully-qualified command name
     * @args    array of arguments to pass to the XML-RPC service
     * @return   result of execution
     */
    public Object execute( String cmd, Object[] args ) throws IOException {
        Vector argv = args == null ? new Vector()
                                   : new Vector( Arrays.asList( args ) );
        logger_.info( "xmlrpc: " + cmd + argv );
        try {
            Object result = client_.execute( cmd, argv );
            if ( result instanceof IOException ) {
                throw (IOException) result;
            }
            else if ( result instanceof Throwable ) {
                Throwable err = (Throwable) result;
                String msg = err.getMessage();
                if ( msg == null ) {
                    msg = "ACR error";
                }
                throw (IOException) new IOException( msg ).initCause( err );
            }
            else if ( result instanceof Collection ) {
                return ((Collection) result).toArray();
            }
            else {
                return result;
            }
        }
        catch ( XmlRpcException e ) {
            throw (IOException)
                  new IOException( "Error communicating with ACR" )
                 .initCause( e );
        }
    }

    /**
     * Returns an output stream which can be used to write to a URI 
     * representing a location in MySpace (an Ivorn).
     *
     *
     * @param  outUri  output URI
     * @return  output stream which will write data to the location 
     *          represented by <code>outUri</code>
     */
    public OutputStream getOutputStream( String outUri )
            throws IOException {

        /* Not working, as far as I can see.  From Noel's comments it 
         * looks like the trouble is at the client end though, so 
         * probably fixable. */
        if ( false ) {
            try {
                return getHttpOutputStream( outUri );
            }
            catch ( IOException e ) {
                logger_.warning( "Failed to output using HTTP streaming " +
                                 "try temporary file" );
            }
        }

        /* If we've got here, we haven't managed an output stream any
         * other way, so do it the dumb way. */
        return getTempFileOutputStream( outUri );
    }

    /**
     * Returns an output stream which can be used to write to a URI 
     * doing it directly via a writable HTTP connection.
     *
     * @param  outUri  output URI
     * @return  output stream which will write data to the location 
     *          represented by <code>outUri</code>
     */
    private OutputStream getHttpOutputStream( String outUri )
            throws IOException {

        String outLoc =
            (String) execute( "astrogrid.myspace.getWriteContentURL",
                              new Object[] { outUri } );
        URL url = new URL( outLoc );

        /* Acquire and configure the HTTP connection. */
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setAllowUserInteraction( false );
        httpConn.setDoInput( false );
        httpConn.setDoOutput( true );
        httpConn.setUseCaches( false );
        httpConn.setRequestMethod( "PUT" );

        /* Attempt to configure for a streamed write.  This is only 
         * available at Java 1.5 and above. */
        try {
            httpConn.getClass().getMethod( "setChunkedStreamingMode",
                                           new Class[] { int.class } )
                    .invoke( httpConn,
                             new Object[] { new Integer( HTTP_CHUNK ) } );
            logger_.config( "Streaming with chunk size " + HTTP_CHUNK 
                          + "to remote URL " + url );
        }
        catch ( Throwable err ) {
            logger_.warning( "Can't stream to output URL (requires J2SE1.5)" );
        }

        /* Make the connection and return the result. */
        httpConn.connect();
        return new FilterOutputStream( httpConn.getOutputStream() ) {
            public void close() throws IOException {
                flush();
                super.close();
            }
        };
    }


    /**
     * Returns an output stream which can be used to write to a URI,
     * where the data is first written to a local temporary file. 
     * This is not very efficient or elegant, but may be used as a fallback
     * if something goes wrong with the HTTP method.
     *
     * <p>The implementation of this currently leaves rather a lot to 
     * be desired; the returned stream is a FileOutputStream pointing
     * at a temporary file, and when it's closed the file contents
     * are copied to MySpace.  This is not only inefficient (since 
     * you're doing an intermediate write to local disk, when it's 
     * got to go on a stream on the network eventually) but it 
     * screws up progress bar type information which describes how much
     * of a file has been written, since all the slow work is done
     * during the close.
     *
     * @param  outUri  output URI
     * @return  output stream which will write data to the location 
     *          represented by <code>outUri</code>
     */
    private OutputStream getTempFileOutputStream( final String outUri )
            throws IOException {
        final File file = File.createTempFile( "acr-temp", ".dat" );
        file.deleteOnExit();
        final String fileURL = file.toURL().toString();
        FileOutputStream ostrm = new FileOutputStream( file ) {
            public void close() throws IOException {
                super.close();      
                try {
                    execute( "astrogrid.myspace.copyURLToContent",
                             new Object[] { fileURL, outUri } );
                }         
                finally {
                    if ( file.delete() ) {
                        logger_.info( "Temporary file " + file + " deleted" );
                    }
                }
            }
        };
        logger_.info( "Writing MySpace-bound data to temp file " + file );
        return ostrm;
    }

    /**
     * Returns the URL of the ACR server, if there is one.
     * The URL is read from the {@link #ACR_FILE} file in the user's
     * home directory.
     *
     * @return   ACR server URL
     * @throws   IOException  if the URL can't be found
     */
    public static URL getServerURL() throws IOException {
        InputStream istrm = null;
        try {
            File rvfile = new File( System.getProperty( "user.home" ),
                                    ACR_FILE );
            istrm = new BufferedInputStream( new FileInputStream( rvfile ) );
            StringBuffer sbuf = new StringBuffer();
            for ( int c; ( c = istrm.read() ) >= 0 && c != '\n'; ) {
                sbuf.append( (char) c );
            }
            sbuf.append( "xmlrpc" );
            return new URL( sbuf.toString() );
        }
        finally {
            if ( istrm != null ) {
                try {
                    istrm.close();
                }
                catch ( IOException e ) {
                    // never mind.
                }
            }
        }
    }
}
