package uk.ac.starlink.ttools.jel;

/**
 * Defines a typed value.  Not necessarily constant in that the value will 
 * not change, but in the context of the jel package it is not dependent 
 * on the column index.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public interface Constant<T> {

    /** 
     * Returns a class of which this object's value will be an instance.
     *
     * @return   content class
     */
    Class<T> getContentClass();

    /**
     * Returns this object's value.  Not necessarily always the same.
     *
     * @return  value
     */
    T getValue();

    /**
     * Indicates whether evaluation of this constant needs to know the row
     * index.  If executing {@link #getValue} may result in a call to
     * {@link StarTableJELRowReader#getCurrentRow},
     * this method must return true.
     *
     * @return  true if evaluating this constant needs or may need to know
     *          the current row index
     */
    boolean requiresRowIndex();
}
