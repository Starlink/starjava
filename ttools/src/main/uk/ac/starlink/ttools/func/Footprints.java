// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import cds.moc.HealpixImpl;
import cds.moc.HealpixMoc;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.cone.PixtoolsHealpix;
import uk.ac.starlink.util.DataSource;

/**
 * Functions related to footprints and coverage.
 * One coverage standard is Multi-Order Coverage maps, described at
 * <a href="http://www.ivoa.net/Documents/Notes/MOC/"
 *         >http://www.ivoa.net/Documents/Notes/MOC/</a>.
 *
 * @author   Mark Taylor
 * @since    29 May 2012
 */
public class Footprints {

    private static final Map<String,HealpixMoc> mocMap_ =
        new HashMap<String,HealpixMoc>();
    private static final HealpixImpl healpix_ = PixtoolsHealpix.getInstance();
    private static final HealpixMoc EMPTY_MOC = new HealpixMoc();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.func" );

    /**
     * Private constructor prevents instantiation.
     */
    private Footprints() {
    }

    /**
     * Indicates whether a given sky position is within a given MOC
     * (Multi-Order Coverage map).
     * If the given <code>mocLocation</code> value does not represent
     * a MOC (for instance no file exists or the file is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be false.
     *
     * @param  mocLocation  location of a FITS MOC file, either as a filename
     *                      or a URL
     * @param  ra     right ascension in degrees
     * @param  dec    declination in degrees
     * @return   true iff the given position falls within the given MOC
     */
    public static boolean inMoc( String mocLocation, double ra, double dec ) {
        HealpixMoc moc = getMoc( mocLocation );
        try {
            return moc.contains( healpix_, ra, dec );
        }
        catch ( Exception e ) {
            return false;
        }
    }

    /**
     * Returns a (possibly cached) MOC object for a given location.
     *
     * @param  loc  MOC FITS file location - filename or URL
     * @return  MOC, not null
     */
    private static HealpixMoc getMoc( String loc ) {
        HealpixMoc moc = mocMap_.get( loc );
        if ( moc == null ) {
            moc = readFitsMoc( loc );
            mocMap_.put( loc, moc );
        }
        return moc;
    }

    /**
     * Reads a MOC from a FITS file at the given location.
     * If the read fails for whatever reason, a message is issued through
     * the logging system, and an empty MOC is returned.
     *
     * @param   loc  MOC FITS file location - filename or URL
     * @return  MOC, not null
     */
    private static HealpixMoc readFitsMoc( String loc ) {
        HealpixMoc moc = new HealpixMoc();
        try {
            InputStream in = DataSource.getInputStream( loc );
            moc.readFits( in );
            in.close();
            logger_.info( "Loaded MOC at " + loc );
            return moc;
        }
        catch ( Exception e ) {
            logger_.warning( "No MOC at " + loc
                           + " (" + e.toString() + ")" );
            return EMPTY_MOC;
        }
    }
}
