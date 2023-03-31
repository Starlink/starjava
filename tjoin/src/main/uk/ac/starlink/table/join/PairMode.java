package uk.ac.starlink.table.join;

import java.io.IOException;

/**
 * Enumeration used to determine which row links result from a pair
 * match operation.
 *
 * @see  RowMatcher#findPairMatches(PairMode)
 */
public enum PairMode {

    /**
     * All matches are returned.
     */
    ALL( true, "All matches" ) {
        LinkSet findPairMatches( RowMatcher rowMatcher )
                throws IOException, InterruptedException {
            return rowMatcher.findAllPairs( 0, 1 );
        }
    },

    /**
     * Only the best matches are returned, obtained symmetrically.
     * Each row from both input tables will appear in at most
     * one RowLink in the result.
     */
    BEST( false, "Best match, symmetric" ) {
        LinkSet findPairMatches( RowMatcher rowMatcher )
                throws IOException, InterruptedException {
            LinkSet lset = rowMatcher.findAllPairs( 0, 1 );
            return rowMatcher.eliminateMultipleRowEntries( lset );
        }
    },

    /**
     * For each row in table 1, only the best match in table 2 is returned.
     * Each row from table 1 will appear a maximum of once in the result,
     * but rows from table 2 may appear multiple times.
     */
    BEST1( true, "Best match for each Table 1 row" ) {
        LinkSet findPairMatches( RowMatcher rowMatcher )
                throws IOException, InterruptedException {
            return rowMatcher
                  .scanForPairs( 1, 0, Coverage.FULL.createTestFactory(),
                                 true );
        }
    },

    /**
     * For each row in table 2, only the best match in table 1 is returned.
     * Each row from table 2 will appear a maximum of once in the result,
     * but rows from table 1 may appear multiple times.
     */
    BEST2( true, "Best match for each Table 2 row" ) {
        LinkSet findPairMatches( RowMatcher rowMatcher )
                throws IOException, InterruptedException {
            return rowMatcher
                  .scanForPairs( 0, 1, Coverage.FULL.createTestFactory(),
                                 true );
        }
    };

    private final boolean mayProduceGroups_;
    private final String summary_;

    /**
     * Constructor.
     *
     * @param  mayProduceGroups  whether this mode can produce result row
     *                           groups
     * @param  summary        short summary of operation
     */
    PairMode( boolean mayProduceGroups, String summary ) {
        mayProduceGroups_ = mayProduceGroups;
        summary_ = summary;
    }

    /**
     * Indicates whether the result of a match performed in this mode
     * may contain non-trivial related groups of rows.
     * A group represents a match in which an object in one table
     * corresponds to more than object in the other table.
     *
     * @return  true iff this mode may result in ambiguous matches
     * @see   MatchStarTables#findGroups
     */
    public boolean mayProduceGroups() {
        return mayProduceGroups_;
    }

    /**
     * Returns a short summary of the matching policy.
     *
     * @return  short description string
     */
    public String getSummary() {
        return summary_;
    }

    /**
     * Executes the pair match on a given row matcher according to
     * this matching mode.
     *
     * @param   rowMatcher  object containing tables
     * @return   set of matched row pairs
     */
    abstract LinkSet findPairMatches( RowMatcher rowMatcher )
            throws IOException, InterruptedException;
}
