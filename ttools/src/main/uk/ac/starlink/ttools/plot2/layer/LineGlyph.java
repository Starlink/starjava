package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Glyph that represents a line from the origin to a given X,Y position.
 *
 * @author   Mark Taylor
 * @since    25 Nov 2013
 */
public abstract class LineGlyph implements Glyph {

    private final int x_;
    private final int y_;

    /** Glyph that paints a single pixel at the origin. */
    public static Glyph POINT = new PointGlyph( 0, 0 );

    private static final int MAX_CACHE_DIST_SQUARE = 8 * 8;
    private static final Map<CoordPair,Glyph> cache_ =
        new ConcurrentHashMap<CoordPair,Glyph>();

    /**
     * Constructor.  In most cases, external users should
     * use one of the factory methods rather than extend this class.
     */
    protected LineGlyph( int x, int y ) {
        x_ = x;
        y_ = y;
    }

    public void paintGlyph( Graphics g ) {
        g.drawLine( 0, 0, x_, y_ );
    }

    /**
     * Returns a glyph to draw a line from the origin to a given point x, y.
     *
     * @param   x  X destination coordinate
     * @param   y  Y destination coordinate
     * @return  line glyph
     */
    public static Glyph getLineGlyph( final int x, final int y ) {

        /* Zero extent, return a single pixel glyph. */
        if ( x == 0 && y == 0 ) {
            return POINT;
        }

        /* If the distance is small, use a cache of precomputed glyphs. */
        else if ( x * x + y * y <= MAX_CACHE_DIST_SQUARE ) {
            CoordPair xy = new CoordPair( (short) x, (short) y );
            Glyph glyph = cache_.get( xy );
            if ( glyph == null ) {
                glyph = createLineGlyph( x, y );
                cache_.put( xy, glyph );
            }
            return glyph;
        }

        /* Otherwise just create one to order. */
        else {
            return createLineGlyph( x, y );
        }
    }

    /**
     * Creates a new glyph representing a line from the origin to X,Y.
     *
     * @param  x  destination X coordinate
     * @param  y  destination Y coordinate
     */
    private static Glyph createLineGlyph( final int x, final int y ) {

        /* Point at origin.  Common and cheap. */
        if ( x == 0 && y == 0 ) {
            return POINT;
        }

        /* Horiontal line. */
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
     * Glyph that paints a single pixel.
     */
    private static class PointGlyph implements Glyph {
        private final int px_;
        private final int py_;

        /**
         * Constructor.
         *
         * @param  px  pixel X coordinate
         * @param  py  pixel Y coordinate
         */
        PointGlyph( int px, int py ) {
            px_ = px;
            py_ = py;
        }
        public void paintGlyph( Graphics g ) {
            g.fillRect( px_, py_, 1, 1 );
        }
        public Pixer createPixer( Rectangle clip ) {
            if ( clip.x <= px_ && clip.x + clip.width >= px_ + 1 &&
                 clip.y <= px_ && clip.y + clip.height >= py_ + 1 ) {
                return new Pixer() {
                    boolean done_;
                    public boolean next() {
                        if ( done_ ) {
                            return false;
                        }
                        else {
                            done_ = true;
                            return true;
                        }
                    }
                    public int getX() {
                        return px_;
                    }
                    public int getY() {
                        return py_;
                    }
                };
            }
            else {
                return null;
            }
        };
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

    /**
     * Aggregates two small integer values.
     * Used to key the cache hash.
     */
    @Equality
    private static final class CoordPair {
        private final int key_;

        /**
         * Constructor.
         *
         * @param  x  X coordinate
         * @param  y  Y coordinate
         */
        CoordPair( short x, short y ) {
            key_ = ( x & 0xffff ) | ( ( y & 0xffff ) << 16 );
        }
        public int hashCode() {
            return key_;
        }
        public boolean equals( Object other ) {
            return other instanceof CoordPair
                && ((CoordPair) other).key_ == this.key_;
        }
    }
}
