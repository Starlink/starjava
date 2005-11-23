package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import javax.swing.JComponent;
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
        setPreferredSize( new Dimension( 300, 300 ) );
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
     * Performs the painting - this method does the acutal work.
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
        g.setColor( Color.BLACK );
        Object antialias = 
            g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             state_.getAntialias() 
                                 ? RenderingHints.VALUE_ANTIALIAS_ON
                                 : RenderingHints.VALUE_ANTIALIAS_OFF );
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
                        double[] rot0 = new double[ 3 ];
                        double[] rot1 = new double[ 3 ];
                        for ( int j = 0; j < 3; j++ ) {
                            rot0[ j ] = flags0[ j ] ? hiBounds_[ j ]
                                                    : loBounds_[ j ];
                            rot1[ j ] = flags1[ j ] ? hiBounds_[ j ]
                                                    : loBounds_[ j ];
                        }
                        trans.transform( rot0 );
                        trans.transform( rot1 );
                        int xp0 = vol.projectX( rot0[ 0 ] );
                        int yp0 = vol.projectY( rot0[ 1 ] );
                        int xp1 = vol.projectX( rot1[ 0 ] );
                        int yp1 = vol.projectY( rot1[ 1 ] );
                        g.drawLine( vol.projectX( rot0[ 0 ] ),
                                    vol.projectY( rot0[ 1 ] ),
                                    vol.projectX( rot1[ 0 ] ),
                                    vol.projectY( rot1[ 1 ] ) );
                    }
                }
            }
        }
        g.setColor( col );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, antialias );
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
         * @param    rotation  9-element rotation matrix
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
    }
}
