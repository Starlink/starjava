/**
 * 
 */
package uk.ac.starlink.splat.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.astrogrid.samp.Client;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.ResultHandler;
import org.astrogrid.samp.gui.ErrorDialog;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.UniformCallActionManager;



/**
 * @author David Andresic
 *
 */
public abstract class SplatUniformCallActionManager extends UniformCallActionManager {

	private final Component parent_;
	private final String mtype_;
	private final String sendType_;
	
	public SplatUniformCallActionManager(Component parent,
			GuiHubConnector connector, String mtype, String sendType) {
		super(parent, connector, mtype, sendType);
		
		this.parent_ = parent;
		this.mtype_ = mtype;
		this.sendType_ = sendType;
	}
	
	public final Component getParent() {
		return parent_;
	}
	
	public final String getMtype() {
		return mtype_;
	}
	
	public final String getSendType() {
		return sendType_;
	}

	@Override
	public Action getSendAction(Client client) {
		Action action = new SendAction( client );
		action.putValue( Action.SHORT_DESCRIPTION,
                "Transmit to " + client + " using SAMP " + getMtype() );
		return action;
	}
	
	@Override
	protected Action createBroadcastAction() {
        return new BroadcastAction();
    }
	
	@Override
	protected final Map createMessage() throws Exception {
		return null;
	}
	
	protected abstract List<Message> createMessages() throws Exception;
	
	/** 
     * Action which performs a send to a particular client.
     */
    private class SendAction extends AbstractAction {
        private final Client client_;
        private final String cName_;

        /**
         * Constructor.
         *
         * @param  client  target client
         */
        SendAction( Client client ) {
            client_ = client;
            cName_ = client.toString();
            putValue( NAME, cName_ );
            putValue( SHORT_DESCRIPTION,
                      "Transmit to " + cName_ + " using SAMP protocol" );
        }

        public void actionPerformed( ActionEvent evt ) {
        	List<Message> messages = null;
        	
        	try {
				messages = createMessages();
			} catch (Exception e) {
				ErrorDialog.showError( getParent(), "Send Error",
                        "Send failure " + e.getMessage(), e );
			}
        	
        	if (messages != null) {
        		for (Map msg : messages) {
        			processMessage(msg);
        		}
        	}
        	
        }
        
        private void processMessage(Map msgMap) {
        	boolean sent = false;
            Message msg = null;
            HubConnection connection = null;
            String tag = null;

            // Attempt to send the messsage.
            try {
                msg = Message.asMessage( msgMap );
                msg.check();
                connection = getConnector().getConnection();
                if ( connection != null ) {
                    tag = createTag();
                    connection.call( client_.getId(), tag, msg );
                    sent = true;
                }
            }
            catch ( Exception e ) {
                ErrorDialog.showError( getParent(), "Send Error",
                                       "Send failure " + e.getMessage(), e );
            }

            // If it was sent, arrange for the result to be processed by 
            // a suitable result handler.
            if ( sent ) {
                assert connection != null;
                assert msg != null;
                assert tag != null;
                Client[] recipients = new Client[] { client_ };
                ResultHandler handler =
                    createResultHandler( connection, msg, recipients );
                registerHandler( tag, recipients, handler );
            }
        }

        public boolean equals( Object o ) {
            if ( o instanceof SendAction ) {
                SendAction other = (SendAction) o;
                return this.client_.equals( other.client_ )
                    && this.cName_.equals( other.cName_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            return client_.hashCode() * 23 + cName_.hashCode();
        }
    }
    
    /**
     * Action for sending broadcast messages.
     */
    private class BroadcastAction extends AbstractAction {

        /**
         * Constructor.
         */
        BroadcastAction() {
            putValue( NAME, "Broadcast " + getSendType() );
            putValue( SHORT_DESCRIPTION,
                      "Transmit " + getSendType() + " to all applications"
                    + " listening using the SAMP protocol" );
            putValue( SMALL_ICON, getBroadcastIcon() );
        }

        public void actionPerformed( ActionEvent evt ) {
            HubConnector connector = getConnector();
            Set recipientIdSet = null;
            Message msg = null;
            HubConnection connection = null;
            String tag = null;

            // Attempt to send the message.
            try {
                msg = Message.asMessage( createMessage() );
                msg.check();
                connection = connector.getConnection();
                if ( connection != null ) {
                    tag = createTag();
                    recipientIdSet = connection.callAll( tag, msg ).keySet();
                }
            }
            catch ( Exception e ) {
                ErrorDialog.showError( parent_, "Send Error",
                                       "Send failure " + e.getMessage(), e );
            }

            // If it was sent, arrange for the results to be passed to
            // a suitable result handler.
            if ( recipientIdSet != null ) {
                assert connection != null;
                assert msg != null;
                assert tag != null;
                List recipientList = new ArrayList();
                Map clientMap = connector.getClientMap();
                for ( Iterator it = recipientIdSet.iterator(); it.hasNext(); ) {
                    String id = (String) it.next();
                    Client recipient = (Client) clientMap.get( id );
                    if ( recipient != null ) {
                        recipientList.add( recipient );
                    }
                }
                Client[] recipients =
                    (Client[]) recipientList.toArray( new Client[ 0 ] );
                ResultHandler handler =
                    createResultHandler( connection, msg, recipients );
                if ( recipients.length == 0 ) {
                    if ( handler != null ) {
                        handler.done();
                    }
                    handler = null;
                }
                registerHandler( tag, recipients, handler );
            }
        }
    }
}
