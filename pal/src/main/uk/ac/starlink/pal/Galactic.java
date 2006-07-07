/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** Galactic longitude and latitude (radians)
 */
public class Galactic {
    private double longitude, latitude;

/** Galactic longitude and latitude
 *  @param a Longitude in radians
 *  @param b Latitude in radians
 */
    public Galactic ( double a, double b ) {
       longitude = a; latitude = b;
    }

/** Get the Longitude
 *  @return Longitude in radians
 */
    public double getLongitude() { return longitude; }

/** Get the Latitude
 *  @return Latitude in radians
 */
    public double getLatitude() { return latitude; }
/** Get the text form of the Galactic in the form [long,lat]
 */
    public String toString() { return "[" + longitude + "," + latitude + "]"; }
}
