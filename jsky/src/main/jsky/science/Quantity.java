//=== File Prolog========================================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 587
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes------------------------------------------------------------------------------
//
//--- Development History----------------------------------------------------------------
//    Date              Author          Reference
//    6/1/99          S.Grosvenor
//      Initial packaging of class
//    05/03/00    S. Grosvenor / 588 Booz-Allen
//      ScienceObject overhaul
//    06/30/00      S. Grosvenor
//      converted to jsky, turned vectors to lists
//
//  10/10/00    S.  Grosvenor / 588 Booz Allen
//
//      Changed to descend from AbstractScienceObject not the more-complex,
//      higher overhead AbstractScienceObjectNode
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

import jsky.util.FormatUtilities;

/**
 * Quantity, abstract super class to match values and units and easily allow
 * developers to manage the units a quantity-style value.  This class also
 * provides infrastructure for creating new types of quantity values and/or
 * new sets of units.
 * <P>
 * This class mixes a static master list of quantity types and their affiliated
 * units.  Subclasses (such as <code>Time</code>) can easily be created that
 * provide a set of units for a different type of quantity.
 * <P>
 * The Quantity class also works with the QuantityPanel GUI component to provide
 * a text entry for a unit that automatically tracks, displays and converts
 * values into a user-settable application wide default unit.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * See <code>Time</code> or <code>Wavelength</code> for an examples of quantity
 * subclasses.
 *
 * @version 	2000.11.29
 * @author 	    Sandy Grosvenor
 **/
public abstract class Quantity extends AbstractScienceObject {

    /**
     * internal holder for quantity amount
     */
    protected double fValue;

    /**
     * Map containing
     * listeners for each defined subclass that are to be notified when a subclass's default units are changed.
     * Map contains one element per subclass, where the subclass is the key and the
     * listener objects are Lists
     */
    private static Map sListeners = new HashMap();

    /**
     * Map containing
     * list of unit long names defined for each defined subclass.
     * Map contains one element per subclass, where the subclass is the key and the
     * units objects are Lists
     */
    private static Map sUnits = new HashMap();

    /**
     * Map containing
     * list of unit abbreviates defined for each defined subclass.
     * Map contains one element per subclass, where the subclass is the key and the
     * units objects are Lists
     */
    private static Map sAbbrev = new HashMap();

    /**
     * Map containing
     * current default units defined for each defined subclass.
     * Map contains one element per subclass, where the subclass is the key and the
     * units objects are Strings
     */
    private static Map sDefaults = new HashMap();

    /**
     * Map containing
     * bound property names defined for each defined subclass.
     * Map contains one element per subclass, where the subclass is the key and the
     * objects are Strings containing the property name to be fired when
     * the default units change.
     */
    private static Map sUnitsProperties = new HashMap();

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * Returns the quantity value in specified units.  Must be defined by
     * subclasses and should support all units supported by the subclass
     **/
    public abstract double getValue(String units);

    /**
     * Returns a new instance an object with same value as creating instance.
     * <P>Note this method is only expected to be called by
     * QuantityPanel.actionPerformed()
     **/
    public abstract Quantity newInstance(double inValue);

    /**
     * Sets the value of the Quantity in specified units
     **/
    protected abstract void setValue(double inValue, String inUnits);

    /**
     * Called by static initializer of subclasses to initialize a new quantity.
     * @param cl The class of the Quantity subclass
     * @param unitNames  a List of long names of the supported units
     * @param abbrevStrings a List of abbreviations of the supported units (must
     *  be same length and order as the unitNames
     * @param defaultUnits the unit that is the starting default unit
     * @param defaultUnitsChangeProperty the bound propertyName to be specified
     * in PropertyChangeEvents when the default units are changed
     **/
    public static void initializeSubClass(Class cl,
                                          List unitNames,
                                          List abbrevStrings,
                                          String defaultUnits,
                                          //String defaultValueChangeProperty,
                                          String defaultUnitsChangeProperty) {
        if (sUnits.get(cl) != null) {
            // kill old entries
            sUnits.remove(cl);
            sAbbrev.remove(cl);
            sDefaults.remove(cl);
            sListeners.remove(cl);
            sUnitsProperties.remove(cl);
            //sValueProperties.remove( cl);
        }
        sUnits.put(cl, unitNames);
        sAbbrev.put(cl, abbrevStrings);
        sDefaults.put(cl, defaultUnits);
        sListeners.put(cl, new HashSet(10));
        sUnitsProperties.put(cl, defaultUnitsChangeProperty);
        //sValueProperties.put( cl, defaultValueChangeProperty);
    }

    /**
     * returns true if the a specified Class has been initialized as a Quantity
     * subclass
     */
    public static boolean isInitialized(Class thisClass) {
        return (sUnits.get(thisClass) != null);
    }

    /**
     * Returns the current default units for the specified Class
     **/
    public static String getDefaultUnitsAbbrev(Class cl) {
        String def = getDefaultUnits(cl);
        return getUnitsAbbrev(cl, def);
    }

    /**
     * Returns the abbreviation for the specified Units type.
     * @param cl Class reference to the desired Quantity subclass
     * @param def the long name of the units for which the abbreviation is desired.
     **/
    public static String getUnitsAbbrev(Class cl, String def) {
        if (def == null) return null;

        List units = (List) sUnits.get(cl);
        int index = units.indexOf(def);

        if (index >= 0) {
            List abbrev = (List) sAbbrev.get(cl);
            return (String) abbrev.get(index);
        }
        else {
            return null;
        }
    }

