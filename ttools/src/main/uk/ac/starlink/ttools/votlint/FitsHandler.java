package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;

/**
 * Element handler for FITS elements.
 * The main job this does is to read the FITS file and check that the
 * columns in it match the columns declared in the VOTable FIELD elements.
 * The messages it issues are warnings rather than errors, since the
 * VOTable standard explicitly says that parsers can treat inconsistencies
 * between FITS and VOTable metadata however they like.
 *
 * @author   Mark Taylor (Starlink)
 * @since    12 Apr 2005
 */
public class FitsHandler extends StreamingHandler
                         implements TableSink {

    public void feed( InputStream in ) throws IOException {
        VotLintCode extnumCode = new VotLintCode( "EXT" );

        /* Stream the data from the stream to a TableSink - this object. */
        String extnum = getAttribute( "extnum" );
        if ( extnum != null && extnum.trim().length() > 0 ) {
            try {
                int en = Integer.parseInt( extnum );
                if ( en <= 0 ) {
                    error( extnumCode,
                           "Non-positive extension number extnum=" + en );
                    extnum = null;
                }
            }
            catch ( NumberFormatException e ) {
                error( extnumCode,
                       "Bad extension number extnum='" + extnum + "'" );
                extnum = null;
            }
        }
        new FitsTableBuilder().streamStarTable( in, this, extnum );

        /* Dispose of the rest of the stream. */
        byte[] buffer = new byte[ 16 * 1024 ];
        while ( in.read( buffer ) > 0 ) {}
    }

    /**
     * Checks that a ValueParser (derived from FIELD element) is 
     * consistent with a ColumnInfo (derived from the FITS stream via
     * a FITS StarTable).
     *
     * @param   vParser  VOTable parsers
     * @param   column   FITS column info
     */
    private void checkConsistent( FieldHandler vField, ColumnInfo cinfo ) {
        ValueParser vParser = vField.getParser();

        /* Check that the classes you'd get from both match.  This is an
         * indirect check, and relies on the coding in ValueParser being
         * compatible with that in the equivalent VOTABLE package classes
         * (Decoder). */
        Class<?> vClass = vParser.getContentClass();
        Class<?> fClass = cinfo.getContentClass();
        if ( ! vClass.equals( fClass ) ) {
            warning( new VotLintCode( "FVM" ),
                     "VOTable/FITS type mismatch for column " + 
                     vField.getRef() + " (" +
                     vClass.getName() + " != " + fClass.getName() + ")" );
        }

        /* Check that the number of elements match.  If either is 
         * indeterminate, call it consistent. */
        int vSize = vParser.getElementCount();
        int fSize;
        if ( cinfo.isArray() ) {
            fSize = 1;
            int[] shape = cinfo.getShape();
            for ( int i = 0; fSize > 0 && i < shape.length; i++ ) {
                fSize *= shape[ i ];
            }
        }
        else {
            fSize = 1;
        }
        if ( vSize > 0 && fSize > 0 && vSize != fSize ) {
            warning( new VotLintCode( "FVM" ),
                     "VOTable/FITS array size mismatch for column " + 
                     vField.getRef() + " (" + vSize + " != " + fSize + ")" );
        }
    }

    /*
     * TableSink implementation.
     */

    public void acceptMetadata( StarTable meta ) {

        /* This is where we get notified about the ColumnInfos that 
         * decoding the FITS stream has come up with. */
        /* First check there are the same number of columns in the FITS
         * table as in the declared table. */
        int ncol = meta.getColumnCount();
        FieldHandler[] fields = getFields();
        if ( ncol != fields.length ) {
            warning( new VotLintCode( "FVI" ),
                     "FITS table has " + ncol + " columns and VOTable has " +
                     fields.length + " - no type checking done" );
        }

        /* Then check that each column looks consistent between the two
         * views. */
        else {
            for ( int icol = 0; icol < ncol; icol++ ) {
                checkConsistent( fields[ icol ], meta.getColumnInfo( icol ) );
            }
        }
    }

    public void acceptRow( Object[] row ) {

        /* Inform the table we've got another row, so it can keep track. */
        foundRow();
    }

    public void endRows() {
    }
}
