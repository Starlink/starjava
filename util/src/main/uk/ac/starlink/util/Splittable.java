package uk.ac.starlink.util;

/**
 * Defines an object which can be split into two for subdivided processing.
 *
 * <p>This does a similar job to {@link java.util.Spliterator},
 * but it imposes no assumptions about the form of the split objects,
 * for instance that they form a sequence that can be iterated over
 * internally, which idiom forms the basis of the Java 8 streams framework.
 * Collections or sequences based on Splittable can use external
 * iteration, which allows better control in some cases.
 *
 * @author   Mark Taylor
 * @since    12 Sep 2019
 */
public interface Splittable<S extends Splittable<S>> {

    /**
     * Attempts to partition this object into two halves,
     * ideally of similar size.
     * If a non-null value is returned, then the content previously
     * contained by this object is now split between this object and
     * the returned object.  If for any reason a split is not carried out,
     * null is returned.
     *
     * <p>Following a successful call, the two parts may be processed 
     * in different threads.
     *
     * @return  other half of this splittable, or null
     * @see     java.util.Spliterator#trySplit
     */
    S split();

    /**
     * Provides an estimate of the number of processable items in this object.
     * A processable item is not a well-defined quantity,
     * but it should generally be something that can be processed fast.
     * For instance, if this object represents a collection of collections,
     * the value that should be returned is the total number of elements
     * rather than the number of collections.
     *
     * <p>If no estimate for the size is available,
     * a negative value should be returned.
     *
     * @return   approximate size, or negative value if not known
     */
    long splittableSize();
}
