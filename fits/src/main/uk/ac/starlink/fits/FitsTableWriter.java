package uk.ac.starlink.fits;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;

/**
 * Handles writing of a StarTable in FITS binary format.
 * Not all columns can be written to a FITS table, only those ones
 * whose <tt>contentClass</tt> is in the following list:
 * <ul>
 * <li>Boolean
 * <li>Character
 * <li>Byte
 * <li>Short
 * <li>Integer
 * <li>Float
 * <li>Double
 * <li>Character
 * <li>String
 * <li>boolean[]
 * <li>char[]
 * <li>byte[]
 * <li>short[]
 * <li>int[]
 * <li>float[]
 * <li>double[]
 * <li>String[]
 * </ul>
 * In all other cases a warning message will be logged and the column
 * will be ignored for writing purposes.
 * <p>
 * Output is currently to fixed-width columns only.  For StarTable columns
 * of variable size, a first pass is made through the table data to
 * determine the largest size they assume, and the size in the output
 * table is set to the largest of these.  Excess space is padded
 * with some sort of blank value (NaN for floating point values,
 * spaces for strings, zero-like values otherwise).
 * <p>
 * Null cell values are written using some zero-like value, not a proper
 * blank value.  Doing this right would require some changes to the tables
 * infrastructure.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableWriter implements StarTableWriter {

    /**
     * Returns "FITS".
     *
     * @return  format name
     */
    public String getFormatName() {
        return "fits-basic";
    }

    /**
     * Returns true if <tt>location</tt> ends with something like ".fit"
     * or ".fits" or ".fts".
     *
     * @param  location  filename
     * @return true if it sounds like a fits file
     */
    public boolean looksLikeFile( String location ) {
        int dotPos = location.lastIndexOf( '.' );
        if ( dotPos > 0 ) {
            String exten = location.substring( dotPos + 1 ).toLowerCase();
            if ( exten.startsWith( "fit" ) || exten.equals( "fts" ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a table in FITS binary format.  Currently the output is
     * to a new file called <tt>location</tt>, in the first extension
     * (HDU 0 is a dummy header, since the primary HDU cannot hold a table).
     *
     * @param  startab  the table to write
     * @param  location  the filename to write to
     */
    public void writeStarTable( StarTable startab, String location )
            throws IOException {
        DataOutputStream strm = null;
        FitsTableSerializer serializer = new FitsTableSerializer( startab );
        try {
            strm = getOutputStream( location );
            writePrimary( startab, strm );
            serializer.writeHeader( strm );
            serializer.writeData( strm );
        }
        finally {
            if ( strm != null ) {
                strm.close();
            }
        }
    }

    /**
     * Called from {@link #writeStarTable} to write headers prior to the
     * BINTABLE header which contains the table proper.
     * The default implementation writes an empty primary HDU;
     * subclasses may write one or more headers, though the first one
     * should be a legal primary FITS HDU.
     *
     * @param  startab  the table which will be written into the next HDU
     * @param  strm     the stream down which it will be written
     */
    protected void writePrimary( StarTable startab, DataOutputStream strm )
            throws IOException {
        FitsConstants.writeEmptyPrimary( strm );
    }

    /**
     * Returns a stream ready to accept a table HDU.
     * Currently just opens a new file at <tt>location</tt> and
     * writes a dummy primary header to it.
     */
    private static DataOutputStream getOutputStream( String location )
            throws IOException {

        /* Interpret the name "-" as standard output. */
        DataOutputStream strm;
        File tmpFile = null;
        if ( location.equals( "-" ) ) {
            strm =
                new DataOutputStream( new BufferedOutputStream( System.out ) );
        }

        /* Otherwise, it's a filename. */
        else {

            /* Check that the file does not already exist.  If it does,
             * ensure that we are not actually overwriting the data in place.
             * This is for the case in which the original file has been,
             * and is still, mapped into memory (MappedFile) and
             * overwriting it would mess up all the data.  This would happen,
             * for instance, in the case that you try to save a table
             * from TOPCAT under the same name that you loaded it as.
             * Since the details of file mapping behaviour are dependent
             * on the OS, the following strategy can't be guaranteed to
             * work, but it's a fair bet under unix. */
            File file = new File( location );
            if ( file.exists() ) {
                tmpFile = new File( file.getPath() + ".bak" );
                if ( ! file.renameTo( tmpFile ) ) {
                    throw new IOException( "Failed to rename " + file +
                                           " -> " + tmpFile );
                }
            }
            final File tmpFile1 = tmpFile;
            OutputStream ostrm =
                new BufferedOutputStream( new FileOutputStream( location ) );
            strm = new DataOutputStream( ostrm ) {
                public void close() throws IOException {
                    super.close();
                    if ( tmpFile1 != null ) {
                        tmpFile1.delete();
                    }
                }
            };
        }

        /* Return the stream. */
        if ( tmpFile != null ) {
            tmpFile.deleteOnExit();
        }
        return strm;
    }
}
