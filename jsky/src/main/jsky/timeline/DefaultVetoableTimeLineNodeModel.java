//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	TimeLineNodeModel
//
//--- Description -------------------------------------------------------------
//	The default class for the vetoable time line node model
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;


/**
 *
 * The default class for the vetoable time line node model.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/10/99
 * @author		M. Fishman
 **/
public class DefaultVetoableTimeLineNodeModel extends DefaultTimeLineNodeModel implements VetoableTimeLineNodeModel {

    private Time fOldStartTime = new Time(0.0);
    private Time fOldEndTime = new Time(0.0);
    protected VetoableChangeSupport fChangeSupport = null;


    /**
     *
     * constructor
     *
     **/
    public DefaultVetoableTimeLineNodeModel(Time start, Time end) {
        this(start, end, "unknown", false);
    }

    public DefaultVetoableTimeLineNodeModel(Time startTime, Time endTime, String name) {
        this(startTime, endTime, name, false);
    }

    public DefaultVetoableTimeLineNodeModel(Time startTime, Time endTime, String name, boolean isConstant) {
        super(startTime, endTime, name, isConstant);
        fChangeSupport = new VetoableChangeSupport(this);
        fOldStartTime = startTime;
        fOldEndTime = endTime;
    }


    /**
     *
     * set the time that this node starts
     *
     **/
    public void setValidStartTime(Time time) throws DetailedPropertyVetoException {
        if (time.getValue(Time.SECOND) != getStartTime().getValue(Time.SECOND)) {
            Time oldTime = getStartTime();
            try {
                if (time.getValue(Time.SECOND) < getEndTime().getValue(Time.SECOND)) {
                    super.setStartTime(time);
                    fChangeSupport.fireVetoableChange(TimeLineNodeModel.START_TIME, oldTime, time);
                    fOldStartTime = oldTime;

                }
                else {
                    throw new DetailedPropertyVetoException(this, VetoableTimeLineNodeModel.NODE_MIN_SIZE_EXCEEDED,
                            "invalid start time", null);
                }

            }
            catch (DetailedPropertyVetoException ex) {
                setStartTime(oldTime);
                throw ex;
            }
            catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     *
     * move node by specified amount
     *
     **/
    public synchronized void moveTimeLineNodeByValid(Time time) throws DetailedPropertyVetoException {
        if (time.getValue(Time.SECOND) != 0) {
            Time oldStartTime = getStartTime();
            Time oldEndTime = getEndTime();
            try {

                super.moveTimeLineNodeBy(time);
                fOldStartTime = oldStartTime;
                fOldEndTime = oldEndTime;

                fChangeSupport.fireVetoableChange(TimeLineNodeModel.START_TIME, oldStartTime,
                        getStartTime());
                fChangeSupport.fireVetoableChange(TimeLineNodeModel.END_TIME, oldEndTime, getEndTime());


            }
            catch (DetailedPropertyVetoException ex) {
                super.moveTimeLineNodeBy(new Time(time.getValue() * -1.0));
                throw ex;
            }
            catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     *
     * set the time on the time line that this node ends
     *
     **/
    public void setValidEndTime(Time time) throws DetailedPropertyVetoException {
        if (time.getValue(Time.SECOND) != getEndTime().getValue(Time.SECOND)) {
            Time oldTime = getEndTime();
            try {
                if (time.getValue(Time.SECOND) > getStartTime().getValue(Time.SECOND)) {
                    super.setEndTime(time);
                    fChangeSupport.fireVetoableChange(TimeLineNodeModel.END_TIME, oldTime, time);
                    fOldEndTime = oldTime;

                }
                else {
                    throw new DetailedPropertyVetoException(this, VetoableTimeLineNodeModel.NODE_MIN_SIZE_EXCEEDED,
                            "invalid end time", null);
                }

            }
            catch (DetailedPropertyVetoException ex) {
                super.setEndTime(oldTime);
                throw ex;
            }
            catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     *
     * set the duration of the time line node
     *
     **/
    public void setValidDuration(Time durationLength) throws DetailedPropertyVetoException {
        if (durationLength != null) {
            Time val = getStartTime();

            setValidEndTime(new Time(getStartTime().getValue(Time.SECOND) +
                    durationLength.getValue(Time.SECOND), Time.SECOND));
        }
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
     * revert the time line node to its previous position
     *
     **/
    public void revertToPrevious() {
        try {
            super.setStartTime(fOldStartTime);
            super.setEndTime(fOldEndTime);
            fChangeSupport.fireVetoableChange(TimeLineNodeModel.NODE, 0, 1);
        }
        catch (DetailedPropertyVetoException ex) {
            // ignore it
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }

    public void vetoableChange(PropertyChangeEvent evt) throws DetailedPropertyVetoException {

        if ((evt.getPropertyName() != TimeLine.NODE_REMOVED) && (evt.getSource() instanceof TimeLineNodeModel)) {
            TimeLineNodeModel node = (TimeLineNodeModel) evt.getSource();

            if ((node != this) && (intersects(node) || node.intersects(this))) {
                throw new DetailedPropertyVetoException(this, VetoableTimeLineNodeModel.NODE_OVERLAP,
                        "node " + getTimeLineNodeName() + " overlaps "
                        + node.getTimeLineNodeName(), evt);

            }
        }
    }


    /**
     *
     * move node to a specified location
     *
     **/
    public synchronized void setValidTimeLineNode(Time start, Time end) throws DetailedPropertyVetoException {

        Time oldStartTime = getStartTime();
        Time oldEndTime = getEndTime();
        try {

            setTimeLineNode(start, end);

            fChangeSupport.fireVetoableChange(TimeLineNodeModel.START_TIME, oldStartTime,
                    getStartTime());
            fChangeSupport.fireVetoableChange(TimeLineNodeModel.END_TIME, oldEndTime, getEndTime());


        }
        catch (DetailedPropertyVetoException ex) {
            setTimeLineNode(start, end);
            throw ex;
        }
        catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }


}
