/*
 * ESO Archive
 *
 * $Id: WorldCoords.java,v 1.8 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created, based on C++ version
 */

package jsky.coords;

import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * Class representing world coordinates (right-ascension, declination,
 * stored as J2000 internally).
 */
public class WorldCoords implements WorldCoordinates, Serializable {

    /** The RA coordinate */
    protected HMS ra;

    /** The DEC coordinate */
    protected DMS dec;

    /** Default constructor: initialize null coordinates. */
    public WorldCoords() {
        ra = new HMS();
        dec = new DMS();
    }

    /**
     * Initialize from ra (hours) and dec (degrees).
     *
     * @param ra The RA value in hours
     * @param dec The DEC value in degrees
     * @param equinox The equinox of the input coordinates.
     */
    public WorldCoords(HMS ra, DMS dec, double equinox) {
        this.ra = ra;
        this.dec = dec;
        init(equinox);
    }

    /**
     * Initialize from ra (hours) and dec (degrees) in J2000.
     *
     * @param ra The RA value in hours
     * @param dec The DEC value in degrees
     */
    public WorldCoords(HMS ra, DMS dec) {
        this(ra, dec, 2000.);
    }

    /**
     * Initialize from ra and dec (in degrees).
     *
     * @param ra The RA value in degrees
     * @param dec The DEC value in degrees
     * @param equinox The equinox of the input coordinates.
     */
    public WorldCoords(double ra, double dec, double equinox) {
        this.ra = new HMS(ra / 15);
        this.dec = new DMS(dec);
        init(equinox);
    }

    /**
     * Initialize from ra and dec (in degrees).
     *
     * @param ra The RA value in degrees
     * @param dec The DEC value in degrees
     * @param equinox The equinox of the input coordinates.
     */
    public WorldCoords(Double ra, Double dec, double equinox) {
        this.ra = new HMS(ra.doubleValue() / 15);
        this.dec = new DMS(dec.doubleValue());
        init(equinox);
    }

    /**
     * Initialize from ra and dec (in degrees J2000).
     *
     * @param ra The RA value in degrees
     * @param dec The DEC value in degrees
     */
    public WorldCoords(double ra, double dec) {
        this(ra, dec, 2000.);
    }

    /**
     * Initialize from an ra, dec point in the given equinox.
     *
     * @param p The RA,Dec point in degrees
     */
    public WorldCoords(Point2D.Double p, double equinox) {
        this(p.getX(), p.getY(), equinox);
    }

    /**
     * Initialize from an ra, dec point (in degrees J2000).
     *
     * @param p The RA,Dec point in degrees
     */
    public WorldCoords(Point2D.Double p) {
        this(p.getX(), p.getY(), 2000.);
    }

    /**
     * Initialize from ra and dec (in degrees J2000).
     *
     * @param ra The RA value in degrees
     * @param dec The DEC value in degrees
     */
    public WorldCoords(Double ra, Double dec) {
        this(ra.doubleValue(), dec.doubleValue(), 2000.);
    }

    /**
     * Initialize from RA hours, minutes, seconds and and DEC degress, minutes, seconds.
     *
     * @param rh RA hours
     * @param rm RA minutes
     * @param rs RA seconds
     * @param dd DEC degrees
     * @param dm DEC minutes
     * @param ds DEC seconds
     * @param equinox The equinox of the input coordinates.
     */
    public WorldCoords(double rh, int rm, double rs, double dd, int dm, double ds, double equinox) {
        ra = new HMS(rh, rm, rs);
        dec = new DMS(dd, dm, ds);
        init(equinox);
    }

    /**
     * Initialize from RA hours, minutes, seconds and and DEC degress, minutes, seconds
     * in J2000.
     *
     * @param rh RA hours
     * @param rm RA minutes
     * @param rs RA seconds
     * @param dd DEC degrees
     * @param dm DEC minutes
     * @param ds DEC seconds
     */
    public WorldCoords(double rh, int rm, double rs, double dd, int dm, double ds) {
        this(rh, rm, rs, dd, dm, ds, 2000.);
    }

    /**
     * Parse RA and DEC in string format.
     *
     * @param ra The RA value in the form "hh mm ss.s", "hh:mm:ss.s",
     *           or just "hh.hhh".
     *
     * @param dec The DEC value in the form "[+/-]dd mm ss.s", "[+/-]dd:mm:ss.s"
     *            or just "[+/-]dd.ddd".
     *
     * @param equinox The equinox of the input coordinates.
     */
    public WorldCoords(String ra, String dec, double equinox) {
        this.ra = new HMS(ra);
        this.dec = new DMS(dec);
        init(equinox);
    }

