package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.table.StreamStarTableWriter;
import uk.ac.starlink.table.StarTable;

/**
 * Handles writing of a <code>StarTable</code> in a column-oriented 
 * FITS binary table format.
 * The table data is stored in a BINTABLE extension which has a single row;
 * each cell in this row contains the data for an entire column of the
 * represented table.
 *
 * <p>This rather specialised format may provide good performance for
 * certain operations on very large, especially very wide, tables.
 * Although it is FITS and can therefore be used in principle for data
 * interchange, in practice most non-STIL processors are unlikely to
 * be able to do much useful with it.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class ColFitsTableWriter extends StreamStarTableWriter {

    public String getFormatName() {
        return "colfits";
    }

    public String getMimeType() {
        return "application/fits";
    }

    public boolean looksLikeFile( String location ) {
        return location.endsWith( ".colfits" );
    }

    public void writeStarTable( StarTable table, OutputStream ostrm )
            throws IOException {
        DataOutputStream out = new DataOutputStream( ostrm );
        ColFitsTableSerializer serializer = new ColFitsTableSerializer( table );
        writePrimary( table, serializer, out );
        serializer.writeHeader( out );
        serializer.writeData( out );
        out.flush();
    }

    protected void writePrimary( StarTable table, ColFitsTableSerializer fitser,
                                 DataOutput out )
            throws IOException {
        FitsConstants.writeEmptyPrimary( out );
    }
}
