/*
 * $Id: Receiver.java,v 1.2 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;

/** A Receiver is an object used by a Pod to receive data
 * from some data source -- such as a data generator, or another
 * Pod. Implementations of this interface will provide a means
 * by which a Channel can be connected, and by which a Transmitter
 * and Receiver can establish contact.
 * <p>
 * Receivers can have string-named properties attached to them.
 * These properties are defined to have meaning only within
 * a given protocol, and can be used by clients to control
 * receivers.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface Receiver extends PropertyContainer {

    /** Get the protocol that this receiver implements.
     */
    public Protocol getProtocol ();

    /** Get the channel to which this receiver is attached, or null
     * if it isn't attached to anything.
     */
    public Channel getChannel ();
}
