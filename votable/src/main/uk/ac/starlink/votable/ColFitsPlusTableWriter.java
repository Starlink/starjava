package uk.ac.starlink.votable;

import java.io.IOException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.fits.ColFitsTableSerializer;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.table.StarTable;

/**
 * Handles writing of a <code>StarTable</code> in a column-oriented
 * FITS binary table format.
 * The table data is stored in a BINTABLE extension which has a single row;
 * each cell in this row contains the data for an entire column of the
 * represented table.  The primary HDU is a byte array containing a
 * VOTable representation of the table metadata, as for 
 * {@link FitsPlusTableWriter}.
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
public class ColFitsPlusTableWriter extends VOTableFitsTableWriter {

    public ColFitsPlusTableWriter() {
        super( "colfits-plus" );
    }

    public boolean looksLikeFile( String location ) {
        return location.endsWith( ".colfits" );
    }

    protected void customisePrimaryHeader( Header hdr )
            throws HeaderCardException {
        hdr.addValue( "COLFITS", true,
                      "Table extension stored column-oriented" );
        hdr.addValue( "VOTMETA", true, "Table metadata in VOTable format" );
    }

    protected boolean isMagic( int icard, String key, String value ) {
        switch ( icard ) {
            case 4:
                return "COLFITS".equals( key ) && "T".equals( value );
            case 5:
                return "VOTMETA".equals( key );
            default:
                return super.isMagic( icard, key, value );
        }
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        return new ColFitsTableSerializer( table );
    }
}
