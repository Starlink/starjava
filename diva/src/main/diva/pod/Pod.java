/*
 * $Id: Pod.java,v 1.3 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;

import java.util.HashMap;


/** A Pod is a "pile of data." This is any complex evolving data
 * structure. Pods communicate through channels, between objects
 * associated with the Pods called transmitters and receivers.
 * Communication is structured according to a protocol, which
 * is like a coarse-grained typing of the data.
 *
 * <P>
 * Pods can have string-named properties attached to them.
 *
 * @version $Revision: 1.3 $
 * @author John Reekie
 * @rating Red
 */
public interface Pod extends PropertyContainer {

    /** Get a transmitter of the given protocol from the Pod.  Null if
     * the protocol is not supported by the pod, or if the Pod is unable
     * to create more transmitters (some Pods, for example, may be able
     * to create only a single transmitter). The properties hash
     * map, if supplied, can be used in a protocol-specific way to
     * place constraints on the selection of a transmitter.
     */
    public Transmitter getTransmitter (Protocol p, HashMap properties);

    /** Get a receiver of the given protocol from the Pod.  Null if
     * the protocol is not supported by the pod. The properties hash
     * map, if supplied, can be used in a protocol-specific way to
     * place constraints on the selection of a receiver.
     */
    public Receiver getReceiver (Protocol p, HashMap properties);
}
