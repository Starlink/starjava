package uk.ac.starlink.ttools.jel;

/**
 * Defines a typed value.  Not necessarily constant in that the value will 
 * not change, but in the context of the jel package it is not dependent 
 * on the column index.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public interface Constant {

    /** 
     * Returns a class of which this object's value will be an instance.
     *
     * @return   content class
     */
    Class getContentClass();

    /**
     * Returns this object's value.  Not necessarily always the same.
     * Must be an instance of {@link #getContentClass} (or null).
     *
     * @return  value
     */
    Object getValue();
}
