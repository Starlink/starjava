//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	05/03/00	S. Grosvenor / 588 Booz-Allen
//		Original implementation, part of breakdown of old ScienceObject class
//      Code brought in primarily from old science.ScienceObject class.
//
//    10/09/00    S. Grosvenor / 588 Booz-Allen
//      Fixed bugs/inconsistencies in clone().  Also improved tracing on
//      propertychanges and clone messages.
//
//  10/10/00 S Grosvenor / 588 Booz-Allen
//      added back in a .equals() check on Node's children.
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
//=== End File Prolog=======================================================

package jsky.science;

import java.lang.*;
import java.util.*;
import java.io.*;

import java.beans.PropertyChangeEvent;

import jsky.util.ReplacementEvent;
import jsky.util.ReplacementVetoException;

/**
 * Basic implementation of the ScienceObjectNodeModel.
 *
 * <P>This code was originally developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 **/
public abstract class AbstractScienceObjectNode extends AbstractScienceObject
        implements ScienceObjectNodeModel {

    /**
     * Whether or not more data is available from the data source.
     **/
    private boolean fMoreDataAvailable;

    /**
     * The source of the contents of the ScienceObjectModel.
     **/
    private ScienceObjectNodeModel fDataSource;

    /**
     * List of ScienceObjectModel children contained within this object.
     **/
    private List fChildren;

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * True when delayed updating is in effect
     **/
    private boolean fHolding = false;

    /**
     * True when delayed updating is in effect AND changes are "stacked up"
     **/
    private boolean fUpdateNeeded = false;

    /**
     * True while an object is in the update process
     **/
    private boolean fPending = false;

    /**
     * Creates new AbstractScienceObjectNode with blank name and holding turned off.
     */
    public AbstractScienceObjectNode() {
        this(null, false);
    }

    /**
     * creates a new AbstractScienceObjectNode with specified name and specified initial
     * holding status.
     * <P>
     * Subclasses will often initialize with holding=true in order to
     * finish populating the object before trying to update it.
     */
    public AbstractScienceObjectNode(String inName, boolean holding) {
        super(inName);

        fHolding = holding;
        fUpdateNeeded = holding;
        fChildren = new ArrayList(5);
        fMoreDataAvailable = false;
        fDataSource = null;

        initializeTransients();
    }

    /**
     * Creates new AbstractScienceObjectNode with blank name and specified holding state.
     **/
    public AbstractScienceObjectNode(boolean holding) {
        this(null, holding);
    }

    /**
     * Creates new AbstractScienceObjectNode with a name and holding turned off.
     */
    public AbstractScienceObjectNode(String inName) {
        this(inName, false);
    }

    protected static String cloneIndent = "";

    /**
     * Clones this object, and does a DEEP clone on the fChildren.
     * <P>
     * NOTE: for subclasses! If a subclass has a separate variable or list that
     * points at fChildren, remember that after this clone() method is completed
     * all fChildren will have also been cloned.
     * <P>  So subclasses may need to "repoint" variables or elemenets of lists that
     * reference child objects, but should NOT re-clone those children.
     * <p>See
     * ExposureGroup.clone() and Exposure.clone() as examples of classes that
     * need to re-proint existing pointers to correctly point at the correct elements.
     */
    public Object clone() {
        AbstractScienceObjectNode newSO = (AbstractScienceObjectNode) super.clone();

        // clone this's children into NewSO's child
        newSO.clearAllListeners();

        if (isTracing()) {
            writeDebug(cloneIndent +
                    getObjectIdString(this) + ".clone",
                    " to " + getObjectIdString(newSO)
            );
            cloneIndent = cloneIndent + "  ";
        }

        newSO.fChildren = new ArrayList(5);

        // now clone all the kids and repoint newSO's fchildren to the new clones
        for (Iterator iter = this.fChildren.iterator(); iter.hasNext();) {
            ScienceObjectModel thisChild = (ScienceObjectModel) iter.next();
            ScienceObjectModel thatChild = (ScienceObjectModel) thisChild.clone();
            newSO.addChild(thatChild);
        }

        if (isTracing()) cloneIndent = cloneIndent.substring(2);
        return newSO;
    }

    /**
     * Looks up the index of a child in the children's list, matching on exact
     * equality (==) NOT on equals().
     * If match is not found, returns -1, otherwise returns
     * the index of the child in the list of children.  This differs from
     * a simple fChildren.indexOf( object) which test for .equals()
     *
     * @param child Child object to be located in the the children list
     * @return index of child or -1 of child is not in the list
     **/
    public int indexOfChild(Object child) {
        for (int childIndex = 0; childIndex < fChildren.size(); childIndex++) {
            Object next = fChildren.get(childIndex);
            if (next == child) {
                return (childIndex);
            }
        }
        return (-1);
    }

    /**
     * Copies a ScienceObjectModel and assigns it a new "Copy of" name.
     * The actual copying is deffered to the object's clone() method.
     *
     * @return	clone of the ScienceObjectModel, but with a unique name
     **/
    public ScienceObjectModel namedClone() {
        AbstractScienceObjectNode copy = (AbstractScienceObjectNode) clone();
        copy.setName(createDefaultName());

        /*
        String name = copy.getName();
        if (name.startsWith("Copy of "))
        {
            copy.setName("Copy (2) of " + name.substring(8));
        }
        else if (name.startsWith("Copy ("))
        {
            // extract copy number
            try
            {
                int copynumber = Integer.valueOf(name.substring(6, name.indexOf(")"))).intValue();
                copy.setName("Copy (" + (copynumber + 1) + ") of " + name.substring(name.indexOf(" of ") + 4));
            }
            catch (NumberFormatException ex)
            {
                copy.setName("Copy (?) of " + name.substring(name.indexOf(" of ") + 4));
            }
        }
        else
        {
            copy.setName("Copy of " + name);
        }
        */

        return copy;
    }

    /**
     * Adds equals() checks on the object's DataSources and Children
     **/
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        if (!(obj instanceof AbstractScienceObjectNode)) return false;
        AbstractScienceObjectNode that = (AbstractScienceObjectNode) obj;

        if ((fDataSource == null) ? (that.fDataSource != null) : !(fDataSource.equals(that.fDataSource))) return false;
        if ((fChildren == null) ? (that.fChildren != null) : !(fChildren.equals(that.fChildren))) return false;

        return true;
    }

    /**
     * Process a request from one of a ScienceObjectModel's children to "replace"
     * itself with another object.
     **/
    public void replaceObject(ReplacementEvent ev)
            throws ReplacementVetoException {
        ScienceObjectModel oldObject = (ScienceObjectModel) ev.getOldValue();
        ScienceObjectModel newObject = (ScienceObjectModel) ev.getNewValue();
        if (oldObject == newObject) return;

        if (oldObject == this) {
            // don't update myself while i'm in limbo
            setHolding(true);

            // request to swap myself out... do this by tell firereplaceobjects on
            // my kids with the newguys' kids
            // NOTE NOTE am assuming that tree of kids is same order

            try {
                // NOTE also to get here, both old and new objects should be SO-NODES!
                Iterator oldKids = getChildren().iterator();
                Iterator newKids = ((ScienceObjectNodeModel) newObject).getChildren().iterator();
                while (oldKids.hasNext() && newKids.hasNext()) {
                    ScienceObjectModel oldC = (ScienceObjectModel) oldKids.next();
                    // I dont want to hear from this kid anymore
                    oldC.removePropertyChangeListener(this);
                    oldC.replaceObject(new ReplacementEvent(oldC,
                            (ScienceObjectModel) newKids.next()));
                }
            }
            catch (ClassCastException ee) {
            } // ok wasnt a node, cant have kids, no problem

            // now tell my listeners that I'm being replaced
            try {
                fireReplaceObject(this, newObject);
            }
            catch (ReplacementVetoException e) {
                writeError(this,
                        "Unexpected exception: " + e.toString());
            }
        }
        else {
            // see if request comes from one of my kids
            // if so, swap out the listeners and I'm done
            for (int index = 0; index < fChildren.size(); index++) {
                if (fChildren.get(index) == oldObject && newObject != oldObject) {
                    fChildren.set(index, newObject);
                    oldObject.removePropertyChangeListener(this);
                    newObject.addPropertyChangeListener(this);
                    newObject.setParent(this);
                }
            }
        }
    }

    /**
     * Prints a reasonably user-friendly description of the object to the specified
     * PrintWriter.  This is performed recursiviely through each of the child
     * objects.
     * @param pw The PrintWriter to which to send the output
     * @param indent A preliminary string (likely all blanks) to pre-pend to
     * each line of output.  Allows recursive calls to an object's children
     * to indent their output.
     */
    public void saveAsText(PrintWriter pw, int indent) {
        // in case someone forgot to implement this themselves
        String id = repeat(" ", indent);
        pw.println(id + toString());
    }

    /**
     * Support method for saveAsText. 
     * Concatenates the specified input string <pre>n</pre> times.
     */
    public static String repeat(String inS, int n) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < n; i++) {
            sb.append(inS);
        }
        return sb.toString();
    }

    /**
     * Single argument varient.  Use initial indentation of 0
     */
    public void saveAsText(PrintWriter pw) {
        saveAsText(pw, 0);
    }

    /**
     * Returns true if more data is available for this science object.
     **/
    public boolean isMoreDataAvailable() {
        return fMoreDataAvailable;
    }

    /**
     * Sets whether or not more data is available for this science object from its
     * DataSource.
     **/
    public void setMoreDataAvailable(boolean more) {
        boolean old = fMoreDataAvailable;
        fMoreDataAvailable = more;

        firePropertyChange(MORE_DATA_PROPERTY, new Boolean(true), new Boolean(false));
    }

    /**
     * Attempts to retrieve more information for the specified ScienceObjectModel and
     * populate the ScienceObjectModel with that extra data.  This only makes sense
     * for ScienceObjects where isMoreDataAvailable() is true.  This method does
     * nothing in AbstractScienceObjectModelNode, but can be defined in subclasses.
     *
     * @param	forObject	retrieve more data for this science object
     **/
    public void retrieveMoreData(ScienceObjectNodeModel forObject) {
        // DataSource class does nothing, so just clear the moredata field since
        // no more data is available.  Subclasses would define this method to
        // retrieve more data.
        forObject.setMoreDataAvailable(false);
    }

    /**
     * Requests that the science object retrieve any extra data that it has
     * available from its data source.
     **/
    public void requestMoreData() {
        if (!fMoreDataAvailable) {
            return;
        }

        if (fDataSource != null) {
            fDataSource.retrieveMoreData(this);
        }
    }

    /**
     * Returns an object that describes the source of the ScienceObjectModel's data.
     **/
    public ScienceObjectNodeModel getDataSource() {
        return fDataSource;
    }

    /**
     * Sets the source of the ScienceObjectModel's data.
     **/
    public void setDataSource(ScienceObjectNodeModel s) {
        ScienceObjectNodeModel old = fDataSource;
        fDataSource = s;

        firePropertyChange(DATA_SOURCE_PROPERTY, old, fDataSource);
    }

    /**
     * Returns true if updates to the object would be "held".
     *
     * @see #processUpdates
     */
    public boolean isHolding() {
        return fHolding;
    }

    /**
     * Sets the "holding" property.
     *
     * @param inH
     * @see #processUpdates
     */
    public void setHolding(boolean inH) {
        fHolding = inH;

        if (!inH && fUpdateNeeded) {
            update();
        }
    }

    /**
     * Sets the flag that indicates whether or not updates to the object are needed.
     *
     * @param inH
     * @see #processUpdates
     */
    public void setUpdatesPending(boolean inH) {
        fUpdateNeeded = inH;
    }

    /**
     * Fires off request to update source and background counts.  If the object's
     * hold status is true, this will set a flag indicating that updates are
     * needed.  If the hold status is false, then the update process begins
     *
     * Note: subclasses should NOT override update().  Instead they should implement/override
     * processUpdates();
     *
     * @see #processUpdates
     */
    protected synchronized void update() {
        if (fHolding) {
            fUpdateNeeded = true;
        }
        else {
            setPending(true);
            processUpdates();
            fUpdateNeeded = false;
        }
    }

    /**
     * Called when updates to a ScienceObject is processed. Works with
     * isHolding(), setHolding(), setUpdatesPending(), and update() to provide
     * to delay a possibly lengthy update process for an object.
     * <p>In the default implementation, this method is empty, and should be overridden
     * by classes that want to be able to hold updates.
     * <P>
     * To use this feature in a subclass of ScienceObject:
     * - Use setHolding() to turn delaying of updates on/off.<br>
     * - Override processUpdates() to contain the actual code that performs
     * the updating of an object. Remember that processUpdates() or the subclass is
     * responsible for setting Pending back to false.
     * - Call update() to request that updates be made to an object. Subclasses
     * should NOT override this method, it will call processUpdates() if holding is
     * off.  If holding is on, update() will set a flag so that processUpdates() will
     * be called as soon as a setHolding(false) is received.
     * - use setUpdatesPending() to manually override the setting of the pending flag.  This
     * is rarely used in subclasses as update() and setHolding() normally manage the pending flag.
     *
     * <P>
     * Subclasses should not call processUpdates() directly.
     */
    protected void processUpdates() {
        // should be overridden to do something if subclass
        // has updates that might be held

        // if subclasses remove this setPending(false), then the subclass takes over
        // responsiblity for setting pending off when appropriate
        setPending(false);
    }

    /**
     * Returns true if the object is up-to-date, false otherwise.
     * At this level it returns true when isHolding() is false, or when
     * isHolding() is true, but there are no updates pending.
     */
    public boolean isUpToDate() {
        if (!isHolding()) {
            return true;
        }
        else {
            return fUpdateNeeded;
        }
    }

    /**
     * Returns true if the object is "valid".  At this level, always returns true,
     * although subclasses may override this
     **/
    public boolean isValid() {
        return true;
    }

    /**
     * Returns true when the object is in the process of performing an update.
     **/
    public boolean isPending() {
        return fPending;
    }

    /**
     * Sets the pending state.  Should rarely be needed outside of the update() method.
     **/
    protected void setPending(boolean b) {
        if (fPending == b) return;
        Boolean hold = new Boolean(fPending);
        fPending = b;
        firePropertyChange(ScienceObjectModel.PENDING_PROPERTY, hold, new Boolean(fPending));
    }

    /**
     * Turns on/off the tracing of propertychange handling in an object;
     */
    public void setTracing(boolean onOff) {
        super.setTracing(onOff);
        writeDebug(
                getObjectIdString(this),
                "tracing has been turned " +
                (onOff? "ON" : "OFF"));
    }

    /**
     * Sets default values for all transient fields.
     **/
    protected void initializeTransients() {
        setParent(null);
    }

    /**
     * Reconstructs the object properly during deserialization.
     **/
    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        // Read all the non-transient fields
        stream.defaultReadObject();

        // Set default values for transient fields
        initializeTransients();

        // Re-add self as listener and parent to all the children
        Iterator iter = fChildren.iterator();
        while (iter.hasNext()) {
            ScienceObjectModel obj = (ScienceObjectModel) iter.next();
            obj.addPropertyChangeListener(this);
            obj.setParent(this);
        }
    }

    /**
     * Adds a ScienceObject as a "child" of the current object.
     * Automatically will handle propertyChange listending, cloning
     * and loading/writing of all official children.
     *
     * @param newKid    ScienceObjectModel to be added
     *
     **/
    public void addChild(ScienceObjectModel newKid) {
        newKid.addPropertyChangeListener(this);

        newKid.setParent(this);
        for (int i = 0; i < fChildren.size(); i++) {
            if (fChildren.get(i) == newKid) return;
        }
        // if we get here then it does not exist
        fChildren.add(newKid);
    }

    /**
     * Removes all occurrences of a ScienceObjectModel as a "child" of the current object.
     * Automatically will handle removing listeners
     *
     * @param oldKid    ScienceObjectModel to be added
     * @return the ScienceObjectModel just removed, or null if oldKid not a valid child
     *
     **/
    public ScienceObjectModel removeChild(ScienceObjectModel oldKid) {
        if (oldKid != null) {
            for (int index = 0; index < fChildren.size(); index++) {
                if (fChildren.get(index) == oldKid) {
                    fChildren.remove(index++);
                    oldKid.removePropertyChangeListener(this);
                    oldKid.setParent(null);
                    return oldKid;
                }
            }
        }
        return null;
    }

    /**
     * Removes all children from the ScienceObjectModel.
     **/
    public void removeAllChildren() {
        synchronized (fChildren) {
            // Create copy of children vector because removeChild will modify original
            Object[] copy = fChildren.toArray();

            // Call removeChild() on each child, which will remove listeners as well
            for (int i = 0; i < copy.length; i++) {
                removeChild((ScienceObjectModel) copy[i]);
            }
        }
    }

    /**
     * Replaces all occurrence of the "old" child with the new child
     * in the propertychange listings.
     *
     * @param oldKid    ScienceObjectModel to be replaced
     * @param newKid    new ScienceObjectModel to be "added"
     * @return the ScienceObjectModel just removed, or null if oldKid not a valid child
     *
     **/
    public ScienceObjectModel replaceChild(ScienceObjectModel oldKid, ScienceObjectModel newKid) {
        if (oldKid != null) {
            for (int index = 0; index < fChildren.size(); index++) {
                if (fChildren.get(index) == oldKid) {
                    fChildren.set(index, newKid);
                    oldKid.removePropertyChangeListener(this);
                    oldKid.setParent(null);

                    if (newKid != null) {
                        newKid.addPropertyChangeListener(this);
                        newKid.setParent(this);
                    }
                    return oldKid;
                }
            }
        }
        // if we get here, the oldKid didnt exist, so we had new kid
        if (newKid != null) addChild(newKid);

        return null;
    }

    /**
     * returns List of children of the science object
     * should be used only when necessary
     **/
    public List getChildren() {
        return fChildren;
    }


}

