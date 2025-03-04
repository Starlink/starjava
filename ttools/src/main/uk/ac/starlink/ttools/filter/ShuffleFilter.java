package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Filter that permutes rows randomly.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2025
 */
public class ShuffleFilter extends BasicFilter {

    public ShuffleFilter() {
        super( "shuffle", "[-seed <int>]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Randomly permutes the rows of the input table.",
            "The random seed can optionally be specified.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        Random rnd = null;
        while ( argIt.hasNext() ) {
            String arg = argIt.next();
            if ( arg.equals( "-seed" ) && argIt.hasNext() && rnd == null ) {
                argIt.remove();
                String seedTxt = argIt.next();
                argIt.remove();
                long lseed;
                try {
                    lseed = Long.parseLong( seedTxt );
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( "Seed \"" + seedTxt
                                          + "\" not numeric" );
                }
                rnd = new Random( lseed );
            }
        }
        if ( rnd == null ) {
            rnd = new Random();
        }
        return new ShuffleStep( rnd );
    }

    /**
     * ProcessingStep implementation for shuffle.
     */
    private static class ShuffleStep implements ProcessingStep {
        private final Random rnd_;

        /**
         * Constructor.
         *
         * @param  rnd  source of randomness
         */
        ShuffleStep( Random rnd ) {
            rnd_ = rnd;
        }

        public StarTable wrap( StarTable table ) throws IOException {
            table = Tables.randomTable( table );
            long lnrow = table.getRowCount();
            if ( lnrow > Integer.MAX_VALUE ) {
                throw new IOException(
                    "Sorry, can't shuffle tables with >2^31 rows" );
            }
            int nrow = (int) lnrow;
            long[] rowMap = new long[ nrow ];
            for ( int i = 0; i < nrow; i++ ) {
                rowMap[ i ] = i;
            }

            /* This uses the random permutation algorithm described in
             * the javadocs for the java.util.Collections shuffle method. */
            for ( int i = nrow - 1; i > 0; i-- ) {
                int iswap = rnd_.nextInt( i );
                long t = rowMap[ iswap ];
                rowMap[ iswap ] = rowMap[ i ];
                rowMap[ i ] = t;
            }
            return new RowPermutedStarTable( table, rowMap );
        }
    }
}
