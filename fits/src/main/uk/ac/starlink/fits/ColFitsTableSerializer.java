package uk.ac.starlink.fits;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Object which can serialize a StarTable to a data stream as a one-row FITS
 * file in which each element contains an entire column of the table.
 *
 * @author   Mark Taylor
 * @since    26 Jun 2006
 */
public class ColFitsTableSerializer implements FitsTableSerializer {

    private final FitsTableSerializerConfig config_;
    private final ColumnStore[] colStores_;
    private final String[] colids_;
    private final int ncol_;
    private final long nrow_;
    private final String tname_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  config  configuration
     * @param  table  table to serialize
     * @throws IOException if it won't be possible to write the given table
     */
    public ColFitsTableSerializer( FitsTableSerializerConfig config,
                                   StarTable table )
            throws IOException {
        config_ = config;

        /* Prepare an array of column storage objects which know how to do
         * serial storage/retrieval of the data in a table column. */
        tname_ = table.getName();
        ncol_ = table.getColumnCount();
        colStores_ = new ColumnStore[ ncol_ ];
        colids_ = new String[ ncol_ ];
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            colids_[ icol ] = info.toString();
            colStores_[ icol ] =
                FileColumnStore.createColumnStore( info, config );
            if ( colStores_[ icol ] == null ) {
                logger_.warning( "Can't serialize column " + info );
            }
            else {
                nUseCol++;
            }
        }
        FitsUtil.checkColumnCount( config.getWide(), nUseCol );

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

    public CardImage[] getHeader() {
        WideFits wide = config_.getWide();

        /* Work out the length of the single table row. */
        long size = 0L; 
        long extSize = 0L;
        int nUseCol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnStore colStore = colStores_[ icol ];
            if ( colStore != null ) {
                nUseCol++;
                long leng = colStore.getDataLength();
                size += leng;
                if ( wide != null &&
                     nUseCol >= wide.getContainerColumnIndex() ) {
                    extSize += leng;
                }
            }
        }

        /* Work out the number of standard and extended columns.
         * This won't fail because of checks carried out in constructor. */
        int nStdCol =
              wide != null && nUseCol > wide.getContainerColumnIndex()
            ? wide.getContainerColumnIndex()
            : nUseCol;
        boolean hasExtCol = nUseCol > nStdCol;

        /* Write the standard part of the FITS header. */
        List<CardImage> cards = new ArrayList<>();
        CardFactory cfact = CardFactory.DEFAULT;
        cards.addAll( Arrays.asList( new CardImage[] {
         
            cfact.createStringCard( "XTENSION", "BINTABLE",
                                    "binary table extension" ),
            cfact.createIntegerCard( "BITPIX", 8, "8-bit bytes" ),
            cfact.createIntegerCard( "NAXIS", 2, "2-dimensional table" ),
            cfact.createIntegerCard( "NAXIS1", size,
                                     "width of single row in bytes" ),
            cfact.createIntegerCard( "NAXIS2", 1, "single-row table" ),
            cfact.createIntegerCard( "PCOUNT", 0, "size of special data area" ),
            cfact.createIntegerCard( "GCOUNT", 1, "one data group" ),
            cfact.createIntegerCard( "TFIELDS", nStdCol, "number of columns" ),
        } ) );

        /* Add EXTNAME record containing table name. */
        if ( tname_ != null && tname_.trim().length() > 0 ) {
            cards.add( cfact.createStringCard( "EXTNAME", tname_,
                                               "table name" ) );
        }

        /* Add extended column header information if applicable. */
        if ( hasExtCol ) {
            cards.addAll( Arrays.asList( wide.getExtensionCards( nUseCol ) ) );
            AbstractWideFits.logWideWrite( logger_, nStdCol, nUseCol ); 
        }

        /* Ask each ColumnStore to add header cards describing the data
         * cell it will write. */
        int jcol = 0;
        for ( int icol = 0; icol < ncol_; icol++ ) {
            ColumnStore colStore = colStores_[ icol ];
            if ( colStore != null ) {
                jcol++;
                if ( hasExtCol && jcol == nStdCol ) {
                    cards.addAll( Arrays.asList(
                        wide.getContainerColumnCards( extSize, nrow_ ) ) );
                }
                BintableColumnHeader colhead =
                      hasExtCol && jcol >= nStdCol
                    ? wide.createExtendedHeader( nStdCol, jcol )
                    : BintableColumnHeader.createStandardHeader( jcol );
                cards.addAll( colStore.getHeaderInfo( colhead, jcol ) );
            }
        }
        return cards.toArray( new CardImage[ 0 ] );
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
                logger_.info( "Writing column " + ( icol + 1 ) + "/" + ncol_ 
                            + ": " + colids_[ icol ] );
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

    public long getRowCount() {
        return nrow_;
    }

    public char getFormatChar( int icol ) {
        ColumnStore colStore = colStores_[ icol ];
        if ( colStore != null ) {
            String tform = getCardValue( colStore, "TFORM" ).trim();
            return tform.charAt( tform.length() - 1 );
        }
        else {
            return (char) 0;
        }
    }

    public int[] getDimensions( int icol ) {
        ColumnStore colStore = colStores_[ icol ];
        if ( colStore != null ) {
            long[] tdims =
                ColFitsStarTable.parseTdim( getCardValue( colStore, "TDIM" ) );
            int[] dims = new int[ tdims.length - 1 ];
            for ( int i = 0; i < tdims.length - 1; i++ ) {
                dims[ i ] = Tables.checkedLongToInt( tdims[ i ] );
            }
            return dims;
        }
        else {
            return null;
        }
    }

    public String getBadValue( int icol ) {
        ColumnStore colStore = colStores_[ icol ];
        if ( colStore != null ) {
            String tnull = getCardValue( colStore, "TNULL" );
            if ( tnull != null && tnull.trim().length() > 0 ) {
                return tnull;
            }
        }
        return null;
    }

    private static String getCardValue( ColumnStore colStore, String tcard ) {
        int icol = 99;
        BintableColumnHeader colhead =
            BintableColumnHeader.createStandardHeader( icol );
        String key = colhead.getKeyName( tcard );
        for ( CardImage card : colStore.getHeaderInfo( colhead, icol ) ) {
            ParsedCard<?> pcard = FitsUtil.parseCard( card.getBytes() );
            if ( pcard != null && key.equals( pcard.getKey() ) ) {
                return String.valueOf( pcard.getValue() );
            }
        }
        return null;
    }
}
