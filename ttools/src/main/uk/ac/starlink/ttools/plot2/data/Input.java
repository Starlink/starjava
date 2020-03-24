package uk.ac.starlink.ttools.plot2.data;

/**
 * Characterises a coordinate value as specified by the user.
 * There may be multiple Input values corresponding to a single
 * coordinate ({@link Coord} as used by the plotting system.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2014
 */
public class Input {

    private final InputMeta meta_;
    private final Class<?> valueClazz_;

    /**
     * Constructor.
     *
     * @param  meta   user-directed metadata
     * @param  valueClazz  data value class; all values must be an instance
     *                     of this type
     */
    public Input( InputMeta meta, Class<?> valueClazz ) {
        meta_ = meta;
        valueClazz_ = valueClazz;
    }

    /**
     * Returns user-directed metadata describing this input.
     *
     * @return  metadata
     */
    public InputMeta getMeta() {
        return meta_;
    }

    /**
     * Returns the data (super-)type of values described by this input.
     *
     * @return  value data type
     */
    public Class<?> getValueClass() {
        return valueClazz_;
    }

    /**
     * Indicates whether a data type is acceptable for this input.
     * The default implementation tests whether the given class is
     * assignable from the value class, but subclasses may override
     * this method to be more specific.
     *
     * @param   clazz   candidate value class
     * @return   true iff input data of the given class is acceptable
     */
    public boolean isClassAcceptable( Class<?> clazz ) {
        return valueClazz_.isAssignableFrom( clazz );
    }

    /**
     * Returns an object that behaves like this one but has different
     * metadata as supplied.
     *
     * @param  meta  new metadata object
     * @return   new Input instance with given metadata
     */
    public Input withMeta( InputMeta meta ) {
        final Input base = this;
        return new Input( meta, valueClazz_ ) {
            @Override
            public boolean isClassAcceptable( Class<?> clazz ) {
                return base.isClassAcceptable( clazz );
            }
        };
    }
}
