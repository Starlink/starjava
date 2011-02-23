package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.vo.TapQuery;

/**
 * Object which can get the output table from a TapQuery.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2011
 */
public interface TapResultProducer {

    /**
     * Waits until the TAP query has completed, and then either returns
     * the output table or throws an appropriate error.
     *
     * @param   query   query whose result to return
     * @return   table result
     * @throws   java.io.InterruptedIOException  if the wait is interrupted
     * @throws   IOException  if the query ended in error status or some
     *           other error occurred during TAP communications
     */
    public StarTable waitForResult( TapQuery query ) throws IOException;
}
