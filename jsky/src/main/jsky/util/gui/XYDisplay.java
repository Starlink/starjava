/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: XYDisplay.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.*;
import java.net.*;
import java.beans.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.border.*;

import jsky.util.*;

/**
 * This widget displays a pair of coordinates with given labels.
 *
 * @version $Revision: 1.3 $ $Date: 2002/07/09 13:30:38 $
 * @author Allan Brighton
 */
public class XYDisplay extends JPanel {

    protected JLabel xLabel;
    protected JLabel yLabel;

    protected JTextField xValue;
    protected JTextField yValue;

    /**
     * Construct an X,Y display with the given labels.
     *
     * @param xLabelStr the label for the X value
     * @param xLabelStr the label for the Y value
     */
    public XYDisplay(String xLabelStr, String yLabelStr) {
        setLayout(new GridBagLayout());
        GridBagUtil layoutUtil = new GridBagUtil(this, (GridBagLayout) getLayout());

        xLabel = new JLabel(xLabelStr);
        yLabel = new JLabel(yLabelStr);

        xValue = new JTextField();
        yValue = new JTextField();

        setupLabel(xLabel);
        setupLabel(yLabel);

        setupTextField(xValue);
        setupTextField(yValue);

        //             comp    x  y  w  h  wtx  wty  fill                           anchor
        layoutUtil.add(xLabel, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        layoutUtil.add(xValue, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        layoutUtil.add(yLabel, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
        layoutUtil.add(yValue, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    }


    /** Set properties for displaying a label */
    protected void setupLabel(JLabel l) {
        l.setForeground(Color.black);
        l.setFont(l.getFont().deriveFont(Font.PLAIN));
    }


    /** Set properties for displaying a value text field */
    protected void setupTextField(JTextField t) {
        t.setEnabled(false);
        t.setDisabledTextColor(Color.black);
        t.setBackground(getBackground());
        t.setBorder(new BevelBorder(BevelBorder.LOWERED));
    }


    /** Set the value width in pixels */
    public void setValueWidth(int w) {
        Dimension d = xValue.getPreferredSize();
        d.width = w;
        xValue.setPreferredSize(d);
        yValue.setPreferredSize(d);
    }


    /**
     * Construct an X,Y display with the default labels "X" and "Y".
     */
    public XYDisplay() {
        this("X", "Y");
    }


    /** Set the value to display for X */
    public void setX(String s) {
        xValue.setText(s);
    }


    /** Set the value to display for X */
    public void setX(double d) {
        xValue.setText(String.valueOf(d));
    }


    /** Set the value to display for Y */
    public void setY(String s) {
        yValue.setText(s);
    }


    /** Set the value to display for Y */
    public void setY(double d) {
        yValue.setText(String.valueOf(d));
    }


    /** Set the X,Y values to display. If p is null, display an empty string. */
    public void set(Point2D.Double p) {
        if (p != null) {
            xValue.setText(String.valueOf(p.getX()));
            yValue.setText(String.valueOf(p.getY()));
        }
        else {
            xValue.setText("");
            yValue.setText("");
        }
    }

    /** Return the widget displaying X label  */
    public JLabel getXLabel() {
        return xLabel;
    }

    /** Return the widget displaying Y label  */
    public JLabel getYLabel() {
        return yLabel;
    }

    /** Return the widget displaying X value  */
    public JTextField getXValue() {
        return xValue;
    }

    /** Return the widget displaying Y value  */
    public JTextField getYValue() {
        return yValue;
    }

    /**
     * test main: usage: java LabelEntry
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        XYDisplay panel = new XYDisplay();
        panel.setX("99.676");
        panel.setY("22.2");
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

