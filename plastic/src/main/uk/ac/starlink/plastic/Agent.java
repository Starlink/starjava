package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Represents a server which is listening out for PLASTIC requests.
 * The hub keeps a list of agents which it can inform of relevant requests.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2006
 */
abstract class Agent {

    private final URI id_; 
    private final String name_;
    private final String label_;
    private final Collection supportedMessages_;

    /**
     * Constructs a new agent.
     *
     * @param   iseq  unique seqence id, used for disambiguating URIs
     * @param   name  generic name of the application
     * @param   supportedMessages   array of messages which this agent is
     *          interested in; an empty array means all messages
     */
    public Agent( int iseq, String name, URI[] supportedMessages ) {
        id_ = MinimalHub.createId( this, name, iseq );
        name_ = name;
        supportedMessages_ = new HashSet( Arrays.asList( supportedMessages ) );
        label_ = name + "-" + iseq;
    }

    /**
     * Returns the unique ID for this agent.
     *
     * @return  id
     */
    public URI getId() {
        return id_;
    }

    /**
     * Returns the generic application name for this agent.
     *
     * @return   application name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a human-readable description of the kind of connection
     * which exists between this agent and the hub.
     *
     * @return   connection description
     */
    public abstract String getConnection();

    /**
     * Indicates whether this agent is interested in receiving a message
     * with a given ID.
     *
     * @param   msg  message id
     * @param   true  iff such messages should be sent
     */
    public boolean supportsMessage( URI msg ) {
        return supportedMessages_.isEmpty()
            || supportedMessages_.contains( msg );
    }

    /**
     * Returns the list of messages supported by this agent.
     * An empty list means any message.
     *
     * @return   message ID array
     */
    public URI[] getSupportedMessages() {
        List msgs = new ArrayList( supportedMessages_ );
        Collections.sort( msgs );
        return (URI[]) msgs.toArray( new URI[ 0 ] );
    }

    /**
     * Executes a PLASTIC request synchronously.
     *
     * @param   sender   sender ID
     * @param   message  message ID
     * @param   args     argument list
     * @return   result of the request
     */
    public abstract Object request( URI sender, URI message, List args )
        throws IOException;

    public String toString() {
        return label_;
    }
}

