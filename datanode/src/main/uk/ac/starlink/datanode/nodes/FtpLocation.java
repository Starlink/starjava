package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Encapsulates the location of a directory on an FTP server.
 */
public class FtpLocation {

    private final URL baseURL;
    private final String path;
    private final String user;
    private final String password;

    private transient FtpLocation parentLoc;
    private transient Boolean hasParentLoc;

    private static Map clientPool = new HashMap();

    /**
     * Constructs a new FtpLocation from a URL-like string.
     * The string must be of the form 
     * "<tt>ftp://[user[:password]@]hostname[:port]path</tt>"
     * where <tt>path</tt> is a '/'-separated absolute pathname of
     * a directory on the server.
     *
     * @param   loc  url of the ftp directory
     */
    public FtpLocation( String loc ) throws NoSuchDataException {
        if ( ! loc.startsWith( "ftp://" ) ) {
            throw new NoSuchDataException( "Not an ftp-type URL" );
        }
        URL url;
        try {
            url = new URL( loc );
        }
        catch ( MalformedURLException e ) {
            throw new NoSuchDataException( "Bad URL: " + loc, e );
        }

        String base = url.getProtocol() + "://";
        if ( url.getUserInfo() != null ) {
            base += url.getUserInfo() + "@";
        }
        base += url.getHost();
        if ( url.getPort() >= 0 ) {
            base += ":" + url.getPort();
        }
     
        try {
            baseURL = new URL( base );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError();
        }

        if ( url.getUserInfo() == null ) {
            user = "anonymous";
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch ( UnknownHostException e ) {
                hostname = "unknown.host";
            }
            password = System.getProperty( "user.name" ) + '@' + hostname;
        }
        else {
            String info = url.getUserInfo();
            int colonpos = info.indexOf( ':' );
            if ( colonpos > 0 ) {
                user = info.substring( 0, colonpos );
                password = info.substring( colonpos + 1 );
            }
            else {
                user = info;
                password = "";
            }
        }
        this.path = url.getPath();
        checkPath( path );
    }

    /**
     * Constructs a new FtpLocation based on this one. 
     * The same host and authentication information will be used, but
     * the directory location is specified by the <tt>path</tt> argument,
     * which may be either absolute (starting with a '/') or relative
     * to this object's path.
     *
     * @param  floc  the object from which to copy connection information
     * @param  path  the absolute or relative path of the new location object
     */
    public FtpLocation( FtpLocation floc, String path )
            throws NoSuchDataException {
        this.baseURL = floc.baseURL;
        this.user = floc.user;
        this.password = floc.password;
        if ( path.indexOf( '/' ) == 0 ) {
            this.path = path;
            checkPath( path );
        }
        else {
            try {
                FTPClient client = getClient();
                boolean ok = true;
                synchronized ( client ) {
                    ok = ok && client.changeWorkingDirectory( path );
                    this.path = client.printWorkingDirectory();
                }
                checkOK( this.path != null && ok, client );
            }
            catch ( IOException e ) {
                throw new NoSuchDataException( e );
            }
        }
    }

    /**
     * Returns the full directory path for this node.
     *
     * @return  pathname of directory in ftp filesystem
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns a URL representing the connection used by this object.
     * This contains the host, but not the path to the directory.
     *
     * @return  connection URL
     */
    public URL getBaseURL() {
        return baseURL;
    }

    /**
     * Returns the URL representing this directory.
     *
     * @return  directory URL
     */
    public URL getURL() {
        try {
            return new URL( baseURL + path );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError();
        }
    }

