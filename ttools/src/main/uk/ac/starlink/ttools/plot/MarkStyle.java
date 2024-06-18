package uk.ac.starlink.ttools.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.util.IntList;

/**
 * Defines a style of marker for plotting in a scatter plot.
 * The marker part of a MarkStyle is characterised visually by its
 * shapeId, colour and size.  If it represents a line to be drawn as well
 * it also has a stroke and a join type.  A matching instance of a
 * MarkStyle style can in general be produced by doing
 * <pre>
 *    style1 = style0.getShapeId()
 *                   .getStyle( style0.getColor(), style0.getSize() );
 *    style1.setLine( style0.getLine() );
 *    style1.setLineWidth( style0.getLineWidth() );
 *    style1.setDash( style0.getDash() );
 *    style1.setHidePoints( style0.getHidePoints() );
 *    style1.setOpaqueLimit( style0.getOpaqueLimit() );
 *    style1.setErrorRenderer( style0.getErrorRenderer() );
 * </pre>
 * style0 and style1 should then match according to the <code>equals()</code>
 * method.  A style may however have a null <code>shapeId</code>, in
 * which case you can't generate a matching instance.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public abstract class MarkStyle extends DefaultStyle {

    private final int size_;
    private final int maxr_;
    private final MarkShape shapeId_;
    private Line line_;
    private boolean hidePoints_;
    private int opaqueLimit_ = 1;
    private ErrorRenderer errorRenderer_;
    private Pixellator pixoffs_;
    private static final RenderingHints pixHints_;
    private ErrorModeSelection[] errorModeSelections_;

    /** Symbolic constant meaning join points by straight line segments. */
    public static final Line DOT_TO_DOT = new Line( "DotToDot" );

    /** Symbolic constant meaning draw a linear regression line. */
    public static final Line LINEAR = new Line( "LinearRegression" );

    public static final int LEGEND_ICON_WIDTH = 20;
    public static final int LEGEND_ICON_HEIGHT = 12;
    private static final RenderingHints.Key AA_KEY =
        RenderingHints.KEY_ANTIALIASING;
    private static final Object AA_ON = RenderingHints.VALUE_ANTIALIAS_ON;
    private static final Font LABEL_FONT =
        new GraphicsBitmap( 1, 1 ).createGraphics().getFont();
    private static final FontRenderContext PIXEL_FRC =
        new FontRenderContext( null, false, false );

    static {
        pixHints_ = new RenderingHints( null );
        pixHints_.put( RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_OFF );
        pixHints_.put( RenderingHints.KEY_RENDERING,
                       RenderingHints.VALUE_RENDER_QUALITY );
        pixHints_.put( RenderingHints.KEY_DITHERING,
                       RenderingHints.VALUE_DITHER_DISABLE );
        pixHints_.put( RenderingHints.KEY_FRACTIONALMETRICS,
                       RenderingHints.VALUE_FRACTIONALMETRICS_ON );
    }

    /**
     * Constructor.
     *
     * @param   color  colour
     * @param   otherAtts  distinguisher for this instance (besides class
     *                     and colour)
     * @param   shapeId  style factory 
     * @param   size     nominal size
     * @param   maxr     maximum radius (furthest distance from centre that
     *                   this style may plot a pixel)
     */
    protected MarkStyle( Color color, Object otherAtts,
                         MarkShape shapeId, int size, int maxr ) {
        super( color, otherAtts );
        shapeId_ = shapeId;
        size_ = size;
        maxr_ = maxr;
        errorRenderer_ = ErrorRenderer.DEFAULT;
        errorModeSelections_ = new ErrorModeSelection[ 0 ];
    }

    /**
     * Draws this marker's shape centered at the origin in a graphics context.
     * Implementing classes don't need to worry about the colour.
     *
     * @param  g  graphics context
     */
    public abstract void drawShape( Graphics g );

    /**
     * Draws this marker's shape centred at the origin suitable for display
     * as a legend.  The default implementation just invokes 
     * {@link #drawShape}, but it may be overridden if there are special
     * requirements, for instance if <code>drawShape</code> draws a minuscule
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
            Pixellator pixer = getPixelOffsets();
            for ( pixer.start(); pixer.next(); ) {
                g.fillRect( pixer.getX(), pixer.getY(), 1, 1 );
            }
        }
    }

    /**
     * Returns the maximum radius of a marker drawn by this class.
     * It is permissible to return a (gross) overestimate if no sensible
     * maximum can be guaranteed.
     *
     * @return   maximum distance from the specified
     *           <code>x</code>,<code>y</code>
     *           point that <code>drawMarker</code> might draw
     */
    public int getMaximumRadius() {
        return maxr_;
    }

    /**
     * Returns this style's shape id.  This is a factory capable of producing
     * match styles which resemble this one in point of shape (but may
     * differ in size or colour).
     *
     * @return   style factory
     */
    public MarkShape getShapeId() {
        return shapeId_;
    }

    /**
     * Returns the nominal size of this style.  In general a size of 1 
     * is the smallest, 2 is the next smallest etc.
     *
     * @return   style size
     */
    public int getSize() {
        return size_;
    }

    /**
     * Sets the line type for this style.
     *
     * @param  line  line type
     */
    public void setLine( Line line ) {
        line_ = line;
    }

    /**
     * Returns the line type for this style.
     *
     * @return  line type
     */
    public Line getLine() {
        return line_;
    }

    /**
     * Sets whether points should be hidden or visible.
     * This will usually only be honoured if the line style is non-null.
     *
     * @param  visible  true if you want points to be invisible
     */
    public void setHidePoints( boolean visible ) {
        hidePoints_ = visible;
    }

    /**
     * Indicates whether points are hidden or visible.
     * This should usually only be honoured if the line style is non-null.
     *
     * @return  true if points are to be invisible
     */
    public boolean getHidePoints() {
        return hidePoints_;
    }

    /**
     * Sets the opacity limit for this style.  The limit is the number
     * of pixels plotted on top of each other which will result in
     * complete opacity.  The default is one, which corresponds to 
     * fully opaque pixels.
     *
     * @param   lim  new opacity limit
     */
    public void setOpaqueLimit( int lim ) {
        if ( lim < 1 ) {
            throw new IllegalArgumentException( lim + " < 1" );
        }
        opaqueLimit_ = lim;
    }

    /**
     * Returns the opacity limit for this style.
     *
     * @return  opacity limit
     */
    public int getOpaqueLimit() {
        return opaqueLimit_;
    }

    /**
     * Sets the style used for drawing error bars around this marker.
     *
     * @param  errorRenderer  error bar style
     */
    public void setErrorRenderer( ErrorRenderer errorRenderer ) {
        errorRenderer_ = errorRenderer;
    }

    /**
     * Returns the style used for drawing error bars around this marker.
     *
     * @return   error bar style
     */
    public ErrorRenderer getErrorRenderer() {
        return errorRenderer_;
    }

    /**
     * Sets the error mode suppliers with which this mark style will be used.
     * These objects are only used to affect the way that legends
     * are drawn; in particular they are NOT used when determining 
     * object equality.
     *
     * @param   errSelections  error mode choices
     */
    public void setErrorModeModels( ErrorModeSelection[] errSelections ) {
        errorModeSelections_ = errSelections;
    }

    /**
     * Returns the colour to use for drawing labels.
     *
     * @return   label colour
     */
    public Color getLabelColor() {
        return Color.BLACK;
    }

    /**
     * Draws this marker centered at a given position.  
     * This method sets the colour of the graphics context and 
     * then calls {@link #drawShape}.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     */
    public void drawMarker( Graphics g, int x, int y ) {
         drawMarker( g, x, y, null );
    }

    /**
     * Draws this marker in a way which may be modified by a supplied
     * <code>ColorTweaker</code> object.  This permits changes to
     * be made to the colour just before the marker is drawn.
     * In some cases this could be handled by modifying the graphics 
     * context before the call to <code>drawMarker</code>, but doing it
     * like this makes sure that the graphics context has been assigned
     * the right colour and position.
     *
     * @param  g  graphics context
     * @param  x  x position
     * @param  y  y position
     * @param  fixer  hook for modifying the colour (may be null)
     */
    public void drawMarker( Graphics g, int x, int y, ColorTweaker fixer ) {
        Color origColor = g.getColor();
        Color markColor = fixer == null ? getColor()
                                        : fixer.tweakColor( getColor() );
        g.setColor( markColor );
        g.translate( x, y );
        drawShape( g );
        g.translate( -x, -y );
        g.setColor( origColor );
    }

    /**
     * Draws error bars using this style's current error renderer.
     *
     * @param  g  graphics context
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
     * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
     * @see    ErrorRenderer#drawErrors
     */
    public void drawErrors( Graphics g, int x, int y,
                            int[] xoffs, int[] yoffs ) {
        drawErrors( g, x, y, xoffs, yoffs, null );
    }

    /**
     * Draws error bars using this style's current error renderer in a way
     * which may be modified by a supplied <code>ColorTweaker</code> object.
     *
     * @param  g  graphics context
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
     * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
     * @param  fixer  hook for modifying the colour (may be null)
     * @see    ErrorRenderer#drawErrors
     */
    public void drawErrors( Graphics g, int x, int y,
                            int[] xoffs, int[] yoffs, ColorTweaker fixer ) {
        Color origColor = g.getColor();
        Color errColor = fixer == null ? getColor()
                                       : fixer.tweakColor( getColor() );
        g.setColor( errColor );
        errorRenderer_.drawErrors( g, x, y, xoffs, yoffs );
        g.setColor( origColor );
    }

    /**
     * Draws a label for a marker at a given point.
     *
     * @param  g  graphics context
     * @param  x  X coordinate of point
     * @param  y  Y coordinate of point
     * @param  label   text to draw
     */
    public void drawLabel( Graphics g, int x, int y, String label ) {
        drawLabel( g, x, y, label, null );
    }

    /**
     * Draws a label for a marker at a given point with optional 
     * colour modification.
     *
     * @param  g  graphics context
     * @param  x  X coordinate of point
     * @param  y  Y coordinate of point
     * @param  label   text to draw
     * @param  fixer  hook for modifying the colour (may be null)
     */
    public void drawLabel( Graphics g, int x, int y, String label,
                           ColorTweaker fixer ) {
        Color origColor = g.getColor();
        Color labelColor = fixer == null ? getLabelColor()
                                         : fixer.tweakColor( getLabelColor() );
        g.setColor( labelColor );
        g.drawString( label, x + 4, y - 4 );
        g.setColor( origColor );
    }

    /**
     * Draws a legend icon for this style without error rendering.
     *
     * @return   legend icon
     */
    public Icon getLegendIcon() {
        int nerr = errorModeSelections_.length;
        ErrorMode[] modes = new ErrorMode[ nerr ];
        for ( int ierr = 0; ierr < nerr; ierr++ ) {
            modes[ ierr ] = errorModeSelections_[ ierr ].getErrorMode();
        }
        return getLegendIcon( modes );
    }

    /**
     * Draws a legend icon for this style given certain error modes.
     *
     * @param   errorModes  array of error modes, one for each dimension
     * @return  legend icon
     */
    public Icon getLegendIcon( ErrorMode[] errorModes ) {
        final Icon errorIcon = errorRenderer_.isBlank( errorModes )
                ? null
                : errorRenderer_.getLegendIcon( errorModes, LEGEND_ICON_WIDTH,
                                                LEGEND_ICON_HEIGHT, 1, 1 );
        return new Icon() {
            public int getIconHeight() {
                return LEGEND_ICON_HEIGHT;
            }
            public int getIconWidth() {
                return LEGEND_ICON_WIDTH;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g;
                boolean hide = getHidePoints();
                if ( getLine() != null ) {
                    Object aaHint = g2.getRenderingHint( AA_KEY );
                    g2.setRenderingHint( AA_KEY, AA_ON );
                    Stroke stroke = g2.getStroke();
                    g2.setStroke( getStroke( BasicStroke.CAP_BUTT,
                                             BasicStroke.JOIN_MITER ) );
                    Color color = g2.getColor();
                    g2.setColor( getColor() );
                    int ypos = y + LEGEND_ICON_HEIGHT / 2;
                    g2.drawLine( x, ypos, x + LEGEND_ICON_WIDTH, ypos );
                    g2.setColor( color );
                    g2.setStroke( stroke );
                    g2.setRenderingHint( AA_KEY, aaHint );
                }
                if ( errorIcon != null ) {
                    Object aaHint = g2.getRenderingHint( AA_KEY );
                    g2.setRenderingHint( AA_KEY, AA_ON );
                    Color color = g2.getColor();
                    g2.setColor( getColor() );
                    errorIcon.paintIcon( c, g2, x, y );
                    g2.setColor( color );
                    g2.setRenderingHint( AA_KEY, aaHint );
                }
                if ( ! hide ) {
                    Color color = g2.getColor();
                    g2.setColor( getColor() );
                    int xoff = x + LEGEND_ICON_WIDTH / 2;
                    int yoff = y + LEGEND_ICON_HEIGHT / 2;
                    g2.translate( xoff, yoff );
                    drawLegendShape( g2 );
                    g2.translate( -xoff, -yoff );
                    g2.setColor( color );
                }
            }
        };
    }

    /**
     * Returns an icon that draws this MarkStyle.
     *
     * @param  width  icon width
     * @param  height icon height
     * @return icon
     */
    public Icon getIcon( final int width, final int height ) {
        return new Icon() {
            public int getIconHeight() {
                return height;
            }
            public int getIconWidth() {
                return width;
            }
            public void paintIcon( Component c, Graphics g, 
                                   int xoff, int yoff ) {
                int x = xoff + width / 2;
                int y = yoff + height / 2;
                drawMarker( g, x, y );
            }
        };
    }

    /**
     * Returns an iterator over pixel offsets which can be used to draw this
     * marker onto a raster.  This can be used as an alternative to 
     * rendering the marker using the <code>drawMarker()</code> methods
     * in situations where it might be more efficient.
     * The assumption is that all the pixels are the same colour.
     *
     * @return   pixel offset iterator representing this style as a bitmap
     */
    public Pixellator getPixelOffsets() {
        if ( pixoffs_ == null ) {

            /* Construct a BufferedImage big enough to hold all the pixels
             * in a rendering of the marker. */
            int xdim = 2 * maxr_ + 1;
            int ydim = 2 * maxr_ + 1;
            int xoff = maxr_;
            int yoff = maxr_;
            BufferedImage im =
                new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );

            /* Draw this marker onto the graphics associated with it.
             * We use high quality rendering hints since we're only going to
             * do this once so we might as well get the best shape for it
             * (not that it seems to make much difference). */
            Graphics2D g = im.createGraphics();
            g.setRenderingHints( pixHints_ );
            drawMarker( g, xoff, yoff, null );

            /* Now examine the pixels in the image we've just drawn to, and
             * extract a list of the touched pixels. */
            Raster raster = im.getData();
            List<Point> pointList = new ArrayList<Point>( xdim * ydim );
            for ( int ix = 0; ix < xdim; ix++ ) {
                for ( int iy = 0; iy < ydim; iy++ ) {
                    int alpha = raster.getSample( ix, iy, 3 );
                    if ( alpha > 0 ) {
                        assert alpha == 255 : alpha;
                        pointList.add( new Point( ix - xoff, iy - yoff ) );
                    }
                }
            }
            Point[] points = pointList.toArray( new Point[ 0 ] );
            pixoffs_ = new PointArrayPixellator( points );
        }
        return pixoffs_;
    }

    /** 
     * Returns an array of 1-dimensional pixel offsets which can be used
     * to draw this marker onto a raster.
     *
     * @param  xStride   X dimension of the buffer
     * @return  array of offsets into a buffer at which pixels representing
     *          this style should be inserted
     */
    public int[] getFlattenedPixelOffsets( int xStride ) {
        IntList offList = new IntList();
        Pixellator pixer = getPixelOffsets();
        for ( pixer.start(); pixer.next(); ) {
            offList.add( pixer.getX() + pixer.getY() * xStride );
        }
        return offList.toIntArray();
    }

    /**
     * Returns an array over pixel positions which can be used to draw a
     * label for this style.  The bounds of the returned pixellator are 
     * a reasonable estimate of the bounds of the text to be drawn.
     *
     * @param  label    text of label to draw
     * @param  x   X coordinate of point to label
     * @param  y   Y coordinate of point to label
     * @param  clip  clipping region, or null
     */
    public Pixellator getLabelPixels( String label, int x, int y,
                                      Rectangle clip ) {

        /* Offset label from central position. */
        x += 4;
        y -= 4;

        /* Do the work.  Currently the graphics-based method is used, it
         * seems to be a lot faster.  The other implementation is available
         * for reference though. */
        if ( true ) {
            return bitmapTextPixellator( label, x, y, getLabelFont(), clip );
        }
        else {
            return glyphTextPixellator( label, x, y, getLabelFont(), clip );
        }
    }

    /**
     * Returns the font to use for labels.
     *
     * @return  label font
     */
    private Font getLabelFont() {
        return LABEL_FONT;
    }

    public boolean equals( Object o ) {
        if ( super.equals( o ) ) {
            MarkStyle other = (MarkStyle) o;
            return this.line_ == other.line_
                && this.hidePoints_ == other.hidePoints_
                && this.opaqueLimit_ == other.opaqueLimit_
                && this.errorRenderer_.equals( other.errorRenderer_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = code * 23 + ( line_ == null ? 1 : line_.hashCode() );
        code = code * 23 + ( hidePoints_ ? 0 : 1 );
        code = code * 23 + opaqueLimit_;
        code = code * 23 + errorRenderer_.hashCode();
        return code;
    }

    /**
     * Utility method indicating whether errors will be drawn for a given 
     * style when a given set of points is plotted.
     *
     * @param  style  plotting style
     * @param  plotData  plotting data
     * @return  false if rendering the error bars will have no effect
     */
    public static boolean hasErrors( MarkStyle style, PlotData plotData ) {
        return plotData.getNerror() > 0
            && ! style.getErrorRenderer().isBlank( null );
    }

    /**
     * Returns a style which looks like a target.  Suitable for use
     * as a cursor.
     */
    public static MarkStyle targetStyle() {
        return new MarkStyle( new Color( 0, 0, 0, 192 ), new Object(),
                              null, 1, 7 ) {
            final Stroke stroke_ = new BasicStroke( 2, BasicStroke.CAP_ROUND,
                                                    BasicStroke.JOIN_ROUND );
            public void drawShape( Graphics g ) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setStroke( stroke_ );
                g2.drawOval( -6, -6, 13, 13 );
                g2.drawLine( 0, +4, 0, +8 );
                g2.drawLine( 0, -4, 0, -8 );
                g2.drawLine( +4, 0, +8, 0 );
                g2.drawLine( -4, 0, -8, 0 );
            }
        };
    }

    /**
     * Gets a text label pixellator using a bitmap.
     * Null may be returned if there are no pixels.
     *
     * @param   text   text to paint
     * @param   x      X coordinate of text origin
     * @param   y      Y coordinate of text origin
     * @param   font   font
     * @param   clip   clipping region, or null
     * @return  pixel iterator, or null
     */
    private static Pixellator bitmapTextPixellator( String text, int x, int y,
                                                    Font font,
                                                    Rectangle clip ) {
        Rectangle bounds = font.getStringBounds( text, PIXEL_FRC ).getBounds();
        int xoff = bounds.x;
        int yoff = bounds.y;
        if ( clip != null ) {
            bounds = new Rectangle( clip.x - x, clip.y - y,
                                    clip.width, clip.height )
                    .intersection( bounds );
        }
        if ( bounds.isEmpty() ) {
            return null;
        }
        else {
            GraphicsBitmap bitmap =
                new GraphicsBitmap( bounds.width, bounds.height );
            Graphics g = bitmap.createGraphics();
            g.setFont( font );
            g.drawString( text, -bounds.x, -bounds.y );
            return new TranslatedPixellator( bitmap.createPixellator(),
                                             x + bounds.x, y + bounds.y );
        }
    }

    /**
     * Gets a text label pixellator using glyph vectors.
     *
     * @param   text   text to paint
     * @param   x      X coordinate of text origin
     * @param   y      Y coordinate of text origin
     * @param   font   font
     * @param   clip   clipping region, or null
     * @return  pixel iterator
     */
    private static Pixellator glyphTextPixellator( String text, int x, int y,
                                                   Font font, Rectangle clip ) {
        Shape outline =
            font.createGlyphVector( PIXEL_FRC, text ).getOutline( x, y );
        Rectangle bounds = outline.getBounds();
        if ( clip != null ) {
            bounds = bounds.intersection( clip );
        }
        Drawing drawing = new Drawing( bounds );
        drawing.fill( outline );
        return drawing;
    }

    /**
     * Enumeration class describing the types of line which can be drawn
     * in association with markers.
     */
    public static class Line {
        private final String name_;
        private Line( String name ) {
            name_ = name;
        }
        public String toString() {
            return name_;
        }
    }
}
