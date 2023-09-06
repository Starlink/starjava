package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Encapsulates four boolean flags, one for each side of a rectangle.
 *
 * @author   Mark Taylor
 * @since    24 May 2023
 */
@Equality
public class SideFlags {

    private final boolean bottom_;
    private final boolean left_;
    private final boolean top_;
    private final boolean right_;

    /** Instance for which all flags are true. */
    public static final SideFlags ALL = new SideFlags( true, true, true, true );

    /** Instance for which all flags are false. */
    public static final SideFlags NONE =
        new SideFlags( false, false, false, false );

    /**
     * Constructor.
     * The order of parameters corresponds to X, Y, X2, Y2 axes.
     *
     * @param  bottom  flag for bottom edge (primary X axis location)
     * @param  left    flag for left edge (primary Y axis location)
     * @param  top     flag for top edge (secondary X axis location)
     * @param  right   flag for right edge (secondary Y axis location)
     */
    public SideFlags( boolean bottom, boolean left,
                      boolean top, boolean right ) {
        bottom_ = bottom;
        left_ = left;
        top_ = top;
        right_ = right;
    }

    /**
     * Returns the state of the flag for the bottom edge.
     *
     * @return  bottom flag
     */
    public boolean isBottom() {
        return bottom_;
    }

    /**
     * Returns the state of the flag for the left hand edge.
     *
     * @return  left flag
     */
    public boolean isLeft() {
        return left_;
    }

    /**
     * Returns the state of the flag for the top edge.
     *
     * @return   top  flag
     */
    public boolean isTop() {
        return top_;
    }

    /**
     * Returns the state of the flag for the right hand edge.
     *
     * @return  right flag
     */
    public boolean isRight() {
        return right_;
    }

    @Override
    public int hashCode() {
        return ( bottom_ ? 1 : 0 )
             + ( left_ ? 2 : 0 )
             + ( top_ ? 4 : 0 )
             + ( right_ ? 8 : 0 );
    }

    @Override
    public boolean equals( Object other ) {
        return other instanceof SideFlags
            && ((SideFlags) other).hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return new StringBuffer( 4 )
              .append( bottom_ ? 'B' : 'b' )
              .append( left_ ? 'L' : 'l' )
              .append( top_ ? 'T' : 't' )
              .append( right_ ? 'R' : 'r' )
              .toString();
    }
}
