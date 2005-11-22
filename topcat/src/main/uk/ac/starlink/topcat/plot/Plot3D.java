package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
            new Transformer3D( state.getTheta(), state.getPhi(),
                               loBounds_, hiBounds_);

        /* Set up a plotting volume to render the 3-d points. */
        PlotVolume vol = new SortPlotVolume( this, g );

        /* Plot bounding box. */
        plotAxes( g, trans, vol );

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
    }

    private static void plotAxes( Graphics g, Transformer3D trans,
                                  PlotVolume vol ) {
        double[][] points = new double[][] {
        };
    }

    /**
     * Transforms points in 3d data space to points in 3d graphics space. 
     */
    private static class Transformer3D {

        final double[] loBounds_;
        final double[] factors_;
        final double cosTheta_;
        final double sinTheta_;
        final double cosPhi_;
        final double sinPhi_;

        /**
         * Constructs a transformer.  A cuboid of interest in data space
         * is specified by the <code>loBounds</code> and <code>hiBounds</code>
         * arrays.  When the <code>transform()</code> method is called
         * on a point within this region, it will transform it to
         * lie in a unit sphere centred on (0.5, 0.5, 0.5) and hence,
         * <i>a fortiori</i>, in the unit cube.
         *
         * @param    theta   zenithal angle of rotation
         * @param    phi     azimuthal angle of rotation
         * @param    loBounds  lower bounds of cuboid of interest (xlo,ylo,zlo)
         * @param    hiBounds  upper bounds of cuboid of interest (xhi,yhi,zhi)
         */
        Transformer3D( double theta, double phi,
                       double[] loBounds, double[] hiBounds ) {
            cosTheta_ = Math.cos( theta );
            sinTheta_ = Math.sin( theta );
            cosPhi_ = Math.cos( phi );
            sinPhi_ = Math.sin( phi );
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

            /* Rotate by theta around the x axis. */
            double y0 = coords[ 1 ];
            double z0 = coords[ 2 ];
            coords[ 1 ] = cosTheta_ * y0 - sinTheta_ * z0;
            coords[ 2 ] = sinTheta_ * y0 + cosTheta_ * z0;

            /* Rotate by phi around the y axis. */
            double x1 = coords[ 0 ];
            double z1 = coords[ 2 ];
            coords[ 0 ] = cosPhi_ * x1 - sinPhi_ * z1;
            coords[ 2 ] = sinPhi_ * x1 + cosPhi_ * z1;

            /* Shift the origin so the unit sphere is centred at (.5,.5,.5). */
            for ( int i = 0; i < 3; i++ ) {
                coords[ i ] += 0.5;
            }
        }
    }
}
