//=== File Prolog========================================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 587
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes------------------------------------------------------------------------------
//
//--- Development History----------------------------------------------------------------
//    Date              Author          Reference
//    11/17/97          S.Grosvenor
//      Initial packaging of class
//
//    7/6/98            S. Grosvenor/588
//      Converted System.out.printlns to MessageLogger
//    01/28/99          S. Grosvenor
//      First release of spectroscopy support
//    06/26/00          S. Grosvenor
//      Converted to jsky and added some more hertz and the ev units
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
 * This class manages wavelengths and provides a means to easily track different units.
 * Users can set a static default units and then retrieve a wavelength value (as  double)
 * by calling getValue().. or user can requests wavelengths in one of the following specific
 * wavelengths: getValue( Wavelength.METER)
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 587
 *    for the Scientist's Expert Assistant (SEA) project for Next Generation
 *    Space Telescope (NGST).
 *
 * @version 	6.1.00
 * @author 	    Sandy Grosvenor
 **/
public class Wavelength extends Quantity {

    public static final String DEFAULTUNITS_PROPERTY = "defaultWavelengthUnit".intern();
    //public static final String VALUE_PROPERTY = "WavelengthValue".intern();

    /**
     * E_METER and other E_* variables are the exponents that represent the
     * multiplier from meters to the specified E_<unit>
     */
    public static final int E_METER = 0;
    public static final int E_ANGSTROM = -10;
    public static final int E_NANOMETER = -9;
    public static final int E_MICRON = -6;
    public static final int E_MILLIMETER = -3;
    public static final int E_CENTIMETER = -2;
    public static final int E_KILOMETER = 3;
    public static final int E_HERTZ = 0;
    public static final int E_KILOHERTZ = 3;
    public static final int E_MEGAHERTZ = 6;
    public static final int E_GIGAHERTZ = 9;
    public static final int E_EV = 0;
    public static final int E_KILOEV = -3;
    public static final int E_MEGAEV = -6;


    /**
     * private array of exponential factors in the same order as the units
     * are given to parent Quantity
     */
    private static int[] fExponents = {E_ANGSTROM, E_NANOMETER, E_MICRON, E_MILLIMETER,
                                       E_CENTIMETER, E_METER, E_KILOMETER, E_HERTZ, E_KILOHERTZ, E_MEGAHERTZ, E_GIGAHERTZ,
                                       E_EV, E_KILOEV, E_MEGAEV};

    // unit types full string labels
    public static final String ANGSTROM = "angstroms".intern();
    public static final String NANOMETER = "nanometers".intern();
    public static final String MICRON = "microns".intern();
    public static final String MILLIMETER = "millimeters".intern();
    public static final String CENTIMETER = "centimeters".intern();
    public static final String METER = "meters".intern();
    public static final String KILOMETER = "kilometers".intern();
    public static final String HERTZ = "hertz".intern();
    public static final String KILOHERTZ = "kilohertz".intern();
    public static final String MEGAHERTZ = "megahertz".intern();
    public static final String GIGAHERTZ = "gigahertz".intern();
    public static final String EV = "electron-volts".intern();
    public static final String KILOEV = "kiloelectron-volts".intern();
    public static final String MEGAEV = "megaelectron-volts".intern();

    // unit types Abbreviated string labels
    public static final String ANGSTROMABBREV = "\u00c5".intern();
    public static final String NANOMETERABBREV = "nm".intern();
    public static final String MICRONABBREV = ("\u00b5" + "m").intern();
    public static final String MILLIMETERABBREV = "mm".intern();
    public static final String CENTIMETERABBREV = "cm".intern();
    public static final String METERABBREV = "m".intern();
    public static final String KILOMETERABBREV = "km".intern();
    public static final String HERTZABBREV = "Hz".intern();
    public static final String KILOHERTZABBREV = "KHz".intern();
    public static final String MEGAHERTZABBREV = "MHz".intern();
    public static final String GIGAHERTZABBREV = "GHz".intern();
    public static final String EVABBREV = "eV".intern();
    public static final String KILOEVABBREV = "KeV".intern();
    public static final String MEGAEVABBREV = "MeV".intern();

    public static final Wavelength MAX_VALUE;
    public static final Wavelength MIN_VALUE;

