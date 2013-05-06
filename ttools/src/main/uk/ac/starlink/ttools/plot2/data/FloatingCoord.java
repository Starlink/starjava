package uk.ac.starlink.ttools.plot2.data;

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
     * @param   name   user-directed coordinate name
     * @param   description  user-directed coordinate description
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   isDouble  true for double precision, false for single
     */
    private FloatingCoord( String name, String description, boolean isRequired,
                           boolean isDouble ) {
        super( name, description, isRequired, Number.class,
               isDouble ? StorageType.DOUBLE : StorageType.FLOAT );
        nan_ = isDouble ? new Double( Double.NaN ) : new Float( Float.NaN );
    }

    public Object userToStorage( Object[] userCoords ) {
        Object c = userCoords[ 0 ];
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
     * @param   name   user-directed coordinate name
     * @param   description  user-directed coordinate description
     * @param   isRequired  true if this coordinate is required for plotting
     * @return   instance
     */
    public static FloatingCoord createCoord( String name, String description,
                                             boolean isRequired ) {
        return new FloatingCoord( name, description, isRequired,
                                  PlotUtil.storeFullPrecision() );
    }
}
