// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IApplyCancel.java,v 1.1 2002/08/04 21:48:51 brighton Exp $

package jsky.util;

/**
 * An interface for dialogs that can be applied or canceled.
 */
public abstract interface IApplyCancel {
    public void apply();
    public void cancel();
}

