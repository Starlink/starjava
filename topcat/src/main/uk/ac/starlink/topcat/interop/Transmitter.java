package uk.ac.starlink.topcat.interop;

import javax.swing.Action;
import javax.swing.JMenu;

/**
 * Interface for an action which notionally sends some information from
 * this application to one or more other applications.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2008
 */
public interface Transmitter {

    /**
     * Returns an action which sends the information to all appropriate
     * recipients.
     *
     * @return  broadcast action
     */
    Action getBroadcastAction();

    /**
     * Returns a per-application menu which allows sending the information
     * to any single one of the appropriate recipients.
     *
     * @return   send menu
     */
    JMenu createSendMenu();

    /**
     * Sets whether the send actions controlled by this transmitter should
     * be enabled or not.  This is an AND-like restriction - the actions
     * may still be disabled for other reasons (e.g. no hub connection).
     *
     * @param  isEnabled  true iff actions may be invoked
     */
    void setEnabled( boolean isEnabled );
}
