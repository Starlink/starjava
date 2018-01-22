// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Functions for writing text to standard output.
 * They will cause output to be written to the console.
 * If you just want values to appear in the activation action logging window,
 * you can just use the expression to report on its own.
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
     * Outputs a string value to system output.
     *
     * @param  str  string value to output
     * @return  short report message
     */
    public static String print( String str ) {
        java.lang.System.out.println( str );
        return str;
    }

    /**
     * Outputs a numeric value to system output.
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
        final String str;
        if ( (double) (long) num == num ) {
            str = Long.toString( (long) num );
        }
        else if ( (double) (float) num == num ) {
            str = Float.toString( (float) num );
        }
        else {
            str = Double.toString( num );
        }
        return print( str );
    }
}
