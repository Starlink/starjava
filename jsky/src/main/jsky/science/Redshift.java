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
 * Redshift manages redshift quantities and provides a means to easily track different units
 * Users can set a static default units and then retrieve a redshift value (as double)
 * by calling getLength().
 *
 * On construction, the normal constructor is Redshift( value, units).
 *
 * <P>Currently units of Z and RADIALVELOCITY are supported. (Some additional
 * units names such as PARSEC have been defined, but are not yet supported as
 * valid units).
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 	7.20.99
 * @author 	    Sandy Grosvenor
 **/
public class Redshift extends Quantity {

    /** serial id needed to ensure backward compatibility when serialized **/
    private static final long serialVersionUID = 662600968082910788L;

    /**
     * testing only
     */
    public static void main(String[] args) {
        double d;
        System.out.println("z=.0033412 to RadV: " + new Redshift(.0033412, Z).getValue(RADIALVELOCITY));
        System.out.println("1000 radV to z: " + new Redshift(1000, RADIALVELOCITY).getValue(Z));
        System.out.println("1 here and back, z: " + new Redshift(1, Z).getValue(Z));
        System.out.println("1 here and back, rv: " + new Redshift(1, RADIALVELOCITY).getValue(RADIALVELOCITY));
        System.exit(0);
    }

    public static final String DEFAULTUNITS_PROPERTY = "defaultRedshiftUnit".intern();
    //public static final String VALUE_PROPERTY = "RedshiftValue".intern();

    // unit types full string labels
    public static final String Z = "Z".intern();
    public static final String RADIALVELOCITY = "RadialVelocity".intern();
    public static final String PARSEC = "Parsec".intern();
    public static final String ASTRONOMICALUNIT = "AstronomicalUnit".intern();
    public static final String DISTANCEMODULUS = "DistanceModulus".intern();


    // unit types abreviated string labels
    public static final String ZABBREV = "z".intern();
    public static final String RADIALVELOCITYABBREV = "km/s".intern();
    public static final String PARSECABBREV = "pc".intern();
    public static final String ASTRONOMICALUNITABBREV = "au".intern();
    public static final String DISTANCEMODULUSABBREV = "dm".intern();

    /**
     * length of a parsec in meters
     */
    private static final double ParsecMeters = 3.0857e16;  // parsec length in meters

    /**
     * length of an astronomical unit in meters
     */
    private static final double AUMeters = 1.496e11; // AU in meters

    static {
        List unitNames = new ArrayList();
        unitNames.add(Z);
        unitNames.add(RADIALVELOCITY);
        //nitNames.add( PARSEC);
        //unitNames.add( ASTRONOMICALUNIT);
        //unitNames.add( DISTANCEMODULUS);

        List unitAbbrev = new ArrayList();
        unitAbbrev.add(ZABBREV);
        unitAbbrev.add(RADIALVELOCITYABBREV);
        //unitAbbrev.add( PARSECABBREV);
        //unitAbbrev.add( ASTRONOMICALUNITABBREV);
        //unitAbbrev.add( DISTANCEMODULUSABBREV);

        Quantity.initializeSubClass(
                Redshift.class,
                unitNames,
                unitAbbrev,
                Z,
                //VALUE_PROPERTY,
                DEFAULTUNITS_PROPERTY);
    }

    /**
     * creates a default Redshift of length 0 with no name
     **/
    public Redshift() {
        this(0);
    }

    /**
     * creates a new Redshift of specified value in the default units
     */
    public Redshift(double inValue) {
        this(inValue, Quantity.getDefaultUnits(Redshift.class));
    }

    /**
     * returns a new Redshift (as a Quantity) with value as specified in
     * default units.
     */
    public Quantity newInstance(double inValue) {
        return new Redshift(inValue);
    }

    /**
     * primary constructor
     * @param inValue double of the actual Redshift
     * @param inUnits string containing the units in which the value is given
     */
    public Redshift(double inValue, String inUnits) {
        super();
        setValue(inValue, inUnits);
    }

    /**
     * Returns a <code>double</code>containing current value in the specified units.
     **/
    public double getValue(String unitsName) {
        try {
            return convert(fValue, Z, unitsName);
        }
        catch (UnitsNotSupportedException e) {
            writeError(this, e.toString());
            return Double.NaN;
        }

    }

    /**
     * Sets the double value of a redshift with the specified units
     **/
    protected void setValue(double inValue, String unitsName) {
        try {
            fValue = convert(inValue, unitsName, Z);
        }
        catch (UnitsNotSupportedException e) {
            writeError(this, e.toString());
            fValue = Double.NaN;
        }
    }

    /**
     * This is the workhorse of the Redshift class - converts a value
     * from one unit to the other.
     */
    public static double convert(double inVal, String fromUnits, String toUnits)
            throws UnitsNotSupportedException {
        double outVal = Double.NaN;

        if (fromUnits.equalsIgnoreCase(Z)) {
            // fValue is Z

            if (toUnits.equalsIgnoreCase(Z)) {
                outVal = inVal;
            }
            else if (toUnits.equalsIgnoreCase(RADIALVELOCITY)) {
                // fValue is Z, outval needs to be km/s,
                double zplus1sq = Math.pow((inVal + 1), 2);
                double mps = Constants.c * (zplus1sq - 1) / (zplus1sq + 1);
                outVal = mps / 1000; // km/s,
            }
            else {
                throw new UnitsNotSupportedException(toUnits);
            }
        }
        else if (toUnits.equalsIgnoreCase(Z)) {
            if (fromUnits.equalsIgnoreCase(Z)) {
                outVal = inVal;
            }
            else if (fromUnits.equalsIgnoreCase(RADIALVELOCITY)) {
                // this is the more accurate version, for small velocities, z and vdivc should be
                // close to equal
                // inVal*1000 converts from km/s to m/s
                double vdivc = inVal * 1000 / Constants.c;
                outVal = Math.pow((1 + vdivc) / (1 - vdivc), .5) - 1;
            }
            else {
                throw new UnitsNotSupportedException(fromUnits);
            }
        }
        else {
            double tmpVal = convert(inVal, fromUnits, Z);
            outVal = convert(tmpVal, Z, toUnits);
        }
        return outVal;
    }

    public static void addDefaultUnitsChangeListener(java.beans.PropertyChangeListener listener) {
        Quantity.addDefaultUnitsChangeListener(Redshift.class, listener);
    }

    public static void removeDefaultUnitsChangeListener(java.beans.PropertyChangeListener listener) {
        Quantity.removeDefaultUnitsChangeListener(Redshift.class, listener);
    }

    public static String getDefaultUnitsAbbrev() {
        return Quantity.getDefaultUnitsAbbrev(Redshift.class);
    }
}
