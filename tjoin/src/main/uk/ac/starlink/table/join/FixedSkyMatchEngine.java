package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * MatchEngine which matches objects on the celestial sphere with a 
 * fixed maximum separation.
 * The tuples it uses are two-element arrays of {@link java.lang.Number}
 * objects, representing Right Ascension and Declination respectively
 * in radians.  Other similar longitude/latitude-like coordinate systems
 * may alternatively be used.
 *
 * @author   Mark Taylor
 * @since    6 Sep 2011
 */
public class FixedSkyMatchEngine extends AbstractSkyMatchEngine {

    private final DescribedValue[] matchParams_;

    private static final DefaultValueInfo SEP_INFO =
        new DefaultValueInfo( "Max Error", Number.class,
                              "Maximum separation along a great circle" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Distance between matched objects "
                            + "along a great circle" );

    static {
        SEP_INFO.setUnitString( "radians" );
        SEP_INFO.setUCD( "pos.angDistance" );

        SCORE_INFO.setUnitString( "arcsec" );
        SCORE_INFO.setUCD( "pos.angDistance" );
    }

    /**
     * Constructor.
     *
     * @param   pixellator  handles sky pixellisation
     * @param   separation  initial value for maximum match separation,
     *                      in radians
     */
    public FixedSkyMatchEngine( SkyPixellator pixellator, double separation ) {
        super( pixellator, separation );
        matchParams_ =
            new DescribedValue[] { new SkyScaleParameter( SEP_INFO ) };
    }

    /**
     * Sets the maximum separation which corresponds to a match.
     *
     * @param  separation  maximum separation in radians
     */
    public void setSeparation( double separation ) {
        setScale( separation );
    }

    /**
     * Returns the maximum separation which corresponds to a match.
     *
     * @return  maximum separation in radians
     */
    public double getSeparation() {
        return getScale();
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] { Tables.RA_INFO, Tables.DEC_INFO };
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        return matchScore( getAlpha( tuple1 ), getDelta( tuple1 ),
                           getAlpha( tuple2 ), getDelta( tuple2 ),
                           getSeparation() );
    }

    public double getScoreScale() {
        return maxScore( getSeparation() );
    }

    public Object[] getBins( Object[] tuple ) {
        double alpha = getAlpha( tuple );
        double delta = getDelta( tuple );
        double radius = getSeparation() * 0.5;
        return ! Double.isNaN( alpha ) && ! Double.isNaN( delta )
             ? getPixellator().createFixedRadiusPixerFactory( radius ).get()
                              .getPixels( alpha, delta )
             : NO_BINS;
    }

    public boolean canBoundMatch() {
        return true;
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        return createExtendedSkyBounds( inRanges[ index ], 0, 1,
                                        getSeparation() );
    }

    public String toString() {
        return "Sky";
    }

    /**
     * Extracts the RA value from a tuple.
     *
     * @param   tuple  object tuple intended for this matcher
     * @return  right ascension coordinate in radians
     */
    double getAlpha( Object[] tuple ) {
        return getNumberValue( tuple[ 0 ] );
    }

    /**
     * Extracts the Declination value from a tuple.
     *
     * @param   tuple  object tuple intended for this matcher
     * @return  declination coordinate in radians
     */
    double getDelta( Object[] tuple ) {
        return getNumberValue( tuple[ 1 ] );
    }

    /**
     * MatchEngine class that behaves like FixedSkyMatchEngine but uses
     * human-friendly units (degrees and arcseconds) rather than radians
     * for tuple elements and match parameters.
     */
    public static class InDegrees extends FixedSkyMatchEngine {
        private final ValueInfo[] tupleInfos_;
        private final DescribedValue[] matchParams_;

        /**
         * Constructor.
         *
         * @param   pixellator  handles sky pixellisation
         * @param   sepRadians  initial value for maximum match separation,
         *                      in radians
         */
        public InDegrees( SkyPixellator pixellator, double sepRadians ) {
            super( pixellator, sepRadians );
            ValueInfo[] infos0 = super.getTupleInfos();
            tupleInfos_ = new ValueInfo[] {
                inDegreeInfo( infos0[ 0 ] ),
                inDegreeInfo( infos0[ 1 ] ),
            };
            DescribedValue[] params0 = super.getMatchParameters();
            matchParams_ = new DescribedValue[] {
                radiansToArcsecParam( params0[ 0 ] ),
            };
            assert tupleInfos_.length == infos0.length;
            assert matchParams_.length == params0.length;
        }
        @Override
        public ValueInfo[] getTupleInfos() {
            return tupleInfos_;
        }
        @Override
        public DescribedValue[] getMatchParameters() {
            return matchParams_;
        }
        @Override
        double getAlpha( Object[] tuple ) {
            return super.getAlpha( tuple ) * FROM_DEG;
        }
        @Override
        double getDelta( Object[] tuple ) {
            return super.getDelta( tuple ) * FROM_DEG;
        }
        @Override
        public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
            return createExtendedSkyBoundsDegrees( inRanges[ index ], 0, 1,
                                                   getSeparation() );
        }
    }
}
