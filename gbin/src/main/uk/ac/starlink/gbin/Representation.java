package uk.ac.starlink.gbin;

/**
 * Defines how an object type in a GBIN file will be represented
 * when the GBIN file is turned into a table.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2015
 */
public interface Representation<T> {

    /**
     * Returns the object type with which this representation presents values.
     * All calls to <code>representValue</code> must return an instance of
     * this class (or null).
     *
     * @return  representation class for values
     */
    Class<T> getContentClass();

    /**
     * Transforms a raw data value to the value as presented by this
     * representation.
     *
     * @param  value   raw value obtained from GBIN file
     * @return   presented value of object, must be compatible with
     *           declared content class
     */
    T representValue( Object value );

    /**
     * Indicates whether values presented by this object are suitable
     * for use as columns in a table.  If not, the assumption is that
     * it's a structured object, and its component parts may be
     * recursed through to acquire multiple records corresponding
     * to one value presented here.
     *
     * @return   true  iff represented values are suitable column values
     */
    boolean isColumn();
}
