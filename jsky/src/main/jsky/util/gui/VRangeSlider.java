//
// RangeSlider.java
//

// Modified from the original Visad version by Allan Brighton
// $Id: VRangeSlider.java,v 1.1 2002/07/09 13:30:38 brighton Exp $

// Modified by Mark Taylor (Starlink): setDrawLabels method added.

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/


//package visad.browser;

package jsky.util.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.util.Convert;


/**
 * A slider widget that allows users to select a lower and upper bound.
 */
public class VRangeSlider extends JComponent
        implements MouseListener, MouseMotionListener {

    /** Default variable name.*/
    private static final String DEFAULT_NAME = "value";

    /** Preferred slider height. */
    private static final int SLIDER_PREF_HEIGHT = 42;

    /** Preferred slider width.*/
    private static final int SLIDER_PREF_WIDTH = 300;

    /** Width of grip. */
    private static final int GRIP_WIDTH = 9;

    /** Height of grip. */
    private static final int GRIP_HEIGHT = 17;

    /** Y-coordinate of top of grip. */
    private static final int GRIP_TOP_Y = 4;

    /** Y-coordinate of bottom of grip. */
    private static final int GRIP_BOTTOM_Y = GRIP_TOP_Y + GRIP_HEIGHT;

    /** Y-coordinate of middle of grip. */
    private static final int GRIP_MIDDLE_Y = GRIP_TOP_Y + (GRIP_HEIGHT / 2);

    /** Height of slider line. */
    private static final int SLIDER_LINE_HEIGHT = GRIP_HEIGHT + 2;

    /** Width of slider line. */
    private static final int SLIDER_LINE_WIDTH = 2;

    /** Height of font. */
    private static final int FONT_HEIGHT = 15;

    /** Y-coordinate of top of font. */
    private static final int FONT_TOP_Y = 27;

    /** Y-coordinate of bottom of font. */
    private static final int FONT_BOTTOM_Y = FONT_TOP_Y + FONT_HEIGHT - 2;

    /** Location of min gripper. */
    private int minGrip = GRIP_WIDTH;

    /** Location of max gripper. */
    private int maxGrip = SLIDER_PREF_WIDTH - GRIP_WIDTH;

    /** Percent through scale of min gripper. */
    protected double minValue = 0;

    /** Percent through scale of max gripper. */
    protected double maxValue = 100;

    /** Minimum slider value. */
    protected double minLimit = 0.0f;

    /** Maximum slider value. */
    protected double maxLimit = 1.0f;

    /** Flag whether mouse is currently affecting min gripper. */
    private boolean minSlide = false;

    /** Flag whether mouse is currently affecting max gripper. */
    private boolean maxSlide = false;

    /** Flag whether left gripper has moved. */
    private boolean lSlideMoved = false;

    /** Flag whether right gripper has moved. */
    private boolean rSlideMoved = false;

    /** Flag whether current text string value needs updating. */
    private boolean textChanged = false;

    /** Variable name for values. */
    private String name;

    /** Label state variable. */
    private double lastMinLimit = 0.0;

    /** Label state variable. */
    private double lastMaxLimit = 0.0;

    /** Label state variable. */
    private String lastCurStr = "";

    /** Minimum widget size.  */
    protected Dimension minSize = null;

    /** Preferred widget size. */
    protected Dimension prefSize = null;

    /** Maximum widget size. */
    protected Dimension maxSize = null;

    /** Used for 3D lines.*/
    private BasicStroke shadowStroke = new BasicStroke(1);

    /** Used for normal drawing. */
    private BasicStroke defaultStroke = new BasicStroke();

    /** Color used for drawing arrows. */
    private Color arrowColor = Color.yellow;

    /** Flag determining whether text labels are drawn. */
    protected boolean drawLabels = true;

    /** list of listeners for change events */
    protected EventListenerList listenerList = new EventListenerList();


    /**
     * Constructs a VRangeSlider with the specified range of values.
     */
    public VRangeSlider(String label, double min, double max) {
        name = label;
        resetValues(min, max);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /** Percent through scale of min gripper. */
    public double getMinValue() {
        return minValue;
    }

    /** Percent through scale of max gripper. */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * Gets minimum and maximum slider values.
     */
    public double[] getMinMaxValues() {
        return new double[]{minValue, maxValue};
    }

    /**
     * Resets the minimum and maximum values.
     */
    protected void resetValues(double min, double max) {
        minLimit = min;
        maxLimit = max;
        minGrip = GRIP_WIDTH;
        maxGrip = getSize().width - GRIP_WIDTH;
        minSlide = false;
        maxSlide = false;
        lSlideMoved = true;
        rSlideMoved = true;
        textChanged = true;

        int w = getSize().width;
        minValue = gripToValue(minGrip, w);
        maxValue = gripToValue(maxGrip, w);
    }

    /**
     * Sets the slider's name.
     */
    public void setName(String name) {
        this.name = name;
        textChanged = true;
        repaint();
    }

    /**
     * Sets the slider's lo and hi bounds.
     */
    public void setBounds(double min, double max) {
        resetValues(min, max);
        valuesUpdated();
        repaint();
    }

    /**
     * Sets the slider's lo and hi values.
     */
    public void setValues(double lo, double hi) {
        int w = getSize().width;
        int g;

        minValue = lo;
        g = minGrip;
        minGrip = valueToGrip(minValue, w);
        if (g != minGrip)
            lSlideMoved = true;

        maxValue = hi;
        g = maxGrip;
        maxGrip = valueToGrip(maxValue, w);
        if (g != maxGrip)
            rSlideMoved = true;

        textChanged = true;
        repaint();
    }

    /**
     * Redraws the slider if the widget width changes.
     */
    public void setBounds(int x, int y, int w, int h) {
        int lastW = getSize().width;
        super.setBounds(x, y, w, h);
        if (lastW != w) {
            minGrip = valueToGrip(minValue, w);
            maxGrip = valueToGrip(maxValue, w);
            Graphics2D g2 = (Graphics2D) getGraphics();
            drawLabels(g2, lastW);
            if (g2 != null) g2.dispose();
        }
    }

    /**
     * Returns true if (px, py) is inside (x, y, w, h)
     */
    private static boolean containedIn(int px, int py, int x, int y, int w, int h) {
        return new Rectangle(x, y, w, h).contains(px, py);
    }

    /**
     * MouseListener method for moving slider.
     */
    public void mousePressed(MouseEvent e) {
        int w = getSize().width;
        int x = e.getX();
        int y = e.getY();
        oldX = x;

        if (containedIn(x, y, minGrip - (GRIP_WIDTH - 1), GRIP_TOP_Y, GRIP_WIDTH, GRIP_HEIGHT)) {
            // mouse pressed in left grip
            minSlide = true;
        }
        else if (containedIn(x, y, maxGrip, GRIP_TOP_Y, GRIP_WIDTH, GRIP_HEIGHT)) {
            // mouse pressed in right grip
            maxSlide = true;
        }
        else if (containedIn(x, y, minGrip, GRIP_TOP_Y - 3, maxGrip - minGrip,
                GRIP_TOP_Y + SLIDER_LINE_HEIGHT - 1)) {
            // mouse pressed in rectangle
            minSlide = true;
            maxSlide = true;
        }
        else if (containedIn(x, y, 0, GRIP_TOP_Y - 3, minGrip - GRIP_WIDTH,
                GRIP_TOP_Y + SLIDER_LINE_HEIGHT - 1)) {
            // mouse pressed to left of grips
            if (x < GRIP_WIDTH)
                minGrip = GRIP_WIDTH;
            else
                minGrip = x;
            minValue = gripToValue(minGrip, w);
            minSlide = true;
            lSlideMoved = true;
            valuesUpdated();
            repaint();
        }
        else if (containedIn(x, y, maxGrip + 1 - GRIP_WIDTH, GRIP_TOP_Y - 3,
                w - maxGrip + GRIP_WIDTH, GRIP_TOP_Y + SLIDER_LINE_HEIGHT - 1)) {
            // mouse pressed to right of grips
            if (x > w - GRIP_WIDTH)
                maxGrip = w - GRIP_WIDTH;
            else
                maxGrip = x;
            maxValue = gripToValue(maxGrip, w);
            maxSlide = true;
            rSlideMoved = true;
            valuesUpdated();
            repaint();
        }
    }

    /**
     * register to receive change events from this object whenever the
     * min or max values are changed.
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
     * Notify any listeners of a change in the image or cut levels.
     */
    protected void fireChange() {
        //System.out.println("XXX min = " + minValue + ", max = " + maxValue);
        ChangeEvent e = new ChangeEvent(this);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(e);
            }
        }
    }

    /**
     * MouseListener method for moving slider.
     */
    public void mouseReleased(MouseEvent e) {
        minSlide = false;
        maxSlide = false;
        textChanged = true;
        repaint();
        fireChange();
    }

    /**
     * Not used.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Not used.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Not used.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Previous mouse X position.
     */
    private int oldX;

    /**
     * MouseMotionListener method for moving slider.
     */
    public void mouseDragged(MouseEvent e) {
        int w = getSize().width;
        int x = e.getX();
        int y = e.getY();

        // move entire range
        if (minSlide && maxSlide) {
            int change = x - oldX;
            if (minGrip + change < GRIP_WIDTH)
                change = GRIP_WIDTH - minGrip;
            else if (maxGrip + change > w - GRIP_WIDTH) {
                change = w - GRIP_WIDTH - maxGrip;
            }
            if (change != 0) {
                minGrip += change;
                minValue = gripToValue(minGrip, w);
                maxGrip += change;
                maxValue = gripToValue(maxGrip, w);
                lSlideMoved = true;
                rSlideMoved = true;
                valuesUpdated();
                repaint();
            }
        }

        // move min gripper if it is held
        else if (minSlide) {
            if (x < GRIP_WIDTH)
                minGrip = GRIP_WIDTH;
            else if (x >= maxGrip)
                minGrip = maxGrip - 1;
            else
                minGrip = x;
            minValue = gripToValue(minGrip, w);
            lSlideMoved = true;
            valuesUpdated();
            repaint();
        }

        // move max gripper if it is held
        else if (maxSlide) {
            if (x > w - GRIP_WIDTH)
                maxGrip = w - GRIP_WIDTH;
            else if (x <= minGrip)
                maxGrip = minGrip + 1;
            else
                maxGrip = x;
            maxValue = gripToValue(maxGrip, w);
            rSlideMoved = true;
            valuesUpdated();
            repaint();
        }

        oldX = x;
    }

    /**
     * Not used.
     */
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Returns minimum size of range slider.
     */
    public Dimension getMinimumSize() {
        if (minSize == null) {
            minSize = new Dimension(0, SLIDER_PREF_HEIGHT);
        }
        return minSize;
    }

    /**
     * Sets minimum size of range slider.
     */
    public void setMinimumSize(Dimension dim) {
        minSize = dim;
    }

    /**
     * Returns preferred size of range slider.
     */
    public Dimension getPreferredSize() {
        if (prefSize == null) {
            prefSize = new Dimension(SLIDER_PREF_WIDTH, SLIDER_PREF_HEIGHT);
        }
        return prefSize;
    }

    /**
     * Sets preferred size of range slider.
     */
    public void setPreferredSize(Dimension dim) {
        prefSize = dim;
    }

    /**
     * Returns maximum size of range slider.
     */
    public Dimension getMaximumSize() {
        if (maxSize == null) {
            maxSize = new Dimension(Integer.MAX_VALUE, SLIDER_PREF_HEIGHT);
        }
        return maxSize;
    }

    /**
     * Sets preferred size of range slider.
     */
    public void setMaximumSize(Dimension dim) {
        maxSize = dim;
    }

    private double gripToValue(int pos, int width) {
        return (((maxLimit - minLimit) * ((double) (pos - GRIP_WIDTH))) /
                (double) (width - (GRIP_WIDTH * 2))) + minLimit;
    }

    private int valueToGrip(double value, int width) {
        double rdouble = (((value - (double) minLimit) *
                (double) (width - (GRIP_WIDTH * 2))) / (maxLimit - minLimit));

        // round away from zero
        if (rdouble < 0.0f)
            rdouble -= 0.5f;
        else
            rdouble += 0.5f;

        return (int) rdouble + GRIP_WIDTH;
    }

    /**
     * Called whenever the min or max value is updated.
     * This method is meant to be overridden by extension classes.
     */
    public void valuesUpdated() {
    }

    /**
     * Draws the slider from scratch.
     */
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        int w = getSize().width;
        Color bg = getBackground();
        Color fg = getForeground();

        // clear old graphics
        g2.setColor(bg);
        g2.fillRect(0, 0, w, SLIDER_PREF_HEIGHT);

        // draw slider lines
        int right = w - 1;

        Line2D.Double[] lines = new Line2D.Double[7];
        lines[0] = new Line2D.Double(0, GRIP_MIDDLE_Y, right, GRIP_MIDDLE_Y);

        lines[1] = new Line2D.Double(0, GRIP_TOP_Y - 4, 0, GRIP_TOP_Y + SLIDER_LINE_HEIGHT);

        lines[2] = new Line2D.Double(0, GRIP_TOP_Y - 4, SLIDER_LINE_WIDTH, GRIP_TOP_Y - 4);

        lines[3] = new Line2D.Double(0, GRIP_TOP_Y + SLIDER_LINE_HEIGHT,
                SLIDER_LINE_WIDTH, GRIP_TOP_Y + SLIDER_LINE_HEIGHT);

        lines[4] = new Line2D.Double(right, GRIP_TOP_Y - 4, right, GRIP_TOP_Y + SLIDER_LINE_HEIGHT);

        lines[5] = new Line2D.Double(right, GRIP_TOP_Y - 4, right - SLIDER_LINE_WIDTH, GRIP_TOP_Y - 4);
        lines[6] = new Line2D.Double(right, GRIP_TOP_Y + SLIDER_LINE_HEIGHT,
                right - SLIDER_LINE_WIDTH, GRIP_TOP_Y + SLIDER_LINE_HEIGHT);

        // draw the lines
        g2.setColor(fg);
        for (int i = 0; i < lines.length; i++)
            g2.draw(lines[i]);

        // draw the shadows
        g2.setStroke(shadowStroke);
        g2.setColor(Color.white);
        for (int i = 0; i < lines.length; i++) {
            lines[i].y1++;
            lines[i].y2++;
            g2.draw(lines[i]);
        }

        // refresh everything
        lSlideMoved = true;
        rSlideMoved = true;
        textChanged = true;

        if (lSlideMoved) {
            g2.setColor(bg);
            g2.fillRect(SLIDER_LINE_WIDTH, GRIP_TOP_Y, maxGrip - 3, GRIP_HEIGHT);

            g2.setColor(fg);
            g2.drawLine(SLIDER_LINE_WIDTH, GRIP_MIDDLE_Y, maxGrip - 3, GRIP_MIDDLE_Y);

            g2.setStroke(shadowStroke);
            g2.setColor(Color.white);
            g2.drawLine(SLIDER_LINE_WIDTH, GRIP_MIDDLE_Y + 1, maxGrip - 3, GRIP_MIDDLE_Y + 1);
            g2.setStroke(defaultStroke);

            g2.setColor(arrowColor);
            int[] xpts = {minGrip - GRIP_WIDTH, minGrip + 1, minGrip + 1};
            int[] ypts = {GRIP_MIDDLE_Y, GRIP_TOP_Y, GRIP_BOTTOM_Y};
            g2.fillPolygon(xpts, ypts, 3);
        }
        if (rSlideMoved) {
            g2.setColor(bg);
            g2.fillRect(minGrip + 1, GRIP_TOP_Y, w - minGrip - 3, GRIP_HEIGHT);

            g2.setColor(fg);
            g2.drawLine(minGrip + 1, GRIP_MIDDLE_Y, w - 3, GRIP_MIDDLE_Y);

            g2.setStroke(shadowStroke);
            g2.setColor(Color.white);
            g2.drawLine(minGrip + 1, GRIP_MIDDLE_Y + 1, w - 3, GRIP_MIDDLE_Y + 1);
            g2.setStroke(defaultStroke);

            g2.setColor(arrowColor);
            int[] xpts = new int[]{maxGrip + GRIP_WIDTH - 1, maxGrip, maxGrip};
            int[] ypts = {GRIP_MIDDLE_Y, GRIP_TOP_Y, GRIP_BOTTOM_Y};
            g2.fillPolygon(xpts, ypts, 3);
        }
        if (lSlideMoved || rSlideMoved) {
            g2.setColor(Color.gray);
            g2.draw3DRect(minGrip + 1, GRIP_MIDDLE_Y - 7, maxGrip - minGrip - 1, 15, true);
            g2.fill3DRect(minGrip + 1, GRIP_MIDDLE_Y - 7, maxGrip - minGrip - 1, 15, true);
        }
        if (textChanged) drawLabels(g2, w);
        lSlideMoved = false;
        rSlideMoved = false;
        textChanged = false;
    }

    /**
     * Determines whether textual annotations should be drawn near this
     * widget.
     * 
     * @param  drawLabels  whether to draw textual annotation
     */
    public void setDrawLabels( boolean drawLabels ) {
        this.drawLabels = drawLabels;
    }

    /**
     * Indicates whether textual annotations will be drawn near this
     * widget.
     *
     * @return  whether textual annotation will be drawn
     */
    public boolean getDrawLabels() {
        return drawLabels;
    }

    /**
     * Updates the labels at the bottom of the widget.
     */
    private void drawLabels(Graphics2D g2, int lastW) {
        // Return without action if no label drawing is required.
        if (!drawLabels) {
            return;
        }
        int w = getSize().width;
        FontMetrics fm = g2.getFontMetrics();
        if (lastMinLimit != minLimit || lastW != w) {
            // minimum bound text string
            g2.setColor(getBackground());
            int sw = fm.stringWidth("" + lastMinLimit);
            g2.fillRect(1, FONT_TOP_Y, sw, FONT_HEIGHT);
            lastMinLimit = minLimit;
        }
        if (lastMaxLimit != maxLimit || lastW != w) {
            // maximum bound text string
            g2.setColor(getBackground());
            int sw = fm.stringWidth("" + lastMaxLimit);
            g2.fillRect(lastW - 4 - sw, FONT_TOP_Y, sw, FONT_HEIGHT);
            lastMaxLimit = maxLimit;
        }
        String minS = Convert.shortString(minValue);
        String maxS = Convert.shortString(maxValue);
        String curStr = name + " = (" + minS + ", " + maxS + ")";
        if (!curStr.equals(lastCurStr) || lastW != w) {
            g2.setColor(getBackground());
            int sw = fm.stringWidth(lastCurStr);
            g2.fillRect((lastW - sw) / 2, FONT_TOP_Y, sw, FONT_HEIGHT);
            lastCurStr = curStr;
        }
        g2.setColor(getForeground());
        g2.drawString(Convert.shortString(minLimit), 1, FONT_BOTTOM_Y);
        String maxStr = Convert.shortString(maxLimit);
        g2.drawString(maxStr, w - 4 - fm.stringWidth(maxStr), FONT_BOTTOM_Y);
        g2.drawString(curStr, (w - fm.stringWidth(curStr)) / 2, FONT_BOTTOM_Y);
    }

    /**
     * Main method for testing purposes.
     */
    public static void main(String[] argv) {
        JFrame frame = new JFrame("Test");
        VRangeSlider rs = new VRangeSlider("", 0.0, 100.0);
        // dynamically set the values
        rs.setBounds(-200.0, 4500.0);
        rs.setValues(22.2222, 1000.5432);
        frame.getContentPane().add(rs, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());

    }
}
