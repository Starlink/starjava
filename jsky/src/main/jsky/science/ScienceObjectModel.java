//=== File Prolog =============================================================
//	This code was adapted by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Development History -----------------------------------------------------
//
//	05/03/00	S. Grosvenor / 588 Booz-Allen
//		Original implementation, part of breakdown of old ScienceObject class
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

/**
 * Defines the basic functionality required for a class to interact within the
 * "Science" framework.  This framework was design initially for the Scientist's
 * Expert Assistant (SEA) and provide a common superclass for a set of science-
 * oriented constructs to work together.
 *
 * <P>This code was originally developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 */
public interface ScienceObjectModel extends ReplaceablePropertyChangeListener {

    /**
     * Property name affiliated with the Name property of a ScienceObjectModel.
     * This name is used in events involving the Name property
     */
    public static final String NAME_PROPERTY = "Name".intern();

    /**
     * Property names used by subclasses to indicate a change in a subclass'
     * Validity status.
     * @deprecated - not being used
     */
    public static final String VALID_PROPERTY = "Valid".intern();

    /**
     * Property names used by subclasses to indicate a change in a subclass'
     * Validity status.
     */
    public static final String PENDING_PROPERTY = "Pending".intern();

    /**
     * Returns the parent, if any.  May be null.
     */
    ScienceObjectNodeModel getParent();

    /**
     * Sets the parent of this object. When set property changes will be propagated
     * from child objects to parent objects
     */
    void setParent(ScienceObjectNodeModel model);

    /**
     * Define a clone() without throwing an exception. All implementers of
     * ScienceObjectModel should be able to clone themselves.
     */
    Object clone();

    /**
     * Return the internal "held" state of the object.  Allows objects to
     * put themselves on "hold".  Useful if updating an object requires a
     * time-consuming update process (such as a remote server call).
     * <P>Internally, when holding is set to true a ScienceObjectModel should
     * not perform the update process, but should track whether or not updates
     * are needed.
     * <P>
     * See AbstractScienceObjectNode for an implementation example.
     */
    boolean isHolding();

    /**
     * Sets the hold state for an object.
     */
    void setHolding(boolean hold);

    /**
     * Returns true if this object should have its property change events traced.
     * This is primarily an debugging utility.
     */
    boolean isTracing();

    /**
     * Sets the tracing level for this object.
     * @see #isTracing
     */
    void setTracing(boolean trace); // proposalnavigationpanel

    /**
     * Adds a listener to receive PropertyChangeNotifications and Replacement events.
     */
    void addPropertyChangeListener(ReplaceablePropertyChangeListener listener);

    /**
     * Removes a listener for receiving PropertyChangeNotifications and Replacement events.
     */
    void removePropertyChangeListener(ReplaceablePropertyChangeListener listener);

    /**
     * Removes all listeners.
     */
    void clearAllListeners();

    /**
     * Covering method to fire a property change notification to listeners.
     */
    void firePropertyChange(String propertyName, Object oldValue, Object newValue);

    /**
     * Returns the Name property, should NOT return a null
     */
    String getName();

    /**
     * Sets the Name property
     */
    void setName(String name);

    /**
     * Returns a Label property - may be same as Name or same as toString() or
     * provide a different implementation
     */
    String getLabel();


    /**
     * For validity purposes, a ScienceObjectNode will be "valid" if
     * it has no exceptions assigned to it.  This method should return
     * the exception assocation with this instance if any
     */
    Exception getException();

    /**
     * Sets an exception on the object, presumably making it "invalid"
     */
    void setException(Exception e);

    /**
     * Returns a boolean indicating whether the internal state of the
     * object is in a valid state and its information scientifically sound.
     */
    boolean isValid();

}
