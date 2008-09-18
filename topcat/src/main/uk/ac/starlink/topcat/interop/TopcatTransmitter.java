package uk.ac.starlink.topcat.interop;

import java.net.URI;
import javax.swing.Action;
import javax.swing.JMenu;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * Extends PlasticTransmitter for TOPCAT use, 
 * just decorating its actions and menus with suitable icons.
 *
 * @author   Mark Taylor
 * @since    23 Aug 2006
 */
public abstract class TopcatTransmitter extends PlasticTransmitter {

    /**
     * Constructor.
     *
     * @param   hubman  hub manager which keeps track of registered apps
     * @param   messageId  the message ID this transmitter will transmit;
     *          only registered applications which claim to suppor this
     *          message are considered
     * @param   sendType  short string representing the type of object which
     *          is transmitted; this is used only for setting up text names
     *          for actions etc
     */
    public TopcatTransmitter( HubManager hubman, URI messageId,
                              String sendType ) {
        super( hubman, messageId, sendType );
    }

    public Action getBroadcastAction() {
        Action act = super.getBroadcastAction();
        act.putValue( Action.SMALL_ICON, ResourceIcon.BROADCAST );
        return act;
    }

    public JMenu createSendMenu() {
        JMenu menu = super.createSendMenu();
        menu.setIcon( ResourceIcon.SEND );
        return menu;
    }
}