    /**
     * Parse RA and DEC in string format.
     *
     * @param ra The RA value in the form "hh mm ss.s", "hh:mm:ss.s",
     *           or just "hh.hhh".
     *
     * @param dec The DEC value in the form "[+/-]dd mm ss.s", "[+/-]dd:mm:ss.s"
     *            or just "[+/-]dd.ddd".
     *
     * @param equinox The equinox of the input coordinates.
     * @param hflag if true, always assume ra is in hours, otherwise, assume deg if
     *        ra is a decimal value.
     */
    public WorldCoords(String ra, String dec, double equinox, boolean hflag) {
        this.ra = new HMS(ra, hflag);
        this.dec = new DMS(dec);
        init(equinox);
    }

    /**
     * Parse RA and DEC in string format (assume J2000).
     *
     * @param ra The RA value in the form "hh mm ss.s", "hh:mm:ss.s",
     *           or just "hh.hhh".
     *
     * @param dec The DEC value in the form "[+/-]dd mm ss.s", "[+/-]dd:mm:ss.s"
     *            or just "[+/-]dd.ddd".
     */
    public WorldCoords(String ra, String dec) {
        this(ra, dec, 2000.);
    }

    /**
     * Parse RA and DEC in string format (assume J2000).
     *
     * @param ra The RA value in the form "hh mm ss.s", "hh:mm:ss.s",
     *           or just "hh.hhh".
     *
     * @param dec The DEC value in the form "[+/-]dd mm ss.s", "[+/-]dd:mm:ss.s"
     *            or just "[+/-]dd.ddd".
     *
     * @param hflag if true, always assume ra is in hours, otherwise, assume deg if
     *        ra is a decimal value.
     */
    public WorldCoords(String ra, String dec, boolean hflag) {
        this(ra, dec, 2000., hflag);
    }

    /** return the RA value */
    public HMS getRA() {
        return ra;
    }

    /** return the DEC value */
    public DMS getDec() {
        return dec;
    }

    /** return the RA value in deg */
    public double getRaDeg() {
        return ra.getVal() * 15.;
    }

    /** return the DEC value in deg */
    public double getDecDeg() {
        return dec.getVal();
    }

    /** return the X (ra) coordinate in deg */
    public double getX() {
        return ra.getVal() * 15.;
    }

    /** return the Y *dec) coordinate in deg */
    public double getY() {
        return dec.getVal();
    }

    /**
     * Return the coordinates as a string in h:m:s [+-]d:m:s format
     */
    public String toString() {
        return ra.toString() + ", " + dec.toString() + " J2000";
    }

    /**
     * Format RA and DEC in h:m:s [+-]d:m:s format in the given equinox
     * and return them as a 2 element String array.
     */
    public String[] format(double equinox) {
        String[] ar = new String[2];
        if (equinox == 2000.0) {
            ar[0] = ra.toString();
            ar[1] = dec.toString();
        }
        else {
            // make tmp copy and convert equinox before printing
            WorldCoords tmp = new WorldCoords(ra, dec);
            tmp.convertEquinox(2000.0, equinox);
            ar[0] = tmp.ra.toString();
            ar[1] = tmp.dec.toString();
        }
        return ar;
    }

    /**
     * Format RA and DEC in h:m:s [+-]d:m:s format in J2000
     * and return them as a 2 element String array.
     */
    public String[] format() {
        return format(2000.);
    }

    /**
     * Return RA and DEC in degrees as an array of 2 doubles {ra, dec} in the given equinox.
     */
    public double[] getRaDec(double equinox) {
        double[] ar = new double[2];
        if (equinox == 2000.0) {
            ar[0] = getRaDeg();
            ar[1] = getDecDeg();
        }
        else {
            // make tmp copy and convert equinox before printing
            WorldCoords tmp = new WorldCoords(ra, dec);
            tmp.convertEquinox(2000.0, equinox);
            ar[0] = tmp.getRaDeg();
            ar[1] = tmp.getDecDeg();
        }
        return ar;
    }


