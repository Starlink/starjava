/*
 * ESO Archive
 *
 * $Id: NumberEntry.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.util.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * This is a JTextField which only allows numeric content.
 */
public class NumberEntry extends JTextField {

    public NumberEntry(int cols) {
        super(cols);
    }

    protected Document createDefaultModel() {
        return new NumericDocument();
    }

    static class NumericDocument extends PlainDocument {

        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {

            if (str == null) {
                return;
            }

            if (offs != 0 && str.equals(".")) {
                super.insertString(offs, str, a);
                return;
            }

            try {
                double value = Double.parseDouble(str);
                super.insertString(offs, str, a);
            }
            catch (Exception e) {
                // something went wrong (most likely we don't have a valid Double)
            }
        }
    }
}

