package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Draws lines composed of a sequence of points onto a 3d plotting surface.
 * Points are submitted one at a time.
 *
 * <p>There is no true 3d line plotting primitive available for a CubeSurface,
 * so we have to do it by hand.  To get the Z-stacking right it is necessary
 * to plot non-tiny lines using a number of pixels or segments, each placed 
 * at the relevant Z coordinate (depth).  This is not done very accurately,
 * but it's hopefully good enough to look OK.  It may also not be very
 * efficient if there are lots of long lines criss-crossing the visible cube,
 * but in that case even if it was done fast it would almost certainly be
 * impossible to see anything useful.
 *
 * <p>Line segments may still be visible if the points they join are outside
 * the visible cube.  In general therefore non-visible points should still
 * be submitted to this tracer for drawing.  The code attempts to deal
 * with them efficiently, avoiding painting where it is not required,
 * and coping with points that are far from the visible region.
 *
 * @author   Mark Taylor
 * @since    19 Jul 2018
 */
public abstract class LineTracer3D {

    final PaperType3D paperType_;
    final Paper paper_;
    final CubeSurface surf_;
    private NPoint lastPoint_;

    private static final double LIMIT = 100;

    /**
     * Constructor.
     *
     * @param   paperType  paper type
     * @param   paper      paper
     * @param   surf       target surface
     */
    protected LineTracer3D( PaperType3D paperType, Paper paper,
                            CubeSurface surf ) {
        paperType_ = paperType;
        paper_ = paper;
        surf_ = surf;
    }

    /**
     * Submits a point for drawing.  Except for the first invocation,
     * this will notionally result in drawing a line segment to the previous
     * point.
     *
     * @param  dpos  3-element array giving the vertex position
     *               in data coordinates
     * @param  color   colour of line segment
     */
    public void addPoint( double[] dpos, Color color ) {

        /* Prepare a normalised point corresponding to the supplied data point.
         * This is in normalised coordinates, and limited so that the
         * most distant point is far, but not too far, away.
         * Lines drawn to such limitedly-distant points will appear to
         * point in the right direction, but not cause an unacceptable
         * computational load. */
        double sx = limit( surf_.normalise( dpos, 0 ) );
        double sy = limit( surf_.normalise( dpos, 1 ) );
        double sz = limit( surf_.normalise( dpos, 2 ) );
        NPoint point = new NPoint( sx, sy, sz, color, surf_ );
        if ( lastPoint_ != null ) {
            traceLine( lastPoint_, point );
        }
        lastPoint_ = point;
    }

    /**
     * Does the work for marking a line segment between two points
     * on this tracer's surface.
     *
     * @param  np0  start point
     * @param  np1  end point
     */
    abstract void traceLine( NPoint np0, NPoint np1 );

    /**
     * Creats a line segment tracer from a given stroke.
     *
     * @param   paperType  paper type
     * @param   paper      paper
     * @param   surf       target surface
     * @param   stroke    line stroke - probably only line width is honoured
     * @return  new tracer
     */
    public static LineTracer3D createTracer( PaperType3D paperType, Paper paper,
                                             CubeSurface surf, Stroke stroke ) {
        return new SegmentTracer( paperType, paper, surf, stroke );
    }

    /**
     * Creates a line tracer from a given line thickness and pixel gap.
     * If pixgap is non-zero, a tracer that draws points rather than line
     * segments is returned.  At present, the points are not very cleverly
     * separated, so it doesn't look that good.
     *
     * @param   paperType  paper type
     * @param   paper      paper
     * @param   surf       target surface
     * @param   thickness  line thickness/marker size (minimum 1)
     * @param   pixgap     nominal gap in pixels between markers
     * @return  new tracer
     */
    public static LineTracer3D createTracer( PaperType3D paperType, Paper paper,
                                             CubeSurface surf, int thickness,
                                             int pixgap ) {
        return pixgap == 0
             ? new SegmentTracer( paperType, paper, surf,
                                  new BasicStroke( thickness,
                                                   BasicStroke.CAP_ROUND,
                                                   BasicStroke.JOIN_ROUND ) )
             : new MarkTracer( paperType, paper, surf,
                               MarkForm.createMarkGlyph( MarkerShape
                                                        .FILLED_CIRCLE,
                                                         thickness - 1, true ),
                               pixgap );
    }

