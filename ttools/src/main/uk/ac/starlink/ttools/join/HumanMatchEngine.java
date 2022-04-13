package uk.ac.starlink.ttools.join;

import java.util.logging.Logger;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.NdRange;
import uk.ac.starlink.ttools.func.CoordsRadians;

/**
 * MatchEngine adaptor which transforms the base engine so that it
 * uses more human-friendly units.  Currently, this means that it uses 
 * eschews radians in favour of degrees or arcseconds for angular quantities;
 * it decides which on the basis of UCDs.
 * In other respects, this engine will behave
 * exactly the same as its base engine.  If the base engine has no
 * human-unfriendly units, this one should behave exactly the same.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class HumanMatchEngine implements MatchEngine {

    private final MatchEngine baseEngine_;
    private final ValueInfo[] tupleInfos_;
    private final DescribedValue[] matchParams_;
    private final DescribedValue[] tuningParams_;
    private final ValueWrapper[] tupleWrappers_;
    private final ValueWrapper scoreWrapper_;
    private final ValueInfo scoreInfo_;
    private final int nval_;
    private final boolean isIdentity_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.join" );

    /**
     * Constructor.
     *
     * @param   baseEngine  the match engine supplying the base behaviour
     *          for this one
     */
    public HumanMatchEngine( MatchEngine baseEngine ) {
        baseEngine_ = baseEngine;
        boolean isIdentity = true;

        /* Get translators for each element of this engine's tuples,
         * and store appropriately modified tuple descriptors. */
        ValueInfo[] tinfos = baseEngine.getTupleInfos();
        nval_ = tinfos.length;
        tupleWrappers_ = new ValueWrapper[ nval_ ];
        tupleInfos_ = new ValueInfo[ nval_ ];
        for ( int i = 0; i < nval_; i++ ) {
            tupleWrappers_[ i ] = createWrapper( tinfos[ i ] );
            tupleInfos_[ i ] = tupleWrappers_[ i ].wrapValueInfo( tinfos[ i ] );
            isIdentity &= tupleWrappers_[ i ].isIdentity();
        }

        /* Get translators for each of this engine's match parameters,
         * and store appropriately modified versions of the parameters. */
        DescribedValue[] mParams = baseEngine.getMatchParameters();
        matchParams_ = new DescribedValue[ mParams.length ];
        for ( int i = 0; i < mParams.length; i++ ) {
            DescribedValue param = mParams[ i ];
            ValueWrapper pWrapper = createWrapper( param.getInfo() );
            matchParams_[ i ] = pWrapper.wrapDescribedValue( param );
            isIdentity &= pWrapper.isIdentity();
        }

        /* Do the same for tuning parameters. */
        DescribedValue[] tParams = baseEngine.getTuningParameters();
        tuningParams_ = new DescribedValue[ tParams.length ];
        for ( int i = 0; i < tParams.length; i++ ) {
            DescribedValue param = tParams[ i ];
            ValueWrapper tWrapper = createWrapper( param.getInfo() );
            tuningParams_[ i ] = tWrapper.wrapDescribedValue( param );
            isIdentity &= tWrapper.isIdentity();
        }

        /* Get and store a wrapper for this engine's match score. */
        ValueInfo minfo = baseEngine.getMatchScoreInfo();
        if ( minfo != null ) {
            scoreWrapper_ = createWrapper( minfo );
            scoreInfo_ = scoreWrapper_.wrapValueInfo( minfo );
            isIdentity &= scoreWrapper_.isIdentity();
        }
        else {
            scoreWrapper_ = NULL_WRAPPER;
            scoreInfo_ = null;
        }
        isIdentity_ = isIdentity;
    }

    /**
     * Indicates whether this object simply duplicates the underlying
     * MatchEngine.
     *
     * @return   true if this wrapper makes no changes to underlying behaviour,
     *           false if it may make changes
     */
    public boolean isIdentity() {
        return isIdentity_;
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
    }

    public DescribedValue[] getTuningParameters() {
        return tuningParams_;
    }

    public ValueInfo[] getTupleInfos() {
        return tupleInfos_;
    }

    public Object[] getBins( Object[] tuple ) {
        return baseEngine_.getBins( unwrapTuple( tuple ) );
    }

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        return scoreWrapper_
              .wrapDouble( baseEngine_.matchScore( unwrapTuple( tuple1 ),
                                                   unwrapTuple( tuple2 ) ) );
    }

    public double getScoreScale() {
        return scoreWrapper_.wrapDouble( baseEngine_.getScoreScale() );
    }

    public ValueInfo getMatchScoreInfo() {
        return scoreInfo_;
    }

    public boolean canBoundMatch() {
        return baseEngine_.canBoundMatch();
    }

    public NdRange getMatchBounds( NdRange[] inRanges, int index ) {
        int nr = inRanges.length;
        NdRange[] unwrappedInRanges = new NdRange[ nr ];
        for ( int ir = 0; ir < nr; ir++ ) {
            NdRange inRng = inRanges[ ir ];
            unwrappedInRanges[ ir ] =
                new NdRange( toComparables( unwrapTuple( inRng.getMins() ) ),
                             toComparables( unwrapTuple( inRng.getMaxs() ) ) );
        }
        NdRange unwrappedResult =
            baseEngine_.getMatchBounds( unwrappedInRanges, index );
        return new NdRange( toComparables( wrapTuple( unwrappedResult
                                                     .getMins() ) ),
                            toComparables( wrapTuple( unwrappedResult
                                                     .getMaxs() ) ) );
    }

    /**
     * Unwraps a tuple of objects from a client of this engine, providing
     * one suitable for the base engine.
     *
     * @param  wrapped   tuple provided by client
     * @return   tuple suitable for base
     */
    private Object[] unwrapTuple( Object[] wrapped ) {
        Object[] unwrapped = new Object[ nval_ ];
        for ( int i = 0; i < nval_; i++ ) {
            unwrapped[ i ] = tupleWrappers_[ i ].unwrapValue( wrapped[ i ] );
        }
        return unwrapped;
    }

    /**
     * Wraps a tuple of objects from the the base engine, providing 
     * one suitable for this engine's clients.
     *
     * @param  unwrapped  tuple provided by base
     * @return  tuple suitable for client
     */
    private Object[] wrapTuple( Object[] unwrapped ) {
        Object[] wrapped = new Object[ nval_ ];
        for ( int i = 0; i < nval_; i++ ) {
            wrapped[ i ] = tupleWrappers_[ i ].wrapValue( unwrapped[ i ] );
        }
        return wrapped;
    }

    /**
     * Converts an array of <code>Object[]</code> to an array of
     * <code>Comparable[]</code>.  Anything which is not comparable is
     * replaced by null.
     *
     * @param  objs  objects
     * @return   comparable array with the same contents as <code>objs</code>
     */
    private Comparable<?>[] toComparables( Object[] objs ) {
        Comparable<?>[] comps = new Comparable<?>[ objs.length ];
        for ( int i = 0; i < comps.length; i++ ) {
            comps[ i ] = objs[ i ] instanceof Comparable 
                       ? (Comparable<?>) objs[ i ]
                       : null;
        }
        return comps;
    }

    /** 
     * Creates a new ValueWrapper instance suitable for adapting values
     * described by a given ValueInfo object.
     *
     * @param   info  value metadata
     * @return   new wrapper which will wrap <code>info</code> type values
     *           in a human-friendly way
     */
    private ValueWrapper createWrapper( ValueInfo info ) {
        String units = info == null ? null : info.getUnitString();
        Class<?> clazz = info == null ? null : info.getContentClass();

        /* If the units are radians, change them to something more
         * comprehensible. */
        if ( ( "radians".equals( units ) || "radian".equals( units ) ) &&
             ( clazz == Double.class || clazz == Number.class ) ) {

            /* For small angles, use arcseconds. */
            if ( isSmallAngle( info ) ) {
                return new DoubleFactorWrapper( CoordsRadians
                                               .ARC_SECOND_RADIANS, "arcsec" );
            }

            /* For large angles, use degrees. */
            else if ( isLargeAngle( info ) ) {
                return new DoubleFactorWrapper( CoordsRadians.DEGREE_RADIANS,
                                                "degrees" );
            }

            /* If in doubt, issue a warning and use degrees. */
            else {
                logger_.warning( "Unknown angular quantity " + info
                               + " - convert to degrees" );
                return new DoubleFactorWrapper( CoordsRadians.DEGREE_RADIANS,
                                                "degrees" );
            }
        }

        /* Otherwise, return a wrapper which does nothing to values. */
        else {
            return NULL_WRAPPER;
        }
    }

    /**
     * Indicates whether a given value is recognised as representing a large
     * angle (such as a coordinate of some kind).
     *
     * @param  info  value metadata
     * @return  true if info is known to represent a large angle
     */
    public boolean isLargeAngle( ValueInfo info ) {
        String ucd = info.getUCD();
        if ( ucd == null ) {
            return false;
        }
        String lucd = ucd.toLowerCase();
        return lucd.matches( "pos[\\._]"
                           + "(eq|az|earth|ecliptic|galactic|supergalactic)"
                           + "[\\._].*" )
            || lucd.startsWith( "pos.posAng".toLowerCase() );
    }

    /**
     * Indicates whether a given value is recognised as representing a small
     * angle (such as an error of some kind).
     *
     * @param  info  value metadata
     * @return  true if info is known to represent a small angle
     */
    public boolean isSmallAngle( ValueInfo info ) {
        String ucd = info.getUCD();
        if ( ucd == null ) {
            return false;
        }
        String lucd = ucd.toLowerCase();
        return lucd.startsWith( "pos.angDistance".toLowerCase() )
            || lucd.startsWith( "pos.angResolution".toLowerCase() )
            || lucd.startsWith( "phys.angSize".toLowerCase() );
    }

    /**
     * Returns a human-friendly version of a supplied MatchEngine.
     * If no changes are required, the original instance is returned.
     *
     * @param  base  original match engine
     * @return   human-friendly version
     */
    public static MatchEngine getHumanMatchEngine( MatchEngine base ) {
        HumanMatchEngine human = new HumanMatchEngine( base );
        return human.isIdentity() ? base : human;
    }

    /**
     * Defines the interface for an adapter which can modify values
     * (e.g. change their units.).
     */
    private static abstract class ValueWrapper {

        /**
         * Converts a wrapped value to an unwrapped one.
         *
         * @param   value  wrapped value
         * @return   unwrapped value
         */
        public abstract Object unwrapValue( Object value );

        /**
         * Converts an unwrapped value to a wrapped one. 
         *
         * @param   value  unwrapped value
         * @return  wrapped value
         */
        public abstract Object wrapValue( Object value );

        /**
         * Converts an unwrapped double precision number to a wrapped one.
         *
         * @param   value  unwrapped value
         * @return  wrapped value
         */
        public abstract double wrapDouble( double value );

        /**
         * Converts an unwrapped ValueInfo to a wrapped one.
         *
         * @param  info  unwrapped value info
         * @return  wrapped value info
         */
        public abstract ValueInfo wrapValueInfo( ValueInfo info );

        /**
         * Converts a wrapped value to an unwrapped one.
         *
         * @param   dv  unwrapped described value
         * @return  wrapped described value
         */
        public abstract DescribedValue wrapDescribedValue( DescribedValue dv );

        /**
         * Indicates whether this wrapper simply duplicates the underlying
         * values.
         *
         * @return   true if this wrapper makes no changes to values,
         *           false if it may change them
         */
        public abstract boolean isIdentity();
    }

    /**
     * Value wrapper instance which performs no transformations on its data.
     */
    private static ValueWrapper NULL_WRAPPER = new ValueWrapper() {
        public Object wrapValue( Object value ) {
            return value;
        }
        public Object unwrapValue( Object value ) {
            return value;
        }
        public double wrapDouble( double value ) {
            return value;
        }
        public ValueInfo wrapValueInfo( ValueInfo info ) {
            return info;
        }
        public DescribedValue wrapDescribedValue( DescribedValue dval ) {
            return dval;
        }
        public boolean isIdentity() {
            return true;
        }
    };

    /**
     * Value wrapper implementation which multiplies its values, 
     * which must be of type {@link java.lang.Double}, by some factor.
     */
    private static class DoubleFactorWrapper extends ValueWrapper {

        final double factor_;
        final String units_;

        /**
         * Constructor.
         *
         * @param  factor  the factor that values are multiplied by when
         *         unwrapping values
         * @param  units   name of the wrapped units
         */
        DoubleFactorWrapper( double factor, String units ) {
            factor_ = factor;
            units_ = units;
        }
        public Object wrapValue( Object value ) {
            return value instanceof Number
                 ? new Double( ((Number) value).doubleValue() / factor_ )
                 : null;
        }
        public Object unwrapValue( Object value ) {
            return value instanceof Number
                 ? new Double( ((Number) value).doubleValue() * factor_ )
                 : null;
        }
        public double wrapDouble( double value ) {
            return value / factor_;
        }
        public ValueInfo wrapValueInfo( ValueInfo info ) {
            DefaultValueInfo vinfo = new DefaultValueInfo( info );
            vinfo.setUnitString( units_ );
            return vinfo;
        }
        public DescribedValue wrapDescribedValue( final DescribedValue dval ) {
            return new DescribedValue( wrapValueInfo( dval.getInfo() ),
                                       wrapValue( dval.getValue() ) ) {
                public Object getValue() {
                    return wrapValue( dval.getValue() );
                }
                public void setValue( Object value ) {
                    dval.setValue( unwrapValue( value ) );
                }
            };
        }
        public boolean isIdentity() {
            return false;
        }
    }
}
