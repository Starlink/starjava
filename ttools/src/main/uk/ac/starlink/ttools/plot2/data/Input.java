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
     * @param  valueClazz  data value class
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
}