    /**
     * Returns the FtpLocation representing the parent directory of this
     * one, if it has one.  If it has no parent (presumably because it
     * is the root) <tt>null</tt> is returned.
     *
     * @return  parent location, or <tt>null</tt>
     */
    public synchronized FtpLocation getParent() {
        if ( hasParentLoc == null ) {
            try {
                String parentPath = null;
                FTPClient client = getClient();
                synchronized ( client ) {
                    if ( client.changeWorkingDirectory( path ) &&
                         client.changeToParentDirectory() ) {
                        parentPath = client.printWorkingDirectory();
                    }
                }
                if ( parentPath != null && ! parentPath.equals( path ) ) {
                    try {
                        parentLoc = new FtpLocation( this, parentPath );
                    }
                    catch ( NoSuchDataException e ) {
                        parentLoc = null;
                    }
                }
                hasParentLoc = Boolean.valueOf( parentLoc != null );
            }
            catch ( IOException e ) {
                hasParentLoc = Boolean.FALSE;
            }
        }
        return parentLoc;
    }

    /**
     * Returns an FTP client for use at this location from a public pool.  
     * In general the pool keeps only one client for each host.  
     * Such clients may be in use by more than one thread at a time, 
     * so you should synchronise on them when performing a sequence 
     * of commands.  Steps are taken to ensure that the returned client
     * is not timed out (and is not just about to be).
     *
     * @return  a pooled client usable at this location
     */
    public synchronized FTPClient getClient() throws IOException {
        FTPClient client;
        if ( ! clientPool.containsKey( baseURL ) ) {
            client = getNewClient();
            clientPool.put( baseURL, client );
        }
        else {
            client = (FTPClient) clientPool.get( baseURL );
            try {
                client.sendNoOp();
            }
            catch ( FTPConnectionClosedException e ) {
                client.disconnect();
                client = getNewClient();
                clientPool.put( baseURL, client );
            }
        }
        try {
            client.sendNoOp();
        }
        catch ( FTPConnectionClosedException e ) {
            client.disconnect();
            clientPool.remove( baseURL );
            throw e;
        }
        return client;
    }

    /**
     * Returns a new FTP client for use at this location.
     * When such a client is being used within a single thread, no
     * synchronization is necessary.
     *
     * @return a new client usable at this location
     */
    private FTPClient getNewClient() throws IOException {
        FTPClient client = new FTPClient();
  // client.addProtocolCommandListener( new ProtocolLogger() );
        String host = baseURL.getHost();
        int port = baseURL.getPort();
        if ( port >= 0 ) {
            client.setDefaultPort( port );
        }
        try {

            /* Make and check the connection. */
            client.connect( host );
            if ( ! FTPReply.isPositiveCompletion( client.getReplyCode() ) ) {
                client.disconnect();
                throw new IOException( "FTP connection to " + host +
                                       " failed: " + client.getReplyString() );
            }

            /* Log in. */
            checkOK( client.login( user, password ), client );
        }
        catch ( IOException e ) {
            throw (IOException) new IOException( "FTP connection to " 
                                               + host + " failed" )
                               .initCause( e );
        }
        return client;
    }

    /**
     * Checks that a given path is accessible using this connection,
     * and throws an exception if it's not.
     *
     * @param   path to check
     * @throws  NoSuchDataException if <tt>path</tt> can't be reached
     */
    private void checkPath( String path ) throws NoSuchDataException {
        try {
            FTPClient client = getClient();
            synchronized ( client ) {
                checkOK( client.changeWorkingDirectory( path ), client );
            }
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( "Error setting directory " + path,
                                           e );
        }
    }

    /**
     * Throws an IOException based on FTP client status if a given value
     * is not true.
     *
     * @param   ok  whether to throw an exception or not
     * @param   client  the client responsible for any trouble
     */
    private void checkOK( boolean ok, FTPClient client ) throws IOException {
        if ( ! ok ) {
            throw new IOException( client.getReplyString() );
        }
    }

    /**
     * For debugging purposes this listener just prints out FTP protocol
     * messages.
     */
    private static class ProtocolLogger implements ProtocolCommandListener {
        static int nlogger = 0;
        String id = ++nlogger + " ";
        public void protocolCommandSent( ProtocolCommandEvent evt ) {
            System.out.print( id + "--> " + evt.getMessage() );
        }
        public void protocolReplyReceived( ProtocolCommandEvent evt ) {
            System.out.print( id + "<-- " + evt.getMessage() );
        }
    }
}
