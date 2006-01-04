package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which paints a 3d plot.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Plot3D extends JComponent {

    private Annotations annotations_;
    private Points points_;
    private Plot3DState state_;
    private double[] loBounds_;
    private double[] hiBounds_;
    private double[] loBoundsG_;
    private double[] hiBoundsG_;
    private PlotVolume lastVol_;
    private Transformer3D lastTrans_;
    private PointRegistry pointReg_;
    private double[][][] sphereGrid_;

    private static final double SQRT3 = Math.sqrt( 3.0 );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /** Number of grid lines above (and below) the equator for sphere grid. */
    private static final int LATITUDE_GRID_LINES = 2;

    /** Number of longitude lines for sphere grid. */
    private static final int LONGITUDE_GRID_LINES = 12;

    /**
     * Constructor.
     */
    public Plot3D() {
        setBackground( Color.white );
        setOpaque( true );
        setPreferredSize( new Dimension( 400, 400 ) );
        setBorder( javax.swing.BorderFactory.createLineBorder( Color.GRAY ) );
        annotations_ = new Annotations();
    }

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
     * Resets the scaling so that all the data points known by this plot
     * will be visible at all rotations.  This method sets 
     * loBounds_, hiBounds_, loBoundsG_ and hiBoundsG_.
     */
    public void rescale() {
        if ( getState().getSpherical() ) {
            rescaleSpherical();
        }
        else {
            rescaleCartesian();
        }
        lastVol_ = null;
        lastTrans_ = null;
    }

    /**
     * Rescale the data ranges in a way suitable for a Cartesian plot.
     */
    private void rescaleCartesian() {
        boolean[] logFlags = getState().getLogFlags();
        double[] loBounds = new double[ 3 ];
        double[] hiBounds = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            loBounds[ i ] = Double.MAX_VALUE;
            hiBounds[ i ] = logFlags[ i ] ? Double.MIN_VALUE
                                          : - Double.MAX_VALUE;
        }
        Points points = getPoints();
        int np = points.getCount();
        RowSubset[] sets = getPointSelection().getSubsets();
        int nset = sets.length;

        double[] coords = new double[ 3 ];
        int nok = 0;
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset && ! use; is++ ) {
                use = use || sets[ is ].isIncluded( lp );
            }
            if ( use ) {
                points.getCoords( ip, coords );
                if ( ! Double.isNaN( coords[ 0 ] ) &&
                     ! Double.isNaN( coords[ 1 ] ) &&
                     ! Double.isNaN( coords[ 2 ] ) &&
                     ! Double.isInfinite( coords[ 0 ] ) &&
                     ! Double.isInfinite( coords[ 1 ] ) &&
                     ! Double.isInfinite( coords[ 2 ] ) &&
                     ! ( logFlags[ 0 ] && coords[ 0 ] <= 0.0 ) &&
                     ! ( logFlags[ 1 ] && coords[ 1 ] <= 0.0 ) &&
                     ! ( logFlags[ 2 ] && coords[ 2 ] <= 0.0 ) ) {
                    nok++;
                    for ( int id = 0; id < 3; id++ ) {
                        loBounds[ id ] = Math.min( loBounds[ id ],
                                                   coords[ id ] );
                        hiBounds[ id ] = Math.max( hiBounds[ id ],
                                                   coords[ id ] );
                    }
                }
            }
        }
        for ( int i = 0; i < 3; i++ ) {
            if ( nok == 0 ) {
                loBounds[ i ] = logFlags[ i ] ? 1.0 : 0.0;
                hiBounds[ i ] = logFlags[ i ] ? 10.0 : 1.0;
            }
            else if ( loBounds[ i ] == hiBounds[ i ] ) {
                loBounds[ i ] = logFlags[ i ] ? loBounds[ i ] / 2.0
                                              : loBounds[ i ] - 1.0;
                hiBounds[ i ] = logFlags[ i ] ? hiBounds[ i ] * 2.0
                                              : hiBounds[ i ] + 1.0;
            }
        }
        loBounds_ = loBounds;
        hiBounds_ = hiBounds;
        loBoundsG_ = new double[ 3 ];
        hiBoundsG_ = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            double lo = loBounds_[ i ];
            double hi = hiBounds_[ i ];
            if ( logFlags[ i ] ) {
                lo = Math.log( lo );
                hi = Math.log( hi );
            }
            boolean flip = getState().getFlipFlags()[ i ];
            loBoundsG_[ i ] = flip ? hi : lo;
            hiBoundsG_[ i ] = flip ? lo : hi;
        }
    }

    /**
     * Rescale the data ranges in a way suitable for a spherical polar plot.
     * The result is a cube centred at the origin with each side equal
     * to twice the maximum radius ((x^2 + y^2 + z^2)^0.5).
     */
    private void rescaleSpherical() {
        double r2max = 0.0;
        Points points = getPoints();
        int np = points.getCount();
        RowSubset[] sets = getPointSelection().getSubsets();
        int nset = sets.length;

        /* Calculate the maximum radius of points being plotted. */
        double[] coords = new double[ 3 ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset && ! use; is++ ) {
                use = use || sets[ is ].isIncluded( lp );
            }
            if ( use ) {
                points.getCoords( ip, coords );
                double r2 = coords[ 0 ] * coords[ 0 ]
                          + coords[ 1 ] * coords[ 1 ]
                          + coords[ 2 ] * coords[ 2 ];
                if ( r2 > r2max && ! Double.isInfinite( r2 ) ) {
                    r2max = r2;
                }
            }
        }

        /* Set the bounds arrays accordingly. */
        double rmax = r2max > 0.0 ? Math.sqrt( r2max ) : 1.0;
        double[] loBounds = new double[ 3 ];
        double[] hiBounds = new double[ 3 ];
        Arrays.fill( loBounds, -rmax );
        Arrays.fill( hiBounds, +rmax );
        loBounds_ = loBounds;
        hiBounds_ = hiBounds;
        loBoundsG_ = (double[]) loBounds.clone();
        hiBoundsG_ = (double[]) hiBounds.clone();
    }

    /**
     * Performs the painting - this method does the actual work.
     */
    protected void paintComponent( Graphics g ) {
        long tStart = System.currentTimeMillis();

        /* Prepare for painting. */
        super.paintComponent( g );
        if ( isOpaque() ) {
            ((Graphics2D) g).setBackground( getBackground() );
            g.clearRect( 0, 0, getWidth(), getHeight() );
        }

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
            new Transformer3D( state.getRotation(), loBoundsG_, hiBoundsG_ );

        /* Set up a plotting volume to render the 3-d points. */
        double padFactor = state.getSpherical() ? 1.05 : Math.sqrt( 3. );
        PlotVolume vol = new SortPlotVolume( this, g, padFactor );
        vol.getDepthTweaker().setFogginess( state_.getFogginess() );

        /* Plot back part of bounding box. */
        boolean grid = state.getGrid();
        if ( grid ) {
            plotAxes( g, trans, vol, false );
        }

        /* Submit each point for drawing in the display volume as
         * appropriate. */
        int np = points.getCount();
        int nInclude = 0;
        int nVisible = 0;
        boolean[] logFlags = getState().getLogFlags();
        double[] coords = new double[ 3 ];
        boolean[] useMask = new boolean[ nset ];
        for ( int ip = 0; ip < np; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset; is++ ) {
                useMask[ is ] = sets[ is ].isIncluded( lp );
                use = use || useMask[ is ];
            }
            if ( use ) {
                nInclude++;
                points.getCoords( ip, coords );
                if ( inRange( coords, loBounds_, hiBounds_ ) &&
                     logize( coords, logFlags ) ) {
                    nVisible++;
                    trans.transform( coords );
                    // coords[ 2 ] = ( ( coords[ 2 ] - .5 ) / SQRT3 ) + .5;
                    for ( int is = 0; is < nset; is++ ) {
                        if ( sets[ is ].isIncluded( lp ) ) {
                            vol.plot( coords, (MarkStyle) styles[ is ] );
                        }
                    }
                }
            }
        }

        /* Plot a teeny static dot in the middle of the data. */
        vol.plot( new double[] { .5, .5, .5 },
                  MarkStyle.pointStyle( Color.black ) );

        /* Tell the volume that all the points are in for plotting.
         * This will do the painting on the graphics context if it hasn't
         * been done already. */
        vol.flush();

        /* Plot front part of bounding box. */
        if ( grid ) {
            plotAxes( g, trans, vol, true );
        }

        /* Draw any annotations. */
        annotations_.draw( g, trans, vol );

        /* Store information about the most recent plot. */
        lastVol_ = vol;
        lastTrans_ = trans;
        pointReg_ = null;

        /* Notify information about the points that were plotted.
         * I'm not sure that this has to be done outside of the paint
         * call, but it seems like the sort of thing that might be true,
         * so do it to be safe. */
        final int np1 = np;
        final int ni1 = nInclude;
        final int nv1 = nVisible;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                reportCounts( np1, ni1, nv1 );
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
        Graphics2D g2 = (Graphics2D) g;
        Object antialias = 
            g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             state_.getAntialias() 
                                 ? RenderingHints.VALUE_ANTIALIAS_ON
                                 : RenderingHints.VALUE_ANTIALIAS_DEFAULT );
        if ( ((Plot3DState) getState()).getSpherical() ) {
            plotSphericalAxes( g, trans, vol, front );
        }
        else {
            plotCartesianAxes( g, trans, vol, front );
        }
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialias );
    }

    /**
     * Draws a bounding box which contains all the points in a Cartesian plot.
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
    private void plotCartesianAxes( Graphics g, Transformer3D trans,
                                    PlotVolume vol, boolean front ) {
        Color col = g.getColor();
        boolean[] flipFlags = getState().getFlipFlags();
        for ( int i0 = 0; i0 < 8; i0++ ) {
            Corner c0 = Corner.getCorner( i0 );
            boolean[] flags0 = c0.getFlags();
            Corner[] friends = c0.getAdjacent();
            for ( int i1 = 0; i1 < friends.length; i1++ ) {
                Corner c1 = friends[ i1 ];
                boolean[] flags1 = c1.getFlags();
                if ( c1.compareTo( c0 ) > 0 ) {
                    double[] mid = new double[ 3 ];
                    for ( int j = 0; j < 3; j++ ) {
                        mid[ j ] = 0.5 * ( ( flags0[ j ] ? hiBoundsG_[ j ]
                                                         : loBoundsG_[ j ] ) 
                                         + ( flags1[ j ] ? hiBoundsG_[ j ]
                                                         : loBoundsG_[ j ] ) );
                    }
                    trans.transform( mid );
                    if ( ( mid[ 2 ] > 0.5 ) != front ) {
                        double[] p0 = new double[ 3 ];
                        double[] p1 = new double[ 3 ];
                        for ( int j = 0; j < 3; j++ ) {
                            p0[ j ] = ( flags0[ j ] ^ flipFlags[ j ] )
                                    ? hiBounds_[ j ] : loBounds_[ j ];
                            p1[ j ] = ( flags1[ j ] ^ flipFlags[ j ] )
                                    ? hiBounds_[ j ] : loBounds_[ j ];
                        }
                        assert c1 != Corner.ORIGIN;
                        if ( c0 == Corner.ORIGIN ) {
                            drawBoxAxis( g, trans, vol, p0, p1 );
                        }
                        else {
                            drawBoxLine( g, trans, vol, p0, p1 );
                        }
                    }
                }
            }
        }
        g.setColor( col );
    }

    /**
     * Draws a simple line between two points.
     *
     * @param  g      graphics context
     * @param  trans  3d transformer
     * @param  vol    plotting volume to receive the graphics
     * @param  p0     start point in data coordinates (3-element array)
     * @param  p1     end point in data coordinates (3-element array)
     */
    private void drawBoxLine( Graphics g, Transformer3D trans, PlotVolume vol,
                              double[] p0, double[] p1 ) {
        Color col = g.getColor();
        boolean[] logFlags = getState().getLogFlags();
        for ( int i = 0; i < 3; i++ ) {
            if ( logFlags[ i ] ) {
                p0[ i ] = Math.log( p0[ i ] );
                p1[ i ] = Math.log( p1[ i ] );
            }
        }
        trans.transform( p0 );
        trans.transform( p1 );
        g.setColor( Color.LIGHT_GRAY );
        g.drawLine( vol.projectX( p0[ 0 ] ), vol.projectY( p0[ 1 ] ),
                    vol.projectX( p1[ 0 ] ), vol.projectY( p1[ 1 ] ) );
        g.setColor( col );
    }

    /**
     * Draws one of the axes for the plot. 
     *
     * @param   g1      graphics context
     * @param   trans   3d transformer
     * @param   vol     plotting volume to receive the graphics
     * @param   p0      origin end of the axis in data coordinates 
     *                  (3-element array)
     * @param   p1      other end of the axis in data coordinates
     *                  (3-element array)
     */
    private void drawBoxAxis( Graphics g1, Transformer3D trans, PlotVolume vol,
                              double[] p0, double[] p1 ) {
        Graphics2D g2 = (Graphics2D) g1.create();
        g2.setColor( Color.BLACK );
        boolean[] logFlags = getState().getLogFlags();
        for ( int i = 0; i < 3; i++ ) {
            if ( logFlags[ i ] ) {
                p0[ i ] = Math.log( p0[ i ] );
                p1[ i ] = Math.log( p1[ i ] );
            }
        }

        /* First draw a line representing the axis.  In principle we could
         * do this by drawing a straight line into the transformed 
         * graphics context we're about to set up, but rounding errors
         * or something mean that the rendering isn't so good if you try
         * that, so do it the straightforward way. */
        double[] d0 = (double[]) p0.clone();
        double[] d1 = (double[]) p1.clone();
        trans.transform( d0 );
        trans.transform( d1 );
        int xp0 = vol.projectX( d0[ 0 ] );
        int yp0 = vol.projectY( d0[ 1 ] );
        int xp1 = vol.projectX( d1[ 0 ] );
        int yp1 = vol.projectY( d1[ 1 ] );
        g2.drawLine( xp0, yp0, xp1, yp1 );

        /* Which axis are we loooking at? */
        int iaxis = -1;
        for ( int i = 0; i < 3; i++ ) {
            if ( p0[ i ] != p1[ i ] ) {
                assert iaxis == -1;
                iaxis = i;
            }
        }
        assert iaxis >= 0 && iaxis < 3;

        /* Which way is up?  We need to decide on a unit vector which defines
         * the plane in which text will be written.  The direction must
         * be perpendicular to the axis but this leaves one degree of
         * freedom.  We want to choose it so that the up vector has a
         * zero component in the direction which will be perpendicular
         * to the viewing plane.  This is still undetermined to a factor
         * of -1; we may revise this later if we find out our choice 
         * has given us upside-down text. */
        double[] up = 
            Matrices.normalise( Matrices.cross( trans.getDepthVector(),
                                                Matrices.unit( iaxis ) ) );

        /* Which way is forward?  Initially choose to write along the
         * axis with lower numbers at the left hand side, but we may
         * decide to revise this if it leads to inside out text. */
        boolean forward = true;

        /* Get some relevant numbers about the dimensions of the graphics
         * space. */
        int fontHeight = g2.getFontMetrics().getHeight();
        int scale = vol.getScale();

        /* Construct a transform to apply to the graphics context which 
         * allows you to write text in a normal font in the rectangle
         * (0,0)->(sx,sy) in such a way that it will appear as a label
         * on the current axis.  It's tempting to try to map to 
         * (one dimension of) the data space somehow, but this won't
         * work because the Graphics2D object works in integers and 
         * so it might well not have the right resolution for an arbitrary
         * data axis. */
        int sx = scale;
        int sy = fontHeight;
        AffineTransform atf = null;
        for ( int itry = 0; atf == null; itry++ ) {

            /* There are a couple of conditions in the following algorithm
             * where a test is made, and if it fails an input is changed
             * and the loop runs again to get a better result.  In principle
             * this should always lead to the right result in a finite
             * number of iterations.  However there may be pathological
             * cases in which it leads to an infinite loop.  This flag
             * spots if it looks like this is happening and makes sure that
             * we just go with the possibly imperfect result we have rather
             * than hang the UI for ever. */
            boolean stopFiddling = itry > 5;

            /* Define a notional region on the graphics plane to which we
             * can plot text.  This is a rectangle based at the origin which
             * has the height of the current font and the width of the
             * relevant cube axis when it's viewed face on. */
            double[] s00 = { 0., 0. };
            double[] s10 = { sx, 0. };
            double[] s01 = { 0., sy };

            /* Define the region in 3d normalised space where the annotation
             * should actually appear. */
            double[] p00 = new double[ 3 ];
            double[] p10 = new double[ 3 ];
            double[] p01 = new double[ 3 ];
            for ( int i = 0; i < 3; i++ ) {
                p00[ i ] = forward ? p0[ i ] : p1[ i ];
                p10[ i ] = forward ? p1[ i ] : p0[ i ];
                p01[ i ] = p00[ i ] + ( hiBoundsG_[ i ] - loBoundsG_[ i ] )
                                    * fontHeight / scale
                                    * up[ i ];
            }

            /* Work out what region on the graphics plane this 3d region 
             * appears at. */
            trans.transform( p00 );
            trans.transform( p10 );
            trans.transform( p01 );
            int[] a00 = { vol.projectX( p00[ 0 ] ), vol.projectY( p00[ 1 ] ) };
            int[] a10 = { vol.projectX( p10[ 0 ] ), vol.projectY( p10[ 1 ] ) };
            int[] a01 = { vol.projectX( p01[ 0 ] ), vol.projectY( p01[ 1 ] ) };

            /* See if the text is upside down.  If so, invert the up
             * vector and try again. */
            if ( a01[ 1 ] < a00[ 1 ] && ! stopFiddling ) {
                up = Matrices.mult( up, -1. );
            }

            /* Set up coefficients for an affine transform. */
            else {
                double[] a = new double[] { a00[ 0 ], a10[ 0 ], a01[ 0 ],
                                            a00[ 1 ], a10[ 1 ], a01[ 1 ],
                                                   1,        1,        1 };
                double[] s = new double[] { s00[ 0 ], s10[ 0 ], s01[ 0 ],
                                            s00[ 1 ], s10[ 1 ], s01[ 1 ],
                                                   1,        1,        1 };
                double[] m = Matrices.mmMult( a, Matrices.invert( s ) );
                double m00 = m[ 0 ];
                double m01 = m[ 1 ];
                double m02 = m[ 2 ];
                double m10 = m[ 3 ];
                double m11 = m[ 4 ];
                double m12 = m[ 5 ];
                assert Math.abs( m[ 6 ] - 0.0 ) < 1e-6 : m[ 6 ];
                assert Math.abs( m[ 7 ] - 0.0 ) < 1e-6 : m[ 7 ];
                assert Math.abs( m[ 8 ] - 1.0 ) < 1e-6 : m[ 8 ];

                /* See if the text is inside out.  If so, flip the sense 
                 * and try again. */
                if ( m00 * m11 - m01 * m10 < 0 && ! stopFiddling ) {
                    forward = ! forward;
                }

                /* If we've got this far our coefficients are going to 
                 * give us text in the correct orientation.  Construct
                 * the transform. */
                else {
                    atf = new AffineTransform( m00, m10, m01, m11, m02, m12 );
                }
            }
        }

        /* If the determinant of the transform is zero, bail out now.
         * Although we could do some of the plotting, you won't miss 
         * much from a singular transformation and it can cause some of
         * the drawing methods to throw unpredictable errors. */
        if ( atf.getDeterminant() == 0 ) {
            return;
        }

        /* Apply the transform to the graphics context.  Subsequent text
         * written to the region (0,0)->(sx,sy) will appear alongside
         * the relevant axis now. */
        g2.transform( atf );

        /* Write the name of the axis. */
        FontMetrics fm = g2.getFontMetrics();
        ValueInfo axisInfo = state_.getAxes()[ iaxis ];
        String label = axisInfo.getName();
        String units = axisInfo.getUnitString();
        if ( units != null && units.trim().length() > 0 ) {
            label += " / " + units.trim();
        }
        g2.drawString( label, ( sx - fm.stringWidth( label ) ) / 2, 2 * sy );

        /* Plot ticks and corresponding numeric labels. */

        /* Work out where to put ticks on the axis.  Do this recursively;
         * if we find that the labels are too crowded on the axis decrease
         * the number of tick marks and try again.  Note we don't currently
         * make many concessions to selecting the right tick marks for
         * logarithmic axes, could do better. */
        boolean log = logFlags[ iaxis ];
        boolean flip = getState().getFlipFlags()[ iaxis ];
        double lo = loBounds_[ iaxis ];
        double hi = hiBounds_[ iaxis ];
        int[] tickPos = null;
        String[] tickLabels = null;
        int nTick = 0;
        int labelGap = fm.stringWidth( "99" );
        for ( int mTick = log ? 2 : 4; nTick == 0 && mTick > 0; mTick-- ) {
            AxisLabeller axer = new AxisLabeller( lo, hi, mTick );
            nTick = axer.getCount();
            tickPos = new int[ nTick ];
            tickLabels = new String[ nTick ];
            for ( int i = 0; i < nTick; i++ ) {
                double tick = axer.getTick( i );
                tickLabels[ i ] = axer.getLabel( i );
                double frac = log ? Math.log( tick / lo ) / Math.log( hi / lo )
                                  : ( tick - lo ) / ( hi - lo );
                tickPos[ i ] = (int) Math.round( sx * ( ( forward ^ flip )
                                                            ? frac
                                                            : ( 1. - frac ) ) );
                if ( i > 0 && Math.abs( tickPos[ i ] - tickPos[ i - 1 ] ) 
                              < fm.stringWidth( tickLabels[ i - 1 ] + "99" ) ) {
                    nTick = 0;
                    break;
                }
            }
        }

        /* Draw the actual tick marks and labels. */
        for ( int i = 0; i < nTick; i++ ) {
            int tpos = tickPos[ i ];
            String tlabel = tickLabels[ i ];
            g2.drawLine( tpos, -2, tpos, +2 );
            g2.drawString( tlabel, tpos - fm.stringWidth( tlabel ) / 2, sy );
        }
    }

    /**
     * Draws a spherical grid containing all the points in a spherical
     * polar plot.
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
    private void plotSphericalAxes( Graphics g, Transformer3D trans,
                                    PlotVolume vol, boolean front ) {
        Color col = g.getColor();

        /* Get the arrays of points which define all the grid lines. 
         * These points are in 3D data space.  We can then use the same
         * machinery that we use for the data points (Transformer3D object etc)
         * to transform these points into graphics coordinates and plot
         * them easily to the screen.
         * Getting a double[][][] array to represent the full grid lines is
         * slightly lazy, one could use an iterator over the points which
         * used a smaller array without recalculation of the trig by
         * exploiting symmetry, but the amount of memory used is unlikely
         * to be large, so it's not a big deal.
         *
         * An alternative approach to plotting the spherical net
         * would be to use the Graphics object's drawArc() method.
         * To do that you'd have to identify where
         * the oval representing each grid circle was in 2D and define
         * an AffineTransform to place the arc since drawArc only lets
         * you draw to an ellipse with x/y aligned major/minor axes.
         * This would possibly be faster, and it should give
         * much better rendering to non-pixel-based Graphics contexts
         * (PostScript) - the grid lines are currently, and unavoidably,
         * rather wobbly.  However, it's not trivial to do. */
        double[][][] grid = getSphericalGrid( vol );
        int nline = grid.length;

        /* Iterate over all grid lines. */
        for ( int iline = 0; iline < nline; iline++ ) {
            g.setColor( front ? Color.DARK_GRAY : Color.LIGHT_GRAY );
            double[][] line = grid[ iline ];
            int np = line.length;
            int[] xp = new int[ np ];
            int[] yp = new int[ np ];
            int ipoint = 0;
            double[] point = new double[ 3 ];

            /* Iterate over all points in the current grid line. */
            for ( int ip = 0; ip < np; ip++ ) {

                /* Acquire and transform the next point defining the current
                 * grid line. */
                System.arraycopy( line[ ip ], 0, point, 0, 3 );
                trans.transform( point );

                /* If the point is plottable (it's in front/behind as required)
                 * then add its graphics coordinates to the list of points
                 * which will form the line to be drawn. */
                if ( ( point[ 2 ] < 0.5 == front ) || point[ 2 ] == 0.5 ) {
                    xp[ ipoint ] = vol.projectX( point[ 0 ] );
                    yp[ ipoint ] = vol.projectY( point[ 1 ] );
                    ipoint++;
                }
                else {

                    /* Otherwise, see if we have a list of points ready 
                     * to draw (this will happen if this is the first point
                     * which goes out of range). */
                    if ( ipoint > 0 ) {

                        /* If so add one extra point, so we don't get gaps
                         * at the boundaries. */
                        if ( ipoint < np ) {
                            xp[ ipoint ] = vol.projectX( point[ 0 ] );
                            yp[ ipoint ] = vol.projectY( point[ 1 ] );
                            ipoint++;
                        }

                        /* Then draw the line from the points thus far
                         * assembled. */
                        g.drawPolyline( xp, yp, ipoint );
                        ipoint = 0;
                    }
                }
            }

            /* End of the current grid line.  If we have any points ready
             * to draw, do it now. */
            if ( ipoint > 0 ) {
                if ( ipoint < np ) {
                    xp[ ipoint ] = vol.projectX( point[ 0 ] );
                    yp[ ipoint ] = vol.projectY( point[ 1 ] );
                    ipoint++;
                }
                g.drawPolyline( xp, yp, ipoint );
                ipoint = 0;
            }
        }
        g.setColor( col );
    }

    /**
     * Returns an array of points defining the grid lines which outline
     * a sphere in data space for this plot.  The returned array is an
     * array of double[][]s, each of which represents a line and is
     * an array of double[3]s each of which represents a point in 3-d
     * data space.  For each line, the first point in the array is
     * the same as the last, which simplifies plotting a bit.
     *
     * @param    vol   plotting volume
     * @return   double[][][3] array of points giving grid lines for a
     *           spherical net 
     */
    private double[][][] getSphericalGrid( PlotVolume vol ) {

        /* Only populate the grid if we don't have a suitable one already
         * prepared. */
        if ( sphereGrid_ == null || vol != lastVol_ ) {
            final int nLat = LATITUDE_GRID_LINES;
            final int nLong = LONGITUDE_GRID_LINES;
            int scale = vol.getScale();
            double radius = hiBoundsG_[ 0 ];

            /* Set the tolerance for how far we are prepared to deviate
             * from an ideal ellipse, which determines how many line 
             * segments we need.  This is in graphics coordinates;
             * on the screen that means pixels, so 0.5 should be 
             * indistinguishable from any smaller number.  Some fairly 
             * straightforward trig will show that the number of 
             * straight line segments required to approximate a circle 
             * of radius r in which no point should deviate more than
             * e from its ideal position is:
             *    2 * PI * sqrt( r / e )
             */
            final double tolerance = 0.5;

            /* Allocate an array with one entry for each grid line. */
            double[][][] grid = new double[ nLong + nLat * 2 + 1 ][][];

            /* In each case, the circle is split into 4 quadrants for
             * the calculations, since they are equivalent to each other
             * by simple cartesian symmetry operations.  This reduces the
             * number of expensive trig calculations which are required. */

            /* Longitude lines and equator. */
            {
                int np = (int) Math.ceil( 2.0 * Math.PI / 4.0 
                                          * Math.sqrt( scale / tolerance ) );
                for ( int il = 0; il < nLong + 1; il++ ) {
                    grid[ il ] = new double[ np * 4 + 1 ][];
                }
                double dTheta = 2.0 * Math.PI / 4.0 / np;
                double dPhi = 2.0 * Math.PI / nLong;
                double[] cosPhis = new double[ nLong ];
                double[] sinPhis = new double[ nLong ];
                for ( int iLong = 0; iLong < nLong; iLong++ ) {
                    double phi = iLong * dPhi;
                    cosPhis[ iLong ] = Math.cos( phi );
                    sinPhis[ iLong ] = Math.sin( phi );
                }
                for ( int ip = 0; ip <= np; ip++ ) {
                    double theta = ip * dTheta;
                    double cosTheta = Math.cos( theta );
                    double sinTheta = Math.sin( theta );
                    for ( int iLong = 0; iLong < nLong; iLong++ ) {
                        int il = iLong;
                        double x = radius * ( cosTheta * cosPhis[ il ] );
                        double y = radius * ( cosTheta * sinPhis[ il ] );
                        double z = radius * ( sinTheta );
                        int ip0 = 0 * np + ip;
                        int ip1 = 2 * np - ip - 1;
                        int ip2 = 2 * np + ip;
                        int ip3 = 4 * np - ip - 1;
                        grid[ il ][ ip0 ] = new double[] { +x, +y, +z };
                        grid[ il ][ ip1 ] = new double[] { -x, -y, +z };
                        grid[ il ][ ip2 ] = new double[] { -x, -y, -z };
                        grid[ il ][ ip3 ] = new double[] { +x, +y, -z };
                    }
                    double x = radius * cosTheta;
                    double y = radius * sinTheta;
                    int ip0 = 0 * np + ip;
                    int ip1 = 2 * np - ip - 1;
                    int ip2 = 2 * np + ip;
                    int ip3 = 4 * np - ip - 1;
                    int il = nLong;
                    grid[ il ][ ip0 ] = new double[] { +x, +y, 0 };
                    grid[ il ][ ip1 ] = new double[] { -x, +y, 0 };
                    grid[ il ][ ip2 ] = new double[] { -x, -y, 0 };
                    grid[ il ][ ip3 ] = new double[] { +x, -y, 0 };
                }
            }

            /* Latitude lines. */
            {
                double dTheta = Math.PI / 2.0 / ( nLat + 1 );
                for ( int iLat = 0; iLat < nLat; iLat++ ) {
                    double theta = ( iLat + 1 ) * dTheta;
                    double cosTheta = Math.cos( theta );
                    double sinTheta = Math.sin( theta );
                    double r = radius * sinTheta;
                    int np = (int)
                        Math.ceil( 2.0 * Math.PI / 4.0 
                                   * Math.sqrt( scale *
                                                cosTheta / tolerance ) );
                    double dPhi = 2.0 * Math.PI / 4.0 / np;
                    for ( int iup = 0; iup < 2; iup++ ) {
                        int il = nLong + 1 + iLat * 2 + iup;
                        grid[ il ] = new double[ np * 4 + 1 ][];
                    }
                    for ( int ip = 0; ip < np; ip++ ) {
                        double phi = ip * dPhi;
                        double x = r * Math.cos( phi );
                        double y = r * Math.sin( phi );
                        int ip0 = 0 * np + ip;
                        int ip1 = 2 * np - ip - 1;
                        int ip2 = 2 * np + ip;
                        int ip3 = 4 * np - ip - 1;
                        for ( int iup = 0; iup < 2; iup++ ) {
                            int il = nLong + 1 + iLat * 2 + iup;
                            double z = iup == 0 ? + radius * cosTheta
                                                : - radius * cosTheta;
                            grid[ il ][ ip0 ] = new double[] { +x, +y, +z };
                            grid[ il ][ ip1 ] = new double[] { -x, +y, +z };
                            grid[ il ][ ip2 ] = new double[] { -x, -y, +z };
                            grid[ il ][ ip3 ] = new double[] { +x, -y, +z };
                        }
                    }
                }
            }

            /* For each line, set the last point equal to the first. */
            for ( int il = 0; il < grid.length; il++ ) {
                double[][] line = grid[ il ];
                assert line[ line.length - 1 ] == null;
                line[ line.length - 1 ] = line[ 0 ];
            }
            sphereGrid_ = grid;
        }
        return sphereGrid_;
    }

    /**
     * This component calls this method following a repaint with the
     * values of the number of points that were plotted.
     * It is intended as a hook for clients which want to know that 
     * information in a way which is kept up to date.
     * The default implementation does nothing.
     *
     * @param   nPoint  total number of points available
     * @param   nIncluded  number of points included in marked subsets
     * @param   nVisible  number of points actually plotted (may be less
     *          nIncluded if some are out of bounds)
     */
    protected void reportCounts( int nPoint, int nIncluded, int nVisible ) {
    }

    /**
     * Returns a PointRegistry which knows where all the points are
     * plotted on the current view.  The registry is returned in a ready
     * state.
     *
     * @return  point registry
     */
    public PointRegistry getPointRegistry() {
        if ( pointReg_ == null ) {
            PlotVolume vol = lastVol_;
            Transformer3D trans = lastTrans_;
            Points points = getPoints();
            PointRegistry reg = new PointRegistry();
            if ( vol != null && points != null && points != null ) {
                boolean[] logFlags = getState().getLogFlags();
                int np = points.getCount();
                RowSubset[] sets = getPointSelection().getSubsets();
                int nset = sets.length;
                double[] coords = new double[ 3 ];
                for ( int ip = 0; ip < np; ip++ ) {
                    long lp = (long) ip; 
                    boolean use = false;
                    for ( int is = 0; is < nset && ! use; is++ ) {
                        use = use || sets[ is ].isIncluded( lp );
                    }
                    if ( use ) {
                        points.getCoords( ip, coords );
                        if ( inRange( coords, loBounds_, hiBounds_ ) &&
                             logize( coords, logFlags ) ) {
                            trans.transform( coords );
                            Point p = new Point( vol.projectX( coords[ 0 ] ),
                                                 vol.projectY( coords[ 1 ] ) );
                            reg.addPoint( ip, p );
                        }
                    }
                }
            }
            reg.ready();
            pointReg_ = reg;
        }
        return pointReg_;
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
    private static boolean logize( double[] coords, boolean[] logFlags ) {
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
     * Transforms points in 3d data space to points in 3d graphics space. 
     */
    private static class Transformer3D {

        final double[] loBounds_;
        final double[] factors_;
        final double[] rot_;

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
         */
        Transformer3D( double[] rotation, double[] loBounds,
                       double[] hiBounds ) {
            rot_ = (double[]) rotation.clone();
            loBounds_ = new double[ 3 ];
            factors_ = new double[ 3 ];
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
            boolean[] logFlags = getState().getLogFlags();
            for ( int i = 0; i < activePoints_.length; i++ ) {
                double[] coords = new double[ 3 ];
                getPoints().getCoords( activePoints_[ i ], coords );
                if ( logize( coords, logFlags ) ) {
                    trans.transform( coords );
                    cursorStyle_.drawMarker( g, vol.projectX( coords[ 0 ] ),
                                                vol.projectY( coords[ 1 ] ) );
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
