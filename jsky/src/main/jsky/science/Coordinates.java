//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class Coordinates
//
//--- Description -------------------------------------------------------------
//	A position in the World Coordinate System.  This position is represented
//	by a right-ascension angle and a declination angle. Both are internally stored
//  in degrees.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	02/13/98	J. Jones / 588
//		Original implementation.
//
//	05/08/98	J. Jones / 588
//		Got rid of roundAsString(), replaced references with
//		jsky.util.FormatUtilities.formatDouble().
//
//	06/24/98	J. Jones / 588
//		Fixed rounding bug in raToString() and decToString().
//
//	07/24/98	J. Jones / 588
//		Added different separator types and ability to globally switch
//		between them when going to/from strings.  Formats supported are:
//			## ## ##.##
//			##:##:##.##
//			##h##m##.##s or ##d##m##.##s
//		Also added zero padding on hours, minutes, and degrees.
//		Also changed the default separator type to spaces.
//
//	08/05/98	J. Jones / 588
//		Added the ability to ask for raToString() and decToString() with
//		specified separator format.
//
//  08/17/98    S. Grosvenor / Feddata
//      Added resourceable support
//
//  09/13/98    M. Fishman/ 588
//      Added methods to link the Coordinates format style with the
//      GeneralPreferencesPanel
//
//	09/21/98	J. Jones / 588
//		Added equinox.
//
//	02/03/99	J. Jones / 588
//		Added setValue() for use when performance is a big issue.
//
//	06/23/99	J. Jones / 588
//		Added methods to work with CoordinatesOffset.
//
//  08/11/99    K. Wolf / AppNet
//      Added Ra and Dec value validation.
//
//  10/29/99    J. Jones / 588
//      Added convenience methods for getting epoch from equinox.
//		Now includes both epoch as double year, and equinox as enumerated value.
//
//  11/9/99     S. Grosvenor / bah
//      Added convenience methods for converting between DEGREE and ARCSEC
//      Additional units can be supported by modifying convert( d, fromunits, tounits)
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

package jsky.science;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import jsky.coords.wcscon;
import jsky.util.FormatUtilities;
import jsky.util.ListenerHandler;

/**
 * A position in the World Coordinate System.  This position is represented
 * by a right-ascension angle and a declination angle.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		10/29/99
 * @author		J. Jones / 588
 **/
public class Coordinates implements Serializable {

    /**
     * Right ascension in degrees
     **/
    private double fRa;

    /**
     * Declination in degrees
     **/
    private double fDec;

    /**
     * Equinox used to display and parse the coordinate values
     * (enumerated value).
     **/
    private int fEquinox;

    /**
     * Epoch of the position (year).
     **/
    private double fEpoch;

    /**
     * List of property change listeners
     **/
    private static ListenerHandler sPropertyChangeListeners = new LocalPropertyChangeHandler();

    /**
     * support conversion units
     **/
    public static final String DEGREE = "Degree".intern();
    public static final String ARCSEC = "ArcSec".intern();
    public static final String RADIAN = "Radian".intern();

    /**
     * Separator character constants.
     **/
    private static final char SPACE_SEPARATOR = ' ';
    private static final char COLON_SEPARATOR = ':';
    private static final char HOUR_LETTER_SEPARATOR = 'h';
    private static final char MINUTE_LETTER_SEPARATOR = 'm';
    private static final char SECOND_LETTER_SEPARATOR = 's';
    private static final char DEGREE_LETTER_SEPARATOR = 'd';

    /** Number of decimal places to show when displaying as string **/
    public static final int NUM_DECIMAL = 2;

    /**
     * Constant for indicating that coordinate values should be separated by spaces.
     **/
    public static final int SPACE_SEPARATOR_STYLE = 0;
    public static final String SPACE_SEPARATOR_STYLE_LABEL = "## ## ##.##";

    /**
     * Constant for indicating that coordinate values should be separated by colons.
     **/
    public static final int COLON_SEPARATOR_STYLE = 1;
    public static final String COLON_SEPARATOR_STYLE_LABEL = "##:##:##.##";

    /**
     * Constant for indicating that coordinate values should be separated by 'h', 'd', 'm', 's' letters.
     **/
    public static final int LETTER_SEPARATOR_STYLE = 2;
    public static final String LETTER_SEPARATOR_STYLE_LABEL = "##h##m##.##s";

    /**
     * The B1950 equinox
     **/
    public static final int B1950_EQUINOX = 1;
    public static final String B1950_EQUINOX_LABEL = "B1950";

    /**
     * The J2000 equinox
     **/
    public static final int J2000_EQUINOX = 2;
    public static final String J2000_EQUINOX_LABEL = "J2000";

