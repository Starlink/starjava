package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.TablePlot;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Defines a style of marker for plotting in a scatter plot.
 * It is defined by {@link MarkerShape}, colour and size.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2021
 */
public class MarkerStyle implements Style {

    private final MarkerShape shapeId_;
    private final Color color_;
    private final int size_;
    private final Consumer<Graphics> drawShape_;
    private final PixerFactory pixerFact_;

    /** Standard width in pixels for legend icon. */
    public static final int LEGEND_ICON_WIDTH = 20;

    /** Standard height in pixels for legend icon. */
    public static final int LEGEND_ICON_HEIGHT = 12;

    private static final RenderingHints PIX_HINTS = createPixerRenderingHints();

    /**
     * Constructs a MarkerStyle based on an object that draws to a Graphics
     * context.
     *
     * @param  shapeId  shape family
     * @param  color   colour
     * @param  size    nominal size (non-negative integer)
     * @param  maxr    maximum radius (furthest distance from center that
     *                 the shape may plot a pixel; overestimate is OK)
     * @param  drawShape  can draw the sized shape to a graphics context
     */
    public MarkerStyle( MarkerShape shapeId, Color color, int size,
                        int maxr, Consumer<Graphics> drawShape ) {
        this( shapeId, color, size, drawShape,
              shapeToPixer( drawShape, maxr ) );
    }

    /**
     * Constructs a MarkerStyle based on an object that draws to a Graphics
     * context and a supplied pixel iterator.
     * These two representations should paint the same shape, but it allows
     * for custom pixel patterns where drawing may not get it quite right.
     * It's also slightly more efficient, but MarkStyle construction is
     * not a critical part of plot generation.
     *
     * @param  shapeId  shape family
     * @param  color   colour
     * @param  size    nominal size (non-negative integer)
     * @param  drawShape  can draw the sized shape to a graphics context
     * @param  pixer   pixel iterator giving shape
     */
    public MarkerStyle( MarkerShape shapeId, Color color, int size,
                        Consumer<Graphics> drawShape, Pixer pixer ) {
        color_ = color;
        shapeId_ = shapeId;
        size_ = size;
        drawShape_ = drawShape;
        pixerFact_ = Pixers.createPixerCopier( pixer );
    }

    /**
     * Draws this marker's shape centered at the origin in a graphics context.
     * Implementing classes don't need to worry about the colour.
     *
     * @param  g  graphics context
     */
    public void drawShape( Graphics g ) {
        drawShape_.accept( g );
    }

    /**
     * Returns an iterator over pixel offsets which can be used to draw this
     * marker onto a raster.  This can be used as an alternative to
     * rendering the marker using the <code>drawMarker()</code> methods
     * in situations where it might be more efficient.
     * The assumption is that all the pixels are the same colour.
     *
     * <p>This implementation doesn't need to be fast, the data will be
     * cached before use.
     *
     * @return   pixel offset iterator representing this style as a bitmap
     */
    public PixerFactory getPixerFactory() {
        return pixerFact_;
    }

    /**
     * Draws this marker's shape centred at the origin suitable for display
     * as a legend.  The default implementation just invokes
     * {@link #drawShape}, but it may be overridden if there are special
     * requirements, for instance if <code>drawShape</code> draws a miniscule
     * graphic.
     *
     * @param   g  graphics context
     */
    public void drawLegendShape( Graphics g ) {

        /* Since the bit patterns are hand crafted for some of these marker
         * shapes, we get better results using the bitmaps directly than
         * leaving it to the graphics context rendering in some cases.
         * Using drawShape on a bitmapped surface can give wonky/ugly
         * renderings. */
        if ( TablePlot.isVectorContext( g ) ) {
            drawShape( g );
        }
        else {
            for ( Pixer pixer = pixerFact_.createPixer(); pixer.next(); ) {
                g.fillRect( pixer.getX(), pixer.getY(), 1, 1 );
            }
        }
    }

    public Icon getLegendIcon() {
        return getLegendIcon( LEGEND_ICON_WIDTH, LEGEND_ICON_HEIGHT );
    }

    /**
     * Returns an icon of a requested size with this marker
     * painted in the center.
     * 
     * @param  width  icon width in pixels
     * @param  height  icon height in pixels
     */
    public Icon getLegendIcon( final int width, final int height ) {
        return new Icon() {
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Color color0 = g.getColor();
                g.setColor( color_ );
                int xoff = x + width / 2;
                int yoff = y + height / 2;
                g.translate( xoff, yoff );
                drawLegendShape( g );
                g.translate( -xoff, -yoff );
                g.setColor( color0 );
            }
        };
    }

    @Override
    public int hashCode() {
        int code = 668097;
        code = 23 * code + shapeId_.hashCode();
        code = 23 * code + color_.hashCode();
        code = 23 * code + size_;
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof MarkerStyle ) {
            MarkerStyle other = (MarkerStyle) o;
            return this.shapeId_.equals( other.shapeId_ )
                && this.color_.equals( other.color_ )
                && this.size_ == other.size_;
        }
        else {
            return false;
        }
    }

    /**
     * Creates the RenderingHints object used for turning drawn shapes into
     * pixel arrays. 
     *
     * @return   new rendering hints
     */
    private static RenderingHints createPixerRenderingHints() {
        RenderingHints hints = new RenderingHints( null );
        hints.put( RenderingHints.KEY_ANTIALIASING,
                   RenderingHints.VALUE_ANTIALIAS_OFF );
        hints.put( RenderingHints.KEY_RENDERING,
                   RenderingHints.VALUE_RENDER_QUALITY );
        hints.put( RenderingHints.KEY_DITHERING,
                   RenderingHints.VALUE_DITHER_DISABLE );
        hints.put( RenderingHints.KEY_FRACTIONALMETRICS,
                   RenderingHints.VALUE_FRACTIONALMETRICS_ON );
        return hints;
    }

    /**
     * Provides a pixer corresponding to activities on a graphics context.
     *
     * @param   drawShape  callback that will draw a shape to a graphic context;
     *                     colours are ignored
     * @param  maxr    maximum radius (furthest distance from origin that
     *                 the shape may plot a pixel; overestimate is OK)
     * @return  iterator over pixels touched
     */
    private static Pixer shapeToPixer( Consumer<Graphics> drawShape,
                                       int maxr ) {

        /* Construct a BufferedImage big enough to hold all the pixels
         * in a rendering of the marker. */
        int xdim = 2 * maxr + 1;
        int ydim = 2 * maxr + 1;
        int xoff = maxr;
        int yoff = maxr;
        BufferedImage im =
            new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );

        /* Draw this marker onto the graphics associated with it.
         * We use high quality rendering hints since we're only going to
         * do this once so we might as well get the best shape for it
         * (not that it seems to make much difference). */
        Graphics2D g = im.createGraphics();
        g.setRenderingHints( PIX_HINTS );
        g.translate( xoff, yoff );
        drawShape.accept( g );

        /* Now examine the pixels in the image we've just drawn to, and
         * extract a list of the touched pixels. */
        Raster raster = im.getData();
        List<Point> pointList = new ArrayList<>( xdim * ydim );
        for ( int ix = 0; ix < xdim; ix++ ) {
            for ( int iy = 0; iy < ydim; iy++ ) {
                int alpha = raster.getSample( ix, iy, 3 );
                if ( alpha > 0 ) {
                    assert alpha == 255 : alpha;
                    pointList.add( new Point( ix - xoff, iy - yoff ) );
                }
            }
        }
        final Point[] points = pointList.toArray( new Point[ 0 ] );
        return Pixers.createPointsPixer( points );
    }
}
