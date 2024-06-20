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
import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Implements sky pixellisation using the HTM (Hierarchical Triangular Mesh)
 * indexing scheme.
 *
 * <p>Note that the {@link HealpixSkyPixellator} implementation normally gives
 * much faster matching than this and should generally be used in
 * preference.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://www.skyserver.org/htm/doc/java/index.html"
 *                   >http://www.skyserver.org/htm/doc/java/index.html</a>
 */
public class HtmSkyPixellator implements SkyPixellator {

    private final DescribedValue levelParam_;
    private double scale_;
    private int level_;

    /**
     * Scale factor which determines the sky pixel size to use,
     * as a multiple of the angular scale, if no level value is set
     * explicitly.  This is a tuning factor (any value will give
     * correct results, but performance may be affected).
     * The current value may not be optimal.
     */
    private static final double DEFAULT_SCALE_FACTOR = 8;

    private static DefaultValueInfo LEVEL_INFO =
        new DefaultValueInfo( "HTM Level", Integer.class,
                              "Controls sky pixel size. "
                            + "Legal range 0 (90deg) - 24 (.01\")." );
    static {
        LEVEL_INFO.setNullable( true );
    }

    /**
     * Constructor.
     */
    public HtmSkyPixellator() {
        levelParam_ = new LevelParameter();
        level_ = -1;
    }

    public void setScale( double scale ) {
        scale_ = scale;
    }

    public double getScale() {
        return scale_;
    }

    public DescribedValue getTuningParameter() {
        return levelParam_;
    }

    public Supplier<VariableRadiusConePixer>
            createVariableRadiusPixerFactory() {
        int level = getLevel();
        final HTMindexImp htm = new HTMindexImp( level, Math.min( level, 2 ) );
        return () -> new VariableRadiusConePixer() {
            public Long[] getPixels( double alpha, double delta,
                                     double radius ) {
                return calculateConePixels( htm, alpha, delta, radius );
            }
        };
    }

    public Supplier<FixedRadiusConePixer>
            createFixedRadiusPixerFactory( final double radius ) {
        int level = getLevel();
        final HTMindexImp htm = new HTMindexImp( level, Math.min( level, 2 ) );
        return () -> new FixedRadiusConePixer() {
            public Long[] getPixels( double alpha, double delta ) {
                return calculateConePixels( htm, alpha, delta, radius );
            }
        };
    }

    /**
     * Sets the HTM level value, which determines sky pixel size.
     * May be in the range 0 (90deg) to 24 (0.01").
     * If set to -1, a suitable value will be used based on the scale.
     *
     * @param  level  new level value
     */
    public void setLevel( int level ) {
        if ( level < -1 || level > 24 ) {
            throw new IllegalArgumentException( "HTM level " + level
                                              + " out of range 0..24" );
        }
        level_ = level;
    }

    /**
     * Returns the HTM level, which determines sky pixel size.
     * The returned value may be the result of a default determination based
     * on scale if no explicit level has been set hitherto, and
     * a non-zero scale is available.
     *
     * @return   level   level value used by this engine
     */
    public int getLevel() {
        if ( level_ >= 0 ) {
            return level_;
        }
        else {
            double scale = getScale();
            return scale > 0 ? calculateDefaultLevel( scale )
                             : -1;
        }
    }

    /**
     * Determines a default value to use for the level paramer
     * based on a given scale.
     *
     * @param  scale  sky distance scale angle, in radians
     */
    public int calculateDefaultLevel( double scale ) {
        double pixelSize = DEFAULT_SCALE_FACTOR * scale;
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
     * Does the work of determining which pixels fall within a specified cone.
     *
     * @param  htm  HTM object
     * @param  alpha   longitude in radians
     * @param  delta   latitude in radians
     * @param  radius  radius in radians
     * @return  pixel list of HTM cells at level of htm object
     *          that may partially overlap the cone
     */
    private static Long[] calculateConePixels( HTMindexImp htm,
                                               double alpha, double delta,
                                               double radius ) {
        double arcminRadius = Math.toDegrees( radius ) * 60.0;
        Circle zone = new Circle( alpha, delta, arcminRadius );

        /* Get the intersection as a range of HTM pixels.
         * The more obvious
         *      range = htm.intersect( zone.getDomain() );
         * is flawed, since it can return pixel IDs which refer to
         * pixels at different HTM levels (i.e. of different sizes).
         * By doing it as below (on advice from Wil O'Mullane) we
         * ensure that all the pixels are at the HTM's natural level. */
        Domain domain = zone.getDomain();
        domain.setOlevel( htm.maxlevel_ );
        HTMrange range = new HTMrange();
        domain.intersect( htm, range, false );

        /* Accumulate a list of the pixel IDs. */
        List<Object> binList = new ArrayList<>();
        try {
            for ( Iterator<?> it = new HTMrangeIterator( range, false );
                  it.hasNext(); ) {
                binList.add( it.next() );
            }
        }
        catch ( HTMException e ) {
            throw new RuntimeException( "Uh-oh", e );
        }
        return binList.toArray( new Long[ 0 ] );
    }

    /**
     * Implements the tuning parameter which controls the level value.
     * This determines the absolute size of the bins.
     */
    private class LevelParameter extends DescribedValue {
        LevelParameter() {
            super( LEVEL_INFO );
        }
        public Object getValue() {
            int level = getLevel();
            return level >= 0 ? Integer.valueOf( level ) : null;
        }
        public void setValue( Object value ) {
            setLevel( value == null ? -1 : ((Integer) value).intValue() );
        }
    }
}
