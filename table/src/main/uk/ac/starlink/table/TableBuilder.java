package uk.ac.starlink.table;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import uk.ac.starlink.util.DataSource;

/**
 * Interface for objects which can construct a <tt>StarTable</tt> from
 * a data resource.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface TableBuilder {

    /**
     * Constructs a {@link StarTable} based on a given <tt>DataSource</tt>.
     * If the source is not recognised or this builder does not know
     * how to construct a table from it, then a 
     * {@link TableFormatException} should be thrown.
     * If this builder thinks it should be able to handle the source
     * but an error occurs during processing, an <tt>IOException</tt>
     * can be thrown.
     * <p>
     * The <tt>wantRandom</tt> parameter is used to indicate whether,
     * ideally, a random-access table should be returned.  There is no
     * requirement for the builder to honour this request, but if
     * it knows how to make both random and non-random tables, it can
     * use this flag to decide which to return.
     *
     * @param  datsrc  the DataSource containing the table resource
     * @param  wantRandom  whether, preferentially, a random access table
     *         should be returned
     * @param  storagePolicy  a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return  a StarTable made out of <tt>datsrc</tt>
     * @throws TableFormatException  if the table is not of a kind that
     *         can be handled by this handler
     * @throws IOException  if an unexpected I/O error occurs during processing
     */
    StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                             StoragePolicy storagePolicy ) 
            throws IOException;

    /**
     * Indicates whether this builder is able to turn a resource of
     * media type indicated by <tt>flavor</tt> into a table.
     * It should return <tt>true</tt> if it thinks that its 
     * {@link #makeStarTable} method stands a reasonable chance of 
     * successfully constructing a <tt>StarTable</tt> from a 
     * <tt>DataSource</tt> whose input stream is described by the
     * {@link java.awt.datatransfer.DataFlavor} <tt>flavor</tt>.
     * It will typically make this determination based on the flavor's
     * MIME type.  
     * <p>
     * For reasons of efficiency it is probably not a 
     * good idea to return <tt>true</tt> unless the flavor looks like
     * it is targeted at this builder; for instance a builder which 
     * uses a text-based format should probably return false for a 
     * flavor which indicates a MIME type of <tt>text/plain</tt>.
     * <p>
     * This method is used in supporting drag and drop functionality
     * (see {@link 
     * StarTableFactory#canImport(java.awt.datatransfer.DataFlavor[])}).
     *
     * @param  flavor  the DataFlavor whose suitability as stream input
     *         is to be assessed
     * @return <tt>true</tt> iff this builder reckons it stands a good 
     *         chance of turning a stream of type <tt>flavor</tt> into a 
     *         <tt>StarTable</tt>
     */
    boolean canImport( DataFlavor flavor );

    /**
     * Returns the name of the format which can be read by this handler.
     * Matching against this string may be used by callers to identify
     * or select this handler from a list.
     *
     * @return  one-word description of this handler's format
     */
    String getFormatName();
}
