package uk.ac.starlink.datanode.nodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.util.DataSource;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * A DataNode which represents a directory on an FTP server.
 * In some circumstances this can end up making rather a lot of
 * FTP connections.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FtpDirectoryDataNode extends DefaultDataNode {

    private final FtpLocation floc;
    private String name;

    public FtpDirectoryDataNode( FtpLocation floc ) {
        this.floc = floc;
        String url = floc.getURL().toExternalForm();
        if ( url.charAt( url.length() - 1 ) == '/' ) {
            url = url.substring( 0, url.length() - 1 );
        }
        setName( url.substring( url.lastIndexOf( '/' ) + 1 ) );
    }

    public FtpDirectoryDataNode( String location ) throws NoSuchDataException {
        this( new FtpLocation( location ) );
    }

    public String getNodeTLA() {
        return "FTP";
    }

    public String getNodeType() {
        return "FTP directory";
    }

    public String getPathSeparator() {
        return "/";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        List fileInfos;
        FTPFile[] files;
        try {
            FTPClient client = floc.getClient();
            synchronized ( client ) {
                setDir( client );
                files = client.listFiles();
            }

            /* Null is returned if the directory contains no files. */
            if ( files == null ) {
                files = new FTPFile[ 0 ];
            }
        }
        catch ( IOException e ) {
            return Collections.singleton( makeErrorChild( e ) ).iterator();
        }
        List fileList = Arrays.asList( files );
        Collections.sort( fileList, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ((FTPFile) o1).getName()
                      .compareTo( ((FTPFile) o2).getName() );
            }
        } );
        final Iterator it = fileList.iterator();
        return new Iterator() {
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                FTPFile file = (FTPFile) it.next();
                String fname = file.getName();
                DataNode node;
                if ( file.isDirectory() ) {
                    try {
                        node = makeChild( new FtpLocation( floc, fname ) );
                    }
                    catch ( NoSuchDataException e ) {
                        node = makeErrorChild( e );
                    }
                }
                else {
                    try {
                        node = makeChild( new FTPFileDataSource( file ) );
                    }
                    catch ( Exception e ) {
                        node = makeErrorChild( e );
                    }
                }
                node.setLabel( fname );
                return node;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Object getParentObject() {
        return floc.getParent();
    }

    private void setDir( FTPClient client ) throws IOException {
        if ( ! client.changeWorkingDirectory( floc.getPath() ) ) {
            throw new IOException( client.getReplyString() );
        }
    }

    /**
     * DataSource implementation which encapsulates the data-supplying
     * abilities of a file in the directory represented by this node.
     * <p>
     * The implementation is currently unsatisfactory; it is very hard
     * go get the threading right so that a transfer can be interrupted
     * if the stream returned from getRawInputStream() gets closed before
     * the end.  So at the moment, small files are cached in a 
     * byte buffer, and for large ones a new FTP connection is opened.
     * This works, but means you get a lot of FTP connections.
     */
    private class FTPFileDataSource extends DataSource {

        private FTPFile file;
        private URL url;
        private FTPClient readingClient;
        private byte[] content;

        public static final int INTRO_LENGTH = 4096;

        public FTPFileDataSource( FTPFile file ) throws IOException {
            super( INTRO_LENGTH );
            this.file = file;
            long size = file.getSize();
            String name = file.getName();
            setName( name );
            try {
                url = new URL( floc.getURL().toExternalForm() + '/' + name );
            }
            catch ( MalformedURLException e ) {
                url = null;
            }

            if ( size < INTRO_LENGTH ) {
                FTPClient client = floc.getClient();
                ByteArrayOutputStream ostrm = 
                    new ByteArrayOutputStream( (int) size );
                synchronized ( client ) {
                    setDir( client );
                    if ( ! client.retrieveFile( name, ostrm ) ) {
                        throw new IOException( client.getReplyString() );
                    }
                }
                content = ostrm.toByteArray();
            }
        }

        public URL getURL() {
            return url;
        }

        public long getRawLength() {
            return file.getSize();
        }

        protected InputStream getRawInputStream() throws IOException {
            if ( content != null ) {
                return new ByteArrayInputStream( content );
            }
            else if ( url == null ) {
                throw new IOException( "Bad URL" );
            }
            else {
                return url.openStream();
            }
        }

  // this doesn't work.
  //    protected InputStream getRawInputStream() throws IOException {
  //        final FTPClient client = floc.getClient();
  //        InputStream strm;
  //        synchronized ( client ) {
  //            setDir( client );
  //            client.setFileType( FTP.IMAGE_FILE_TYPE );
  //            strm = client.retrieveFileStream( file.getName() );
  //            if ( strm != null ) {
  //                readingClient = client;
  //                new Thread( "FTP reader: " + file.getName() ) {
  //                    public void run() {
  //                        synchronized ( client ) {
  //                            try {
  //                                client.completePendingCommand();
  //                            }
  //                            catch ( IOException e ) {
  //                                e.printStackTrace();
  //                            }
  //                            finally {
  //                                readingClient = null;
  //                            }
  //                        }
  //                    }
  //                }.start();
  //            }
  //            else {
  //                throw new IOException( client.getReplyString() );
  //            }
  //        }
  //        return new FilterInputStream( strm ) {
  //            public void close() throws IOException {
  //                FTPClient client = readingClient;
  //                if ( client != null ) {
  //                    client.abort();
  //                }
  //                super.close();
  //            }
  //        };
  //    }
    }
}
