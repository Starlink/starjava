//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	DefaultTimeLineModel
//
//--- Description -------------------------------------------------------------
//	the default time line model
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	12/27/99	M. Fishman
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
import java.beans.PropertyChangeListener;
import java.util.*;


/**
 * The default model of a time line.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		12/27/99
 * @author		M. Fishman
 **/
public class DefaultTimeLineModel implements TimeLineModel {


    protected Comparator sComparator = new TimeLineNodeModel.TimeLineNodeComparator();
    protected List fNodes;
    protected List fChangeListeners;
    private int fIntervalCount;


    private Time fStartTime;
    private Time fEndTime;
    private Date fStartDate;


    protected PropertyChangeListener fMyChildListener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            evt.setPropagationId(DefaultTimeLineModel.this);
            firePropertyChange(evt);
        }
    };

    /**
     *
     * constructor
     *
     * @param interval the number of intervals in a 50 minute time line
     *
     **/
    public DefaultTimeLineModel(int interval) {
        this(0, 50, interval);
    }

    /**
     *
     * constructor
     *
     * @param start the starting minute of the timeline
     * @param end the ending minute of the timeline
     * @param intervals the number of intervals on the timeline
     *
     **/
    public DefaultTimeLineModel(int start, int end, int intervals) {
        this(new Time((double) start, Time.MINUTE), new Time((double) end, Time.MINUTE), intervals);
    }

    /**
     *
     * constructor
     *
     * @param start the starting time of the timeline
     * @param end the ending time of the timeline
     * @param intervals the number of intervals on the timeline
     *
     **/
    public DefaultTimeLineModel(Time start, Time end, int intervals) {
        super();
        fIntervalCount = intervals;
        fStartTime = start;
        fEndTime = end;


        fNodes = Collections.synchronizedList(new ArrayList(5));

        fChangeListeners = Collections.synchronizedList(new ArrayList(5));
    }


    /**
     *
     * add a time line node to the time line without checking its legality
     *
     **/
    public void addTimeLineNode(TimeLineNodeModel node) {
        if (!fNodes.contains(node)) {
            node.setParent(this);
            fNodes.add(node);
            node.addPropertyChangeListener(fMyChildListener);
            Collections.sort(fNodes, sComparator);
            updateExternal();
            firePropertyChange(new PropertyChangeEvent(this, TimeLineModel.NODE_ADDED, null, node));

        }
    }


    /**
     *
     * remove a time line node from the time line
     *
     **/
    public synchronized void removeTimeLineNode(TimeLineNodeModel node) {
        if (fNodes.contains(node)) {
            node.removePropertyChangeListener(fMyChildListener);
            node.setParent(null);
            fNodes.remove(node);
            firePropertyChange(new PropertyChangeEvent(node, NODE_REMOVED, node, null));

        }
    }

    /**
     *
     * remove all time line nodes from time line
     *
     **/
    public void removeAllTimeLineNodes() {
        for (Iterator iter = fNodes.iterator(); iter.hasNext();) {
            TimeLineNodeModel node = (TimeLineNodeModel) iter.next();
            node.removePropertyChangeListener(fMyChildListener);
            iter.remove();
        }
        firePropertyChange(new PropertyChangeEvent(this, ALL_NODES_REMOVED, this, null));
    }


    /**
     *
     * get the number of intervals in the time line
     *
     **/
    public int getIntervalCount() {
        return fIntervalCount;
    }


    /**
     *
     * get an iterator for the time line node models
     *
     **/
    public Iterator getTimeLineNodesIterator() {
        return fNodes.iterator();
    }

    /**
     *
     * get the time line node models in a uneditable list
     *
     **/
    public List getTimeLineNodes() {
        Collections.sort(fNodes, sComparator);
        return Collections.unmodifiableList(fNodes);
    }


    /**
     *
     * add a property change listener to the time line.
     *
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (!fChangeListeners.contains(listener)) {
            fChangeListeners.add(listener);
        }

    }

    /**
     *
     * remove a property change listener from the time line
     *
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        fChangeListeners.remove(listener);
    }


    /**
     *
     * takes a time and converts it into a date where the new date is
     * equal to the start date + the time
     *
     **/
    public Date getDateForTime(Time time) {
        Date result = null;
        if (fStartDate != null) {
            long dateMilliSecs = fStartDate.getTime();
            long timeMilliSecs = (long) (time.getValue(Time.SECOND) * 1000.0);
            result = new Date(dateMilliSecs + timeMilliSecs);
        }
        return result;
    }

    /**
     *
     * takes a date and convert it to a time where the new time is
     * equal to the date - start date
     *
     **/
    public Time getTimeForDate(Date date) {
        Time result = null;
        if (fStartDate != null) {
            double startMilliSecs = (double) fStartDate.getTime();
            double dateMilliSecs = (double) date.getTime();
            result = new Time((dateMilliSecs - startMilliSecs) / 1000.0, Time.SECOND);
        }
        return result;
    }

    /**
     *
     * set the date from which the timeline should start
     *
     *  Note: if the date is not null then all time values are considered offsets from it
     *
     **/
    public void setStartDate(Date date) {
        fStartDate = date;
    }

    /**
     *
     * get the start date
     *
     **/
    public Date getStartDate() {
        return fStartDate;
    }


    /**
     *
     * get the starting value in the timeline
     *
     **/
    public Time getStartTime() {
        return fStartTime;
    }

    /**
     *
     * get the ending value of the timeline
     *
     **/
    public Time getEndTime() {
        return fEndTime;
    }


    /**
     *
     * fires a change event to all listeners of the timeline
     *
     **/
    protected void firePropertyChange(PropertyChangeEvent evt) {
        for (Iterator listIterator = fChangeListeners.iterator(); listIterator.hasNext();) {
            PropertyChangeListener listener = (PropertyChangeListener) listIterator.next();
            if (listener != evt.getSource()) {
                listener.propertyChange(evt);
            }
        }
    }

    /**
     *
     * returns whether or not the model contains the specified node
     *
     */
    public boolean contains(TimeLineNodeModel model) {
        return fNodes.contains(model);
    }

    protected void updateExternal() {
    }

}
