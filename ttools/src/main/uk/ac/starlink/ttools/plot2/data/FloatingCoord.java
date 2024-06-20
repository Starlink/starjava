package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Function;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeDomain;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.table.ValueInfo;
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

    /** Coordinate instance used for weighting values. */
    public static FloatingCoord WEIGHT_COORD = createCoord(
        new InputMeta( "weight", "Weight" )
       .setShortDescription( "Non-unit weighting of data points" )
       .setXmlDescription( new String[] {
            "<p>Weighting of data points.",
            "If supplied, each point contributes a value",
            "to the histogram equal to the data value",
            "multiplied by this coordinate.",
            "If not supplied, the effect is the same as",
            "supplying a fixed value of one.",
            "</p>",
        } )
    , false );

    /**
     * Constructor.
     *
     * @param   meta  input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   isDouble  true for double precision, false for single
     * @param   inputClass   class of input coordinate quantity
     */
    private FloatingCoord( InputMeta meta, boolean isRequired,
                           Domain<?> domain, boolean isDouble ) {
        super( meta, isRequired, domain,
               isDouble ? StorageType.DOUBLE : StorageType.FLOAT );
        nan_ = isDouble ? Double.valueOf( Double.NaN )
                        : Float.valueOf( Float.NaN );
    }

    public Function<Object[],Number> inputStorage( ValueInfo[] infos,
                                                   DomainMapper[] dms ) {
        return userValues -> {
            Object c = userValues[ 0 ];
            return c instanceof Number ? ((Number) c) : nan_;
        };
    }

    /**
     * Reads a floating point value from an appropriate field
     * in a given Tuple.
     *
     * @param  tuple  tuple
     * @param  icol  index of column in tuple corresponding to this Coord
     * @return  value of floating point field
     */
    public double readDoubleCoord( Tuple tuple, int icol ) {
        return tuple.getDoubleValue( icol );
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
        return new FloatingCoord( meta, isRequired, SimpleDomain.NUMERIC_DOMAIN,
                                  PlotUtil.storeFullPrecision() );
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
        final Double nan = Double.valueOf( Double.NaN );
        return new FloatingCoord( meta, isRequired, TimeDomain.INSTANCE, true ){
            @Override
            public Function<Object[],Number>
                    inputStorage( ValueInfo[] infos, DomainMapper[] dms ) {
                if ( dms[ 0 ] instanceof TimeMapper ) {
                    final TimeMapper tMap = (TimeMapper) dms[ 0 ];
                    return userValues -> tMap.toUnixSeconds( userValues[ 0 ] );
                }
                else {
                    return userValues -> Double.NaN;
                }
            }
        };
    }
}