    /**
     * Returns the current default units for the specified Quantity subclass
     **/
    public static String getDefaultUnits(Class cl) {
        return (String) sDefaults.get(cl);
    }

    /**
     * Returns the current default propertyName for the specified Quantity subclass
     **/
    public static String getDefaultUnitsProperty(Class cl) {
        return (String) sUnitsProperties.get(cl);
    }

    /**
     * Sets the default units, for the specified Quantity subclass
     **/
    public static void setDefaultUnits(Class cl, String inUnits) {
        String oldDefault = (String) sDefaults.get(cl);
        if (oldDefault == null || !oldDefault.equals(inUnits)) {
            sDefaults.remove(cl);
            sDefaults.put(cl, inUnits);

            PropertyChangeEvent evt = new PropertyChangeEvent(
                    cl.toString(), (String) sUnitsProperties.get(cl), oldDefault, inUnits);
            fireDefaultUnitsChange(cl, evt);
        }
    }


    /**
     * Returns the value of the an instance as a <code>double</code> in the
     * current default units
     */
    public double getValue() {
        return getValue(getDefaultUnits());
    }

    /**
     * Returns the name of the current default units
     */
    public String getDefaultUnits() {
        return (String) sDefaults.get(this.getClass());
    }

    /**
     * returns a string displaying the current value in the default units
     * to 2 decimal places of accuracy.  @see toString( decs)
     */
    public String toString() {
        return toString(2);
    }

    /**
     * formats the current quantity value in the default units to the specfied decimal accuracy.
     * Scientific notation may be used if necessary.
     */
    public String toString(int decs) {
        return FormatUtilities.formatDouble(getValue(), decs) + " " + getDefaultUnitsAbbrev(this.getClass());
    }

    /**
     * compares to Quantities for equality.
     */
    public boolean equals(Object that) {
        if (that == this) return true;
        if (that == null) return false;

        if (!(that instanceof Quantity)) return false;

        return (this.fValue == ((Quantity) that).fValue);
    }

    /**
     * sets the hashcode value to the <code>int</code> of the value
     */
    public int hashCode() {
        return (int) fValue;
    }

    /**
     * Returns the list of all units for specfied Quantity subclass
     *
     * @param cl Quantity subclass
     */
    public static List getAllUnits(Class cl) {
        return (List) sUnits.get(cl);
    }

    /**
     * Returns the list of string labels for this instance's class
     */
    public List getAllUnits() {
        return (List) sUnits.get(this.getClass());
    }

    /**
     *
     * Returns the list of abbreviations for specified Quantity subclass
     *
     **/
    public static List getAllUnitsAbbrev(Class cl) {
        return (List) sAbbrev.get(cl);
    }

    /**
     *
     * Returns the list of abbreviations for this instance's class
     *
     **/
    public List getAllUnitsAbbrev() {
        return (List) sAbbrev.get(this.getClass());
    }

    /**
     *
     * Does a case-insensitve search through the list of valid units for
     * a class and returns the proper casing for the specified units
     *
     * @param cl the subclass of Quantity to be searched
     * @param inUnits the units string to be searched for
     *
     **/
    public static String getUnitsIgnoreCase(Class cl, String inUnits) {
        Iterator iter = ((List) sUnits.get(cl)).iterator();
        while (iter.hasNext()) {
            String u = (String) iter.next();
            if (inUnits.equalsIgnoreCase(u)) return u;
        }
        return null;
    }

    /**
     *
     *  Add a listener to the list of objects listening to changes in the default unit type
     *
     *  @param listener to add
     *
     **/
    public static void addDefaultUnitsChangeListener(Class cl, PropertyChangeListener listener) {
        Collection myListeners = (Collection) sListeners.get(cl);

        if (!myListeners.contains(listener)) {
            myListeners.add(listener);
        }
    }

    /**
     * adds the parameter to the value of the quantity and returns a NEW quantity
     * Throws a ClassCastException if the two classes are not compatible
     */
    public Quantity add(Quantity q) throws ClassCastException {
        if (!this.getClass().equals(q.getClass()))
            throw new ClassCastException("Cannot add " + q.getClass() + " to " + this.getClass());

        Quantity q2 = (Quantity) this.clone();
        String defUnits = getDefaultUnits(q2.getClass());
        q2.setValue(this.getValue(defUnits) + q.getValue(defUnits), defUnits);
        return q2;
    }

    /**
     *
     *  Remove a listener from the list of objects listening to changes in the default unit type
     *
     *  @param listener to remove
     *
     **/
    public static void removeDefaultUnitsChangeListener(Class cl, PropertyChangeListener listener) {
        Collection myListeners = (Collection) sListeners.get(cl);

        if (myListeners.contains(listener)) {
            myListeners.remove(listener);
        }
    }

    /**
     *
     * Fire a property change event to all listeners of the default unit type for
     * the specified Quantity subclass
     *
     **/
    protected static void fireDefaultUnitsChange(Class cl, PropertyChangeEvent evt) {
        Collection listeners;
        synchronized (sListeners) {
            listeners = (Collection) sListeners.get(cl);
        }

        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            PropertyChangeListener listener = (PropertyChangeListener) iter.next();
            listener.propertyChange(evt);
        }
    }

    /**
     *
     * Fire a property change event to all listeners of the default unit type
     * of this instance's class
     *
     **/
    protected void fireDefaultUnitsChange(PropertyChangeEvent evt) {
        fireDefaultUnitsChange(this.getClass(), evt);
    }

}
