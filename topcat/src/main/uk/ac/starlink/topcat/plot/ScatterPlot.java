package uk.ac.starlink.topcat.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which can display a scatter plot of points.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2004
 */
public class ScatterPlot extends SurfacePlot {

    private Annotations annotations_;
    private Points lastPoints_;
    private PlotState lastState_;
    private PlotSurface lastSurface_;
    private int lastWidth_;
    private int lastHeight_;
    private Image image_;
    private XYStats[] statSets_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructs a new scatter plot, specifying the initial plotting surface
     * which provides axis plotting and so on.
     *
     * @param  surface  plotting surface implementation
     */
    public ScatterPlot( PlotSurface surface ) {
        super();
        add( new ScatterDataPanel() );
        annotations_ = new Annotations();
        setSurface( surface );
    }
    
    public void setState( PlotState state ) {
        super.setState( state );
        annotations_.validate();
    }

    /**
     * Sets the points at the indices given by the <tt>ips</tt> array 
     * of the Points object as "active".
     * They will be marked out somehow or other when plotted.
     *
     * @param  ips  active point array
     */
    public void setActivePoints( int[] ips ) {
        annotations_.setActivePoints( ips );
    }

    /**
     * Works out the range of coordinates to accommodate all the data
     * points owned by this plot.
     *
     * @return   4-element array (xlo,ylo,xhi,yhi)
     */
    public double[] getFullDataRange() {
        boolean xlog = getState().getLogFlags()[ 0 ];
        boolean ylog = getState().getLogFlags()[ 1 ];
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        double ylo = Double.POSITIVE_INFINITY;
        double yhi = ylog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        /* Go through all points getting max/min values. */
        int nok = 0;
        Points points = getPoints();
        if ( points != null ) {
            RowSubset[] rsets = getPointSelection().getSubsets();
            int nrset = rsets.length;
            int np = points.getCount();
            double[] coords = new double[ 2 ];
            for ( int ip = 0; ip < np; ip++ ) {

                /* First see if this point will be plotted. */
                boolean use = false;
                long lp = (long) ip;
                for ( int is = 0; ! use && is < nrset; is++ ) {
                    use = use || rsets[ is ].isIncluded( lp );
                }
                if ( use ) {
                    points.getCoords( ip, coords );
                    double xp = coords[ 0 ];
                    double yp = coords[ 1 ];
                    if ( ! Double.isNaN( xp ) && 
                         ! Double.isNaN( yp ) &&
                         ! Double.isInfinite( xp ) && 
                         ! Double.isInfinite( yp ) &&
                         ( ! xlog || xp > 0.0 ) &&
                         ( ! ylog || yp > 0.0 ) ) {
                        nok++;
                        if ( xp < xlo ) {
                            xlo = xp;
                        }
                        if ( xp > xhi ) {
                            xhi = xp;
                        }
                        if ( yp < ylo ) {
                            ylo = yp;
                        }
                        if ( yp > yhi ) {
                            yhi = yp;
                        }
                    }
                }
            }
        }

        /* Return result. */
        return nok == 0 ? null : new double[] { xlo, ylo, xhi, yhi };
    }

