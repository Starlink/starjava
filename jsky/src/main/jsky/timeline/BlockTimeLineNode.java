//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	TimeLineNode
//
//--- Description -------------------------------------------------------------
//	A class for a block on the time line that is uneditable by a user
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	05/19/99	M. Fishman
//
//		Original implementation.
//
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
//package gov.nasa.gsfc.util.gui;

package jsky.timeline;

import jsky.science.Time;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.text.DecimalFormat;


/**
 * An interface for a single node on the time line.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		04/27/99
 * @author		M. Fishman
 **/
public class BlockTimeLineNode implements TimeLineNode {

    private static float sThumbHeight = 12;

    protected VetoableChangeSupport fChangeSupport = null;
    private DefaultVetoableTimeLineNodeModel fModel;
    private Color fUnselectedColor = Color.gray;
    private float fThumbBegin = 0.0f;
    private float fThumbEnd = 0.0f;
    private BasicStroke fDefaultStroke = new BasicStroke();
    private BasicStroke fShadowStroke = new BasicStroke(1);
    private Line2D.Float fThumbShadowLine = new Line2D.Float();
    private Line2D.Float fThumbTopShadowLine = new Line2D.Float();
    private Line2D.Float fThumbRightShadowLine = new Line2D.Float();
    private Line2D.Float fThumbLeftShadowLine = new Line2D.Float();
    private TimeLine fTimeLine = null;
    protected Rectangle2D.Float fThumb = new Rectangle2D.Float();

    /**
     *
     * constructor
     *
     **/
    public BlockTimeLineNode(Time start, Time end) {
        this(start, end, "unknown");
    }

    public BlockTimeLineNode(Time startTime, Time endTime, String name) {

        fModel = new DefaultVetoableTimeLineNodeModel(startTime, endTime, name, true);
        fChangeSupport = new VetoableChangeSupport(this);
        fModel.addVetoableChangeListener(new VetoableChangeListener() {

            public void vetoableChange(PropertyChangeEvent evt)
                    throws PropertyVetoException {
                fChangeSupport.fireVetoableChange(evt.getPropertyName(),
                        evt.getOldValue(),
                        evt.getNewValue());
            }

        });
    }

    /**
     *
     * set the selection mode of the time linenode
     *
     **/
    public void setSelectionMode(int mode) {
        // not used in this class
    }

    /**
     *
     * get the selection mode of the time line node
     *
     **/
    public int getSelectionMode() {
        return 0;
    }

