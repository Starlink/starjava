package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Object which can serialize a StarTable to a data stream as a one-row FITS
 * file in which each element contains an entire column of the table.
 *
 * @author   Mark Taylor
 * @since    26 Jun 2006
 */
public class ColFitsTableSerializer {

    private final ColumnStore[] colStores_;
    private final int ncol_;
    private final long nrow_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param   table  table to serialize
     */
    public ColFitsTableSerializer( StarTable table ) throws IOException {

        /* Prepare an array of column storage objects which know how to do
         * serial storage/retrieval of the data in a table column. */
        ncol_ = table.getColumnCount();
        colStores_ = new ColumnStore[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            colStores_[ icol ] = FileColumnStore.createColumnStore( info );
            if ( colStores_[ icol ] == null ) {
                logger_.warning( "Can't serialize column " + info );
            }
        }

        /* Store the table data into these storage objects. */
        boolean ok = false;
        RowSequence rseq = table.getRowSequence();
        try {
            long lrow = 0L;
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    ColumnStore colStore = colStores_[ icol ];
                    if ( colStore != null ) {
                        colStore.storeValue( row[ icol ] ); 
                    }
                }
                lrow++;
            }
            nrow_ = lrow;
            ok = true;
        }
        finally {
            rseq.close();

            /* In case of error, tidy up now, since these objects may be
             * expensive. */
            if ( ! ok ) {
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    if ( colStores_[ icol ] != null ) {
                        colStores_[ icol ].dispose();
                    }
                }
            }
        }

        /* Declare that the storage has been completed, thereby readying the
         * storage objects for data retrieval. */
        assert ok;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            if ( colStores_[ icol ] != null ) {
                if ( colStores_[ icol ] != null ) {
                    colStores_[ icol ].endStores();
                }
            }
        }
    }

    /**
     * Write a FITS header appropriate for the data in this serializer.
     *
     * @param   out   destination stream
     */
    public void writeHeader( DataOutput out ) throws IOException {

        /* Work out the length of the single table row. */
        long size = 0L; 
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnStore colStore = colStores_[ icol ];
            if ( colStore != null ) {
                nUseCol++;
                size += colStore.getDataLength();
            }
        }

        /* Write the standard part of the FITS header. */
        Header hdr = new Header();
        try {
            hdr.addValue( "XTENSION", "BINTABLE", "binary table extension" );
            hdr.addValue( "BITPIX", 8, "8-bit bytes" );
            hdr.addValue( "NAXIS", 2, "2-dimensional table" );
            hdr.addValue( "NAXIS1", size, "width of single row in bytes" );
            hdr.addValue( "NAXIS2", 1, "single-row table" );
            hdr.addValue( "PCOUNT", 0, "size of special data area" );
            hdr.addValue( "GCOUNT", 1, "one data group" );
            hdr.addValue( "TFIELDS", nUseCol, "number of columns" );

            /* Ask each ColumnStore to add header cards describing the data
             * cell it will write. */
            int jcol = 0;
            for ( int icol = 0; icol < ncol_; icol++ ) {
                ColumnStore colStore = colStores_[ icol ];
                if ( colStore != null ) {
                    colStore.addHeaderInfo( hdr, ++jcol );
                }
            }

            /* Output the header to the destination stream. */
            FitsConstants.writeHeader( out, hdr );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Write the FITS data unit populated by this serializer.
     *
     * @param  out  destination stream
     */
    public void writeData( DataOutput out ) throws IOException {

        /* Write the cell provided by each serializer in turn. */
        long size = 0L;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            if ( colStores_[ icol ] != null ) {
                ColumnStore colStore = colStores_[ icol ];
                colStore.streamData( out );
                size += colStore.getDataLength();
                colStore.dispose();
            }
        }

        /* Write padding if necessary. */
        int over = (int) ( size % 2880L );
        if ( over > 0 ) {
            out.write( new byte[ 2880 - over ] );
        }
    }
}
