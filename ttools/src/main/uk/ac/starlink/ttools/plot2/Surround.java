package uk.ac.starlink.ttools.plot2;

import java.awt.Insets;
import java.awt.Rectangle;

/**
 * Describes the area outside a rectangle reserved for annotations.
 * A {@link Block} is attached to each side of the rectangle.
 *
 * <p>This does a somewhat similar job to {@link java.awt.Insets},
 * but provides more detail about what's happening at the corners.
 *
 * @author   Mark Taylor
 * @since    7 Dec 2022
 */
public class Surround {

    public Block top;
    public Block left;
    public Block bottom;
    public Block right;

    /**
     * Constructs a new Surround with no reserved space.
     */
    public Surround() {
        this( new Block(), new Block(), new Block(), new Block() );
    }

    /**
     * Clone constructor.  Creates a deep copy of a template surround.
     *
     * @param   other  template
     */
    public Surround( Surround other ) {
        this( new Block( other.top ), new Block( other.left ),
              new Block( other.bottom ), new Block( other.right ) );
    }

    /**
     * Constructs a surround with provided blocks.
     *
     * @param  top     block for top side
     * @param  left    block for left side
     * @param  bottom  block for bottom side
     * @param  right   block for right side
     */
    public Surround( Block top, Block left, Block bottom, Block right ) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    /**
     * Returns an Insets object representing all the space reserved
     * by this Surround.
     *
     * @return  new Insets
     */
    public Insets toInsets() {
        int itop = max3( top.extent, left.under, right.under );
        int ileft = max3( left.extent, top.under, bottom.under );
        int ibottom = max3( bottom.extent, left.over, right.over );
        int iright = max3( right.extent, top.over, bottom.over );
        return new Insets( itop, ileft, ibottom, iright );
    }

    /**
     * Returns an array of four rectangles giving the areas described
     * by this object as applied to a given inner rectangle.
     *
     * @param  inner  inner rectangle
     * @return   rectangles for (top, left, bottom, right) blocks
     */
    public Rectangle[] getRegions( Rectangle inner ) {
        int x = inner.x;
        int y = inner.y;
        int w = inner.width;
        int h = inner.height;
        Rectangle topRect =
            new Rectangle( x - top.under, y - top.extent,
                           w + top.under + top.over, top.extent );
        Rectangle leftRect =
            new Rectangle( x - left.extent, y - left.under,
                           left.extent, h + left.under + left.over );
        Rectangle bottomRect =
            new Rectangle( x - bottom.under, y + h,
                           w + bottom.under + bottom.over, bottom.extent );
        Rectangle rightRect =
            new Rectangle( x + w, y - right.under,
                           right.extent, h + right.under + right.over );
        return new Rectangle[] { topRect, leftRect, bottomRect, rightRect };
    }

    /**
     * Adds another surround to this one.
     * Block extents are stacked, but under and over regions are
     * set to the larger value.
     *
     * @param  other  surround to add to this one
     * @return  new surround
     */
    public Surround add( Surround other ) {
        return new Surround( top.add( other.top ),
                             left.add( other.left ),
                             bottom.add( other.bottom ),
                             right.add( other.right ) );
    }

    /**
     * Returns a new surround that represents the union of the areas
     * represented by this one and a supplied one.
     *
     * @param   other  other surround
     * @return  new surround
     */
    public Surround union( Surround other ) {
        return new Surround( top.union( other.top ),
                             left.union( other.left ),
                             bottom.union( other.bottom ),
                             right.union( other.right ) );
    }

    /**
     * Creates a Surround from an Insets.
     * The resulting blocks have extents, but no under or over parts.
     *
     * @param  insets  insets object
     * @return   new Surround
     */
    public static Surround fromInsets( Insets insets ) {
        return new Surround( new Block( insets.top ),
                             new Block( insets.left ),
                             new Block( insets.bottom ),
                             new Block( insets.right ) );
    }

    /**
     * Utility function giving the maximum of three values.
     *
     * @param  i1  value 1
     * @param  i2  value 2
     * @param  i3  value 3
     * @return   largest of inputs
     */
    private static int max3( int i1, int i2, int i3 ) {
        return Math.max( i1, Math.max( i2, i3 ) );
    }

    /**
     * Represents the space taken along one side of a rectangle.
     * The extent is considered to be the distance perpendicular to the side
     * away from the rectangle, while the under/over values are
     * non-negative overhang distances in the negative/positive
     * directions parallel to the side.
     *
     * The top block is represented by the following magnificent ASCII-art:
     * <pre>
     *       ----------------------------------------------------------
     *       |           .              ^                  .          |
     *       |           .              |                  .          |
     *       |(--under--).            extent               .(--over--)|
     *       |           .              |                  .          |
     *       |-----------.              V                  .----------|
     *                   +++++++++++++++++++++++++++++++++++
     *                   ++++++ Attached rectangle +++++++++
     *                   +++++++++++++++++++++++++++++++++++
     * </pre>
     */
    public static class Block {

        public int extent;
        public int under;
        public int over;
 
        /**
         * Constructs an empty block.
         */
        public Block() {
            this( 0, 0, 0 );
        }

        /**
         * Clone constructor.
         * A deep copy of the provided template is constructed.
         * @param  other  template block
         */
        public Block( Block other ) {
            this( other.extent, other.under, other.over );
        }

        /**
         * Constructs a block with an extent but no over or under.
         *
         * @param  extent  extent
         */
        public Block( int extent ) {
            this( extent, 0, 0 );
        }

        /**
         * Constructs a block with all members supplied.
         *
         * @param  extent  extent perpendicular to axis
         * @param  under   non-negative overhang in negative direction
         *                 parallel to axis
         * @param  over    non-negative overhang in positive direction
         *                 parallel to axis
         */
        public Block( int extent, int under, int over ) {
            this.extent = extent;
            this.under = under;
            this.over = over;
        }

        /**
         * Creates a block which is represents the sum of this and another.
         * Extents are added, and the larger over/under values are used.
         *
         * @param   other  block to add to this one
         * @return  new block
         */
        public Block add( Block other ) {
            return new Block( this.extent + other.extent,
                              Math.max( this.under, other.under ),
                              Math.max( this.over, other.over ) );
        }

        /**
         * Creates a block which represents the union of this and another.
         * For each of extent, under and over, the maximum values is used.
         *
         * @param   other  block to add to this one
         * @return  new block
         */
        public Block union( Block other ) {
            return new Block( Math.max( this.extent, other.extent ),
                              Math.max( this.under, other.under ),
                              Math.max( this.over, other.over ) );
        }
    }
}
