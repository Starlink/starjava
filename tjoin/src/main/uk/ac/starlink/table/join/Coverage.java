package uk.ac.starlink.table.join;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Provides a compact representation of a region of space.
 * An instance of this class is in general mutable,
 * and starts off empty but can be extended by feeding it tuples via
 * the {@link #extend} method.
 * Once populated, tests for inclusion of other tuples in the coverage
 * thus represented can be performed by use of the {@link #createTestFactory}
 * method.
 *
 * <p>The details of how the accumulated set of input tuples affects
 * the inclusion of test tuples are determined by the nature of the
 * matching a given coverage implementation is designed to support.
 * When used for crossmatching, the idea is that the coverage represents
 * the region of space in which an external tuple must be included
 * in order to be a potential match.
 *
 * <p>Instances of this class are not thread-safe, so should not be used
 * from multiple threads concurrently.
 *
 * @author  Mark Taylor
 * @since   8 Jun 2022
 */
public interface Coverage {

    /**
     * Instance representing full coverage.
     * The {@link #createTestFactory} test always returns true.
     * Note that in accordance with the general contract, the set operations
     * are only supported for use with compatible coverage objects,
     * so attempting to perform an intersection with another coverage instance
     * will fail (with an UnsupportedOperationException).
     */
    public static final Coverage FULL = new Coverage() {
        public void extend( Object[] tuple ) {
        }
        public boolean isEmpty() {
            return false;
        }
        public Supplier<Predicate<Object[]>> createTestFactory() {
            return () -> ( tuple -> true );
        }
        public void union( Coverage other ) {
        }
        public void intersection( Coverage other ) {
            if ( other != this ) {
                throw new UnsupportedOperationException();
            }
        }
        public String coverageText() {
            return "full";
        }
    };

    /**
     * Extends this coverage by adding a tuple to it.
     * The tuple contains the coordinates of a row from a table
     * suitable for use with this coverage.
     * The state of this coverage object should not depend on the
     * order in which tuples are submitted.
     *
     * @param  tuple  coordinate data
     */
    void extend( Object[] tuple );

    /**
     * Returns a supplier for an object that can test whether given
     * tuples are considered to fall within the current state of this
     * coverage object.
     * The returned Supplier must be thread-safe, and should not be
     * affected by subsequent changes to the state of this coverage object.
     * The Predicates it dispenses however are not required to be thread-safe,
     * and must not be used concurrently from different threads.
     *
     * @return   supplier of tuple inclusion tests
     */
    Supplier<Predicate<Object[]>> createTestFactory();

    /**
     * Returns true if the coverage represents the empty set.
     *
     * @return   true iff the {@link #createTestFactory} test
     *           is guaranteed to return false
     */
    boolean isEmpty();

    /**
     * Narrows this coverage object to contain only the intersection of
     * its current state and the supplied coverage.
     *
     * @param  other  different coverage object of a type
     *                assumed compatible with this object
     */
    void intersection( Coverage other );

    /**
     * Modifies the state of this coverage object as if all the tuples
     * fed to the other had been fed to this one as well as its current
     * contents.
     *
     * @param  other  different coverage object of a type
     *                assumed compatible with this object
     */
    void union( Coverage other );

    /**
     * Provides a short, human-readable indication of the coverage.
     *
     * @return  string representation
     */
    String coverageText();
}
