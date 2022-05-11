package uk.ac.starlink.table.join;

import java.util.function.Supplier;
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

    public Supplier<MatchKit> createMatchKitFactory() {
        final double separation = getSeparation();
        final Supplier<FixedRadiusConePixer> pixerFact =
            getPixellator().createFixedRadiusPixerFactory( 0.5 * separation );
        final CoordReader coordReader = getCoordReader();
        return () -> new FixedMatchKit( separation, pixerFact.get(),
                                        coordReader );
    }

    public double getScoreScale() {
        return maxScore( getSeparation() );
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
     * Returns an object for decoding tuples.
     *
     * @return   coord reader
     */
    CoordReader getCoordReader() {
        return CoordReader.RADIANS;
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class FixedMatchKit implements MatchKit {

        final double separation_;
        final FixedRadiusConePixer conePixer_;
        final CoordReader coordReader_;

        /**
         * Constructor.
         *
         * @param  separation  maximum separation in radians
         * @param  conePixer   sky pixellation implementation
         * @param  coordReader  converts tuples to coordinates in radians
         */
        FixedMatchKit( double separation, FixedRadiusConePixer conePixer,
                       CoordReader coordReader ) {
            separation_ = separation;
            conePixer_ = conePixer;
            coordReader_ = coordReader;
        }

        public Object[] getBins( Object[] tuple ) {
            double alpha = coordReader_.getAlpha( tuple );
            double delta = coordReader_.getDelta( tuple );
            return ! Double.isNaN( alpha ) && ! Double.isNaN( delta )
                 ? conePixer_.getPixels( alpha, delta )
                 : NO_BINS;
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            return AbstractSkyMatchEngine
                  .matchScore( coordReader_.getAlpha( tuple1 ),
                               coordReader_.getDelta( tuple1 ),
                               coordReader_.getAlpha( tuple2 ),
                               coordReader_.getDelta( tuple2 ),
                               separation_ );
        }
    }

    /**
     * Can read numeric sky coordinates from a supplied tuple.
     */
    private interface CoordReader {

        /** Instance for use with tuples supplied in radians. */
        public static CoordReader RADIANS = new CoordReader() {
            public double getAlpha( Object[] tuple ) {
                return getNumberValue( tuple[ 0 ] );
            }
            public double getDelta( Object[] tuple ) {
                return getNumberValue( tuple[ 1 ] );
            }
        };

        /** Instance for use with tuples supplied in degrees. */
        public static CoordReader DEGREES = new CoordReader() {
            public double getAlpha( Object[] tuple ) {
                return RADIANS.getAlpha( tuple ) * FROM_DEG;
            }
            public double getDelta( Object[] tuple ) {
                return RADIANS.getDelta( tuple ) * FROM_DEG;
            }
        };

        /**
         * Extracts the RA value from a tuple.
         *
         * @param   tuple  object tuple intended for this matcher
         * @return  right ascension coordinate in radians
         */
        double getAlpha( Object[] tuple );

        /**
         * Extracts the Declination value from a tuple.
         *
         * @param   tuple  object tuple intended for this matcher
         * @return  declination coordinate in radians
         */
        double getDelta( Object[] tuple );
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
        CoordReader getCoordReader() {
            return CoordReader.DEGREES;
        }
        @Override
        public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
            return createExtendedSkyBoundsDegrees( inRanges[ index ], 0, 1,
                                                   getSeparation() );
        }
    }
}
