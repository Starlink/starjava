package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * QuerySequenceFactory that presorts rows according to HEALPix pixel index.
 *
 * @author   Mark Taylor
 * @since    3 Jul 2014
 */
public class HealpixSortedQuerySequenceFactory implements QuerySequenceFactory {

    private final QuerySequenceFactory baseFact_;
    private final PixtoolsHealpix hpix_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /** The largest order for which Healpix IDs can fit into an int is 13(?). */
    private static final int ORDER = 12;

    /**
     * Constructor.
     *
     * @param   baseFact  query sequence factory on which this one is based
     */
    public HealpixSortedQuerySequenceFactory( QuerySequenceFactory baseFact ) {
        baseFact_ = baseFact;
        hpix_ = PixtoolsHealpix.getInstance();
    }

    public ConeQueryRowSequence createQuerySequence( StarTable table )
            throws IOException {
        if ( ! table.isRandom() ) {
            throw new IllegalArgumentException( "Non-random-access table" );
        }
        logger_.info( "Pre-sorting rows by HEALPix index, order " + ORDER );

        /* First store the HEALPix pixel index for each row alongside
         * a record of the row index in the underlying table to which it
         * corresponds.  We pack these into a single long (32 bits for each).
         * 32 bits each is enough: for the HEALPix index because we know
         * the order, and for the table because we bail out if it has more
         * than 2^31 rows. */
        int nrow = Tables.checkedLongToInt( table.getRowCount() );
        long[] codes = new long[ nrow ];
        ConeQueryRowSequence preSeq = baseFact_.createQuerySequence( table );
        int irow = 0;
        while ( preSeq.next() ) {
            double ra = preSeq.getRa();
            double dec = preSeq.getDec();
            long lndex = preSeq.getIndex();
            int hpixIndex = Tables
                           .assertLongToInt( hpix_.ang2pix( ORDER, ra, dec ) );
            int rowIndex = Tables.assertLongToInt( lndex );
            codes[ irow ] = packHpixRow( hpixIndex, rowIndex );
            irow++;
        }
        preSeq.close();

        /* Sort them.  Since the most significant 32 bits is composed of the
         * HEALPix index, they come out in the right order.  The other end
         * of the long is unimportant. */
        logger_.config( "Sorting " + irow + " HEALPix indices" );
        Arrays.sort( codes, 0, irow );

        /* Turn the array of packed longs into an array of row indices,
         * by discarding the HEALPix index part, no longer needed. */
        final long[] rowMap = new long[ irow ];
        for ( int ir = 0; ir < irow; ir++ ) {
            rowMap[ ir ] = unpackRowIndex( codes[ ir ] );
        }

        /* Use this row index mapping array to provide a query sequence
         * with an adjusted row ordering. */
        StarTable sortedTable = new RowPermutedStarTable( table, rowMap );
        return new WrapperQuerySequence( baseFact_
                                        .createQuerySequence( sortedTable ) ) {
            @Override
            public long getIndex() throws IOException {
                return rowMap[ Tables.assertLongToInt( super.getIndex() ) ];
            }
        };
    }

    /**
     * Packs two integers into a long.  The healpix part goes in the
     * most significant bits.
     *
     * @param  healpixIndex   healpix pixel identifier
     * @param  rowIndex    index in the underlying table of the row
     */
    private static long packHpixRow( int healpixIndex, int rowIndex ) {
        return ( (long) healpixIndex << 32 ) | (long) rowIndex;
    }

    /**
     * Returns the row index from a packed long.
     *
     * @return  value previously packed as <code>rowIndex</code>
     */
    private static int unpackRowIndex( long packed ) {
        return (int) packed;
    }
}
