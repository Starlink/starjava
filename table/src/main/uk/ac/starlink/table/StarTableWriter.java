package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Defines an object which can output a <tt>StarTable</tt> in a particular
 * format.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface StarTableWriter {
   
    /**
     * Writes a <tt>StarTable</tt> object to a given location.
     * If possible, a location of "-" should be taken as a request to
     * write to standard output.
     *
     * @param  startab  the table to write
     * @param  location  the destination of the written object 
     *         (probably, but not necessarily, a filename)
     */
    void writeStarTable( StarTable startab, String location )
            throws IOException;

    /**
     * Indicates whether the destination is of a familiar form for this
     * kind of writer.  This may be used to guess what kind of format
     * a table should be written in.  Implementations should return
     * <tt>true</tt> for values of <tt>location</tt> which look like
     * the normal form for their output format, for instance one with
     * the usual file extension.
     *
     * @param  location  the location name (probably filename)
     * @return <tt>true</tt> iff it looks like a file this writer would
     *         normally write
     */
    boolean looksLikeFile( String location );

    /**
     * Gives the name of the format which is written by this writer.
     * Matching against this string may be used by callers to identify
     * or select this writer from a list.
     *
     * @param   a short string identifying the output format of this writer
     */
    String getFormatName();
}
