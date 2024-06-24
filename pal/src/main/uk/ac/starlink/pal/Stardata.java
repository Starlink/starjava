/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** Various characteristics of an Observation
 */
public class Stardata {
    private AngleDR angle;
    private double motion[];
    private double parallax, radv;

/** Various characteristics of an Observation
 *  @param a Angle (&alpha;,&delta;)
 *  @param pm Proper motion
 *  @param p Parallax
 *  @param rv Radial velocity
 */
    public Stardata ( AngleDR a, double pm[], double p, double rv ) {
        angle = a; motion = pm; parallax = p; radv = rv;
    }

/** Get the Angle
 *  @return Angle (&alpha;,&delta;)
 */
    public AngleDR getAngle() { return angle; }

/** Get the Proper motion
 *  @return Proper motion
 */
    public double[] getMotion() { return motion; }

/** Get the Parallax
 *  @return Parallax
 */
    public double getParallax() { return parallax; }

/** Get the radial velocity
 *  @return Radial velocity
 */
    public double getRV() { return radv; }

/** Get the current values as a string
 *  @return Angle, Proper motion, parallax and radial velocity
 */
    public String toString() {
        return angle + " " + motion + " " + parallax + " " + radv;
    }
}

