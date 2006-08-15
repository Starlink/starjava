package uk.ac.starlink.plastic;

import java.net.URI;
import java.util.List;

/**
 * Agent implementation which doesn't execute any requests.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2006
 */
class NoCallBackAgent extends Agent {

    /**
     * Constructor.
     *
     * @param   iseq  unique seqence id, used for disambiguating URIs
     * @param   name  generic name of the application
     */
    public NoCallBackAgent( int iseq, String name ) {
        super( iseq, name, new URI[ 0 ] );
    }

    public String getConnection() {
        return "None";
    }

    public boolean supportsMessage( URI message ) {
        return false;
    }

    public Object request( URI sender, URI message, List args ) {
        throw new UnsupportedOperationException();
    }
}
