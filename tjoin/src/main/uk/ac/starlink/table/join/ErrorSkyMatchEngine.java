package uk.ac.starlink.table.join;

import java.util.function.Supplier;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * MatchEngine which matches objects on the celestial sphere according to
 * per-object error radii.
 * The tuples it uses are three-element arrays of {@link java.lang.Number}
 * objects, representing Right Ascension, Declination, and error radius
 * respectively, all in radians.  Other similar longitude/latitude-like
 * coordinate system may alternatively be used.
 * Two tuples are considered to match if the distance along a great circle
 * of their central positions is no greater than the sum of their per-object
 * radii.
 *
 * <p>A length scale must be supplied, which should be of comparable size
 * to the average per-object error, and which affects performance but not
 * the result.  The effect of this is to provide a default for the 
 * pixellisation tuning parameter.  If the tuning parameter is set directly,
 * the length scale is ignored.
 *
 * @author   Mark Taylor
 * @since    6 Sep 2011
 */
public class ErrorSkyMatchEngine extends AbstractSkyMatchEngine {

    private final DescribedValue[] matchParams_;

    private static final DefaultValueInfo SCALE_INFO =
        new DefaultValueInfo( "Scale", Number.class,
                              "Rough average of per-object error distance; "
                            + "just used for tuning to set "
                            + "default pixel size" );
    private static final DefaultValueInfo ERR_INFO =
        new DefaultValueInfo( "Error", Number.class,
                              "Per-object error radius along a great circle" );
    private static final DefaultValueInfo SCORE_INFO =
        new DefaultValueInfo( "Separation", Double.class,
                              "Scaled distance between matched objects " +
                              "along a great circle (0..1)" );
    static {
        SCALE_INFO.setUnitString( "radians" );
        SCALE_INFO.setUCD( "pos.angDistance" );
        SCALE_INFO.setNullable( false );

        ERR_INFO.setUnitString( "radians" );
        ERR_INFO.setUCD( "pos.angDistance" );
        ERR_INFO.setNullable( false );
    }

    /**
     * Constructor.
     *
     * @param  pixellator  handles sky pixellisation
     * @param  scale       initial value for length scale, in radians
     */
    public ErrorSkyMatchEngine( SkyPixellator pixellator, double scale ) {
        super( pixellator, scale );
        matchParams_ =
            new DescribedValue[] { new SkyScaleParameter( SCALE_INFO ) };
    }

    /**
     * Sets the length scale.
     *
     * @param  scale rough value of per-object errors, in radians
     */
    public void setScale( double scale ) {
        super.setScale( scale );
    }

    /**
     * Returns the length scale.
     *
     * @return  length scale value in radians
     */
    public double getScale() {
        return super.getScale();
    }

