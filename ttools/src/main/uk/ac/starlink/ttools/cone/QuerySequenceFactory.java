package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Object which can generate an iterator over cone search specifiers.
 *
 * @author   Mark Taylor
 * @since    16 Oct 2007
 */
public interface QuerySequenceFactory {

    /**
     * Creates a query sequence.
     *
     * @param  table  input table
     */
    ConeQueryRowSequence createQuerySequence( StarTable table )
            throws IOException;
}
