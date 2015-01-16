package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import java.awt.Graphics;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * XYShape for drawing lines that start at the origin and terminate
 * at the given X,Y displacement.
 *
 * <p>Singleton class.
 *
 * @author   Mark Taylor
 * @since    15 Jan 2015
 */
public class LineXYShape extends XYShape {

    private static final LineXYShape instance_ = new LineXYShape();

    /**
     * Private constructor prevents instantiation.
     */
    private LineXYShape() {
        super( "Line", 16 );
    }

    protected Glyph createGlyph( short x, short y ) {

        /* Point at origin.  Common and cheap. */
        if ( x == 0 && y == 0 ) {
            return POINT;
        }

        /* Horizontal line. */
        else if ( y == 0 ) {
            final int xlo;
            final int xhi;
            if ( x >= 0 ) {
                xlo = 0;
                xhi = x;
            }
            else {
                xlo = x;
                xhi = 0;
            }
            return new LineGlyph( x, y ) {
                public Pixer createPixer( Rectangle clip ) {
                    if ( clip.y <= 0 && clip.y + clip.height >= 1 ) {
                        int xmin = Math.max( xlo, clip.x );
                        int xmax = Math.min( xhi, clip.x + clip.width - 1 );
                        return xmin <= xmax ? new HorizontalPixer( xmin, xmax )
                                            : null;
                    }
                    else {
                        return null;
                    }
                }
            };
        }

        /* Vertical line. */
        else if ( x == 0 ) {
            final int ylo;
            final int yhi;
            if ( y >= 0 ) {
                ylo = 0;
                yhi = y;
            }
            else {
                ylo = y;
                yhi = 0;
            }
            return new LineGlyph( x, y ) {
                public Pixer createPixer( Rectangle clip ) {
                    if ( clip.x <= 0 && clip.x + clip.width >= 1 ) {
                        int ymin = Math.max( ylo, clip.y );
                        int ymax = Math.min( yhi, clip.y + clip.height - 1 );
                        return ymin <= ymax ? new VerticalPixer( ymin, ymax )
                                            : null;
                    }
                    else {
                        return null;
                    }
                }
            };
        }

        /* Diagonal line, more horizontal than vertical. */
        else if ( Math.abs( x ) > Math.abs( y ) ) {
            final int xlo;
            final int xhi;
            if ( x >= 0 ) {
                xlo = 0;
                xhi = x;
            }
            else {
                xlo = x;
                xhi = 0;
            }
            final double slope = (double) y / (double) x;
            return new LineGlyph( x, y ) {
                public Pixer createPixer( Rectangle clip ) {
                    int xmin = Math.max( xlo, clip.x );
                    int xmax = Math.min( xhi, clip.x + clip.width - 1 );
                    return xmin <= xmax
                         ? Pixers.clip( new ShallowPixer( xmin, xmax, slope ),
                                        clip )
                         : null;
                }
            };
        }

        /* Diagonal line, more vertical than horizontal. */
        else {
            assert Math.abs( x ) <= Math.abs( y );
            final int ylo;
            final int yhi;
            if ( y >= 0 ) {
                ylo = 0;
                yhi = y;
            }
            else {
                ylo = y;
                yhi = 0;
            }
            final double slope = (double) x / (double) y;
            return new LineGlyph( x, y ) {
                public Pixer createPixer( Rectangle clip ) {
                    int ymin = Math.max( ylo, clip.y );
                    int ymax = Math.min( yhi, clip.y + clip.height - 1 );
                    return ymin <= ymax
                         ? Pixers.clip( new SteepPixer( ymin, ymax, slope ),
                                        clip )
                         : null;
                }
            };
        }
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  singleton instance
     */
    public static LineXYShape getInstance() {
        return instance_;
    }

    /**
     * Abstract superclass for glyph to paint lines.
     */
    private static abstract class LineGlyph implements Glyph {

        private final short x_;
        private final short y_;

        /**
         * Constructor.
         *
         * @param  x  horizontal displacement
         * @param  y  vertical displacement
         */
        protected LineGlyph( short x, short y ) {
            x_ = x;
            y_ = y;
        }

        public void paintGlyph( Graphics g ) {
            g.drawLine( 0, 0, x_, y_ );
        }
    }

    /**
     * Pixel iterator representing a horizontal line segment at Y=0.
     */
    private static class HorizontalPixer implements Pixer {
        private int x_;
        private final int xmax_;

        /**
         * Constructor.
         *
         * @param  xmin  minimum X value, inclusive
         * @param  xmax  maximum X value, inclusive
         */
        HorizontalPixer( int xmin, int xmax ) {
            x_ = xmin - 1;
            xmax_ = xmax;
        }
        public boolean next() {
            return ++x_ <= xmax_;
        }
        public int getX() {
            return x_;
        }
        public int getY() {
            return 0;
        }
    }

    /**
     * Pixel iterator representing a vertical line segment at X=0.
     */
    private static class VerticalPixer implements Pixer {
        private int y_;
        private final int ymax_;

        /**
         * Constructor.
         *
         * @param  ymin  minimum Y value, inclusive
         * @param  ymax  maximum Y value, inclusive
         */
        VerticalPixer( int ymin, int ymax ) {
            y_ = ymin - 1;
            ymax_ = ymax;
        }
        public boolean next() {
            return ++y_ <= ymax_;
        }
        public int getX() {
            return 0;
        }
        public int getY() {
            return y_;
        }
    }

    /**
     * Pixel iterator representing a segment of a diagonal line closer
     * to the horizontal than the vertical, which would pass through
     * the origin.
     */
    private static class ShallowPixer implements Pixer {
        private final int xmax_;
        private final double slope_;
        private int x_;

        /**
         * Constructor.
         *
         * @param   xmin  minimum X coordinate, inclusive
         * @param   xmax  maximum X coordinate, inclusive
         * @param   slope  dy/dx
         */
        ShallowPixer( int xmin, int xmax, double slope ) {
            xmax_ = xmax;
            slope_ = slope;
            x_ = xmin - 1;
        }
        public boolean next() {
            return ++x_ <= xmax_;
        }
        public int getX() {
            return x_;
        }
        public int getY() {
            return (int) Math.round( x_ * slope_ );
        }
    }

    /**
     * Pixel iterator representing a segment of a diagonal line closer
     * to the vertical than the horizontal, which would pass through
     * the origin.
     */
    private static class SteepPixer implements Pixer {
        private final int ymax_;
        private final double slope_;
        private int y_;

        /**
         * Constructor.
         *
         * @param  ymin  minimum Y coordinate, inclusive
         * @param  ymax  maximum Y coordinate, inclusive
         * @param  slope   dx/dy
         */
        SteepPixer( int ymin, int ymax, double slope ) {
            ymax_ = ymax;
            slope_ = slope;
            y_ = ymin - 1;
        }
        public boolean next() {
            return ++y_ <= ymax_;
        }
        public int getX() {
            return (int) Math.round( y_ * slope_ );
        }
        public int getY() {
            return y_;
        }
    }
}
