package uk.ac.starlink.topcat;

/**
 * RowSubset implementation which provides the complement of a base set.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Mar 2004
 */
public class InverseRowSubset implements RowSubset {
    private final RowSubset base;
    private static final String INVERT_PREFIX = "not_";

    public InverseRowSubset( RowSubset base ) {
        this.base = base;
    }

    public boolean isIncluded( long lrow ) {
        return ! base.isIncluded( lrow );
    }

    public String getName() {
        String baseName = base.getName();
        return baseName.startsWith( INVERT_PREFIX ) 
             ? baseName.substring( INVERT_PREFIX.length() )
             : INVERT_PREFIX + baseName;
    }

    public String toString() {
        return getName();
    }
}
