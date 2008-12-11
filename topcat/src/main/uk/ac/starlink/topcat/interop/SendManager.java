package uk.ac.starlink.topcat.interop;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.ResponseHandler;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.SubscribedClientListModel;

/**
 * Provides a ComboBoxModel allowing selection of SAMP clients subscribed
 * to a given MType.  An option corresponding to broadcast to all clients
 * is also provided.  This class is useful for implementing {@link Activity}
 * subclasses using SAMP.
 *
 * @author   Mark Taylor
 * @since    17 Sep 2008
 */
public class SendManager {

    private final GuiHubConnector connector_;
    private final SendComboBoxModel comboBoxModel_;
    private final String mtype_;
    private String tag_;

    private static final Object BROADCAST = "All Clients";
    private static final Logger logger_ =
        Logger.getLogger( SendManager.class.getName() );

    /**
     * Constructor.
     *
     * @param   connector  hub connector
     * @param   mtype  MType to which all selectable clients must be subscribed
     */
    public SendManager( GuiHubConnector connector, String mtype ) {
        connector_ = connector;
        mtype_ = mtype;
        comboBoxModel_ =
            new SendComboBoxModel( new SubscribedClientListModel( connector,
                                                                  mtype ) );
    }

    /**
     * Returns a combo box model which allows to select any of the subscribed
     * clients, or broadcast to all.
     *
     * @return   combo box model
     */
    public ComboBoxModel getComboBoxModel() {
        return comboBoxModel_;
    }

    /**
     * Sends a given message by notification to the currently selected target
     * client or clients.  This message will presumably have the MType 
     * supplied to this object in the constructor.
     *
     * @param  message   {@link org.astrogrid.samp.Message}-like map
     */
    public void notify( Map message ) throws SampException {
        HubConnection connection = connector_.getConnection();
        if ( connection != null ) {
            Client client = comboBoxModel_.getClient();
            if ( client == null ) {
                connection.notifyAll( message );
            }
            else {
                connection.notify( client.getId(), message );
            }
        }
    }

    /**
     * Sends a given message by call/response to the currently selected target
     * client or clients.  This message will presumably have the MType 
     * supplied to this object in the constructor.
     * The response is logged, but nothing else is done with it.
     *
     * @param  message   {@link org.astrogrid.samp.Message}-like map
     */
    public void call( Map message ) throws SampException {
        HubConnection connection = connector_.getConnection();
        if ( connection != null ) {
            Client client = comboBoxModel_.getClient();
            if ( client == null ) {
                connection.callAll( getTag(), message );
            }
            else {
                connection.call( client.getId(), getTag(), message );
            }
        }
    }

    /**
     * Returns the message tag which is used for asynchronous call/response
     * sends by this object.
     * 
     * @return   tag
     */
    private synchronized String getTag() {
        if ( tag_ == null ) {
            final String tag = connector_.createTag( this );
            ResponseHandler handler = new ResponseHandler() {
                public boolean ownsTag( String msgTag ) {
                    return tag.equals( msgTag );
                }
                public void receiveResponse( HubConnection connection,
                                             String responderId, String msgTag,
                                             Response response ) {
                    if ( response.isOK() ) {
                        logger_.info( "Success response for " + mtype_
                                    + " from " + responderId );
                    }
                    else {
                        logger_.warning( "Error response for " + mtype_
                                       + " from " + responderId + ": "
                                       + response.getStatus() );
                    }
                }
            };
            connector_.addResponseHandler( handler );
            tag_ = tag;
        }
        return tag_;
    }

    /**
     * ComboBoxModel implementation used by this class.
     * It essentially mirrors an existing client model but prepends a 
     * broadcast option.
     */
    private static class SendComboBoxModel extends AbstractListModel
                                           implements ComboBoxModel {
        private final ListModel clientListModel_;
        private Object selectedItem_ = BROADCAST;

        /**
         * Constructor.
         *
         * @param  clientListModel  list model containing suitable 
         *                          {@link org.astrogrid.samp.Client}s
         */
        SendComboBoxModel( ListModel clientListModel ) {
            clientListModel_ = clientListModel;

            /* Watch the underlying client model for changes and 
             * update this one accordingly. */
            clientListModel_.addListDataListener( new ListDataListener() {
                public void contentsChanged( ListDataEvent evt ) {
                    fireContentsChanged( evt.getSource(),
                                         adjustIndex( evt.getIndex0() ),
                                         adjustIndex( evt.getIndex1() ) );
                }
                public void intervalAdded( ListDataEvent evt ) {
                    fireIntervalAdded( evt.getSource(),
                                       adjustIndex( evt.getIndex0() ),
                                       adjustIndex( evt.getIndex1() ) );
                }
                public void intervalRemoved( ListDataEvent evt ) {
                    fireIntervalRemoved( evt.getSource(),
                                         adjustIndex( evt.getIndex0() ),
                                         adjustIndex( evt.getIndex1() ) );
                }
                private int adjustIndex( int index ) {
                    return index >= 0 ? index + 1
                                      : index;
                }
            } );
        }

        public int getSize() {
            return clientListModel_.getSize() + 1;
        }

        public Object getElementAt( int index ) {
            return index == 0 ? BROADCAST
                              : clientListModel_.getElementAt( index - 1 );
        }

        public Object getSelectedItem() {
            return selectedItem_;
        }

        public void setSelectedItem( Object item ) {
            selectedItem_ = item;
        }

        /**
         * Returns the currently selected client if there is one.
         * If the currently selected item is not a client (this can be assumed
         * to mean that the broadcast item is selected), then null is returned.
         *
         * @return  currently selected client, or null
         */
        public Client getClient() {
            return selectedItem_ instanceof Client ? (Client) selectedItem_
                                                   : null;
        }
    }
}
