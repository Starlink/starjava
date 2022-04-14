package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match engine which works with tuples representing RA, Dec and range.
 * Each tuple must be a 3-element array of {@link java.lang.Number} objects:
 * first element is Right Ascension in radians,
 * second element is Declination in radians,
 * third element is range (units are arbitrary, but will be the same as
 * the error supplied in the constructor).
 *
 * @author   Mark Taylor (Starlink)
 */
public class SphericalPolarMatchEngine extends AbstractCartesianMatchEngine {

    private final DescribedValue[] matchParams_;

    private static final DefaultValueInfo R_INFO =
        new DefaultValueInfo( "Distance", Number.class,
                              "Distance from origin" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Cartesian distance between matched points" );
    private static final DefaultValueInfo ERR_INFO =
        new DefaultValueInfo( "Error", Number.class,
                              "Maximum Cartesian separation for match" );
    static {
        ERR_INFO.setUnitString( "units of distance" );
    }

    /**
     * Constructs a new match engine which will match on differences
     * not greater than a given number <tt>err</tt>, in the same units
     * that the range part of the tuples is specified.
     *
     * @param   err  maximum separation for a match
     */
    public SphericalPolarMatchEngine( double err ) {
        super( 3 );
        matchParams_ = new DescribedValue[] {
                           new IsotropicScaleParameter( ERR_INFO ) };
        setIsotropicScale( err );
    }

    /**
     * Sets the isotropic matching error.
     *
     * @param   err  radius of error sphere
     */
    public void setError( double err ) {
        setIsotropicScale( err );
    }

    /**
     * Returns the isotropic matching error.
     *
     * @return  radius of error sphere
     */
    public double getError() {
        return getIsotropicScale();
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] { Tables.RA_INFO, Tables.DEC_INFO, R_INFO };
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        return matchScore( 3, toXyz( tuple1 ), toXyz( tuple2 ), getError() );
    }

    public double getScoreScale() {
        return getError();
    }

    public Object[] getBins( Object[] tuple ) {
        return getRadiusBins( toXyz( tuple ), getError() * 0.5 );
    }

    /**
     * Returns false.  It would probably be possible to implement this,
     * but not very easy.
     */
    public boolean canBoundMatch() {
        return false;
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return "Sky 3D";
    }

    /**
     * Converts a submitted match tuple to Cartesian coordinates.
     *
     * @param   tuple  input tuple
     * @return  (x,y,z) array
     */
    double[] toXyz( Object[] tuple ) {
        return toXyz( getNumberValue( tuple[ 0 ] ),
                      getNumberValue( tuple[ 1 ] ),
                      getNumberValue( tuple[ 2 ] ) );
    }

    /**
     * Converts spherical polar to Cartesian coordinates.
     *
     * @param  raRad  RA in radians
     * @param  decRad  declination in radians
     * @param  r   radius
     * @return  (x,y,z) array
     */
    static double[] toXyz( double raRad, double decRad, double r ) {
        double cd = Math.cos( decRad );
        double sd = Math.sin( decRad );
        double cr = Math.cos( raRad );
        double sr = Math.sin( raRad );

        double x = r * cr * cd;
        double y = r * sr * cd;
        double z = r * sd;

        return new double[] { x, y, z };
    }

    /**
     * MatchEngine class that behaves like SphericalPolarSkyMatchEngine but
     * uses human-friendly units (degrees and arcseconds) rather than radians
     * for tuple elements and match parameters.
     */
    public static class InDegrees extends SphericalPolarMatchEngine {
        private final ValueInfo[] tupleInfos_;
        private static final double FROM_DEG = AbstractSkyMatchEngine.FROM_DEG;

        /**
         * Constructor.
         *
         * @param   err  maximum separation for a match
         */
        public InDegrees( double err ) {
            super( err );
            ValueInfo[] infos0 = super.getTupleInfos();
            tupleInfos_ = new ValueInfo[] {
                AbstractSkyMatchEngine.inDegreeInfo( infos0[ 0 ] ),
                AbstractSkyMatchEngine.inDegreeInfo( infos0[ 1 ] ),
                infos0[ 2 ],
            };
            assert tupleInfos_.length == infos0.length;
        }
        @Override
        double[] toXyz( Object[] tuple ) {
            return toXyz( getNumberValue( tuple[ 0 ] ) * FROM_DEG,
                          getNumberValue( tuple[ 1 ] ) * FROM_DEG,
                          getNumberValue( tuple[ 2 ] ) );
        }
        @Override
        public ValueInfo[] getTupleInfos() {
            return tupleInfos_;
        }
    }
}
