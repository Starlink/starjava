// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.topcat.func;

/**
 * Specialist functions for use with data from the the 2QZ survey.
 * Spectral data are taken directly from the 2QZ web site at
 * <code>http://www.2dfquasar.org/"</code>.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Sep 2004
 */
public class TwoQZ {

    /** String which is prepended to the object NAME for the FITS file URL. */
    public static final String FITS_BASE = "http://www.2dfquasar.org/fits/";

    /** String which is appended to the object NAME for the FITS file URL. */
    public static final String FITS_TAIL = ".fits.gz";

    /**
     * Private constructor prevents instantiation.
     */
    private TwoQZ() {
    }

    /**
     * Displays all the spectra relating to a 2QZ object in an external
     * viewer (SPLAT).
     *
     * @param   name  object name (NAME column)
     * @param   nobs  number of observations to display (NOBS column)
     * @return  short log message
     */
    public static String spectra2QZ( String name, int nobs ) {
        int rah = Integer.parseInt( name.substring( 1, 3 ) );
        String[] locs = new String[ nobs ];
        String base = FITS_BASE + getSubdir( name );
        for ( int i = 0; i < nobs; i++ ) {
            locs[ i ] = base + name + (char) ( 'a' + i ) + FITS_TAIL;
        }
        return Display.splatMulti( locs );
    }

    private static String getSubdir( String name ) {
        int rah = Integer.parseInt( name.substring( 1, 3 ) );
        int rah1 = rah + 1;
        return "ra" + format2( rah ) + '_' + format2( rah1 ) + '/';
    }

    private static String format2( int num ) {
        String out = Integer.toString( num );
        return out.length() == 2 ? out
                                 : "0" + out.charAt( 0 );
    }

}
