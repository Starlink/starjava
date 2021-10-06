package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.BitSet;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Provides drawing primitives on a pixel map.
 * This is a bit like a {@link java.awt.Graphics}, but renders only to
 * a one-bit-per-pixel bitmap.  After drawings have been done, the
 * object can be used as a {@link PixerFactory} to get a list of the pixels
 * which have been hit at least once by one of the drawing methods called
 * during the life of the object.  Pixels will not be repeated in this list.
 *
 * <p>The drawing methods are intended to be as efficient as possible.
 * Bounds checking is done, so it is generally not problematic (or
 * inefficient) to attempt drawing operations with coordinates far outside
 * the drawing's range.
 *
 * @author   Mark Taylor
 * @since    13 Sep 2021
 */
public class PixelDrawing implements PixerFactory {

    private final BitSet pixelMask_;
    private final int x0_;
    private final int y0_;
    private final int width_;
    private final int height_;

    /**
     * Constructs a drawing with bounds given by a rectangle.
     *
     * @param  bounds  rectangle giving the region in which pixels may be
     *         plotted
     */
    public PixelDrawing( Rectangle bounds ) {
        this( bounds.x, bounds.y, bounds.width, bounds.height );
    }

    /**
     * Constructs a drawing with bounds given explicitly.
     *
     * @param  x0  lower bound of X coordinate
     * @param  y0  upper bound of Y coordinate
     * @param  width   extent in X direction
     * @param  height  extent in Y direction
     */
    public PixelDrawing( int x0, int y0, int width, int height ) {
        x0_ = x0;
        y0_ = y0;
        width_ = width;
        height_ = height;
        pixelMask_ = new BitSet( width_ * height_ );
    }

    /**
     * Adds a single pixel to the list of pixels which have been plotted
     * if it is within this drawing's bounds.
     * Calling it with coordinates which have already been plotted,
     * or which are outside this drawing's bounds, has no effect.
     *
     * @param  x  X coordinate
     * @param  y  Y coordinate
     */
    public void addPixel( int x, int y ) {
        if ( contains( x, y ) ) {
            addPixelUnchecked( x, y );
        }
    }

    /**
     * Adds a single pixel to the list of pixels which have been plotted
     * without bounds checking.  An assertion is made that
     * the coordinates are outside of this drawing's bounds.
     *
     * @param  x   X coordinate, must be within this drawing's X bounds
     * @param  y   Y coordinate, must be within this drawing's Y bounds
     */
    public void addPixelUnchecked( int x, int y ) {
        assert contains( x, y ) : "(" + x + "," + y + ") not in "
                                + new Rectangle( x0_, y0_, width_, height_ );
        int xoff = x - x0_;
        int yoff = y - y0_;
        int packed = xoff + width_ * yoff;
        assert packed >= 0 && packed < width_ * height_;
        pixelMask_.set( packed );
    }

    /**
     * Draws a straight line between two points.
     *
     * @param   x0  X coordinate of first point
     * @param   y0  Y coordinate of first point
     * @param   x1  X coordinate of second point
     * @param   y1  Y coordinate of second point
     * @see  java.awt.Graphics#drawLine
     */
    public void drawLine( int x0, int y0, int x1, int y1 ) {

        /* Vertical line. */
        if ( x0 == x1 ) {
            int x = x0;
            if ( x >= x0_ && x < x0_ + width_ ) {
                if ( y0 > y1 ) {
                    int y2 = y1;
                    y1 = y0;
                    y0 = y2;
                }
                int ya = Math.max( y0, y0_ );
                int yb = Math.min( y1, y0_ + height_ - 1 );
                for ( int y = ya; y <= yb; y++ ) {
                    addPixelUnchecked( x, y );
                }
            }
        }

        /* Horizontal line. */
        else if ( y0 == y1 ) {
            int y = y0;
            if ( y >= y0_ && y < y0_ + height_ ) {
                if ( x0 > x1 ) {
                    int x2 = x1;
                    x1 = x0;
                    x0 = x2;
                }
                int xa = Math.max( x0, x0_ );
                int xb = Math.min( x1, x0_ + width_ - 1 );
                for ( int x = xa; x <= xb; x++ ) {
                    addPixelUnchecked( x, y );
                }
            }
        }

        /* Diagonal line, more horizontal than vertical. */
        else if ( Math.abs( x1 - x0 ) > Math.abs( y1 - y0 ) ) {
            if ( x0 > x1 ) {
                int x2 = x1;
                int y2 = y1;
                x1 = x0;
                y1 = y0;
                x0 = x2;
                y0 = y2;
            }
            double slope = (double) ( y1 - y0 ) / (double) ( x1 - x0 );
            int xa = Math.max( x0, x0_ );
            int xb = Math.min( x1, x0_ + width_ );
            for ( int x = xa; x <= xb; x++ ) {
                addPixel( x, y0 + (int) Math.round( ( x - x0 ) * slope ) );
            }
        }

        /* Diagonal line, more vertical than horizontal. */
        else {
            assert Math.abs( x1 - x0 ) <= Math.abs( y1 - y0 );
            if ( y0 > y1 ) {
                int x2 = x1;
                int y2 = y1;
                x1 = x0;
                y1 = y0;
                x0 = x2;
                y0 = y2;
            }
            double slope = (double) ( x1 - x0 ) / (double) ( y1 - y0 );
            int ya = Math.max( y0, y0_ );
            int yb = Math.min( y1, y0_ + height_ );
            for ( int y = ya; y <= yb; y++ ) {
                addPixel( x0 + (int) Math.round( ( y - y0 ) * slope ), y );
            }
        }
    }

