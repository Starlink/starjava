// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.cone.Coverage.Amount;
import uk.ac.starlink.ttools.cone.MocCoverage;
import uk.ac.starlink.ttools.cone.UrlMocCoverage;
import uk.ac.starlink.util.URLUtils;

/**
 * Functions related to coverage and footprints.
 *
 * <p>One coverage standard is Multi-Order Coverage maps, described at
 * <a href="http://www.ivoa.net/Documents/MOC/"
 *         >http://www.ivoa.net/Documents/MOC/</a>.
 * MOC positions are always defined in ICRS equatorial coordinates.
 *
 * <p>MOC locations may be given as either the filename or the URL of
 * a MOC FITS file.  Alternatively, they may be the identifier of a VizieR
 * table, for instance "<code>V/139/sdss9</code>" (SDSS DR9).
 * A list of all the MOCs available from VizieR can
 * currently be found at
 * <a href="http://alasky.u-strasbg.fr/footprints/tables/vizier/"
 *         >http://alasky.u-strasbg.fr/footprints/tables/vizier/</a>.
 * You can search for VizieR table identifiers from the
 * VizieR web page
 * (<a href="http://vizier.u-strasbg.fr/"
 *          >http://vizier.u-strasbg.fr/</a>);
 * note you must use
 * the <em>table</em> identifier (like "<code>V/139/sdss9</code>")
 * and not the <em>catalogue</em> identifier (like "<code>V/139</code>").
 *
 * @author   Mark Taylor
 * @since    29 May 2012
 */
public class Coverage {

    private static final Map<String,MocCoverage> mocMap_ =
        new HashMap<String,MocCoverage>();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.func" );

    /**
     * Private constructor prevents instantiation.
     */
    private Coverage() {
    }

    /**
     * Indicates whether a given sky position falls strictly within a given MOC
     * (Multi-Order Coverage map).
     * If the given <code>mocLocation</code> value does not represent
     * a MOC (for instance no file exists or the file is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be false.
     *
     * @param  mocLocation  location of a FITS MOC file:
     *                      a filename, a URL, or a VizieR table name
     * @param  ra     ICRS right ascension in degrees
     * @param  dec    ICRS declination in degrees
     * @return   true iff the given position falls within the given MOC
     */
    public static boolean inMoc( String mocLocation, double ra, double dec ) {
        return nearMoc( mocLocation, ra, dec, 0 );
    }

    /**
     * Indicates whether a given sky position either falls within,
     * or is within a certain distance of the edge of,
     * a given MOC (Multi-Order Coverage map).
     * If the given <code>mocLocation</code> value does not represent
     * a MOC (for instance no file exists or the file is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be false.
     *
     * @param  mocLocation  location of a FITS MOC file:
     *                      a filename, a URL, or a VizieR table name
     * @param  ra     ICRS right ascension in degrees
     * @param  dec    ICRS declination in degrees
     * @param  distanceDeg   permitted distance from MOC boundary in degrees
     * @return   true iff the given position is within <code>distance</code>
     *           degrees of the given MOC
     */
    public static boolean nearMoc( String mocLocation, double ra, double dec,
                                   double distanceDeg ) {
        MocCoverage cov = getMocCoverage( mocLocation );

        /* Note we fail to false not true here in case of no data.
         * It's documented like that, so we have to.  This is
         * (probably) a good idea since people will realise the
         * coverage isn't working if they get no results,
         * but probably wouldn't know they'd got the location wrong
         * otherwise - clients of this class are expected to be mostly
         * humans, not machines. */
        return cov == null || ( cov.getAmount() == Amount.NO_DATA )
             ? false
             : cov.discOverlaps( ra, dec, distanceDeg );
    }

    /**
     * Returns a (possibly cached) coverage object for a given location.
     * Any coverage object returned is ready for use (initialised),
     * but the return value may be null if no data is available.
     *
     * @param  loc  MOC FITS file location - filename or URL
     * @return  initialised coverage object, may be null if not known
     */
    private static MocCoverage getMocCoverage( String loc ) {
        if ( ! mocMap_.containsKey( loc ) ) {
            mocMap_.put( loc, createMocCoverage( loc ) );
        }
        return mocMap_.get( loc );
    }

    /**
     * Creates a MOC from a string, which may be either a VizieR table ID
     * or the URL or filename of a MOC file.
     *
     * @param   loc  MOC location
     * @return  initialised coverage object, or null
     */
    private static MocCoverage createMocCoverage( String loc ) {
        try {
            URL url = URLUtils.makeURL( loc );
            MocCoverage cov = new UrlMocCoverage( url );
            cov.initCoverage();
            if ( cov.getAmount() != Amount.NO_DATA ) {
                return cov;
            }
            else {
                logger_.config( "No MOC at location: " + loc );
            }
        }
        catch ( Exception e ) {
            logger_.log( Level.INFO, "No MOC at location: " + loc, e );
        }
        try {
            MocCoverage cov = UrlMocCoverage.getVizierMoc( loc, -1 );
            cov.initCoverage();
            if ( cov.getAmount() != Amount.NO_DATA ) {
                return cov;
            }
            else {
                logger_.config( "No VizieR MOC: " + loc );
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.INFO, "No VizieR MOC: " + loc, e );
        }
        logger_.warning( "Unknown MOC: " + loc + " - assume no coverage" );
        return null;
    }
}
