package uk.ac.starlink.table.join;

import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMindex;
import edu.jhu.htm.core.HTMindexImp;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.HTMrangeIterator;
import edu.jhu.htm.geometry.Circle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements the object matching interface for sky coordinates (RA, Dec)
 * using the HTM (Hierarchical Triangular Mesh) scheme.
 * The tuples it uses are two-element arrays of {@link java.lang.Number}
 * objects, the first giving Right Ascension in radians, and
 * the second giving Declination in radians.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <http://www.sdss.jhu.edu/htm/doc/>
 */
public class HTMMatchEngine implements MatchEngine {

    private final double separation;
    private final double arcminSep;
    private final HTMindex htm;

    /**
     * Scaling factor which determines the size of the mesh cells used
     * as a multiple of the size of the separation.  It can be used as
     * a tuning parameter.  It must be &gt;1.
     */
    public final static double MESH_SCALE = 3.0;

    /**
     * Constructs a new match engine which considers two points 
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   arcminSep   match radius in radians
     */
    public HTMMatchEngine( double separation ) {
        this.separation = separation;
        arcminSep = Math.toDegrees( separation ) * 60.0;

        /* Construct an HTM index with mesh elements of a size suitable
         * for the requested resolution. */
        assert MESH_SCALE > 1.0;
        try {
            this.htm = new HTMindexImp( Math.toDegrees( separation ) 
                                        * MESH_SCALE ); 
        }
        catch ( HTMException e ) {
            throw (IllegalArgumentException) 
                  new IllegalArgumentException( "Bad separation? " 
                                              + separation + " radians" )
                 .initCause( e );
        }
    }

    /**
     * Matches two tuples representing RA,Dec coordinates if they are
     * within <tt>arcminSep</tt> arc minutes of each other on the sky.
     *
     * @param   radec1  2-element array of Number objects giving RA &amp; dec
     *                  of first point
     * @param   radec2  2-element array of Number objects giving RA &amp; dec
     *                  of second point
     * @return  <tt>true</tt> iff <tt>radec1</tt> is close to <tt>radec2</tt>
     */
    public boolean matches( Object[] radec1, Object[] radec2 ) {

        /* Cheap test which will throw out most comparisons straight away:
         * see if the separation in declination is greater than the maximum
         * acceptable separation. */
        double dec1 = ((Number) radec1[ 1 ]).doubleValue();
        double dec2 = ((Number) radec2[ 1 ]).doubleValue();
        if ( Math.abs( dec1 - dec2 ) > separation ) {
            return false;
        }

        /* Declinations at least are close; do a proper test. */
        double ra1 = ((Number) radec1[ 0 ]).doubleValue();
        double ra2 = ((Number) radec2[ 0 ]).doubleValue();
        double sep = Math.acos( Math.sin( dec1 ) * Math.sin( dec2 ) +
                              ( Math.cos( dec1 ) * Math.cos( dec2 ) )
                              * Math.cos( ra1 - ra2 ) );
        return sep <= separation;
    }

    /**
     * Returns all the HTM cells which fall wholly or partially within 
     * <tt>arcminSep</tt> arcminutes of a given position.
     * 
     * @param  radec  2-element array of Number objects giving RA &amp; Dec
     *         of the position to test
     */
    public Object[] getBins( Object[] radec ) {
        Circle zone = new Circle( ((Number) radec[ 0 ]).doubleValue(),
                                  ((Number) radec[ 1 ]).doubleValue(),
                                  arcminSep );
        HTMrange range = htm.intersect( zone.getDomain() );
        List binList = new ArrayList();
        try {
            for ( Iterator it = new HTMrangeIterator( range, false ); 
                  it.hasNext(); ) {
                binList.add( it.next() );
            }
        }
        catch ( HTMException e ) {
            throw new RuntimeException( "Uh-oh", e );
        }
        return binList.toArray();
    }
}
