package uk.ac.starlink.table.join;

import edu.jhu.htm.core.Domain;
import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMindexImp;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.HTMrangeIterator;
import edu.jhu.htm.geometry.Circle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;

/**
 * Implements the object matching interface for sky coordinates
 * using the HTM (Hierarchical Triangular Mesh) pixel-indexing scheme.
 * <p>
 * Note that the {@link HEALPixMatchEngine} implementation normally gives
 * much faster matching than this and should generally be used in
 * preference.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://www.sdss.jhu.edu/htm/doc/"
 *                   >http://www.sdss.jhu.edu/htm/doc</a>
 */
public class HTMMatchEngine extends SkyMatchEngine {

    private final DescribedValue levelParam_;
    private int level_;
    private HTMindexImp htm_;

    /**
     * Scale factor which determines the sky pixel size scale to use,
     * as a multiple of the separation size, if no level value is set
     * explicitly.  This is a tuning factor (any value will give
     * correct results, but performance may be affected).
     * The current value may not be optimal.
     */
    private static final double DEFAULT_SCALE_FACTOR = 2;

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
     * @param   useErrors   if true, per-row errors can be specified as
     *          a third element of the tuples; otherwise only the fixed
     *          separation value counts
     */
    public HTMMatchEngine( double separation, boolean useErrors ) {
        super( useErrors );
        levelParam_ = new LevelParameter();
        level_ = -1;
        setSeparation( separation );
    }

    public void setSeparation( double separation ) {
        super.setSeparation( separation );
        configureLevel();
    }

    /**
     * Returns all the HTM cells which fall wholly or partially within
     * <tt>err</tt> radians of a given position.
     *
     * @param  ra   right ascension
     * @param  dec  declination
     * @param  err  error
     * @return  bin list
     */
    public Object[] getBins( double ra, double dec, double err ) {
        double arcminErr = Math.toDegrees( err ) * 60.0;
        Circle zone = new Circle( ra, dec, arcminErr );

        /* Get the intersection as a range of HTM pixels.
         * The more obvious 
         *      range = htm_.intersect( zone.getDomain() );
         * is flawed, since it can return pixel IDs which refer to 
         * pixels at different HTM levels (i.e. of different sizes).
         * By doing it as below (on advice from Wil O'Mullane) we
         * ensure that all the pixels are at the HTM's natural level. */
        Domain domain = zone.getDomain();
        domain.setOlevel( htm_.maxlevel_ );
        HTMrange range = new HTMrange();
        domain.intersect( htm_, range, false );

        /* Accumulate a list of the pixel IDs. */
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

    public DescribedValue[] getTuningParameters() {
        return new DescribedValue[] { levelParam_ };
    }

    /**
     * Sets the HTM level value, which determines sky pixel size.
     * May be in the range 0 (90deg) to 24 (0.01").
     * If set to -1, a suitable value will be used based on the separation.
     *
     * @param  level  new level value
     */
    public void setLevel( int level ) {
        if ( level < -1 || level > 24 ) {
            throw new IllegalArgumentException( "HTM level " + level
                                              + " out of range 0..24" );
        }
        level_ = level;
        configureLevel();
    }

    /**
     * Returns the HTM level, which determines sky pixel size.
     * The returned value may be the result of a default determination based
     * on separation if no explicit level has been set hitherto, and
     * a non-zero separation is available.
     *
     * @return   level   level value used by this engine
     */
    public int getLevel() {
        if ( level_ >= 0 ) {
            return level_;
        }
        else {
            double sep = getSeparation();
            return sep > 0 ? calculateDefaultLevel( sep )
                           : -1;
        }
    }

    /**
     * Updates internal state for the current values of separation and level.
     */
    private void configureLevel() {
        int level = getLevel();
        htm_ = new HTMindexImp( level, Math.min( level, 2 ) );
    }

    /**
     * Determines a default value to use for the level paramer
     * based on a given separation.
     *
     * @param  sep  max sky separation angle for a match, in radians
     */
    public int calculateDefaultLevel( double sep ) {
        double pixelSize = DEFAULT_SCALE_FACTOR * sep;
        double pixelSizeDeg = Math.toDegrees( pixelSize );

        /* This code stolen from HTMindexImp constructor. */
        int lev = 5;
        double htmwidth = 2.8125;
        while ( htmwidth > pixelSizeDeg && lev < 25 ) {
            htmwidth /= 2;
            lev++;
        }
        return lev;
    }

    /**
     * Implements the tuning parameter which controls the level value.
     * This determines the absolute size of the bins.
     */
    private class LevelParameter extends DescribedValue {
        LevelParameter() {
            super( new DefaultValueInfo( "HTM Level", Integer.class ) );
            DefaultValueInfo info = (DefaultValueInfo) getInfo();
            info.setDescription( "Controls sky pixel size. "
                               + "Legal range 0 (90deg) - 24 (.01\")." );
            info.setNullable( true );
        }
        public Object getValue() {
            int level = getLevel();
            return level >= 0 ? new Integer( level ) : null;
        }
        public void setValue( Object value ) {
            setLevel( value == null ? -1 : ((Integer) value).intValue() );
        }
    }
}