    /**
     * Fills a rectangle.
     *
     * @param   x  X coordinate of top left corner
     * @param   y  Y coordinate of top left corner
     * @param   width   width
     * @param   height  height
     * @see   java.awt.Graphics#fillRect
     */
    public void fillRect( int x, int y, int width, int height ) {
        int xlo = Math.max( x0_, x );
        int xhi = Math.min( x0_ + width_, x + width );
        int ylo = Math.max( y0_, y );
        int yhi = Math.min( y0_ + height_, y + height );
        if ( xlo < xhi && ylo < yhi ) {
            for ( int ix = xlo; ix < xhi; ix++ ) {
                for ( int iy = ylo; iy < yhi; iy++ ) {
                    addPixelUnchecked( ix, iy );
                }
            }
        }
    }

    /**
     * Draws the outline of an ellipse with horizontal/vertical axes.
     *
     * @param  x  X coordinate of top left corner of enclosing rectangle
     * @param  y  Y coordinate of top left corner of enclosing rectangle
     * @param  width   width of enclosing rectangle
     * @param  height  height of enclosing rectangle
     * @see   java.awt.Graphics#drawOval
     */
    public void drawOval( int x, int y, int width, int height ) {
        int a = width / 2;
        int b = height / 2;
        double a2r = 1.0 / ( (double) a * a );
        double b2r = 1.0 / ( (double) b * b );
        int x0 = x + a;
        int y0 = y + b;

        int xmin;
        int xmax;
        int xp = x0 - x0_;
        int xq = x0_ + width_ - x0;
        if ( xp < 0 ) {
            xmin = - xp;
            xmax = + xq;
        }
        else if ( xq < 0 ) {
            xmin = - xq;
            xmax = + xp;
        }
        else {
            xmin = 0;
            xmax = Math.min( a, Math.max( xp, xq ) );
        }
        int lasty = 0;
        for ( int ix = xmin; ix < xmax; ix++ ) {
            int iy = (int) Math.round( b * Math.sqrt( 1.0 - a2r * ix * ix ) );
            addPixel( x0 + ix, y0 + iy );
            addPixel( x0 + ix, y0 - iy );
            addPixel( x0 - ix, y0 + iy );
            addPixel( x0 - ix, y0 - iy );
            if ( lasty - iy > 1 ) {
                break;
            }
            lasty = iy;
        }

        int ymin;
        int ymax;
        int yp = y0 - y0_;
        int yq = y0_ + height_ - y0;
        if ( yp < 0 ) {
            ymin = - yp;
            ymax = + yq;
        }
        else if ( yq < 0 ) {
            ymin = - yq;
            ymax = + yp;
        }
        else {
            ymin = 0;
            ymax = Math.min( b, Math.max( yp, yq ) );
        }
        int lastx = 0;
        for ( int iy = ymin; iy < ymax; iy++ ) {
            int ix = (int) Math.round( a * Math.sqrt( 1.0 - b2r * iy * iy ) );
            addPixel( x0 + ix, y0 + iy );
            addPixel( x0 + ix, y0 - iy );
            addPixel( x0 - ix, y0 + iy );
            addPixel( x0 - ix, y0 - iy );
            if ( lastx - ix > 1 ) {
                break;
            }
            lastx = ix;
        }
    }

