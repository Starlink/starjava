package uk.ac.starlink.ttools.cone;

/**
 * Enumeration of ways to submit an upload match to a service.
 *
 * @author   Mark Taylor
 * @since    17 May 2014
 */
public enum ServiceFindMode {

    /** All matches. */
    ALL( false, false, false, false ),

    /** Best remote match only for each input row. */
    BEST( true, false, false, true ),

    /** Best input match only for each remote row. */
    BEST_REMOTE( true, false, true, false ),

    /** All matches, score column only. */
    ALL_SCORE( false, true, false, false ),

    /** Best remote match only for each input row, score column only. */
    BEST_SCORE( true, true, false, true );

    private final boolean bestOnly_;
    private final boolean scoreOnly_;
    private final boolean remoteUnique_;
    private final boolean supportsOneToOne_;

    /**
     * Constructor.
     *
     * @param  bestOnly  true for best match only, false for all matches
     * @param  scoreOnly  only the score column is required, other columns
     *                    not included in the match result
     * @param  remoteUnique  whether match type requires each remote row to
     *                       appear at most once in the result
     * @param  supportsOneToOne  true iff 1:1 row usage for input/output
     *                           table is supported
     */
    ServiceFindMode( boolean bestOnly, boolean scoreOnly,
                     boolean remoteUnique, boolean supportsOneToOne ) {
        bestOnly_ = bestOnly;
        scoreOnly_ = scoreOnly;
        remoteUnique_ = remoteUnique;
        supportsOneToOne_ = supportsOneToOne;
    }

    /**
     * Indicates whether only the rows with the best match to an input row
     * are returned, or all matches.
     *
     * @return  true for best match only, false for all matches
     */
    public boolean isBestOnly() {
        return bestOnly_;
    }

    /**
     * Indicates whether the output columns will contain just the score,
     * or (at least some) columns from the remote table.
     *
     * @return  true  iff only the match score column is returned
     */
    public boolean isScoreOnly() {
        return scoreOnly_;
    }

    /**
     * Indicates whether the nature of this match requires that each
     * row from the remote table may appear at most once in the result.
     *
     * <p>If performing the upload match in blocks, a true result from
     * this method may mean that some post-processing of the result
     * needs to be done.
     *
     * @return  true iff remote rows must appear &lt;=1 time in result
     */
    public boolean isRemoteUnique() {
        return remoteUnique_;
    }

    /**
     * Indicates whether this mode will allow use in a context where there
     * is exactly one output row for each input row.
     *
     * @return   true iff 1:1 input/output row usage is supported
     */
    public boolean supportsOneToOne() {
        return supportsOneToOne_;
    }
}