    static {
        List unitNames = new ArrayList();
        unitNames.add(ANGSTROM);
        unitNames.add(NANOMETER);
        unitNames.add(MICRON);
        unitNames.add(MILLIMETER);
        unitNames.add(CENTIMETER);
        unitNames.add(METER);
        unitNames.add(KILOMETER);
        unitNames.add(HERTZ);
        unitNames.add(KILOHERTZ);
        unitNames.add(MEGAHERTZ);
        unitNames.add(GIGAHERTZ);
        unitNames.add(EV);
        unitNames.add(KILOEV);
        unitNames.add(MEGAEV);

        List unitAbbrev = new ArrayList();
        unitAbbrev.add(ANGSTROMABBREV);
        unitAbbrev.add(NANOMETERABBREV);
        unitAbbrev.add(MICRONABBREV);
        unitAbbrev.add(MILLIMETERABBREV);
        unitAbbrev.add(CENTIMETERABBREV);
        unitAbbrev.add(METERABBREV);
        unitAbbrev.add(KILOMETERABBREV);
        unitAbbrev.add(HERTZABBREV);
        unitAbbrev.add(KILOHERTZABBREV);
        unitAbbrev.add(MEGAHERTZABBREV);
        unitAbbrev.add(GIGAHERTZABBREV);
        unitAbbrev.add(EVABBREV);
        unitAbbrev.add(KILOEVABBREV);
        unitAbbrev.add(MEGAEVABBREV);

        Quantity.initializeSubClass(
                Wavelength.class,
                unitNames,
                unitAbbrev,
                ANGSTROM,
                //VALUE_PROPERTY,
                DEFAULTUNITS_PROPERTY);

        MAX_VALUE = new Wavelength(Double.MAX_VALUE, ANGSTROM);
        MIN_VALUE = new Wavelength(0, ANGSTROM);
    }

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    public Wavelength() {
        this(0);
    }

    /**
     * primary constructor
     * @param inValue double of the actual Wavelength
     */
    public Wavelength(double inValue) {
        this(inValue, null);
    }

    /**
     * Primary constructor.
     * @param inValue double of the actual Wavelength
     * @param inUnits string containing the units in which the wavelength is given
     */
    public Wavelength(double inValue, String inUnits) {
        super();

        if (inUnits == null) {
            inUnits = getDefaultUnits();
        }

        setValue(inValue, inUnits);
    }

    /**
     * Returns a new Wavelength object with the specified value in the default units.
     */
    public Quantity newInstance(double inValue) {
        return new Wavelength(inValue);
    }

    /**
     * Returns value in specified Units.
     **/
    public double getValue(String unitsName) {
        try {
            return convert(fValue, METER, unitsName);
        }
        catch (UnitsNotSupportedException e) {
            writeError(this, e.toString());
            return Double.NaN;
        }

    }

    /**
     * Sets the double value with the specified units.
     **/
    protected void setValue(double inValue, String unitsName) {
        try {
            fValue = convert(inValue, unitsName, METER);
        }
        catch (UnitsNotSupportedException e) {
            writeError(this, e.toString());
            fValue = Double.NaN;
        }
    }


    /**
     * The workhorse of the Wavelength class.  Converts a value between a source
     * and a destination units
     */
    public static double convert(double inVal, String fromUnits, String toUnits)
            throws UnitsNotSupportedException {
        double outVal = Double.NaN;
        if (fromUnits.equalsIgnoreCase(METER)) {
            int index = getAllUnits(Wavelength.class).indexOf(toUnits);
            if (index < 0) {
                throw new UnitsNotSupportedException(toUnits);
            }
            else if (toUnits.endsWith(HERTZ)) {
                outVal = Constants.c / inVal / Math.pow(10, fExponents[index]);
            }
            else if (toUnits.endsWith(EV)) {
                outVal = inVal / Constants.lambda0 * Math.pow(10, fExponents[index]);
            }
            else {
                outVal = inVal / Math.pow(10, fExponents[index]);
            }
        }
        else if (toUnits.equalsIgnoreCase(METER)) {
            int index = getAllUnits(Wavelength.class).indexOf(fromUnits);

            if (index < 0) {
                throw new UnitsNotSupportedException(fromUnits);
            }
            else if (fromUnits.endsWith(HERTZ)) {
                outVal = inVal / Constants.c * Math.pow(10, fExponents[index]);
            }
            else if (fromUnits.endsWith(EV)) {
                outVal = Constants.lambda0 / inVal / Math.pow(10, fExponents[index]);
            }
            else {
                outVal = inVal * Math.pow(10, fExponents[index]);
            }
        }
        else {
            double tmpVal = convert(inVal, fromUnits, METER);
            outVal = convert(tmpVal, METER, toUnits);
        }
        return outVal;
    }

    public static void addDefaultUnitsChangeListener(java.beans.PropertyChangeListener listener) {
        Quantity.addDefaultUnitsChangeListener(Wavelength.class, listener);
    }

    public static void removeDefaultUnitsChangeListener(java.beans.PropertyChangeListener listener) {
        Quantity.removeDefaultUnitsChangeListener(Wavelength.class, listener);
    }
}
