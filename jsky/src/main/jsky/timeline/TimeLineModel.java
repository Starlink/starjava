//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	TimeLineModel
//
//--- Description -------------------------------------------------------------
//	The model for a time line
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
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 *
 * The model for a time line.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		05/19/99
 * @author		M. Fishman
 **/
public interface TimeLineModel {

    public static final String NODE_ADDED = "node added";
    public static final String NODE_REMOVED = "node removed";
    public static final String ALL_NODES_REMOVED = "all nodes removed";

    /**
     *
     * add a time line node to the time line without checking its legality
     *
     **/
    public void addTimeLineNode(TimeLineNodeModel node);


    /**
     *
     * remove a time line node from the time line
     *
     **/
    public void removeTimeLineNode(TimeLineNodeModel node);

    /**
     *
     * remove all time line nodes from time line
     *
     **/
    public void removeAllTimeLineNodes();


    /**
     *
     * get the number of intervals in the time line
     *
     **/
    public int getIntervalCount();


    /**
     *
     * get an iterator for the time line nodes
     *
     **/
    public Iterator getTimeLineNodesIterator();

    /**
     *
     * get the time line nodes in a uneditable list
     *
     **/
    public List getTimeLineNodes();


    /**
     *
     * add a  property change listener to the time line.
     *
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     *
     * remove a property change listener from the time line
     *
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener);


    /**
     *
     * takes a time and converts it into a date where the new date is
     * equal to the start date + the time
     *
     **/
    public Date getDateForTime(Time time);

    /**
     *
     * takes a date and convert it to a time where the new time is
     * equal to the date - start date
     *
     **/
    public Time getTimeForDate(Date date);

    /**
     *
     * set the date from which the timeline should start
     *
     *  Note: if the date is not null then all time values are considered offsets from it
     *
     **/
    public void setStartDate(Date date);


    /**
     *
     * get the start date
     *
     **/
    public Date getStartDate();


    /**
     *
     * get the starting value in the timeline
     *
     **/
    public Time getStartTime();

    /**
     *
     * get the ending value of the timeline
     *
     **/
    public Time getEndTime();


    /**
     *
     * returns whether or not the model contains the specified node
     */
    public boolean contains(TimeLineNodeModel model);


}
