package uk.ac.starlink.table;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines an object which can output a <code>StarTable</code> in a particular
 * format.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface StarTableWriter {
   
    /**
     * Writes a <code>StarTable</code> object to a given output stream.
     * The implementation can assume that <code>out</code> is suitable for
     * direct writing (for instance it should not normally wrap it in a 
     * {@link java.io.BufferedOutputStream}), and should not close it
     * at the end of the call.
     *
     * <p>Not all table writers are capable of writing to a stream;
     * an implementation may throw a {@link TableFormatException} to 
     * indicate that it cannot do so.
     *
     * @param  startab  the table to write
     * @param  out  the output stream to which <code>startab</code> should be 
     *              written
     * @throws  TableFormatException  if this table cannot be written to a
     *          stream
     * @throws  IOException  if there is some I/O error
     */
    void writeStarTable( StarTable startab, OutputStream out )
            throws TableFormatException, IOException;

    /**
     * Writes a <code>StarTable</code> object to a given location.
     * Implementations are free to interpret the <code>location</code> argument
     * in any way appropriate for them.  Typically however the location
     * will simply be used to get an output stream (for instance interpreting
     * it as a filename).  In this case the <code>sto</code> argument should
     * normally be used to turn <code>location</code> into a stream.
     * {@link StreamStarTableWriter} provides a suitable implementation
     * for this case.
     *
     * @param  startab  table to write
     * @param  location   destination for <code>startab</code>
     * @param  sto     StarTableOutput which dispatched this request
     * @throws  TableFormatException   if <code>startab</code> cannot be
     *          written to <code>location</code>
     * @throws  IOException  if there is some I/O error
     */
    void writeStarTable( StarTable startab, String location, 
                         StarTableOutput sto )
            throws TableFormatException, IOException;

    /**
     * Indicates whether the destination is of a familiar form for this
     * kind of writer.  This may be used to guess what kind of format
     * a table should be written in.  Implementations should return
     * <code>true</code> for values of <code>location</code> which look like
     * the normal form for their output format, for instance one with
     * the usual file extension.
     *
     * @param  location  the location name (probably filename)
     * @return <code>true</code> iff it looks like a file this writer would
     *         normally write
     */
    boolean looksLikeFile( String location );

    /**
     * Gives the name of the format which is written by this writer.
     * Matching against this string may be used by callers to identify
     * or select this writer from a list.
     *
     * @return   a short string identifying the output format of this writer
     */
    String getFormatName();

    /**
     * Returns a string suitable for use as the value of a MIME 
     * Content-Type header.  If no suitable MIME type is available
     * or known, one of "<code>application/octet-stream</code>"
     * (for binary formats) or "<code>text/plain</code>" for ASCII ones)
     * is recommended.
     *
     * @return   MIME content type
     */
    String getMimeType();
}
