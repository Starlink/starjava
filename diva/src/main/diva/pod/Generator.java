/*
 * $Id: Generator.java,v 1.1 2002/02/07 02:58:07 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.pod;

import diva.util.PropertyContainer;

import java.util.HashMap;


/** A Generator is an object that generates data. Generators might
 * generate data incrementally, such a web crawler, or a running
 * simulation. Or, they might simply represent an existing store
 * of data and generate that data into a protocol.
 *
 * <P> It is possible for objects to implement both the Generator
 * and Pod interfaces. This would be used for Pods that have the
 * generator-like property of producing data.
 *
 * <P> Generators can have string-named properties attached to them.
 *
 * @version $Revision: 1.1 $
 * @author John Reekie
 * @rating Red
 */
public interface Generator extends PropertyContainer {

    /** Get the flag that says whether the generator is enabled.
     */
    public boolean getEnabled ();

    /** Get a transmitter of the given protocol from the Generator.  Null if
     * the protocol is not supported by the pod, or if the Pod is unable
     * to create more transmitters (some Pods, for example, may be able
     * to create only a single transmitter). The properties hash
     * map, if supplied, can be used in a protocol-specific way to
     * place constraints on the selection of a transmitter.
     */
    public Transmitter getTransmitter (Protocol p, HashMap properties);

    /** Enable or disable the generator. By default, generators should
     * start off disabled, so that clients can make the necessary connections
     * before the generator starts producing data.
     */
    public void setEnabled (boolean enabled);
}
