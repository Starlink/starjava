/*
 * ESO Archive
 *
 * $Id: LabelEntrySlider.java,v 1.6 2002/07/09 13:30:38 brighton Exp $
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
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * Combines a text label, an entry widget, and a slider widget
 * in a single panel
 */
public class LabelEntrySlider extends JPanel {

    // widgets
    private JLabel label;
    private JTextField field;
    private BasicArrowButton left, right;
    private JSlider slider;

    // lower amd upper bounds
    private double minValue;
    private double maxValue;

    // number of ticks
    private int numValues;

    // used to convert between integer slider units and double cut levels
    private double scale;

    // list of listeners for change events
    private EventListenerList listenerList = new EventListenerList();

    /** event sent to listeners when cut levels change */
    private ChangeEvent changeEvent;

    /**
     * Constructor.
     * @param text the text of the label
     * @param minVal The initial lower limit.
     * @param maxVal The initial upper limit.
     * @param numVals The number of distinct values.
     */
    public LabelEntrySlider(String text, double minVal, double maxVal, int numVals) {
        changeEvent = new ChangeEvent(this);

        setLayout(new GridBagLayout());
        GridBagUtil layoutUtil = new GridBagUtil(this, (GridBagLayout) getLayout());

        // label
        label = new JLabel(text);

        // entry
        field = new NumberEntry(8);
        field.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                // System.out.println("LabelEntrySlider: field action: " + field.getText());
                setValue(Double.parseDouble(field.getText()));
            }
        });

        // left arrow button
        left = new BasicArrowButton(SwingConstants.WEST);
        left.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int i = slider.getValue() - 1;
                setValue(minValue + i * scale);
            }
        });

        // slider
        slider = new JSlider();
        slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                setValue(minValue + slider.getValue() * scale);
            }
        });
        slider.addMouseListener(new MouseInputAdapter() {

            public void mouseReleased(MouseEvent me) {
                setValue(minValue + slider.getValue() * scale);
            }
        });

        // right arrow button
        right = new BasicArrowButton(SwingConstants.EAST);
        right.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int i = slider.getValue() + 1;
                setValue(minValue + i * scale);
            }
        });

        final int
                east = GridBagConstraints.EAST,
                west = GridBagConstraints.WEST,
                none = GridBagConstraints.NONE,
                horizontal = GridBagConstraints.HORIZONTAL;

        //             component  x  y  w  h  weightx weighty fill        anchor
        //             ---------  -  -  -  -  ------- ------- ----        ------
        layoutUtil.add(label, 0, 0, 1, 1, 0.0, 0.0, none, west);
        layoutUtil.add(field, 1, 0, 1, 1, 0.0, 0.0, none, west);
        layoutUtil.add(left, 2, 0, 1, 1, 0.0, 0.0, none, west);
        layoutUtil.add(slider, 3, 0, 1, 1, 0.0, 1.0, horizontal, west);
        layoutUtil.add(right, 4, 0, 1, 1, 0.0, 0.0, none, west);

        setBounds(minVal, maxVal, numVals);
    }


    /**
     * Default Constructor.
     */
    public LabelEntrySlider() {
        this("label", 0., 100., 100);
    }


    /**
     * Set the value displayed
     */
    public void setValue(double value) {
        boolean changed = false;

        if (value < minValue) {
            minValue = value;
            setBounds(minValue, maxValue, numValues);
        }
        else if (value >= maxValue) {
            maxValue = value;
            setBounds(minValue, maxValue, numValues);
        }

        int val = (int) Math.round((value - minValue) / scale);
        if (val != slider.getValue()) {
            changed = true;
            slider.setValue(val);
        }

        String s = String.valueOf((float) value);
        if (!s.equals(field.getText())) {
            changed = true;
            field.setText(s);
        }

        if (changed || !slider.getValueIsAdjusting())
            fireChange();
    }


    /**
     * Return the current value.
     */
    public double getValue() {
        return Double.parseDouble(field.getText());
    }


    /**
     * Return the valueIsAdjusting property of the slider
     */
    public boolean getValueIsAdjusting() {
        return slider.getValueIsAdjusting();
    }


    /**
     * Set the valueIsAdjusting property of the slider
     */
    public void setValueIsAdjusting(boolean b) {
        slider.setValueIsAdjusting(b);
    }


    /**
     * Set the lower and upper bounds and the number of ticks.
     */
    public void setBounds(double minVal, double maxVal, int numVals) {
        minValue = minVal;
        maxValue = maxVal;
        numValues = numVals;

        scale = (maxValue - minValue) / numValues;

        if (slider.getMinimum() != 0)
            slider.setMinimum(0);

        if (slider.getMaximum() != numVals - 1)
            slider.setMaximum(numVals - 1);
    }


    /** Return the label */
    public JLabel getLabel() {
        return label;
    }

    /** Return the entry */
    public JTextField getTextField() {
        return field;
    }

    /** Return the left arrow button */
    public BasicArrowButton getLeftButton() {
        return left;
    }

    /** Return the right arrow button */
    public BasicArrowButton getRightButton() {
        return right;
    }

    /** Return the slider */
    public JSlider getSlider() {
        return slider;
    }


    /**
     * register to receive change events from this object.
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notify any listeners of a change.
     */
    protected void fireChange() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * test main: usage: java LabelEntrySlider
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        LabelEntrySlider les = new LabelEntrySlider("Test", 0, 100, 100);
        frame.getContentPane().add(les, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

