package uk.ac.starlink.ttools.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import uk.ac.starlink.util.WrapUtils;

/**
 * Plot3D which works with spherical polar coordinates.
 *
 * @author   Mark Taylor
 * @since    23 Mar 2007
 */
public class SphericalPlot3D extends Plot3D {

    private double[][][] sphereGrid_;
    private double lastRadius_;
    private int lastScale_;

    /** Number of grid lines above (and below) the equator for sphere grid. */
    private static final int LATITUDE_GRID_LINES = 2;

    /** Number of longitude lines for sphere grid. */
    private static final int LONGITUDE_GRID_LINES = 12;

    /** Padding round the edge of the sphere in the graphics component. */
    private static final int SPHERE_PAD = 8;

    /**
     * Constructor.
     */
    public SphericalPlot3D() {
        super();
    }

    protected RangeChecker configureRanges( Plot3DState state ) {
        double rmax = state.getRanges()[ 0 ][ 1 ];
        loBounds_ = new double[ 3 ];
        hiBounds_ = new double[ 3 ];
        Arrays.fill( loBounds_, -rmax );
        Arrays.fill( hiBounds_, +rmax );
        loBoundsG_ = (double[]) loBounds_.clone();
        hiBoundsG_ = (double[]) hiBounds_.clone();
        final double range2 = rmax * rmax * 1.0001;
        return new RangeChecker() {
            boolean inRange( double[] coords ) {
                return ( coords[ 0 ] * coords[ 0 ] +
                         coords[ 1 ] * coords[ 1 ] +
                         coords[ 2 ] * coords[ 2 ] ) <= range2;
            }
        };
    }

    protected double getPadding( Plot3DState state, Graphics g, 
                                 int[] padBorders ) {
        Arrays.fill( padBorders, SPHERE_PAD );

        /* Make room for an axis if we need one. */
        SphericalPlotState sstate = (SphericalPlotState) state;
        if ( sstate.getRadialInfo() != null &&
             sstate.getZoomScale() == 1.0 ) {
            padBorders[ annotateAtSide() ? 0 : 2 ]
               += g.getFontMetrics().getHeight() * 2 + SPHERE_PAD;
        }
        return 1.0;
    }

    /**
     * Data bounds include one range for the radial coordinate; ranges
     * on the other axes aren't much use.  The radial one will just be
     * between [0..1] if no radial coordinate has been chosen.
     * Ranges for any auxiliary axes are also added.
     */
    public DataBounds calculateBounds( PlotData data, PlotState state ) {
        boolean hasRadial =
            ((SphericalPlotState) state).getRadialInfo() != null;

        /* Set up blank range objects. */
        int ndim = data.getNdim();
        int naux = ndim - 3;
        Range[] auxRanges = new Range[ naux ];
        for ( int iaux = 0; iaux < naux; iaux++ ) {
            auxRanges[ iaux ] = new Range();
        }

        /* Submit each data point which will be plotted to the ranges as
         * appropriate. */
        int nset = data.getSetCount();
        int[] npoints = new int[ nset ];
        PointSequence pseq = data.getPointSequence();
        int ip = 0;
        double r2max = 0.0;
        while ( pseq.next() ) {
            boolean isUsed = false;
            for ( int iset = 0; iset < nset; iset++ ) {
                if ( pseq.isIncluded( iset ) ) {
                    isUsed = true;
                    npoints[ iset ]++;
                }
            }
            if ( isUsed ) {
                if ( hasRadial || naux > 0 ) {
                    double[] coords = pseq.getPoint();
                    boolean isValid = true;
                    for ( int idim = 0; idim < 3 && isValid; idim++ ) {
                        isValid = isValid
                               && ( ! Double.isNaN( coords[ idim ] ) &&
                                    ! Double.isInfinite( coords[ idim ] ) );
                    }
                    if ( isValid ) {
                        if ( hasRadial ) {
                            double r2 = coords[ 0 ] * coords[ 0 ]
                                      + coords[ 1 ] * coords[ 1 ]
                                      + coords[ 2 ] * coords[ 2 ];
                            if ( r2 > r2max ) {
                                r2max = r2;
                            }
                        }
                        for ( int iaux = 0; iaux < naux; iaux++ ) {
                            auxRanges[ iaux ].submit( coords[ iaux + 3 ] );
                        }
                        ip++;
                    }
                }

                /* If there are no radial coords and no aux axes we hardly
                 * need to look at the coordinates - just count points for
                 * efficiency. */
                else {
                    ip++;
                }
            }
        }
        pseq.close();
        double rmax = hasRadial ? Math.sqrt( r2max ) : 1.0;

        /* Assemble and return data bounds object. */
        Range[] ranges = new Range[ 1 + naux ];
        ranges[ 0 ] = new Range( 0.0, rmax );
        for ( int iaux = 0; iaux < naux; iaux++ ) {
            ranges[ 1 + iaux ] = auxRanges[ iaux ];
        }
        return new DataBounds( ranges, ip, npoints );
    }

