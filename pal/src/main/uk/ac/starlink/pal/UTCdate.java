/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** UTC Date
 */
public class UTCdate {
    private double Date, Deriv;
    boolean hasDeriv = false;

/** UTC Date
 */
    public UTCdate ( double date, double derivative ) {
        Date = date; hasDeriv = true; Deriv = derivative;
    }

/** UTC Date
 */
    public UTCdate ( double date ) {
        Date = date; hasDeriv = false;
    }
   
/** UTC Date
 */
    public double getDate() { return Date; }

/** UTC Date
 */
    public double getDeriv() { return ( hasDeriv ? Deriv : 0.0 ); }

/** Get String representation
 */
    public String toString() {
        String s = Double.toString( Date );
        if ( hasDeriv ) s = s.concat( " (" + Deriv + ") ");
        return s;
    }
}