    /**
     *
     * set the unselected color for the time line node
     *
     **/
    public void setUnselectedColor(Color color) {
        if (color != fUnselectedColor) {
            Color oldColor = fUnselectedColor;
            try {
                fUnselectedColor = color;
                fChangeSupport.fireVetoableChange(TimeLineNode.UNSELECTED_COLOR,
                        oldColor, color);

            }
            catch (DetailedPropertyVetoException ex) {
                fUnselectedColor = oldColor;
            }
            catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }

        }
    }

    /**
     *
     * get the the unselected color the time line node
     *
     **/
    public Color getUnselectedColor() {
        return fUnselectedColor;
    }

    /**
     *
     * set the selected color for the time line node
     *
     **/
    public void setSelectedColor(Color color) {
        // there is no selected color
    }

    /**
     *
     * get the the selected color the time line node
     *
     **/
    public Color getSelectedColor() {
        return null;
    }

    /**
     *
     * get the time on the time line that this node starts
     *
     **/
    public Time getStartTime() {
        return fModel.getStartTime();
    }

    /**
     *
     * set the time that this node starts
     *
     **/
    public void setStartTime(Time time) throws DetailedPropertyVetoException {
        try {
            fModel.setValidStartTime(time);
        }
        catch (DetailedPropertyVetoException ex) {
            calculateNodeDimensions();
            throw ex;
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }

    }

    /**
     *
     * move node by specified amount
     *
     **/
    public void moveTimeLineNodeBy(Time time) throws DetailedPropertyVetoException {
        try {
            fModel.moveTimeLineNodeByValid(time);

        }
        catch (DetailedPropertyVetoException ex) {
            calculateNodeDimensions();
            throw ex;
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *
     * get the time on the time line that this node ends
     *
     **/
    public Time getEndTime() {
        return fModel.getEndTime();
    }

    /**
     *
     * set the time on the time line that this node ends
     *
     **/
    public void setEndTime(Time time) throws DetailedPropertyVetoException {
        try {
            fModel.setValidEndTime(time);
        }
        catch (DetailedPropertyVetoException ex) {
            calculateNodeDimensions();
            throw ex;
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }


    /**
     *
     * get the duration of the time line node
     *
     **/
    public Time getDuration() {
        return fModel.getDuration();
    }


    /**
     *
     * set the duration of the time line node
     *
     **/
    public void setDuration(Time durationLength) throws DetailedPropertyVetoException {
        try {
            fModel.setValidDuration(durationLength);
        }
        catch (DetailedPropertyVetoException ex) {
            calculateNodeDimensions();
            throw ex;
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * paint the time line node
     *
     * @param graphics the graphics component to paint
     **/
    public void paintTimeLineNode(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font origFont = graphics.getFont();
        Time startTime = fModel.getStartTime();
        Time endTime = fModel.getEndTime();

        if (!((startTime.getValue(Time.SECOND) > fTimeLine.getDisplayEnd().getValue(Time.SECOND)) ||
                (endTime.getValue(Time.SECOND) < fTimeLine.getDisplayStart().getValue(Time.SECOND)))) {

            calculateNodeDimensions();
            float thumbWidth = (fThumbEnd - fThumbBegin);


            // draw the thumb
            fThumb.height = sThumbHeight;
            fThumb.width = thumbWidth;
            fThumb.x = fThumbBegin;
            fThumb.y = (fTimeLine.getHeight() / 2f - sThumbHeight / 2f);
            graphics.setStroke(fDefaultStroke);
            graphics.setColor(fUnselectedColor);

            graphics.draw(fThumb);
            graphics.fill(fThumb);





            // this is the bottom shadow line of the thumb
            fThumbShadowLine.x1 = fThumb.x;
            fThumbShadowLine.y1 = fThumb.y + sThumbHeight;
            fThumbShadowLine.x2 = fThumb.x + thumbWidth;
            fThumbShadowLine.y2 = fThumb.y + sThumbHeight;

            graphics.setStroke(fShadowStroke);
            graphics.setColor(Color.black);
            graphics.draw(fThumbShadowLine);

            // this is the top shadow line of the thumb
            fThumbTopShadowLine.x1 = fThumb.x;
            fThumbTopShadowLine.y1 = fThumb.y;
            fThumbTopShadowLine.x2 = fThumb.x + thumbWidth;
            fThumbTopShadowLine.y2 = fThumb.y;

            graphics.setStroke(fShadowStroke);
            graphics.setColor(Color.white);
            graphics.draw(fThumbTopShadowLine);

            // this is the left shadow line of the thumb
            fThumbLeftShadowLine.x1 = fThumb.x;
            fThumbLeftShadowLine.y1 = fThumb.y;
            fThumbLeftShadowLine.x2 = fThumb.x;
            fThumbLeftShadowLine.y2 = fThumb.y + sThumbHeight;

            graphics.setStroke(fShadowStroke);
            graphics.setColor(Color.lightGray);
            graphics.draw(fThumbLeftShadowLine);

            // this is the right shadow line of the thumb
            fThumbRightShadowLine.x1 = fThumb.x + thumbWidth;
            fThumbRightShadowLine.y1 = fThumb.y;
            fThumbRightShadowLine.x2 = fThumb.x + thumbWidth;
            fThumbRightShadowLine.y2 = fThumb.y + sThumbHeight;

            graphics.setStroke(fShadowStroke);
            graphics.setColor(Color.black);
            graphics.draw(fThumbRightShadowLine);




            // draw start label
            DecimalFormat startForm = new DecimalFormat();
            startForm.setMaximumFractionDigits(1);

            if (startTime.getValue() > fTimeLine.getDisplayStart().getValue()) {
                graphics.setFont(TimeLineNode.DEFAULT_FONT);
                String startStr = startForm.format(startTime.getValue(Time.MINUTE));
                Rectangle2D startBounds = graphics.getFontMetrics().getStringBounds(startStr,
                        graphics);
                graphics.setColor(Color.black);
                float startX = fThumb.x - (float) (startBounds.getWidth() / 2.0);
                float startY = fThumb.y + sThumbHeight + (float) (startBounds.getHeight() + TimeLineNode.DEFAULT_LABEL_SPACE);
                if (startBounds.getWidth() > thumbWidth / 2f) {
                    graphics.setFont(TimeLineNode.REVERSE_ROTATED_FONT);

                }
                graphics.drawString(startStr, startX, startY);
                graphics.setFont(origFont);
            }

            // draw end label
            if (endTime.getValue() < fTimeLine.getDisplayEnd().getValue()) {
                graphics.setFont(TimeLineNode.DEFAULT_FONT);
                DecimalFormat endForm = new DecimalFormat();
                endForm.setMaximumFractionDigits(1);
                String endStr = startForm.format(endTime.getValue(Time.MINUTE));
                Rectangle2D endBounds = graphics.getFontMetrics().getStringBounds(endStr,
                        graphics);
                graphics.setColor(Color.black);
                float endX = fThumb.x + thumbWidth - (float) (endBounds.getWidth() / 2.0);
                float endY = fThumb.y + sThumbHeight + (float) (endBounds.getHeight() + TimeLineNode.DEFAULT_LABEL_SPACE);
                if (endBounds.getWidth() > thumbWidth / 2f) {
                    graphics.setFont(TimeLineNode.REVERSE_ROTATED_FONT);

                }
                graphics.drawString(endStr, endX, endY);
                graphics.setFont(origFont);

            }


            // draw name label

            //Font newFont=origFont.deriveFont(origFont.getSize2D() -2f);
            //graphics.setFont(newFont);
            graphics.setFont(TimeLineNode.DEFAULT_FONT);
            Rectangle2D nameBounds = graphics.getFontMetrics().getStringBounds(getTimeLineNodeName(),
                    graphics);
            graphics.setColor(Color.black);
            float textX = fThumb.x + thumbWidth / 2f - (float) (nameBounds.getWidth() / 2.0);
            float textY = fThumb.y - (float) (nameBounds.getHeight() - TimeLineNode.DEFAULT_LABEL_SPACE);
            if (nameBounds.getWidth() > thumbWidth) {
                graphics.setFont(TimeLineNode.ROTATED_FONT);
                textX = fThumb.x + thumbWidth / 2f;

            }


            graphics.drawString(this.getTimeLineNodeName(), textX, textY);
            graphics.setFont(origFont);


        }


    }

    /**
     *
     * handle mouse events
     *
     **/
    public void handleMouseEvent(MouseEvent evt) {
        // node not mouse aware
    }

    /**
     *
     * handle mouse events
     *
     **/
    public void handleMouseDragEvent(MouseEvent evt) {
        // node not mouse aware
    }


    /**
     *
     * handle mouse events
     *
     **/
    public void handleMouseMoveEvent(MouseEvent evt) {
        // not mouse aware

    }

    /**
     *
     * handle key event
     *
     **/
    public void handleKeyEvent(KeyEvent evt) throws DetailedPropertyVetoException {
        // not key aware
    }


    /**
     *
     * give the time line node a name
     *
     **/
    public void setTimeLineNodeName(String name) {
        fModel.setTimeLineNodeName(name);
    }

    /**
     *
     * get the name of the time line node
     *
     **/
    public String getTimeLineNodeName() {
        return fModel.getTimeLineNodeName();
    }


    /**
     *
     * set the parent time line
     *
     **/
    public void setParent(TimeLine timeLine) {
        fTimeLine = timeLine;
    }

    /**
     *
     * get the parent time line
     *
     **/
    public TimeLine getParent() {
        return fTimeLine;
    }

    /**
     *
     * calculate thumb's dimensions in pixels
     *
     **/
    protected void calculateNodeDimensions() {
        fThumbBegin = fTimeLine.getPointForTime(getStartTime());
        fThumbEnd = fTimeLine.getPointForTime(getEndTime());
    }


    /**
     *
     * returns whether the node is currently being dragged
     *
     **/
    public boolean isDragging() {
        return false;
    }


    /**
     *
     * returns whther the node is currently selected
     *
     **/
    public boolean isSelected() {
        return false;
    }


    /**
     *
     * revert the time line node to its previous position
     *
     **/
    public void revertToPrevious() {
        fModel.revertToPrevious();
    }

    public void vetoableChange(PropertyChangeEvent evt) throws DetailedPropertyVetoException {
        if ((evt.getPropertyName() != TimeLine.NODE_REMOVED) && (evt.getSource() instanceof TimeLineNode)) {
            TimeLineNode node = (TimeLineNode) evt.getSource();

            if ((node != this) && (intersects(node) || node.intersects(this))) {
                throw new DetailedPropertyVetoException(this, VetoableTimeLineNodeModel.NODE_OVERLAP,
                        "node " + getTimeLineNodeName() + " overlaps "
                        + node.getTimeLineNodeName(), evt);

            }
        }
    }


    /**
     *
     * returns what area of a time line node a point exists in
     *
     **/
    public int getAreaForPoint(Point pt) {
        int result = TimeLineNode.UNSELECTED;
        if (fThumb.contains(pt.x, pt.y)) {
            result = TimeLineNode.NODE_SELECTED;
        }
        return result;
    }

    /**
     *
     * returns whether the node intersects the passed in node
     *
     **/
    public boolean intersects(TimeLineNode node) {
        boolean result = fModel.intersects(node.getModel());
        return result;
    }


    /**
     *
     * returns the center point for the time line node
     *
     **/
    public Point getCenterPoint() {
        double value = fModel.getStartTime().getValue() +
                (fModel.getEndTime().getValue() - fModel.getStartTime().getValue()) / 2.0;
        Time centerTime = new Time(value);
        float x = fTimeLine.getPointForTime(centerTime);
        float y = fTimeLine.getHeight() / 2f;
        Point pt = new Point((int) Math.round(x), (int) Math.round(y));
        return pt;
    }


    /**
     *
     * move node to a specified location
     *
     **/
    public synchronized void setTimeLineNode(Time start, Time end) throws DetailedPropertyVetoException {
        Time oldStartTime = fModel.getStartTime();
        Time oldEndTime = fModel.getEndTime();
        try {
            fModel.setValidTimeLineNode(start, end);
            if (fTimeLine != null) {
                Time displayEdge = fTimeLine.getDisplayStart();
                if ((fModel.getStartTime().getValue() < displayEdge.getValue() &&
                        oldStartTime.getValue() >= displayEdge.getValue())) {
                    fChangeSupport.fireVetoableChange(TimeLineNode.HIT_LEFT_EDGE,
                            oldStartTime, fModel.getStartTime());

                }


                displayEdge = fTimeLine.getDisplayEnd();
                if ((fModel.getEndTime().getValue() > displayEdge.getValue() &&
                        oldEndTime.getValue() <= displayEdge.getValue())) {
                    fChangeSupport.fireVetoableChange(TimeLineNode.HIT_RIGHT_EDGE, oldEndTime, fModel.getEndTime());
                }


            }


        }
        catch (DetailedPropertyVetoException ex) {
            throw ex;
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }


    /**
     *
     * returns whether the specified point is in the node
     *
     **/
    public boolean containsPoint(Point pt) {
        Time time = fTimeLine.getTimeForPoint(pt.x);
        boolean result = false;
        if ((time.getValue() >= fModel.getStartTime().getValue()) &&
                (time.getValue() <= fModel.getEndTime().getValue())) {
            result = true;
        }
        return result;

    }


    /**
     *
     * returns a description for the area at the specified point
     *
     **/
    public String getDescription(Point pt) {
        String result = null;
        if (fThumb.contains(pt.x, pt.y)) {
            result = getTimeLineNodeName();
            Time duration = getDuration();
            DecimalFormat form = new DecimalFormat();
            form.setMaximumFractionDigits(2);
            String str = form.format(duration.getValue(fTimeLine.getUnitsType()));
            str = str + " " + Time.getUnitsAbbrev(fTimeLine.getUnitsType());
            result += " = " + str;

        }

        return result;
    }


    /**
     *
     * add a property change listener to the node
     *
     **/
    public void addVetoableChangeListener(VetoableChangeListener listener) {
        fChangeSupport.addVetoableChangeListener(listener);

    }

    /**
     *
     * remove a propertyChangeListener to the node
     *
     **/
    public void removeVetoableChangeListener(VetoableChangeListener listener) {
        fChangeSupport.removeVetoableChangeListener(listener);
    }

    /**
     *
     * get the node's underlying model
     *
     **/
    public TimeLineNodeModel getModel() {
        return fModel;
    }

    /**
     *
     * set the time line node's underlying model
     *
     **/
    public void setModel(TimeLineNodeModel model) {
        // tbd
    }

    /**
     *
     * get the cursor for the specified point
     *
     */
    public Cursor getCursor(MouseEvent evt) {
        return getParent().getCursor();
    }

    public String toString() {
        return fModel.toString();
    }
}

