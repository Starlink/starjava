package uk.ac.starlink.ttools.plot;

import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.BitSet;
import java.util.Iterator;

/**
 * Provides drawing primitives on a pixel map.
 * This is a bit like a {@link java.awt.Graphics}, but renders only to
 * a one-bit-per-pixel bitmap.  After drawings have been done, the
 * object can be used as a {@link Pixellator} to get a list of the pixels
 * which have been hit at least once by one of the drawing methods called
 * during the life of the object.  Pixels will not be repeated in this list.
 *
 * <p>The drawing methods are intended to be as efficient as possible.
 * Bounds checking is done, so it is generally not problematic (or 
 * inefficient) to attempt drawing operations with coordinates far outside
 * the drawing's range.
 *
 * @author   Mark Taylor
 * @since    20 Mar 2007
 */
public class Drawing implements Pixellator {

    private final BitSet pixelMask_;
    private final Rectangle bounds_;
    private int nextPointKey_ = Integer.MAX_VALUE;
    private Point point_;

    private static final Stroke STROKE = new BasicStroke();

    /**
     * Constructs a drawing with given pixel bounds.
     *
     * @param  bounds  rectangle giving the region in which pixels may be
     *         plotted
     */
    public Drawing( Rectangle bounds ) {
        bounds_ = bounds;
        pixelMask_ = new BitSet( bounds.width * bounds.height );
    }

    public Rectangle getBounds() {
        return new Rectangle( bounds_ );
    }

    /**
     * Adds a single pixel to the list of pixels which have been plotted.
     * Calling it with coordinates which have already been plotted,
     * or which are outside this drawing's bounds, has no effect.
     *
     * @param  x  X coordinate
     * @param  y  Y coordinate
     */
    public void addPixel( int x, int y ) {
        if ( bounds_.contains( x, y ) ) {
            int xoff = x - bounds_.x;
            int yoff = y - bounds_.y;
            int packed = xoff + bounds_.width * yoff;
            assert packed >= 0 && packed < bounds_.width * bounds_.height;
            pixelMask_.set( packed );
        }
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
            if ( y0 > y1 ) {
                int y2 = y1;
                y1 = y0;
                y0 = y2;
            }
            y0 = Math.max( y0, bounds_.y );
            y1 = Math.min( y1, bounds_.y + bounds_.height );
            for ( int y = y0; y <= y1; y++ ) {
                addPixel( x, y );
            }
        }

