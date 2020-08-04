package uk.ac.starlink.util;

/**
 * Utility sub-interface of Splittable suitable for use with
 * splittable data that can be iterated over.
 *
 * <p>The main purpose of this interface is to act as standard
 * documentation for a <code>next</code> method where it is provided.
 *
 * @author   Mark Taylor
 * @since    13 Sep 2019
 */
public interface Sequence {

    /**
     * Move to the next item in the sequence.
     * Must be called before accessing each item, including the first one.
     * Returns false when there are no items left.
     *
     * <p>This method is declared to throw an untyped exception.
     * Subinterfaces are encouraged to override this method
     * restricting the exception type or throwing no exception.
     *
     * @return  true iff the current sequence element has data
     * @throws   Exception if there is some failure
     */
    boolean next() throws Exception;
}
