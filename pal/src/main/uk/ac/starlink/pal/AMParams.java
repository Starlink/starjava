/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;
import java.math.*;

/** Star-independent mean-to-apparent parameters
 */
public class AMParams {
    private double timeint;
    private double baryc[];
    private double helioc[];
    private double grad;
    private double ev[];
    private double sqroot;
    private double precession[][];

/** Initialise mean-to-apparent parameters
 */
    AMParams ( ) {
        timeint = 0.0;
        baryc = new double[3];
        helioc = new double[3];
        grad = 0.0;
        ev = new double[3];
        sqroot = 0.0;
        precession = new double[3][3];
    }

/** Set star-independent mean-to-apparent parameters
 *  @param tim      Time interval for proper motion (Julian years)
 *  @param barypos  Barycentric position of the Earth (AU)
 *  @param heliopos Heliocentric direction of the Earth ( unit vector)
 *  @param sun      Gravitational radius of the Sun) x 2 / (Sun-Earth distance)
 *  @param v        Barycentric Earth velocity in units of c
 *  @param sqrt     Square root of ( 1 - |v x v| )
 *  @param precess  Precession/nutation 3x3 matrix
 */
    AMParams ( double tim, double barypos[], double heliopos[], double sun,
            double v[], double sqrt, double precess[][] ) {
       timeint = tim;
       baryc = barypos;
       helioc = heliopos;
       grad = sun;
       ev = v;
       sqroot = sqrt;
       precession = precess;
    }

/** Get time interval for proper motion
 *  @return Time interval (Julian years)
 */
    public double getTimeint() { return timeint; }

/** Get barycentric position of the Earth
 *  @return Barycentric position of the Earth (AU) [3]
 */
    public double[] getBary() { return baryc; }

/** Get heliocentric direction of the Earth
 *  @return Heliocentric direction of the Earth (unit vector) [3]
 */
    public double[] getHelio() { return helioc; }

/** Get gravitational radius of Sun
 *  @return (Gravitational radius of Sun) x 2 / (Sun-earth distance)
 */
    public double getGrad() { return grad; }

/** Get barycentric Earth velocity
 *  @return Barycentric Earth velocity in units of c [3]
 */
    public double[] getEarthv() { return ev; }

/** Get square root of ( 1 - |v x v| )
 *  @return Square root of ( 1 - |v x v| )
 */
    public double getRoot() { return sqroot; }

/** Get precession/nutation matix
 *  @return Precession/nutation matix [3][3]
 */
    public double[][] getPrecess() { return precession; }

/** set time interval for proper motion
 *  @param t Time interval (Julian years)
 */
    public void setTimeint( double t ) { timeint = t; }

/** Set barycentric position of the Earth
 *  @param b[3] Barycentric position of the Earth (AU)
 */
    public void setBary( double b[] ) { baryc = b; }

/** Set heliocentric direction of the Earth
 *  @param h[3] Heliocentric direction of the Earth (unit vector)
 */
    public void setHelio( double h[] ) { helioc = h; }

/** Set gravitational radius of Sun
 *  @param g (Gravitational radius of Sun) x 2 / (Sun-earth distance)
 */
    public void setGrad( double g ) { grad = g; }

/** set barycentric Earth velocity
 *  @param bev[3] barycentric Earth velocity in units of c
 */
    public void setEarthv( double bev[] ) {
        ev = bev;
    }

/** Set square root of ( 1 - |v x v| )
 *  @param sqrt Square Root of ( 1 - |v x v| )
 */
    public void setRoot( double sqrt ) { sqroot = sqrt; }

/** Get precession/nutation matix
 *  @param p[3][3] Precession/nutation matix
 */
    public void setPrecess( double p[][] ) { precession = p; }

/** Get string representation apparent parameters
 *  @return String representation
 */
    public String toString() {
        return "Parameters: " + timeint + " (" + baryc.length + ") (" +
                helioc.length + ") " + grad + " ("  + ev.length + ") " +
                sqroot+ " ("  + precession.length + ")";
    }
}
