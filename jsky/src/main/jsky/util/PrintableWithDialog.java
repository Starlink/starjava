/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: PrintableWithDialog.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.util;

import java.awt.print.PrinterException;


/**
 * An interface for widgets that can pop up a print dialog to send their
 * contents to the printer.
 */
public abstract interface PrintableWithDialog {

    /**
     * Display a print dialog to print the contents of this object.
     */
    public void print() throws PrinterException;
}
