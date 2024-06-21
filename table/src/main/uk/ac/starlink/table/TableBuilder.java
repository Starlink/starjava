package uk.ac.starlink.table;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.util.DataSource;

/**
 * Interface for objects which can construct a <code>StarTable</code> from
 * a data resource.
 * TableBuilder implementations may also choose to implement
 * {@link MultiTableBuilder}.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface TableBuilder {

    /**
     * Constructs a {@link StarTable} based on a given <code>DataSource</code>.
     * If the source is not recognised or this builder does not know
     * how to construct a table from it, then a 
     * {@link TableFormatException} should be thrown.
     * If this builder thinks it should be able to handle the source
     * but an error occurs during processing, an <code>IOException</code>
     * can be thrown.
     * <p>
     * The <code>wantRandom</code> parameter is used to indicate whether,
     * ideally, a random-access table should be returned.  There is no
     * requirement for the builder to honour this request, but if
     * it knows how to make both random and non-random tables, it can
     * use this flag to decide which to return.
     * <p>
     * <strong>Note:</strong> the presence of the <code>wantRandom</code>
     * parameter is somewhat misleading.  TableBuilder implementations 
     * usually should, and do, ignore it (it would be removed from the
     * interface if it were not for backward compatibility issues).
     * Regardless of the value of this parameter, implementations should
     * return a random-access table only if it is easy for them to do so;
     * in particular they should not use the supplied 
     * <code>storagePolicy</code>, or any other resource-expensive measure,
     * to randomise a sequential table just because the 
     * <code>wantRandom</code> parameter is true.
     *
     * @param  datsrc  the DataSource containing the table resource
     * @param  wantRandom  whether, preferentially, a random access table
     *         should be returned
     * @param  storagePolicy  a StoragePolicy object which may be used to
     *         supply scratch storage if the builder needs it
     * @return  a StarTable made out of <code>datsrc</code>
     * @throws TableFormatException  if the table is not of a kind that
     *         can be handled by this handler
     * @throws IOException  if an unexpected I/O error occurs during processing
     */
    StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                             StoragePolicy storagePolicy ) 
            throws IOException;

    /**
     * Reads a table from an input stream and writes it a row at a time 
     * to a sink.  Not all implementations will be able to do this;
     * for instance, extracting the table from the data may be a two-pass
     * process.  Implementations which are unable to perform this 
     * function should throw a {@link TableFormatException}.
     *
     * <p>The input stream should be prepared for use prior to calling
     * this method, so implementations should not in general attempt to
     * decompress or buffer <code>istrm</code>.
     *
     * @param  istrm   input stream containing table data
     * @param  sink    destination of the table
     * @param  pos     position identifier describing the location of the
     *                 table within the stream;
     *                 see {@link uk.ac.starlink.util.DataSource#getPosition}
     *                 (may be null)
     * @throws  TableFormatException  if the table can't be streamed or
     *          the data is malformed
     * @throws  IOException  if some other error occurs
     */
    void streamStarTable( InputStream istrm, TableSink sink, String pos )
            throws IOException;

    /**
     * Indicates whether this builder is able to turn a resource of
     * media type indicated by <code>flavor</code> into a table.
     * It should return <code>true</code> if it thinks that its 
     * {@link #streamStarTable} method stands a reasonable chance of 
     * successfully constructing a <code>StarTable</code> from a 
     * <code>DataSource</code> whose input stream is described by the
     * {@link java.awt.datatransfer.DataFlavor} <code>flavor</code>.
     * It will typically make this determination based on the flavor's
     * MIME type.  
     * <p>
     * This method should only return <code>true</code> if the flavor looks like
     * it is targeted at this builder; for instance a builder which 
     * uses a text-based format should return false for a 
     * flavor which indicates a MIME type of <code>text/plain</code>.
     * <p>
     * This method is used in supporting drag and drop functionality
     * (see {@link 
     * StarTableFactory#canImport(java.awt.datatransfer.DataFlavor[])}).
     *
     * @param  flavor  the DataFlavor whose suitability as stream input
     *         is to be assessed
     * @return <code>true</code> iff this builder reckons it stands a good 
     *         chance of turning a stream of type <code>flavor</code> into a 
     *         <code>StarTable</code>
     */
    boolean canImport( DataFlavor flavor );

    /**
     * Indicates whether the given location string is of a familiar form
     * for this builder.
     * Implementations should return <code>true</code> if there is
     * a good chance that a file with the given <code>location</code>
     * can be interpreted by this reader, for instance if it has a
     * suitable file extension.
     *
     * <p>This method may be used to guess, on a best-efforts basis,
     * whether this builder is suitable for reading a file from
     * a given location.  Attempts may still be made to read inputs
     * for which this method returns false.
     * It is less important for builders that can recognise
     * files by magic number, which is generally preferable to using
     * filenames.
     *
     * @param  location   the location string, such as a filename or URL
     *                    (not null)
     * @return  true iff there is a good chance that the named input
     *          can be interpreted by this reader
     */
    boolean looksLikeFile( String location );

    /**
     * Returns the name of the format which can be read by this handler.
     * Matching against this string may be used by callers to identify
     * or select this handler from a list.
     *
     * @return  one-word description of this handler's format
     */
    String getFormatName();
}
