package uk.ac.starlink.fits;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.MetaCopyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableFormatException;

/**
 * TableWriter implementation that writes FITS files using the HEALPix-FITS
 * convention.  This convention is somewhat standard for encoding
 * HEALPix maps in FITS files.
 *
 * <p>It is not always necessary to use this output handler,
 * since the normal FitsTableWriter also inserts the relevant
 * HEALPix-FITS headers if it encounters a table that looks like
 * a HEALPix map.  However, this implementation differs in a couple of ways:
 * if it is presented with a table that does not look like a HEALPix map,
 * it will throw a TableFormatException rather than just performing
 * non-HEALPix output, and it will rearrange the columns so that the
 * healpix index is in the first column and named "PIXEL" if required.
 *
 * @author   Mark Taylor
 * @since    6 Dec 2018
 * @see
 * <a href="https://healpix.sourceforge.io/data/examples/healpix_fits_specs.pdf"
 *    >HEALPix-FITS convention</a>
 */
public class HealpixFitsTableWriter extends AbstractFitsTableWriter {

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public HealpixFitsTableWriter() {
        super( "fits-healpix" );
        setAllowSignedByte( false );
        setWide( null );
    }

    /**
     * Returns false.
     */
    public boolean looksLikeFile( String location ) {
        return false;
    }

    /**
     * Returns a StandardFitsTableSerializer only if the table looks like
     * a HEALpix map.  The serializer may rearrange the columns so that
     * the pixel index column comes first.
     * If the table doesn't look like HEALpix, a TableFormatException
     * will be thrown.
     */
    protected FitsTableSerializer createSerializer( StarTable table )
            throws TableFormatException, IOException {

        /* Check that this is annotated as a HEALPix table. */
        List<DescribedValue> tparams = table.getParameters();
        if ( ! HealpixTableInfo.isHealpix( tparams ) ) {
            throw new TableFormatException( "Table is not annotated"
                                          + " as HEALPix" );
        }
        HealpixTableInfo hpxInfo = HealpixTableInfo.fromParams( tparams );

        /* Find out if there is an explicit HEALPix index column,
         * and if so, ensure it is the first column, and named PIXEL. */
        boolean isExplicit;
        String ipixColName = hpxInfo.getPixelColumnName();
        if ( ipixColName != null ) {
            int icIpix = getColumnIndex( table, ipixColName );
            if ( icIpix < 0 ) {
                throw new TableFormatException( "No column \"" + ipixColName
                                              + "\" in table" );
            }
            if ( icIpix > 0 ) {
                int ncol = table.getColumnCount();
                int[] colmap = new int[ ncol ];
                int jc = 0;
                colmap[ jc++ ] = icIpix;
                for ( int ic = 0; ic < ncol; ic++ ) {
                    if ( ic != icIpix ) {
                        colmap[ jc++ ] = ic;
                    }
                }
                assert jc == ncol;
                table = new ColumnPermutedStarTable( table, colmap, true );
            }
            if ( ! "PIXEL".equals( table.getColumnInfo( 0 ).getName() ) ) {
                table = new MetaCopyStarTable( table );
                ColumnInfo info0 = table.getColumnInfo( 0 );
                info0.setName( "PIXEL" );
                info0.setDescription( "HEALPix pixel index" );
                table.setParameter( new DescribedValue( HealpixTableInfo
                                                       .HPX_COLNAME_INFO,
                                                        info0.getName() ) );
                hpxInfo = HealpixTableInfo.fromParams( table.getParameters() );
            }
            isExplicit = true;
        }
        else {
            isExplicit = false;
        }

        /* Create a serializer based on the, possibly reorganised, table. */
        StandardFitsTableSerializer fitser =
            new StandardFitsTableSerializer( getConfig(), table );

        /* Do a dummy run of constructing the Healpix headers.
         * This call will be made later during serializer invocation,
         * but on that occasion exceptions will be logged and swallowed.
         * Here, the exceptions are uncaught so that tables that are
         * not HEALPix will cause a write error. */
        fitser.getHealpixHeaders( hpxInfo );

        /* If we've got this far, the serializer will be able to write
         * a HEALPix FITS file. */
        return fitser;
    }

    /**
     * Returns the column index of a column with a given name.
     *
     * @param  table  table
     * @param  cname  column name
     * @return  index of the first column with a name matchin cname,
     *          or -1 if there is none
     */
    private static final int getColumnIndex( StarTable table, String cname ) {
        int ncol = table.getColumnCount();
        for ( int ic = 0; ic < ncol; ic++ ) {
            if ( cname.equals( table.getColumnInfo( ic ).getName() ) ) {
                return ic;
            }
        }
        return -1;
    }
}
