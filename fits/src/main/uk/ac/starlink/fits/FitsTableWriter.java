package uk.ac.starlink.fits;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StreamStarTableWriter;

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
public class FitsTableWriter extends StreamStarTableWriter {

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
    public void writeStarTable( StarTable startab, OutputStream out )
            throws IOException {
        DataOutputStream strm = new DataOutputStream( out );
        FitsTableSerializer serializer = new FitsTableSerializer( startab );
        writePrimary( startab, strm );
        serializer.writeHeader( strm );
        serializer.writeData( strm );
        strm.flush();
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
}
