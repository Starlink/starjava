/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** A String with positional information
 */
public class palString {
    private String string;
    private int pos;
    private int flag;
    private int digit;

/** Create a new string
 *  @param s The text to store
 */
    public palString ( String s ) {
        string = s; pos = 0; flag = 0; digit = 0;
    }

/** Get the string
 *  @return The text of the string
 */
    public String getString() { return string; }

/** Get the next character of the string
 *  @return The next character
 */
    public char getNextChar( ) { return string.charAt( pos++ ); }

/** Move the string pointer back one character
 */
    public void backChar( ) { pos--; }

/** Move the string pointer forward one character
 */
    public void incrChar( ) { pos++; }

/** Get the character at a set position in the string
 *  @param n The position of the character to get
 *  @return The character at position n
 */
    public char getChar( int n ) { return string.charAt( n-1 ); }

/** Get the character at the current position in the string
 *  @return The current character
 */
    public char getChar( ) { return string.charAt( pos ); }

/** Get the last character
 *  @return The previous character
 */
    public char getlastChar( ) { return string.charAt( --pos ); }

/** Get the current character position
 *  @return The current character position (starting at 1)
 */
    public int getPos() { return pos+1; }

/** Set the current character position
 *  @param n The current character position (starting at 1)
 */
    public void setPos( int n ) { pos = n-1; }

/** Set the Digit Flag
 *  @param n The digit flag
 */
    public void setDigit( int n ) { digit = n; }

/** Get the Digit Flag
 *  @return The digit flag
 */
    public int getDigit( ) { return digit; }

/** Set the Status flag
 *  @param n Flag value
 */
    public void setFlag( int n ) { flag = n; }

/** Get the Status flag
 *  @return Flag value
 */
    public int getFlag( ) { return flag; }

/** Get the length of the string
 *  @return String length
 */
    public int length( ) { return string.length(); }

/** Get the string
 *  @return The string
 */
    public String toString() { return string; }
}

