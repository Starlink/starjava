package uk.ac.starlink.topcat.interop;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenu;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * Transmitter implementation which is permanently disabled.
 * The transmit actions cannot therefore be invoked by the user;
 * if they are invoked programatically, they will do nothing.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2010
 */
public class DisabledTransmitter implements Transmitter {

    private final String sendType_;
    private final Action broadcastAct_;

    /**
     * Constructor.
     *
     * @param  sendType  short string describing type of item
     *         (notionally) being sent
     */
    public DisabledTransmitter( String sendType ) {
        sendType_ = sendType;
        broadcastAct_ = new BasicAction( "Broadcast " + sendType,
                                         ResourceIcon.BROADCAST,
                                         "Transmit " + sendType +
                                         " to all applications" ) {
            { super.setEnabled( false ); }
            public void setEnabled( boolean enabled ) {
            }
            public void actionPerformed( ActionEvent evt ) {
            }
        };
    }

    public Action getBroadcastAction() {
        return broadcastAct_;
    }

    public JMenu createSendMenu() {
        return new JMenu( "Send " + sendType_ + " to..." ) {
            { super.setEnabled( false ); }
            public void setEnabled( boolean enabled ) {
            }
        };
    }

    public void setEnabled( boolean enabled ) {
    }
}
