// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Functions for simple logging output.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    2 Sep 2004
 */
public class Output {

    /**
     * Private constructor prevents instantiation.
     */
    private Output() {
    }

    /**
     * Outputs a string value to the user log.
     *
     * @param  str  string value to output
     * @return  short report message
     */
    public static String print( String str ) {
        return str;
    }

    /**
     * Outputs a numeric value to the user log.
     *
     * @param   num  numeric value to output
     * @return  short report message
     */
    public static String print( double num ) {

        /* Since this method is declared to take double, any numeric 
         * argument can be widened to fit in it, so we don't need to 
         * clutter up the public interface with lots of overloaded print()
         * methods.  The downcasting is done so that you don't get
         * extra decimal places when they didn't ought to be there. */
        if ( (double) (long) num == num ) {
            return Long.toString( (long) num );
        }
        else if ( (double) (float) num == num ) {
            return Float.toString( (float) num );
        }
        else {
            return Double.toString( num );
        }
    }
}
