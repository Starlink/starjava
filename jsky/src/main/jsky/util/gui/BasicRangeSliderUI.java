//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    5/11/01      S. Grosvenor / 588 Booz-Allen
//      Initial implementation.
//    5/23/01       S. Grosvenor / 588 Booz-Allen
//      Made thumbs partially transparent to not hide the scale values
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

package jsky.util.gui;

import javax.swing.plaf.basic.*;
import javax.swing.plaf.ComponentUI;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SwingUtilities;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.AlphaComposite;

import javax.swing.event.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * The core repaints for our RangeSlider
 */

public class BasicRangeSliderUI extends BasicSliderUI
        implements RangeSliderUI {

    public static ComponentUI createUI(JComponent c) {
        return new BasicRangeSliderUI((JSlider) c, true);
    }

    public BasicRangeSliderUI(JSlider b, boolean showe) {
        super(b);
        showExtent = showe;
    }

    private static final int LABEL_OFFSET = 0;
    private static final int TICK_OFFSET = 10;

    protected Rectangle extRect = null;

    protected transient boolean isDraggingThumb = false;
    protected transient boolean isDraggingExtent = false;
    protected transient boolean showExtent = true;

    protected void calculateLabelRect() {
        labelRect = new Rectangle(trackRect);
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            labelRect.height += LABEL_OFFSET;
            labelRect.y += LABEL_OFFSET;
        }
        else {
            // not tested yet
            labelRect.width += LABEL_OFFSET;
            labelRect.x += LABEL_OFFSET;
        }
    }

    protected void calculateTickRect() {
        tickRect = new Rectangle(trackRect);
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            tickRect.height += TICK_OFFSET;
            tickRect.y += TICK_OFFSET;
        }
        else {
            // not tested yet
            tickRect.width += TICK_OFFSET;
            tickRect.x += TICK_OFFSET;
        }
    }

    public Dimension getPreferredSize(JComponent c) {
        recalculateIfInsetsChanged();
        Dimension d;
        if (slider.getOrientation() == JSlider.VERTICAL) {
            d = new Dimension(getPreferredVerticalSize());
            d.width = insetCache.left + insetCache.right;
            d.width += focusInsets.left + focusInsets.right;
            d.width += Math.max(trackRect.width, Math.max(tickRect.width, labelRect.width));
        }
        else {
            d = new Dimension(getPreferredHorizontalSize());
            d.height = insetCache.top + insetCache.bottom;
            d.height += focusInsets.top + focusInsets.bottom;
            d.height += Math.max(trackRect.height, Math.max(tickRect.height, labelRect.height));
        }

        return d;
    }

    public void installUI(JComponent c) {
        isDraggingExtent = false;
        isDraggingThumb = false;
        extRect = new Rectangle();

        super.installUI(c);

        if (insetCache == null) insetCache = new java.awt.Insets(0, 0, 0, 0);
    }

    public void uninstallUI(JComponent c) {
        extRect = null;

        super.uninstallUI(c);
    }

    protected void calculateGeometry() {
        super.calculateGeometry();
        calculateExtentSize();
        calculateExtentLocation();
    }

    protected void calculateExtentSize() {
        if (showExtent) {
            Dimension size = getExtentSize();
            extRect.setSize(size.width, size.height);
        }
        else {
            extRect.setSize(0, 0);
        }
    }

    protected Dimension getExtentSize() {
        return getThumbSize();
    }

    protected int getRangeRight() {
        return slider.getValue() + slider.getExtent();
    }

    protected void setRangeMax(int value) {
        int newExtent = value - slider.getValue();
        if (newExtent > 0) slider.setExtent(newExtent);
    }

    protected int getRangeLeft() {
        return slider.getValue();
    }

    protected void setRangeMin(int value) {
        if (showExtent) {
            int newExtent = slider.getValue() + slider.getExtent() - value;
            if (newExtent >= 0) {
                slider.setValue(value);
                slider.setExtent(newExtent);
            }
            else {
                // max out at right edge of range
                slider.setExtent(0);
                slider.setValue(slider.getValue() + slider.getExtent());
            }
        }
        else {
            slider.setValue(value);
        }
    }

    protected void calculateExtentLocation() {
        if (slider.getSnapToTicks()) {
            int extValue = getRangeRight();

            int snappedValue = extValue;
            int majorTickSpacing = slider.getMajorTickSpacing();
            int minorTickSpacing = slider.getMinorTickSpacing();
            int tickSpacing = 0;

            if (minorTickSpacing > 0) {
                tickSpacing = minorTickSpacing;
            }
            else if (majorTickSpacing > 0) {
                tickSpacing = majorTickSpacing;
            }

            if (tickSpacing != 0) {
                // If it's not on a tick, change the value
                if ((extValue - slider.getMinimum()) % tickSpacing != 0) {
                    float temp = (float) (extValue - slider.getMinimum()) / (float) tickSpacing;
                    int whichTick = Math.round(temp);
                    snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
                }

                if (snappedValue != extValue) {
                    setRangeMax(snappedValue);
                }
            }
        }

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int valuePosition = xPositionForValue(getRangeRight());

            extRect.x = valuePosition - (extRect.width / 2);
            extRect.y = trackRect.y;
        }
        else {
            int valuePosition = yPositionForValue(getRangeRight());

            extRect.x = trackRect.x;
            extRect.y = valuePosition - (extRect.height / 2);
        }
    }

    protected void calculateTrackBuffer() {
        super.calculateTrackBuffer();

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            trackBuffer = Math.max(trackBuffer, extRect.width / 2);
        }
        else {
            trackBuffer = Math.max(trackBuffer, extRect.height / 2);
        }
    }


    protected void calculateTrackRect() {
        super.calculateTrackRect();
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            trackRect.height = Math.max(trackRect.height, extRect.height);
        }
        else {
            trackRect.width = Math.max(trackRect.width, extRect.width);
        }
    }

    protected PropertyChangeListener createPropertyChangeListener(final JSlider slider) {
        return new BasicSliderUI.PropertyChangeHandler() {

            // code from BasicSliderUI.PropertyChangeHandler
            public void propertyChange(PropertyChangeEvent e) {

                String propertyName = e.getPropertyName();

                if (propertyName.equals("model")) {
                    ((BoundedRangeModel) e.getOldValue()).removeChangeListener(changeListener);
                    ((BoundedRangeModel) e.getNewValue()).addChangeListener(changeListener);

                    calculateThumbLocation();
                    calculateExtentLocation();

                    slider.repaint();
                }
                else {
                    super.propertyChange(e);
                }
            }
        };
    }

    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        Rectangle clip = g.getClipBounds();

        if (clip.intersects(extRect)) {
            paintExtent(g);
        }
    }

    public void paintThumb(Graphics g) {
        paintThumb(g, thumbRect, 0); //(showExtent ? Math.PI/2 : 0));
    }

    public void paintThumbLocal(Graphics g) {
        super.paintThumb(g);
    }

    public void paintExtent(Graphics g) {
        if (showExtent) paintThumb(g, extRect, 0); //-Math.PI/2);
    }

    /**
     * still relies on "parent" paintThumb
     */
    public void paintThumb(Graphics g, Rectangle knobBounds, double theta) {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;

        g2.translate(knobBounds.x, knobBounds.y);
        g2.rotate(theta, knobBounds.width / 2, knobBounds.height / 2);
        g2.translate(-knobBounds.x, -knobBounds.y);

        Rectangle holdRect = thumbRect;

        thumbRect = knobBounds;

        java.awt.Composite holdComposite = g2.getComposite();

        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
        g2.setComposite(ac);

        paintThumbLocal(g2);

        g2.setComposite(holdComposite);
        thumbRect = holdRect;

        g2.translate(knobBounds.x, knobBounds.y);
        g2.rotate(-theta, knobBounds.width / 2, knobBounds.height / 2);
        g2.translate(-knobBounds.x, -knobBounds.y);
    }


    // Used exclusively by setExtentLocation()
    private static Rectangle unionExtRect = new Rectangle();

    public void setExtentLocation(int x, int y) {
        unionExtRect.setBounds(extRect);
        extRect.setLocation(x, y);

        SwingUtilities.computeUnion(extRect.x, extRect.y, extRect.width, extRect.height, unionExtRect);
        slider.repaint(unionExtRect.x, unionExtRect.y, unionExtRect.width, unionExtRect.height);
    }


    /**
     * Data model listener.
     *
     * This inner class is marked &quot;public&quot; due to a compiler bug.
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <Foo>.
     */
    protected ChangeListener createChangeListener(final JSlider slider) {
        return new RangeSliderChangeListener();
    }

    public class RangeSliderChangeListener implements ChangeListener {

        // code from BasicSliderUI.ChangeHandler
        public void stateChanged(ChangeEvent e) {
            if (!isDraggingThumb) {
                calculateThumbLocation();
                slider.repaint();
            }
            if (!isDraggingExtent) {
                calculateExtentLocation();
                slider.repaint();
            }
        }
    }

    /**
     * Track mouse movements.
     *
     * This inner class is marked &quot;public&quot; due to a compiler bug.
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <Foo>.
     */
    protected TrackListener createTrackListener(JSlider slider) {
        return new RangeSliderTrackListener();
    }

    public boolean thumbContains(int x, int y) {
        return thumbRect.contains(x, y);
    }

    public boolean extentContains(int x, int y) {
        return extRect.contains(x, y);
    }

    public class RangeSliderTrackListener extends BasicSliderUI.TrackListener {

        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            isDraggingThumb = false;
            isDraggingExtent = false;
        }

        /**
         * If the mouse is pressed above the "thumb" component
         * then reduce the scrollbars value by one page ("page up"),
         * otherwise increase it by one page.  If there is no
         * thumb then page up if the mouse is in the upper half
         * of the track.
         */
        public void mousePressed(MouseEvent e) {
            if (!slider.isEnabled())
                return;

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            slider.requestFocus();

            if (thumbRect.contains(currentMouseX, currentMouseY)) {
                // click on main thumb - left slider
                switch (slider.getOrientation()) {
                case JSlider.VERTICAL:
                    offset = currentMouseY - thumbRect.y;
                    break;

                case JSlider.HORIZONTAL:
                    offset = currentMouseX - thumbRect.x;
                    break;
                }
                isDraggingThumb = true;
                slider.setValueIsAdjusting(true);
            }
            else if (extRect.contains(currentMouseX, currentMouseY)) {
                // click on right slider
                switch (slider.getOrientation()) {
                case JSlider.VERTICAL:
                    offset = currentMouseY - extRect.y;
                    break;

                case JSlider.HORIZONTAL:
                    offset = currentMouseX - extRect.x;
                    break;
                }
                isDraggingExtent = true;
                slider.setValueIsAdjusting(true);
            }
            else {
                // did not click on either slider
                isDraggingThumb = false;
                isDraggingExtent = false;


                slider.setValueIsAdjusting(true);

                Dimension sbSize = slider.getSize();
                int direction = POSITIVE_SCROLL;

                /*
                * for the moment don't move on clicks/drags outside of Thumb or Extent Slider at all
                */
//                switch ( slider.getOrientation() )
//                {
//                    case JSlider.VERTICAL:
//                        if ( thumbRect.isEmpty() )
//                        {
//                            int scrollbarCenter = sbSize.height / 2;
//                            if ( !drawInverted() )
//                            {
//                                direction = (currentMouseY < scrollbarCenter) ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
//                            }
//                            else
//                            {
//                                direction = (currentMouseY < scrollbarCenter) ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
//                            }
//                        }
//                        else
//                        {
//                            int thumbY = thumbRect.y;
//                            if ( !drawInverted() )
//                            {
//                                direction = (currentMouseY < thumbY) ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
//                            }
//                            else
//                            {
//                                direction = (currentMouseY < thumbY) ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
//                            }
//                        }
//                        break;
//
//                    case JSlider.HORIZONTAL:
//                        if ( thumbRect.isEmpty() )
//                        {
//                            int scrollbarCenter = sbSize.width / 2;
//                            if ( !drawInverted() )
//                            {
//                                direction = (currentMouseX < scrollbarCenter) ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
//                            }
//                            else
//                            {
//                                direction = (currentMouseX < scrollbarCenter) ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
//                            }
//                        }
//                        else
//                        {
//                            int thumbX = thumbRect.x;
//                            if ( !drawInverted() )
//                            {
//                                direction = (currentMouseX < thumbX) ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
//                            }
//                            else
//                            {
//                                direction = (currentMouseX < thumbX) ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
//                            }
//                        }
//                        break;
//                }
//                scrollDueToClickInTrack(direction);
//                Rectangle r = thumbRect;
//                if ( !r.contains(currentMouseX, currentMouseY) )
//                {
//                    if ( shouldScroll( direction, thumbRect) )
//                    {
//                        scrollTimer.stop();
//                        scrollListener.setDirection(direction);
//                        scrollTimer.start();
//                    }
//                }
            }
        }

        public boolean shouldScroll(int direction, Rectangle r) {
            if (slider.getOrientation() == JSlider.VERTICAL) {
                if (drawInverted() ? direction < 0 : direction > 0) {
                    if (r.y + r.height <= currentMouseY) {
                        return false;
                    }
                }
                else if (r.y >= currentMouseY) {
                    return false;
                }
            }
            else {
                if (drawInverted() ? direction < 0 : direction > 0) {
                    if (r.x + r.width >= currentMouseX) {
                        return false;
                    }
                }
                else if (r.x <= currentMouseX) {
                    return false;
                }
            }

            if (direction > 0 && getRangeLeft() + getRangeRight() >= slider.getMaximum()) {
                return false;
            }
            else if (direction < 0 && getRangeLeft() <= slider.getMinimum()) {
                return false;
            }
            return true;
        }

        /**
         * Set the models value to the position of the top/left
         * of the thumb relative to the origin of the track.
         */
        public void mouseDragged(MouseEvent e) {
            BasicScrollBarUI ui;

            if (!slider.isEnabled())
                return;

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (isDraggingThumb) {
                int thumbMiddle = 0;
                int extMiddle = 0;
                switch (slider.getOrientation()) {
                case JSlider.VERTICAL:
                    int halfThumbHeight = thumbRect.height / 2;
                    int thumbTop = e.getY() - offset;
                    int hardTop = trackRect.y;
                    int hardBottom = trackRect.y + (trackRect.height - 1);

                    thumbTop = Math.max(thumbTop, hardTop - halfThumbHeight);
                    thumbTop = Math.min(thumbTop, hardBottom - halfThumbHeight);

                    thumbMiddle = thumbTop + halfThumbHeight;
                    extMiddle = extRect.y + extRect.height / 2;
                    if ((showExtent) && (extMiddle > thumbMiddle)) {
                        thumbMiddle = extMiddle;
                        thumbTop = thumbMiddle - halfThumbHeight;
                    }

                    setRangeMin(valueForYPosition(thumbMiddle));
                    setThumbLocation(thumbRect.x, thumbTop);
                    break;
                case JSlider.HORIZONTAL:
                    int halfThumbWidth = thumbRect.width / 2;
                    int thumbLeft = e.getX() - offset;
                    int hardLeft = trackRect.x;
                    int hardRight = trackRect.x + (trackRect.width - 1);

                    thumbLeft = Math.max(thumbLeft, hardLeft - halfThumbWidth);
                    thumbLeft = Math.min(thumbLeft, hardRight - halfThumbWidth);

                    thumbMiddle = thumbLeft + halfThumbWidth;
                    extMiddle = extRect.x + extRect.width / 2;
                    if ((showExtent) && (extMiddle < thumbMiddle)) {
                        thumbMiddle = extMiddle;
                        thumbLeft = thumbMiddle - halfThumbWidth;
                    }

                    setRangeMin(valueForXPosition(thumbMiddle));
                    setThumbLocation(thumbLeft, thumbRect.y);
                    break;
                default:
                    return;
                }
            }
            else if (isDraggingExtent) {
                int extMiddle = 0;
                int thumbMiddle = 0;
                switch (slider.getOrientation()) {
                case JSlider.VERTICAL:
                    int halfThumbHeight = extRect.height / 2;
                    int extTop = e.getY() - offset;
                    int trackTop = trackRect.y;
                    int trackBottom = trackRect.y + (trackRect.height - 1);

                    extTop = Math.max(extTop, trackTop - halfThumbHeight);
                    extTop = Math.min(extTop, trackBottom - halfThumbHeight);

                    extMiddle = extTop + halfThumbHeight;
                    thumbMiddle = thumbRect.y + thumbRect.height / 2;
                    if (thumbMiddle < extMiddle) {
                        extMiddle = thumbMiddle;
                        extTop = extMiddle - halfThumbHeight;
                    }

                    setExtentLocation(extRect.x, extTop);
                    setRangeMax(valueForYPosition(extMiddle));
                    break;
                case JSlider.HORIZONTAL:
                    int halfThumbWidth = extRect.width / 2;
                    int extLeft = e.getX() - offset;
                    int trackLeft = trackRect.x;
                    int trackRight = trackRect.x + (trackRect.width - 1);

                    extLeft = Math.max(extLeft, trackLeft - halfThumbWidth);
                    extLeft = Math.min(extLeft, trackRight - halfThumbWidth);

                    extMiddle = extLeft + halfThumbWidth;
                    thumbMiddle = thumbRect.x + thumbRect.width / 2;
                    if (thumbMiddle > extMiddle) {
                        extMiddle = thumbMiddle;
                        extLeft = extMiddle - halfThumbWidth;
                    }

                    setRangeMax(valueForXPosition(extMiddle));
                    setExtentLocation(extLeft, extRect.y);
                    break;
                default:
                    return;
                }
            } // of dragging extent
        } // mouse dragged

    } // RangeSliderTrackListener


}