// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Functions for general display of spectra in a window.
 * Display is currently done using the SPLAT program, if available
 * (<a href="http://www.starlink.ac.uk/splat/">http://www.starlink.ac.uk/splat/</a>).
 * Recognised spectrum formats include 1-dimensional FITS arrays and NDF files.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Oct 2004
 */
public class Spectrum {

    /**
     * Private constructor prevents instantiation.
     */
    private Spectrum() {
    }

    /**
     * Displays the file at the given location in a spectrum viewer.
     *
     * @param  label identifies the window in which the spectrum will
     *         be displayed
     * @param  location   spectrum location - may be a filename or URL
     * @return  short log message
     */
    public static String displaySpectrum( String label, String location ) {
        if ( TopcatUtils.canSplat() ) {
            return Splat.splat( label, location );
        }
        else {
            return "No spectrum viewer available.";
        }
    }

    /**
     * Displays the files at the given locations in a spectrum viewer.
     * Each file represents a single spectrum, but they will be displayed
     * within the same viewer window.
     *
     * @param  label identifies the window in which the spectrum will
     *         be displayed
     * @param  locations array of spectrum locations - may be filenames or URLs
     * @return  short log message
     */
    public static String displaySpectra( String label, String[] locations ) {
        if ( TopcatUtils.canSplat() ) {
            return Splat.splatMulti( label, locations );
        }
        else {
            return "No spectrum viewer available";
        }
    }

}
