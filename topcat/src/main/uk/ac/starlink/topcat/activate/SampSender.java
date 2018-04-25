package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.ComboBoxModel;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.ErrInfo;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.client.ResultHandler;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.GuiHubConnector;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.interop.SendManager;
import uk.ac.starlink.topcat.interop.SampCommunicator;
import uk.ac.starlink.topcat.interop.TopcatCommunicator;
import uk.ac.starlink.topcat.interop.TopcatSampControl;

/**
 * Takes care of sending SAMP messages from activation methods.
 *
 * <p>It does a similar job to its predecessor
 * {@link uk.ac.starlink.topcat.interop.SendManager},
 * which it cannibalises,
 * but unlike that class it provides support for synchronous message
 * sending (call/callAll), with result strings handed back to the
 * calling code rather than just discarded or pushed through the
 * logging system.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2018
 */
public class SampSender {

    private final String mtype_;
    private final SampCommunicator communicator_;
    private final GuiHubConnector connector_;
    private final SendManager sendManager_;
    private final ListModel clientListModel_;
    private int timeoutSec_;

    /**
     * Constructor.
     * The supplied MType determines what the offered list of
     * recipient clients will contain.
     *
     * @param   mtype  message type to which this sender is sensitive
     */
    public SampSender( String mtype ) {
        mtype_ = mtype;
        TopcatCommunicator communicator =
            ControlWindow.getInstance().getCommunicator();
        if ( communicator instanceof SampCommunicator ) {
            communicator_ = (SampCommunicator) communicator;
            connector_ = communicator_.getConnector();
            sendManager_ =
                new SendManager( communicator_.getConnector(), mtype );
            clientListModel_ = sendManager_.getClientListModel();
        }
        else {
            communicator_ = null;
            connector_ = null;
            sendManager_ = null;
            clientListModel_ = new AbstractListModel() {
                public int getSize() {
                    return 0;
                }
                public Object getElementAt( int i ) {
                    throw new IllegalArgumentException();
                }
            };
        }
        timeoutSec_ = 20;
    }

    /**
     * Indicates whether this sender has a chance of working.
     *
     * @return   false if this will never work
     */
    public boolean isAvailable() {
        return sendManager_ != null;
    }

    /**
     * Returns a list of the clients that are current possible targets
     * for this sender's messages (subscribed to the relevant MType).
     * If this list is empty, then the sender can't currently
     * do any useful work.
     *
     * @return   listmodel containing appropriately-subscribed clients
     */
    public ListModel getClientListModel() {
        return clientListModel_;
    }

    /**
     * Returns a ComboBoxModel listing clients that are subscribed to
     * this sender's MType.
     *
     * @return  client selection list
     */
    public ComboBoxModel getClientSelectionModel() {
        return sendManager_.getComboBoxModel();
    }

    /**
     * Returns this sender's Samp control.
     *
     * @return  samp control object
     */
    public TopcatSampControl getSampControl() {
        return communicator_.getSampControl();
    }

    /**
     * Returns this sender's hub connector.
     *
     * @return  connector object
     */
    public GuiHubConnector getConnector() {
        return connector_;
    }

    /**
     * Invoked to perform an activation action which involves sending
     * a SAMP message to this sender's currently selected target client(s).
     *
     * @param  message  message to send
     * @return   outcome
     */
    public Outcome activateMessage( Map message ) {
        if ( !isAvailable() ) {
            return Outcome.failure( "No SAMP at all." );
        }
        try {
            if ( connector_.getConnection() == null ) {
                return Outcome.failure( "SAMP not available (no hub?)" );
            }
            Client client = sendManager_.getSelectedClient();

            /* Targeted to a single client. */
            if ( client != null ) {
                Response response =
                    connector_.callAndWait( client.getId(), message,
                                            timeoutSec_ );
                return singleResponse( client, response );
            }

            /* Broadcast to all clients. */
            else {
                MapResultHandler handler = new MapResultHandler();
                connector_.callAll( message, handler, timeoutSec_ );
                try {
                    synchronized ( handler ) {
                        while ( ! handler.isDone() ) {
                            handler.wait();
                        }
                    }
                }
                catch ( InterruptedException e ) {
                    return Outcome.failure( "Broadcast wait interruped" );
                }
                Map<Client,Response> responseMap = handler.responseMap_;
                if ( responseMap.isEmpty() ) {
                    return Outcome.failure( "No responses" );
                }
                else if ( responseMap.size() == 1 ) {
                    Map.Entry<Client,Response> entry =
                        responseMap.entrySet().iterator().next();
                    return singleResponse( entry.getKey(), entry.getValue() );
                }
                else {
                    return multiResponse( responseMap );
                }
            }
        }
        catch ( SampException e ) {
            return Outcome.failure( e );
        }
    }

