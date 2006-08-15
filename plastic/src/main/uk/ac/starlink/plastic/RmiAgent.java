package uk.ac.starlink.plastic;

import java.net.URI;
import java.util.List;
import org.votech.plastic.PlasticListener;

/**
 * Agent implementation which uses RMI-LITE for communications.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2006
 */
class RmiAgent extends Agent {

    private final PlasticListener listener_;

    /**
     * Constructor.
     *
     * @param   iseq  unique seqence id, used for disambiguating URIs
     * @param   name  generic name of the application
     * @param   listener   plastic listener object
     */
    public RmiAgent( int iseq, String name, URI[] supportedMessages, 
                     PlasticListener listener ) {
        super( iseq, name, supportedMessages );
        listener_ = listener;
    }

    public String getConnection() {
        return "Java RMI: " + listener_;
    }

    public Object request( URI sender, URI message, List args ) {
        return listener_.perform( sender, message, args );
    }
}
