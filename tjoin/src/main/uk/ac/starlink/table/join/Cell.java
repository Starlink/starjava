package uk.ac.starlink.table.join;

/**
 * Represents a cell on a Cartesian grid.
 * This is just a wrapper for a <code>long[]</code> array,
 * with suitable equals and hashCode methods defined.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2011
 */
class Cell {

    private final long[] label_;

    /**
     * Constructor.
     *
     * @param   label  array determining content of this cell;
     *                 it is not cloned
     */
    public Cell( long[] label ) {
        label_ = label;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Cell ) {
            Cell other = (Cell) o;
            long[] otherLabel = other.label_;
            int ndim = label_.length;
            for ( int i = 0; i < ndim; i++ ) {
                if ( otherLabel[ i ] != label_[ i ] ) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 37;
        int ndim = label_.length;
        for ( int i = 0; i < ndim; i++ ) {
            code = 23 * code + (int) label_[ i ];
        }
        return code;
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder( "(" );
        for ( int i = 0; i < label_.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ',' );
            }
            sbuf.append( label_[ i ] );
        }
        sbuf.append( ')' );
        return sbuf.toString();
    }
}
