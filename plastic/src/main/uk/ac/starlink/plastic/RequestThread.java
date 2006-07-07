package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Thread which executes a synchronous request on the hub.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2005
 */
class RequestThread extends Thread {

    private final Agent agent_;
    private final URI sender_;
    private final URI message_;
    private final List args_;
    private Object result_;
    private boolean done_;
    private IOException error_;

    /**
     * Constructor.
     *
     * @param  agent   agent representing the listening application
     * @param  sender  source of the message
     * @param  message message ID
     * @param  args    messsage arguments
     */
    public RequestThread( Agent agent, URI sender, URI message, List args ) {
        super( message + ": " + sender + " -> " + agent.getId() );
        agent_ = agent;
        sender_ = sender;
        message_ = message;
        args_ = args;
    }

    /**
     * Returns the agent this thread is talking to.
     *
     * @return  agent
     */
    public Agent getAgent() {
        return agent_;
    }

    public void run() {
        try {
            result_ = agent_.request( sender_, message_, args_ );
        }
        catch ( IOException e ) {
            error_ = e;
        }
        catch ( Throwable e ) {
            error_ = (IOException) new IOException( e.getMessage() )
                                  .initCause( e );
        }
        finally {
            done_ = true;
        }
    }

    /**
     * Returns the result of the request.
     *
     * @throws  IOException  if an exception was thrown from the 
     *          message execution
     * @throws  IllegalStateException   if the run method has not completed yet
     */
    public Object getResult() throws IOException {
        if ( done_ ) {
            if ( error_ != null ) {
                throw error_;
            }
            else {
                return result_;
            }
        }
        else {
            throw new IllegalStateException();
        }
    }
}