    /**
     * Computes distance and position angle solving a spherical
     * triangle (no approximations).
     * The arguments are the coordinates in decimal degrees and
     * the result is an array containing the 2 values.
     *
     * @param dra0 center RA
     * @param decd0 center DEC
     * @param dra point RA
     * @param decd point DEC
     *
     * @return An array of 2 doubles ar[2], where ar[0] is the distance in arcmin and
     *         ar[1] is the position angle phi in degrees (East of North).
     *
     * (Based on the C version from A. P. Martinez.)
     */
    public static double[] dispos(double dra0, double decd0, double dra, double decd) {
        double[] ar = new double[2];		// return array
        double alf, alf0, del, del0, phi;
        double sd, sd0, cd, cd0, cosda, cosd, sind, sinpa, cospa;
        double radian = 180. / Math.PI;

        // coo transformed in radiants
        alf = dra / radian;
        alf0 = dra0 / radian;
        del = decd / radian;
        del0 = decd0 / radian;

        sd0 = Math.sin(del0);
        sd = Math.sin(del);
        cd0 = Math.cos(del0);
        cd = Math.cos(del);
        cosda = Math.cos(alf - alf0);
        cosd = sd0 * sd + cd0 * cd * cosda;
        double dist = Math.acos(cosd);
        phi = 0.0;
        if (dist > 0.0000004) {
            sind = Math.sin(dist);
            cospa = (sd * cd0 - cd * sd0 * cosda) / sind;
            if (cospa > 1.0)
                cospa = 1.0;
            sinpa = cd * Math.sin(alf - alf0) / sind;
            phi = Math.acos(cospa) * radian;
            if (sinpa < 0.0)
                phi = 360.0 - phi;
        }
        dist *= radian;
        dist *= 60.0;
        if (decd0 == 90.)
            phi = 180.0;
        if (decd0 == -90.)
            phi = 0.0;

        ar[0] = dist;
        ar[1] = phi;
        return ar;
    }

    /**
     * return the distance between this position and the given one in arcmin
     * and also set the position angle
     *
     * @param pos The other point.
     *
     * @return An array of 2 doubles ar[2], where ar[0] is the distance in arcmin and
     *         ar[1] is the position angle in degrees (East of North).
     */
    public double[] dispos(WorldCoordinates pos) {
        return dispos(getRaDeg(), getDecDeg(), pos.getRaDeg(), pos.getDecDeg());
    }

    /**
     * Compute the distance in degrees between the two given coordinates.
     * (Based on the C version in D. Mink's wcssubs package.)
     */
    public static double wcsdist(double x1, double y1, double x2, double y2) {
        double xr1, xr2, yr1, yr2, w, diff, cosb;
        double[] pos1 = new double[3], pos2 = new double[3];
        int i;

        // Convert two vectors to direction cosines
        double deg = Math.PI / 180.;
        xr1 = x1 * deg;
        yr1 = y1 * deg;
        cosb = Math.cos(yr1);
        pos1[0] = Math.cos(xr1) * cosb;
        pos1[1] = Math.sin(xr1) * cosb;
        pos1[2] = Math.sin(yr1);

        xr2 = x2 * deg;
        yr2 = y2 * deg;
        cosb = Math.cos(yr2);
        pos2[0] = Math.cos(xr2) * cosb;
        pos2[1] = Math.sin(xr2) * cosb;
        pos2[2] = Math.sin(yr2);

        // Modulus squared of half the difference vector
        w = 0.0;
        for (i = 0; i < 3; i++)
            w = w + (pos1[i] - pos2[i]) * (pos1[i] - pos2[i]);

        w = w / 4.0;
        if (w > 1.0)
            w = 1.0;

        // Angle beween the vectors
        diff = 2.0 * Math.atan2(Math.sqrt(w), Math.sqrt(1.0 - w));

        // convert to deg and return
        return diff * 180. / Math.PI;
    }


    /**
     * Return the distance between this position and the given one in arcmin.
     * @param pos The other point.
     * @return The distance to the given point in arcmin.
     */
    public double dist(WorldCoords pos) {
        double[] ar = dispos(getRaDeg(), getDecDeg(), pos.getRaDeg(), pos.getDecDeg());
        return ar[0];
    }

    /**
     * Return the distance between this position and the given one in arcmin.
     * @param pos The other point.
     * @return The distance to the given point in arcmin.
     */
    public double dist(Coordinates pos) {
        return dist((WorldCoords) pos);
    }

    /**
     * static member to get the distance between 2 points in arcmin
     *
     * @param ra0 The first RA value
     * @param dec0 The first DEC value
     * @param ra1 The second RA value
     * @param dec1 The second DEC value
     *
     * @return The distance in arcmin.
     */
    static public double dist(double ra0, double dec0, double ra1, double dec1) {
        double[] ar = dispos(ra0, dec0, ra1, dec1);
        return ar[0];
    }

