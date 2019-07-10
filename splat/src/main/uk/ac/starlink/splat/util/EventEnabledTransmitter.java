/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     04-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JMenu;

/**
 * Interface defining an object which sends fixed messages over a messaging
 * system.  Provides useful GUI points of access to the functionality.
 *
 * @see SampCommunicator
 * @author Mark Taylor
 * @author David Andresic
 * @version $Id$
 */
public interface EventEnabledTransmitter extends Transmitter, MouseListener
{
	// just marking interface
}
