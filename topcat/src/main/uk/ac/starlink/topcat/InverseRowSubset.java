package uk.ac.starlink.topcat;

/**
 * RowSubset implementation which provides the complement of a base set.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Mar 2004
 */
public class InverseRowSubset extends RowSubset {
    private final RowSubset base;
    private static final String INVERT_PREFIX = "not_";

    /**
     * Constructor.
     *
     * @param   base   subset to be inverted
     */
    public InverseRowSubset( RowSubset base ) {
        super( invertName( base.getName() ) );
        this.base = base;
    }

    public boolean isIncluded( long lrow ) {
        return ! base.isIncluded( lrow );
    }

    /**
     * Utility function to invert the name of a subset.
     *
     * @param   name  base name
     * @return  negation of <code>name</code>
     */
    private static String invertName( String name ) {
        return name.startsWith( INVERT_PREFIX )
             ? name.substring( INVERT_PREFIX.length() )
             : INVERT_PREFIX + name;
    }
}
