package uk.ac.starlink.ttools;

import java.io.IOException;

/**
 * Exception thrown to indicate that the requested data are unavailable
 * since they come from a non-rewindable stream and have already been
 * read once.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Feb 2005
 */
public class StreamRereadException extends IOException {

    public StreamRereadException() {
        this( "Can't re-read data from stream" );
    }

    public StreamRereadException( String msg ) {
        super( msg );
    }
}