    /**
     * Given a radius in arcmin, return an array {pos1, pos2} with the 2 endpoints
     * that form a box with center at "this" position.
     *
     * @param radius The radius in arcmin.
     *
     * @return Array of 2 WorldCoords objects that are the endpoints of a box
     *         with the given radius and centered at "this" position.
     */
    public WorldCoordinates[] box(double radius) {
        WorldCoordinates[] ar = new WorldCoordinates[2]; // return array

        // get units in degrees
        double ra = this.ra.getVal(), dec = this.dec.getVal();
        radius /= 60.0;

        // get width of square
        double width = Math.sqrt(2.0 * radius * radius);
        double r1, r2, d1, d2, cosdec;

        d1 = dec - width / 2.0;
        if (d1 <= -90.0) {
            d1 = -90.0;
            d2 = dec + width / 2.0;
            r1 = 0.0;
            r2 = 24.0;
        }
        else {
            d2 = dec + width / 2.0;
            if (d2 >= 90.0) {
                d1 = dec - width / 2.0;
                d2 = 90.0;
                r1 = 0.0;
                r2 = 24.0;
            }
            else {
                if (dec > 0.0)
                    cosdec = Math.abs(Math.cos(d1 * Math.PI / 180.));
                else
                    cosdec = Math.abs(Math.cos(d2 * Math.PI / 180.));

                r1 = ra - width / 15. / 2 / cosdec;
                r2 = ra + width / 15. / 2 / cosdec;

                if (r1 < 0.0)
                    r1 += 24;
                if (r2 > 24.0)
                    r2 -= 24;
            }
        }

        // set the result array
        ar[0] = new WorldCoords(r1 * 15., d1);
        ar[1] = new WorldCoords(r2 * 15., d2);

        return ar;
    }


    /**
     * Given the endpoints of a box (pos1, pos2), return an array containing the
     * center ra, dec in degrees, as well as the width, height, and radius of the
     * box in arcmin.
     *
     * @param pos1 The first endpoint of the box.
     * @param pos1 The second endpoint of the box.
     *
     * @return An array of 5 doubles: {ra, dec, width, height, radius}
     *         where (ra, dec) gives the center position in deg,
     *         width and height are the size of the box in arcmin,
     *         and radius is the distance from the center to a corner in arcmin.
     */
    public double[] center(WorldCoordinates pos1, WorldCoordinates pos2) {
        double ar[] = new double[5]; // result

        // get center pos
        double ra1 = pos1.getRaDeg(), dec1 = pos1.getDecDeg();
        double ra2 = pos2.getRaDeg(), dec2 = pos2.getDecDeg();
        ar[0] = (ra1 + ra2) / 2.0;
        ar[1] = (dec1 + dec2) / 2.0;

        // get width and height of box
        ar[2] = (wcsdist(ra1, dec1, ra2, dec1) * 60.);
        ar[3] = (wcsdist(ra1, dec1, ra1, dec2) * 60.);

        // radius is half the distance from pos1 to pos2
        ar[4] = (wcsdist(ra1, dec1, ra2, dec2) * 60.) / 2.;

        return ar;
    }


    /** Returns the name of the coordinate system as a string. */
    public String getCoordinateSystemName() {
        return "wcs";
    }

    /**
     * Called by constructors to initialize the new object.
     * Set dec to include the sign and check the ra and dec ranges.
     * The equinox argument indicates the equinox of ra and dec.
     * Internally, ra and dec are converted if needed and stored in J2000.
     */
    private void init(double equinox) {
        checkRange();
        convertEquinox(equinox, 2000.);
    }

    /**
     * Convert the coordinates from fromEquinox to toEquinox.
     */
    private void convertEquinox(double fromEquinox, double toEquinox) {
        if (fromEquinox == toEquinox)
            return;

        double[] q0 = new double[2], q1 = new double[2];
        q0[0] = ra.getVal() * 15;	// hours to degrees
        q0[1] = dec.getVal();
        JPrec.prej_q(q0, q1, fromEquinox, toEquinox);
        ra = new HMS(q1[0] / 15);	// degrees to hours
        dec = new DMS(q1[1]);
    }

    /** check range of ra,dec values */
    private void checkRange() {
        double ra = this.ra.getVal(), dec = this.dec.getVal();

        if (ra < -0.001 || ra >= 25.0) {
            // System.out.println("XXX RA value " + ra + " out of range (0..24 hours)");
            // throw new IllegalArgumentException("RA value " + ra + " out of range (0..24 hours)");
        }

        if (dec < -90. || dec > 90.) {
            // System.out.println("XXX DEC value " + dec + " out of range (-90..+90 deg)");
            // throw new IllegalArgumentException("DEC value " + dec + " out of range (-90..+90 deg)");
        }
    }

