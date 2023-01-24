package uk.ac.starlink.topcat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.ac.starlink.util.CgiQuery;

/**
 * Extends CgiQuery so it can write to a local file.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Oct 2004
 */
public class TopcatCgiQuery extends CgiQuery {

    /**
     * Constructs a CGI query with no arguments.
     * 
     * @param  base  base part of the CGI URL (the bit before the '?')
     */
    public TopcatCgiQuery( String base ) {
        super( base );
    }

    /**
     * Sends this query and writes the result to a temporary file.
     *
     * @param  extension  file extension of file to write to
     * @return  file to which query result was written
     */
    public File executeAsLocalFile( String extension ) throws IOException {
        File file = File.createTempFile( "tcQuery", extension );
        file.deleteOnExit();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream( toURL().openStream() );
            out = new BufferedOutputStream( new FileOutputStream( file ) );
            for ( int b; ( b = in.read() ) >= 0; ) {
                out.write( b );
            }
        }
        finally {
            if ( in != null ) {
                in.close();
            }
            if ( out != null ) {
                out.close();
            }
        }
        return file;
    }
}
