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
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Implements the object matching interface for sky coordinates (RA, Dec)
 * using the HTM (Hierarchical Triangular Mesh) scheme.
 * The tuples it uses are two-element arrays of {@link java.lang.Number}
 * objects, the first giving Right Ascension in radians, and
 * the second giving Declination in radians.
 * The <tt>separation</tt> attribute indicates how many radians may 
 * separate two points on the celestial sphere for them to be considered
 * matching.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <http://www.sdss.jhu.edu/htm/doc/>
 */
public class HTMMatchEngine implements MatchEngine {

    private double separation;
    private double arcminSep;
    private HTMindex htm;
    private DescribedValue sepValue = new SeparationValue();

    private static final DefaultValueInfo RA_INFO = 
        new DefaultValueInfo( "RA", Number.class, "Right Ascension" );
    private static final DefaultValueInfo DEC_INFO =
        new DefaultValueInfo( "Dec", Number.class, "Declination" );
    private static final DefaultValueInfo SEP_INFO =
        new DefaultValueInfo( "Error", Double.class, 
                              "Maximum separation along a great circle" );
    static {
        RA_INFO.setUnitString( "radians" );
        DEC_INFO.setUnitString( "radians" );
        SEP_INFO.setUnitString( "radians" );
        RA_INFO.setNullable( false );
        DEC_INFO.setNullable( false );
        RA_INFO.setUCD( "POS_EQ_RA" );
        DEC_INFO.setUCD( "POS_EQ_DEC" );
    }

    /**
     * Scaling factor which determines the size of the mesh cells used
     * as a multiple of the size of the separation.  It can be used as
     * a tuning parameter.  It must be &gt;1.
     */
    public final static double MESH_SCALE = 1;

    /**
     * Constructs a new match engine which considers two points 
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation   match radius in radians
     */
    public HTMMatchEngine( double separation ) {
        setSeparation( separation );
    }

    /**
     * Configures this match engine to consider two points 
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation   match radius in radians
     */
    public void setSeparation( double separation ) {
        this.separation = separation;
        this.arcminSep = Math.toDegrees( separation ) * 60.0;

        /* Construct an HTM index with mesh elements of a size suitable
         * for the requested resolution. */
        // assert MESH_SCALE > ??; not sure what is the maximum sensible value
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
     * Returns the separation between points within which they will be
     * considered to match.
     *
     * @return   match radius in radians
     */
    public double getSeparation() {
        return separation;
    }

    /**
     * Matches two tuples representing RA,Dec coordinates if they are
     * within <tt>separation</tt> radians of each other on the sky.
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
        double sep = calculateSeparation( ra1, dec1, ra2, dec2 );
        return sep <= separation;
    }

    /**
     * Returns all the HTM cells which fall wholly or partially within 
     * <tt>separation</tt> radians of a given position.
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

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] { RA_INFO, DEC_INFO };
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[] { sepValue };
    }

    public String toString() {
        return "Sky";
    }

    /**
     * Returns the distance along a great circle between two points.
     *
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
    private double calculateSeparation( double ra1, double dec1,
                                        double ra2, double dec2 ) {
        return haversineSeparationFormula( ra1, dec1, ra2, dec2 );
    }

    /**
     * Law of cosines for spherical trigonometry.
     * This is ill-conditioned for small angles (the cases we are generally
     * interested in here.  So don't use it!
     *
     * @deprecated  Ill-conditioned for small angles
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     */
    private double cosineSeparationFormula( double ra1, double dec1,
                                            double ra2, double dec2 ) {
        return Math.acos( Math.sin( dec1 ) * Math.sin( dec2 ) +
                          Math.cos( dec1 ) * Math.cos( dec2 ) 
                                           * Math.cos( ra1 - ra2 ) );
    }

    
    /**
     * Haversine formula for spherical trigonometry.
     * This does not have the numerical instabilities of the cosine formula
     * at small angles. 
     * <p>
     * This implementation derives from Bob Chamberlain's contribution
     * to the comp.infosystems.gis FAQ; he cites
     * R.W.Sinnott, "Virtues of the Haversine", Sky and Telescope vol.68,
     * no.2, 1984, p159.
     * 
     * @param   ra1  right ascension of point 1 in radians
     * @param   dec1 declination of point 1 in radians
     * @param   ra2  right ascension of point 2 in radians
     * @param   dec2 declination of point 2 in radians
     * @return  angular separation of point 1 and point 2 in radians
     * @see  <http://www.census.gov/geo/www/gis-faq.txt>
     */
    private double haversineSeparationFormula( double ra1, double dec1,
                                               double ra2, double dec2 ) {
        double sd2 = Math.sin( 0.5 * ( dec2 - dec1 ) );
        double sr2 = Math.sin( 0.5 * ( ra2 - ra1 ) );
        double a = sd2 * sd2 + 
                   sr2 * sr2 * Math.cos( dec1 ) * Math.cos( dec2 );
        return a < 1.0 ? 2.0 * Math.asin( Math.sqrt( a ) )
                       : Math.PI;
    }

    /**
     * Implements the parameter which controls the matching error.
     */
    private class SeparationValue extends DescribedValue {
        SeparationValue() {
            super( SEP_INFO );
        }
        public Object getValue() {
            return new Double( separation );
        }
        public void setValue( Object value ) {
            setSeparation( ((Double) value).doubleValue() );
        }
    }
}
