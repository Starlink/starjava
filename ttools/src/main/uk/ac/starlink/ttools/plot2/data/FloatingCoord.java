package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Coord implementation for floating point values.
 * This covers both single and double precision.
 * Currently there is a factory method to generate an instance of this
 * class, which uses single or double precision according to a 
 * global configuration parameter.
 * Although in general double precision processing is important,
 * for plotting purposes it's not usually going to make a visible 
 * difference, and the cached storage requirements are cut in half
 * if you use single precision instead.
 * Maybe see about different configuration options for this in the future.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public class FloatingCoord extends SingleCoord {

    private final Number nan_;

    /**
     * Constructor.
     *
     * @param   meta  input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   isDouble  true for double precision, false for single
     * @param   inputClass   class of input coordinate quantity
     * @param   domain  DomainMapper subtype for this coord, or null
     */
    private FloatingCoord( InputMeta meta, boolean isRequired,
                           boolean isDouble, Class inputClass,
                           Class<? extends DomainMapper> domain ) {
        super( meta, isRequired, inputClass,
               isDouble ? StorageType.DOUBLE : StorageType.FLOAT, domain );
        nan_ = isDouble ? new Double( Double.NaN ) : new Float( Float.NaN );
    }

    public Object inputToStorage( Object[] userValues,
                                  DomainMapper[] mappers ) {
        Object c = userValues[ 0 ];
        return c instanceof Number ? ((Number) c) : nan_;
    }

    /**
     * Reads a floating point value from an appropriate column
     * in the current row of a given TupleSequence.
     *
     * @param  tseq  sequence positioned at a row
     * @param  icol  index of column in sequence corresponding to this Coord
     * @return  value of floating point column at the current sequence row
     */
    public double readDoubleCoord( TupleSequence tseq, int icol ) {
        return tseq.getDoubleValue( icol );
    }

    /**
     * Factory method to return an instance of this class.
     * Implementation is currently determined by the
     * {@link PlotUtil#storeFullPrecision} method.
     *
     * @param   meta   input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     * @return   instance
     */
    public static FloatingCoord createCoord( InputMeta meta,
                                             boolean isRequired ) {
        return new FloatingCoord( meta, isRequired,
                                  PlotUtil.storeFullPrecision(),
                                  Number.class, null );
    }

    /**
     * Returns a new time coordinate.  This works in the TimeDomain,
     * and the numeric value returned should normally be seconds since
     * the Unix epoch (1 Jan 1970 midnight).
     *
     * @param   meta   input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     * @return   instance
     */
    public static FloatingCoord createTimeCoord( InputMeta meta,
                                                 boolean isRequired ) {
        final Double nan = new Double( Double.NaN );
        return new FloatingCoord( meta, isRequired, true,
                                  Object.class, TimeMapper.class ) {
            @Override
            public Object inputToStorage( Object[] userValues,
                                          DomainMapper[] mappers ) {
                DomainMapper mapper = mappers[ 0 ];
                Object userValue = userValues[ 0 ];
                if ( mapper instanceof TimeMapper ) {
                    return ((TimeMapper) mapper).toUnixSeconds( userValue );
                }
                else {
                    return userValue instanceof Number
                         ? ((Number) userValue)
                         : nan;
                }
            }
        };
    }
}
