//=== File Prolog =============================================================
//	This code was adapted by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class AbstractScienceObject
//
//--- Description -------------------------------------------------------------
//	AbstractScienceObject provides minimal implementation for the
//  ScienceObjectModel interface.
//
//--- Development History -----------------------------------------------------
//
//	05/03/00	S. Grosvenor / 588 Booz-Allen
//		Original implementation, part of breakdown of old ScienceObject class
//      Code brought in primarily from old event.ReplaceablePropertyChangeSupport
//      and old science.ScienceObject classes.
//
//    10/09/00    S. Grosvenor / 588 Booz-Allen
//      Fixed bugs/inconsistencies in clone().  Also improved tracing on
//      propertychanges and clone messages.
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
//=== End File Prolog====================================================================

package jsky.science;

import jsky.util.ReplaceablePropertyChangeListener;
import jsky.util.ReplacementEvent;
import jsky.util.ReplacementVetoException;
import jsky.util.ReplaceablePropertyVetoException;

import java.beans.PropertyChangeEvent;
import java.util.EventObject;
import java.util.HashMap;

import java.io.Serializable;
import java.io.IOException;

/**
 * Basic implementation of the ScienceObjectModel interface.
 *
 * <P>This code was originally developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 */
public abstract class AbstractScienceObject implements ScienceObjectModel,
        Cloneable,
        Serializable,
        Comparable {

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * name of the object
     */
    private String fName;

    /**
     * true if tracing is on for this object
     */
    private boolean fTracing = false;

    /**
     * point to parent node, may be null
     */
    transient private ScienceObjectNodeModel fParent;

    /**
     * array of listeners to this object
     */
    transient private ReplaceablePropertyChangeListener[] fListeners;


    /**
     * event monitors are for testing purposes only (see junit.SeaTestCase)
     */
    transient private static EventMonitor[] sMonitors;

    /**
     * static array of objects that are currently be traced (@see setTracing())
     */
    transient private static Object[] sTracers = null;

    /**
     * list of last index number for creating default names
     * for new objects.
     */
    transient private static HashMap sClassIndices = null;

    public AbstractScienceObject() {
        this(null);
    }

    public AbstractScienceObject(String inName) {
        super();
        setParent(null);
        fName = inName;
    }

    /**
     * local implementation: returns the name of the object, unless thats null
     * in which case it returns call the superclass (Object)'s toString()
     */
    public String toString() {
        if ((fName == null) || (fName.length() == 0))
            return super.toString();
        else
            return fName;
    }

    /**
     * Returns the parent object in a hierarchy of objects.  May return null.
     */
    public ScienceObjectNodeModel getParent() {
        return fParent;
    }

    /**
     * Sets the parent in the hierarchy.  Note that this does not send
     * out any change notification, to avoid endless looping
     */
    public void setParent(ScienceObjectNodeModel model) {
        fParent = model;
    }

    /**
     * default implement: always returns false.  Subclasses that wish to
     * implement a "pending" capability should override this method
     */
    public boolean isPending() {
        return false;
    }

    /**
     * contains a local exception relevant to a problem in the current state
     * of the object
     */
    private Exception fException;

    /**
     * returns the local exception (if any).  Returns null indicates that there
     * are no current problems with the state of this object
     */
    public Exception getException() {
        return fException;
    }

    /**
     * sets an exception.  This is public, but would normally be used within
     * the object when a problems arises.
     */
    public void setException(Exception e) {
        fException = e;
    }

    /**
     * default implementation: the object is valid if and only if there is
     * no specified local exception
     */
    public boolean isValid() {
        return (fException == null);
    }

    /**
     * default implementation: always returns false, should be overridden by
     * subclasses that want to implement a delayed updating capability
     */
    public boolean isHolding() {
        return false;
    }

    /**
     * default implementation: does nothing
     */
    public void setHolding(boolean hold) {
    }

    /**
     * default implementation: fires superclass (Object)'s clone and suppresses the
     * CloneNotSupportedException, returning null if super.clone() fails
     */
    public Object clone() {
        AbstractScienceObject cl = null;
        try {
            cl = (AbstractScienceObject) super.clone();
            cl.setName(getName());
        }
        catch (CloneNotSupportedException e) {
        }
        return cl;
    }

    /**
     * Default implementation: compares the Names of the objects if they
     * are ScienceObjectModels.  If compared object is not a ScienceObjectModel,
     * returns -1.
     **/
    public int compareTo(Object o) {
        try {
            return getName().compareTo(((ScienceObjectModel) o).getName());
        }
        catch (Exception e) {
            return -1;
        }
    }

    /**
     * Default implementation: checks for same name only
     **/
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AbstractScienceObject)) return false;
        AbstractScienceObject that = (AbstractScienceObject) obj;

        return areNamesEqual(that);
    }

    /**
     * returns the Name property.  Returns empty string if the name is null.
     * Will not return null!
     */
    public String getName() {
        if (fName == null) fName = createDefaultName();
        return ((fName == null) ? "" : fName);
    }

    /**
     * checks equality of the Name property.
     */
    protected boolean areNamesEqual(ScienceObjectModel target) {
        String targName = target.getName();
        if (fName == null && targName != null) return false;
        if (fName != null && targName == null) return false;
        if (fName != null && targName != null && !(fName.equals(targName))) return false;

        return (true);
    }

    /**
     * Creates and returns a default name for an object.  May not be pretty: the default
     * name is the last part of the class name and a numerical suffix.  Should
     * be a unique name within each program run.
     */
    protected String createDefaultName() {
        if (sClassIndices == null) sClassIndices = new HashMap();
        String cName = this.getClass().toString();
        cName = cName.substring(cName.lastIndexOf(".") + 1);
        Integer index = (Integer) sClassIndices.get(cName);
        if (index == null) {
            // first time for this class
            index = new Integer(1);
        }
        else {
            index = new Integer(index.intValue() + 1);
        }
        sClassIndices.put(cName, index);
        return cName + index;
    }

    /**
     * Sets the Name property.
     **/
    public void setName(String inName) {
        String oldName = fName;
        fName = inName;
        firePropertyChange(NAME_PROPERTY, oldName, inName);
    }

    /**
     * Convenience method for storing a "user friendly" name for an object.
     * Should return a "pretty" label for a multi-line pane, something nicer
     * (and non-recursive) than plain old toString().
     * However, in this default implementation, toString() is what you get.
     **/
    public String getLabel() {
        return toString();
    }

    /**
     * Returns the current tracing mode, @see setTracing()
     */
    public boolean isTracing() {
        return (fTracing || sTraceAll);
    }

    private static boolean sTraceAll = false;

    /**
     * Sets a global tracing flag to turn tracing on for all AbstractScienceObjects.
     * @see #setTracing
     */
    public static void setTraceAll(boolean t) {
        sTraceAll = t;
    }

    /**
     * Sets debugging tracing on or off.  This is a debugging feature for developers.
     * When set to true, trace messages will be sent to the debug destination
     * for _every_ call to firePropertyChange or fireReplaceObject.
     * <P>This is particularly useful for tracking down problems in clone() or
     * replaceObject().
     */
    public void setTracing(boolean onOff) {
        if (!sTraceAll) {
            writeDebug(getObjectIdString(), "tracing has been turned " +
                    (onOff? "ON" : "OFF"));
            fTracing = onOff;
        }
    }

    /**
     * Convenience method for cloning.  Useful where subclassed object does not
     * want to have the listener vector "cloned" but rather emptied
     */
    public void clearAllListeners() {
        fListeners = null;
        sTracers = null;
    }

    /**
     * add a listener
     *
     * @param listener  The ReplaceablePropertyChangeListener to be added
     */
    public void addPropertyChangeListener(ReplaceablePropertyChangeListener listener) {
        synchronized (this) {
            ReplaceablePropertyChangeListener[] els = null;
            if (fListeners != null) {
                for (int i = 0; i < fListeners.length; i++) {
                    if (fListeners[i] == listener) return; // already have it, don't add it
                }
                int length = fListeners.length;
                els = new ReplaceablePropertyChangeListener[length + 1];
                System.arraycopy(fListeners, 0, els, 0, length);
            }
            else {
                els = new ReplaceablePropertyChangeListener[1];
            }
            els[els.length - 1] = listener;
            fListeners = els;
        }
    }

    /**
     * Remove a listener
     *
     * @param listener  The ReplaceablePropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(
            ReplaceablePropertyChangeListener listener) {
        if (fListeners == null) return;
        synchronized (this) {
            int length = fListeners.length;
            for (int i = 0; i < fListeners.length; i++) if (fListeners[i] == listener) length--;
            if (length == 0) {
                fListeners = null;
                return;
            }
            ReplaceablePropertyChangeListener[] els = new ReplaceablePropertyChangeListener[length];
            int nexti = 0;
            for (int i = 0; i < fListeners.length; i++) {
                if (fListeners[i] != listener) {
                    els[nexti++] = fListeners[i];
                }
            }
            fListeners = els;
        }
    }

    /**
     * add an event monitor, each monitor will be notified of of all
     * PropertyChangeEvents fired by this object.  Expected to be of use
     * for testing and debugging ONLY
     *
     * @param listener An event monitor to be added
     */
    public static void addEventMonitor(EventMonitor listener) {
        EventMonitor[] els = null;
        if (sMonitors != null) {
            for (int i = 0; i < sMonitors.length; i++) {
                if (sMonitors[i] == listener) return; // already have it, don't add it
            }
            int length = sMonitors.length;
            els = new EventMonitor[length + 1];
            System.arraycopy(sMonitors, 0, els, 0, length);
        }
        else {
            els = new EventMonitor[1];
        }
        els[els.length - 1] = listener;
        sMonitors = els;
    }

    /**
     * Remove a listener
     *
     * @param listener  The ReplaceablePropertyChangeListener to be removed
     */
    public static void removeEventMonitor(EventMonitor listener) {
        if (sMonitors == null) return;

        int length = sMonitors.length;
        for (int i = 0; i < sMonitors.length; i++) if (sMonitors[i] == listener) length--;
        if (length == 0) {
            sMonitors = null;
            return;
        }
        EventMonitor[] els = new EventMonitor[length];
        int nexti = 0;
        for (int i = 0; i < sMonitors.length; i++) {
            if (sMonitors[i] != listener) {
                els[nexti++] = sMonitors[i];
            }
        }
        sMonitors = els;
    }

    public void notifyEventMonitors(Object target, java.util.EventObject event) {
        if (sMonitors != null) {
            for (int i = 0; i < sMonitors.length; i++) {
                sMonitors[i].eventFired(this, target, event);
            }
        }
    }

    /**
     * default implementation. Propogates propertyChanges to all listeners.
     * This is a basic component of the hierarchy of changes events supported
     * by AbstractScienceObject.
     */
    public void propertyChange(PropertyChangeEvent ev) {
        if (!ev.getPropertyName().equals(NAME_PROPERTY)) {
            firePropertyChange(getName() + "." + ev.getPropertyName(), ev.getOldValue(), ev.getNewValue());
        }
    }

    /**
     * Convenience debugging method for classes that do not support replaceObject() but
     * find it has been called anyway.
     * Displays a message indicating that the replaceObject() method has not yet been
     * "implemented".  Should be called by Objects who want to postpont defining replaceObject
     * but gives centralize message and tracking ability
     **/
    public static void replaceObjectNYI(Object source, ReplacementEvent event) {
        System.err.println("[ERROR] " + source +
                ": Unimplemented replaceObject call, oldv=" +
                getObjectIdString(event.getOldValue()) +
                " newv=" + getObjectIdString(event.getNewValue())
        );
    }

    /**
     * Default implementation.  Does nothing
     **/
    public void replaceObject(ReplacementEvent ev)
            throws ReplacementVetoException {
        // do nothing
    }

    /**
     * Report a bound property update to any registered listeners.
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    public void firePropertyChange(String propertyName,
                                   Object oldValue, Object newValue) {
        firePropertyChange(propertyName, oldValue, newValue, isTracing());
    }

    /**
     * tracks indentation in recursive sequence of debug messages
     */
    private static String debugIndent = "";

    /**
     * tracks indentation in recursive sequence of debug messages
     */
    private static String replaceIndent = "";

    /**
     * Report a bound property update to any registered listeners.
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    public void firePropertyChange(String propertyName,
                                   Object oldValue, Object newValue, boolean trace) {
        if (oldValue != null && oldValue.equals(newValue)) return;
        if (oldValue == null && newValue == null) return;

        ReplaceablePropertyChangeListener[] localListeners = fListeners;
        if (localListeners == null) return;

        PropertyChangeEvent evt = new PropertyChangeEvent(
                this,
                propertyName, oldValue, newValue);

        if (trace) {
            writeDebug(debugIndent + getObjectIdString(this) + ".firepropertyChange", propertyName);
            debugIndent = debugIndent + "  ";
        }

        for (int i = 0; i < localListeners.length; i++) {
            ReplaceablePropertyChangeListener target = localListeners[i];

            if (trace) writeDebug(debugIndent, getObjectIdString(target));

            try {
                notifyEventMonitors(target, evt);
                target.propertyChange(evt);
            }
            catch (ReplaceablePropertyVetoException ex) {
                writeError(this,
                        "Unexpected ReplaceablePropertyVetoException returned and ignored, " +
                        ex.toString());
            }
        }
        if (trace) debugIndent = debugIndent.substring(2);
    }

    /**
     * Report a bound property update to any registered listeners.
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    public void fireVetoableChange(String propertyName,
                                   Object oldValue, Object newValue)
            throws ReplaceablePropertyVetoException {
        fireVetoableChange(propertyName, oldValue, newValue, isTracing());
    }

    /**
     * Report a bound property update to any registered fListeners.
     * If any listener throws a ReplaceablePropertyVetoException, then this
     * method will "undo" the property change notifications and throw
     * the exception back to the original caller
     *
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     * @throws ReplaceablePropertyVetoException  passed on if received from a listener
     */
    public void fireVetoableChange(String propertyName,
                                   Object oldValue, Object newValue, boolean trace)
            throws ReplaceablePropertyVetoException {
        Object eSource = this;

        if (oldValue != null && oldValue.equals(newValue)) return;
        if ((oldValue == null) && (newValue == null)) return;

        ReplaceablePropertyChangeListener[] localListeners = fListeners;
        if (localListeners == null) return;

        PropertyChangeEvent evt = new PropertyChangeEvent(eSource,
                propertyName, oldValue, newValue);

        for (int i = 0; i < localListeners.length; i++) {
            ReplaceablePropertyChangeListener target = localListeners[i];
            if (trace) {
                writeDebug("    calling",
                        getObjectIdString(target) + ".propertyChange");
            }
            try {
                target.propertyChange(evt);
            }
            catch (ReplaceablePropertyVetoException ex) {
                PropertyChangeEvent reverseEvent = new PropertyChangeEvent(eSource,
                        propertyName, newValue, oldValue);
                for (int i2 = i - 1; i2 >= 0; i2--) {
                    ReplaceablePropertyChangeListener reverseTarget = localListeners[i2];
                    if (trace) {
                        writeDebug("    reversing",
                                getObjectIdString(reverseTarget) + ".propertyChange");
                    }
                    try {
                        reverseTarget.propertyChange(reverseEvent);
                    }
                    catch (ReplaceablePropertyVetoException ex2) {
                        writeError(this,
                                "Unexpected ReplaceablePropertyVetoException returned and ignored, " +
                                ex2.toString());
                    }
                }
                throw ex;
            }
        }
    }

    /**
     * Report a replaceObject event to any registered ReplaceablePropertyChangeListeners
     * that are also ReplaceablePropertyChangeListeners
     * No events are sent if the old and new objects are '=='. It does not
     * matter if they are .equals() true
     *
     * @param oldObject  The object to be replaced by a new object
     * @param newObject  The new object to replace the current object
     */
    public void fireReplaceObject(Object oldObject, Object newObject)
            throws ReplacementVetoException {
        fireReplaceObject(oldObject, newObject, isTracing());
    }

    /**
     * Report a replaceObject event to any registered propertyChangelisteners
     * that are also ReplaceablePropertyChangeListeners
     * No events are sent if the old and new objects are '=='. It does not
     * matter if they are .equals() true
     *
     * @param newObject  The new object to replace the current object
     */
    public void fireReplaceObject(ReplaceablePropertyChangeListener newObject)
            throws ReplacementVetoException {
        fireReplaceObject(this, newObject, isTracing());
    }

    /**
     * Report a replaceObject event to any registered ReplaceablePropertyChangeListeners
     * that are also ReplaceablePropertyChangeListeners
     * No events are sent if the old and new objects are '=='. It does not
     * matter if they are .equals() true
     *
     * @param newObject  The new object to replace the current object
     */
    public void fireReplaceObject(Object oldObject, Object newObject, boolean trace)
            throws ReplacementVetoException {
        if (oldObject == newObject) return;
        if (oldObject == null && newObject == null) return;

        ReplaceablePropertyChangeListener[] localListeners = fListeners;
        if (localListeners == null) return;
        ReplaceablePropertyChangeListener target = null;

        int i = 0; // so is accessible in catch phrase
        if (trace) {
            writeDebug(replaceIndent + getObjectIdString(this) + ".fireReplaceObject",
                    "old=" + getObjectIdString(oldObject) + ",new=" + getObjectIdString(newObject));
            replaceIndent = replaceIndent + "  ";
        }

        try {
            ReplacementEvent evt = new ReplacementEvent(oldObject, newObject);

            for (i = 0; i < localListeners.length; i++) {
                target = localListeners[i];
                if (trace) writeDebug(replaceIndent, getObjectIdString(target));
                notifyEventMonitors(target, evt);
                target.replaceObject(evt);
            }
        }
        catch (ReplacementVetoException ex) {
            writeDebug(this,
                    "ReplacementVetoException trapped in replaceObject, from " +
                    target.toString());

            try {
                ReplacementEvent reverseEvt = new ReplacementEvent(newObject, oldObject);
                for (int j = i - 1; j >= 0; j--) {
                    target = localListeners[j];
                    if (target instanceof ReplaceablePropertyChangeListener) {
                        notifyEventMonitors(target, reverseEvt);
                        ((ReplaceablePropertyChangeListener) target).replaceObject(reverseEvt);
                    }
                }
            }
            catch (ReplacementVetoException ex2) {
                // ignore it
            }
            throw ex;
        }
        if (trace) replaceIndent = replaceIndent.substring(2);
    }

    /**
     * Indicates whether or not debugging messages should be generated
     */
    private static boolean showDebug = false;

    /**
     * default implementation.  Sends a formatted DEBUG message to System.err
     */
    protected void writeDebug(Object source, Object message) {
        if (showDebug) System.err.println("[DEBUG] " + source + ": " + message);
    }

    /**
     * default implementation.  Sends a formatted ERROR message to System.err
     */
    protected void writeError(Object source, Object message) {
        System.err.println("[ERROR] " + source + ": " + message);
    }

    /**
     * Useful for debugging output, returns a String containing the class and hashcode of
     * the specified object.
     **/
    public String getObjectIdString() {
        return getObjectIdString(this);
    }

    /**
     * Useful for debugging output, returns a String containing the class and hashcode of
     * the specified object.
     **/
    public static String getObjectIdString(Object obj) {
        String retval;
        if (obj == null) {
            retval = "<null>";
        }
        else {
            retval = obj.getClass().toString() + "@" + Integer.toHexString(obj.hashCode());
            String gov = "class ";
            int idx = gov.length();
            if (retval.indexOf(gov) >= 0)
                retval = retval.substring(idx, retval.length());
        }
        return retval;
    }


    public interface EventMonitor {

        void eventFired(Object from, Object to, java.util.EventObject event);
    }


}