    /** Bound property name. */
    public static final String FORMATSTYLEPROPERTY = "FormatStyleProperty".intern();

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * Set of all possible separator style labels
     **/
    private static final String[] ALL_STYLE_LABELS =
            {
                SPACE_SEPARATOR_STYLE_LABEL,
                COLON_SEPARATOR_STYLE_LABEL,
                LETTER_SEPARATOR_STYLE_LABEL
            };

    /**
     * Set of all possible equinox labels
     **/
    private static final String[] ALL_EQUINOX_LABELS =
            {
                J2000_EQUINOX_LABEL,
                B1950_EQUINOX_LABEL
            };

    /**
     * The global separator style
     **/
    private static int sSeparatorStyle = SPACE_SEPARATOR_STYLE;

    /**
     * The default equinox
     **/
    private static final int DEFAULT_EQUINOX = J2000_EQUINOX;

    /**
     * Constructs a coordinates object with RA and Dec of zero.
     **/
    public Coordinates() {
        super();

        fRa = 0.0;
        fDec = 0.0;
        fEquinox = DEFAULT_EQUINOX;
        fEpoch = 2000.0;
    }

    /**
     * Constructs a coordinates object with specified RA and Dec in the default equinox.
     *
     * @param	ra	right-ascension in degrees
     * @param	dec	declination in degrees
     **/
    public Coordinates(double ra, double dec) {
        this(ra, dec, DEFAULT_EQUINOX);
    }

    /**
     * Constructs a coordinates object with specified RA and Dec in the specified equinox.
     *
     * @param	ra	right-ascension in degrees
     * @param	dec	declination in degrees
     * @param   equinox equinox constant
     **/
    public Coordinates(double ra, double dec, int equinox) {
        super();

        // Make sure coordinates are within bounds
        fRa = validateRa(ra);
        fDec = validateDec(dec);

        setEquinox(equinox);
    }

    /**
     * Constructs a coordinates object with specified RA and Dec in the specified equinox and epoch.
     *
     * @param	ra	right-ascension in degrees
     * @param	dec	declination in degrees
     * @param   equinox equinox constant
     * @param	epoch	epoch year
     **/
    public Coordinates(double ra, double dec, int equinox, double epoch) {
        this(ra, dec, equinox);
        setEpoch(epoch);
    }

    /**
     * Constructs a new coordinates object that is a copy of the specified object.
     *
     * @param	old		Coordinates object to copy
     **/
    public Coordinates(Coordinates old) {
        this(old.getRa(), old.getDec(), old.getEquinox(), old.getEpoch());
    }

    /**
     * Returns the right-ascension of the position in degrees.
     *
     * @return	right-ascension of the position in degrees
     **/
    public double getRa() {
        return fRa;
    }

    /**
     * Returns the declination of the position in degrees.
     *
     * @return	declination of the position in degrees
     **/
    public double getDec() {
        return fDec;
    }

    /**
     * converts a coordinate from specified source units to the destination units
     * @param coord  The coordinate to be converted
     * @param fromUnits  String containing source units
     * @param toUnits String containing target units
     **/
    public static double convert(double coord, String fromUnits, String toUnits) {
        if (!fromUnits.equals(DEGREE) && !toUnits.equals(DEGREE)) {
            double deg = convert(coord, fromUnits, DEGREE);
            return convert(deg, DEGREE, toUnits);
        }
        else if (fromUnits.equals(DEGREE)) {
            if (toUnits.equals(ARCSEC)) {
                return coord * 3600.0;
            }
            else if (toUnits.equals(DEGREE)) {
                return coord;
            }
            else if (toUnits.equals(RADIAN)) {
                return coord / 180. * Math.PI;
            }
            else {
                writeError("Coordinates.convert", "Target units unsupported, " + toUnits);
                return Double.NaN;
            }
        }
        else // must be toUnits.equals( DEGREE)
        {
            if (fromUnits.equals(ARCSEC)) {
                return coord / 3600.0;
            }
            else if (fromUnits.equals(DEGREE)) {
                return coord;
            }
            else if (fromUnits.equals(RADIAN)) {
                return coord * 180. / Math.PI;
            }
            else {
                writeError("Coordinates.convert", "Source units unsupported, " + fromUnits);
                return Double.NaN;
            }
        }
    }

    protected static void writeError(Object source, Object message) {
        System.err.println("[ERROR] " + source + ": " + message);
    }

    /**
     * Returns the right-ascension of the position in degrees.
     *
     * @return	right-ascension of the position in degrees
     **/
    public double getRa(String units) {
        return convert(fRa, DEGREE, units);
    }

