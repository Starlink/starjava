//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	DefaultTimeLineNodeModel
//
//--- Description -------------------------------------------------------------
//	default version of the time line node model
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
import java.beans.PropertyChangeSupport;


/**
 * Default version of the time line node model.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/10/99
 * @author		M. Fishman
 **/
public class DefaultTimeLineNodeModel implements TimeLineNodeModel {

    private boolean fIsConstant;
    private String fTimeLineNodeName = "unknown";
    private Time fStartTime = new Time(0.0);
    private Time fEndTime = new Time(0.0);
    private PropertyChangeSupport fChangeSupport = null;
    private TimeLineModel fParent;

    /**
     *
     * constructor
     *
     **/
    public DefaultTimeLineNodeModel(Time start, Time end) {
        this(start, end, "unknown", false);
    }

    public DefaultTimeLineNodeModel(Time startTime, Time endTime, String name) {
        this(startTime, endTime, name, false);
    }

    public DefaultTimeLineNodeModel(Time startTime, Time endTime, String name, boolean isConstant) {
        fChangeSupport = new PropertyChangeSupport(this);
        fTimeLineNodeName = name;
        fStartTime = startTime;
        fEndTime = endTime;
        fIsConstant = isConstant;

    }


    /**
     *
     * get the time on the time line that this node starts
     *
     **/
    public Time getStartTime() {
        return fStartTime;
    }

    /**
     *
     * set the time that this node starts
     *
     **/
    public void setStartTime(Time time) {
        if (time.getValue(Time.SECOND) != fStartTime.getValue(Time.SECOND)) {
            Time oldTime = fStartTime;
            if (time.getValue(Time.SECOND) < getEndTime().getValue(Time.SECOND)) {
                fStartTime = time;
                fChangeSupport.firePropertyChange(TimeLineNodeModel.START_TIME, oldTime, time);
            }
        }
    }

    /**
     *
     * move node by specified amount
     *
     **/
    public void moveTimeLineNodeBy(Time time) {
        if (time.getValue(Time.SECOND) != 0) {
            Time oldStartTime = fStartTime;
            Time oldEndTime = fEndTime;
            fStartTime = new Time(fStartTime.getValue(Time.SECOND) + time.getValue(Time.SECOND),
                    Time.SECOND);
            fEndTime = new Time(fEndTime.getValue(Time.SECOND) + time.getValue(Time.SECOND),
                    Time.SECOND);
            fChangeSupport.firePropertyChange(TimeLineNodeModel.START_TIME, oldStartTime, fStartTime);
            fChangeSupport.firePropertyChange(TimeLineNodeModel.END_TIME, oldEndTime, fEndTime);

        }
    }

    /**
     *
     * get the time on the time line that this node ends
     *
     **/
    public Time getEndTime() {
        return fEndTime;
    }

    /**
     *
     * set the time on the time line that this node ends
     *
     **/
    public void setEndTime(Time time) {
        if (time.getValue(Time.SECOND) != fEndTime.getValue(Time.SECOND)) {
            Time oldTime = fEndTime;
            if (time.getValue(Time.SECOND) > getStartTime().getValue(Time.SECOND)) {
                fEndTime = time;
                fChangeSupport.firePropertyChange(TimeLineNodeModel.END_TIME, oldTime, time);
            }

        }
    }


    /**
     *
     * get the duration of the time line node
     *
     **/
    public Time getDuration() {
        double value = fEndTime.getValue(Time.SECOND) - fStartTime.getValue(Time.SECOND);
        return new Time(value, Time.SECOND);
    }


    /**
     *
     * set the duration of the time line node
     *
     **/
    public void setDuration(Time durationLength) {
        if (durationLength != null) {
            Time val = getStartTime();

            setEndTime(new Time(fStartTime.getValue(Time.SECOND) +
                    durationLength.getValue(Time.SECOND), Time.SECOND));
        }
    }


    /**
     *
     * add a property change listener to the node
     *
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        fChangeSupport.addPropertyChangeListener(listener);

    }

    /**
     *
     * remove a propertyChangeListener to the node
     *
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        fChangeSupport.removePropertyChangeListener(listener);
    }


    /**
     *
     * give the time line node a name
     *
     **/
    public void setTimeLineNodeName(String name) {
        if (fTimeLineNodeName != name) {
            String oldName = fTimeLineNodeName;
            fTimeLineNodeName = name;
            fChangeSupport.firePropertyChange(TimeLineNodeModel.NAME, oldName, name);
        }
    }

    /**
     *
     * get the name of the time line node
     *
     **/
    public String getTimeLineNodeName() {
        return fTimeLineNodeName;
    }

    /**
     *
     * returns whether the node intersects the passed in node
     *
     **/
    public boolean intersects(TimeLineNodeModel node) {
        double nodeStart = node.getStartTime().getValue(Time.SECOND);
        double nodeEnd = node.getEndTime().getValue(Time.SECOND);
        double thisStart = fStartTime.getValue(Time.SECOND);
        double thisEnd = fEndTime.getValue(Time.SECOND);
        boolean result = false;
        if (((nodeStart > thisStart) && (nodeStart < thisEnd)) ||
                ((nodeEnd < thisEnd) && (nodeEnd > thisStart)) ||
                ((nodeStart < thisStart) && (nodeEnd > thisStart))) {
            result = true;
        }
        return result;
    }

    /**
     *
     * get the time line node's parent
     *
     */
    public TimeLineModel getParent() {
        return fParent;
    }

    /**
     *
     * set the time line node's parent
     *
     */
    public void setParent(TimeLineModel parent) {
        if (parent != fParent) {
            TimeLineModel oldParent = fParent;
            fParent = parent;
            fChangeSupport.firePropertyChange(TimeLineNodeModel.PARENT, oldParent,
                    fParent);
        }
    }

    /**
     *
     * move node to a specified location
     *
     **/
    public synchronized void setTimeLineNode(Time start, Time end) {
        Time oldStartTime = fStartTime;
        Time oldEndTime = fEndTime;
        if ((fStartTime.getValue() != start.getValue()) ||
                (fEndTime.getValue() != end.getValue())) {
            fStartTime = start;
            fEndTime = end;


            fChangeSupport.firePropertyChange(TimeLineNodeModel.START_TIME, oldStartTime,
                    fStartTime);
            fChangeSupport.firePropertyChange(TimeLineNodeModel.END_TIME, oldEndTime, fEndTime);

        }

    }

    /**
     *
     * is the node considered a constant
     *
     */
    public boolean isConstant() {
        return fIsConstant;
    }

    /**
     *
     * set whether or not the node is considered a constant or not
     *
     */
    public void setConstant(boolean isConstant) {
        fIsConstant = isConstant;
    }


    /**
     *
     * get the gui node class for this model
     *
     */
    public Class getGUIClass() {
        return DefaultTimeLineNode.class;
    }

    public String toString() {
        String str = getTimeLineNodeName() + ":\n";
        str += "\t start time:\t" + getStartTime().getValue(Time.MINUTE) + "\n";
        str += "\t end time:\t" + getEndTime().getValue(Time.MINUTE) + "\n";
        return str;
    }


}

