//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class ListenerHandler
//
//--- Description -------------------------------------------------------------
//    A session object containing a proposal and state information about that
//    proposal.  The session provides capabilities for loading, saving, and reverting
//    a proposal.  The ProposalSession also has a Class level capability to manage
//    the different sessions.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	10/23/01 S Grosvenor
//
//		Original implementation.
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

package jsky.util;

import java.util.Arrays;
import java.util.Iterator;


/**
 * encapsulation of thread proof method of managing a set of listeners.
 */
public abstract class ListenerHandler {

    /**
     * to be defined by subclasses, responsible for firing an event to an
     * instance of a listener
     */
    abstract public void fireEvent(String eventkey, Object listener, Object event);

    /**
     * array of listeners to this object
     */
    Object[] fListeners;

    public ListenerHandler() {
        fListeners = null;
    }

    /**
     * add a listener
     *
     * @param listener  The listener to be added
     */
    public void addListener(Object listener) {
        synchronized (this) {
            Object[] els = null;
            if (fListeners != null) {
                for (int i = 0; i < fListeners.length; i++) {
                    if (fListeners[i] == listener) return; // already have it, don't add it
                }
                int length = fListeners.length;
                els = new Object[length + 1];
                System.arraycopy(fListeners, 0, els, 0, length);
            }
            else {
                els = new Object[1];
            }
            els[els.length - 1] = listener;
            fListeners = els;
        }
    }

    /**
     * Remove a listener
     *
     * @param listener  The listener to be removed
     */
    public void removeListener(Object listener) {
        if (fListeners == null) return;
        synchronized (this) {
            int length = fListeners.length;
            for (int i = 0; i < fListeners.length; i++) if (fListeners[i] == listener) length--;
            if (length == 0) {
                fListeners = null;
                return;
            }
            Object[] els = new Object[length];
            int nexti = 0;
            for (int i = 0; i < fListeners.length; i++) {
                if (fListeners[i] != listener) {
                    els[nexti++] = fListeners[i];
                }
            }
            fListeners = els;
        }
    }

    /**
     * sends the event to all listeners.  Calls the abstract class fireEvent( listener, event) for each item
     * in the listener list
     */
    public void sendEvent(Object evt) {
        sendEvent(null, evt);
    }

    /**
     * sends the event to all listeners.  Calls the abstract class fireEvent( listener, event) for each item
     * in the listener list
     */
    public void sendEvent(String eventKey, Object evt) {
        Object[] localListeners = fListeners;
        if (localListeners == null) return;

        for (int i = 0; i < localListeners.length; i++) {
            Object target = localListeners[i];

            fireEvent(eventKey, target, evt);  // call to abstract which knows more specifics about classes of listener and event
        }
    }

    /**
     * returns an iterator to each listener in the list
     */
    public Iterator listeners() {
        return Arrays.asList(fListeners).iterator();
    }

    public boolean contains(Object o) {
        if (fListeners == null || fListeners.length == 0) return false;

        Object[] localListeners = fListeners;
        for (int i = 0; i < localListeners.length; i++) {
            if (localListeners[i].equals(o)) return true;
        }
        return false;
    }

    public int size() {
        if (fListeners == null) return 0;
        return fListeners.length;
    }
}