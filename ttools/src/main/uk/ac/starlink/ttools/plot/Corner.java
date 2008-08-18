package uk.ac.starlink.ttools.plot;

/**
 * Describes the corners of a 3-dimensional cube.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Corner implements Comparable {

    private final int index_;

    private final static int NDIM = 3;
    private final static Corner[] CORNERS;
    static {
        CORNERS = new Corner[ 1 << NDIM ];
        for ( int i = 0; i < CORNERS.length; i++ ) {
            CORNERS[ i ] = new Corner( i );
        }
    }

    /** The origin. */
    public static final Corner ORIGIN = getCorner( 0 );

    /**
     * Private sole constructor.
     * Constructs a new corner with a given index.
     *
     * @param  index  corner index (0 <= index < 8).
     */
    private Corner( int index ) {
        index_ = index;
    }

    /**
     * Returns an array of the three corners which are adjacent to this one
     * (connected by a cube edge).
     *
     * @return   3-element corner array
     */
    public Corner[] getAdjacent() {
        Corner[] adj = new Corner[ NDIM ];
        for ( int idim = 0; idim < NDIM; idim++ ) {
            boolean[] flags = getFlags();
            flags[ idim ] = ! flags[ idim ];
            adj[ idim ] = getCorner( getIndex( flags ) );
        }
        return adj;
    }

    /**
     * Returns an array of boolean flags; the <code>i</code>'th flag 
     * indicates whether the <code>i</code>'th coordinate is low or high
     * (zero or one for a unit cube).
     *
     * @return   three element array of booleans describing coordinates of
     *           this corner
     */
    public boolean[] getFlags() {
        boolean[] flags = new boolean[ NDIM ];
        for ( int idim = 0; idim < NDIM; idim++ ) {
            flags[ idim ] = ( index_ & ( 1 << idim ) ) > 0;
        }
        return flags;
    }

    /**
     * Factory method giving one of the cube corners.
     * The <code>index</code> determines which corner you get.
     *
     * @param   index  corner ID; 0 <= index < 8
     */
    public static Corner getCorner( int index ) {
        return CORNERS[ index ];
    }

    /**
     * Returns the index corresponding to a given triple of coordinate flags.
     *
     * @param  flags   three-element array of low/high coordinate value flags
     * @return   corner index; 0 <= index < 8
     */
    private static int getIndex( boolean[] flags ) {
        int index = 0;
        for ( int idim = 0; idim < NDIM; idim++ ) {
            index += ( flags[ idim ] ? 1 : 0 ) << idim;
        }
        return index;
    }

    public boolean equals( Object other ) {
        return other instanceof Corner && ((Corner) other).index_ == index_;
    }

    public int hashCode() {
        return index_;
    }

    public String toString() {
        boolean[] flags = getFlags();
        return index_ + ": " + ( flags[ 0 ] ? 1 : 0 )
                             + ( flags[ 1 ] ? 1 : 0 )
                             + ( flags[ 2 ] ? 1 : 0 );
    }

    /**
     * Defines some arbitrary but consistent ordering of corners.
     */
    public int compareTo( Object other ) {
        int oIndex = ((Corner) other).index_;
        if ( index_ < oIndex ) {
            return -1;
        }
        else if ( index_ > oIndex ) {
            return +1;
        }
        else {
            return 0;
        }
    }
}
