/*
 * $Id: TraceModel.java,v 1.11 2002/05/16 21:20:05 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A model that contains data for a TracePane.
 * 
 * @author John Reekie     (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.11 $
 * @rating Yellow
 */
public class TraceModel {

    /** The collection of Traces
     */
    private HashMap _traces;

    /** The sequence of traces
     */
    private ArrayList _traceArray;

    /** Create a new TraceModel with an unspecified initial capacity.
     */
    public TraceModel () {
        _traces = new HashMap();
        _traceArray = new ArrayList();
    }

    /** Create a new TraceModel with the given initial capacity.
     */
    public TraceModel (int capacity) {
        _traces = new HashMap(capacity);
        _traceArray = new ArrayList(capacity);
    }

    /** Add a new trace
     */
    public void addTrace (Object key, Trace trace) {
        trace._id = _traceArray.size();
        _traces.put(key, trace);
        _traceArray.add(trace);
    }

    /** Remove all data from the model
     */
    public void clear () {
        _traces = new HashMap();
        _traceArray = new ArrayList();
    }

    /** Get the trace at the given key
     */
    public Trace getTrace (String key) {
        return (Trace) _traces.get(key);
    }

    /** Get the trace at the given index
     */
    public Trace getTrace (int index) {
        return (Trace) _traceArray.get(index);
    }

    /** Get the number of traces
     */
    public int size () {
        return _traceArray.size();
    }

    /** Get an iterator over all trace keys
     */
    public Iterator traceKeys () {
        return _traces.keySet().iterator();
    }

    /** Get an iterator over all traces
     */
    public Iterator traces () {
        return _traceArray.iterator();
    }

    /////////////////////////////////////////////////////////////
    //// Trace

    /** The data contained along a single trace
     */
    public static class Trace {

        /** The ID
         */
        private int _id = -1;

         /** The user object of the trace
         */
        private Object _userObject;

        /** The sequence of elements
         */
        private ArrayList _elements;

        /** A unique counter in this trace
         */
        private int _unique = 0;

        /** Create a new Trace with an unspecified initial capacity
         */
        public Trace () {
            _elements = new ArrayList();
        }

        /** Create a new Trace with the given initial capacity
         */
        public Trace (int capacity) {
            _elements = new ArrayList(capacity);
        }

        /** Append an element to the trace. The start time of the 
         * element must be no less than the end of the previous time.
         * The ID of the element will be set to a unique value within
         * this trace.
         */
        public void add (Element elt) {
            // FIXME: Check end time
            _elements.add(elt);
            elt._id = _unique++;
            elt._trace = this;
        }

        /** Get an iterator over all elements
         */
        public Iterator elements () {
            return _elements.iterator();
        }

        /** Get an element at the specified index. FIXME: is this useful?
         */
        public Element get (int index) {
            return (Element) _elements.get(index);
        }

        /** Get the element that overlaps the given time, or null
         * if there isn't one.
         */
        public Element get (double location) {
            // FIXME
            return null;
        }

        /** Get the id of the trace. Once the trace has been
         * added to a model, the id is guaranteed to be unique
         * within that model.
         */
        public int getID() {
            return _id;
        }

        /** Get the user object.
         */
        public Object getUserObject () {
            return _userObject;
        }

        /** Insert an element according to its start time
         * The ID of the element will be set to a unique value within
         * this trace.
         */
        public void insert (Element elt) {
            // FIXME;
            elt._id = _unique++;
            elt._trace = this;
        }

        /** Set the user object.
         */
        public void setUserObject (Object o) {
            _userObject = o;
        }

         /** Truncate the trace by removing all elements less
         * than the given start time. By "less," this means
         * elements that have an end time less than the given start
         * time.
         */
        public void truncate (double startTime) {
            // FIXME
        }

        /** Truncate the trace by removing all elements greater
         * than the given end time.
         */
        public void truncateEnd (double endTime) {
            // FIXME
        }
    }

    /////////////////////////////////////////////////////////////
    //// Element

    /** A single element of a trace
     */
    public static class Element {

        /** Say that the element is open at the start
         */
        public static final int OPEN_START = 4;

        /** Say that the element is open at the end
         */
        public static final int OPEN_END = 8;

        /** The closure status
         */
        public int closure = 0;

        /** The start time
         */
        public double startTime;

        /** The end time
         */
        public double stopTime;

        /** The ID
         */
        private int _id = -1;

        /** The parent trace
         */
        private Trace _trace;

        /** The integer value
         */
        public int intValue;

        /** The object value
         */
        public Object userObject;

        /** Create a new element with no values set.
         */
        public Element () {
            // nothing
        }

        /** Create a new element with the given start time, stop time,
         * and integer value.
         */
        public Element (double startTime, double stopTime, int value) {
            this.startTime = startTime;
            this.stopTime = stopTime;
            this.intValue = value;
        }

         /** Create a new element with the given start time, stop time,
         * integer value, and user object.
         */
        public Element (double startTime, double stopTime, int value,
                        Object o) {
            this.startTime = startTime;
            this.stopTime = stopTime;
            this.intValue = value;
            this.userObject = o;
        }

        /** Get the id of the element. Once the element has been
         * added to a trace, the if is guaranteed to be unique
         * within that trace.
         */
        public int getID() {
            return _id;
        }

        /** Get the trace that owns this element.
         */
        public Trace getTrace() {
            return _trace;
        }

        /** Print a string representation
         */
        public String toString () {
            return super.toString()
                + "["
                + "startTime=" + startTime + ","
                + "stopTime=" + stopTime + ","
                + "intValue=" + intValue + ","
                + "userObject=" + userObject + "]";
        }
    }
}


