package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Represents one step of a table processing pipeline.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Mar 2005
 */
public interface ProcessingStep {

    /**
     * Performs a table filtering step.
     *
     * @param  base   input table
     * @return   output table
     */
    StarTable wrap( StarTable base ) throws IOException;

}