        /* Horizontal line. */
        else if ( y0 == y1 ) {
            int y = y0;
            if ( x0 > x1 ) {
                int x2 = x1;
                x1 = x0;
                x0 = x2;
            }
            x0 = Math.max( x0, bounds_.x );
            x1 = Math.min( x1, bounds_.x + bounds_.width );
            for ( int x = x0; x <= x1; x++ ) {
                addPixel( x, y );
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
            x0 = Math.max( x0, bounds_.x );
            x1 = Math.min( x1, bounds_.x + bounds_.width );
            for ( int x = x0; x <= x1; x++ ) {
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
            y0 = Math.max( y0, bounds_.y );
            y1 = Math.min( y1, bounds_.y + bounds_.height );
            for ( int y = y0; y <= y1; y++ ) {
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
        int xlo = Math.max( bounds_.x, x );
        int xhi = Math.min( bounds_.x + bounds_.width, x + width );
        int ylo = Math.max( bounds_.y, y );
        int yhi = Math.min( bounds_.y + bounds_.height, y + height );
        if ( xlo < xhi && ylo < yhi ) {
            for ( int ix = xlo; ix < xhi; ix++ ) {
                for ( int iy = ylo; iy < yhi; iy++ ) {
                    addPixel( ix, iy );
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
        int xp = x0 - bounds_.x;
        int xq = bounds_.x + bounds_.width - x0;
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
        int yp = y0 - bounds_.y;
        int yq = bounds_.y + bounds_.height - y0;
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
        int xlo = Math.max( bounds_.x, x );
        int xhi = Math.min( bounds_.x + bounds_.width, x + width );
        int ylo = Math.max( bounds_.y, y );
        int yhi = Math.min( bounds_.y + bounds_.height, y + height );
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
                    addPixel( ix, iy );
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
        int xlo = Math.max( x0 - xmax, bounds_.x );
        int xhi = Math.min( x0 + xmax, bounds_.x + bounds_.width );
        int ylo = Math.max( y0 - ymax, bounds_.y );
        int yhi = Math.min( y0 + ymax, bounds_.y + bounds_.height );

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
        int xlo = Math.max( x0 - xmax, bounds_.x );
        int xhi = Math.min( x0 + xmax, bounds_.x + bounds_.width );
        int ylo = Math.max( y0 - ymax, bounds_.y );
        int yhi = Math.min( y0 + ymax, bounds_.y + bounds_.height );

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
                        addPixel( x, y );
                    }
                }
            }
        }
    }

    /**
     * Fills an arbitrary shape.
     *
     * @param  shape  shape to fill
     * @see   java.awt.Graphics2#fill
     */
    public void fill( Shape shape ) {
        Rectangle box = shape.getBounds();
        int xlo = Math.max( bounds_.x, box.x );
        int xhi = Math.min( bounds_.x + bounds_.width, box.x + box.width );
        int ylo = Math.max( bounds_.y, box.y );
        int yhi = Math.min( bounds_.y + bounds_.height, box.y + box.height );
        if ( xhi >= xlo && yhi >= ylo ) {
            for ( int x = xlo; x <= xhi; x++ ) {
                double px = (double) x;
                for ( int y = ylo; y <= yhi; y++ ) {
                    double py = (double) y;
                    if ( shape.contains( px, py ) ) {
                        addPixel( x, y );
                    }
                }
            }
        }
    }

    /**
     * Draws the outline of an arbitrary shape.
     * May not be that efficient.
     *
     * @param   shape  shape to draw
     * @see   java.awt.Graphics2#draw
     */
    public void draw( Shape shape ) {
        fill( STROKE.createStrokedShape( shape ) );
    }

    /**
     * Adds all the pixels from the given Pixellator to this drawing.
     *
     * @param  pixellator  iterator over pixels to add
     */
    public void addPixels( Pixellator pixellator ) {
        for ( pixellator.start(); pixellator.next(); ) {
            addPixel( pixellator.getX(), pixellator.getY() );
        }
    }

    //
    // Pixellator interface.
    //
    public void start() {
        point_ = new Point();
        nextPointKey_ = -1;
    }

    public boolean next() {
        nextPointKey_ = pixelMask_.nextSetBit( nextPointKey_ + 1 );
        if ( nextPointKey_ >= 0 ) {
            point_.x = nextPointKey_ % bounds_.width + bounds_.x;
            point_.y = nextPointKey_ / bounds_.width + bounds_.y;
            return true;
        }
        else {
            point_ = null;
            nextPointKey_ = Integer.MAX_VALUE;
            return false;
        }
    }

    public int getX() {
        return point_.x;
    }

    public int getY() {
        return point_.y;
    }

    /**
     * Combines an array of given pixellators to produce a single one which
     * iterates over all the pixels.  It is tempting just to provide a
     * new Pixellator implementation which iterates over its consituent
     * ones to do this, but that would risk returning some pixels multiple
     * times, which we don't want.
     *
     * @param  pixers   array of pixellators to combine
     * @return   pixellator comprising the union of the supplied ones
     */
    public static Pixellator combinePixellators( Pixellator[] pixers ) {
        Rectangle bounds = null;
        for ( int is = 0; is < pixers.length; is++ ) {
            Rectangle sbounds = pixers[ is ].getBounds();
            if ( bounds != null ) {
                if ( sbounds != null ) {
                    bounds.add( new Rectangle( sbounds ) );
                }
            }
            else {
                bounds = new Rectangle( sbounds );
            }
        }
        if ( bounds == null ) {
            return new Drawing( new Rectangle( 0, 0, 0, 0 ) ) {
                public Rectangle getBounds() {
                    return null;
                }
            };
        }
        Drawing union = new Drawing( bounds );
        for ( int is = 0; is < pixers.length; is++ ) {
            union.addPixels( pixers[ is ] );
        }
        return union;
    }
}