    /**
     * Returns the declination of the position in degrees.
     *
     * @return	declination of the position in degrees
     **/
    public double getDec(String units) {
        return convert(fDec, DEGREE, units);
    }

    /**
     * Sets the right-ascension of the position in degrees.
     *
     * @param	ra	the new right-ascension of the position in degrees
     **/
    public void setRa(double ra) {
        fRa = validateRa(ra);
    }

    /**
     * Sets the declination of the position in degrees.
     *
     * @param	dec	the new declination of the position in degrees
     **/
    public void setDec(double dec) {
        fDec = validateDec(dec);
    }

    /**
     * Sets the right-ascension of the position in degrees.
     *
     * @param	ra	the new right-ascension of the position
     * @param   units ra's units
     **/
    public void setRa(double ra, String units) {
        fRa = validateRa(convert(ra, units, DEGREE));
    }

    /**
     * Sets the declination of the position in degrees.
     *
     * @param	dec	the new declination of the position in degrees
     * @param   units dec's units
     **/
    public void setDec(double dec, String units) {
        fDec = validateDec(convert(dec, units, DEGREE));
    }

    /**
     * Sets the right-ascension and declination of the position in degrees.
     *
     * @param	ra	the new right-ascension of the position in degrees
     * @param	dec	the new declination of the position in degrees
     **/
    public void setValue(double ra, double dec) {
        setRa(ra);
        setDec(dec);
    }

    /**
     *
     * Returns a properly validated right-ascension in specified units
     *
     * @param	ra	a right-ascension of a position
     * @param   units the units of the right ascension
     * @return  a validated right-ascension of the position
     **/
    static final public double validateRa(double ra, String units) {
        return convert(validateRa(convert(ra, units, DEGREE)), DEGREE, units);
    }

    /**
     *
     * Returns a properly validated right-ascension in degrees.
     *
     * @param	ra	a right-ascension of a position in degrees
     * @return  a validated right-ascension of the position in degrees.
     **/
    static final public double validateRa(double ra) {
        double newRA = ra % 360.0;// Must be >= 0.0 and < 360.0 Degrees
        if (newRA < 0.0) newRA += 360.0; // must be positive too
        return newRA;
    }

    /**
     *
     * Returns a properly validated declination in specified units
     *
     * @param	ra	a declination of a position
     * @param   units the units of the declination
     * @return  a validated declination of the position
     **/
    static final public double validateDec(double dec, String units) {
        return convert(validateDec(convert(dec, units, DEGREE)), DEGREE, units);
    }

    /**
     *
     * Returns a properly validated declination in degrees.
     *
     * @param	dec	a declination of a position in degrees
     * @return  a validated declination of the position in degrees.
     **/
    static final public double validateDec(double dec) {
        double newDec = dec % 360.0;      // Want a manageable angle

        if (newDec < 0.0) newDec += 360.0;  // Make negative positive

        if (newDec > 270.0) {
            newDec -= 360.0;
        }
        else if (newDec > 90.0) {
            newDec = 180.0 - newDec;
        }

        return newDec;                      // Now between -90.0 and 90.0
    }

    /**
     * Returns the ecliptic longitude of the position in degrees.
     * @see "getEclipticLatitude for source tcl code from WFPC2"
     *
     * @return	ecliptic longitude of the position in degrees
     **/
    public double getEclipticLongitude() {
        double ra_rads = convert(fRa * 15, DEGREE, RADIAN);
        double dec_rads = convert(fDec, DEGREE, RADIAN);
        double beta = convert(getEclipticLatitude(), DEGREE, RADIAN);

        double lambda = Math.acos(Math.cos(dec_rads) * Math.cos(ra_rads) / Math.cos(beta));
        return convert(lambda, RADIAN, DEGREE);
    }

    /**
     * Returns the ecliptic latitude of the position in degrees.
     * This code converted from the WFPC2 ETC
     *
     * @return	ecliptic latitude of the position in degrees
     **/
    public double getEclipticLatitude() {
        /* This code converted from the WFPC2 ETC:
           proc ecliptical_coords {alpha delta {time 1}} {
                # Input:  alpha in hh mm ss     delta in dd mm ss
                # Output: lambda in dd          beta in dd
                # The singular points are not yet treated

                # conversion from hours to degrees to rad
                set alpha [rad_from_deg [expr 15*[decimal $alpha]]]

                # conversion from degrees to rad
                set delta [rad_from_deg [decimal $delta]]
                set eps [rad_from_deg [obliquity_of_ecliptic $time]]
                set beta [expr asin(sin($delta)*cos($eps) \
                                        - cos($delta)*sin($eps)*sin($alpha))]
                set lambda [expr acos(cos($delta)*cos($alpha) / cos($beta))]
            #   set lambda [expr asin((sin($delta)*sin($eps) \
            #		       + cos($delta)*cos($eps)*sin($alpha)) \
            #			      / cos($beta))]

                # conversion from rad to degrees
                set lambda [deg_from_rad $lambda]
                set beta [deg_from_rad $beta]
                return [format "%11.7f %11.7f" $lambda $beta]
            }
        */

        double ra_rads = convert(fRa * 15, DEGREE, RADIAN);
        double dec_rads = convert(fDec, DEGREE, RADIAN);
        double eps = getObliquityOfEcliptic();

        double lat = Math.asin(
                Math.sin(dec_rads) * Math.cos(eps) - Math.cos(dec_rads) * Math.sin(eps) * Math.sin(ra_rads)
        );
        return convert(lat, RADIAN, DEGREE);
    }

