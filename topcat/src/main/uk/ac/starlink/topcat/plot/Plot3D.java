package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatUtils;

/**
 * Component which paints a 3d plot.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public abstract class Plot3D extends JPanel {

    private Annotations annotations_;
    private Points points_;
    private Plot3DState state_;
    private RangeChecker rangeChecker_;
    private PlotVolume lastVol_;
    private Transformer3D lastTrans_;
    private int plotTime_;
    private Object plotvolWorkspace_;
    private int[] padBorders_;
    private double zmax_;
    private Callbacks callbacker_ ;
    private final JComponent plotArea_;
    private final boolean canZoom_;
    protected double[] loBounds_;
    protected double[] hiBounds_;
    protected double[] loBoundsG_;
    protected double[] hiBoundsG_;

    private static final MarkStyle DOT_STYLE =
        MarkShape.POINT.getStyle( Color.BLACK, 1 );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructor.
     *
     * @param  zoom  whether zooming around the central point is permitted
     */
    public Plot3D( boolean zoom ) {
        super( new BorderLayout() );
        canZoom_ = zoom;
        plotArea_ = new Plot3DDataPanel();
        plotArea_.setBackground( Color.white );
        plotArea_.setOpaque( true );
        plotArea_.setPreferredSize( new Dimension( 450, 450 ) );
        plotArea_.setBorder( BorderFactory
                            .createLineBorder( Color.DARK_GRAY ) );
        add( plotArea_, BorderLayout.CENTER );
        setOpaque( false );
        annotations_ = new Annotations();

        /* Initialize Callback recipient with a no-op implementation. */
        callbacker_ = new Callbacks() {
            public void reportCounts( int nPoint, int nInc, int nVis ) {}
            public void requestZoom( double zoom ) {}
        };

        /* Set up a zoom region if required. */
        if ( zoom ) {
            Zoomer zoomer = new Zoomer() {
                public void mousePressed( java.awt.event.MouseEvent evt ) {
                    super.mousePressed( evt );
                }
            };
            zoomer.setRegions( Arrays.asList( getZoomRegions() ) );
            zoomer.setCursorComponent( this );
            this.addMouseListener( zoomer );
            this.addMouseMotionListener( zoomer );
        }
    }

    /**
     * Provides notification that the range constraints are now as described
     * in a supplied <code>state</code> object.
     * A suitable {@link RangeChecker} object should be returned,
     * but the implementation should take care of any other updates to
     * its internal state which are required as well.
     *
     * @param  state  plot state
     * @return   a range checker appropriate to <code>state</code>'s range 
     *           constraints
     */
    protected abstract RangeChecker configureRanges( Plot3DState state );

    /**
     * Works out padding factors to be used for the plot volume.
     * The return value is the <code>padFactor</code>; the amount of 
     * space outside the unit cube in both dimensions.  1 means no 
     * extra space.  The <code>padBorders</code> array is a 4-element
     * array whose values on entry are ignored; on exit it should contain
     * space, additional to <code>padFactor</code>, to be left around
     * the edges of the plot.  The order is (left,right,bottom,top).
     *
     * @param  state   plot state
     * @param  g       graphics context
     * @param  padBorders  4-element array, filled on return
     * @return  pad factor (>=1)
     * @see   PlotVolume#PlotVolume
     */
    protected abstract double getPadding( Plot3DState state, Graphics g,
                                          int[] padBorders );

    /**
     * Indicates whether only the front part of the plot should be plotted.
     *
     * @param   state  plot state
     * @return   true iff parts of the plot behind the centre of the Z axis
     *           should be ignored
     */
    protected abstract boolean frontOnly( Plot3DState state );

    /**
     * Returns an array of 3 flags which indicate whether logarithmic scaling
     * is in force on the X, Y and Z axes respectively.
     *
     * @return  3-element array of Cartesian axis log scaling flags
     */
    protected abstract boolean[] get3DLogFlags();

    /**
     * Draws grid lines which contain all the known points.
     * According to the value of the <code>front</code> parameter,
     * either the lines which are behind all the data points,
     * or the lines which are in front of all the data points are drawn.
     * Thus, the routine needs to be called twice to plot all the lines.
     * The graphics context has had all the customisation it needs.
     *
     * @param   state  plot state
     * @param   g      graphics context
     * @param   trans  transformer which maps data space to 3d graphics space
     * @param   vol    the plotting volume onto which the plot is done
     * @param   front  true for lines in front of data, false for lines behind
     */
    protected abstract void plotAxes( Plot3DState state, Graphics g,
                                      Transformer3D trans, PlotVolume vol,
                                      boolean front );

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     *
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        points_ = points;
        lastVol_ = null;
        lastTrans_ = null;
    }

    /**
     * Returns the data set for this point.
     *
     * @return  data points
     */
    public Points getPoints() {
        return points_;
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( Plot3DState state ) {
        state_ = state;
        annotations_.validate();
        lastVol_ = null;
        lastTrans_ = null;
        PointSelection psel = state.getPointSelection();

        /* Adjust internal state according to requested ranges. */
        if ( state.getValid() ) {
            rangeChecker_ = configureRanges( state );
        }
    }

    /**
     * Returns the most recently set state for this plot.
     *
     * @return  plot state
     */
    public Plot3DState getState() {
        return state_;
    }

    /**
     * Returns the current point selection.
     * This convenience method just retrieves it from the current plot state.
     * 
     * @return   point selection
     */
    public PointSelection getPointSelection() {
        return state_.getPointSelection();
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
     * Returns the bounds of the actual plotting area.
     *
     * @return  plot area bounds
     */
    public Rectangle getPlotBounds() {
        return plotArea_.getBounds();
    }

    /**
     * Configures this object with an object that can receive callbacks
     * when certain events take place.  This method should be called with
     * a non-null value argument early in the life of this object
     * (before plotting begins).
     *
     * @param  callbacker   event callback recipient
     */
    public void setCallbacks( Callbacks callbacker ) {
        callbacker_ = callbacker;
    }

    /**
     * Indicates whether this component offers user-initiated zooming 
     * around the centre.
     *
     * @return  true iff zooming is permitted
     */
    public boolean canZoom() {
        return canZoom_;
    }

    /**
     * Performs the painting - this method does the actual work.
     */
    private void drawData( Graphics g, Component c ) {

        /* Prepare data. */
        Points points = getPoints();
        Plot3DState state = getState();
        if ( points == null || state == null || ! state.getValid() ) {
            return;
        }
        RowSubset[] sets = getPointSelection().getSubsets();
        Style[] styles = getPointSelection().getStyles();
        int nset = sets.length;

        /* Set up a transformer to do the mapping from data space to
         * normalised 3-d view space. */
        Transformer3D trans = 
            new Transformer3D( state.getRotation(), loBoundsG_, hiBoundsG_,
                               state.getZoomScale() );

        /* Work out padding factors for the plot volume. */
        padBorders_ = new int[ 4 ]; 
        double padFactor = getPadding( state, g, padBorders_ );

        /* Prepare an array of styles which we may need to plot on the
         * PlotVolume object. */
        List plotStyleList = new ArrayList( Arrays.asList( styles ) );
        int iDotStyle = plotStyleList.size();
        plotStyleList.add( DOT_STYLE );
        MarkStyle[] plotStyles =
            (MarkStyle[]) plotStyleList.toArray( new MarkStyle[ 0 ] );

        /* See if all the markers are opaque; if they are we can use a 
         * simpler rendering algorithm. */
        boolean allOpaque = true;
        for ( int is = 0; is < nset && allOpaque; is++ ) {
            allOpaque = allOpaque && plotStyles[ is ].getOpaqueLimit() == 1;
        }
        Shader[] shaders = state.getShaders();
        for ( int iaux = 0; iaux < shaders.length; iaux++ ) {
            Shader shader = shaders[ iaux ];
            allOpaque = allOpaque && ! Shaders.isTransparent( shaders[ iaux ] );
        }

        /* See if there are any error bars to be plotted.  If not, we can
         * use more efficient rendering machinery. */
        boolean anyErrors = false;
        for ( int is = 0; is < nset && ! anyErrors; is++ ) {
            anyErrors = anyErrors ||
                        MarkStyle.hasErrors( (MarkStyle) styles[ is ], points );
        }

        /* Get fogginess. */
        double fog = state.getFogginess();

        /* Work out how points will be shaded according to auxiliary axis
         * coordinates.  This does not include the effect of fogging,
         * which will be handled separately. */
        DataColorTweaker tweaker = ShaderTweaker.createTweaker( 3, state );

        /* Decide what rendering algorithm we're going to have to use, and
         * create a PlotVolume object accordingly. */
        PlotVolume vol;
        if ( GraphicsWindow.isVector( g ) ) {
            if ( ! allOpaque ) {
                logger_.warning( "Can't render transparency in PostScript" );
            }
            vol = new VectorSortPlotVolume( c, g, plotStyles, padFactor,
                                            padBorders_, fog, tweaker );
        }
        else if ( allOpaque ) {
            vol = new ZBufferPlotVolume( c, g, plotStyles, padFactor,
                                         padBorders_, fog, tweaker,
                                         getZBufferWorkspace() );
        }
        else {
            vol = new BitmapSortPlotVolume( c, g, plotStyles, padFactor,
                                            padBorders_, fog, anyErrors,
                                            -1.0, 2.0, tweaker,
                                            getBitmapSortWorkspace() );
        }

        /* If we're zoomed on a spherical plot and the points are only
         * on the surface of the sphere, then we don't want to see the
         * points on the far surface. */
        boolean frontOnly = frontOnly( state );

        /* Plot back part of bounding box. */
        boolean grid = state.getGrid();
        if ( grid && ! frontOnly ) {
            plotAxes( g, trans, vol, false );
        }

        /* Work out which sets have markers and which sets have error bars
         * drawn. */
        boolean[] showPoints = new boolean[ nset ];
        boolean[] showErrors = new boolean[ nset ];
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = (MarkStyle) styles[ is ];
            showPoints[ is ] = ! style.getHidePoints();
            showErrors[ is ] = MarkStyle.hasErrors( style, points );
        }

        /* Start the stopwatch to time how long it takes to plot all points. */
        long tStart = System.currentTimeMillis();

        /* If this is an intermediate plot (during a rotation, with a 
         * guaranteed non-intermediate one to follow shortly), then prepare
         * to plot only a fraction of the points.  In this way we can 
         * ensure reasonable responsiveness - dragging the plot to rotate
         * it will not take ages between frames. */
        boolean isRotating = state.getRotating();
        int step = isRotating ? Math.max( plotTime_ / 100, 1 )
                              : 1;

        /* If we're only looking at the front surface of a sphere and we're
         * zoomed we can arrange a limit on the Z coordinate to filter points.
         * This is partly optimisation (not plotting points that aren't
         * seen because they are off screen laterally) and partly so we
         * don't see points on the far surface (zmax=0.5).  
         * By my quick trig the limit for optimisation ought to be 
         * Math.sqrt(0.5)*(1-Math.sqrt(1-0.25/(zoom*zoom)),
         * but that gives a region that's too small so I must have gone wrong.
         * There's something about aspect ratio to take into account too.
         * So, leave it as 0.5 for now, which is correct but not optimal. */
        zmax_ = frontOnly ? 0.5 : Double.MAX_VALUE;

        /* Submit each point for drawing in the display volume as
         * appropriate. */
        int np = points.getCount();
        int nInclude = 0;
        int nVisible = 0;
        boolean[] logFlags = get3DLogFlags();
        boolean[] showMarkPoints = new boolean[ nset ];
        boolean[] showMarkErrors = new boolean[ nset ];
        double[] centre = new double[ 3 ];
        int nerr = points.getNerror();
        double[] xerrs = new double[ nerr ];
        double[] yerrs = new double[ nerr ];
        double[] zerrs = new double[ nerr ];
        RangeChecker ranger = rangeChecker_;
        for ( int ip = 0; ip < np; ip += step ) {
            long lp = (long) ip;
            boolean use = false;
            boolean useErrors = false;
            for ( int is = 0; is < nset; is++ ) {
                boolean included = sets[ is ].isIncluded( lp );
                use = use || included;
                boolean showP = included && showPoints[ is ];
                boolean showE = included && showErrors[ is ];
                useErrors = useErrors || showE;
                showMarkPoints[ is ] = showP;
                showMarkErrors[ is ] = showE;
            }
            if ( use ) {
                nInclude++;
                double[] coords = points.getPoint( ip );
                centre[ 0 ] = coords[ 0 ];
                centre[ 1 ] = coords[ 1 ];
                centre[ 2 ] = coords[ 2 ];
                if ( ranger.inRange( coords ) && logize( coords, logFlags ) ) {
                    trans.transform( coords );
                    if ( coords[ 2 ] < zmax_ ) {
                        if ( useErrors ) {
                            double[][] errors = points.getErrors( ip );
                            useErrors = useErrors &&
                                transformErrors( trans, ranger, logFlags,
                                                 errors, xerrs, yerrs, zerrs );
                        }
                        for ( int is = 0; is < nset; is++ ) {
                            boolean plotted = false;
                            if ( useErrors && showMarkErrors[ is ] ) {
                                boolean vis =
                                    vol.plot3d( coords, is,
                                                showMarkPoints[ is ],
                                                nerr, xerrs, yerrs, zerrs );
                                if ( vis ) {
                                    nVisible++;
                                }
                                plotted = true;
                            }
                            if ( ! plotted && showMarkPoints[ is ] ) {
                                boolean vis = vol.plot3d( coords, is );
                                if ( vis ) {
                                    nVisible++;
                                }
                            }
                        }
                    }
                }
            }
        }

        /* Plot a teeny static dot in the middle of the data. */
        double[] dot = new double[ points.getNdim() ];
        dot[ 0 ] = 0.5;
        dot[ 1 ] = 0.5;
        dot[ 2 ] = 0.5;
        vol.plot3d( dot, iDotStyle );

        /* Tell the volume that all the points are in for plotting.
         * This will do the painting on the graphics context if it hasn't
         * been done already. */
        vol.flush();

        /* Calculate time to plot all points. */
        if ( ! isRotating ) {
            plotTime_ = (int) ( System.currentTimeMillis() - tStart );
        }

        /* Plot front part of bounding box. */
        if ( grid ) {
            plotAxes( g, trans, vol, true );
        }

        /* Draw any annotations. */
        annotations_.draw( g, trans, vol );

        /* Store information about the most recent plot. */
        lastVol_ = vol;
        lastTrans_ = trans;

        /* Notify information about the points that were plotted.
         * I'm not sure that this has to be done outside of the paint
         * call, but it seems like the sort of thing that might be true,
         * so do it to be safe. */
        final int np1 = np;
        final int ni1 = nInclude;
        final int nv1 = nVisible;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                callbacker_.reportCounts( np1, ni1, nv1 );
            }
        } );

        /* Log the time this painting took for tuning purposes. */
        logger_.fine( "3D plot time (ms): " + 
                     ( System.currentTimeMillis() - tStart ) );
    }

    /**
     * Draws grid lines which contain all the known points.
     * According to the value of the <code>front</code> parameter, 
     * either the lines which are behind all the data points,
     * or the lines which are in front of all the data points are drawn.
     * Thus, the routine needs to be called twice to plot all the lines.
     *
     * @param   g      graphics context
     * @param   trans  transformer which maps data space to 3d graphics space
     * @param   vol    the plotting volume onto which the plot is done
     * @param   front  true for lines in front of data, false for lines behind
     */
    private void plotAxes( Graphics g, Transformer3D trans,
                           PlotVolume vol, boolean front ) {
        Plot3DState state = getState();
        Graphics2D g2 = (Graphics2D) g;
        Object antialias = 
            g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             state_.getAntialias() 
                                 ? RenderingHints.VALUE_ANTIALIAS_ON
                                 : RenderingHints.VALUE_ANTIALIAS_DEFAULT );
        plotAxes( state, g, trans, vol, front );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialias );
    }

    /**
     * Returns an iterator over the points plotted last time this component
     * painted itself.
     *
     * @return  point iterator
     */
    public PointIterator getPlottedPointIterator() {
        final PlotVolume vol = lastVol_;
        final Transformer3D trans = lastTrans_;
        final Points points = getPoints();
        if ( vol != null && trans != null && points != null ) {
            final boolean[] logFlags = get3DLogFlags();
            final int np = points.getCount();
            final RowSubset[] sets = getPointSelection().getSubsets();
            final int nset = sets.length;
            final RangeChecker ranger = rangeChecker_;
            Rectangle plotBounds = getPlotBounds();
            final int xmin = plotBounds.x + 1;
            final int xmax = plotBounds.x + plotBounds.width - 2;
            final int ymin = plotBounds.y + 1;
            final int ymax = plotBounds.y + plotBounds.height - 2;
            return new PointIterator() {
                int ip = -1;
                int[] point = new int[ 3 ];
                protected int[] nextPoint() {
                    while ( ++ip < np ) {
                        long lp = (long) ip;
                        boolean use = false;
                        for ( int is = 0; is < nset && !use; is++ ) {
                            use = use || sets[ is ].isIncluded( lp );
                        }
                        if ( use ) {
                            double[] coords = points.getPoint( ip );
                            if ( ranger.inRange( coords ) &&
                                 logize( coords, logFlags ) ) {
                                trans.transform( coords );
                                int px = vol.projectX( coords[ 0 ] );
                                int py = vol.projectY( coords[ 1 ] );
                                double z = coords[ 2 ];
                                if ( px >= xmin && px <= xmax &&
                                     py >= ymin && py <= ymax &&
                                     z <= zmax_ ) {
                                    point[ 0 ] = ip;
                                    point[ 1 ] = px;
                                    point[ 2 ] = py;
                                    return point;
                                }
                            }
                        }
                    }
                    return null;
                }
            };
        }
        else {
            return null;
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
     * Lazily constructs and returns a ZBufferPlotVolume workspace object
     * owned by this plot.
     *
     * @return   new or used workspace object
     */
    private ZBufferPlotVolume.Workspace getZBufferWorkspace() {
        if ( ! ( plotvolWorkspace_ instanceof ZBufferPlotVolume.Workspace ) ) {
            plotvolWorkspace_ = new ZBufferPlotVolume.Workspace();
        }
        return (ZBufferPlotVolume.Workspace) plotvolWorkspace_;
    }

    /**
     * Lazily constructs and returns a BitmapSortPlotVolume workspace object
     * owned by this plot.
     *
     * @return   new or used workspace object
     */
    private BitmapSortPlotVolume.Workspace getBitmapSortWorkspace() {
        if ( ! ( plotvolWorkspace_ instanceof
                 BitmapSortPlotVolume.Workspace ) ) {
            plotvolWorkspace_ = new BitmapSortPlotVolume.Workspace();
        }
        return (BitmapSortPlotVolume.Workspace ) plotvolWorkspace_;
    }

    /**
     * Converts coordinates to logarithmic values if necessary.
     * The <code>coords</code> array holds the input values on entry,
     * and each of these will be turned into logarithms of themselves
     * on exit iff the corresponding element of the logFlags array is
     * true.
     * The return value will be true if the conversion went OK, and
     * false if it couldn't be done for at least one coordinate, 
     * because it was non-positive.
     *
     * @param  coords  3-element coordinate array
     */
    protected static boolean logize( double[] coords, boolean[] logFlags ) {
        for ( int iax = 0; iax < 3; iax++ ) {
            if ( logFlags[ iax ] ) {
                if ( coords[ iax ] > 0 ) {
                    coords[ iax ] = Math.log( coords[ iax ] );
                }
                else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines whether a 3-d coordinate is within given 3-d bounds.
     * Sitting on the boundary counts as in range. 
     * If any of the elements of <code>coords</code> is NaN, false
     * is returned.
     *
     * @param   coords 3-element array giving coordinates to test
     * @param   lo     3-element array giving lower bounds of range
     * @param   hi     3-element array giving upper bounds of range
     * @return  true iff <code>coords</code> is between <code>lo</code>
     *          and <code>hi</code>
     */
    private static boolean inRange( double[] coords, double[] lo,
                                    double[] hi ) {
        return coords[ 0 ] >= lo[ 0 ] && coords[ 0 ] <= hi[ 0 ]
            && coords[ 1 ] >= lo[ 1 ] && coords[ 1 ] <= hi[ 1 ]
            && coords[ 2 ] >= lo[ 2 ] && coords[ 2 ] <= hi[ 2 ];
    }

    /**
     * Transforms errors from the form they assume in input data (offsets
     * to a central data point in data space) to a set of absolute 
     * coordinates of points in the transformed graphics space.
     * The arrangement of the input data offsets
     * (<code>loErrs</code>, <code>hiErrs</code>) is as determined by the 
     * {@link Points} object.
     * The number and ordering of the output data points
     * (<code>xerrs</code>, <code>yerrs</code>, <code>zerrs</code>)
     * are as required by {@link ErrorRenderer} objects.
     *
     * <p>Points which don't represent errors, either because they have
     * zero offsets or because they fall outside of the range of this 3D plot,
     * are represented in the output coordinates as <code>Double.NaN</code>.
     * The return value indicates whether any of the transformed values 
     * have non-blank values - if false, then error drawing is pointless.
     *
     * @param   trans   data space -> graphics space transformer
     * @param   ranger  range checker - anything out of range will be discarded
     * @param   logFlags  flags for which axes will be plotted logarithmically
     * @param   errors   data space error points, in pairs
     * @param   xerrs   graphics space X coordinates for error points
     * @param   yerrs   graphics space Y coordinates for error points
     * @param   zerrs   graphics space Z coordinates for error points
     * @return  true if some of the calculated errors are non-blank
     */
    protected static boolean transformErrors( Transformer3D trans,
                                              RangeChecker ranger,
                                              boolean[] logFlags,
                                              double[][] errors, double[] xerrs,
                                              double[] yerrs, double[] zerrs ) {

        /* Initialise output error values. */
        int nerr = xerrs.length;
        assert nerr == yerrs.length;
        assert nerr == zerrs.length;
        for ( int ierr = 0; ierr < nerr; ierr++ ) {
            xerrs[ ierr ] = Double.NaN;
            yerrs[ ierr ] = Double.NaN;
            zerrs[ ierr ] = Double.NaN;
        }

        /* Initialise other variables. */
        boolean hasError = false;
        int ierr = 0;
        int nerrDim = errors.length / 2;
        for ( int ied = 0; ied < nerrDim; ied++ ) {
            double[] lo = errors[ ied * 2 + 0 ];
            double[] hi = errors[ ied * 2 + 1 ];
            if ( lo != null ) {
                if ( ranger.inRange( lo ) && logize( lo, logFlags ) ) {
                    trans.transform( lo );
                    xerrs[ ierr ] = lo[ 0 ];
                    yerrs[ ierr ] = lo[ 1 ];
                    zerrs[ ierr ] = lo[ 2 ];
                    hasError = true;
                }
            }
            ierr++;
            if ( hi != null ) {
                if ( ranger.inRange( hi ) && logize( hi, logFlags ) ) {
                    trans.transform( hi );
                    xerrs[ ierr ] = hi[ 0 ];
                    yerrs[ ierr ] = hi[ 1 ];
                    zerrs[ ierr ] = hi[ 2 ];
                    hasError = true;
                }
            }
            ierr++;
        }
        return hasError;
    }

    /**
     * Returns an array of zoom region objects for use with this plot.
     * 
     * @return  zoom region array
     */
    private ZoomRegion[] getZoomRegions() {
        return new ZoomRegion[] {

            /* Left hand side of plot. */
            new ZoomRegion3D( false ) {
                protected Rectangle getTarget( Rectangle display,
                                               Rectangle bounds ) {
                    int x = display.x + display.width;
                    int width = bounds.width - x;
                    int y = display.y;
                    int height = display.height;
                    return new Rectangle( x, y, width, height );
                }
            },

            /* Right hand side of plot. */
            new ZoomRegion3D( false ) {
                protected Rectangle getTarget( Rectangle display,
                                               Rectangle bounds ) {
                    return new Rectangle( 0, display.y,
                                          display.x, display.height );
                }
            },
        };
    }

    /**
     * Zoom Region abstract superclass which handles central zooming of
     * this plot.  Different concrete subclasses will have different
     * target regions.
     */
    private abstract class ZoomRegion3D extends CentreZoomRegion {

        /**
         * Constructor.
         *
         * @param   isX  true for horizontal target, false for vertical
         */
        ZoomRegion3D( boolean isX ) {
            super( isX );
        }

        /**
         * Defines the target region given the component and plot regions.
         *
         * @param   display  bounds of the plot region
         * @param   bounds   bounds of the entire component
         */
        protected abstract Rectangle getTarget( Rectangle display,
                                                Rectangle bounds );

        public Rectangle getDisplay() {
            Rectangle display = new Rectangle( getPlotBounds() );
            display.x += padBorders_[ 0 ];
            display.y += padBorders_[ 3 ];
            display.width -= padBorders_[ 0 ] + padBorders_[ 1 ];
            display.height -= padBorders_[ 2 ] + padBorders_[ 3 ];
                return display;
        }

        public Rectangle getTarget() {
            return getTarget( plotArea_.getBounds(), Plot3D.this.getBounds() );
        }

        public void zoomed( double[][] bounds ) {
            callbacker_.requestZoom( state_.getZoomScale() / bounds[ 0 ][ 0 ] );
        }

    }

    /**
     * Defines an object which can provide some services which this Plot3D
     * uses in relation to interacting with its environment.
     */
    public static interface Callbacks {

        /**
         * This method is called following a repaint with the
         * values of the number of points that were plotted.
         * It is intended as a hook for clients which want to know that 
         * information in a way which is kept up to date.
         *
         * @param   nPoint  total number of points available
         * @param   nInc  number of points included in marked subsets
         * @param   nVis  number of points actually plotted (may be less
         *          nIncluded if some are out of bounds)
         */
        void reportCounts( int nPoint, int nInc, int nVis );

        /**
         * This method is called when a zoom request has been completed.
         *
         * @param  zoom  change in zoom level (relative) - 1 means no change,
         *         greater than one is zoom in, less is zoom out
         */
        void requestZoom( double zoom );
    }

    /**
     * Interface for checking that a 3-d coordinate is in range.
     */
    protected static abstract class RangeChecker {

        /**
         * Returns true iff the 3-element coords array is considered in
         * plotting range for a particular 3D plot.
         * Sitting on the boundary normally counts as in range.
         * If any of the elements of <code>coords</code> is NaN,
         * the return should be <code>false</code>.
         *
         * @param  coords  3-element coordinates of a point in data space
         * @return  true  iff coords in in range for this plot
         */
        abstract boolean inRange( double[] coords );
    }

    /**
     * Transforms points in 3d data space to points in 3d graphics space. 
     */
    protected static class Transformer3D {

        final double[] loBounds_;
        final double[] factors_;
        final double[] rot_;
        final double zoom_;

        /**
         * Constructs a transformer.  A cuboid of interest in data space
         * is specified by the <code>loBounds</code> and <code>hiBounds</code>
         * arrays.  When the <code>transform()</code> method is called
         * on a point within this region, it will transform it to
         * lie in a unit sphere centred on (0.5, 0.5, 0.5) and hence,
         * <i>a fortiori</i>, in the unit cube.
         *
         * @param    rotation  9-element unitary rotation matrix
         * @param    loBounds  lower bounds of cuboid of interest (xlo,ylo,zlo)
         * @param    hiBounds  upper bounds of cuboid of interest (xhi,yhi,zhi)
         * @param    zoom      zoom factor to apply to X-Y values
         */
        Transformer3D( double[] rotation, double[] loBounds,
                       double[] hiBounds, double zoom ) {
            rot_ = (double[]) rotation.clone();
            loBounds_ = new double[ 3 ];
            factors_ = new double[ 3 ];
            zoom_ = zoom;
            for ( int i = 0; i < 3; i++ ) {
                double lo = loBounds[ i ];
                double hi = hiBounds[ i ];
                if ( lo == hi ) {
                    lo = lo - 1.0;
                    hi = hi + 1.0;
                }
                loBounds_[ i ] = lo;
                factors_[ i ] = 1.0 / ( hi - lo );
            }
        }

        /**
         * Transforms a point in data space to a point in graphics space.
         *
         * @param  coords  point coordinates (modified on exit)
         */
        void transform( double[] coords ) {

            /* Shift the coordinates to within a unit sphere centered on
             * the origin. */
            for ( int i = 0; i < 3; i++ ) {
                coords[ i ] = ( coords[ i ] - loBounds_[ i ] ) * factors_[ i ]
                            - 0.5;
            }

            /* Perform rotations as determined by the rotation matrix. */
            double x = coords[ 0 ];
            double y = coords[ 1 ];
            double z = coords[ 2 ];
            coords[ 0 ] = rot_[ 0 ] * x + rot_[ 1 ] * y + rot_[ 2 ] * z;
            coords[ 1 ] = rot_[ 3 ] * x + rot_[ 4 ] * y + rot_[ 5 ] * z;
            coords[ 2 ] = rot_[ 6 ] * x + rot_[ 7 ] * y + rot_[ 8 ] * z;

            /* Zoom. */
            coords[ 0 ] *= zoom_;
            coords[ 1 ] *= zoom_;

            /* Shift the origin so the unit sphere is centred at (.5,.5,.5). */
            for ( int i = 0; i < 3; i++ ) {
                coords[ i ] += 0.5;
            }
        }

        /**
         * Returns the vector in data space which points into the screen.
         *
         * @return   vector normal to view
         */
        double[] getDepthVector() {
            return Matrices.normalise(
                Matrices.mvMult( Matrices.invert( rot_ ),
                                 new double[] { 0., 0., 1. } ) );
        }
    }

    /**
     * Component containing the plot.
     */
    private class Plot3DDataPanel extends JComponent {
        private boolean failed_ = false;
        Plot3DDataPanel() {
            setBackground( Color.WHITE );
            setOpaque( true );
        }
        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );
            if ( isOpaque() ) {
                ((Graphics2D) g).setBackground( getBackground() );
                g.clearRect( 0, 0, getWidth(), getHeight() );
            }
            if ( ! failed_ ) {
                try {
                    drawData( g, this );
                }
                catch ( OutOfMemoryError e ) {
                    failed_ = true;
                    TopcatUtils.memoryErrorLater( e );
                }
            }
        }
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
        void draw( Graphics g, Transformer3D trans, PlotVolume vol ) {

            /* Check that the points object is not empty and bail out if so.  
             * I think this is a hack; we shouldn't be here if it is empty.
             * However not making this check results in ugly stackdumps. */
            Points points = getPoints();
            if ( points.getCount() == 0 ) {
                return;
            }

            /* Draw markers for any active points. */
            boolean[] logFlags = get3DLogFlags();
            for ( int i = 0; i < activePoints_.length; i++ ) {
                double[] coords = points.getPoint( activePoints_[ i ] );
                if ( logize( coords, logFlags ) ) {
                    trans.transform( coords );
                    if ( ! Double.isNaN( coords[ 0 ] ) &&
                         ! Double.isNaN( coords[ 1 ] ) &&
                         ! Double.isNaN( coords[ 2 ] ) ) {
                        cursorStyle_.drawMarker( g,
                                                 vol.projectX( coords[ 0 ] ),
                                                 vol.projectY( coords[ 1 ] ) );
                    }
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
}
