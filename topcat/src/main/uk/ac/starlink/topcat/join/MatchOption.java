package uk.ac.starlink.topcat.join;

import uk.ac.starlink.table.join.MultiJoinType;

/**
 * Enumeration class defining requirements on table rows for constructing
 * a matched table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Mar 2004
 */
public class MatchOption {

    private final String description_;
    private final MultiJoinType joinType_;

    /** Option for including matched rows only. */
    public static final MatchOption MATCHED = 
        new MatchOption( "Matched Rows Only", MultiJoinType.MATCH );

    /** Option for including unmatched rows only. */
    public static final MatchOption UNMATCHED = 
        new MatchOption( "Unmatched Rows Only", MultiJoinType.NOMATCH );

    /** Option for including all rows, matched or unmatched. */
    public static final MatchOption ANY = 
        new MatchOption( "All Rows", MultiJoinType.ALWAYS );

    /** Option for default behaviour (all 2+-way matches are included). */
    public static final MatchOption DEFAULT =
        new MatchOption( "Default", MultiJoinType.DEFAULT );

    /**
     * Private sole constructor.
     *
     * @param   description, used for labelling components
     * @param   joinType   corresponding join type
     */
    private MatchOption( String description, MultiJoinType joinType ) {
        description_ = description;
        joinType_ = joinType;
    }

    /**
     * Returns the join type.
     *
     * @return   join type
     */
    public MultiJoinType getJoinType() {
        return joinType_;
    }

    /**
     * Returns the human-readable description.
     *
     * @return  description
     */
    public String toString() {
        return description_;
    }
}
