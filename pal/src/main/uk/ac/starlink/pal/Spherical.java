/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/**
 * Point in Spherical coordinates (with radial velocity)
 */
public class Spherical {
    private double lon, lat, rad;
    private boolean vel;
    private double lond, latd, radd;

/**
 * Point in Spherical coordinates
 * @param a Longitude
 * @param b Latitude
 * @param c Radial
 */
    public Spherical ( double a, double b, double c ) {
        lon = a; lat = b; rad = c;
        vel = false;
    }
/**
 * Point in Spherical coordinates and Radial Veocity
 * @param a Longitude
 * @param b Latitude
 * @param c Radial
 * @param aa Velocity component along longitude
 * @param bb Velocity component along latitude
 * @param cc Velocity component along radial
 */
    public Spherical ( double a, double b, double c,
                   double aa, double bb, double cc ) {
        lon = a; lat = b; rad = c;
        vel = true;
        lond = aa; latd = bb; radd = cc;
    }

/**
 * Get Longitude
 * @return Longitude
 */
    public double getLong() { return lon; }

/**
 * Get Latitude
 * @return Latitude
 */
    public double getLat() { return lat; }

/**
 * Get radial component
 * @return Radial component
 */
    public double getRadial() { return rad; }

/**
 * Get longitude velocity component
 * @return Longitude velocity component
 */
    public double getLongDeriv() { return lond; }

/**
 * Get latitude velocity component
 * @return Latitude velocity component
 */
    public double getLatDeriv() { return latd; }

/**
 * Get radial velocity component
 * @return Radial velocity component
 */
    public double getRadialDeriv() { return radd; }

/**
 * Get as String
 * @return Point (and velocity) expressed as [a, b, c] ( [aa, bb, cc] )
 */
    public String toString() {
        String s = "[" + lon + "," + lat + "," + rad + "]";
        if ( vel ) {
            s.concat ( " [" + lond + "," + latd + "," + radd + "]" );
        }
        return s;
    }
}
