package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Plot3D which works with Cartesian coordinates.
 *
 * @author   Mark Taylor
 * @since    23 Mar 2007
 */
public class CartesianPlot3D extends Plot3D {

    /**
     * Constructor.
     */
    public CartesianPlot3D() {
        super();
    }

    protected RangeChecker configureRanges( Plot3DState state ) {
        loBounds_ = new double[ 3 ];
        hiBounds_ = new double[ 3 ];
        loBoundsG_ = new double[ 3 ];
        hiBoundsG_ = new double[ 3 ];
        for ( int i = 0; i < 3; i++ ) {
            double lo = state.getRanges()[ i ][ 0 ];
            double hi = state.getRanges()[ i ][ 1 ];
            loBounds_[ i ] = lo;
            hiBounds_[ i ] = hi;
            boolean log = state.getLogFlags()[ i ];
            boolean flip = state.getFlipFlags()[ i ];
            double gLo = log ? Math.log( lo ) : lo;
            double gHi = log ? Math.log( hi ) : hi;
            loBoundsG_[ i ] = flip ? gHi : gLo;
            hiBoundsG_[ i ] = flip ? gLo : gHi;
        }
        return new RangeChecker() {
            boolean inRange( double[] coords ) {
                return coords[ 0 ] >= loBounds_[ 0 ]
                    && coords[ 1 ] >= loBounds_[ 1 ]
                    && coords[ 2 ] >= loBounds_[ 2 ]
                    && coords[ 0 ] <= hiBounds_[ 0 ]
                    && coords[ 1 ] <= hiBounds_[ 1 ]
                    && coords[ 2 ] <= hiBounds_[ 2 ];
            }
        };
    }

    protected double getPadding( Plot3DState state, Graphics g,
                                 int[] padBorders ) {
        padBorders[ 0 ] = 2;
        padBorders[ 1 ] = 2;
        padBorders[ 2 ] = 2;
        padBorders[ 3 ] = 2;

        /* Since the cube may get rotated, we need to make sure that
         * its longest diagonal can be accommodated normal to the 
         * line of view. */
        return Math.sqrt( 3. );
    }

    protected boolean frontOnly( Plot3DState state ) {
        return false;
    }

    protected boolean[] get3DLogFlags() {
        return getState().getLogFlags();
    }

    protected void plotAxes( Plot3DState state, Graphics g, Transformer3D trans,
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
                            drawBoxAxis( state, g, trans, vol, p0, p1 );
                        }
                        else {
                            drawBoxLine( state, g, trans, vol, p0, p1 );
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
     * @param  state  plot state
     * @param  g      graphics context
     * @param  trans  3d transformer
     * @param  vol    plotting volume to receive the graphics
     * @param  p0     start point in data coordinates (3-element array)
     * @param  p1     end point in data coordinates (3-element array)
     */
    private void drawBoxLine( Plot3DState state, Graphics g,
                              Transformer3D trans, PlotVolume vol,
                              double[] p0, double[] p1 ) {
        Color col = g.getColor();
        boolean[] logFlags = state.getLogFlags();
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
     * @param   state   plot state
     * @param   g1      graphics context
     * @param   trans   3d transformer
     * @param   vol     plotting volume to receive the graphics
     * @param   p0      origin end of the axis in data coordinates
     *                  (3-element array)
     * @param   p1      other end of the axis in data coordinates
     *                  (3-element array)
     */
    private void drawBoxAxis( Plot3DState state, Graphics g1,
                              Transformer3D trans, PlotVolume vol,
                              double[] p0, double[] p1 ) {
        Graphics2D g2 = (Graphics2D) g1.create();
        g2.setColor( Color.BLACK );
        boolean[] logFlags = state.getLogFlags();
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

        /* Draw the annotated axis. */
        new AxisLabeller( state.getAxisLabels()[ iaxis ],
                          loBounds_[ iaxis ], hiBounds_[ iaxis ], sx,
                          logFlags[ iaxis ],
                          ( ! forward ) ^ state.getFlipFlags()[ iaxis ],
                          g2.getFontMetrics(), AxisLabeller.X, 4, 24, 24 )
           .annotateAxis( g2 );
    }
}
