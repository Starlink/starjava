package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import javax.swing.JComponent;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which paints a 3d plot.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public class Plot3D extends JComponent {

    private Points points_;
    private Plot3DState state_;
    private double[] loBounds_;
    private double[] hiBounds_;

    private static final double SQRT3 = Math.sqrt( 3.0 );

    /**
     * Constructor.
     */
    public Plot3D() {
        setBackground( Color.white );
        setOpaque( true );
        setPreferredSize( new Dimension( 400, 400 ) );
        setBorder( javax.swing.BorderFactory.createLineBorder( Color.GRAY ) );
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     *
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        points_ = points;
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
     * Resets the scaling so that all the data points known by this plot
     * will be visible at all rotations.
     */
    public void rescale() {
        double[] loBounds = new double[ 3 ];
        double[] hiBounds = new double[ 3 ];
        Arrays.fill( loBounds, Double.MAX_VALUE );
        Arrays.fill( hiBounds, -Double.MAX_VALUE );
        Points points = getPoints();
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
                if ( ! Double.isNaN( coords[ 0 ] ) &&
                     ! Double.isNaN( coords[ 1 ] ) &&
                     ! Double.isNaN( coords[ 2 ] ) ) {
                    for ( int id = 0; id < 3; id++ ) {
                        loBounds[ id ] = Math.min( loBounds[ id ],
                                                   coords[ id ] );
                        hiBounds[ id ] = Math.max( hiBounds[ id ],
                                                   coords[ id ] );
                    }
                }
            }
        }
        loBounds_ = loBounds;
        hiBounds_ = hiBounds;
    }

    /**
     * Performs the painting - this method does the actual work.
     */
    protected void paintComponent( Graphics g ) {

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
            new Transformer3D( state.getRotation(), loBounds_, hiBounds_);

        /* Set up a plotting volume to render the 3-d points. */
        PlotVolume vol = new SortPlotVolume( this, g );
        vol.getDepthTweaker().setFogginess( state_.getFogginess() );

        /* Plot back part of bounding box. */
        plotAxes( g, trans, vol, false );

        /* Submit each point for drawing in the display volume as
         * appropriate. */
        int np = points.getCount();
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
                points.getCoords( ip, coords );
                if ( ! Double.isNaN( coords[ 0 ] ) &&
                     ! Double.isNaN( coords[ 1 ] ) && 
                     ! Double.isNaN( coords[ 2 ] ) ) {
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
        plotAxes( g, trans, vol, true );
    }

    /**
     * Draws a bounding box which contains all the known points.
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
        Color col = g.getColor();
        Object antialias = 
            g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             state_.getAntialias() 
                                 ? RenderingHints.VALUE_ANTIALIAS_ON
                                 : RenderingHints.VALUE_ANTIALIAS_DEFAULT );
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
                        mid[ j ] = 0.5 * ( ( flags0[ j ] ? hiBounds_[ j ]
                                                         : loBounds_[ j ] ) 
                                         + ( flags1[ j ] ? hiBounds_[ j ]
                                                         : loBounds_[ j ] ) );
                    }
                    trans.transform( mid );
                    if ( ( mid[ 2 ] > 0.5 ) != front ) {
                        double[] p0 = new double[ 3 ];
                        double[] p1 = new double[ 3 ];
                        for ( int j = 0; j < 3; j++ ) {
                            p0[ j ] = flags0[ j ] ? hiBounds_[ j ]
                                                  : loBounds_[ j ];
                            p1[ j ] = flags1[ j ] ? hiBounds_[ j ] 
                                                  : loBounds_[ j ];
                        }
                        assert c1 != Corner.ORIGIN;
                        if ( c0 == Corner.ORIGIN ) {
                            drawAxis( g, trans, vol, p0, p1 );
                        }
                        else {
                            drawBoxLine( g, trans, vol, p0, p1 );
                        }
                    }
                }
            }
        }
        g.setColor( col );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialias );
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
    private void drawAxis( Graphics g1, Transformer3D trans, PlotVolume vol,
                           double[] p0, double[] p1 ) {
        Graphics2D g2 = (Graphics2D) g1.create();
        g2.setColor( Color.BLACK );

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
        assert iaxis != -1;

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
        while ( atf == null ) {

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
                p01[ i ] = p00[ i ] + ( hiBounds_[ i ] - loBounds_[ i ] ) 
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
            if ( a01[ 1 ] < a00[ 1 ] ) {
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
                assert Math.abs( m[ 6 ] - 0.0 ) < 1e-6;
                assert Math.abs( m[ 7 ] - 0.0 ) < 1e-6;
                assert Math.abs( m[ 8 ] - 1.0 ) < 1e-6;

                /* See if the text is inside out.  If so, flip the sense 
                 * and try again. */
                if ( m00 * m11 - m01 * m10 < 0 ) {
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
         * the number of tick marks and try again. */
        double lo = loBounds_[ iaxis ];
        double hi = hiBounds_[ iaxis ];
        int[] tickPos = null;
        String[] tickLabels = null;
        int nTick = 0;
        int labelGap = fm.stringWidth( "99" );
        for ( int mTick = 4; nTick == 0 && mTick > 0; mTick-- ) {
            AxisLabeller axer = new AxisLabeller( lo, hi, mTick );
            nTick = axer.getCount();
            tickPos = new int[ nTick ];
            tickLabels = new String[ nTick ];
            for ( int i = 0; i < nTick; i++ ) {
                double tick = axer.getTick( i );
                tickLabels[ i ] = axer.getLabel( i );
                double frac = ( tick - lo ) / ( hi - lo );
                tickPos[ i ] =
                    (int) Math.round( sx * ( forward ? frac : ( 1. - frac ) ) );
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
            g2.drawString( tlabel, tpos - fm.stringWidth( tlabel ), sy );
        }
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
                loBounds_[ i ] = lo;
                factors_[ i ] = 1.0 / Math.abs( hi - lo );
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
}
