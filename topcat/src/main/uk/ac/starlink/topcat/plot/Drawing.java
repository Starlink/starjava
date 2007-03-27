package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Provides drawing primitives on a pixel map.
 * This is a bit like a {@link java.awt.Graphics}, but renders only to
 * a one-bit-per-pixel bitmap.  After drawings have been done, the
 * {@link #getPixels} method can be called to get a list of the pixels
 * which have been hit at least once by one of the drawing methods called
 * during the life of the object.
 *
 * <p>The drawing methods are intended to be as efficient as possible.
 * Bounds checking is done, so it is generally not problematic (or 
 * inefficient) to attempt drawing operations with coordinates far outside
 * the drawing's range.
 *
 * @author   Mark Taylor
 * @since    20 Mar 2007
 */
public class Drawing {

    private final Set pixelSet_;
    private final Rectangle bounds_;

    private static final Stroke STROKE = new BasicStroke();

    /**
     * Constructor.
     *
     * @param  bounds  rectangle giving the region in which pixels may be
     *         plotted
     */
    public Drawing( Rectangle bounds ) {
        bounds_ = bounds;
        pixelSet_ = new HashSet();
    }

    /**
     * Returns a list of the pixel coordinates which have been plotted
     * by operations on this drawing.
     * The format of the result is a 2N-element array giving the coordinates
     * of N pixels, in the form (x0,y0, x1,y1, x2,y2, ...).
     * No pixels will be repeated in the list.  No pixels in the list
     * will come from outside the bounds given in the constructor.
     *
     * @return   list of x,y pixel coordinates for plotted pixels
     */
    public int[] getPixels() {
        int[] pixels = new int[ pixelSet_.size() * 2 ];
        int ic = 0;
        for ( Iterator it = pixelSet_.iterator(); it.hasNext(); ) {
            Point p = (Point) it.next();
            pixels[ ic++ ] = p.x;
            pixels[ ic++ ] = p.y;
        }
        assert ic == pixels.length;
        return pixels;
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
            pixelSet_.add( new Point( x, y ) );
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
        double a2r = 1.0 / ( a * a );
        double b2r = 1.0 / ( b * b );
        int x0 = x + a;
        int y0 = y + b;

        int xmax = Math.min( a, Math.max( x0 - bounds_.x,
                                          bounds_.x + bounds_.width - x0 ) );
        int lasty = b;
        for ( int ix = 0; ix < xmax; ix++ ) {
            int iy = (int) Math.round( b * Math.sqrt( 1.0 - ix * ix * a2r ) );
            addPixel( x0 + ix, y0 + iy );
            addPixel( x0 + ix, y0 - iy );
            addPixel( x0 - ix, y0 + iy );
            addPixel( x0 - ix, y0 - iy );
            if ( lasty - iy > 1 ) {
                break;
            }
            lasty = iy;
        }

        int ymax = Math.min( b, Math.max( y0 - bounds_.y,
                                          bounds_.y + bounds_.height - y0 ) );
        int lastx = a;
        for ( int iy = 0; iy < ymax; iy++ ) {
            int ix = (int) Math.round( a * Math.sqrt( 1.0 - iy * iy * b2r ) );
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
        int a2 = a * a;
        int b2 = b * b;
        int a2b2 = a2 * b2;
        for ( int ix = xlo; ix <= xhi; ix++ ) {
            int jx = ix - x0;
            int jxb2 = jx * jx * b2;
            for ( int iy = ylo; iy <= yhi; iy++ ) {
                int jy = iy - y0;
                int jya2 = jy * jy * a2;
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

        double kxx = ay * ay + by * by;
        double kxy = -2 * ( ax * ay + bx * by );
        double kyy = ax * ax + bx * bx;
        double r1 = ax * by - bx * ay;
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

        double kxx = ay * ay + by * by;
        double kxy = -2 * ( ax * ay + bx * by );
        double kyy = ax * ax + bx * bx;
        double r = ax * by - bx * ay;
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
     * Draws the outlie of an arbitrary shape.
     * May not be that efficient.
     *
     * @param   shape  shape to draw
     * @see   java.awt.Graphics2#draw
     */
    public void draw( Shape shape ) {
        fill( STROKE.createStrokedShape( shape ) );
    }
}
