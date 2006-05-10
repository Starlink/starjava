package uk.ac.starlink.astrogrid;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import uk.ac.starlink.util.DataSource;

/**
 * DataSource implementation which reads from MySpace using ACR.
 * 
 * @author   Mark Taylor
 * @since    9 Sep 2005
 */
public class AcrDataSource extends DataSource {

    private final AcrConnection connection_;
    private final String uri_;

    /**
     * Constructor.
     *
     * @param   connection  connection object
     * @param   uri   ivorn of remote file
     */
    public AcrDataSource( AcrConnection connection, String uri ) {
        connection_ = connection;
        uri_ = uri;
    }

    public long getLength() {
        try {
            Map info = (Map) connection_
                            .execute( "astrogrid.myspace.getNodeInformation",
                                      new Object[] { uri_ } );
            return ((Number) info.get( "size" )).longValue();
        } 
        catch ( Exception e ) {
            return super.getLength();
        }
    }

    public InputStream getRawInputStream() throws IOException {
        String url = (String) connection_
                             .execute( "astrogrid.myspace.getReadContentURL",
                                       new Object[] { uri_ } );

        /* Wrap the URL stream in a BufferedInputStream.  This is to work
         * around persistent problems with markSupported() returning the
         * wrong value somewhere along the line.  See comments in 
         * DataSource source code. */
        /* Actually, this could probably be removed, since I am now
         * handling this using the mark.workaround system property.
         * Again, see DataSource. */
        return new BufferedInputStream( new URL( url ).openStream() );
    }
}
