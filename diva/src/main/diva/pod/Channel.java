/*
 * $Id: Channel.java,v 1.2 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;


/** A Channel is the object that transmits data between a Transmitter
 * and a Receiver.  Implementations of this interface will provide, at
 * a minimum, a means by which the transmitter of the same protocol
 * can pass data into the channel, and a means to pass data to the
 * receiver. In general, a channel can perform other functions -- for
 * example, filtering, transforming, or aggregating data.
 *
 * <p>
 * Channels can have string-named properties attached to them.  These
 * properties are defined to have meaning only within a given
 * protocol, and can be used by clients to specify the behavior of a
 * channel.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface Channel extends PropertyContainer {

    /** Get the protocol that this channel implements.
     */
    public Protocol getProtocol ();

    /** Connect this channel between a transmitter and receiver.
     */
    public void connect (Transmitter t, Receiver r);

    /** Disconnect this channel from its transmitter and receiver.
     * The channel is allowed to assume that both the transmitter
     * and receiver still exist and can be reconnected to if
     * needed.
     */
    public void disconnect ();

    /** Get the Receiver that this channel is attached to, or
     * null if it is not attached.
     */
    public Receiver getReceiver ();

    /** Get the Transmitter that this channel is attached to, or
     * null if it is not attached.
     */
    public Transmitter getTransmitter ();
}
