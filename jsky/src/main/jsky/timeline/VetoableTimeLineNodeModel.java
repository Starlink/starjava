//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	TimeLineNodeModel
//
//--- Description -------------------------------------------------------------
//	The model for a vetoable event aware time line node
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

import java.beans.VetoableChangeListener;


/**
 *
 * The model for an event aware time line node.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/10/99
 * @author		M. Fishman
 **/
public interface VetoableTimeLineNodeModel extends VetoableChangeListener, TimeLineNodeModel {

    // veto reasons
    public static final String NODE_OVERLAP = "Node Overlap";
    public static final String NODE_MIN_SIZE_EXCEEDED = "The Node minimum size has been exceeded";
    public static final String NODE_MAX_SIZE_EXCEEDED = "The Node maximum size has been exceeded";
    public static final String HIT_LEFT_EDGE = "HitLeftEdge";
    public static final String HIT_RIGHT_EDGE = "HitRightEdge";
    public static final String BIC = "Because I can";  // this type should not really be used.
    // it is really just a placeholder.





    /**
     *
     * set the point on the time line that this node starts
     *
     **/
    public void setValidStartTime(Time time) throws DetailedPropertyVetoException;

    /**
     *
     * set the time that this node ends
     *
     **/
    public void setValidEndTime(Time time) throws DetailedPropertyVetoException;

    /**
     *
     * move node by specified time
     *
     **/
    public void moveTimeLineNodeByValid(Time time) throws DetailedPropertyVetoException;


    /**
     *
     * set the duration of the time line node
     *
     **/
    public void setValidDuration(Time durationLength) throws DetailedPropertyVetoException;


    /**
     *
     * revert the time line node to its previous position
     *
     **/
    public void revertToPrevious();

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
     * move node to a specified location
     *
     **/
    public void setValidTimeLineNode(Time start, Time end) throws DetailedPropertyVetoException;


}
