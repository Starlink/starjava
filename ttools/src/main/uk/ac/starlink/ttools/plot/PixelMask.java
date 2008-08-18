package uk.ac.starlink.ttools.plot;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.BitSet;

/**
 * 2-dimensional bit mask.  Like a {@link java.util.BitSet} but can be used
 * to keep track of coverage of a given region of 2-dimensional space.
 *
 * @author   Mark Taylor
 * @since    20 Aug 2007
 */
public class PixelMask {

    private final Rectangle box_;
    private final int npix_;
    private final BitSet mask_;

    /**
     * Constructor.
     *
     * @param   box  defines the boundary of the pixel mask region
     */
    public PixelMask( Rectangle box ) {
        box_ = new Rectangle( box );
        npix_ = box.width * box.height;
        mask_ = new BitSet( npix_ );
    }

    /**
     * Sets all the pixels in the given rectangle.  Pixels outside the 
     * boundaries of this mask are ignored.
     *
     * @param  rect  region to set
     */
    public void set( Rectangle rect ) {
        int xmin = Math.max( rect.x, box_.x );
        int xmax = Math.min( rect.x + rect.width, box_.x + box_.width );
        int ymin = Math.max( rect.y, box_.y );
        int ymax = Math.min( rect.y + rect.height, box_.y + box_.height );
        if ( xmax >= xmin ) {
            for ( int y = ymin; y <= ymax; y++ ) {
                int base = ( y - box_.y ) * box_.width - box_.x;
                assert base + xmin >= 0 && base + xmin < npix_;
                assert base + xmax >= 0 && base + xmax < npix_;
                mask_.set( base + xmin, base + xmax + 1 );
            }
        }
    }

    /**
     * Sets the pixel at the given coordinates.  If it is outside the
     * boundaries of this mask it is ignored.
     *
     * @param   p   coordinates of point to set
     */
    public void set( Point p ) {
        set( p.x, p.y );
    }    

    /**
     * Indicates whether a pixel at given point is set.  If it is outside
     * the boundaries of this mask the answer is false.
     *
     * @param   p   coordinates of point to test
     * @return   true  iff p is set
     */
    public boolean get( Point p ) {
        return get( p.x, p.y );
    }

    /**
     * Sets the pixel at the given coordinates.  If it is outside the
     * boundaries of this mask it is ignored.
     *
     * @param  x  X coordinate of point to set
     * @param  y  Y coordinate of point to set
     */
    public void set( int x, int y ) {
        int index = getIndex( x, y );
        if ( index >= 0 && index < npix_ ) {
            mask_.set( index );
        }
    }

    /**
     * Indicates whether a pixel at given point is set.  If it is outside
     * the boundaries of this mask the answer is false.
     *
     * @param   x  X coordinate of point to test
     * @param   y  Y coordinate of point to test
     * @return   true  iff (x,y) is set
     */
    public boolean get( int x, int y ) {
        int index = getIndex( x, y );
        return ( index >= 0 && index < npix_ )
             ? mask_.get( index )
             : false;
    }

    /**
     * Returns the unchecked index at given coordinates.  Note that if the
     * given coordinates are outside the boundaries of this mask, the
     * return value should not be used for indexing.
     *
     * @param   x  X coordinate
     * @param   y  Y coordinate
     * @return  index value
     */
    private int getIndex( int x, int y ) {
        return ( y - box_.y ) * box_.width + ( x - box_.x );
    }
}
