/*
 * $Id: Transmitter.java,v 1.2 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;


/** A Transmitter is an object used by a Pod to transmit data.
 * Implementations of this interface will provide a means by which a
 * Channel can be connected, and by which a Transmitter and Receiver
 * can establish contact.
 * 
 * <p> Transmitters can have string-named properties attached to them.
 * These properties are defined to have meaning only within a given
 * protocol, and can be used by clients to specify the behavior of a
 * transmitter.
 *
 * <P> There are two sets of data that can be emitted by a
 * Transmitter: data that was already in the Pod when the transmitter
 * is enabled, and data that comes into being after the transmitter is
 * enabled.  By default, transmitters only emit the latter kind of
 * data -- that is, changes in the Pod that occur after the
 * transmitter is enabled.  In some cases -- generators, in particular
 * -- there is no such thing as "existing data." In other cases, a Pod
 * contains an evolving set of data and is able to produce that data
 * into its transmitters.  These transmitters can be rewound, meaning
 * that they will proceed to emit all the data in the Pod, and
 * continue with data that changes. Usually, if a transmitter is going
 * to be rewound, it should be rewound before connecting a channel, so
 * that data is emitted in an order that will make sense to the
 * receiver.
 *
 * @version $Revision: 1.2 $
 * @author John Reekie
 * @rating Red
 */
public interface Transmitter extends PropertyContainer {

    /** Get the channel to which this transmitter is attached, or null
     * if it isn't attached to anything.
     */
    public Channel getChannel ();

    /** Get the protocol that this transmitter implements.
     */
    public Protocol getProtocol ();

    /** Get a flag indicating whether this transmitter can be rewound.
     */
    public boolean isRewindable ();

    /** Rewind the transmitter. If the transmitter cannot be rewound,
     * then an implementor can throw an exception.
     */
    public void rewind ();
}
