package uk.ac.starlink.ttools.calc;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Defines an operation which turns an input tuple into an output tuple.
 * Suitable for use with {@link MultiServiceColumnCalculator}.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public interface ServiceOperation {

    /**
     * Returns the metadata-only table describing the rows which will
     * be returned by this operation.  Used for passing to
     * {@link uk.ac.starlink.table.TableSink#acceptMetadata}.
     * Its data must not be read.
     *
     * @return   data-less table
     */
    public StarTable getResultMetadata();

    /**
     * Calculates the output tuple for a given input tuple.
     *
     * @param  tuple  input tuple
     * @return  output tuple, corresponding to this object's declared metadata
     */
    public Object[] calculateRow( Object[] tuple ) throws IOException;
}
