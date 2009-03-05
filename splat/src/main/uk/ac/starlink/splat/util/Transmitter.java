/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     04-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import javax.swing.Action;
import javax.swing.JMenu;

/**
 * Interface defining an object which sends fixed messages over a messaging
 * system.  Provides useful GUI points of access to the functionality.
 *
 * @see SampCommunicator
 * @author Mark Taylor
 * @version $Id$
 */
public interface Transmitter
{

    /**
     * Returns an action which sends the information to all appropriate
     * recipients.
     */
    Action getBroadcastAction();

    /**
     * Returns a menu with one item per recipient application,
     * for sending the message to any single one of the appropriate
     * recipients.
     */
    JMenu createSendMenu();

    /**
     * Sets whether the send actions controlled by this transmitter should
     * be enabled or not.  This is an AND-like restriction - the actions
     * may still be disabled for other reasons (e.g. no hub connection).
     */
    void setEnabled( boolean isEnabled );
}
