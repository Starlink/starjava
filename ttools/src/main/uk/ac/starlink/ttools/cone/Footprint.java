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
     * different threads.  If {@link #isFootprintReady} returns true,
     * this method will return directly.
     * Following a successful return of this method,
     * {@link #isFootprintReady} will return true.
     */
    void initFootprint() throws IOException;

    /**
     * Indicates whether the query methods of this object are ready for
     * use.  If not, {@link #initFootprint} should be called.
     *
     * @return  true iff query methods may be called
     */
    boolean isFootprintReady();

    /**
     * Indicates whether a given disc on the sphere overlaps, or may overlap
     * with this footprint.  False positives are permitted.
     *
     * @param  alphaDeg   central longitude in degrees
     * @param  deltaDeg   central latitude in degrees
     * @param  radiusDeg  radius in degrees
     * @return   false if the given disc definitely does not overlap
     *                 this footprint; otherwise true
     */
    boolean discOverlaps( double alphaDeg, double deltaDeg, double radiusDeg );
}
