/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/**
 * Angle defined by Hour, minute, second and fraction of a second
 */
public class palTime {
    private int hour, min, sec;
    private double fraction;
    private char sign;
    private static NumberFormat frac = NumberFormat.getNumberInstance();
    private static NumberFormat lead = NumberFormat.getNumberInstance();

/**
 *  Angle defined by Hour, minute, second, fraction and sign
 *  @param h Hour
 *  @param m Minute
 *  @param s Second
 *  @param frac Fraction of second
 *  @param c Sign ('+' or '-')
 */
    public palTime ( int h, int m, int s, double frac, char c ) {
        hour = h; min = m; sec = s; fraction = frac; sign = c;
    }

/**
 * Angle defined by Hour, minute, second, fraction
 *  @param h Hour
 *  @param m Minute
 *  @param s Second
 *  @param frac Fraction of second
 */
    public palTime ( int h, int m, int s, double frac ) {
        hour = h; min = m; sec = s; fraction = frac;
        sign = ( hour >= 0 ? '+' : '-' );
        if ( hour == 0 && min < 0 ) sign = '-';
        if ( hour == 0 && min == 0 && sec < 0) sign = '-';
        if ( hour == 0 && min == 0 && sec == 0 && frac < 0.0 )  sign = '-';
    }

/**
 * Angles defined by Hour, minute, second
 *  @param h Hour
 *  @param m Minute
 *  @param s Second
 */
    public palTime ( int h, int m, int s ) {
        hour = h; min = m; sec = s; fraction = 0.0;
        sign = ( hour > 0 ? '+' : '-' );
        if ( hour == 0 && min < 0 ) sign = '-';
        if ( hour == 0 && min == 0 && sec < 0) sign = '-';
    }

/**
 *  Get the hour
 *  @return Hour
 */
    public int getHour() { return hour; }

/**
 *  Get the minute
 *  @return Minute
 */
    public int getMin() { return min; }

/**
 *  Get the second
 *  @return Second
 */
    public int getSec() { return sec; }

/**
 *  Get the fraction of a second
 *  @return Fraction
 */
    public double getFraction() { return fraction; }

/**
 *  Get the fraction of a second to a set precision
 *  @param n Number of decimal places
 *  @return Fraction
 */
    public int getFraction(int n) { return (int)(fraction*Math.pow(10,n) ); }

/**
 *  Get the sign
 *  @return Sign of the Angle
 */
    public char getSign() { return sign; }

/**
 *  Get the sign as a String
 *  @return Sign of the Angle
 */
    public String printSign( ) { return "" + sign; }

/**
 *  Get the Angle as a String to a set precision
 *  @param n Number of decimal places in fraction
 *  @return The angle as a String in the form 'hh mm ss.f'
 */
    public String toString( int n ) {
        int h = hour, m = min, s = sec; 
        lead.setMinimumIntegerDigits(2);
        frac.setMaximumIntegerDigits(0);
        frac.setMaximumFractionDigits(n);
        frac.setMinimumFractionDigits(n);
        String sf = frac.format(fraction);
        if ( n == 0 ) sf = "";
        else if ( sf.equals( frac.format(0) ) ) {
            if ( fraction > 0.5 ) {
                s++;
                if ( s == 60 ) {
                    s = 0; m++;
                    if ( m == 60 ) {
                        m = 0; h++;
                        if ( h == 24 ) h = 0;
                    }
                }
            }
        }
        return h + " " + lead.format(m) + " " + lead.format(s) + sf;
    }

/**
 *  Get the Angle as a String
 *  @return The angle as a String in the form 'hh:mm:ss.ff'
 */
    public String toString() {
        frac.setMaximumIntegerDigits(0);
        lead.setMinimumIntegerDigits(2);
        return hour + ":" + lead.format(min) + ":" + lead.format(sec) +
          frac.format(fraction);
    }
}
