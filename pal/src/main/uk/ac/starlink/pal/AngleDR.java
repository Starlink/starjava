/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;


/** Angle consisting of (&alpha;, &delta;) pairs in Radians
 */
public class AngleDR {
    private double alpha, delta, rad;
    private boolean radial;
    private static NumberFormat frac = NumberFormat.getNumberInstance();

/** Angle consisting of (&alpha;, &delta;) pair and optional radial.
 *  @param a alpha
 *  @param d delta
 *  @param r radial
 */
    public AngleDR ( double a, double d, double r ) {
        alpha = a; delta = d; radial = true; rad = r;
    }

/** Angle consisting of (alpha, delta).
 *  @param a Alpha
 *  @param d Delta
 */
    public AngleDR ( double a, double d ) {
        alpha = a; delta = d; radial = false;
    }

/** Set the first component of the angle
 *  @param a &alpha;
 */
    public void setAlpha( double a ) { alpha = a; }

/** Set the second component of the angle
 *  @param d &delta;
 */
    public void setDelta( double d ) { delta = d; }

/** Set the Radial component of the angle
 *  @param r Radial
 */
    public void setRad( double r ) { rad = r; radial = true; }

/** Get the first component of the angle
 *  @return &alpha;
 */
    public double getAlpha() { return alpha; }

/** Get the second component of the angle
 *  @return &delta;
 */
    public double getDelta() { return delta; }

/** Get the Radial component of the angle
 *  @return Radial
 */
    public double getRadial() { return ( radial? rad : 0.0 ); }

/** Get string representation of Angle to n decimal places
 *  as two (or three) numbers
 *  @param n Number of decimal places
 *  @return String representation
 */
    public String toString( int n ) {
        String result;
        frac.setMaximumFractionDigits( n );
        frac.setMinimumFractionDigits( n );
        result = frac.format( alpha ) +  " " + frac.format( delta );
        if ( radial ) result.concat( " " + frac.format( rad ) );
        return result;
    }

/** Get string representation of Angle as bracketed pair (or triplet)
 *  @return String representation
 */
    public String toString() {
        String result;
        result = "(" + alpha + "," + delta;
        if ( radial ) result.concat( ", " + rad );
        result.concat( ")" );
        return result;
    }
}