    /**
     * Fills an ellipse with horizontal/vertical axes.
     *
     * @param  x  X coordinate of top left corner of enclosing rectangle
     * @param  y  Y coordinate of top left corner of enclosing rectangle
     * @param  width   width of enclosing rectangle
     * @param  height  height of enclosing rectangle
     * @see   java.awt.Graphics#drawOval
     */
    public void fillOval( int x, int y, int width, int height ) {
        int a = width / 2;
        int b = height / 2;
        int x0 = x + a;
        int y0 = y + b;
        int xlo = Math.max( x0_, x );
        int xhi = Math.min( x0_ + width_ - 1, x + width - 1 );
        int ylo = Math.max( y0_, y );
        int yhi = Math.min( y0_ + height_ - 1, y + height - 1 );
        double a2 = (double) a * a;
        double b2 = (double) b * b;
        double a2b2 = a2 * b2;
        for ( int ix = xlo; ix <= xhi; ix++ ) {
            int jx = ix - x0;
            double jxb2 = b2 * jx * jx;
            for ( int iy = ylo; iy <= yhi; iy++ ) {
                int jy = iy - y0;
                double jya2 = a2 * jy * jy;
                if ( jxb2 + jya2 <= a2b2 ) {
                    addPixelUnchecked( ix, iy );
                }
            }
        }
    }

    /**
     * Draws the outline of an ellipse with no restrictions on the alignment
     * of its axes.
     *
     * @param  x0  X coordinate of ellipse centre
     * @param  y0  Y coordinate of ellipse centre
     * @param  ax  X component of semi-major (or -minor) axis
     * @param  ay  Y component of semi-major (or -minor) axis
     * @param  bx  X component of semi-minor (or -major) axis
     * @param  by  Y component of semi-minor (Or -major) axis
     */
    public void drawEllipse( int x0, int y0, int ax, int ay, int bx, int by ) {
        int xmax = Math.abs( ax ) + Math.abs( bx );
        int ymax = Math.abs( ay ) + Math.abs( by );
        int xlo = Math.max( x0 - xmax, x0_ );
        int xhi = Math.min( x0 + xmax, x0_ + width_ );
        int ylo = Math.max( y0 - ymax, y0_ );
        int yhi = Math.min( y0 + ymax, y0_ + height_ );

        double kxx = (double) ay * ay + (double) by * by;
        double kxy = -2 * ( (double) ax * ay + (double) bx * by );
        double kyy = (double) ax * ax + (double) bx * bx;
        double r1 = (double) ax * by - (double) bx * ay;
        double r2 = r1 * r1;

        for ( int x = xlo; x <= xhi; x++ ) {
            double x1 = x - x0;
            double x2 = x1 * x1;
            double cA = kyy;
            double cB = kxy * x1;
            double cC = kxx * x2 - r2;
            double a2r = 0.5 / cA;
            double yz = y0 - cB * a2r;
            double yd = Math.sqrt( cB * cB - 4 * cA * cC ) * a2r;
            if ( ! Double.isNaN( yd ) ) {
                addPixel( x, (int) Math.round( yz - yd ) );
                addPixel( x, (int) Math.round( yz + yd ) );
            }
        }

        for ( int y = ylo; y <= yhi; y++ ) {
            double y1 = y - y0;
            double y2 = y1 * y1;
            double cA = kxx;
            double cB = kxy * y1;
            double cC = kyy * y2 - r2;
            double a2r = 0.5 / cA;
            double xz = x0 - cB * a2r;
            double xd = Math.sqrt( cB * cB - 4 * cA * cC ) * a2r;
            if ( ! Double.isNaN( xd ) ) {
                addPixel( (int) Math.round( xz - xd ), y );
                addPixel( (int) Math.round( xz + xd ), y );
            }
        }
    }

    /**
     * Fills an ellipse with no restrictions on the alignment of its axes.
     *
     * @param  x0  X coordinate of ellipse centre
     * @param  y0  Y coordinate of ellipse centre
     * @param  ax  X component of semi-major (or -minor) axis
     * @param  ay  Y component of semi-major (or -minor) axis
     * @param  bx  X component of semi-minor (or -major) axis
     * @param  by  Y component of semi-minor (Or -major) axis
     */
    public void fillEllipse( int x0, int y0, int ax, int ay, int bx, int by ) {
        int xmax = Math.abs( ax ) + Math.abs( bx );
        int ymax = Math.abs( ay ) + Math.abs( by );
        int xlo = Math.max( x0 - xmax, x0_ );
        int xhi = Math.min( x0 + xmax, x0_ + width_ - 1 );
        int ylo = Math.max( y0 - ymax, y0_ );
        int yhi = Math.min( y0 + ymax, y0_ + height_ - 1 );

        double kxx = (double) ay * ay + (double) by * by;
        double kxy = -2 * ( (double) ax * ay + (double) bx * by );
        double kyy = (double) ax * ax + (double) bx * bx;
        double r = (double) ax * by - (double) bx * ay;
        double r2 = r * r;

        if ( xhi - xlo > 0 && yhi - ylo > 0 ) {
            for ( int x = xlo; x <= xhi; x++ ) {
                double x1 = x - x0;
                double x2 = x1 * x1;
                for ( int y = ylo; y <= yhi; y++ ) {
                    double y1 = y - y0;
                    double y2 = y1 * y1;
                    if ( kxx * x2 + kxy * x1 * y1 + kyy * y2 <= r2 ) {
                        addPixelUnchecked( x, y );
                    }
                }
            }
        }
    }