    /**
     * The angle at which the plane of the ecliptic is inclined to the
     * plane of the equator at a point in time. Code is ported from
     * the WFPC2 tcl-based ETC, see also "Practical Astronomy with your
     * Calculator" by Peter Duffett-Smith
     * <P>
     * The "point in time" used taken from the Equinox property of
     * the Coordinate to be converted.
     * @return	ecliptic latitude of the position in decimal degrees
     **/
    private double getObliquityOfEcliptic() {
        /* ------- based on code from WFPC2 tcl/tk ETC:
            proc obliquity_of_ecliptic {{time 1}} {
                # time in centuries since 1900
                # defaults to year 2000
                # returns results in degree
                set eps [decimal {23 27 8.26}]
                set result [expr $eps + \
                            (- 46.845*$time \
                            - 0.0059*pow($time,2) \
                            + 0.00181*pow($time,3))/3600.]
                return [format "%11.7f" $result]
        -------- */

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(1900, 01, 01);
        long time1900 = cal.getTime().getTime();

        cal.set(2000, 01, 01);
        long oneCentury = cal.getTime().getTime() - time1900;

        switch (fEquinox) {
        case B1950_EQUINOX:
            cal.set(1950, 01, 01);
            break;
        case J2000_EQUINOX:
            cal.set(2000, 01, 01);
            break;
        }
        long timeTarget = cal.getTime().getTime();

        long centuriesSince1900 = (timeTarget - time1900) / oneCentury + 1;

        // set eps [decimal {23 27 8.26}]  -- angle in 1900, in hh mm sec
        double eps1900 = 23 + 27 / 60. + 8.25 / 3600.;

        double eps = eps1900 + (-46.845 * centuriesSince1900 -
                0.0059 * Math.pow(centuriesSince1900, 2) + 0.00181 * Math.pow(centuriesSince1900, 3)) / 3600.;
        return eps;
    }

    /**
     * Returns the equinox used to display and parse the coordinate values.
     * This is an enumerated value, not the year.
     *
     * @return	enumerated equinox constant
     **/
    public int getEquinox() {
        return fEquinox;
    }

    /**
     * Sets the equinox used to display and parse the coordinate values.
     * Setting the equinox also sets the epoch.
     *
     * @param	equinox enumerated equinox constant
     **/
    public void setEquinox(int equinox) {
        if (equinox != J2000_EQUINOX && equinox != B1950_EQUINOX) {
            throw new IllegalArgumentException("Equinox must be either J2000 or B1950 constant");
        }

        fEquinox = equinox;
        fEpoch = equinoxIntToYear(fEquinox);
    }

    /**
     * Returns the epoch year for the position.
     *
     * @return	epoch year
     **/
    public double getEpoch() {
        return fEpoch;
    }

    /**
     * Sets the epoch year for the position.  Does not affect equinox.
     *
     * @param	epoch	the epoch year
     **/
    public void setEpoch(double epoch) {
        fEpoch = epoch;
    }

    /**
     * Returns the global separator style.
     *
     * @return the global separator style
     **/
    public static int getSeparatorStyle() {
        return sSeparatorStyle;
    }

    /**
     * Changes the global separator style.  Notifies listeners of change.
     *
     * @param style	new global separator style
     **/
    public static void setSeparatorStyle(int style) {
        if (sSeparatorStyle != style) {
            Class thisClass = jsky.science.Coordinates.class;
            int oldStyle = sSeparatorStyle;
            sSeparatorStyle = style;
            PropertyChangeEvent evt = new PropertyChangeEvent(thisClass,
                    FORMATSTYLEPROPERTY,
                    new Integer(oldStyle),
                    new Integer(sSeparatorStyle));
            fireSeparatorStyleChange(evt);
        }
    }

    /**
     * Returns the default equinox
     *
     * @return the default equinox
     **/
    public static int getDefaultEquinox() {
        return DEFAULT_EQUINOX;
    }

