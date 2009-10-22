package uk.ac.starlink.table;

import java.io.IOException;
import uk.ac.starlink.util.DataSource;

/**
 * Interface for objects which can construct an array of StarTables
 * from a data resource.
 * In most cases, this interface should be implemented by objects which
 * also implement {@link TableBuilder}, but which additionally know how
 * to read more than one table at a time.
 *
 * @author   Mark Taylor
 * @since    22 Oct 2009
 */
public interface MultiTableBuilder extends TableBuilder {

    /**
     * Constructs an array of StarTables based on a given DataSource.
     * If the source is not recognised or a this object does not know
     * how to make tables from it, then a {@link TableFormatException}
     * should be thrown.  If this builder thinks it should be able to
     * handle the source but an error occurs during processing, an
     * <code>IOException</code> can be thrown.
     *
     * <p>The <code>position</code> of the data source should usually 
     * be ignored by this method, since it will return all the tables
     * in the given stream.
     *
     * @param  datsrc  the DataSource containing the table resource
     * @param  storagePolicy  a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return an array of StarTables read from <code>datsrc</code>
     *
     * @throws TableFormatException  if the table is not of a kind that
     *         can be handled by this handler
     * @throws IOException  if an unexpected I/O error occurs during processing
     */
    public StarTable[] makeStarTables( DataSource datsrc, 
                                       StoragePolicy storagePolicy )
            throws IOException;
}