    /**
     * Test cases
     */
    public static void main(String[] args) {
        WorldCoords c1 = new WorldCoords(49.95096, 41.51173);
        WorldCoords c2 = new WorldCoords(3, 19, 48.2304, 41, 30, 42.228);
        WorldCoords c3 = new WorldCoords(new HMS(3, 19, 48.2304), new DMS(41, 30, 42.228));
        WorldCoords c4 = new WorldCoords(new HMS(c1.getRA()), new DMS(c1.getDec()));
        WorldCoords c5 = new WorldCoords("3 19 48.2304", "+41 30 42.228", 2000.0);
        WorldCoords c6 = new WorldCoords("3:19:48.2304", "+41:30:42.228", 2000.0);
        WorldCoords c7 = new WorldCoords(Double.toString(49.95096 / 15.), "41.51173", 2000.0);

        System.out.println("these coords should all be the same (or very close):");
        System.out.println(c1);
        System.out.println(c2);
        System.out.println(c3);
        System.out.println(c4);
        System.out.println(c5);
        System.out.println(c6);
        System.out.println(c7);

        c1 = new WorldCoords(49.95096, -41.51173);
        c2 = new WorldCoords(3, 19, 48.2304, -41, 30, 42.228);
        c3 = new WorldCoords(new HMS(3, 19, 48.2304), new DMS(-41, 30, 42.228));
        c4 = new WorldCoords(new HMS(c1.getRA()), new DMS(c1.getDec()));
        c5 = new WorldCoords("3 19 48.2304", "-41 30 42.228", 2000.0);
        c6 = new WorldCoords("3:19:48.2304", "-41:30:42.228", 2000.0);
        c7 = new WorldCoords(Double.toString(49.95096 / 15.), "-41.51173", 2000.0);

        System.out.println("Here is the same with negative dec:");
        System.out.println(c1);
        System.out.println(c2);
        System.out.println(c3);
        System.out.println(c4);
        System.out.println(c5);
        System.out.println(c6);
        System.out.println(c7);

        WorldCoords c8 = new WorldCoords("3:19", "+41:30", 2000.0);
        WorldCoords c9 = new WorldCoords("3", "+41", 2000.0);
        System.out.println("And with missing minutes, ... seconds, ...:");
        System.out.println(c8);
        System.out.println(c9);

        // test the "box" method (get 2 points given a radius)
        WorldCoordinates c10 = new WorldCoords("03:19:48.243", "+41:30:40.31"), c11, c12;
        WorldCoordinates[] ar1 = c10.box(7.05);
        c11 = ar1[0];
        c12 = ar1[1];
        System.out.println("box of radius 7.05 with center at (03:19:48.243, +41:30:40.31):");
        System.out.println(c11);
        System.out.println(c12);

        // test values at or near 0,0
        WorldCoords c13 = new WorldCoords("0", "+41:30:40.31");
        System.out.println("With ra = 0.0: " + c13 + " ("
                + c13.getRA().getVal() + ", " + c13.getDec().getVal() + ")");

        WorldCoords c14 = new WorldCoords("0.0", "-0.0");
        System.out.println("With ra = 0.0, dec = -0.0: " + c14 + " ("
                + c14.getRA().getVal() + ", " + c14.getDec().getVal() + ")");

        WorldCoords c15 = new WorldCoords("0:0:1", "-0:1:1");
        System.out.println("With ra = 0:0:1, dec = -0:1:1: " + c15 + " ("
                + c15.getRA().getVal() + ", " + c15.getDec().getVal() + ")");

        // test conversion between h:m:s and deg and back
        WorldCoords c16 = new WorldCoords("22:45:22.74", "-39:34:14.63");
        System.out.println("test conversion between h:m:s and deg and back");
        System.out.println("22:45:22.74 -39:34:14.63 == " + c16 + " == " + c16.toString());

        String[] ar2 = c16.format();
        System.out.println(" == " + ar2[0] + " " + ar2[1]);
        WorldCoords c17 = new WorldCoords(ar2[0], ar2[1]);
        System.out.println(" == " + c17);

        // test equinox conversion
        WorldCoords c18 = new WorldCoords(0.0, 0.0, 1950.);
        String[] ar3 = c18.format(1950.);
        System.out.println("00:00:00 B1950 == " + c18 + " J2000 == " + ar3[0] + " " + ar3[1] + " B1950");
    }
}