    protected boolean frontOnly( Plot3DState state ) {
        SphericalPlotState sstate = (SphericalPlotState) state;
        return sstate.getZoomScale() > 2
            && sstate.getRadialInfo() == null;
    }

    protected boolean[] get3DLogFlags() {
        return new boolean[ 3 ];
    }

    protected void plotAxes( Plot3DState state, Graphics g, Transformer3D trans,
                             PlotVolume vol, boolean front ) {

        /* Decline to plot axes if there's a large zoom: for one thing
         * you can't mostly see them and for another there could be
         * problems with the plotting when the points are
         * way off screen. */
        if ( state.getZoomScale() > 100 ) {
            return;
        }

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
        int scale = vol.getScale();
        double radius = hiBoundsG_[ 0 ];
        if ( sphereGrid_ == null ||
             scale != lastScale_ || radius != lastRadius_ ) {
            sphereGrid_ = calculateSphericalGrid( scale, radius );
            lastScale_ = scale;
            lastRadius_ = radius;
        }
        double[][][] grid = sphereGrid_;

        /* Iterate over all grid lines. */
        int nline = grid.length;
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

        /* Now draw a single horizontal axis to indicate the range of
         * the radial coordinate. */
        SphericalPlotState sstate = ((SphericalPlotState) getState());
        if ( sstate.getRadialInfo() != null && sstate.getZoomScale() == 1.0 ) {
            boolean log = sstate.getRadialLog();
            double[] d0 = new double[] { 0.0, 0.0, 0.0 };
            trans.transform( d0 );
            int xp0 = vol.projectX( d0[ 0 ] );
            int yp0 = vol.projectY( d0[ 1 ] );
            int xp1 = xp0 + scale / 2;
            int yp1 = yp0 + scale / 2 + SPHERE_PAD;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor( Color.BLACK );
            if ( annotateAtSide() ) {
                g2.rotate( Math.PI / 2, xp0, yp0 );
            }
            g2.translate( xp0, yp1 );
            g2.drawLine( 0, 0, xp1 - xp0, 0 );
            double rad = hiBounds_[ 0 ];
            new AxisLabeller( sstate.getAxisLabels()[ 0 ],
                              log ? Math.exp( 0.0 ) : 0.0,
                              log ? Math.exp( rad ) : rad,
                              xp1 - xp0, log, false, g2.getFontMetrics(),
                              AxisLabeller.X, 6, xp1 - xp0, SPHERE_PAD )
               .annotateAxis( g2 );
        }
    }

    /**     
     * Decides whether, other thiings being equal, it's better to put
     * annotation information at the bottom of the plotting area or at
     * the side.
     *      
     * @return   true if annotations should be at the side,
     *           false if they should be at the top/bottom
     */     
    private boolean annotateAtSide() {
        Rectangle plotBounds = getPlotBounds();
        return plotBounds.width - plotBounds.height > SPHERE_PAD * 2;
    }

    /**
     * Returns an array of points defining the grid lines which outline
     * a sphere in data space for this plot.  The returned array is an
     * array of double[][]s, each of which represents a line and is
     * an array of double[3]s each of which represents a point in 3-d
     * data space.  For each line, the first point in the array is
     * the same as the last, which simplifies plotting a bit.
     *
     * @param    scale   scaling constant for the plotting volume
     * @param    radius  radius of the sphere
     * @return   double[][][3] array of points giving grid lines for a
     *           spherical net
     */
    private static double[][][] calculateSphericalGrid( int scale,
                                                        double radius ) {

        final int nLat = LATITUDE_GRID_LINES;
        final int nLong = LONGITUDE_GRID_LINES;

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
                               * Math.sqrt( scale * cosTheta / tolerance ) );
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

        /* Return. */
        return grid;
    }
}
