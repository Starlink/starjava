package uk.ac.starlink.ttools.jel;

/**
 * Constant implementation which always has the same value.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public class FixedConstant<T> implements Constant<T> {

    private final Class<T> clazz_;
    private final T value_;

    /**
     * Constructs a constant with a given value and class.
     *
     * @param  clazz  content class
     * @param  value  value
     */
    public FixedConstant( T value, Class<T> clazz ) {
        value_ = value;
        clazz_ = clazz;
    }

    public Class<T> getContentClass() {
        return clazz_;
    }

    public T getValue() {
        return value_;
    }

    public boolean requiresRowIndex() {
        return false;
    }

    /**
     * Constructs a constant with a given value.
     * The constant parameterised type is the runtime type of
     * the supplied value.
     *
     * @param   value  constant value
     * @return  new constant
     */
    public static <T> FixedConstant<T> createConstant( T value ) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) value.getClass();
        return new FixedConstant<T>( value, clazz );
    }
}
