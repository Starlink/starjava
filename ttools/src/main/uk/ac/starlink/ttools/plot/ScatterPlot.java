package uk.ac.starlink.ttools.plot;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Logger;
import javax.swing.JComponent;
import uk.ac.starlink.util.IntList;

/**
 * Component which can display a scatter plot of points.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2004
 */
public class ScatterPlot extends SurfacePlot {

    private PlotData lastData_;
    private PlotState lastState_;
    private PlotSurface lastSurface_;
    private int lastWidth_;
    private int lastHeight_;
    private Image image_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot" );

    /**
     * Constructs a new scatter plot, specifying the initial plotting surface
     * which provides axis plotting and so on.
     *
     * @param  surface  plotting surface implementation
     */
    @SuppressWarnings("this-escape")
    public ScatterPlot( PlotSurface surface ) {
        super();
        add( new ScatterDataPanel() );
        setSurface( surface );
    }
    
    public void setState( PlotState state ) {
        super.setState( state );
    }

    /**
     * Plots the points of this scatter plot onto a given graphics context
     * using its current plotting surface to define the mapping of data
     * to graphics space.
     * The <code>pixels</code> parameter determines how the data are drawn.
     * Note that in order to render transparency, it is necessary to
     * set <code>pixels=true</code>.
     *
     * @param  graphics  graphics context
     * @param  pixels    true for pixel-like drawing, false for vector-like
     */
    private void drawData( Graphics graphics, boolean pixels ) {
        PlotState state = getState();
        if ( state == null || ! state.getValid() ) {
            return;
        }
        PlotData data = state.getPlotData();
        PlotSurface surface = getSurface();
        if ( data == null || surface == null ||
             surface.getClip().getBounds().isEmpty() ) {
            return;
        }

        /* Clone the graphics context and configure the clip to correspond
         * to the plotting surface. */
        Graphics2D g; 
        g = (Graphics2D) graphics.create();
        g.setClip( getSurface().getClip() );

        /* Get ready to plot. */
        int nset = data.getSetCount();

        /* Draw the points. */
        IntList indexList = new IntList();
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) data.getSetStyle( is );
            if ( ( ! style.getHidePoints() )
                 || MarkStyle.hasErrors( style, data ) ) {
                indexList.add( is );
            }
        }
        int[] activeIsets = indexList.toIntArray();
        PlotData activeData = new SubsetSelectionPlotData( data, activeIsets );
        int[] pointCounts = new int[ 3 ];
        DataColorTweaker tweaker = ShaderTweaker.createTweaker( 2, state );

        /* Do the actual plotting of points.  Different algorithms are 
         * required according to the details of how it needs to be plotted. */
        if ( pixels ) {
            if ( tweaker == null ) {
                plotPointsBitmap( g, activeData, surface, pointCounts );
            }
            else {
                plotPointsTweakedBitmap( g, activeData, surface, tweaker,
                                         pointCounts );
            }
        }
        else {
            plotPointsVector( g, activeData, surface, tweaker, pointCounts );
        }

        /* Write labels as required. */
        if ( data.hasLabels() ) {
            PixelMask mask = new PixelMask( surface.getClip().getBounds() );
            PointSequence pseq = activeData.getPointSequence();
            while ( pseq.next() ) {
                String label = pseq.getLabel();
                if ( label != null && label.trim().length() > 0 ) {
                    int iset = -1;
                    for ( int js = activeIsets.length - 1; iset < 0 && js >= 0;
                          js-- ) {
                        if ( pseq.isIncluded( activeIsets[ js ] ) ) {
                            iset = activeIsets[ js ];
                        }
                    }
                    if ( iset >= 0 ) {
                        double[] coords = pseq.getPoint();
                        double x = coords[ 0 ];
                        double y = coords[ 1 ];
                        Point point = surface.dataToGraphics( x, y, true );
                        if ( point != null ) {

                            /* Maintain a mask of points which have been the
                             * origin of text labels already.  Subsequent
                             * text labels on the same point will not be 
                             * plotted.  Such overwriting would be illegible
                             * in any case, and doing this prevents drawing
                             * strings for potentially very many points
                             * (expensive). */
                            if ( ! mask.get( point ) ) {
                                mask.set( point );
                                MarkStyle style =
                                    (MarkStyle) data.getSetStyle( iset );
                                g.setColor( style.getLabelColor() );
                                style.drawLabel( g, point.x, point.y, label );
                            }
                        }
                    }
                }
            }
            pseq.close();
        }

        /* Join the dots as required. */
        int huge = Math.max( getWidth(), getHeight() ) * 100;
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) data.getSetStyle( is );
            if ( style.getLine() == MarkStyle.DOT_TO_DOT ) {
                Graphics2D lineGraphics = (Graphics2D) g.create();
                lineGraphics.setColor( style.getColor() );
                lineGraphics.setStroke( style
                                       .getStroke( BasicStroke.CAP_BUTT,
                                                   BasicStroke.JOIN_MITER ) );
                lineGraphics.setRenderingHint(
                                 RenderingHints.KEY_ANTIALIASING,
                                 state.getAntialias()
                                     ? RenderingHints.VALUE_ANTIALIAS_ON
                                     : RenderingHints.VALUE_ANTIALIAS_OFF );
                int lastxp = 0;
                int lastyp = 0;
                boolean notFirst = false;
                PointSequence pseq = data.getPointSequence();
                while ( pseq.next() ) {
                    if ( pseq.isIncluded( is ) ) {
                        double[] coords = pseq.getPoint();
                        double x = coords[ 0 ];
                        double y = coords[ 1 ];
                        Point point = surface.dataToGraphics( x, y, false );
                        if ( point != null ) {

                            /* Limit plotted/recorded positions to be not too
                             * far off the plotted area.  Failing to do this
                             * can result in attempts to draw lines kilometres
                             * long, which can have an adverse effect on 
                             * the graphics system. */
                            int xp =
                                Math.max( -huge, Math.min( huge, point.x ) );
                            int yp =
                                Math.max( -huge, Math.min( huge, point.y ) );
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
                pseq.close();
            }
        }

        /* Do linear regression as required. */
        final XYStats[] statSets = new XYStats[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) data.getSetStyle( is );
            if ( style.getLine() == MarkStyle.LINEAR ) {

                /* Accumulate statistics. */
                XYStats stats = new XYStats( state.getLogFlags()[ 0 ],
                                             state.getLogFlags()[ 1 ] );
                statSets[ is ] = stats;
                int maxr = style.getMaximumRadius();
                int maxr2 = maxr * 2;
                PointSequence pseq = data.getPointSequence();
                while ( pseq.next() ) {
                    if ( pseq.isIncluded( is ) ) {
                        double[] coords = pseq.getPoint();
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
                pseq.close();

                /* Draw regression line. */
                Graphics2D g2 = g;
                Object aaHint =
                    g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     state.getAntialias()
                                         ? RenderingHints.VALUE_ANTIALIAS_ON
                                         : RenderingHints.VALUE_ANTIALIAS_OFF );
                Color color = g2.getColor();
                g2.setColor( style.getColor() );
                Stroke stroke = g2.getStroke();
                g2.setStroke( style.getStroke( BasicStroke.CAP_BUTT,
                                               BasicStroke.JOIN_MITER ) );
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
                g2.setStroke( stroke );
                g2.setColor( color );
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, aaHint );
            }
        }
        firePlotChangedLater( new ScatterPlotEvent( this, state,
                                                    pointCounts[ 0 ],
                                                    pointCounts[ 1 ],
                                                    pointCounts[ 2 ],
                                                    statSets ) );
    }

    /**
     * Returns an iterator over the points plotted last time this component
     * plotted itself.
     *
     * @return  point iterator
     */
    public PointIterator getPlottedPointIterator() {
        return new PlotDataPointIterator( getState().getPlotData(),
                                          getPointPlacer() );
    }

    /**
     * Returns a point placer suitable for this plot.
     *
     * @return  point placer
     */
    public PointPlacer getPointPlacer() {
        final PlotSurface surface = getSurface();
        return new PointPlacer() {
            public Point getXY( double[] coords ) {
                return surface.dataToGraphics( coords[ 0 ], coords[ 1 ], true );
            }
        };
    }

    /**
     * Plots markers representing the data points using vector graphics
     * (graphics.draw* methods).  This fails to render any transparency
     * of the markers.
     *
     * @param   g   graphics context
     * @param   data  plot data object
     * @param   surface  plotting surface
     * @param   tweaker   colour tweaker
     * @param   counts   if non-null, receives three values:
     *                   potential, included and visible point counts
     */
    private static void plotPointsVector( Graphics2D g, PlotData data,
                                          PlotSurface surface,
                                          DataColorTweaker tweaker,
                                          int[] counts ) {
        int nset = data.getSetCount();
        MarkStyle[] styles = getStyles( data );
        int noff = data.getNerror();
        int[] xoffs = new int[ noff ];
        int[] yoffs = new int[ noff ];
        BitSet includedMask = counts == null ? null : new BitSet();
        BitSet visibleMask = counts == null ? null : new BitSet();
        int nPotential = -1;
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = styles[ is ];
            ErrorRenderer errorRenderer = style.getErrorRenderer();
            boolean showMarks = ! style.getHidePoints();
            boolean showErrors = MarkStyle.hasErrors( style, data );
            assert showMarks || showErrors : "Why bother?";
            int maxr = style.getMaximumRadius();
            int maxr2 = maxr * 2;
            PointSequence pseq = data.getPointSequence();
            int ip = 0;
            for ( ; pseq.next(); ip++ ) {
                if ( pseq.isIncluded( is ) ) {
                    if ( counts != null ) {
                        includedMask.set( ip );
                    }
                    double[] coords = pseq.getPoint();
                    double x = coords[ 0 ];
                    double y = coords[ 1 ];
                    Point point = surface.dataToGraphics( x, y, true );
                    if ( point != null && 
                         ( tweaker == null || tweaker.setCoords( coords ) ) ) {
                        if ( counts != null ) {
                            visibleMask.set( ip );
                        }
                        int xp = point.x;
                        int yp = point.y;
                        if ( showMarks &&
                             g.hitClip( xp - maxr, yp - maxr, maxr2, maxr2 ) ) {
                            style.drawMarker( g, xp, yp, tweaker );
                        }
                        if ( showErrors ) {
                            double[][] errors = pseq.getErrors();
                            if ( transformErrors( point, coords, errors,
                                                  surface, xoffs, yoffs ) ) {
                                style.drawErrors( g, xp, yp, xoffs, yoffs,
                                                  tweaker );
                            }
                        }
                    }
                }
            }
            pseq.close();
            nPotential = ip;
        }

        /* Record information about the number of points seen. */
        if ( counts != null ) {
            counts[ 0 ] = nPotential;
            counts[ 1 ] = includedMask.cardinality();
            counts[ 2 ] = visibleMask.cardinality();
        }
    }

    /**
     * Plots markers representing the data points using bitmap type graphics.
     *
     * @param   g   graphics context
     * @param   data   plot data object
     * @param   surface  plotting surface
     * @param   counts   if non-null, receives three values:
     *                   potential, included and visible point counts
     */
    private static void plotPointsBitmap( Graphics2D g, PlotData data,
                                          PlotSurface surface, int[] counts ) {
        int nset = data.getSetCount();
        MarkStyle[] styles = getStyles( data );

        /* Work out which sets do not have associated markers drawn. */
        boolean[] showPoints = new boolean[ nset ];
        boolean[] showErrors = new boolean[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            showPoints[ is ] = ! styles[ is ].getHidePoints();
            showErrors[ is ] = MarkStyle.hasErrors( styles[ is ], data );
        }

        /* Work out padding round the edge of the raster we will be drawing on.
         * This has to be big enough that we can draw markers on the edge
         * of the visible part and not have them wrap round.  In this
         * way we can avoid doing some of the edge checking. */
        int maxr = 0;
        for ( int is = 0; is < nset; is++ ) {
            if ( showPoints[ is ] ) {
                maxr = Math.max( styles[ is ].getMaximumRadius(), maxr );
            }
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
            buffers[ is ] = new int[ npix ];
            pixoffs[ is ] = styles[ is ].getFlattenedPixelOffsets( xdim );
            npixoffs[ is ] = pixoffs[ is ].length;
        }

        /* Prepare a clip rectangle to act as bounds for the error renderer. */
        Rectangle bufClip = new Rectangle( surface.getClip().getBounds() );
        bufClip.x = pad;
        bufClip.y = pad;

        /* For each point, if it's included in any subset, then increment
         * every element of its buffer which falls under the drawing of
         * that subset's marker.  This will give us, for each subset, 
         * a raster buffer which contains values indicating how many 
         * times each of its pixels has been painted. */
        BitSet mask = new BitSet( npix );
        int noff = data.getNerror();
        int[] xoffs = new int[ noff ];
        int[] yoffs = new int[ noff ];
        boolean[] showPointMarks = new boolean[ nset ];
        boolean[] showPointErrors = new boolean[ nset ];
        int nIncluded = 0;
        int nVisible = 0;
        PointSequence pseq = data.getPointSequence();
        int ip = 0;
        for ( ; pseq.next(); ip++ ) {
            boolean use = false;
            for ( int is = 0; is < nset; is++ ) {
                boolean included = pseq.isIncluded( is );
                use = use || included;
                showPointMarks[ is ] = included && showPoints[ is ];
                showPointErrors[ is ] = included && showErrors[ is ];
            }
            if ( use ) {
                nIncluded++;
                double[] coords = pseq.getPoint();
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
                        nVisible++;
                        int base = xbase + xdim * ybase;
                        for ( int is = 0; is < nset; is++ ) {
                            boolean done = false;
                            if ( showPointErrors[ is ] ) {
                                double[][] errors = pseq.getErrors();
                                if ( transformErrors( point, coords, errors,
                                                      surface, xoffs,
                                                      yoffs ) ) {
                                    Pixellator pixer;
                                    Pixellator epixer =
                                        styles[ is ].getErrorRenderer()
                                       .getPixels( bufClip, xbase, ybase,
                                                   xoffs, yoffs );
                                    if ( showPointMarks[ is ] ) {
                                        Pixellator mpixer =
                                            new TranslatedPixellator(
                                                styles[ is ].getPixelOffsets(), 
                                                xbase, ybase );
                                        pixer = Drawing.combinePixellators(
                                            new Pixellator[] {
                                                mpixer, epixer, 
                                            }
                                        );
                                    }
                                    else {
                                        pixer = epixer;
                                    }
                                    for ( pixer.start(); pixer.next(); ) {
                                        int ipix = pixer.getX()
                                                 + pixer.getY() * xdim;
                                        buffers[ is ][ ipix ]++;
                                        mask.set( ipix );
                                    }
                                    done = true;
                                }
                            }
                            if ( showPointMarks[ is ] && ! done ) {
                                for ( int ioff = 0; ioff < npixoffs[ is ];
                                      ioff++ ) {
                                    int ipix = base + pixoffs[ is ][ ioff ];
                                    buffers[ is ][ ipix ]++;
                                    mask.set( ipix );
                                }
                                done = true;
                            }
                        }
                    }
                }
            }
        }
        int nPotential = ip;
        pseq.close();

        /* Record information about the number of points seen. */
        if ( counts != null ) {
            counts[ 0 ] = nPotential;
            counts[ 1 ] = nIncluded;
            counts[ 2 ] = nVisible;
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
        Arrays.fill( rgbBuf, 0x00ffffff );

        /* By using the bit set for iteration we skip consideration of any
         * pixels which have never been touched, which may well be a large
         * majority of them.  This should be good for efficiency. */
        for ( int ipix = mask.nextSetBit( 0 ); ipix >= 0;
              ipix = mask.nextSetBit( ipix + 1 ) ) {
            float remain = 1.0f;
            float[] weights = new float[ nset ];
            for ( int is = nset - 1; is >= 0; is-- ) {
                int count = buffers[ is ][ ipix ];
                if ( remain > 0f ) {
                    float weight = Math.min( remain, opacity[ is ] * count );
                    weights[ is ] = weight;
                    remain -= weight;
                }
            }
            if ( remain < 1.0f ) {
                float totWeight = 1.0f - remain;
                float[] argb = new float[ 4 ];
                argb[ 3 ] = totWeight;
                for ( int is = 0; is < nset; is++ ) {
                    float fw = weights[ is ] / totWeight;
                    if ( fw > 0 ) {
                        argb[ 0 ] += fw * rCols[ is ];
                        argb[ 1 ] += fw * gCols[ is ];
                        argb[ 2 ] += fw * bCols[ is ];
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
     * Plots markers representing the data points using bitmap type graphics,
     * adjusting colours for each point using a ColorTweaker object.
     *
     * @param   g   graphics context
     * @param   data  plot data object
     * @param   surface  plotting surface
     * @param   tweaker  colour tweaker
     * @param   counts   if non-null, receives three values:
     *                   potential, included and visible point counts
     */
    private static void plotPointsTweakedBitmap( Graphics2D g, PlotData data,
                                                 PlotSurface surface,
                                                 DataColorTweaker tweaker,
                                                 int[] counts ) {
        int nset = data.getSetCount();
        MarkStyle[] styles = getStyles( data );

        /* Work out padding round the edge of the raster we will be drawing on.
         * This has to be big enough that we can draw markers on the edge
         * of the visible part and not have them wrap round.  In this
         * way we can avoid doing some of the edge checking. */
        int maxr = 0;
        for ( int is = 0; is < nset; is++ ) {
            if ( ! styles[ is ].getHidePoints() ) {
                maxr = Math.max( styles[ is ].getMaximumRadius(), maxr );
            }
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

        /* Set up the raster buffer. */
        float[][] rgbaBuf = new float[ 4 ][ npix ];

        /* Prepare a clip rectangle to act as bounds for the error renderer. */
        Rectangle bufClip = new Rectangle( surface.getClip().getBounds() );
        bufClip.x = pad;
        bufClip.y = pad;

        /* Plot the points from each subset in turn, starting with the ones
         * latest in the sequence so that they will fill up the alpha 
         * channel if there is not enough alpha to go round. */
        BitSet includedMask = counts == null ? null : new BitSet();
        BitSet visibleMask = counts == null ? null : new BitSet();
        BitSet pixelMask = new BitSet();
        int noff = data.getNerror();
        int[] xoffs = new int[ noff ];
        int[] yoffs = new int[ noff ]; 
        float[] rgba = new float[ 4 ];
        int nPotential = -1;
        for ( int is = nset - 1; is >= 0; is-- ) {
            MarkStyle style = styles[ is ];
            ErrorRenderer errorRenderer = style.getErrorRenderer();
            boolean showMarks = ! style.getHidePoints();
            boolean showErrors = MarkStyle.hasErrors( style, data );
            Pixellator markPixer = style.getPixelOffsets();
            int[] pixoffs = style.getFlattenedPixelOffsets( xdim );
            int npixoff = pixoffs.length;
            float alpha = 1.0f / style.getOpaqueLimit();
            float[] baseRgba = style.getColor().getRGBComponents( null );
            assert showMarks || showErrors : "Why bother?";

            /* Iterate over each point. */
            PointSequence pseq = data.getPointSequence();
            int ip = 0;
            for ( ; pseq.next(); ip++ ) {
                if ( pseq.isIncluded( is ) ) {
                    if ( counts != null ) {
                        includedMask.set( ip );
                    }
                    double[] coords = pseq.getPoint();
                    double x = coords[ 0 ];
                    double y = coords[ 1 ];
                    Point point = surface.dataToGraphics( x, y, true );
                    if ( point != null &&
                         ( tweaker == null || tweaker.setCoords( coords ) ) ) {
                        int xp = point.x;
                        int yp = point.y;
                        int xbase = xp - xoff;
                        int ybase = yp - yoff;
                        if ( xbase > maxr && xbase < xdim - maxr &&
                             ybase > maxr && ybase < ydim - maxr ) {
                            if ( counts != null ) {
                                visibleMask.set( ip );
                            }
                            rgba[ 0 ] = baseRgba[ 0 ];
                            rgba[ 1 ] = baseRgba[ 1 ];
                            rgba[ 2 ] = baseRgba[ 2 ];
                            rgba[ 3 ] = baseRgba[ 3 ];
                            tweaker.tweakColor( rgba );
                            rgba[ 3 ] *= alpha;
                            int base = xbase + xdim * ybase;
                            boolean done = false;
                            if ( showErrors ) {
                                double[][] errors = pseq.getErrors();
                                if ( transformErrors( point, coords, errors,
                                                      surface, xoffs,
                                                      yoffs ) ) {
                                    Pixellator pixer;
                                    Pixellator epixer =
                                        errorRenderer
                                       .getPixels( bufClip, xbase, ybase,
                                                   xoffs, yoffs );
                                    if ( showMarks ) {
                                        Pixellator mpixer =
                                            new TranslatedPixellator(
                                                markPixer, xbase, ybase );
                                        pixer = Drawing.combinePixellators(
                                            new Pixellator[] {
                                                mpixer, epixer,
                                            }
                                        );
                                    }
                                    else {
                                        pixer = epixer;
                                    }
                                    for ( pixer.start(); pixer.next(); ) {
                                        int ipix = pixer.getX()
                                                 + pixer.getY() * xdim;
                                        paintPixel( rgbaBuf, pixelMask,
                                                    ipix, rgba );
                                    }
                                    done = true;
                                }
                            }
                            if ( showMarks && ! done ) {
                                for ( int ioff = 0; ioff < npixoff; ioff++ ) {
                                    int ipix = base + pixoffs[ ioff ];
                                    paintPixel( rgbaBuf, pixelMask,
                                                ipix, rgba );
                                }
                                done = true;
                            }
                        }
                    }
                }
            }
            pseq.close();
            nPotential = ip;
        }

        /* Record information about the number of points seen. */
        if ( counts != null ) {
            counts[ 0 ] = nPotential;
            counts[ 1 ] = includedMask.cardinality();
            counts[ 2 ] = visibleMask.cardinality();
        }

        /* Prepare an image and copy the pixel data into it for those 
         * pixels which have been touched. */
        BufferedImage im =
            new BufferedImage( xdim, ydim, BufferedImage.TYPE_INT_ARGB );
        ColorModel colorModel = im.getColorModel();
        assert colorModel.equals( ColorModel.getRGBdefault() );
        assert ! colorModel.isAlphaPremultiplied();
        int[] rgbBuf = new int[ xdim * ydim ];
        Arrays.fill( rgbBuf, 0x00ffff0f );
        for ( int ipix = pixelMask.nextSetBit( 0 ); ipix >= 0;
              ipix = pixelMask.nextSetBit( ipix + 1 ) ) {
            float weight = rgbaBuf[ 3 ][ ipix ];
            float w1 = 1.0f / weight;
            rgba[ 0 ] = rgbaBuf[ 0 ][ ipix ] * w1;
            rgba[ 1 ] = rgbaBuf[ 1 ][ ipix ] * w1;
            rgba[ 2 ] = rgbaBuf[ 2 ][ ipix ] * w1;
            rgba[ 3 ] = weight;
            rgbBuf[ ipix ] = colorModel.getDataElement( rgba, 0 );
        }

        /* Finally paint the constructed image onto the graphics context. */
        im.setRGB( 0, 0, xdim, ydim, rgbBuf, 0, xdim );
        g.drawImage( im, xoff, yoff, null );
    }

    /**
     * Colours a pixel in an array with optional transparency.
     *
     * @param   rgbaBuf  4-element array of npix-element arrays: (r,g,b,a)
     * @param   mask   bit vector to be marked for pixels which are touched
     * @param   ipix   index into buffer and mask
     * @param   rgba   colour of pixel to be painted: (r,g,b,a)
     */
    private static void paintPixel( float[][] rgbaBuf, BitSet mask, int ipix,
                                    float[] rgba ) {
        float used = rgbaBuf[ 3 ][ ipix ];
        if ( used < 1f ) {
            mask.set( ipix );
            float weight = Math.min( 1f - used, rgba[ 3 ] );
            rgbaBuf[ 0 ][ ipix ] += weight * rgba[ 0 ];
            rgbaBuf[ 1 ][ ipix ] += weight * rgba[ 1 ];
            rgbaBuf[ 2 ][ ipix ] += weight * rgba[ 2 ];
            rgbaBuf[ 3 ][ ipix ] += weight;
        }
    }

    /**
     * Transforms error bounds from data space to graphics space.
     * The results are written into supplied X and Y graphics space offset
     * arrays, in the form required by MarkStyle.drawErrors.  
     * The return value indicates whether there are any non-empty 
     * errors bars to draw.
     *
     * @param  point  central value in graphics space
     * @param  centre  central value in data space (2-element array x,y)
     * @param  errors  error bar end positions in data space; these must be
     *                 paired (lo,hi in each dimension) and may be null for
     *                 no/zero error
     * @param  surface plotting surface
     * @param  xoffs   array into which X offset values from the central point
     *                 in graphics coordinates will be written
     * @param  yoffs   array into which Y offset values from the central point
     *                 in graphics coordinates will be written
     * @return  true   iff any of the elements of <code>xoffs</code>, 
     *                <code>yoffs</code> are non-zero
     */
    public static boolean transformErrors( Point point, double[] centre,
                                           double[][] errors,
                                           PlotSurface surface,
                                           int[] xoffs, int[] yoffs ) {

        /* Initialise output offset values to zero. */
        int noff = xoffs.length;
        assert noff == yoffs.length;
        for ( int ioff = 0; ioff < noff; ioff++ ) {
            xoffs[ ioff ] = 0;
            yoffs[ ioff ] = 0;
        }

        /* Initialise other variables. */
        boolean hasError = false;
        int px = point.x;
        int py = point.y;
        double cx = centre[ 0 ];
        double cy = centre[ 1 ];
        int ioff = 0;

        /* Process pairs of lower/upper bounds in each dimension. */
        int nerrDim = errors.length / 2;
        for ( int ied = 0; ied < nerrDim; ied++ ) {
            double[] lo = errors[ ied * 2 + 0 ];
            double[] hi = errors[ ied * 2 + 1 ];
            if ( lo != null ) {
                Point plo = surface.dataToGraphics( lo[ 0 ], lo[ 1 ], false );
                if ( plo != null ) {
                    int xo = plo.x - px;
                    int yo = plo.y - py;
                    if ( xo != 0 || yo != 0 ) {
                        xoffs[ ioff ] = xo;
                        yoffs[ ioff ] = yo;
                        hasError = true;
                    }
                }
            }
            ioff++;
            if ( hi != null ) {
                Point phi = surface.dataToGraphics( hi[ 0 ], hi[ 1 ], false );
                if ( phi != null ) {
                    int xo = phi.x - px;
                    int yo = phi.y - py;
                    if ( xo != 0 || yo != 0 ) {
                        xoffs[ ioff ] = xo; 
                        yoffs[ ioff ] = yo;
                        hasError = true;
                    }
                }
            }
            ioff++;
        }

        /* Return status. */
        return hasError;
    }

    /**
     * Graphical component which does the actual plotting of the points.
     * Its basic job is to call {@link #drawData} 
     * in its <code>paintComponent</code> method.  
     * However it makes things a bit more
     * complicated than that for the purposes of efficiency.
     */
    private class ScatterDataPanel extends JComponent {

        ScatterDataPanel() {
            setOpaque( false );
        }

        /**
         * Draws the component to a graphics context which probably 
         * represents the screen, by calling {@link #drawData}.
         * Since <code>drawData</code> might be fairly expensive 
         * (if there are a lot of points), and <code>paintComponent</code> 
         * can be called often without the data changing 
         * (e.g. cascading window uncover events) it is advantageous 
         * to cache the drawn points in an Image and effectively
         * blit it to the screen on subsequent paint requests.
         */
        protected void paintComponent( Graphics g ) {
            if ( isOpaque() ) {
                Color color = g.getColor();
                g.setColor( getBackground() );
                g.fillRect( 0, 0, getWidth(), getHeight() );
                g.setColor( color );
            }
 
            /* If the plotting requirements have changed since last time
             * we painted, we can use the cached image. */
            int width = getBounds().width;
            int height = getBounds().height;
            if ( getState() == lastState_ &&
                 getSurface() == lastSurface_ &&
                 width == lastWidth_ &&
                 height == lastHeight_ ) {
                assert image_ != null;
            }

            /* We need to replot the points. */
            else {
                long start = System.currentTimeMillis();

                /* Get an image to plot into.  Take care that this is an
                 * image which can take advantage of hardware acceleration
                 * (e.g. "new BufferedImage()" is no good). */
                image_ = createImage( width, height );
                Graphics ig = image_.getGraphics();

                /* Draw the surface (axis labels etc). */
                getSurface().paintSurface( ig );

                /* Plot the actual points into the cached buffer. */
                drawData( ig, true );

                /* Record the state which corresponds to the most recent
                 * plot into the cached buffer. */
                lastState_ = getState();
                lastSurface_ = getSurface();
                lastWidth_ = width;
                lastHeight_ = height;
                logger_.info( "Repaint scatter plot: "
                            + ( System.currentTimeMillis() - start ) + "ms" );
            }

            /* Copy the image from the (new or old) cached buffer onto
             * the graphics context we are being asked to paint into. */
            boolean done = g.drawImage( image_, 0, 0, null );
            assert done;
        }

        /**
         * The normal implementation for this method just calls 
         * <code>paintComponent</code>.
         * However, if we are painting the component 
         * for some reason other than posting it
         * on the screen, we don't want to use the cached-image approach
         * used in <code>paintComponent</code>, since it might be a 
         * non-pixelised graphics context (e.g. postscript).
         * So for <code>printComponent</code>, we invoke <code>drawData</code>
         * directly in an appropriate manner.
         */
        protected void printComponent( Graphics g ) {
            PlotState state = getState();
            if ( state != null && state.getPlotData() != null ) {
                PlotData data = state.getPlotData();
                MarkStyle[] styles = getStyles( data );

                /* Find out if we've got a vector-like graphics context. */
                boolean isVector = isVectorContext( g );

                /* Work out if there is any transparent rendering. */
                boolean hasTransparent = false;
                for ( int is = 0; is < styles.length; is++ ) {
                    if ( styles[ is ].getOpaqueLimit() != 1 ) {
                        hasTransparent = true;
                    }
                }
                Shader[] shaders = state.getShaders();
                for ( int iaux = 0; iaux < shaders.length; iaux++ ) {
                    if ( Shaders.isTransparent( shaders[ iaux ] ) ) {
                        hasTransparent = true;
                    }
                }

                /* If we have transparent pixels then we can't use vector
                 * graphics to plot the data.  Moreover, some graphics
                 * contexts (e.g.  PostScript based ones) are incapable 
                 * of handling transparency, so we don't want to draw a
                 * semi-transparent image directly on to the given 
                 * graphics context.
                 * We therefore render into a new BufferedImage with a
                 * white background and then draw that image on to
                 * the given context.  We can still use the base context to
                 * draw the axes etc, which is necessary to get nicely
                 * drawn characters if the context is vector-like. */
                if ( hasTransparent || ! isVector ) {

                    /* Note what we're doing. */
                    if ( isVector ) {
                        logger_.warning( "Using bitmapped postscript output, " +
                                         "necessary to retain pixel " +
                                         "transparency" );
                    }
                    
                    /* Create an image for rendering into. */
                    int w = getWidth();
                    int h = getHeight();
                    BufferedImage im =
                        new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );

                    /* Give it a transparent white background. */
                    Graphics2D gim = im.createGraphics();
                    Color imColor = gim.getColor();
                    Composite imComposite = gim.getComposite();
                    gim.setColor( new Color( 0x00ffffff, true ) );
                    gim.setComposite( AlphaComposite.Src );
                    gim.fillRect( 0, 0, w, h );
                    gim.setComposite( imComposite );
                    gim.setColor( imColor );

                    /* Draw the axes onto it - only the grid will be retained,
                     * the numeric and text labels will be clipped (use the
                     * vector ones instead). */
                    getSurface().paintSurface( gim );

                    /* Draw the data onto it. */
                    drawData( gim, true );
                    gim.dispose();

                    /* Identify the region of the pixellised image which is
                     * to be used.  We extend the clip by 2 pixels in each
                     * direction.  The reason for this is that the same line
                     * which fills the region (0,1) on a pixel image fills
                     * the region (-0.5,0.5) on vector-type context
                     * (something like that anyway), so clipping the exact
                     * paintable clip on the plotting surface gives you 
                     * ugly half-pixel breaks in lines.  So we take it one
                     * pixel out to get the image border and a further one
                     * to cover the remains of that border in the vector
                     * context.  That gives you the right look using the
                     * org.jibble.epsgraphics.EpsGraphics2D context anyway.
                     * I'm not sure if having to do this is a bug in the
                     * Jibble renderer. */
                    Rectangle clip = getSurface().getClip().getBounds();
                    int cx = clip.x - 2;
                    int cy = clip.y - 2;
                    int cw = clip.width + 4;
                    int ch = clip.height + 4;
                    BufferedImage clipIm = im.getSubimage( cx, cy, cw, ch );
                    g.drawImage( clipIm, cx, cy, null );
                }

                /* If there's no problems with transparency and we have a
                 * vector context, draw using vector graphics. */
                else {
                    drawData( g, false );
                }
            }
        }
    }

    /**
     * Utility method to extract the array of per-subset styles from a plot
     * data object as a MarkStyle array.
     *
     * @param   data  plot data
     * @return   array of mark styles
     */
    private static MarkStyle[] getStyles( PlotData data ) {
        int nset = data.getSetCount();
        MarkStyle[] styles = new MarkStyle[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            styles[ is ] = (MarkStyle) data.getSetStyle( is );
        }
        return styles;
    }
}
