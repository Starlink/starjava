/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;


/** Cartesian Point in 2D or 3D space
 */
public class Cartesian {
    private double x, y, z;
    private boolean threed, vel;
    private double xvel, yvel, zvel;

/** Cartesian Point in 2D space
 *  @param a x component
 *  @param b y component
 */
    public Cartesian ( double a, double b ) {
        x = a; y = b; z = 0.0;
        vel = false; threed = false;
    }

/** Cartesian Point in 3D space 
 *  @param a x component
 *  @param b y component
 *  @param c z component
 */
    public Cartesian ( double a, double b, double c ) {
        x = a; y = b; z = c;
        vel = false; threed = true;
    }

/** Cartesian Point in 3D space and its derivitive 
 *  @param a x component
 *  @param b y component
 *  @param c z component
 *  @param aa first derivitive of x
 *  @param bb first derivitive of y
 *  @param cc first derivitive of z
 */
    public Cartesian ( double a, double b, double c,
                       double aa, double bb, double cc ) {
        x = a; y = b; z = c;
        vel = true; threed = true;
        xvel = aa; yvel = bb; zvel = cc;
    }

/** Get the x component
 *  @return The x component
 */
    public double getX() { return x; }

/** Get the y component 
 *  @return The y component
 */
    public double getY() { return y; }

/** Get the z component 
 *  @return The z component
 */
    public double getZ() { return z; }

/** Get the first derivitive of x ( velocity) 
 *  @return The x velocity component
 */
    public double getXvel() { return xvel; }

/** Get the first derivitive of y ( velocity) 
 *  @return The t velocity component
 */
    public double getYvel() { return yvel; }

/** Get the first derivitive of z ( velocity) 
 *  @return The z velocity component
 */
    public double getZvel() { return zvel; }

/** Get the value of the point as a string of the form (x,y,z)
 *  and the velocity (if set) in the form (aa,bb,cc) 
 */
    public String toString() {
        String result = "(" + x + "," + y;
        if ( threed ) result.concat( "," + z );
        result.concat ( ")" );
        if ( vel ) {
            result.concat ( " (" + xvel + "," + yvel + "," + zvel + ")" );
        }
        return result;
    }
}
