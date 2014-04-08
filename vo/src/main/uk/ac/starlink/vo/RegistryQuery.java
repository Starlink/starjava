package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import uk.ac.starlink.table.DescribedValue;

/**
 * Describes a query on a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    4 Jan 2005
 */
public interface RegistryQuery {

    /**
     * Executes the query described by this object and returns an 
     * Iterator over {@link RegResource} objects.
     * Note that the iterator's <code>next</code> method may throw the
     * unchecked exception 
     * {@link uk.ac.starlink.registry.RegistryQueryException} with a cause
     * indicating the underlying error in case of a registry access problem.
     *
     * @return  iterator over {@link RegResource}s
     */
    Iterator<RegResource> getQueryIterator() throws IOException;

    /**
     * Executes the query described by this object and returns the result as
     * an array of {@link RegResource}s.
     *
     * @return   resource list
     */
    RegResource[] getQueryResources() throws IOException;

    /**
     * Returns the query text.
     *
     * @return  query
     */
    String getText();

    /**
     * Returns the registry URL.
     *
     * @return url
     */
    URL getRegistry();

    /**
     * Returns a set of DescribedValue objects which characterise this query.
     * These would be suitable for use in the parameter list of a 
     * {@link uk.ac.starlink.table.StarTable} resulting from the execution
     * of this query.
     */
    DescribedValue[] getMetadata();
}
