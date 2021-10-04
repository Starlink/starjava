package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Abstract superclass for shapes characterised by a pair of short integer
 * values.  These values typically represent the horizontal and vertical
 * extent of a shape, but they don't have to.
 *
 * <p>This object acts as a factory for Glyphs.
 * These glyphs are lazily cached per instance of this class for
 * small values of the X and Y coordinates (since there aren't very
 * many of them, and they are probably used frequently);
 * for larger values, the glyphs are created on demand.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2015
 */
public abstract class XYShape {

    private final String name_;
    private final int maxCacheRadius_;
    private final Glyph pointGlyph_;

    /** Glyph that paints a single pixel at the origin. */
    public static Glyph POINT = new PointGlyph( 0, 0 );

    private final Map<ShortPair,Glyph> cache_ =
        new ConcurrentHashMap<ShortPair,Glyph>();

    /**
     * Constructs a shape with a specified cache limit and point glyph.
     *
     * @param  name  shape name
     * @param  maxCacheRadius  glyphs are cached if both input values
     *                         have an absolute value lower than or equal
     *                         to this limit
     * @param  pointGlyph   glyph to dispense for zero-length lines;
     *                      may be null for no special-case behaviour
     */
    protected XYShape( String name, int maxCacheRadius, Glyph pointGlyph ) {
        name_ = name;
        maxCacheRadius_ = maxCacheRadius;
        pointGlyph_ = pointGlyph;
    }

    /**
     * Constructs a shape with a default cache limit
     * and single-pixel point glyph.
     *
     * @param  name  shape name
     */
    protected XYShape( String name ) {
        this( name, 16, XYShape.POINT );
    }

    /**
     * Returns the name of this shape.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Indicates whether a glyph of a given size will be cached.
     *
     * @param  sx  X radius
     * @param  sy  Y radius
     * @return   true iff glyph will be cached, false if it will be created
     *           on demand
     */
    public boolean isCached( short sx, short sy ) {
        return sx >= -maxCacheRadius_ && sx <= maxCacheRadius_ &&
               sy >= -maxCacheRadius_ && sy <= maxCacheRadius_;
    }

    /**
     * Obtains a glyph characterised by a given pair of values.
     * The glyph may be newly created or obtained from a cache.
     * 
     * @param  sx  X value
     * @param  sy  Y value
     * @return  glyph
     */
    public Glyph getGlyph( short sx, short sy ) {

        /* Zero extent, return a single pixel glyph. */
        if ( sx == 0 && sy == 0 && pointGlyph_ != null ) {
            return pointGlyph_;
        }

        /* If the coordinates are small, use a lazy cache of precomputed
         * glyphs. */
        else if ( isCached( sx, sy ) ) {
            ShortPair xy = new ShortPair( sx, sy );
            Glyph glyph = cache_.get( xy );
            if ( glyph == null ) {
                glyph = createGlyph( sx, sy );
                cache_.put( xy, glyph );
            }
            return glyph;
        }

        /* Otherwise create one to order. */
        else {
            return createGlyph( sx, sy );
        }
    }

    /**
     * Constructs a new glyph with given coordinates.
     *
     * @param  sx  X value
     * @param  sy  Y value
     * @return  new glyph
     */
    protected abstract Glyph createGlyph( short sx, short sy );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns an icon displaying a given shape.
     * The assumption is that the X and Y values are half width and
     * half height of the shape.
     *
     * @param  shape   shape
     * @param  width   icon width
     * @param  height  icon height
     * @param  useComponentColor  if true, the glyph will be painted in
     *                            the component's foreground colour
     * @return   icon
     */
    public static Icon createIcon( XYShape shape, int width, int height,
                                   final boolean useComponentColor ) {
        final short sx = (short) ( width / 2 );
        final short sy = (short) ( height / 2 );
        final Glyph glyph = shape.getGlyph( sx, sy );
        return new Icon() {
            public int getIconWidth() {
                return sx * 2;
            }
            public int getIconHeight() {
                return sy * 2;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                int xoff = x + sx; 
                int yoff = y + sy;
                g.translate( xoff, yoff );
                Color color0 = g.getColor();
                if ( useComponentColor ) {
                    g.setColor( c.getForeground() );
                }
                glyph.paintGlyph( g );
                g.setColor( color0 );
                g.translate( -xoff, -yoff );
            }
        };
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
}