    /**
     * Compares this Coordinates object to the specified object and
     * returns true if the objects are equivalent.
     *
     * @param	obj	object to compare to this Coodinates object
     **/
    public boolean equals(Object obj) {
        if (obj instanceof Coordinates) {
            Coordinates c = (Coordinates) obj;
            return (fRa == c.getRa()) && (fDec == c.getDec()) && (fEquinox == c.getEquinox()) && (fEpoch == c.getEpoch());
        }
        return false;
    }

    /**
     * Returns the hashcode for this Coordinates.
     * Hashing algorithm taken from java.awt.geom.Point2D.
     *
     * @return	a hash code for this Coordinates.
     */
    public int hashCode() {
        long bits = Double.doubleToLongBits(fRa);
        bits ^= Double.doubleToLongBits(fDec) * 31;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Translates this position by the specified amounts so that
     * the new position equals the current position + the arguments.
     * Note that the arguments are in arcseconds while the Coordinates
     * RA and Dec are in degrees.
     *
     * @param	raArcsec	amount to translate the right-ascension
     * @param	decArcsec	amount to translate the declination
     **/
    public void translate(double raArcsec, double decArcsec) {
        fRa += raArcsec / 3600;
        fDec += decArcsec / 3600;

        // Make sure results are within bounds
        fRa = validateRa(fRa);
        fDec = validateDec(fDec);
    }

    /**
     * Translates this position by the specified offset so that
     * the new position equals the current position + the argument.
     *
     * @param	offset		amount to add to current coordinates
     **/
    public void translate(CoordinatesOffset offset) {
        fRa += offset.getRa();
        fDec += offset.getDec();

        // Make sure results are within bounds
        fRa = validateRa(fRa);
        fDec = validateDec(fDec);
    }

    /**
     * Rotates the position angleRad radians about a specified center position
     * (ra, dec).
     *
     * @param	angleRad	rotation angle in radians
     * @param	ra			rotate about a center point with this ra
     * @param	dec			rotate about a center point with this dec
     **/
    public void rotate(double angleRad, double ra, double dec) {
        // Rotate point and translate by original position
        double raNew = (fRa * Math.cos(angleRad) - fDec * Math.sin(angleRad)) + ra;
        double decNew = (fRa * Math.sin(angleRad) + fDec * Math.cos(angleRad)) + dec;

        // Store new point
        fRa = raNew;
        fDec = decNew;

        // Make sure results are within bounds
        fRa = validateRa(fRa);
        fDec = validateDec(fDec);
    }

    /**
     * Adds an offset to the current coordinates and returns the
     * sum as a new Coordinates object.
     *
     * @param	offset	add to the current coordinates
     * @return			sum of the two
     **/
    public Coordinates add(CoordinatesOffset offset) {
        return new Coordinates(fRa + offset.getRa(), fDec + offset.getDec(), fEquinox, fEpoch);
    }

    /**
     * Subtracts the coordinates c from the current coordinates and returns the
     * difference as a new CoordinatesOffset.
     *
     * @param	c	subtract these coordinates from current
     * @return		difference between two points
     **/
    public CoordinatesOffset subtract(Coordinates c) {
        // If other coordinates are in different equinox,
        // convert to current equinox
        Coordinates c2 = c;
        if (c2.getEquinox() != fEquinox) {
            switch (fEquinox) {
            case B1950_EQUINOX:
                c2 = c2.toB1950();
                break;

            case J2000_EQUINOX:
                c2 = c2.toJ2000();
                break;
            }
        }

        return new CoordinatesOffset(fRa - c2.fRa, fDec - c2.fDec);
    }

    /**
     * Returns a string representation of the position.
     *
     * @return	position as a string
     **/
    public String toString() {
        return "RA: " + raToString() + ", DEC: " + decToString() + " (" + getEpoch() + ")";
    }

    /**
     * Returns the coordinates converted to the B1950 equinox,
     * or just returns itself if already B1950.
     *
     * @return	coordinates converted to B1950
     **/
    public Coordinates toB1950() {
        switch (getEquinox()) {
        case B1950_EQUINOX:
            return this;

        case J2000_EQUINOX:
            Point2D.Double out = wcscon.fk524(new Point2D.Double(getRa(), getDec()));
            return new Coordinates(out.x, out.y, B1950_EQUINOX);

        default:
            return null;
        }
    }

    /**
     * Returns the coordinates converted to the J2000 equinox,
     * or just returns itself if already J2000.
     *
     * @return	coordinates converted to J2000
     **/
    public Coordinates toJ2000() {
        switch (getEquinox()) {
        case J2000_EQUINOX:
            return this;

        case B1950_EQUINOX:
            Point2D.Double out = wcscon.fk425(new Point2D.Double(getRa(), getDec()));
            return new Coordinates(out.x, out.y, J2000_EQUINOX);

        default:
            return null;
        }
    }

    /**
     * Returns a string representation of the right-ascension.
     *
     * @return	right-ascension as a string
     **/
    public String raToString() {
        return raToString(sSeparatorStyle);
    }

    /**
     * Returns a string representation of the right-ascension.
     * The desired separator style can be specified as an argument.
     *
     * @param	separatorStyle	style of separator desired in output
     * @return	right-ascension as a string
     **/
    public String raToString(int separatorStyle) {
        double a, b;
        double seconds;
        int hours;
        int minutes;

        a = fRa / 15.0;

        // Convert to hours
        hours = (int) a;

        // Compute minutes
        b = (a - (double) hours) * 60.0;
        minutes = (int) b;

        // Compute seconds
        seconds = (b - (double) minutes) * 60.0;

        // Truncate the seconds string
        String secondsStr = FormatUtilities.formatDouble(seconds, NUM_DECIMAL);

        // Check for rounding overflow from toStringRounded()
        if (seconds > 59.999999 || secondsStr.startsWith("60.")) {
            seconds = 0.0;
            secondsStr = "0.00";
            ++minutes;
        }

        if (minutes > 59) {
            minutes = 0;
            ++hours;
        }

        if (hours > 23) {
            hours -= 24;
        }

        String hoursStr = "";
        if (hours >= 10) {
            hoursStr += hours;
        }
        else {
            hoursStr = "0" + hours;
        }

        String minutesStr = "";
        if (minutes >= 10) {
            minutesStr += minutes;
        }
        else {
            minutesStr = "0" + minutes;
        }

        switch (separatorStyle) {
        case SPACE_SEPARATOR_STYLE:
        default:
            return hoursStr + SPACE_SEPARATOR + minutesStr
                    + SPACE_SEPARATOR + secondsStr;

        case COLON_SEPARATOR_STYLE:
            return hoursStr + COLON_SEPARATOR + minutesStr
                    + COLON_SEPARATOR + secondsStr;

        case LETTER_SEPARATOR_STYLE:
            return hoursStr + HOUR_LETTER_SEPARATOR + minutesStr
                    + MINUTE_LETTER_SEPARATOR + secondsStr + SECOND_LETTER_SEPARATOR;
        }
    }

    /**
     * Returns a string representation of the declination.
     *
     * @return	declination as a string
     **/
    public String decToString() {
        return decToString(sSeparatorStyle);
    }

    /**
     * Returns a string representation of the declination.
     * The desired separator style can be specified as an argument.
     *
     * @param	separatorStyle	style of separator desired in output
     * @return	declination as a string
     **/
    public String decToString(int separatorStyle) {
        double a, b;
        double seconds;
        char sign;
        int degrees;
        int minutes;
        int isec;

        a = fDec;

        // Set sign and do all the rest with a positive
        if (a < 0) {
            sign = '-';
            a = -a;
        }
        else {
            sign = '+';
        }

        // Convert to degrees
        degrees = (int) a;

        // Compute minutes
        b = (a - (double) degrees) * 60.0;
        minutes = (int) b;

        // Compute seconds
        seconds = (b - (double) minutes) * 60.0;

        // Truncate the seconds string
        String secondsStr = FormatUtilities.formatDouble(seconds, NUM_DECIMAL);

        // Check for rounding overflow from toStringRounded()
        if (seconds > 59.999999 || secondsStr.startsWith("60.")) {
            seconds = 0.0;
            secondsStr = "0.00";
            ++minutes;
        }

        if (minutes > 59) {
            minutes = 0;
            ++degrees;
        }

        String degreesStr = "";
        if (degrees >= 10) {
            degreesStr += degrees;
        }
        else {
            degreesStr = "0" + degrees;
        }

        String minutesStr = "";
        if (minutes >= 10) {
            minutesStr += minutes;
        }
        else {
            minutesStr = "0" + minutes;
        }

        switch (separatorStyle) {
        case SPACE_SEPARATOR_STYLE:
        default:
            return "" + sign + degreesStr + SPACE_SEPARATOR + minutesStr
                    + SPACE_SEPARATOR + secondsStr;

        case COLON_SEPARATOR_STYLE:
            return "" + sign + degreesStr + COLON_SEPARATOR + minutesStr
                    + COLON_SEPARATOR + secondsStr;

        case LETTER_SEPARATOR_STYLE:
            return "" + sign + degreesStr + DEGREE_LETTER_SEPARATOR + minutesStr
                    + MINUTE_LETTER_SEPARATOR + secondsStr + SECOND_LETTER_SEPARATOR;
        }
    }

    /**
     * Returns a string representation of the equinox.
     *
     * @return	equinox as a string
     **/
    public String equinoxToString() {
        return equinoxIntToString(getEquinox());
    }

    /**
     * Returns a string representation of the epoch.
     *
     * @return	epoch as a string
     **/
    public String epochToString() {
        return String.valueOf(getEpoch());
    }

    /**
     * Creates a new Coordinates object using two strings for the RA and Dec.
     * The strings are parsed and the values fed into a new Coordinates object.
     *
     * @param	raInput     Right-ascension in string format
     * @param	decInput	Declination in string format
     * @param	equinox 	equinox constant for the new Coordinates
     * @return	new Coordinates object for the input RA and Dec
     * @exception NumberFormatException	thrown if unable to parse the input strings
     **/
    public static Coordinates valueOf(String raInput, String decInput, int equinox)
            throws NumberFormatException, IllegalArgumentException {
        Coordinates c = valueOf(raInput, decInput);
        c.setEquinox(equinox);
        return c;
    }

    /**
     * Creates a new Coordinates object using two strings for the RA and Dec.
     * The strings are parsed and the values fed into a new Coordinates object.
     *
     * @param	raInput	Right-ascension in string format
     * @param	decInput	Declination in string format
     * @return	new Coordinates object for the input RA and Dec
     * @exception NumberFormatException	thrown if unable to parse the input strings
     **/
    public static Coordinates valueOf(String raInput, String decInput)
            throws NumberFormatException, IllegalArgumentException {
        if (raInput == null || decInput == null || raInput.length() == 0 || decInput.length() == 0) {
            throw new NumberFormatException();
        }

        // Use the proper separators for the current style
        char dSep, mSep, sSep;
        switch (sSeparatorStyle) {
        case SPACE_SEPARATOR_STYLE:
        default:
            dSep = SPACE_SEPARATOR;
            mSep = SPACE_SEPARATOR;
            sSep = SPACE_SEPARATOR;
            break;

        case COLON_SEPARATOR_STYLE:
            dSep = COLON_SEPARATOR;
            mSep = COLON_SEPARATOR;
            sSep = COLON_SEPARATOR;
            break;

        case LETTER_SEPARATOR_STYLE:
            dSep = HOUR_LETTER_SEPARATOR;
            mSep = MINUTE_LETTER_SEPARATOR;
            sSep = SECOND_LETTER_SEPARATOR;
            break;
        }

        // Extract Right-ascension
        double ra = degreesFromString(raInput, dSep, mSep, sSep);

        // degreesFromString assumes degrees, but was really in hours, so correct
        if (raInput.indexOf(dSep) >= 0) {
            ra *= 15.0;
        }

        ra = validateRa(ra);
        if (ra < 0.0 || ra >= 360.0) {
            throw new IllegalArgumentException("Right-ascension must be between 0 and 360 degrees.");
        }

        if (dSep == HOUR_LETTER_SEPARATOR) {
            dSep = DEGREE_LETTER_SEPARATOR;
        }

        // Extract declination
        double dec = degreesFromString(decInput, dSep, mSep, sSep);

        dec = validateDec(dec);
        if (dec < -90.0 || dec > 90.0) {
            throw new IllegalArgumentException("Declination must be between -90.0 and 90.0 degrees.");
        }

        return new Coordinates(ra, dec);
    }

    /**
     * Parses the input string and returns its double value in degrees.
     * The string can either be a single double value, single integer value,
     * or of the form "Degrees<sep>Minutes<sep>Seconds", where <sep> is specified
     * in the arguments.
     *
     * @param	input	input string to parse
     * @param	dSep	the degrees separator character
     * @param	mSep	the minutes separator character
     * @param	sSep	the seconds separator character
     * @return	double	degrees value from the input string
     * @exception NumberFormatException	thrown if unable to parse the input string
     **/
    private static final double degreesFromString(String input, char dSep, char mSep, char sSep)
            throws NumberFormatException {
        String inString = input.trim();
        String temp = "";

        double degrees = 0.0;
        double sign = 0.0;
        double d = 0.0, m = 0.0, s = 0.0;

        if (inString.charAt(0) == '-') {
            sign = -1.0;
            inString = inString.substring(1);
        }
        else if (inString.charAt(0) == '+') {
            sign = 1.0;
            inString = inString.substring(1);
        }
        else {
            sign = 1.0;
        }

        if (inString.indexOf(dSep) == -1) {
            if (inString.indexOf('.') >= 0) {
                // is single double value in degrees
                degrees = Double.valueOf(inString).doubleValue();
            }
            else {
                // is single integer value in degrees
                degrees = (double) Integer.valueOf(inString).intValue();
            }
        }
        else // is #<separator>#<separator># formatted string
        {
            temp = inString.substring(0, inString.indexOf(dSep));
            inString = inString.substring(inString.indexOf(dSep) + 1);
            d = (double) Integer.valueOf(temp).intValue();

            if (inString.indexOf(mSep) == -1) {
                throw new NumberFormatException();
            }

            temp = inString.substring(0, inString.indexOf(mSep));
            inString = inString.substring(inString.indexOf(mSep) + 1);
            if (inString.indexOf(sSep) != -1) // get rid of trailing seconds separator
            {
                inString = inString.substring(0, inString.indexOf(sSep));
            }
            m = (double) Integer.valueOf(temp).intValue();

            s = Double.valueOf(inString).doubleValue();

            degrees = sign * (d + (m / 60.0) + (s / 3600.0));
        }

        return degrees;
    }

    /**
     *
     * returns the separator style given its label
     *
     **/
    public static int separatorStyleStringToInt(String style) {
        if ((style != null) && (style.length() > 1)) {
            if (style.equals(SPACE_SEPARATOR_STYLE_LABEL))
                return SPACE_SEPARATOR_STYLE;
            else if (style.equals(COLON_SEPARATOR_STYLE_LABEL))
                return COLON_SEPARATOR_STYLE;
            else if (style.equals(LETTER_SEPARATOR_STYLE_LABEL)) return LETTER_SEPARATOR_STYLE;
        }
        return 0;
    }

    /**
     *
     * returns the full label name of a separator style
     *
     **/
    public static String separatorStyleIntToString(int style) {
        String s = "not defined";

        switch (style) {
        case SPACE_SEPARATOR_STYLE:
            s = SPACE_SEPARATOR_STYLE_LABEL;
            break;
        case COLON_SEPARATOR_STYLE:
            s = COLON_SEPARATOR_STYLE_LABEL;
            break;
        case LETTER_SEPARATOR_STYLE:
            s = LETTER_SEPARATOR_STYLE_LABEL;
            break;
        }
        return s;
    }


    /**
     *
     * returns the list of string labels for possible separator styles
     *
     **/
    public static String[] getAllSeparatorStyles() {
        return ALL_STYLE_LABELS;
    }

    /**
     *
     * returns the equinox given its label
     *
     **/
    public static int equinoxStringToInt(String equinox) {
        if ((equinox != null) && (equinox.length() > 1)) {
            if (equinox.equals(B1950_EQUINOX_LABEL))
                return B1950_EQUINOX;
            else if (equinox.equals(J2000_EQUINOX_LABEL)) return J2000_EQUINOX;
        }
        return 0;
    }

    /**
     *
     * returns the full label name of an equinox
     *
     **/
    public static String equinoxIntToString(int equinox) {
        String s = "not defined";

        switch (equinox) {
        case B1950_EQUINOX:
            s = B1950_EQUINOX_LABEL;
            break;
        case J2000_EQUINOX:
            s = J2000_EQUINOX_LABEL;
            break;
        }
        return s;
    }

    /**
     *
     * returns the year of the enumerated equinox value
     *
     **/
    public static double equinoxIntToYear(int equinox) {
        double year = Double.NaN;

        switch (equinox) {
        case B1950_EQUINOX:
            year = 1950.0;
            break;
        case J2000_EQUINOX:
            year = 2000.0;
            break;
        }
        return year;
    }

    /**
     *
     * returns the list of string labels for possible equinoxs
     *
     **/
    public static String[] getAllEquinoxes() {
        return ALL_EQUINOX_LABELS;
    }

    /**
     *
     *  add a listener to the list of objects listening to changes in the separator style
     *
     *  @param listener to add
     *
     **/
    public static void addSeparatorStyleChangeListener(PropertyChangeListener listener) {
        sPropertyChangeListeners.addListener(listener);
    }

    /**
     *
     *  remove a listener from the list of objects listening to changes in the separator style
     *
     *  @param listener to remove
     *
     **/
    public static void removeSeparatorStyleChangeListener(PropertyChangeListener listener) {
        sPropertyChangeListeners.removeListener(listener);
    }

    /**
     *
     * fire a property change event to all listeners of the separator style
     *
     **/
    protected static void fireSeparatorStyleChange(PropertyChangeEvent evt) {
        sPropertyChangeListeners.sendEvent("", evt);
    }

    public static class LocalPropertyChangeHandler extends ListenerHandler {

        public void fireEvent(String eventkey, Object listener, Object event) {
            ((PropertyChangeListener) listener).propertyChange(
                    (PropertyChangeEvent) event);
        }
    }

}
