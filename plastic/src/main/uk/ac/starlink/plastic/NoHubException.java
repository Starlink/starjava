package uk.ac.starlink.plastic;

import java.io.IOException;

/**
 * Thrown if it seems that no PLASTIC hub is currently running.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2006
 */
public class NoHubException extends IOException {

    public NoHubException() {
        super();
    }

    public NoHubException( String msg ) {
        super( msg );
    }

    public NoHubException( Throwable e ) {
        super();
        initCause( e );
    }


    public NoHubException( String msg, Throwable e ) {
        super( msg );
        initCause( e );
    }
}
