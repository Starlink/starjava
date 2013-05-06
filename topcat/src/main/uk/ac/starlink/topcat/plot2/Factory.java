package uk.ac.starlink.topcat.plot2;

/**
 * Simple interface for deferred production of an object.
 * Differs from {@link java.util.concurrent.Callable}
 * in that it does not throw any checked exception.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public interface Factory<T> {

    /**
     * Returns an object.
     *
     * @return  result
     */
    T getItem();
}
