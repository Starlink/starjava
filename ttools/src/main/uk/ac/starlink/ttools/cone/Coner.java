package uk.ac.starlink.ttools.cone;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Defines a service which can perform cone search-like operations.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2007
 */
public interface Coner {

    /**
     * Returns any configuration parameters associated with this object.
     *
     * @return  parameter array
     */
    Parameter[] getParameters();

    /**
     * Returns the name of the coordinate system used by this Coner.
     * Spatial matching is done using Right Ascension and Declination
     * in degrees but the exact coordinate system is up to this object.
     * This method should return a string such as "ICRS" which specifies
     * the ecliptic system in use.  It may return the empty string if
     * no assumption is made.
     *
     * <p>This string is used only for documentation purposes, for instance
     * in prompt strings issued to the user.
     *
     * @return  ecliptic coordinate system name
     */
    String getSkySystem();

    /**
     * Returns a searcher object which can perform the actual cone searches
     * as configured by this object's parameters.
     * If the <code>bestOnly</code> flag is set, then only the best match
     * is required.  The implementation may use this as a hint if it helps
     * efficiency, but is not obliged to return single-row tables, since
     * extraneous rows will be filtered out later.  Similarly any rows
     * which do not actually match the given criteria will be filtered out
     * later, so it is not an error to return too many rows.
     *
     * @param   env  execution environment
     * @param   bestOnly  true iff only the best match will be used
     */
    ConeSearcher createSearcher( Environment env, boolean bestOnly )
        throws TaskException;
}
