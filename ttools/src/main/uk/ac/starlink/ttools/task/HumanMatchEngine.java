package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.ttools.func.Coords;

/**
 * MatchEngine adaptor which transforms the base engine so that it
 * uses more human-friendly units.  Currently, this means that it uses 
 * eschews radians in favour of degrees for RA &amp; Dec,
 * and in favour of arcseconds for other quantities (assumed errors of
 * some kind).  Obviously, in other respects, this engine will behave
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
    private final ValueWrapper[] tupleWrappers_;
    private final ValueWrapper scoreWrapper_;
    private final ValueInfo scoreInfo_;
    private final int nval_;

    /**
     * Constructor.
     *
     * @param   baseEngine  the match engine supplying the base behaviour
     *          for this one
     */
    public HumanMatchEngine( MatchEngine baseEngine ) {
        baseEngine_ = baseEngine;

        /* Get translators for each element of this engine's tuples,
         * and store appropriately modified tuple descriptors. */
        ValueInfo[] tinfos = baseEngine.getTupleInfos();
        nval_ = tinfos.length;
        tupleWrappers_ = new ValueWrapper[ nval_ ];
        tupleInfos_ = new ValueInfo[ nval_ ];
        for ( int i = 0; i < nval_; i++ ) {
            tupleWrappers_[ i ] = createWrapper( tinfos[ i ] );
            tupleInfos_[ i ] = tupleWrappers_[ i ].wrapValueInfo( tinfos[ i ] );
        }

        /* Get translators for each of this engine's match parameters,
         * and store appropriately modified versions of the parameters. */
        DescribedValue[] params = baseEngine.getMatchParameters();
        matchParams_ = new DescribedValue[ params.length ];
        for ( int i = 0; i < params.length; i++ ) {
            DescribedValue param = params[ i ];
            matchParams_[ i ] = createWrapper( param.getInfo() )
                               .wrapDescribedValue( param );
        }

        /* Get and store a wrapper for this engine's match score. */
        ValueInfo minfo = baseEngine.getMatchScoreInfo();
        scoreWrapper_ = createWrapper( minfo );
        scoreInfo_ = scoreWrapper_.wrapValueInfo( minfo );
    }

    public DescribedValue[] getMatchParameters() {
        return matchParams_;
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

    public ValueInfo getMatchScoreInfo() {
        return scoreInfo_;
    }

    public boolean canBoundMatch() {
        return baseEngine_.canBoundMatch();
    }

    public Comparable[][] getMatchBounds( Comparable[] minTuple,
                                          Comparable[] maxTuple ) {
        Comparable[][] unwrappedResult =
            baseEngine_.getMatchBounds(
                            toComparables( unwrapTuple( minTuple ) ),
                            toComparables( unwrapTuple( maxTuple ) ) );
        return new Comparable[][] {
            toComparables( wrapTuple( unwrappedResult[ 0 ] ) ),
            toComparables( wrapTuple( unwrappedResult[ 1 ] ) ),
        };
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
    private Comparable[] toComparables( Object[] objs ) {
        Comparable[] comps = new Comparable[ objs.length ];
        for ( int i = 0; i < comps.length; i++ ) {
            comps[ i ] = objs[ i ] instanceof Comparable 
                       ? (Comparable) objs[ i ]
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
    private static ValueWrapper createWrapper( ValueInfo info ) {
        String units = info.getUnitString();
        Class clazz = info.getContentClass();

        /* If it's an RA or Dec description, change radians to degrees. */
        if ( matches( info, Tables.RA_INFO ) ||
             matches( info, Tables.DEC_INFO ) ) {
            if ( "radians".equals( units ) &&
                 clazz == Double.class || clazz == Number.class ) {
                return new DoubleFactorWrapper( Coords.DEGREE, "degrees" );
            }
        }

        /* Otherwise if it has radians, change radians to arcseconds. */
        else if ( "radians".equals( units ) &&
                  clazz == Double.class || clazz == Number.class ) {
            return new DoubleFactorWrapper( Coords.ARC_SECOND, "arcsec" );
        }

        /* Otherwise, return a wrapper which does nothing to values. */
        return NULL_WRAPPER;
    }

    /**
     * Utility method to determine whether an info resembles another one.
     * Currently matches name and content class.
     *
     * @param  info1  first info
     * @param  info2  second info
     * @return  true iff <code>info1</code> matches <code>info2</code>
     */
    private static boolean matches( ValueInfo info1, ValueInfo info2 ) {
        return info1 != null 
            && info2 != null
            && info1.getName().equals( info2.getName() )
            && info1.getContentClass().equals( info2.getContentClass() );
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
    }
}