    /**
     * LineTracer3D implementation that places spaced markers.
     * Could be improved.
     */
    private static class MarkTracer extends LineTracer3D {

        private final Glyph glyph_;
        private final int pixgap_;

        /**
         * Constructor.
         *
         * @param   paperType  paper type
         * @param   paper      paper
         * @param   surf       target surface
         * @param   glyph     glyph to paint at each point
         * @param   pixgap     nominal gap in pixels between markers
         */
        public MarkTracer( PaperType3D paperType, Paper paper,
                           CubeSurface surf, Glyph glyph, int pixgap ) {
            super( paperType, paper, surf );
            glyph_ = glyph;
            pixgap_ = pixgap;
        }

        void traceLine( NPoint n0, NPoint n1 ) {
            if ( n0.isSegmentVisible( n1 ) ) {
                GPoint3D g0 = n0.getGraphicsPoint();
                GPoint3D g1 = n1.getGraphicsPoint();
                double dx = g1.x - g0.x;
                double dy = g1.y - g0.y;
                double dz = g1.z - g0.z;
                double dist = Math.hypot( dx, dy );
                int np = (int) Math.ceil( dist / pixgap_ );
                for ( int ip = 0; ip < np; ip++ ) {
                    double f = ip / (double) np;
                    Color color = scaleColor( n0.color_, n1.color_, f );
                    paperType_.placeGlyph( paper_,
                                           g0.x + dx * f,
                                           g0.y + dy * f,
                                           g0.z + dz * f,
                                           glyph_, color );
                }
            }
        }

        /**
         * Interpolates a colour between two given ones.
         *
         * @param  c1  first colour
         * @param  c2  second colour
         * @param   f  fractional scale between c1 and c2 (range 0..1)
         * @return  interpolated colour
         */
        private static Color scaleColor( Color c1, Color c2, double f ) {
            if ( c1 == null || c2 == null ) {
                return null;
            }
            else if ( c1.equals( c2 ) ) {
                return c1;
            }
            else {
                int r1 = c1.getRed();
                int g1 = c1.getGreen();
                int b1 = c1.getBlue();
                int r2 = c2.getRed();
                int g2 = c2.getGreen();
                int b2 = c2.getBlue();
                return new Color( r1 + (int) ( f * ( r2 - r1 ) ),
                                  g1 + (int) ( f * ( g2 - g1 ) ),
                                  b1 + (int) ( f * ( b2 - b1 ) ) );
            }
        }
    }

    /**
     * LineTracer3D implementation that places line segments.
     * Slightly surprisingly, this seems to work OK in both bitmapped and
     * vector contexts.
     */
    private static class SegmentTracer extends LineTracer3D {

        private final LineGlyphFactory liner_;

        private static final int SEGMAX = 5;
        private static final int SEGMAX2 = SEGMAX * SEGMAX;

        /**
         * Constructor.
         *
         * @param  paperType  paper type
         * @param  paper     paper
         * @param  surf   3d plotting surface
         * @param  stroke   line drawing stroke
         */
        public SegmentTracer( PaperType3D paperType, Paper paper,
                              CubeSurface surf, final Stroke stroke ) {
            super( paperType, paper, surf );
            final XYShape lineShape = stroke.equals( LineXYShape.STROKE )
                                    ? LineXYShape.INSTANCE
                                    : new StrokeXYShape( stroke );

            /* Prepare to generate glyphs in a different way for bitmapped and
             * vector contexts.  The bitmapped way is more efficient because
             * the same Glyph objects can be reused many times, which for
             * some rendering contexts (SortedPaperType3D) can dramatically
             * reduce the memory required.  But if rendering to a vector context
             * the quantisation of positions onto the pixel grid makes the lines
             * come out very jagged, so handle that with a glyphs that preserve
             * the precision of the line segment positioning more faithfully. */
            liner_ = paperType.isBitmap()
                   ? new LineGlyphFactory() {
                         public Glyph getLineGlyph( double kx, double ky ) {
                             return lineShape
                                   .getGlyph( (short) Math.round( kx ),
                                              (short) Math.round( ky ) );
                         }
                     }
                   : new LineGlyphFactory() {
                         public Glyph getLineGlyph( double kx, double ky ) {
                             return new DoubleLineGlyph( kx, ky, lineShape,
                                                         stroke );
                         }
                     };
        }

