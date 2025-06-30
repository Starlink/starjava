package uk.ac.starlink.topcat;

import uk.ac.starlink.ttools.jel.Constant;

/**
 * Named JEL value that can be set by users for use in expressions.
 * This is constant in the sense that it does not vary with row index,
 * but its value can be changed by the user.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2025
 */
public class UserConstant<T> implements Constant<T> {

    private final Class<T> clazz_;
    private String name_;
    private T value_;

    /**
     * Constructor.
     *
     * @param  clazz  value type
     * @param  name   constant name for use in JEL expressions
     * @param  value  initial value
     */
    public UserConstant( Class<T> clazz, String name, T value ) {
        clazz_ = clazz;
        name_ = name;
        value_ = value;
    }

    /**
     * Returns the value type for this constant.
     *
     * @return  class
     */
    public Class<T> getContentClass() {
        return clazz_;
    }

    /**
     * Returns the name of this constant.
     *
     * @return  name as used in JEL expressions
     */
    public String getName() {
        return name_;
    }

    /**
     * Sets the name of this constant.
     *
     * @param  name  new name
     */
    public void setName( String name ) {
        name_ = name;
    }

    /**
     * Returns the value of this constant.
     *
     * @return  current value
     */
    public T getValue() {
        return value_;
    }

    /**
     * Sets the value of this constant.
     *
     * @param  value  new value
     */
    public void setValue( T value ) {
        value_ = value;
    }

    /**
     * returns false
     */
    public boolean requiresRowIndex() {
        return false;
    }

    @Override
    public String toString() {
        return name_;
    }
}
