//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	TimeLineNode
//
//--- Description -------------------------------------------------------------
//	An interface for a single node on a time line
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


//       specific imports
// tbd - some rearchitecting should be done to remove the need for these SEA

import jsky.science.Time;

import javax.swing.JLabel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.beans.VetoableChangeListener;


/**
 *
 * An interface for a single node on the time line.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		04/27/99
 * @author		M. Fishman
 **/
public interface TimeLineNode extends VetoableChangeListener {


    // selection modes
    public static final int UNSELECTED = 0;
    public static final int LEFT_HANDLE_SELECTED = 1;
    public static final int NODE_SELECTED = 2;
    public static final int RIGHT_HANDLE_SELECTED = 3;

    // fonts
    public static final Font DEFAULT_FONT = new JLabel().getFont().deriveFont(Font.PLAIN, 10.0F);
    public static final Font ROTATED_FONT = DEFAULT_FONT.deriveFont(AffineTransform.getRotateInstance(11.0 * Math.PI / 6.0));
    public static final Font REVERSE_ROTATED_FONT = DEFAULT_FONT.deriveFont(AffineTransform.getRotateInstance(Math.PI / 6.0));

    // cursors
    public static final Cursor MOVE_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);


    // default label spacing
    public static final double DEFAULT_LABEL_SPACE = 2.0;

    // veto reasons
    public static final String NODE_OVERLAP = "Node Overlap";
    public static final String NODE_MIN_SIZE_EXCEEDED = "The Node minimum size has been exceeded";
    public static final String NODE_MAX_SIZE_EXCEEDED = "The Node maximum size has been exceeded";
    public static final String HIT_LEFT_EDGE = "HitLeftEdge";
    public static final String HIT_RIGHT_EDGE = "HitRightEdge";
    public static final String BIC = "Because I can";  // this type should not really be used.
    // it is really just a placeholder.

    // property types
    public static final String MODE = "Mode";
    public static final String SELECTED_COLOR = "SelectedColor";
    public static final String UNSELECTED_COLOR = "UnselectedColor";
    public static final String START_TIME = "StartTime";
    public static final String END_TIME = "EndTime";
    public static final String NODE = "Node";
    public static final String NAME = "Name";


    /**
     *
     * set the selection mode of the time linenode
     *
     **/
    public void setSelectionMode(int mode);


    /**
     *
     * get the selection mode of the time line node
     *
     **/
    public int getSelectionMode();

    /**
     *
     * set the unselected color for the time line node
     *
     **/
    public void setUnselectedColor(Color color);

    /**
     *
     * get the the unselected color the time line node
     *
     **/
    public Color getUnselectedColor();

    /**
     *
     * set the selected color for the time line node
     *
     **/
    public void setSelectedColor(Color color);

    /**
     *
     * get the the selected color the time line node
     *
     **/
    public Color getSelectedColor();

    /**
     *
     * get the time that this node starts
     *
     **/
    public Time getStartTime();

    /**
     *
     * set the point on the time line that this node starts
     *
     **/
    public void setStartTime(Time time) throws DetailedPropertyVetoException;

    /**
     *
     * get the time that this node ends
     *
     **/
    public Time getEndTime();

    /**
     *
     * set the time that this node ends
     *
     **/
    public void setEndTime(Time time) throws DetailedPropertyVetoException;

    /**
     *
     * move node by specified time
     *
     **/
    public void moveTimeLineNodeBy(Time time) throws DetailedPropertyVetoException;

    /**
     *
     * get the duration of the time line node
     *
     **/
    public Time getDuration();


    /**
     *
     * set the duration of the time line node
     *
     **/
    public void setDuration(Time durationLength) throws DetailedPropertyVetoException;


    /**
     *
     * give the time line node a name
     *
     **/
    public void setTimeLineNodeName(String name);

    /**
     *
     * get the name of the time line node
     *
     **/
    public String getTimeLineNodeName();


    /**
     *
     * paint the time line node
     *
     * @param graphics the graphics component to paint
     *
     **/
    public void paintTimeLineNode(Graphics2D graphics);


    /**
     *
     * add a property change listener to the node
     *
     **/
    public void addVetoableChangeListener(VetoableChangeListener listener);

    /**
     *
     * remove a propertyChangeListener to the node
     *
     **/
    public void removeVetoableChangeListener(VetoableChangeListener listener);


    /**
     *
     * handle mouse events
     *
     **/
    public void handleMouseEvent(MouseEvent evt);

    /**
     *
     * handle mouse events
     *
     **/
    public void handleMouseDragEvent(MouseEvent evt);


    /**
     *
     * handle mouse events
     *
     **/
    public void handleMouseMoveEvent(MouseEvent evt);

    /**
     *
     * handle key event
     *
     **/
    public void handleKeyEvent(KeyEvent evt) throws DetailedPropertyVetoException;


    /**
     *
     * set the parent time line
     *
     **/
    public void setParent(TimeLine timeLine);

    /**
     *
     * get the parent time line
     *
     **/
    public TimeLine getParent();


    /**
     *
     * returns whether the node is currently being dragged
     *
     **/
    public boolean isDragging();


    /**
     *
     * returns whther the node is currently selected
     *
     **/
    public boolean isSelected();

    /**
     *
     * revert the time line node to its previous position
     *
     **/
    public void revertToPrevious();


    /**
     *
     * returns whether the node intersects the passed in node
     *
     **/
    public boolean intersects(TimeLineNode node);

    /**
     *
     * returns what area of a time line node a point exists in
     *
     **/
    public int getAreaForPoint(Point pt);

    /**
     *
     * returns the center point for the time line node
     *
     **/
    public Point getCenterPoint();


    /**
     *
     * move node to a specified location
     *
     **/
    public void setTimeLineNode(Time start, Time end) throws DetailedPropertyVetoException;

    /**
     *
     * returns whether the specified point is in the node
     *
     **/
    public boolean containsPoint(Point pt);


    /**
     *
     * returns a description for the area at the specified point
     *
     **/
    public String getDescription(Point pt);


    /**
     *
     * get the node's underlying model
     *
     **/
    public TimeLineNodeModel getModel();

    /**
     *
     * set the time line node's underlying model
     *
     **/
    public void setModel(TimeLineNodeModel model);

    /**
     *
     * get the cursor for the specified point
     *
     */
    public Cursor getCursor(MouseEvent evt);


}

