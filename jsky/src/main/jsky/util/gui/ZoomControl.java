/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ZoomControl.java,v 1.7 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import jsky.util.Resources;


/**
 * Implements a simple widget to control zooming in and out.
 * This class consists of a combo box with mag settings and optional zoom in/out
 * buttons.
 *
 * @version $Revision: 1.7 $ $Date: 2002/07/09 13:30:38 $
 * @author Allan Brighton
 */
public class ZoomControl extends JPanel {

    /** Used to display a menu of magnification values */
    protected JComboBox comboBox;

    /** Button used to zoom in */
    protected JButton zoomIn;

    /** Button used to zoom out */
    protected JButton zoomOut;

    /** labels for menu */
    protected String[] labels;


    /**
     * Construct a widget to control the zooming in and out.
     *
     * @param labels an array of labels for the combo box (should represent numeric values)
     * @param withButtons if true, display zoomIn/zoomOut buttons
     */
    public ZoomControl(String[] labels, boolean withButtons) {
        this.labels = labels;
        comboBox = new JComboBox(labels);
        comboBox.setEditable(true);
        comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String newSelection = (String) cb.getSelectedItem();
                try {
                    float f = Float.parseFloat(newSelection);
                }
                catch (NumberFormatException ex) {
                    DialogUtil.error("Please enter the numeric zoom factor.");
                    return;
                }
                fireAction(new ActionEvent(this, 0, newSelection));
            }
        });

        // Use a smaller size than the default.
        // hack: there is no easy way to get the text size before the component
        // is actually displayed and if we wait until then, the interface will
        // visibly be resized at startup...
        setTextWidth(55);

        JPanel panel = new JPanel();
        add(comboBox);

        if (withButtons) {
            zoomIn = new JButton(Resources.getIcon("ZoomIn24.gif"));
            zoomIn.setBorder(new BevelBorder(BevelBorder.RAISED));
            zoomIn.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    zoom(true);
                }
            });
            add(zoomIn);

            zoomOut = new JButton(Resources.getIcon("ZoomOut24.gif"));
            zoomOut.setBorder(new BevelBorder(BevelBorder.RAISED));
            zoomOut.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    zoom(false);
                }
            });
            add(zoomOut);
        }

        setSelectedItem("1");
    }


    /**
     * Construct a widget to control zooming in and out.
     *
     * @param labels an array of labels for the combo box (should represent numeric values)
     */
    public ZoomControl(String[] labels) {
        this(labels, true);
    }


    /** Set the width of the widget */
    public void setTextWidth(int width) {
        Dimension d = comboBox.getPreferredSize();
        d.width = width;
        comboBox.setPreferredSize(d);
    }


    /** Set the selected value */
    public void setSelectedItem(String value) {
        comboBox.getModel().setSelectedItem(value);
    }


    /** Set the selected value */
    public void setSelectedItem(float zoomFactor) {
        String s;
        if (zoomFactor < 1) {
            if (zoomFactor < 0.1)
                return;		// set lower limit for display purposes

            NumberFormat fmt = NumberFormat.getInstance(Locale.US);
            fmt.setMaximumFractionDigits(1);
            s = String.valueOf(fmt.format(zoomFactor));
        }
        else {
            s = String.valueOf((int) zoomFactor);
        }
        comboBox.getModel().setSelectedItem(s);
    }


    /**
     * Zoom in (if in is true), otherwise out by one step.
     *
     * @param in true if zooming in, otherwise zoom out
     */
    protected void zoom(boolean in) {
        String s = (String) comboBox.getSelectedItem();
        float zoomFactor = Float.valueOf(s).floatValue();

        if (in) {
            if (zoomFactor < 1.0)
                zoomFactor += 0.1;
            else
                zoomFactor += 1;
        }
        else {
            if (zoomFactor <= 1.0)
                zoomFactor -= 0.1;
            else
                zoomFactor -= 1;
        }

        setSelectedItem(zoomFactor);
    }


    /** Return the combo box used to display a menu of magnification values */
    public JComboBox getComboBox() {
        return comboBox;
    }


    /** Return the button used to zoom in. */
    public JButton getZoomInButton() {
        return zoomIn;
    }


    /** Return the button used to zoom out. */
    public JButton getZoomOutButton() {
        return zoomOut;
    }


    /**
     * Register to receive action events from this object whenever the
     * zoom factor is changed.
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }


    /**
     * Stop receiving action events from this object.
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notify any listeners of a change in the zoom factor.
     */
    protected void fireAction(ActionEvent actionEvent) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(actionEvent);
            }
        }
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        String[] labels = {"0.125", "0.25", "0.5", "1", "2", "4", "8", "16"};
        ZoomControl zoomControl = new ZoomControl(labels);
        zoomControl.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                float f = Float.parseFloat(e.getActionCommand()); // guaranteed to be a float value
                System.out.println("Set zoom factor to: " + f);
            }
        });

        frame.getContentPane().add(zoomControl, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

