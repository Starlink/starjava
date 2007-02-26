package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import javax.swing.Icon;
import uk.ac.starlink.topcat.EmptyIcon;

/**
 * Renders error bars.
 * 
 * @author   Mark Taylor
 * @since    20 Feb 2007
 */
public abstract class ErrorRenderer {

    /** Error renderer which draws nothing. */
    public static ErrorRenderer NONE = new Blank();

    /** General purpose error renderer. */
    public static ErrorRenderer DEFAULT = new CappedLine( true, 0 );

    /** Error renderer suitable for use in user controls. */
    public static ErrorRenderer EXAMPLE = new CappedLine( true, 3 );

    private static final ErrorRenderer[] OPTIONS_2D = new ErrorRenderer[] {
        NONE,
        DEFAULT,
        new CappedLine( true, 3 ),
        new CappedLine( false, 3 ),
        new OpenEllipse(),
        new OpenRectangle(),
        new FilledEllipse(),
        new FilledRectangle(),
    };

    private static ErrorRenderer[] OPTIONS_GENERAL = new ErrorRenderer[] {
        NONE,
        DEFAULT,
        new CappedLine( true, 3 ),
        new CappedLine( false, 3 ),
    };

    private static final int LEGEND_WIDTH = 40;
    private static final int LEGEND_HEIGHT = 16;
    private static final int LEGEND_XPAD = 5;
    private static final int LEGEND_YPAD = 1;

    /**
     * Returns an icon giving a general example of what this form looks like.
     *
     * @return  example icon
     */
    public abstract Icon getLegendIcon();

    /**
     * Returns an icon giving an example of what this form looks like in a
     * detailed context.
     *
     * @param   modes  array of ErrorModes, one per error dimension (x, y, ...)
     * @param   width  total width of icon
     * @param   height total height of icon
     * @param   xpad   internal horizontal padding of icon
     * @param   ypad   internal vertical padding of icon
     */
    public abstract Icon getLegendIcon( ErrorMode[] modes, int width,
                                        int height, int xpad, int ypad );

    /**
     * Indicates whether this renderer is known to produce no output 
     * for a particular set of ErrorModes.
     *
     * @param   modes   error mode context
     * @return  true if this renderer can be guaranteed to paint nothing
     */
    public abstract boolean isBlank( ErrorMode[] modes );

    /**
     * Reports whether this form can be used on a given error dimensionality.
     *
     * @param  ndim  number of error dimensions to be used
     * @return  true iff this object can do rendering
     */
    public abstract boolean supportsDimensionality( int ndim );

