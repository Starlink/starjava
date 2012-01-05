package uk.ac.starlink.ttools.cone;

import java.io.IOException;

/**
 * Defines coverage of a sky positional search service.
 *
 * @author   Mark Taylor
 * @since    16 Dec 2011
 */
public interface Footprint {

    /**
     * Must be called before any of the query methods are used.
     * May be time consuming (it may contact an external service).
     * It is legal to call this method multiple times from the same or
     * different threads.  If {@link #getCoverage} returns non-null,
     * this method will return directly.
     * Following a successful or error return of this method,
     * {@link #getCoverage} will return non-null.
     */
    void initFootprint() throws IOException;

    /**
     * Returns the footprint coverage type.
     * If the footprint is not ready for use, null is returned.
     * In that case, {@link #initFootprint} must be called before use.
     *
     * @return  footprint coverage
     */
    Coverage getCoverage();

    /**
     * Indicates whether a given disc on the sphere overlaps, or may overlap
     * with this footprint.  False positives are permitted.
     *
     * @param  alphaDeg   central longitude in degrees
     * @param  deltaDeg   central latitude in degrees
     * @param  radiusDeg  radius in degrees
     * @return   false if the given disc definitely does not overlap
     *                 this footprint; otherwise true
     * @throws  IllegalStateException  if <code>initFootprint</code>
     *                                 has not been called
     */
    boolean discOverlaps( double alphaDeg, double deltaDeg, double radiusDeg );

    /**
     * Describes a type of coverage.
     */
    public enum Coverage {

        /** No coverage data is known. */
        NO_DATA( Boolean.TRUE ),

        /** Coverage data is known; no sky regions are covered. */
        NO_SKY( Boolean.FALSE ),

        /** Coverage data is known; the whole sky is covered. */
        ALL_SKY( Boolean.TRUE ),

        /** Coverage data is known; some, but not all of the sky is covered. */
        SOME_SKY( null );

        private final Boolean knownResult_;

        /**
         * Constructor.
         *
         * @param  knownResult  fixed result value
         */
        Coverage( Boolean knownResult ) {
            knownResult_ = knownResult;
        }

        /**
         * Returns the single fixed answer to all footprint coverage queries.
         * For an interesting footprint, the result is null (no fixed answer),
         * but for other types a fixed value of True or False may be returned.
         *
         * @return  constant answer to footprint queries
         */
        public Boolean getKnownResult() {
            return knownResult_;
        }
    }
}
