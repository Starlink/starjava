package uk.ac.starlink.ttools.cone;

import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMindexImp;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.geometry.Circle;

/**
 * HTM implementation of SkyTiling.
 *
 * @author   Mark Taylor
 * @since    12 Dec 2007
 */
public class HtmTiling implements SkyTiling {

    private final HTMindexImp htm_;

    /**
     * Constructs an HtmTiling given an HTMindexImp.
     *
     * @param  htm  HTM index object
     */
    public HtmTiling( HTMindexImp htm ) {
        htm_ = htm;
    }

    /**
     * Constructs an HtmTiling with a given level.
     *
     * @param  level   HTM level
     */
    public HtmTiling( int level ) {
        this( new HTMindexImp( level ) );
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
        HTMrange range = new HTMrange();
        new Circle( ra, dec, radius * 60 ).getConvex()
                                          .intersect( htm_, range, false );

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