    /**
     * Renders error bars in one or more dimensions around a given point.
     * The positions of the ends of error bars relative to the the point
     * are given.  There may in general be (2*N) of these, though certain
     * <code>ErrorRenderer</code> implementations may impose restrictions
     * on this count.  They must come in consecutive pairs which describe
     * error bars along the same axis in different directions.
     * Missing error bars are represented as (0,0).  The values must come
     * in axis order where that makes sense, but note in some contexts
     * (e.g. 3D) these may be data axes rather than graphics plane axes.
     *
     * @param  g  graphics context
     * @param  x  data point X coordinate
     * @param  y  data point Y coordinate
     * @param  xoffs  X coordinates of error bar limit offsets from (x,y)
     * @param  yoffs  Y coordinates of error bar limit offsets from (x,y)
     */
    public abstract void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                     int[] yoffs );

    /**
     * Returns an array of ErrorRenderers which can render 2-dimensional errors.
     *
     * @return  selection of renderers
     */
    public static ErrorRenderer[] getOptions2d() {
        return (ErrorRenderer[]) OPTIONS_2D.clone();
    }

    /**
     * Returns an array of ErrorRenderers which can render errors of arbitrary
     * dimensionality.
     *
     * @return  selection of renderers
     */
    public static ErrorRenderer[] getOptionsGeneral() {
        return (ErrorRenderer[]) OPTIONS_GENERAL.clone();
    }

    /**
     * Icon which represents an ErrorRenderer in a form suitable for a legend.
     */
    private static class ErrorRendererIcon implements Icon {

        private final ErrorRenderer renderer_;
        private final int width_;
        private final int height_;
        private final int[] xoffs_;
        private final int[] yoffs_;

        /**
         * Constructs an icon with default characteristics.
         *
         * @param  renderer  error renderer
         * @param  ndim    dimensionality
         */
        public ErrorRendererIcon( ErrorRenderer renderer, int ndim ) {
            this( renderer, fillModeArray( ndim, ErrorMode.SYMMETRIC ),
                  LEGEND_WIDTH, LEGEND_HEIGHT, LEGEND_XPAD, LEGEND_YPAD );
        }

        /**
         * Constructs an icon with specified characteristics.
         *
         * @param  renderer  error renderer
         * @param   modes  array of ErrorModes, one per error dimension
         * @param   width  total width of icon
         * @param   height total height of icon
         * @param   xpad   internal horizontal padding of icon
         * @param   ypad   internal vertical padding of icon
         */
        public ErrorRendererIcon( ErrorRenderer renderer, ErrorMode[] modes,
                                  int width, int height, int xpad, int ypad ) {
            renderer_ = renderer;
            width_ = width;
            height_ = height;
            int w2 = width / 2 - xpad;
            int h2 = height / 2 - ypad;
            int ndim = modes.length;
            xoffs_ = new int[ ndim * 2 ];
            yoffs_ = new int[ ndim * 2 ];
            if ( ndim > 0 ) {
                ErrorMode mode = modes[ 0 ];
                xoffs_[ 0 ] = (int) Math.round( - mode.getExampleLower() * w2 );
                yoffs_[ 0 ] = 0;
                xoffs_[ 1 ] = (int) Math.round( + mode.getExampleUpper() * w2 );
                yoffs_[ 1 ] = 0;
            }
            if ( ndim > 1 ) {
                ErrorMode mode = modes[ 1 ];
                xoffs_[ 2 ] = 0;
                yoffs_[ 2 ] = (int) Math.round( + mode.getExampleLower() * h2 );
                xoffs_[ 3 ] = 0;
                yoffs_[ 3 ] = (int) Math.round( - mode.getExampleUpper() * h2 );
            }
            if ( ndim > 2 ) {
                ErrorMode mode = modes[ 2 ];
                double theta = Math.toRadians( 40 );
                double slant = 0.8;
                double c = Math.cos( theta ) * slant;
                double s = Math.sin( theta ) * slant;
                double lo = mode.getExampleLower();
                double hi = mode.getExampleUpper();
                xoffs_[ 4 ] = (int) Math.round( - c * lo * w2 );
                yoffs_[ 4 ] = (int) Math.round( + s * lo * h2 );
                xoffs_[ 5 ] = (int) Math.round( + c * hi * w2 );
                yoffs_[ 5 ] = (int) Math.round( - s * hi * h2 );
            }
        }

        public int getIconWidth() {
            return width_;
        }

        public int getIconHeight() {
            return height_;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON );
            renderer_.drawErrors( g2, x + width_ / 2, y + height_ / 2,
                                  xoffs_, yoffs_ );
            g2.dispose();
        }

        /**
         * Utility method to return an array containing all the same modes.
         *
         * @param  leng  number of elements
         * @param  mode  content of each element
         * @return array filled with <code>mode</code>
         */
        private static ErrorMode[] fillModeArray( int leng, ErrorMode mode ) {
            ErrorMode[] modes = new ErrorMode[ leng ];
            Arrays.fill( modes, mode );
            return modes;
        }
    }

    /**
     * ErrorRenderer which renders an (optional) line from the data point
     * to the given offset, and an (optional) cap normal to that line
     * at its furthest extent.  Works for any dimensionality.
     */
    private static class CappedLine extends ErrorRenderer {

        private final boolean lines_;
        private final int capsize_;
        private final Icon legend_;

        /**
         * Constructor.
         *
         * @param  lines   true iff you want error lines drawn
         * @param  capsize  the number of pixels in each direction the
         *                  cap should extend; zero means no cap
         */
        CappedLine( boolean lines, int capsize ) {
            lines_ = lines;
            capsize_ = capsize;
            legend_ = new ErrorRendererIcon( this, 2 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new ErrorRendererIcon( this, modes, width, height,
                                          xpad, ypad );
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return ErrorMode.allBlank( modes );
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
            int np = xoffs.length;
            for ( int ip = 0; ip < np; ip++ ) {
                int xoff = xoffs[ ip ];
                int yoff = yoffs[ ip ];

                /* Only do drawing for a non-blank line. */
                if ( xoff != 0 || yoff != 0 ) {

                    /* Draw line if required. */
                    if ( lines_ ) {
                        g.drawLine( x, y, x + xoffs[ ip ], y + yoffs[ ip ] );
                    }

                    /* Draw bar if required. */
                    if ( capsize_ > 0 ) {

                        /* For rectilinear offsets, draw the cap manually. */
                        if ( xoff == 0 ) {
                            g.drawLine( x - capsize_, y + yoff,
                                        x + capsize_, y + yoff );
                        }
                        else if ( yoff == 0 ) {
                            g.drawLine( x + xoff, y - capsize_,
                                        x + xoff, y + capsize_ );
                        }

                        /* For more general offsets, transform the graphics
                         * context so that we can draw the cap along an axis.
                         * This is better than calculating the position in 
                         * the original orientation because that would require
                         * integer rounding (at least in antialiased contexts
                         * the difference may be visible). */
                        else {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.translate( x, y );
                            g2.rotate( Math.atan2( yoff, xoff ) );
                            double l2 = xoff * xoff + yoff * yoff;
                            int leng = (int) Math.round( Math.sqrt( l2 ) );
                            g2.drawLine( leng, - capsize_, leng, capsize_ );
                            g2.dispose();
                        }
                    }
                }
            }
        }
    }

    /**
     * Generic abstract renderer for cases in which the rendered object is
     * effectively a quadrilateral of some sort.  Concrete subclasses
     * must implement {@link #drawOblong} to mark the space as appropriate.
     * Only works properly for two-dimensional errors.
     */
    private static abstract class Oblong extends ErrorRenderer {

        private final Icon legend_;

        /**
         * Constructor.
         */
        Oblong() {
            legend_ = new ErrorRendererIcon( this, 2 );
        }

        public boolean supportsDimensionality( int ndim ) {
            return ndim == 2;
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new ErrorRendererIcon( this, modes, width, height,
                                          xpad, ypad );
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return ErrorMode.allBlank( modes );
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs, 
                                int[] yoffs ) {

            /* If there are only 1-dimensional bounds, just draw a line. */
            if ( xoffs.length == 2 ) {
                g.drawLine( x + xoffs[ 0 ], y + yoffs[ 0 ],
                            x + xoffs[ 1 ], y + yoffs[ 1 ] );
            }

            /* Otherwise we better have two dimensions. */
            else if ( xoffs.length != 4 || ! ( g instanceof Graphics2D ) ) {
                return;
            }

            /* If the X and Y offsets are aligned along X and Y axes we
             * can do it easily. */
            else if ( yoffs[ 0 ] == 0 && yoffs[ 1 ] == 0 &&
                      xoffs[ 2 ] == 0 && xoffs[ 2 ] == 0 ) {
                int xlo = Math.min( xoffs[ 0 ], xoffs[ 1 ] );
                int xhi = Math.max( xoffs[ 0 ], xoffs[ 1 ] );
                int ylo = Math.min( yoffs[ 2 ], yoffs[ 3 ] );
                int yhi = Math.max( yoffs[ 2 ], yoffs[ 3 ] );
                int width = xhi - xlo;
                int height = yhi - ylo;
                if ( width > 0 || height > 0 ) {
                    drawOblong( g, x + xlo, y + ylo, width, height );
                }
            }

            /* Otherwise transform the space so that the error bounds are
             * contained in a rectangle aligned along the axes. */
            else {
                double dx1 = xoffs[ 1 ] - xoffs[ 0 ];
                double dy1 = yoffs[ 1 ] - yoffs[ 0 ];
                double dx2 = xoffs[ 3 ] - xoffs[ 2 ];
                double dy2 = yoffs[ 3 ] - yoffs[ 2 ];
                double width = Math.sqrt( dx1 * dx1 + dy1 * dy1 );
                double height = Math.sqrt( dx2 * dx2 + dy2 * dy2 );
                double[] m1 = new double[] {
                    width, 0,      0,
                    0,     height, 0,
                    1,     1,      1,
                };
                int[] xo = xoffs;
                int[] yo = yoffs;
                double[] m2 = new double[] {
                    x + xo[1] + xo[2], x + xo[0] + xo[3], x + xo[0] + xo[2],
                    y + yo[1] + yo[2], y + yo[0] + yo[3], y + yo[0] + yo[2],
                    1,                 1,                 1,
                };
                double[] m3 = Matrices.mmMult( m2, Matrices.invert( m1 ) );
                AffineTransform trans = new AffineTransform( m3[ 0 ], m3[ 3 ],
                                                             m3[ 1 ], m3[ 4 ],
                                                             m3[ 2 ], m3[ 5 ] );
                Graphics2D g2 = (Graphics2D) g.create();
                g2.transform( trans );
                drawOblong( g2, 0, 0, (int) Math.round( width ),
                            (int) Math.round( height ) );
                g2.dispose();
            }
        }

        /**
         * Does the actual drawing of the error region.
         * The region covers the range (x..x+width, y..y+height).
         *
         * @param   g  graphics context
         * @param   x  X coordinate of origin
         * @param   y  Y coordinate of origin
         * @param   width   X extent of region
         * @param   height  Y extent of region
         */
        protected abstract void drawOblong( Graphics g, int x, int y,
                                            int width, int height );
    }

    /**
     * Oblong using an open ellipse.
     */
    private static class OpenEllipse extends Oblong {
        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.drawOval( x, y, width, height );
        }
    }

    /**
     * Oblong using a filled ellipse.
     */
    private static class FilledEllipse extends Oblong {
        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.fillOval( x, y, width, height );
        }
    }

    /**
     * Oblong using an open rectangle.
     */
    private static class OpenRectangle extends Oblong {
        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.drawRect( x, y, width, height );
        }
    }

    /**
     * Oblong using a filled rectangle.
     */
    private static class FilledRectangle extends Oblong {
        protected void drawOblong( Graphics g, int x, int y,
                                   int width, int height ) {
            g.fillRect( x, y, width, height );
        }
    }

    /**
     * Null error renderer.  It draws nothing.
     */
    private static class Blank extends ErrorRenderer {

        private final Icon legend_;

        Blank() {
            legend_ = new EmptyIcon( 0, 0 );
        }

        public Icon getLegendIcon() {
            return legend_;
        }

        public Icon getLegendIcon( ErrorMode[] modes, int width, int height,
                                   int xpad, int ypad ) {
            return new EmptyIcon( width, height );
        }

        public boolean supportsDimensionality( int ndim ) {
            return true;
        }

        public boolean isBlank( ErrorMode[] modes ) {
            return true;
        }

        public void drawErrors( Graphics g, int x, int y, int[] xoffs,
                                int[] yoffs ) {
        }
    }
}
