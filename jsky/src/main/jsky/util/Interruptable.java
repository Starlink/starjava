/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: Interruptable.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.util;

/**
 * An interface for objects that can be interrupted (to stop whatever they
 * are doing).
 */
public abstract interface Interruptable {

    /**
     * Interrupt the current background thread.
     */
    public void interrupt();
}
