//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	TimeLineNodeModel
//
//--- Description -------------------------------------------------------------
//	The model for a time line node
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	12/10/99	M. Fishman
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

import java.beans.PropertyChangeListener;
import java.util.Comparator;

/**
 *
 *  The model for a time line node.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/10/99
 * @author		M. Fishman
 **/
public interface TimeLineNodeModel {

    // property types
    public static final String MODE = "Mode";
    public static final String START_TIME = "StartTime";
    public static final String END_TIME = "EndTime";
    public static final String NODE = "Node";
    public static final String NAME = "Name";
    public static final String PARENT = "Parent";


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
    public void setStartTime(Time time);

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
    public void setEndTime(Time time);

    /**
     *
     * move node by specified time
     *
     **/
    public void moveTimeLineNodeBy(Time time);

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
    public void setDuration(Time durationLength);


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
     * returns whether the node intersects the passed in node
     *
     **/
    public boolean intersects(TimeLineNodeModel node);


    /**
     *
     * move node to a specified location
     *
     **/
    public void setTimeLineNode(Time start, Time end);

    /**
     *
     * add a property change listener to the model
     *
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * remove a property change listener from the model
     *
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * get the time line node's parent
     *
     */
    public TimeLineModel getParent();

    /**
     *
     * set the time line node's parent
     *
     */
    public void setParent(TimeLineModel parent);

    /**
     *
     * is the node considered a constant
     *
     */
    public boolean isConstant();

    /**
     *
     * set whether or not the node is considered a constant or not
     *
     */
    public void setConstant(boolean isConstant);


    /**
     *
     * get the gui node class for this model
     *
     */
    public Class getGUIClass();


    /**
     *
     * inner class used for sorting time line nodes
     *
     **/
    public class TimeLineNodeComparator implements Comparator {

        public int compare(Object o1,
                           Object o2) {
            double start1 = ((TimeLineNodeModel) o1).getStartTime().getValue(Time.SECOND);
            double start2 = ((TimeLineNodeModel) o2).getStartTime().getValue(Time.SECOND);
            return (int) Math.round(start1 - start2);

        }

        public boolean equals(Object obj) {
            return super.equals(obj);
        }

    }

}
