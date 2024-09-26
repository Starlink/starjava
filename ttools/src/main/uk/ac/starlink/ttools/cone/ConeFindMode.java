package uk.ac.starlink.ttools.cone;

import uk.ac.starlink.table.join.PairMode;

/**
 * Find mode for SkyConeMatch2 results.
 *
 * @author   Mark Taylor
 * @since    26 Sep 2024
 */
public enum ConeFindMode {

    /** Best match. */
    BEST( "best", true, false, String.join( "\n",
        "Only the matching query table row closest to",
        "the input table row will be output.",
        "Input table rows with no matches will be omitted.",
        "(Note this corresponds to the",
        "<code>" + PairMode.BEST1.toString().toLowerCase() + "</code>",
        "option in the pair matching commands, and <code>best1</code>",
        "is a permitted alias).",
        ""
    ) ),

    /** All matches. */
    ALL( "all", false, false, String.join( "\n",
        "All query table rows which match the input table row will be output.",
        "Input table rows with no matches will be omitted.",
        ""
    ) ),

    /** One output row for each input row. */
    EACH( "each", true, true, String.join( "\n",
        "There will be one output table row for each input table row.",
        "If matches are found, the closest one from the query table",
        "will be output, and in the case of no matches,",
        "the query table columns will be blank.",
        ""
    ) );

    private final String name_;
    private final boolean bestOnly_;
    private final boolean includeBlanks_;
    private final String xmlDescription_;

    /**
     * Constructor.
     *
     * @param  name  human-readable mode name
     * @param  bestOnly  true iff only the best result for each query
     *                   is required
     * @param  includeBlanks  true iff output should include input rows
     *                        even in the case of no matches
     * @param  xmlDescription  description in XML, no wrapping element
     */
    ConeFindMode( String name, boolean bestOnly, boolean includeBlanks,
                  String xmlDescription ) {
        name_ = name;
        bestOnly_ = bestOnly;
        includeBlanks_ = includeBlanks;
        xmlDescription_ = xmlDescription;
    }

    /**
     * Indicates if only best matches per query are required.
     *
     * @return  true iff only the best match for each input table row
     *          is required, false for all matches within radius
     */
    public boolean isBestOnly() {
        return bestOnly_;
    }

    /**
     * Indicates if output should include input rows even when no match
     * was found.
     *
     * @return  true iff a row is to be output for input rows
     *          for which the cone search has no matches
     */
    public boolean isIncludeBlanks() {
        return includeBlanks_;
    }

    /**
     * Returns XML description of behaviour.
     *
     * @return  XML description, no wrapping element
     */
    public String getXmlDescription() {
        return xmlDescription_;
    }

    @Override
    public String toString() {
        return name_;
    }
}
