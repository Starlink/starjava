package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * ConeQueryRowSequence sub-interface which additionally defines a method
 * for retrieving the result of the cone search itself.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2008
 */
public interface ConeResultRowSequence extends ConeQueryRowSequence {

    /**
     * Returns the result of the cone search for the current row of this 
     * sequence.  The work will typically be done using
     * {@link ConeMatcher#getConeResult}.
     *
     * <p>If no records in the cone are found, the return value may either
     * be null or (preferably) an empty table with the correct columns.
     *
     * @return  table giving rows strictly within the match criteria for
     *          the current row of this cone query sequence, or null
     */
    public StarTable getConeResult() throws IOException;
}
