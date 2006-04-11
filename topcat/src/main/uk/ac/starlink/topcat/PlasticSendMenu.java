package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Menu which contains items for sending a PLASTIC message to individual
 * PLASTIC listeners.
 *
 * @author   Mark Taylor
 * @since    11 Apr 2006
 */
public abstract class PlasticSendMenu extends JMenu
                                      implements ListDataListener {

    private final HubManager hubman_;
    private final ListModel appList_;
    private final URI message_;

    /**
     * Constructor.
     *
     * @param   title  menu title
     * @param   icon   menu icon
     * @param   shortdesc  short description (tooltip)
     * @param   hubman   hub manager
     * @param   message  message id for the message which this menu will send
     */
    public PlasticSendMenu( String title, Icon icon, String shortdesc,
                            HubManager hubman, URI message ) {
        super( title );
        setIcon( icon );
        setToolTipText( shortdesc );
        hubman_ = hubman;
        message_ = message;
        appList_ = hubman.getApplicationListModel();
        appList_.addListDataListener( this );
        updateMenuItems();
    }

    /**
     * Callback which actually sends the message.
     * The application which is to be messaged is given.
     *
     * @param  app   application to message
     */
    protected abstract void send( ApplicationItem app ) throws IOException;

    /*
     * ListDataListener implmentation.
     */

    public void contentsChanged( ListDataEvent evt ) {
        updateMenuItems();
    }

    public void intervalAdded( ListDataEvent evt ) {
        updateMenuItems();
    }

    public void intervalRemoved( ListDataEvent evt ) {
        updateMenuItems();
    }

    /**
     * Updates the state of this menu to reflect the current state of 
     * its base ListModel.
     */
    private void updateMenuItems() {
        removeAll();
        URI self = hubman_.getRegisteredId();
        for ( int i = 0; i < appList_.getSize(); i++ ) {
            ApplicationItem app = (ApplicationItem) appList_.getElementAt( i );
            if ( ! self.equals( app.getId() ) ) {
                List msgs = app.getSupportedMessages();
                if ( msgs == null || msgs.isEmpty() ||
                     msgs.contains( message_ ) ) {
                    add( new SendAction( app ) );
                }
            }
        }
    }

    /**
     * Action which is invoked when the menu items are selected.
     */
    private class SendAction extends AbstractAction {
        final ApplicationItem app_;

        /**
         * Constructs a new action which will cause the given application
         * to be messaged.
         */
        SendAction( final ApplicationItem app ) {
            super( "Send to " + app );
            app_ = app;
        }

        public void actionPerformed( ActionEvent evt ) {
            try {
                hubman_.register();
                send( app_ );
            }
            catch ( IOException e ) {
                ErrorDialog.showError( PlasticSendMenu.this,
                                       "PLASTIC Error", e );
            }
        }
    }
}
