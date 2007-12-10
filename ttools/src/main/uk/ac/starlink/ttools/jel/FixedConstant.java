package uk.ac.starlink.ttools.jel;

/**
 * Constant implementation which always has the same value.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public class FixedConstant implements Constant {

    private final Class clazz_;
    private final Object value_;

    /**
     * Constructs a constant with a given value and class.
     *
     * @param  clazz  content class
     * @param  value  value
     */
    public FixedConstant( Object value, Class clazz ) {
        value_ = value;
        clazz_ = clazz;
    }

    /**
     * Constructs a constant with a given value.
     * The class is the class of <code>value</code>.
     *
     * @param  value  value
     */
    public FixedConstant( Object value ) {
        this( value, value.getClass() );
    }

    public Class getContentClass() {
        return clazz_;
    }

    public Object getValue() {
        return value_;
    }
}