    /**
     * Draws a filled polygon on this drawing.
     *
     * @param  xs   X coordinates of vertices
     * @param  ys   Y coordinates of vertices
     * @param  np   number of vertices
     */
    public void fillPolygon( int[] xs, int[] ys, int np ) {
        if ( np > 0 ) {
            int xlo = xs[ 0 ];
            int xhi = xs[ 0 ];
            int ylo = ys[ 0 ];
            int yhi = ys[ 0 ];
            for ( int ip = 1; ip < np; ip++ ) {
                int x = xs[ ip ];
                int y = ys[ ip ];
                xlo = Math.min( xlo, x );
                xhi = Math.max( xhi, x );
                ylo = Math.min( ylo, y );
                yhi = Math.max( yhi, y );
            }
            if ( ! ( xhi < x0_ || xlo > x0_ + width_ ||
                     yhi < y0_ || ylo > y0_ + height_ ) ) {
                Rectangle bounds = new Rectangle( x0_, y0_, width_, height_ );
                Pixer pixer = new FillPixer( xs, ys, np, bounds );
                while ( pixer.next() ) {
                    addPixelUnchecked( pixer.getX(), pixer.getY() );
                }
            }
        }
    }

    /**
     * Fills an arbitrary shape.
     *
     * @param  shape  shape to fill
     * @see   java.awt.Graphics2D#fill
     * @deprecated  may be slow for large shapes; use fillPolygon if possible
     */
    @Deprecated
    public void fill( Shape shape ) {
        Rectangle box = shape.getBounds();
        int xlo = Math.max( x0_, box.x );
        int xhi = Math.min( x0_ + width_ - 1, box.x + box.width - 1 );
        int ylo = Math.max( y0_, box.y );
        int yhi = Math.min( y0_ + height_ - 1, box.y + box.height - 1 );
        if ( xhi >= xlo && yhi >= ylo ) {
            for ( int x = xlo; x <= xhi; x++ ) {
                double px = (double) x;
                for ( int y = ylo; y <= yhi; y++ ) {
                    double py = (double) y;
                    if ( shape.contains( px, py ) ) {
                        addPixelUnchecked( x, y );
                    }
                }
            }
        }
    }

    /**
     * Indicates whether a given position is within the bounds of this
     * drawing.
     *
     * @param  x  X coordinate
     * @param  y  Y coordinate
     * @return   true iff (x,y) is in bounds
     */
    public boolean contains( int x, int y ) {
        return x >= x0_ && x < x0_ + width_
            && y >= y0_ && y < y0_ + height_;
    }

    /**
     * Returns the pixel mask containing the data for this drawing.
     * The coordinate mapping is not currently defined, but the return value
     * of this method for two PixelDrawings with the same bounds will
     * have the same bit-to-pixel correspondance; a set bit corresponds to
     * a painted pixel.  This value may be modified in place.
     *
     * @return  bit map giving pixel data
     */
    public BitSet getPixels() {
        return pixelMask_;
    }

    public int getMinX() {
        return x0_;
    }

    public int getMaxX() {
        return x0_ + width_ - 1;
    }

    public int getMinY() {
        return y0_;
    }

    public int getMaxY() {
        return y0_ + height_ - 1;
    }

    public int getPixelCount() {
        return pixelMask_.cardinality();
    }

    /**
     * Returns a Pixer that interrogates this drawing's bitmap.
     * May not be maximally efficient if it will be called many times.
     */
    public Pixer createPixer() {
        return new Pixer() {
            int ipNext_ = -1;
            int ix_ = Integer.MIN_VALUE;
            int iy_ = Integer.MIN_VALUE;
            public boolean next() {
                ipNext_ = pixelMask_.nextSetBit( ipNext_ + 1 );
                if ( ipNext_ >= 0 ) {
                    ix_ = ipNext_ % width_ + x0_;
                    iy_ = ipNext_ / width_ + y0_;
                    return true;
                }
                else {
                    ipNext_ = Integer.MAX_VALUE - 1;
                    ix_ = Integer.MIN_VALUE;
                    iy_ = Integer.MIN_VALUE;
                    return false;
                }
            }
            public int getX() {
                return ix_;
            }
            public int getY() {
                return iy_;
            }
        };
    }
}
