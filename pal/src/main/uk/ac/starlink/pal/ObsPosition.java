/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** Position of Observer
 */
public class ObsPosition {
    private double Longitude, Latitude, Height, Rad;
    private boolean Radial = false; 

/** Position of Observer
 *  @param lon    Longitude
 *  @param lat    Latitude
 *  @param height Height
 */
    public ObsPosition ( double lon, double lat, double height ) {
        Longitude = lon; Latitude = lat; Height = height;
    }   

/** Position of Observer
 *  @param lon    Longitude
 *  @param lat    Latitude
 *  @param height Height
 *  @param rad    Radial velocity
 */
    public ObsPosition ( double lon, double lat, double height, double rad ) {
        Longitude = lon; Latitude = lat; Height = height;
        Radial = true; Rad = rad;
    }   

/** Get the longitude
 *  @return Longitude
 */
    public double getLongitude() { return Longitude; }

/** Get the latitude
 *  @return Latitude
 */
    public double getLatitude() { return Latitude; }

/** Get the Height
 *  @return Height
 */
    public double getHeight() { return Height; }

/** Get the radial velocity
 *  @return Radial velocity
 */
    public double getRad() { return Rad; }

/** Get the Observers position as a String
 */
    public String toString() {
        String s = Longitude + "," + Latitude + " " + Height;
        if ( Radial ) s = s.concat( " (" + Rad + ")" );
        return s;
    }
}
