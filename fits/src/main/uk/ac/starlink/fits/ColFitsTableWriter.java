package uk.ac.starlink.fits;

import java.io.IOException;
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
public class ColFitsTableWriter extends AbstractFitsTableWriter {

    public ColFitsTableWriter() {
        super( "colfits-basic" );
    }

    public boolean looksLikeFile( String location ) {
        return location.endsWith( ".colfits" );
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        return new ColFitsTableSerializer( table );
    }
}
