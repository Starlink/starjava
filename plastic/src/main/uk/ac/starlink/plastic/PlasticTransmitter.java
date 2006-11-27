package uk.ac.starlink.plastic;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ListModel;
import javax.swing.JMenu;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Provides actions for transmitting messages of a particular kind using
 * PLASTIC.  The purpose of this class is to provide actions and menus
 * which allow the user to send a message to either one or multiple
 * appropriate listeners.  These actions/menus are kept up to date 
 * with respect to the current set of registered listener applications.
 * Only applications which claim to support a particular message are
 * considerered.  If there are no such applications the send actions
 * are disabled.
 *
 * @author   Mark Taylor
 * @since    12 Apr 2006
 */
public abstract class PlasticTransmitter {

    private final HubManager hubman_;
    private final URI messageId_;
    private final String sendType_;
    private final ListModel appList_;
    private final Action broadcastAct_;
    private List sendActList_;
    private boolean enabled_;
    private List menuList_;

    /**
     * Constructs a new transmitter.
     *
     * @param   hubman  hub manager which keeps track of registered apps
     * @param   messageId  the message ID this transmitter will transmit;
     *          only registered applications which claim to suppor this
     *          message are considered
     * @param   sendType  short string representing the type of object which
     *          is transmitted; this is used only for setting up text names
     *          for actions etc
     */
    public PlasticTransmitter( HubManager hubman, URI messageId,
                               String sendType ) {
        hubman_ = hubman;   
        messageId_ = messageId;
        sendType_ = sendType;
        appList_ = hubman.getApplicationListModel();

        /* Ensure that any changes to the list of registered applications
         * is reflected in the state of this transmitter. */
        appList_.addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                updateState();
            }
            public void intervalAdded( ListDataEvent evt ) {
                updateState();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                updateState();
            }
        } );

        /* Construct broadcast action. */
        broadcastAct_ = new TransmitAction( null );
        broadcastAct_.putValue( Action.NAME, "Broadcast " + sendType_ );
        broadcastAct_.putValue( Action.SHORT_DESCRIPTION,
                                "Transmit " + sendType + " to all applications "
                              + "listening with the PLASTIC protocol" );

        /* Initialise other state. */
        enabled_ = true;
        menuList_ = new ArrayList();
        updateState();
    }

    /**
     * This performs the actual work of transmission to one or all registered
     * applications.
     *
     * @param   hub  hub object
     * @param   clientId  identifier under which this application is 
     *                    registered with <code>hub</code>
     * @param   app  application to which the transmit should be done.
     *               If null, broadcast to all suitable listeners.
     */
    protected abstract void transmit( PlasticHubListener hub, URI clientId,
                                      ApplicationItem app )
            throws IOException;

    /**
     * Sets the enabled status of this transmitter.  This acts as a restriction
     * (AND) on the enabled status of the menus and actions controlled by
     * this transmitter.  If there are no suitable recipient applications
     * registered they will be disabled anyway.
     *
     * @param  enabled   false to ensure that the actions are disabled,
     *          true means they may be enabled
     */
    public void setEnabled( boolean enabled ) {
        enabled_ = enabled;
        updateEnabledness();
    }

    /**
     * Returns an action which will broadcast a message 
     * to all suitable registered applications.
     *
     * <p>This action is currently not disabled when there are no suitable
     * listeners, mainly for debugging purposes (so you can see if a 
     * message is getting sent and what it looks like even in absence of 
     * suitable listeners).
     *
     * @return  broadcast action
     */
    public Action getBroadcastAction() {
        return broadcastAct_;
    }

    /**
     * Returns a new menu which provides options to send a message to
     * one of the registered listeners at a time.  This menu will be
     * disabled when no suitable listeners are registered.
     *
     * @return   new message send menu
     */
    public JMenu createSendMenu() {
        JMenu menu = new JMenu( "Send " + sendType_ + " to ..." );
        for ( Iterator it = sendActList_.iterator(); it.hasNext(); ) {
            menu.add( (Action) it.next() );
        }
        menu.setToolTipText( "Transmit " + sendType_ + " to a single " +
                             "application using PLASTIC" );
        menuList_.add( menu );
        updateEnabledness();
        return menu;
    }

    /**
     * Updates this transmitter's state when the list of registered listeners
     * has changed.
     */
    private void updateState() {

        /* Assemble a list of actions which represent sends to all the
         * suitable registered listeners. */
        URI self = hubman_.getRegisteredId();
        List actList = new ArrayList();
        for ( int i = 0; i < appList_.getSize(); i++ ) {
            ApplicationItem app = (ApplicationItem) appList_.getElementAt( i );
            if ( ! app.getId().equals( self ) ) {
                List msgs = app.getSupportedMessages();
                if ( msgs == null || msgs.isEmpty() ||
                     msgs.contains( messageId_ ) ) {
                    actList.add( new TransmitAction( app ) );
                }
            }
        }

        /* If it differs from last time, update state. */
        if ( ! actList.equals( sendActList_ ) ) {
            sendActList_ = actList;
                    
            /* Clear and repopulate each menu we have dispensed. */
            for ( Iterator menuIt = menuList_.iterator(); menuIt.hasNext(); ) {
                JMenu menu = (JMenu) menuIt.next();
                menu.removeAll();
                for ( Iterator actIt = sendActList_.iterator();
                      actIt.hasNext(); ) {
                    menu.add( (Action) actIt.next() );
                }
            }

            /* Enabled status may have changed. */
            updateEnabledness();
        }
    }

    /**
     * Updates the enabled status of controlled actions in accordance with
     * this object's current state.
     */
    private void updateEnabledness() {
        boolean active = enabled_ && sendActList_.size() > 0;
        broadcastAct_.setEnabled( enabled_ );
        for ( Iterator it = menuList_.iterator(); it.hasNext(); ) {
            ((JMenu) it.next()).setEnabled( active );
        }
    }

    /**
     * Action which sends a PLASTIC message to a given application.
     * Instances are considered equal if they reference the same 
     * application.
     */
    private class TransmitAction extends AbstractAction {
        final ApplicationItem app_;

        /**
         * Constructor.
         *
         * @param   app  target application; null for broadcast to all
         */
        TransmitAction( ApplicationItem app ) {
            super( app == null ? "Broadcast" : ( "Send to " + app ) );
            app_ = app;
        }

        public void actionPerformed( ActionEvent evt ) {
            try {
                hubman_.register();
                PlasticHubListener hub = hubman_.getHub();
                URI clientId = hubman_.getRegisteredId();
                if ( hub != null && clientId != null ) {
                    transmit( hub, clientId, app_ );
                }
            }
            catch ( IOException e ) {
                Object src = evt.getSource();
                Component parent = src instanceof Component
                                 ? (Component) src
                                 : null;
                ErrorDialog.showError( parent, "PLASTIC Send Error", e );
            }
        }

        public boolean equals( Object o ) {
            if ( o instanceof TransmitAction ) {
                TransmitAction other = (TransmitAction) o;
                return ( this.app_ == null && other.app_ == null )
                    || ( this.app_ != null && this.app_.equals( other.app_ ) );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            return app_ == null ? 0 : app_.getId().hashCode();
        }
    }
}
