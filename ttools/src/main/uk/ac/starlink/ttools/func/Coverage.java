// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import uk.ac.starlink.ttools.Area;
import uk.ac.starlink.ttools.AreaDomain;
import uk.ac.starlink.ttools.build.HideDoc;
import uk.ac.starlink.ttools.cone.AsciiMocCoverage;
import uk.ac.starlink.ttools.cone.Coverage.Amount;
import uk.ac.starlink.ttools.cone.MocCoverage;
import uk.ac.starlink.ttools.cone.UrlMocCoverage;
import uk.ac.starlink.ttools.moc.MocBuilder;
import uk.ac.starlink.ttools.moc.MocImpl;
import uk.ac.starlink.ttools.moc.MocStreamFormat;
import uk.ac.starlink.util.URLUtils;

/**
 * Functions related to coverage and footprints.
 *
 * <p>One coverage standard is <strong>Multi-Order Coverage maps</strong>,
 * described at
 * <a href="http://www.ivoa.net/Documents/MOC/"
 *         >http://www.ivoa.net/Documents/MOC/</a>.
 * MOC positions are always defined in ICRS equatorial coordinates.
 *
 * <p>MOCs may be specified using a string argument of the functions
 * in one of the following ways:
 * <ul>
 * <li>The filename of a MOC FITS file</li>
 * <li>The URL of a MOC FITS file</li>
 * <li>The identifier of a VizieR table, for instance
 *     "<code>V/139/sdss9</code>" (SDSS DR9)</li>
 * <li>An ASCII MOC string, for instance
 *     "<code>1/1 2 4 2/12-14 21 23 25 8/</code>"</li>
 * </ul>
 *
 * <p>A list of all the MOCs available from VizieR can
 * currently be found at
 * <a href="https://alasky.cds.unistra.fr/footprints/tables/vizier/"
 *         >https://alasky.cds.unistra.fr/footprints/tables/vizier/</a>.
 * You can search for VizieR table identifiers from the
 * VizieR web page
 * (<a href="https://vizier.cds.unistra.fr/"
 *          >https://vizier.cds.unistra.fr/</a>);
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

    /** The number of steradians on the sphere, 4 PI. */
    public static final double SPHERE_STERADIAN = 4 * Math.PI;

    /** The number of square degrees on the sphere, approx 41253. */
    public static final double SPHERE_SQDEG = 360 * 360 / Math.PI;

    /**
     * Private constructor prevents instantiation.
     */
    private Coverage() {
    }

    /**
     * Indicates whether a given sky position falls strictly within a given MOC
     * (Multi-Order Coverage map).
     * If the given <code>moc</code> value does not represent a MOC
     * (for instance no file exists or the file/string is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be false.
     *
     * @param  moc    a MOC identifier;
     *                a filename, a URL, a VizieR table name,
     *                or an ASCII MOC string
     * @param  ra     ICRS right ascension in degrees
     * @param  dec    ICRS declination in degrees
     * @return   true iff the given position falls within the given MOC
     */
    public static boolean inMoc( String moc, double ra, double dec ) {
        return nearMoc( moc, ra, dec, 0 );
    }

    /**
     * Indicates whether a given sky position either falls within,
     * or is within a certain distance of the edge of,
     * a given MOC (Multi-Order Coverage map).
     * If the given <code>moc</code> value does not represent a MOC
     * (for instance no file exists or the file/string is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be false.
     *
     * @param  moc    a MOC identifier;
     *                a filename, a URL, a VizieR table name,
     *                or an ASCII MOC string
     * @param  ra     ICRS right ascension in degrees
     * @param  dec    ICRS declination in degrees
     * @param  distanceDeg   permitted distance from MOC boundary in degrees
     * @return   true iff the given position is within <code>distance</code>
     *           degrees of the given MOC
     */
    public static boolean nearMoc( String moc, double ra, double dec,
                                   double distanceDeg ) {
        MocCoverage cov = getMocCoverage( moc );

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
     * Returns the proportion of the sky covered by a given MOC.
     *
     * <p>If the given <code>moc</code> value does not represent a MOC
     * (for instance no file exists or the file/string is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be NaN.
     *
     * @param  moc    a MOC identifier;
     *                a filename, a URL, a VizieR table name,
     *                or an ASCII MOC string
     * @return   a fractional value in the range 0..1
     */
    public static double mocSkyProportion( String moc ) {
        MocCoverage cov = getMocCoverage( moc );
        return cov == null || ( cov.getAmount() == Amount.NO_DATA )
             ? Double.NaN
             : cov.getMoc().getCoverage();
    }

    /**
     * Returns the number of unique tiles within a given MOC.
     *
     * <p>If the given <code>moc</code> value does not represent a MOC
     * (for instance no file exists or the file/string is not in MOC format)
     * a warning will be issued the first time it's referenced, and
     * the result will be 0.
     *
     * @param  moc    a MOC identifier;
     *                a filename, a URL, a VizieR table name,
     *                or an ASCII MOC string
     * @return  number of tiles in the MOC
     */
    public static long mocTileCount( String moc ) {
        MocCoverage cov = getMocCoverage( moc );
        return cov == null || ( cov.getAmount() == Amount.NO_DATA )
             ? 0
             : cov.getMoc().getNbCoding();
    }

    /**
     * Returns the ASCII MOC representation of a small circle on the sky.
     *
     * @param  order  MOC order
     * @param  ra   right acension of circle center in degrees
     * @param  dec  declination of circle center in degrees
     * @param  radiusDeg  radius of circle in degrees
     * @return   ASCII MOC covering the circle at the given order
     */
    public static String asciiMocCircle( int order, double ra, double dec,
                                         double radiusDeg ) {
        return asciiMocCircle( order, new double[] { ra, dec, radiusDeg } );
    }

    /**
     * Returns the ASCII MOC representation of a small circle on the sky.
     *
     * @param  order  MOC order
     * @param  array3   3-element array giving right ascension of center,
     *                  declination of center, and radius of circle,
     *                  all in degrees
     * @return   ASCII MOC covering the circle at the given order
     */
    public static String asciiMocCircle( int order, double[] array3 ) {
        return asciiMoc( Area.Type.CIRCLE, order, array3 );
    }

    @HideDoc
    public static String asciiMocCircle( int order, float[] array3 ) {
        return asciiMocCircle( order, toDoubleArray( array3 ) );
    }

    /**
     * Returns the ASCII MOC representation of an ellipse on the sky.
     *
     * @param  order  MOC order
     * @param  ra   right acension of ellipse center in degrees
     * @param  dec  declination of ellipse center in degrees
     * @param  rmajDeg  major radius in degrees
     * @param  rminDeg  minor radius in degrees
     * @param  posAngDeg  position angle as degrees east of north to the
     *                    major axis
     * @return   ASCII MOC covering the ellipse at the given order
     */
    public static String asciiMocEllipse( int order,
                                          double ra, double dec,
                                          double rmajDeg, double rminDeg,
                                          double posAngDeg ) {
        return asciiMocEllipse( order,
                                new double[] { ra, dec, rmajDeg, rminDeg,
                                               posAngDeg } );
    }

    /**
     * Returns the ASCII MOC representation of an ellipse on the sky.
     *
     * @param  order  MOC order
     * @param  array5  5-element array giving right ascension of center,
     *                 declination of center, major radius, minor radius
     *                 and position angle east of north, all in degrees
     * @return   ASCII MOC covering the ellipse at the given order
     */
    public static String asciiMocEllipse( int order, double[] array5 ) {
        return asciiMoc( Area.Type.ELLIPSE, order, array5 );
    }

    @HideDoc
    public static String asciiMocEllipse( int order, float[] array5 ) {
        return asciiMocEllipse( order, toDoubleArray( array5 ) );
    }

    /**
     * Returns the ASCII MOC representation of a polygon on the sky.
     *
     * @param  order  MOC order
     * @param  vertices   polygon vertices (ra1,dec1, ra2,dec2, ...),
     *                    either as multiple arguments or supplied as
     *                    a single array
     * @return   ASCII MOC covering the polygon at the given order
     */
    public static String asciiMocPolygon( int order, double... vertices ) {
        return asciiMoc( Area.Type.POLYGON, order, vertices );
    }

    @HideDoc
    public static String asciiMocPolygon( int order, float... vertices ) {
        return asciiMocPolygon( order, toDoubleArray( vertices ) );
    }

    /**
     * Returns the ASCII MOC representation of an STC-S area specification.
     * STC-S is a somewhat obsolete region description syntax,
     * but still used in some places.
     *
     * @param  order  MOC order
     * @param  stcs  STC-S string
     * @return   ASCII MOC covering the STC-S region
     */
    public static String asciiMocStcs( int order, String stcs ) {
        if ( stcs == null || stcs.trim().length() == 0 ) {
            return null;
        }
        Area area = AreaDomain.stcsArea( stcs, true );
        return area == null
             ? null
             : uniqsToMocAscii( order, area.toMocUniqs( order ) );
    }

    /**
     * Returns the ASCII MOC representation of an area with a given type.
     *
     * @param  areaType  area type
     * @param  order    order of MOC
     * @param  areaData   areaType-specific array defining the shape coords
     * @return   ASCII MOC covering the shape at the given order
     */
    private static String asciiMoc( Area.Type areaType, int order,
                                    double[] areaData ) {
        if ( areaType == null || areaData == null ||
             ! areaType.isLegalArrayLength( areaData.length ) ) {
            return null;
        }
        return uniqsToMocAscii( order, areaType.toMocUniqs( areaData, order ) );
    }

    /**
     * Converts an array of not-necessarily-normalised uniq values to
     * an ASCII MOC.
     *
     * @param  order  output MOC order
     * @param  mocUniqs   array of uniq values
     * @return  ASCII MOC
     */
    private static String uniqsToMocAscii( int order, long[] mocUniqs ) {
        MocBuilder mocBuilder = MocImpl.AUTO.createMocBuilder( order );
        for ( long uniq : mocUniqs ) {
            mocBuilder.addTile( uniqToOrder( uniq ), uniqToIndex( uniq ) );
        }
        mocBuilder.endTiles();
        long[] orderCounts = mocBuilder.getOrderCounts();
        long ntile = 0;
        for ( int io = 0; io < orderCounts.length; io++ ) {
            ntile += orderCounts[ io ];
        }
        try ( ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
            MocStreamFormat.ASCII
                           .writeMoc( mocBuilder.createOrderedUniqIterator(),
                                      ntile, order, out );
            return new String( out.toByteArray(), StandardCharsets.UTF_8 )
                  .trim();
        }
        catch ( IOException e ) {
            return null;
        }
    }

    /**
     * Returns a (possibly cached) coverage object for a given location.
     * Any coverage object returned is ready for use (initialised),
     * but the return value may be null if no data is available.
     *
     * @param  mocTxt a MOC identifier;
     *                a filename, a URL, a VizieR table name,
     *                or an ASCII MOC string
     * @return  initialised coverage object, may be null if not known
     */
    private static MocCoverage getMocCoverage( String mocTxt ) {
        if ( ! mocMap_.containsKey( mocTxt ) ) {
            mocMap_.put( mocTxt, createMocCoverage( mocTxt ) );
        }
        return mocMap_.get( mocTxt );
    }

    /**
     * Creates a MOC from a string, which may be either a VizieR table ID
     * or the URL or filename of a MOC file.
     *
     * @param   mocTxt  MOC location
     * @return  initialised coverage object, or null
     */
    private static MocCoverage createMocCoverage( String mocTxt ) {
        if ( AsciiMocCoverage.looksLikeAsciiMoc( mocTxt ) ) {
            try {
                MocCoverage cov = new AsciiMocCoverage( mocTxt );
                cov.initCoverage();
                if ( cov.getAmount() != Amount.NO_DATA ) {
                    return cov;
                }
                else {
                    logger_.info( "Looks like ASCII MOC but parsing failed: "
                                + mocTxt );
                    return null;
                }
            }
            catch ( IOException e ) {
                return null;
            }
        }
        try {
            URL url = URLUtils.makeURL( mocTxt );
            MocCoverage cov = new UrlMocCoverage( url );
            cov.initCoverage();
            if ( cov.getAmount() != Amount.NO_DATA ) {
                return cov;
            }
            else {
                logger_.config( "No MOC at location: " + mocTxt );
            }
        }
        catch ( Exception e ) {
            logger_.log( Level.INFO, "No MOC at location: " + mocTxt, e );
        }
        try {
            MocCoverage cov = UrlMocCoverage.getVizierMoc( mocTxt, -1 );
            cov.initCoverage();
            if ( cov.getAmount() != Amount.NO_DATA ) {
                return cov;
            }
            else {
                logger_.config( "No VizieR MOC: " + mocTxt );
            }
        }
        catch ( IOException e ) {
            logger_.log( Level.INFO, "No VizieR MOC: " + mocTxt, e );
        }
        logger_.warning( "Unknown MOC: " + mocTxt + " - assume no coverage" );
        return null;
    }

    /**
     * Converts a HEALPix order and and tile index into a UNIQ-encoded
     * integer as used in MOC encoding.
     * The result is <code>index + 4**(1+order)</code>.
     *
     * <p>If the order or index are out of bounds, behaviour is undefined.
     *
     * @param  order  HEALPix order, in range 0..29
     * @param  index  tile index within the given level
     * @return  uniq-encoded value
     */
    public static long mocUniq( int order, long index ) {
        return ( 4L << ( 2 * order ) ) + index;
    }

    /**
     * Extracts the HEALPix order from a UNIQ-encoded integer
     * as used in MOC encoding.
     *
     * <p>If the supplied value is not a legal UNIQ integer,
     * -1 is returned.
     *
     * @param  uniq  uniq-encoded value
     * @return   HEALPix order
     */
    public static int uniqToOrder( long uniq ) {
        // Copied from function from_uniq_ivoa in src/nested/mod.rs at
        // https://github.com/cds-astro/cds-healpix-rust
        return uniq > 3 ? ( 61 - Long.numberOfLeadingZeros( uniq ) ) >> 1
                        : -1;
    }

    /**
     * Extracts the HEALPix pixel index from a UNIQ-encoded integer
     * as used in MOC encoding.
     *
     * <p>If the supplied value is not a legal UNIQ integer,
     * -1 is returned.
     *
     * @param  uniq  uniq-encoded value
     * @return  pixel index
     */
    public static long uniqToIndex( long uniq ) {
        // Copied from function from_uniq_ivoa in src/nested/mod.rs at
        // https://github.com/cds-astro/cds-healpix-rust
        return uniq > 3 ? uniq - ( 4L << ( uniqToOrder( uniq ) << 1 ) )
                        : -1;
    }

    /**
     * Converts a list of HEALPix indices at the same order into a MOC,
     * and returns its ASCII representation.
     *
     * <p>Note this may not be a very fast operation, so use within
     * TOPCAT, where evaluation is lazy and happens every time the
     * cell value is required, may lead to slow behaviour.
     *
     * @example <code>indicesToMocAscii(1,intArray(24,25,26,27,39,42,43)) =
     *                "0/6 1/39 42-43"</code>
     *
     * @param  order  MOC order of tiles
     * @param  indices   array of tile indices at the given order
     * @return   ASCII representation of MOC at the given order
     */
    public static String indicesToMocAscii( int order, long[] indices ) {
        return indices == null || indices.length == 0
             ? null
             : indicesToMocAscii( order, LongStream.of( indices ) );
    }

    /**
     * Converts a list of integer HEALPix indices at the same order into a MOC,
     * and returns its ASCII representation.
     *
     * @param  order  MOC order of tiles
     * @param  indices   array of tile indices at the given order
     * @return   ASCII representation of MOC at the given order
     */
    @HideDoc
    public static String indicesToMocAscii( int order, int[] indices ) {
        return indices == null || indices.length == 0
             ? null
             : indicesToMocAscii( order,
                                  IntStream.of( indices ).mapToLong( i -> i ) );
        
    }

    /**
     * Converts a stream of HEALPix indices at the same order into a MOC,
     * and returns its ASCII representation.
     *
     * @param  order  MOC order of tiles
     * @param  indexStream   stream of indices, not null
     * @return  ASCII representation of MOC at the given order,
     *          or null if there's a problem
     */
    private static String indicesToMocAscii( int order,
                                             LongStream indexStream ) {
        if ( order < 0 || order > 29 ) {
            return null;
        }
        MocBuilder mb = MocImpl.AUTO.createMocBuilder( order );
        indexStream.forEach( l -> mb.addTile( order, l ) );
        mb.endTiles();
        long ntile = LongStream.of( mb.getOrderCounts() ).sum();
        try ( ByteArrayOutputStream bout = new ByteArrayOutputStream() ) {
            MocStreamFormat.ASCII.writeMoc( mb.createOrderedUniqIterator(),
                                            ntile, order, bout );
            return new String( bout.toByteArray(), StandardCharsets.UTF_8 )
                  .trim();
        }
        catch ( IOException e ) {
            return null;
        }
    }

    /**
     * Converts a float[] array to its double[] equivalent.
     *
     * @param  farray  input array
     * @return  double array with the same values as farray
     */
    private static double[] toDoubleArray( float[] farray ) {
        if ( farray != null ) {
            int n = farray.length;
            double[] darray = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                darray[ i ] = farray[ i ];
            }
            return darray;
        }
        else {
            return null;
        }
    }
}
