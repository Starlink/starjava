package uk.ac.starlink.topcat.join;

/**
 * Enumeration class defining requirements on table rows for constructing
 * a matched table.
 * <p>
 * I'm still not certain about the possible values of this enumeration,
 * in particular what it means to mix MATCHED, ALL and UNMATCHED together.
 * At time of writing, UNMATCHED isn't used anywhere for this reason.
 * Might come in handy some time though.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Mar 2004
 */
public class MatchOption {

    /** Option for including matched rows only. */
    public static final MatchOption MATCHED = 
        new MatchOption( "Matched Rows Only" );

    /** Option for including unmatched rows only. */
    public static final MatchOption UNMATCHED = 
        new MatchOption( "Unmatched Rows Only" );

    /** Option for including all rows, matched or unmatched. */
    public static final MatchOption ANY = 
        new MatchOption( "All Rows" );

    private final String description;

    private MatchOption( String description ) {
        this.description = description;
    }
    public String toString() {
        return description;
    }
}