    public ValueInfo[] getTupleInfos() {
        return new ValueInfo[] {
            Tables.RA_INFO, Tables.DEC_INFO, ERR_INFO,
        };
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public ValueInfo getMatchScoreInfo() {
        return SCORE_INFO;
    }

    public Supplier<MatchKit> createMatchKitFactory() {
        final Supplier<VariableRadiusConePixer> pixerFact =
            getPixellator().createVariableRadiusPixerFactory();
        final CoordReader coordReader = getCoordReader();
        return () -> new ErrorMatchKit( pixerFact.get(), coordReader );
    }

    public Supplier<Coverage> createCoverageFactory() {
        final double scale = getScale();
        final CoordReader coordReader = getCoordReader();
        final SkyCoverage.TupleDecoder coneDecoder = ( tuple, lonLatErr ) -> {
            double alpha = coordReader.getAlpha( tuple );
            double delta = coordReader.getDelta( tuple );
            if ( Double.isFinite( alpha ) && Double.isFinite( delta ) ) {
                lonLatErr[ 0 ] = alpha;
                lonLatErr[ 1 ] = delta;
                lonLatErr[ 2 ] = coordReader.getError( tuple );
                return true;
            }
            else {
                return false;
            }
        };
        return () -> SkyCoverage
                    .createVariableErrorCoverage( scale, coneDecoder );
    }

    /**
     * Returns unity.
     */
    public double getScoreScale() {
        return 1.0;
    }

    public boolean canBoundMatch() {
        return true;
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        double maxRadius = 0;
        for ( NdRange inRange : inRanges ) {
            maxRadius =
                Math.max( maxRadius,
                          getCoordReader().getError( inRange.getMaxs() ) );
        }
        return createExtendedSkyBounds( inRanges[ index ], 0, 1,
                                        2 * maxRadius );
    }

    public String toString() {
        return "Sky with Errors";
    }

    /**
     * Returns an object which can decode tuples.
     *
     * @return  coord reader
     */
    CoordReader getCoordReader() {
        return CoordReader.RADIANS;
    }

    /**
     * MatchKit implementation for use with this class.
     */
    private static class ErrorMatchKit implements MatchKit {
        final VariableRadiusConePixer conePixer_;
        final CoordReader coordReader_;

        /**
         * Constructor.
         *
         * @param   conePixer  sky pixellation implementation
         * @param   coordReader  extracts coords from tuple
         */
        ErrorMatchKit( VariableRadiusConePixer conePixer,
                       CoordReader coordReader ) {
            conePixer_ = conePixer;
            coordReader_ = coordReader;
        }

        public Object[] getBins( Object[] tuple ) {
            double alpha = coordReader_.getAlpha( tuple );
            double delta = coordReader_.getDelta( tuple );
            double error = coordReader_.getError( tuple );
            return ! Double.isNaN( alpha ) && ! Double.isNaN( delta )
                     && error >= 0
                 ? conePixer_.getPixels( alpha, delta, error )
                 : NO_BINS;
        }

        public double matchScore( Object[] tuple1, Object[] tuple2 ) {
            double delta1 = coordReader_.getDelta( tuple1 );
            double delta2 = coordReader_.getDelta( tuple2 );
            double maxerr = coordReader_.getError( tuple1 )
                          + coordReader_.getError( tuple2 );

            /* Cheap test which will throw out most comparisons straight away:
             * see if the separation in declination is greater than the maximum
             * acceptable separation. */
            if ( Math.abs( delta1 - delta2 ) > maxerr ) {
                return -1.0;
            }

            /* Otherwise declinations at least are close; do a proper test. */
            double alpha1 = coordReader_.getAlpha( tuple1 );
            double alpha2 = coordReader_.getAlpha( tuple2 );
            double sep = calculateSeparation( alpha1, delta1, alpha2, delta2 );
            if ( sep <= maxerr ) {
                return maxerr > 0 ? sep / maxerr : 0.0;
            }
            else {
                return -1.0;
            }
        }
    }

    /**
     * Extracts coordinates from a tuple.
     */
    private interface CoordReader {

        /** CoordReader instance for input in radians. */
        public static CoordReader RADIANS = new CoordReader() {
            public double getAlpha( Object[] tuple ) {
                return getNumberValue( tuple[ 0 ] );
            }
            public double getDelta( Object[] tuple ) {
                return getNumberValue( tuple[ 1 ] );
            }
            public double getError( Object[] tuple ) {
                return getNumberValue( tuple[ 2 ] );
            }
        };

        /** CoordReader instance for input in degrees and arcseconds. */
        public static CoordReader DEGREES = new CoordReader() {
            public double getAlpha( Object[] tuple ) {
                return RADIANS.getAlpha( tuple ) * FROM_DEG;
            }
            public double getDelta( Object[] tuple ) {
                return RADIANS.getDelta( tuple ) * FROM_DEG;
            }
            public double getError( Object[] tuple ) {
                return RADIANS.getError( tuple ) * FROM_ARCSEC;
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

        /**
         * Extracts the per-object error radius from a tuple.
         *
         * @param   tuple  object tuple intended for this matcher
         * @return  error radius in radians
         */
        double getError( Object[] tuple );
    }

    /**
     * MatchEngine class that behaves like ErrorSkyMatchEngine but uses
     * human-friendly units (degrees and arcseconds) rather than radians
     * for tuple elements and match parameters.
     */
    public static class InDegrees extends ErrorSkyMatchEngine {
        private final ValueInfo[] tupleInfos_;
        private final DescribedValue[] matchParams_;

        /**
         * Constructor.
         *
         * @param  pixellator  handles sky pixellisation
         * @param  scaleRadians  initial value for length scale, in radians
         */
        public InDegrees( SkyPixellator pixellator, double scaleRadians ) {
            super( pixellator, scaleRadians );
            ValueInfo[] infos0 = super.getTupleInfos();
            tupleInfos_ = new ValueInfo[] {
                inDegreeInfo( infos0[ 0 ] ),
                inDegreeInfo( infos0[ 1 ] ),
                inArcsecInfo( infos0[ 2 ] ),
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
            double maxRadiusArcsec = 0;
            for ( NdRange inRange : inRanges ) {
                maxRadiusArcsec =
                    Math.max( maxRadiusArcsec,
                              getNumberValue( inRange.getMaxs()[ 2 ] ) );
            }
            double maxRadius = maxRadiusArcsec * FROM_ARCSEC;
            return createExtendedSkyBoundsDegrees( inRanges[ index ], 0, 1,
                                                   2 * maxRadius );
        }
    }
}
