package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import uk.ac.starlink.ttools.plot2.Slow;

/**
 * Provides a data-bearing object capable of providing the actual data
 * for a number of data specifications.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
public interface DataStoreFactory {
  
    /**
     * Generates a DataStore capable of supplying the data for a given
     * list of DataSpec objects.
     * The <code>prevStore</code> argument may optionally supply the
     * result of a previous invocation of this method.
     * The implementation may choose to make use of the internal state
     * of such an instance for efficiency, for instance by re-using data
     * that has already been read.
     *
     * <p>Since the bulk data is managed by the DataStore object,
     * care should be taken about what happens to the DataStore
     * objects supplied to and returned from this method.
     * In particular, code both invoking and implementing this method should
     * usually make sure not to keep a reference to the <code>prevStore</code>
     * argument.
     *
     * <p>This method may perform the actual reading, and therefore take time.
     * It is not intended to be invoked on the event dispatch thread.
     *
     * @param   specs  data specifications; some elements may be null
     * @param   prevStore  previously obtained DataStore, or null
     * @return   new data store
     */
    @Slow
    DataStore readDataStore( DataSpec[] specs, DataStore prevStore )
            throws IOException, InterruptedException;
}
