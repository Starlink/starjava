package uk.ac.starlink.table.join;

/**
 * Enumeration defining how each table in a multi-table join can determines
 * the acceptability of a match.  Acceptability can be used to decide which
 * matches form part of the output table resulting from a match.
 *
 * @author   Mark Taylor
 * @since    4 Dec 2007
 */
public class MultiJoinType {

    private final String name_;

    /** Table must be present in an acceptable match. */
    public static final MultiJoinType MATCH = new MultiJoinType( "match" );

    /** Table must be absent in an acceptable match. */
    public static final MultiJoinType NOMATCH = new MultiJoinType( "nomatch" );

    /** Any match containing table (even alone) is acceptable. 
     *  Overrides MATCH and NOMATCH. */
    public static final MultiJoinType ALWAYS = new MultiJoinType( "always" );

    /** No constraints on match inclusion for table. */
    public static final MultiJoinType DEFAULT = new MultiJoinType( "default" );

    /**
     * Private sole constructor.
     *
     * @param  name  type name
     */
    private MultiJoinType( String name ) {
        name_ = name;
    }

    /**
     * Returns join type name.
     *
     * @return  name
     */
    public String toString() {
        return name_;
    }

    /**
     * Determines acceptability of a sequence of items based on a sequence
     * of acceptability criteria.
     *
     * @param  joinTypes  array of acceptability criteria
     * @param  present   array of flags for presence/absence of items
     * @return  true iff acceptability criteria are fulfilled
     */
    public static boolean accept( MultiJoinType[] joinTypes,
                                  boolean[] present ) {
        int n = joinTypes.length;
        if ( present.length != n ) {
            throw new IllegalArgumentException( "Count mismatch" );
        }
        for ( int i = 0; i < n; i++ ) {
            if ( joinTypes[ i ] == null ) {
                throw new NullPointerException();
            }
            if ( joinTypes[ i ] == ALWAYS && present[ i ] ) {
                return true;
            }
        }
        for ( int i = 0; i < n; i++ ) {
            if ( joinTypes[ i ] == MATCH && ! present[ i ] ) {
                return false;
            }
            if ( joinTypes[ i ] == NOMATCH && present[ i ] ) {
                return false;
            }
        }
        return true;
    }
}
