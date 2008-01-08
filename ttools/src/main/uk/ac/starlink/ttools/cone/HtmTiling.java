package uk.ac.starlink.ttools.cone;

import edu.jhu.htm.core.Domain;
import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMindexImp;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.geometry.Circle;
import uk.ac.starlink.ttools.func.Tilings;

/**
 * HTM implementation of SkyTiling.
 *
 * @author   Mark Taylor
 * @since    12 Dec 2007
 */
public class HtmTiling implements SkyTiling {

    private final HTMindexImp htm_;
    private final int level_;
    private final double resolution_;

    /**
     * Constructs an HtmTiling given an HTMindexImp.
     *
     * @param  htm  HTM index object
     */
    public HtmTiling( HTMindexImp htm ) {
        htm_ = htm;
        level_ = htm.maxlevel_;
        resolution_ = Tilings.htmResolution( level_ );
    }

    /**
     * Constructs an HtmTiling with a given level.
     *
     * @param  level   HTM level
     */
    public HtmTiling( int level ) {
        this( new HTMindexImp( level ) );
        assert level_ == level;
    }

    public long getPositionTile( double ra, double dec ) {
        try {
            return htm_.lookupId( ra, dec );
        }
        catch ( HTMException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "HTM error: " + e.getMessage() )
                 .initCause( e );
        }
    }

    public long[] getTileRange( double ra, double dec, double radius ) {

        /* If the radius is too big, don't attempt the calculation - 
         * range determination might be slow. */
        if ( radius > resolution_ * 50 ) {
            return null;
        }

        /* Get the intersection as a range of HTM pixels.
         * The more obvious
         *      range = htm_.intersect( zone.getDomain() );
         * is flawed, since it can return pixel IDs which refer to
         * pixels at different HTM levels (i.e. of different sizes).
         * By doing it as below (on advice from Wil O'Mullane) we
         * ensure that all the pixels are at the HTM's natural level. */
        Circle zone = new Circle( ra, dec, radius * 60.0 );
        Domain domain = zone.getDomain();
        domain.setOlevel( level_ );
        HTMrange range = new HTMrange();
        domain.intersect( htm_, range, false );

        /* Turn it into a (lo, hi) bounding range. */
        long lo = Long.MAX_VALUE;
        long hi = Long.MIN_VALUE;
        range.reset();
        for ( long[] lohi; ! endRange( lohi = range.getNext() ); ) {
            lo = Math.min( lo, lohi[ 0 ] );
            hi = Math.max( hi, lohi[ 1 ] );
        }
        return lo <= hi ? new long[] { lo, hi }
                        : null;
    }

    public String toString() {
        return "htm" + level_;
    }

    /**
     * Returns true if a range element represents the final one in a range.
     *
     * @param   lohi   2-element low,high bounds
     * @return  true iff both elements of <code>lohi</code> are zero
     */
    private static boolean endRange( long[] lohi ) {
        return lohi[ 0 ] == 0L && lohi[ 1 ] == 0L;
    }
}
