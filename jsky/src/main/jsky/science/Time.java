//=== File Prolog========================================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 587
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes------------------------------------------------------------------------------
//
//--- Development History----------------------------------------------------------------
//    Date              Author          Reference
//    5/28/99          S.Grosvenor
//      Initial packaging of class
//    8/5/99		C.Burkhard
//	Added AddValue Method
//    9/8/99        M. Fishman
//  Added Day unit
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

import java.util.List;
import java.util.ArrayList;

/**
 * Time, an object to manage quantities of Time.  This is not Time of Day based (see Java
 * Calendar class for that stuff), but rather amounts of Time.
 *
 * <P>Currently support units are: SECOND, MINUTE, HOUR, DAY
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 587
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 	5.29.99
 * @author 	    Sandy Grosvenor
 **/
public class Time extends Quantity {

    /** serial id needed to ensure backward compatibility when serialized **/
    private static final long serialVersionUID = 3592097662233611662L;

    public static final String DEFAULTUNITS_PROPERTY = "defaultTimeUnit".intern();
    //public static final String VALUE_PROPERTY = "TimeValue".intern();

    // unit types full string labels
    public static final String SECOND = "Seconds".intern();
    public static final String MINUTE = "Minutes".intern();
    public static final String HOUR = "Hours".intern();
    public static final String DAY = "Days".intern();

    // unit types abreviated string labels
    public static final String SECONDABBREV = "sec".intern();
    public static final String MINUTEABBREV = "min".intern();
    public static final String HOURABBREV = ("hr").intern();
    public static final String DAYABBREV = ("day").intern();

    static {
        List unitNames = new ArrayList();
        unitNames.add(SECOND);
        unitNames.add(MINUTE);
        unitNames.add(HOUR);
        unitNames.add(DAY);

        List unitAbbrev = new ArrayList();
        unitAbbrev.add(SECONDABBREV);
        unitAbbrev.add(MINUTEABBREV);
        unitAbbrev.add(HOURABBREV);
        unitAbbrev.add(DAYABBREV);

        Quantity.initializeSubClass(
                Time.class,
                unitNames,
                unitAbbrev,
                SECOND,
                //VALUE_PROPERTY,
                DEFAULTUNITS_PROPERTY);
    }

    /**
     * Creates a default Time of length 0 in the default units.
     **/
    public Time() {
        this(0);
    }

    /**
     * Create a Time quantity of specified value in the default units.
     */
    public Time(double inValue) {
        this(inValue, Quantity.getDefaultUnits(Time.class));
    }

    /**
     * Primary constructor.
     * @param inValue double of the time quantity
     * @param inUnits string containing the units in which the value is given
     */
    public Time(double inValue, String inUnits) {
        super();
        setValue(inValue, inUnits);
    }

    /**
     * returns a new Time object with the specified value in the default units.
     */
    public Quantity newInstance(double inValue) {
        return new Time(inValue);
    }

    /**
     * Returns value in specified Units.
     **/
    public double getValue(String unitsName) {
        try {
            return convert(fValue, SECOND, unitsName);
        }
        catch (UnitsNotSupportedException e) {
            writeError(this, e.toString());
            return Double.NaN;
        }

    }

    /**
     * Sets the value with the specified units
     **/
    protected void setValue(double inValue, String unitsName) {
        try {
            fValue = convert(inValue, unitsName, SECOND);
        }
        catch (UnitsNotSupportedException e) {
            writeError(this, e.toString());
            fValue = Double.NaN;
        }
    }

    /**
     * The workhorse of the Time class.  Converts a value between a source
     * and a destination units
     */
    public static double convert(double inVal, String fromUnits, String toUnits)
            throws UnitsNotSupportedException {
        double outVal = Double.NaN;
        if (fromUnits.equalsIgnoreCase(SECOND)) {
            if (toUnits.equalsIgnoreCase(SECOND)) {
                outVal = inVal;
            }
            else if (toUnits.equalsIgnoreCase(MINUTE)) {
                outVal = inVal / 60.0;
            }
            else if (toUnits.equalsIgnoreCase(HOUR)) {
                outVal = inVal / 3600.0;
            }
            else if (toUnits.equalsIgnoreCase(DAY)) {
                outVal = inVal / 86400.0;
            }
            else {
                throw new UnitsNotSupportedException(toUnits);
            }
        }
        else if (toUnits.equalsIgnoreCase(SECOND)) {
            if (fromUnits.equalsIgnoreCase(SECOND)) {
                outVal = inVal;
            }
            else if (fromUnits.equalsIgnoreCase(MINUTE)) {
                outVal = inVal * 60.0;
            }
            else if (fromUnits.equalsIgnoreCase(HOUR)) {
                outVal = inVal * 3600.0;
            }
            else if (fromUnits.equalsIgnoreCase(DAY)) {
                outVal = inVal * 86400.0;
            }
            else {
                throw new UnitsNotSupportedException(fromUnits);
            }
        }
        else {
            double tmpVal = convert(inVal, fromUnits, SECOND);
            outVal = convert(tmpVal, SECOND, toUnits);
        }
        return outVal;
    }

    public static void addDefaultUnitsChangeListener(java.beans.PropertyChangeListener listener) {
        Quantity.addDefaultUnitsChangeListener(Time.class, listener);
    }

    public static void removeDefaultUnitsChangeListener(java.beans.PropertyChangeListener listener) {
        Quantity.removeDefaultUnitsChangeListener(Time.class, listener);
    }

    public static String getDefaultUnitsAbbrev() {
        return Quantity.getDefaultUnitsAbbrev(Time.class);
    }

    public static String getUnitsAbbrev(String unitType) {
        return Quantity.getUnitsAbbrev(Time.class, unitType);
    }
}