    /**
     * Plots the points of this scatter plot onto a given graphics context
     * using its current plotting surface to define the mapping of data
     * to graphics space.
     *
     * @param  graphics  graphics context
     * @param  pixels    true if the graphics context is pixel-like, 
     *                   false if it's vector-like
     */
    private void drawData( Graphics graphics, boolean pixels ) {
        Points points = getPoints();
        PlotState state = getState();
        PlotSurface surface = getSurface();
        if ( points == null || state == null || surface == null ) {
            return;
        }

        /* Clone the graphics context and configure the clip to correspond
         * to the plotting surface. */
        Graphics2D g; 
        g = (Graphics2D) graphics.create();
        g.setClip( getSurface().getClip() );

        /* Get ready to plot. */
        int np = points.getCount();
        RowSubset[] sets = getPointSelection().getSubsets();
        Style[] styles = getPointSelection().getStyles();
        int nset = sets.length;

        /* Draw the points. */
        List setList = new ArrayList();
        List styleList = new ArrayList();
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) styles[ is ];
            if ( ! style.getHidePoints() ) {
                setList.add( sets[ is ] );
                styleList.add( style );
            }
        }
        RowSubset[] activeSets =
            (RowSubset[]) setList.toArray( new RowSubset[ 0 ] );
        MarkStyle[] activeStyles =
            (MarkStyle[]) styleList.toArray( new MarkStyle[ 0 ] );
        if ( pixels ) {
            plotPointsBitmap( g, points, activeSets, activeStyles, surface );
        }
        else {
            plotPointsVector( g, points, activeSets, activeStyles, surface );
        }

        /* Join the dots as required. */
        double[] coords = new double[ 2 ];
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) styles[ is ];
            if ( style.getLine() == MarkStyle.DOT_TO_DOT ) {
                RowSubset set = sets[ is ];
                Graphics2D lineGraphics = (Graphics2D) g.create();
                style.configureForLine( lineGraphics, BasicStroke.CAP_BUTT,
                                        BasicStroke.JOIN_MITER );
                int lastxp = 0;
                int lastyp = 0;
                boolean notFirst = false;
                for ( int ip = 0; ip < np; ip++ ) {
                    if ( set.isIncluded( (long) ip ) ) {
                        points.getCoords( ip, coords );
                        double x = coords[ 0 ];
                        double y = coords[ 1 ];
                        Point point = surface.dataToGraphics( x, y, false );
                        if ( point != null ) {
                            int xp = point.x;
                            int yp = point.y;
                            if ( notFirst ) {
                                lineGraphics.drawLine( lastxp, lastyp, xp, yp );
                            }
                            else {
                                notFirst = true;
                            }
                            lastxp = xp;
                            lastyp = yp;
                        }
                    }
                }
            }
        }

        /* Do linear regression as required. */
        statSets_ = new XYStats[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            RowSubset set = sets[ is ];
            MarkStyle style = (MarkStyle) styles[ is ];
            if ( style.getLine() == MarkStyle.LINEAR ) {

                /* Accumulate statistics. */
                XYStats stats = new XYStats( state.getLogFlags()[ 0 ],
                                             state.getLogFlags()[ 1 ] );
                statSets_[ is ] = stats;
                int maxr = style.getMaximumRadius();
                int maxr2 = maxr * 2;
                for ( int ip = 0; ip < np; ip++ ) {
                    if ( set.isIncluded( (long) ip ) ) {
                        points.getCoords( ip, coords );
                        double x = coords[ 0 ];
                        double y = coords[ 1 ];
                        Point point = surface.dataToGraphics( x, y, true );
                        if ( point != null ) {
                            int xp = point.x;
                            int yp = point.y;
                            if ( g.hitClip( xp - maxr, yp - maxr,
                                            maxr2, maxr2 ) ) {
                                stats.addPoint( x, y );
                            }
                        }
                    }
                }

                /* Draw regression line. */
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
                style.configureForLine( g2, BasicStroke.CAP_BUTT,
                                        BasicStroke.JOIN_MITER );
                double[] ends = stats.linearRegressionLine();
                if ( ends != null ) {
                    Point p1 = surface.dataToGraphics( ends[ 0 ], ends[ 1 ],
                                                       false );
                    Point p2 = surface.dataToGraphics( ends[ 2 ], ends[ 3 ],
                                                       false );
                    if ( p1 != null && p2 != null ) {
                        g2.drawLine( p1.x, p1.y, p2.x, p2.y );
                    }
                }
            }
        }
    }

    /**
     * Plots markers representing the data points using vector graphics
     * (graphics.draw* methods).
     *
     * @param   g   graphics context
     * @param   points  data points object
     * @param   sets   row subsets
     * @param   styles   array of MarkStyle objects corresponding to sets
     * @param   surface  plotting surface
     */
    private static void plotPointsVector( Graphics2D g, Points points,
                                          RowSubset[] sets, MarkStyle[] styles, 
                                          PlotSurface surface ) {
        int np = points.getCount();
        int nset = sets.length;
        double[] coords = new double[ 2 ];
        for ( int is = 0; is < nset; is++ ) {
            RowSubset set = sets[ is ];
            MarkStyle style = styles[ is ];
            int maxr = style.getMaximumRadius();
            int maxr2 = maxr * 2;
            for ( int ip = 0; ip < np; ip++ ) {
                if ( set.isIncluded( (long) ip ) ) {
                    points.getCoords( ip, coords );
                    double x = coords[ 0 ];
                    double y = coords[ 1 ];
                    Point point = surface.dataToGraphics( x, y, true );
                    if ( point != null ) {
                        int xp = point.x;
                        int yp = point.y;
                        if ( g.hitClip( xp - maxr, yp - maxr,
                                        maxr2, maxr2 ) ) {
                            style.drawMarker( g, xp, yp );
                        }
                    }
                }
            }
        }
    }

    /**
     * Plots markers representing the data points using bitmap type graphics.
     *
     * @param   g   graphics context
     * @param   points  data points object
     * @param   sets   row subsets
     * @param   styles   array of MarkStyle objects corresponding to sets
     * @param   surface  plotting surface
     */
    private static void plotPointsBitmap( Graphics2D g, Points points,
                                          RowSubset[] sets, MarkStyle[] styles,
                                          PlotSurface surface ) {
        int np = points.getCount();
        int nset = sets.length;

        /* Work out padding round the edge of the raster we will be drawing on.
         * This has to be big enough that we can draw markers on the edge
         * of the visible part and not have them wrap round.  In this
         * way we can avoid doing some of the edge checking. */
        int maxr = 0;
        for ( int is = 0; is < nset; is++ ) {
            maxr = Math.max( styles[ is ].getMaximumRadius(), maxr );
        }
        int pad = maxr * 2 + 1;

        /* Work out the dimensions of the raster and what offsets we need
         * to use to write to it. */
        Rectangle clip = surface.getClip().getBounds();
        int xdim = clip.width + 2 * pad;
        int ydim = clip.height + 2 * pad;
        int xoff = clip.x - pad;
        int yoff = clip.y - pad;
        int npix = xdim * ydim;

        /* Set up raster buffers and xy offsets for drawing markers 
         * for each of the row subsets we will be plotting. */
        int[][] buffers = new int[ nset ][];
        int[][] pixoffs = new int[ nset ][];
        int[] npixoffs = new int[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = styles[ is ];
            buffers[ is ] = new int[ npix ];
            int[] xypixoffs = style.getPixelOffsets();
            npixoffs[ is ] = xypixoffs.length / 2;
            pixoffs[ is ] = new int[ npixoffs[ is ] ];
            for ( int ioff = 0; ioff < npixoffs[ is ]; ioff++ ) {
                int xoffi = xypixoffs[ ioff * 2 + 0 ];
                int yoffi = xypixoffs[ ioff * 2 + 1 ];
                pixoffs[ is ][ ioff ] = xoffi + yoffi * xdim;
            }
        }

        /* For each point, if it's included in any subset, then increment
         * every element of its buffer which falls under the drawing of
         * that subset's marker.  This will give us, for each subset, 
         * a raster buffer which contains values indicating how many 
         * times each of its pixels has been painted. */
        BitSet mask = new BitSet( npix );
        double[] coords = new double[ 2 ];
        for ( int ip = 0; ip < np; ip++ ) {
            points.getCoords( ip, coords );
            double x = coords[ 0 ];
            double y = coords[ 1 ];
            Point point = surface.dataToGraphics( x, y, true );
            if ( point != null ) {
                int xp = point.x;
                int yp = point.y;
                int xbase = xp - xoff;
                int ybase = yp - yoff;
                if ( xbase > maxr && xbase < xdim - maxr &&
                     ybase > maxr && ybase < ydim - maxr ) {
                    int base = xbase + xdim * ybase;
                    for ( int is = 0; is < nset; is++ ) {
                        if ( sets[ is ].isIncluded( (long) ip ) ) {
                            for ( int ioff = 0; ioff < npixoffs[ is ];
                                  ioff++ ) {
                                int ipix = base + pixoffs[ is ][ ioff ];
                                buffers[ is ][ ipix ]++;
                                mask.set( ipix );
                            }
                        }
                    }
                }
            }
        }

        /* Now combine the rasters using the colours and opacities described
         * in the corresponding MarkStyle objects.  Each count of each
         * pixel is weighted by (the inverse of) its style's opacity limit.
         * When an opacity of 1 is reached, no further contributions are
         * made.  The resulting pixels have a (non-premultiplied) alpha 
         * channel which indicates whether the full opacity limit has
         * been reached or not. 
         * This algorithm, though somewhat ad-hoc, was chosen because it
         * reduces in the limit of an opacity limit of unity for all styles
         * to behaviour which is what you'd expect from normal opaque pixels:
         * an opaque pixel in front of any other pixel blocks it out. */
        float[] opacity = new float[ nset ];
        float[] rCols = new float[ nset ];
        float[] gCols = new float[ nset ];
        float[] bCols = new float[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = styles[ is ];
            opacity[ is ] = 1.0f / style.getOpaqueLimit();
            float[] rgb = style.getColor().getRGBColorComponents( null );
            rCols[ is ] = rgb[ 0 ];
            gCols[ is ] = rgb[ 1 ];
            bCols[ is ] = rgb[ 2 ];
        }
        BufferedImage im =
            new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );
        ColorModel colorModel = im.getColorModel();
        assert colorModel.equals( ColorModel.getRGBdefault() );
        assert ! colorModel.isAlphaPremultiplied();
        int[] rgbBuf = new int[ xdim * ydim ];

        /* By using the bit set for iteration we skip consideration of any
         * pixels which have never been touched, which may well be a large
         * majority of them.  This should be good for efficiency. */
        for ( int ipix = mask.nextSetBit( 0 ); ipix >= 0;
              ipix = mask.nextSetBit( ipix + 1 ) ) {
            float remain = 1.0f;
            float[] weights = new float[ nset ];
            for ( int is = nset - 1; is >= 0 && remain > 0.0; is-- ) {
                float weight = opacity[ is ] * buffers[ is ][ ipix ];
                weight = Math.min( remain, weight );
                weights[ is ] = weight;
                remain -= weight;
            }
            if ( remain < 1.0f ) {
                float totWeight = 1.0f - remain;
                float[] argb = new float[ 4 ];
                argb[ 3 ] = totWeight;
                for ( int is = 0; is < nset; is++ ) {
                    float weight = weights[ is ] / totWeight;
                    if ( weight > 0 ) {
                        argb[ 0 ] += weight * rCols[ is ];
                        argb[ 1 ] += weight * gCols[ is ];
                        argb[ 2 ] += weight * bCols[ is ];
                    }
                }
                rgbBuf[ ipix ] = colorModel.getDataElement( argb, 0 );
            }
        }

        /* Finally paint the constructed image onto the graphics context. */
        im.setRGB( 0, 0, xdim, ydim, rgbBuf, 0, xdim );    
        g.drawImage( im, xoff, yoff, null );
    }

    /**
     * Returns the X-Y statistics calculated the last time this component
     * was painted.
     *
     * @return  X-Y correlation statistics objects
     */
    public XYStats[] getCorrelations() {
        return statSets_;
    }

    /**
     * Plots the registered annotations of this scatter plot onto a given
     * graphics context using its current plotting surface to define the
     * mapping of data to graphics space.
     *
     * @param  g  graphics context
     */
    private void drawAnnotations( Graphics g ) {
        annotations_.draw( g );
    }

    /**
     * This class takes care of all the markings plotted over the top of
     * the plot proper.  It's coded as an extra class just to make it tidy,
     * these workings could equally be in the body of ScatterPlot.
     */
    private class Annotations {

        int[] activePoints_ = new int[ 0 ];
        final MarkStyle cursorStyle_ = MarkStyle.targetStyle();

        /**
         * Sets a number of points to be marked out.
         * Any negative indices in the array, or ones which are not visible
         * in the current plot, are ignored.
         *
         * @param  ips  indices of the points to be marked
         */
        void setActivePoints( int[] ips ) {
            ips = dropInvisible( ips );
            if ( ! Arrays.equals( ips, activePoints_ ) ) {
                activePoints_ = ips;
                repaint();
            }
        }

        /**
         * Paints all the current annotations onto a given graphics context.
         *
         * @param  g  graphics context
         */
        void draw( Graphics graphics ) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            /* Draw any active points. */
            for ( int i = 0; i < activePoints_.length; i++ ) {
                double[] coords = new double[ 2 ];
                getPoints().getCoords( activePoints_[ i ], coords );
                Point p = getSurface().dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                       true );
                if ( p != null ) {
                    cursorStyle_.drawMarker( graphics, p.x, p.y );
                }
            }
        }

        /**
         * Updates this annotations object as appropriate for the current
         * state of the plot.
         */
        void validate() {
            /* If there are active points which are no longer visible in
             * this plot, drop them. */
            activePoints_ = getState().getValid() 
                          ? dropInvisible( activePoints_ )
                          : new int[ 0 ];
        }

        /**
         * Removes any invisible points from an array of point indices.
         *
         * @param  ips   point index array
         * @return  subset of ips
         */
        private int[] dropInvisible( int[] ips ) {
            List ipList = new ArrayList();
            for ( int i = 0; i < ips.length; i++ ) {
                int ip = ips[ i ];
                if ( ip >= 0 && isIncluded( ip ) ) {
                    ipList.add( new Integer( ip ) );
                }
            }
            ips = new int[ ipList.size() ];
            for ( int i = 0; i < ips.length; i++ ) {
                ips[ i ] = ((Integer) ipList.get( i )).intValue();
            }
            return ips;
        }
    }

    /**
     * Determines whether a point with a given index is included in the
     * current plot.  This doesn't necessarily mean it's visible, since
     * it might fall outside the bounds of the current display area,
     * but it means the point does conceptually form part of what is
     * being plotted.
     * 
     * @param  ip  index of point to check
     * @return  true  iff point <tt>ip</tt> is included in this plot
     */
    private boolean isIncluded( int ip ) {
        RowSubset[] sets = getPointSelection().getSubsets();
        int nset = sets.length;
        for ( int is = 0; is < nset; is++ ) {
            if ( sets[ is ].isIncluded( (long) ip ) ) {
                return true;
            }
        } 
        return false;
    }

    /**
     * Graphical component which does the actual plotting of the points.
     * Its basic job is to call {@link @drawData} and {@link drawAnnotations}
     * in its {@link paintComponent} method.  
     * However it makes things a bit more
     * complicated than that for the purposes of efficiency.
     */
    private class ScatterDataPanel extends JComponent {

        ScatterDataPanel() {
            setOpaque( false );
        }

        /**
         * Draws the component to a graphics context which probably 
         * represents the screen, by calling {@link #drawData}
         * and {@link #drawAnnotations}.
         * Since <tt>drawData</tt> might be fairly expensive 
         * (if there are a lot of points), and <tt>paintComponent</tt> 
         * can be called often without the data changing 
         * (e.g. cascading window uncover events) it is advantageous 
         * to cache the drawn points in an Image and effectively
         * blit it to the screen on subsequent paint requests.
         */
        protected void paintComponent( Graphics g ) {
 
            /* If the plotting requirements have changed since last time
             * we painted, we can use the cached image. */
            int width = getBounds().width;
            int height = getBounds().height;
            if ( getState() == lastState_ &&
                 getSurface() == lastSurface_ &&
                 getPoints() == lastPoints_ &&
                 width == lastWidth_ &&
                 height == lastHeight_ ) {
                assert image_ != null;
            }

            /* We need to replot the points. */
            else {

                /* Get an image to plot into.  Take care that this is an
                 * image which can take advantage of hardware acceleration
                 * (e.g. "new BufferedImage()" is no good). */
                image_ = createImage( width, height );
                Graphics ig = image_.getGraphics();

                /* Unfortunately, accelerated-graphics-capable contexts
                 * are unable to handle transparency very well.  This means
                 * that the plotting surface will not show up behind the
                 * plotted points in the cached image buffer, even though
                 * this component is supposed to be transparent. 
                 * So we fudge it by calling the plotting surface's 
                 * <tt>paintSurface</tt> method (introduced for this purpose)
                 * here.  This is not incredibly tidy. */
                getSurface().paintSurface( ig );

                /* Plot the actual points into the cached buffer. */
                drawData( ig, true );

                /* Record the state which corresponds to the most recent
                 * plot into the cached buffer. */
                lastState_ = getState();
                lastSurface_ = getSurface();
                lastPoints_ = getPoints();
                lastWidth_ = width;
                lastHeight_ = height;
            }

            /* Copy the image from the (new or old) cached buffer onto
             * the graphics context we are being asked to paint into. */
            boolean done = g.drawImage( image_, 0, 0, null );
            assert done;
            drawAnnotations( g );
        }

        /**
         * The normal implementation for this method calls 
         * <tt>paintComponent</tt> somewhere along the way.
         * However, if we are painting the component 
         * for some reason other than posting it
         * on the screen, we don't want to use the cached-image approach
         * used in <tt>paintComponent</tt>, since it might be a 
         * non-pixelised graphics context (e.g. postscript).
         * So for <tt>printComponent</tt>, just invoke <tt>drawData</tt>
         * straightforwardly.
         *
         * <p>A more respectable way to do this might be to test the
         * <tt>Graphics2D.getDeviceConfiguration().getDevice().getType()</tt>
         * value in <tt>paintComponent</tt> - however, for at least one
         * known printer-type graphics context 
         * (<tt>org.jibble.epsgraphics.EpsGraphics2D</tt>) this returns
         * the wrong value (TYPE_IMAGE_BUFFER not TYPE_PRINTER).
         */
        protected void printComponent( Graphics g ) {
            if ( getPoints() != null && getState() != null ) {

            // this doesn't work properly - the background is black
            //  /* If there are any transparent styles involved, we will
            //   * need to plot using pixels, since printer graphics contexts
            //   * usually can't handle transparency 
            //   * (well, PostScript can't). */
            //  boolean usePixels = false;
            //  Style[] styles = getPointSelection().getStyles();
            //  for ( int is = 0; is < styles.length; is++ ) {
            //      if ( ((MarkStyle) styles[ is ]).getOpaqueLimit() != 1 ) {
            //          usePixels = true;
            //      }
            //  }
            //  if ( usePixels = true ) {
            //      logger_.warning( "Using bitmapped postscript output, " +
            //                       "necessary to retain pixel transparency" );
            //  }

                drawData( g, usePixels );
                drawAnnotations( g );
            }
        }
    }

}
