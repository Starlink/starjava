/*
 * ESO Archive
 *
 * $Id: LabelEntry.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */


package jsky.util.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * This widget combines a JLabel with a JTextField to form a labeled entry
 * widget.
 */
public class LabelEntry extends LabelWidget {

    protected JTextField value;

    /**
     * Constructor.
     * @param labelText The text to display in the label.
     * @param valueText The string to display in the text field.
     * @param labelAtLeft True if the label should be displayed at the left, otherwise
     * the label will be at the top.
     */
    public LabelEntry(String labelText, String valueText, boolean labelAtLeft) {
        super(labelText);

        value = new JTextField(valueText);

        if (labelAtLeft) {
            //             comp   x  y  w  h  wtx  wty  fill        anchor
            layoutUtil.add(value, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL,
                    GridBagConstraints.WEST);
        }
        else {
            //             comp   x  y  w  h  wtx  wty  fill        anchor
            layoutUtil.add(value, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL,
                    GridBagConstraints.WEST);
        }
    }

    public LabelEntry(String labelText, String valueText) {
        this(labelText, valueText, true);
    }

    public LabelEntry(String labelText) {
        this(labelText, "", true);
    }

    public void setLabelText(String labelText) {
        label.setText(labelText);
    }

    public String getLabelText() {
        return label.getText();
    }

    public void setText(String valueText) {
        value.setText(valueText);
    }

    public String getText() {
        return value.getText();
    }

    public JLabel getLabel() {
        return label;
    }

    public JTextField getValue() {
        return value;
    }

    public void addActionListener(ActionListener l) {
        value.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        value.removeActionListener(l);
    }


    /**
     * test main: usage: java LabelEntry
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        LabelEntry l1 = new LabelEntry("test1", "passed 1");
        LabelEntry l2 = new LabelEntry("test2", "passed 2", false);
        frame.getContentPane().add(l1, BorderLayout.NORTH);
        frame.getContentPane().add(l2, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