    /**
     * Returns a message explaining why this sender can't do any useful
     * work at the moment.  If it can, null is returned.
     *
     * @return  unavailablity message, or null if all is working
     */
    public String getUnavailableText() {
        if ( !isAvailable() ) {
            return "No SAMP at all.";
        }
        else if ( !connector_.isConnected() ) {
            return "SAMP not available (no hub?)";
        }
        else if ( clientListModel_.getSize() == 0 ) {
            return "No SAMP clients subscribed to " + mtype_;
        }
        else {
            return null;
        }
    }

    /**
     * Handles a SAMP response when only one is received.
     *
     * @param  client  sole responding client
     * @param  response  client's response
     * @return outcome
     */
    private Outcome singleResponse( Client client, Response response ) {
        String status = response.getStatus();
        ErrInfo errInfo = response.getErrInfo();
        if ( response.isOK() ) {
            String txt = "Successfully sent to " + client;
            try {
                Map result = response.getResult();
                if ( result != null && ! result.isEmpty() ) {
                    txt += ": " + result;
                }
            }
            catch ( RuntimeException e ) {
                txt += ": " + e;
            }
            return Outcome.success( txt );
        }
        else if ( Response.WARNING_STATUS.equals( status ) ) {
            String txt = "Warning response from " + client;
            if ( errInfo != null ) {
                txt += ": " + errInfo.getErrortxt();
            }
            return Outcome.success( txt );
        }
        else if ( Response.ERROR_STATUS.equals( status ) ) {
            String txt = "Error response from " + client;
            if ( errInfo != null ) {
                txt += ": " + errInfo.getErrortxt();
            }
            return Outcome.failure( txt );
        }
        else {
            return Outcome.failure( "Unexpected state" );
        }
    }

    /**
     * Handles SAMP responses when there are multiple ones
     * (presumably following a broadcast).
     *
     * @param  responseMap  map of client-&gt;response pairs
     * @return  outcome
     */
    private Outcome multiResponse( Map<Client,Response> responseMap ) {
        StringBuffer okBuf = new StringBuffer();
        StringBuffer errBuf = new StringBuffer();
        int nOk = 0;
        int nErr = 0;
        for ( Map.Entry<Client,Response> entry : responseMap.entrySet() ) {
            boolean isOk = entry.getValue().isOK();
            if ( isOk ) {
                nOk++;
            }
            else {
                nErr++;
            }
            StringBuffer sbuf = isOk ? okBuf : errBuf;
            if ( sbuf.length() > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( SampUtils.toString( entry.getKey() ) );
        }
        if ( nOk == 0 && nErr == 0 ) {
            return Outcome.failure( "No responses" );
        }
        String okTxt = "" + nOk + " OK "
                     + ( nOk == 1 ? "response" : "responses" )
                     + " (" + okBuf + ")";
        String errTxt = "" + nErr + " non-OK "
                     + ( nErr == 1 ? "response" : "responses" )
                     + " (" + errBuf + ")";
        if ( nOk > 0 ) {
            return Outcome
                  .success( okTxt + ( nErr > 0 ? ( "; " + errTxt ) : "" ) );
        }
        else {
            assert nErr > 0;
            return Outcome
                  .failure( errTxt + ( nOk > 0 ? ( "; " + okTxt ) : "" ) );
        }
    }

    /**
     * ResultHandler that retains responses in a map for later use.
     */
    private static class MapResultHandler implements ResultHandler {
        private boolean isDone_;
        final Map<Client,Response> responseMap_;

        /**
         * Constructor.
         */
        MapResultHandler() {
            responseMap_ = new ConcurrentHashMap<Client,Response>();
        }
        public void result( Client responder, Response response ) {
            responseMap_.put( responder, response );
        }

        /**
         * Called when all responses are in, or timeout has occurred.
         * Calls <code>notifyAll</code> on this object.
         */
        public synchronized void done() {
            isDone_ = true;
            notifyAll();
        }

        /**
         * Indicates whether <code>done</code> has been called.
         */
        public synchronized boolean isDone() {
            return isDone_;
        }
    }
}
