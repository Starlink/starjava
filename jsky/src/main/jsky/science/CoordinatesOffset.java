//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class CoordinatesOffset
//
//--- Description -------------------------------------------------------------
//	The difference between two coordinates in the World Coordinate System.
//	This change is represented by a delta right-ascension and delta declination
//	in degrees.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	06/23/99	J. Jones / 588
//		Original implementation.
//
//  08/11/99    K. Wolf / AppNet
//      Added validation of Ra and Dec when setting.
//
//  12/02/99    K. Wolf / AppNet
//      Removed the previous validation. Offsets are deltas, not actual Ra, Dec coordinates.
//      Therefore change in Ra should range +/- 360. Also added rotate.
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

import java.io.Serializable;

/**
 * The difference between two coordinates in the World Coordinate System.
 * This change is represented by a delta right-ascension and delta declination
 * in degrees.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		06/23/99
 * @author		J. Jones / 588
 **/
public class CoordinatesOffset implements Serializable {

    /** Number of decimal places to show when displaying as string **/
    public static final int NUM_DECIMAL = 2;

    /**
     * The RA offset in degrees.
     **/
    private double fRa;

    /**
     * The DEC offset in degrees.
     **/
    private double fDec;

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    /**
     * Constructs and initializes an offset of 0, 0 degrees.
     **/
    public CoordinatesOffset() {
        this(0.0, 0.0);
    }

    /**
     * Constructs and initializes an offset with the same values as the specified
     * CoordinatesOffset object.
     **/
    public CoordinatesOffset(CoordinatesOffset co) {
        this(co.getRa(), co.getDec());
    }

    /**
     * Constructs and initializes an offset with the specified values in degrees.
     * @param ra the right-ascension
     * @param dec the declination
     **/
    public CoordinatesOffset(double ra, double dec) {
        set(ra, dec);
    }

    /**
     * Constructs and initializes an offset with the specified values in specified units
     * @param ra the right-ascension
     * @param dec the declination
     * @param the source units of the ra and dec
     **/
    public CoordinatesOffset(double ra, double dec, String units) {
        set(Coordinates.convert(ra, units, Coordinates.DEGREE),
                Coordinates.convert(dec, units, Coordinates.DEGREE));
    }

    /**
     * Returns the right-ascension offset in degrees.
     **/
    public double getRa() {
        return fRa;
    }

    /**
     * Returns the declination offset in degrees.
     **/
    public double getDec() {
        return fDec;
    }

    /**
     * Returns the right-ascension offset in specified units
     **/
    public double getRa(String units) {
        return Coordinates.convert(fRa, Coordinates.DEGREE, units);
    }

    /**
     * Returns the declination offset in specified units
     **/
    public double getDec(String units) {
        return Coordinates.convert(fDec, Coordinates.DEGREE, units);
    }

    /**
     * Sets the offset values.  Values are in degrees.
     **/
    public void set(double ra, double dec) {
        // Make sure results are within bounds, but fRa and range +/- 360
        fRa = validateRa(ra);
        fDec = Coordinates.validateDec(dec);
    }

    /**
     *
     *  Unlike Coordinates, CoordinatesOffsets can have a negative right ascension
     *  The reason is CoordinateOffsets represent deltas (+/-) to a Coordinate.
     *  When the CoordinateOffsets is added to the Coordinate then the proper
     *  result happens.
     *
     * @param	ra	a right-ascension of a position in degrees
     * @return  a validated +/- right-ascension of the position in degrees.
     **/
    static final public double validateRa(double ra) {
        double newRA = ra % 360.0;// Must be >= -360.0 and < 360.0 Degrees
        return newRA;
    }

    /**
     * Translates the offset values such that ra = ra + dra and dec = dec + ddec (degrees).
     **/
    public void translate(double dra, double ddec) {
        set(fRa + dra, fDec + ddec);
    }

    /**
     * Adds another offset to the offset and returns a new offset which is the sum of the two.
     *
     * @param	delta	add this amount to the offset
     * @return			new offset that is the sum of the two offsets
     **/
    public CoordinatesOffset add(CoordinatesOffset delta) {
        return new CoordinatesOffset(fRa + delta.fRa, fDec + delta.fDec);
    }

    /**
     * Subtracts another offset from the offset and returns a new offset which is the difference of the two.
     *
     * @param	delta	subtract this amount to the offset
     * @return			new offset that is the difference of the two offsets
     **/
    public CoordinatesOffset subtract(CoordinatesOffset delta) {
        return new CoordinatesOffset(fRa - delta.fRa, fDec - delta.fDec);
    }

    /**
     * Rotates the position angleRad radians about a specified center position
     * (ra, dec).
     *
     * @param	angleRad	rotation angle in radians about center point 0,0
     * @param	ra			rotate about a center point with this ra
     * @param	dec			rotate about a center point with this dec
     **/
    public void rotate(double angleRad) {
        // Rotate point and translate by original position
        double raNew = fRa * Math.cos(angleRad) - fDec * Math.sin(angleRad);
        double decNew = fRa * Math.sin(angleRad) + fDec * Math.cos(angleRad);

        // Now update me
        set(raNew, decNew);
    }

    /**
     * Determines whether two offsets are equal. Two instances of
     * <code>CoordinatesOffset</code> are equal if the values of their
     * <code>ra</code> and <code>dec</code> member fields, representing
     * their offset values in the coordinate system, are equal.
     * @param      obj   an object to be compared with this point.
     * @return     <code>true</code> if the object to be compared is
     *				an instance of <code>CoordinatesOffset</code> and has
     *				the same values; <code>false</code> otherwise.
     **/
    public boolean equals(Object obj) {
        if (obj instanceof CoordinatesOffset) {
            CoordinatesOffset offset = (CoordinatesOffset) obj;
            return (fRa == offset.fRa) && (fDec == offset.fDec);
        }
        return false;
    }

    /**
     * Returns a string representation of this offset's values.
     *
     * @return  a string representation of this offset's values
     **/
    public String toString() {
        Coordinates tempCoord = new Coordinates(fRa, fDec);
        return tempCoord.toString();
    }
}
