//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class ProperMotion
//
//--- Description -------------------------------------------------------------
//	The apparent motion of an object on the celestial sphere.
//	ProperMotion is a change in right-ascension and declination in degrees
//	per year. ProperMotion also includes an error for both values (+/- degrees).
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	10/27/99	J. Jones / 588
//		Original implementation.
//
//  11/10/99    S. Grosvenor/ BAH
//      Changed relationship to CoordinateOffset, from inheriting it to
//      "has a" offset, now extends AbstractScienceObjectNode
//
//    05/03/00    S. Grosvenor / 588 Booz-Allen
//      ScienceObject overhaul
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
//
//=== End File Prolog =========================================================

package jsky.science;

/**
 * The apparent motion of an object on the celestial sphere.
 * ProperMotion is a change in right-ascension and declination in degrees
 * per year. ProperMotion also includes an error for both values (+/- degrees).
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		2000.05.03
 * @author		J. Jones / 588
 **/
public class ProperMotion extends AbstractScienceObject {

    /**
     * the amount of the offset
     **/
    private CoordinatesOffset fOffset;

    /**
     * The RA component of proper motion error in degrees.
     **/
    private double fRaError;

    /**
     * The DEC component of proper motion error in degrees.
     **/
    private double fDecError;

    /**
     * The Stream Unique Identifier for this class.
     **/
    private static final long serialVersionUID = 1L;

    public static final String OFFSET_PROPERTY = "Offset".intern();
    public static final String RAERROR_PROPERTY = "RaError".intern();
    public static final String DECERROR_PROPERTY = "DecError".intern();

    /**
     * Constructs and initializes a proper motion of 0, 0 degrees per year.
     **/
    public ProperMotion() {
        this(0.0, 0.0);
    }

    /**
     * Constructs and initializes a proper motion with the same values as the specified
     * ProperMotion object.
     **/
    public ProperMotion(ProperMotion pm) {
        this(pm.getOffset().getRa(), pm.getOffset().getDec(), pm.getRaError(), pm.getDecError());
    }

    /**
     * Constructs and initializes a proper motion with the specified values in degrees.
     **/
    public ProperMotion(double ra, double dec) {
        this(ra, dec, 0.0, 0.0);
    }

    /**
     * Constructs and initializes a proper motion with the specified values in specified units.
     **/
    public ProperMotion(double ra, double dec, String units) {
        this(ra, dec, 0.0, 0.0, units);
    }

    /**
     * Constructs and initializes a proper motion with the specified values in specified units
     **/
    public ProperMotion(double ra, double dec, double raErr, double decErr, String units) {
        this(Coordinates.convert(ra, units, Coordinates.DEGREE),
                Coordinates.convert(dec, units, Coordinates.DEGREE),
                Coordinates.convert(raErr, units, Coordinates.DEGREE),
                Coordinates.convert(decErr, units, Coordinates.DEGREE));

    }

    /**
     * Constructs and initializes a proper motion with the specified values in degrees.
     **/
    public ProperMotion(double ra, double dec, double raErr, double decErr) {
        super();

        setOffset(new CoordinatesOffset(ra, dec));
        setRaError(raErr);
        setDecError(decErr);
    }

    public CoordinatesOffset getOffset() {
        return fOffset;
    }

    public void setOffset(CoordinatesOffset offset) {
        CoordinatesOffset old = fOffset;
        fOffset = offset;
        firePropertyChange(OFFSET_PROPERTY, old, fOffset);
    }

    /**
     * Returns the right-ascension component of proper motion error in degrees.
     **/
    public double getRaError() {
        return fRaError;
    }

    /**
     * Returns the declination component of proper motion error in degrees.
     **/
    public double getDecError() {
        return fDecError;
    }

    /**
     * Sets the proper motion RA error in degrees.
     **/
    public void setRaError(double err) {
        double hold = fRaError;
        fRaError = err;
        firePropertyChange(RAERROR_PROPERTY, new Double(hold), new Double(fRaError));
    }

    /**
     * Sets the proper motion Dec error in degrees.
     **/
    public void setDecError(double err) {
        double hold = fDecError;
        fDecError = err;
        firePropertyChange(DECERROR_PROPERTY, new Double(hold), new Double(fDecError));
    }

    /**
     * Computes a new position by taking an existing position and accounting
     * for proper motion over the specified number of years.
     * Uses the epoch of the oldPosition as the starting year.
     *
     * @param	oldPosition		start with this position
     * @param	endYear			compute new position for this year
     * @return					position for the new year
     **/
    public Coordinates computePosition(Coordinates oldPosition, double endYear) {
        return computePosition(oldPosition, endYear, oldPosition.getEpoch());
    }

    /**
     * Computes a new position by taking an existing position and accounting
     * for proper motion over the specified number of years.
     * Uses the startYear instead of the epoch of the oldPosition as the
     * starting year.
     *
     * @param	oldPosition		start with this position
     * @param	endYear			compute new position for this year
     * @param	startYear		assume oldPosition measured on this year
     * @return					position for the new year
     **/
    public Coordinates computePosition(Coordinates oldPosition, double endYear, double startYear) {
        return oldPosition.add(new CoordinatesOffset(
                (endYear - startYear) * getOffset().getRa(), (endYear - startYear) * getOffset().getDec()));
    }

    /**
     * Determines whether two proper motions are equal.
     **/
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof ProperMotion)) return false;

        ProperMotion that = (ProperMotion) obj;

        return (fRaError == that.fRaError) && (fDecError == that.fDecError);
    }

    /**
     * Returns a string representation of this proper motion's values.
     *
     * @return  a string representation of this proper motion's values
     **/
    public String toString() {
        return getOffset().getRa(Coordinates.ARCSEC) +
                ", " + getOffset().getDec(Coordinates.ARCSEC) +
                " +/- " + Coordinates.convert(fRaError, Coordinates.DEGREE, Coordinates.ARCSEC) +
                ", " + Coordinates.convert(fDecError, Coordinates.DEGREE, Coordinates.ARCSEC) +
                " arcsec/year";
    }
}