        /**
         * Paints a line, if visible, between two normalised points.
         *
         * @param  n0  start point
         * @param  n1  end point
         * @param  color   colour of line segment
         */
        void traceLine( NPoint n0, NPoint n1 ) {
            if ( n0.isSegmentVisible( n1 ) ) {
                GPoint3D g0 = n0.getGraphicsPoint();
                GPoint3D g1 = n1.getGraphicsPoint();

                /* If the points are in the same graphics pixel, either draw
                 * a point if the colour has changed and the latest one is
                 * closer, or don't draw anything. */
                if ( coincides( g0, g1 ) ) {
                    if ( g1.z < g0.z && ! n0.color_.equals( n1.color_ ) ) {
                        paperType_.placeGlyph( paper_, g0.x, g0.y, g1.z,
                                               liner_.getLineGlyph( 0, 0 ),
                                               n1.color_ );
                    }
                }
                else {
                    double kx = g1.x - g0.x;
                    double ky = g1.y - g0.y;

                    /* If the points are quite close together, draw a small
                     * line segment to join them. */
                    if ( kx * kx + ky * ky < SEGMAX2 ) {
                        Glyph glyph = liner_.getLineGlyph( kx, ky );
                        paperType_.placeGlyph( paper_,
                                               g0.x, g0.y,
                                               0.5 * ( g0.z + g1.z ), glyph,
                                               bisect( n0.color_, n1.color_ ) );
                    }

                    /* Otherwise, split the line up into two and recurse. */
                    else {
                        NPoint n2 = bisect( n0, n1 );
                        traceLine( n0, n2 );
                        traceLine( n2, n1 );
                    }
                }
            }
        }

        /**
         * Finds the NPoint half way between two given NPoints.
         * The metric in use corresponds to difference on the 2d graphics plane.
         *
         * @param  n1  first point
         * @param  n2  second point
         * @return  point midway between n1 and n2
         */
        private NPoint bisect( NPoint n1, NPoint n2 ) {
            return new NPoint( 0.5 * ( n1.sx_ + n2.sx_ ),
                               0.5 * ( n1.sy_ + n2.sy_ ),
                               0.5 * ( n1.sz_ + n2.sz_ ),
                               bisect( n1.color_, n2.color_ ), surf_ );
        }

        /**
         * Finds the colour half way between two colours.
         *
         * @param   c1  first colour
         * @param   c2  second colour
         * @return   mean colour
         */
        private Color bisect( Color c1, Color c2 ) {
            if ( c1 == null || c2 == null ) {
                return null;
            }
            else if ( c1.equals( c2 ) ) {
                return c1;
            }
            else {
                return new Color( ( c1.getRed() + c2.getRed() ) >> 1,
                                  ( c1.getGreen() + c2.getGreen() ) >> 1, 
                                  ( c1.getBlue() + c2.getBlue() ) >> 1 );
            }
        }
    }

    /**
     * Returns the input value, limited to +/-LIMIT.
     *
     * @param  input value
     * @return  input value, or -LIMIT if it's smaller than that,
     *                       or +LIMIT if it's larger than that
     */
    private static double limit( double s ) {
        return Math.max( -LIMIT, Math.min( +LIMIT, s ) );
    }

    /**
     * Indicates whether two points correspond to the same graphics pixel.
     *
     * @param  p1  first point
     * @param  p2  second point
     * @return  true iff both points map to the same screen pixel
     */
    private static boolean coincides( GPoint3D p1, GPoint3D p2 ) {
        return toInt( p1.x ) == toInt( p2.x )
            && toInt( p1.y ) == toInt( p2.y );
    }

