package uk.ac.starlink.util;

/**
 * Utility sub-interface of Splittable suitable for use with
 * splittable data that can be iterated over.
 *
 * @author   Mark Taylor
 * @since    13 Sep 2019
 */
public interface SplittableSequence<S extends SplittableSequence<S>>
        extends Splittable<S> {

    /**
     * Move to the next item in the sequence.
     * Must be called before accessing each item, including the first one.
     * Returns false when there are no items left.
     *
     * @return  true iff the current sequence element has data
     */
    boolean next();
}
