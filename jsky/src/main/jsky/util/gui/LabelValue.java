/*
 * ESO Archive
 *
 * $Id: LabelValue.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.util.gui;

import java.awt.*;
import javax.swing.*;


/**
 * This widget displays a label and a string value.
 */
public class LabelValue extends LabelEntry {

    /**
     * Constructor.
     * @param labelText The text to display in the label.
     * @param valueText The value string to display next to the label.
     * @param labelAtLeft True if the label should be displayed at the left, otherwise
     * the label will be at the top.
     */
    public LabelValue(String labelText, String valueText, boolean labelAtLeft) {
        super(labelText, valueText, labelAtLeft);
        value.setEnabled(false);
        value.setDisabledTextColor(Color.black);
        value.setBackground(getBackground());
    }

    public LabelValue(String labelText, String valueText) {
        this(labelText, valueText, true);
    }

    public LabelValue(String labelText) {
        this(labelText, "", true);
    }

    /**
     * test main: usage: java LabelValue
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        LabelValue lv = new LabelValue("test", "failed");
        lv.setText("passed");
        frame.getContentPane().add(lv, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