    /**
     * Maps a floating point graphics coordinate to an integer one
     * for the purposes of coincidence testing.
     *
     * @param  d  graphics position
     * @return  grahpics pixel index
     */
    private static int toInt( double d ) {
        return PlotUtil.ifloor( d );
    }

    /**
     * Represents a point in normalised graphics space.
     * In normalised coordinates, the visible cube covers the range
     * (-1,1) in X/Y/Z.
     */
    private static class NPoint {
        final double sx_;
        final double sy_;
        final double sz_;
        final Color color_;
        final CubeSurface surf_;
        final int regionX_;
        final int regionY_;
        final int regionZ_;
        GPoint3D gpoint_;

        /**
         * Constructor.
         *
         * @param   sx  normalised X coordinate
         * @param   sy  normalised Y coordinate
         * @param   sz  normalised Z coordinate
         * @param   color  colour
         */
        NPoint( double sx, double sy, double sz, Color color,
                CubeSurface surf ) {
            sx_ = sx;
            sy_ = sy;
            sz_ = sz;
            color_ = color;
            surf_ = surf;
            regionX_ = getRegion( sx_ );
            regionY_ = getRegion( sy_ );
            regionZ_ = getRegion( sz_ );
        }

        /**
         * Indicates whether any part of the line between this point and
         * another may pass through the visible cube.
         *
         * @param  other  another point
         * @return   false if the line segment is known to fall entirely
         *           outside the visible cube; true otherwise
         */
        boolean isSegmentVisible( NPoint other ) {

            /* This is fast and effective for excluding many segements,
             * but it doesn't exclude all invisible ones.
             * There is probably a better algorithm. */
            return this.regionX_ * other.regionX_ != 1
                && this.regionY_ * other.regionY_ != 1
                && this.regionZ_ * other.regionZ_ != 1;
        }

        /**
         * Returns a 3d graphics point correspoinding to this normalised point.
         *
         * @return  graphics point
         */
        GPoint3D getGraphicsPoint() {
            if ( gpoint_ == null ) {
                gpoint_ = new GPoint3D();
                surf_.normalisedToGraphicZ( sx_, sy_, sz_, gpoint_ );
            }
            return gpoint_;
        }

        /**     
         * Returns the <em>region</em> of a point with respect to
         * the interval (-1,+1).
         * The return value is -1, 0, or 1 according to whether the point
         * is lower than, within, or higher than the interval bounds.
         *      
         * @param   s   test value
         * @return  region code
         */         
        private int getRegion( double s ) {
            return s >= -1 ? ( s < 1 ? 0 : +1 ) : -1;
        }
    }

    /**
     * Defines line glyph construction.
     */
    private interface LineGlyphFactory {

        /**
         * Return a glyph representing a line segment from the origin to
         * a given graphics offset.
         *
         * @param  kx  X offset
         * @param  ky  Y offset
         * @return   line glyph
         */
        Glyph getLineGlyph( double kx, double ky );
    }

    /**
     * Glyph representing a line from the origin to a given (x,y) offset,
     * preserving offset position to reasonable accuracy.
     */
    private static class DoubleLineGlyph implements Glyph {
        private final double kx_;
        private final double ky_;
        private final XYShape shape_;
        private final Stroke stroke_;

        /**
         * Constructor.
         *
         * @param  kx  X offset in pixels
         * @param  ky  Y offset in pixels
         * @param  shape   drawing shape
         * @param  stroke  drawing stroke
         */
        public DoubleLineGlyph( double kx, double ky, XYShape shape,
                                Stroke stroke ) {
            kx_ = kx;
            ky_ = ky;
            shape_ = shape;
            stroke_ = stroke;
        }

        public Pixer createPixer( Rectangle clip ) {
            return shape_.getGlyph( (short) kx_, (short) ky_ )
                         .createPixer( clip );
        }

        public void paintGlyph( Graphics g ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            g2.setStroke( stroke_ );
            g2.draw( new Line2D.Double( 0, 0, kx_, ky_ ) );
            g2.setStroke( stroke0 );
        }
    }
}
