/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** Modified Julian Date
 */
public class mjDate {
    private int year, month, day, dayinyear;
    private double fraction, djm;
    private int mtab[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

/** Modified Julian Date
 *  @param yr Year
 *  @param mn Month
 *  @param d  Day
 */
    public mjDate ( int yr, int mn, int d ) throws palError {
/* Validate year */
        if ( yr < -4711 ) throw new palError( 1, "Invalid Year" );
/* Validate month */
        if ( ( mn < 1 ) || ( mn > 12 ) ) throw new palError( 2, "Invalid Month" );
/* Allow for (Gregorian) leap year */
        mtab[1] = ( ( ( yr % 4 ) == 0 ) &&
             ( ( ( yr % 100 ) != 0 ) || ( ( yr % 400 ) == 0 ) ) ) ?
             29 : 28;
/* Validate day */
        if ( d < 1 || d > mtab[mn-1] ) throw new palError( 3, "Invalid Day" );

/* Calculate day in year */        
        for ( int i=0, dayinyear = d; i < mn; i++ ) dayinyear += mtab[i];
        year = yr; month = mn; day = d;
        fraction = 0.0; djm = 0.0;
    }

/** Modified Julian Date
 *  @param yr Year
 *  @param mn Month
 *  @param d  Day
 *  @param fract Fraction of day
 */
    public mjDate ( int yr, int mn, int d, double fract ) throws palError {
/* Validate year */
        if ( yr < -4711 ) throw new palError( 1, "Invalid Year" );
/* Validate month */
        if ( ( mn < 1 ) || ( mn > 12 ) ) throw new palError( 2, "Invalid Month" );
/* Validate Fraction */
        if ( ( fract < 0.0 ) || ( fract > 1.0 ) )
                                         throw new palError( 4, "Invalid Fraction" );
/* Allow for (Gregorian) leap year */
        mtab[1] = ( ( ( yr % 4 ) == 0 ) &&
             ( ( ( yr % 100 ) != 0 ) || ( ( yr % 400 ) == 0 ) ) ) ?
             29 : 28;
/* Validate day */
        if ( d < 1 || d > mtab[mn-1] ) throw new palError( 3, "Invalid Day" );

/* Calculate day in year */        
        for ( int i=0, dayinyear = d; i < mn; i++ ) dayinyear += mtab[i];
        year = yr; month = mn; day = d;
        fraction = fract; djm = 0.0;
    }

/** Modified Julian Date
 *  @param yr Year
 *  @param diy Day in year
 */
    public mjDate ( int yr, int diy ) throws palError {
/* Validate year */
        if ( yr < -4711 ) throw new palError( 1, "Invalid Year" );
/* Allow for (Gregorian) leap year */
        mtab[1] = ( ( ( yr % 4 ) == 0 ) &&
             ( ( ( yr % 100 ) != 0 ) || ( ( yr % 400 ) == 0 ) ) ) ?
             29 : 28;
/* Validate month */
        int days = 0;
        for ( int m=0; m < 12; m++ ) {
            days += mtab[m];
            if ( diy <= days ) {
                days -= mtab[m];
                month = m+1;
                break;
            }
        }
        if ( diy > days ) throw new palError( 3, "Invalid day" );
        day = diy - days;
        year = yr; dayinyear = diy;
        fraction = 0.0; djm = 0.0;
    }

/** Get year
 *  @return Year
 */
    public int getYear() { return year; }

/** Get month
 *  @return Month
 */
    public int getMonth() { return month; }

/** Get day
 *  @return Day
 */
    public int getDay() { return day; }

/** Get fraction as integer number of places.
 *  @return fraction * 10^n
 */
    public int getFraction( int n ) { return (int) (fraction*Math.pow(10,n) ); }

/** Get fraction
 *  @return Fraction of day
 */
    public double getFraction( ) { return fraction; }

/** Get day in year
 *  @return Day in year
 */
    public int getDayinYear() { return dayinyear; }

/** return date as string to required precision
 *  @param n Number of decimal places in fraction
 *  @return Date in form YYYY MM DD.F
 */
    public String toString( int n ) {
        NumberFormat frac = NumberFormat.getNumberInstance();
        frac.setMaximumIntegerDigits(0);
        frac.setMinimumIntegerDigits(0);
        frac.setMaximumFractionDigits(n);
        frac.setMinimumFractionDigits(n);
        return year + " " + month + " " + day + frac.format( fraction );
    }

/** return date as string
 *  @return Date in form dd/mm/yyyy
 */
    public String toString() {
        NumberFormat leadzero = NumberFormat.getNumberInstance();
        leadzero.setMinimumIntegerDigits(2);
        return day + "/" + leadzero.format(month) + "/" + year;
    }
}
