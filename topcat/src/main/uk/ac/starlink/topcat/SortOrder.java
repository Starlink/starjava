package uk.ac.starlink.topcat;

import java.util.Arrays;

/**
 * Defines a sorting order for a table.
 * An instance of this class defines the ordering by which a sort is done,
 * rather than a given row sequence.
 *
 * <p>Note that the sense (up or down) of the sort is selected separately
 * than by this object.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Feb 2004
 */
public class SortOrder {

    private final String[] expressions_;

    /** SortOrder instance indicating the natural order of the data. */
    public static final SortOrder NONE = new SortOrder( new String[ 0 ] );

    /**
     * Constructs a new sort order based on a table column.
     * 
     * @param  expressions  list of JEL sort expressions,
     *                      most significant first
     */
    public SortOrder( String[] expressions ) {
        expressions_ = expressions;
    }

    /**
     * Gives the expressions on which this table is based.
     *
     * @return   array of sort JEL expressions, most significant first
     */
    public String[] getExpressions() {
        return expressions_;
    }

    @Override
    public String toString() {
        switch ( expressions_.length ) {
            case 0:
                return "";
            case 1:
                return expressions_[ 0 ];
            default:
                return Arrays.toString( expressions_ );
        }
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SortOrder ) {
            SortOrder other = (SortOrder) o;
            return Arrays.equals( this.expressions_, other.expressions_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode( expressions_ );
    }
}
