/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: LabelJPanel.java,v 1.4 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import jsky.util.*;

/**
 * This widget combines a JLabel with a JPanel.
 *
 * @version $Revision: 1.4 $ $Date: 2002/07/09 13:30:38 $
 * @author Allan Brighton
 */
public class LabelJPanel extends LabelWidget {

    protected JPanel panel;

    /**
     * Constructor.
     * @param labelText the text to display in the label.
     * @param panel the panel to display.
     * @param labelAtLeft true if the label should be displayed at the left, otherwise
     * the label will be at the top.
     * @param labelAnchor the anchor for the the label (GridBagConstraints constant: default: NORTHEAST)
     */
    public LabelJPanel(String labelText, JPanel panel, boolean labelAtLeft, int labelAnchor) {
        super(labelText, labelAnchor);
        this.panel = panel;

        if (labelAtLeft) {
            //             comp   x  y  w  h  wtx  wty  fill                           anchor
            layoutUtil.add(panel, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        }
        else {
            //             comp   x  y  w  h  wtx  wty  fill                           anchor
            layoutUtil.add(panel, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        }
    }


    /**
     * Constructor.
     * @param labelText the text to display in the label.
     * @param panel the panel to display.
     * @param labelAtLeft true if the label should be displayed at the left, otherwise
     * the label will be at the top.
     */
    public LabelJPanel(String labelText, JPanel panel, boolean labelAtLeft) {
        this(labelText, panel, labelAtLeft, GridBagConstraints.NORTHEAST);
    }


    /**
     * Constructor.
     * @param labelText the text to display in the label.
     * @param panel the panel to display.
     */
    public LabelJPanel(String labelText, JPanel panel) {
        this(labelText, panel, true);
    }


    /**
     * Constructor.
     * @param labelText the text to display in the label.
     */
    public void setLabelText(String labelText) {
        label.setText(labelText);
    }

    /** Return the text of the label */
    public String getLabelText() {
        return label.getText();
    }

    /** Return the internal label object. */
    public JLabel getLabel() {
        return label;
    }

    /** Return the panel object. */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * test main: usage: java LabelEntry
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        String[] names = {"one", "two", "three", "four",
                          "five", "six", "seven", "eight",
                          "eight", "nine", "ten"};
        ToggleButtonPanel panel = new ToggleButtonPanel(names, 0, 4, false, 1, 1);
        LabelJPanel labelPanel = new LabelJPanel("Numbers", panel, false);
        frame.getContentPane().add(labelPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

