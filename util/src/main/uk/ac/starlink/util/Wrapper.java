package uk.ac.starlink.util;

/**
 * Mixin interface which indicates an object built on top of a base object.
 * 
 * @author   Mark Taylor
 * @since    17 Aug 2007
 * @see      WrapUtils
 */
public interface Wrapper {

    /**
     * Returns the base object.
     *
     * @return  base
     */
    Object getBase();
}
