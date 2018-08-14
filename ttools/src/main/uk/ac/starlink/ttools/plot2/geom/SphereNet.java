package uk.ac.starlink.ttools.plot2.geom;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Calculates line segments forming longitude and latitude lines
 * for a sphere around the data origin of a cube surface.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2018
 */
public class SphereNet {

    private final CubeSurface surf_;
    private final int gsize_;
    private final double[] dlos_;
    private final double[] dhis_;

    /**
     * Constructor.
     *
     * @param  surf  surface on which grid lines will be specified
     */
    public SphereNet( CubeSurface surf ) {
        surf_ = surf;
        Rectangle bounds = surf_.getPlotBounds();
        gsize_ = Math.max( bounds.width, bounds.height );
        double[] xlims = surf.getDataLimits( 0 );
        double[] ylims = surf.getDataLimits( 1 );
        double[] zlims = surf.getDataLimits( 2 );
        dlos_ = new double[] { xlims[ 0 ], ylims[ 0 ], zlims[ 0 ] };
        dhis_ = new double[] { xlims[ 1 ], ylims[ 1 ], zlims[ 1 ] };
    }

    /**
     * Returns a reasonable radius for the sphere if none is explicitly
     * specified.  The general idea is to have something that will be
     * visible.  If the data origin is within the visible region, then
     * the largest sphere that will fit is used, otherwise it's a sphere
     * for which at least part of the surface appears in the visible region.
     *
     * @return   reasonable radius for sphere in data units
     */
    public double getDefaultRadius() {
        if ( containsOrigin() ) {
            double r = Double.POSITIVE_INFINITY;
            for ( int i = 0; i < 3; i++ ) {
                r = Math.min( r, Math.abs( dlos_[ i ] ) );
                r = Math.min( r, Math.abs( dhis_[ i ] ) );
            }
            return r;
        }
        else {
            double r = 0;
            for ( int i = 0; i < 3; i++ ) {
                r = Math.max( r, Math.abs( dlos_[ i ] ) );
                r = Math.max( r, Math.abs( dhis_[ i ] ) );
            }
            return r;
        }
    }

    /**
     * Returns an array of line specifications representing lines of
     * longitude.
     *
     * @param  dRadius  radius in data units
     * @param  nLon     number of equally spaced great circles required;
     *                  note this is half the number of meridians
     * @return   <code>nLon</code>-element array of meridian specifiers
     */
    public Line3d[] getLongitudeLines( double dRadius, int nLon ) {
        double dsize = 1.0 / unitNorm( 2 );

        /* Allocate arrays for the point positions.  Each great circle
         * has the same number of entries, which means we can reuse
         * some calculations. */
        double[][][] grid = new double[ nLon ][][];
        int np = getQuadrantPointCount( dsize, dRadius );
        for ( int il = 0; il < nLon; il++ ) {
            grid[ il ] = new double[ np * 4 + 1 ][];
        }
        double dTheta = 2.0 * Math.PI / 4.0 / np;
        double dPhi = Math.PI / nLon;
        double[] cosPhis = new double[ nLon ];
        double[] sinPhis = new double[ nLon ];
        for ( int iLon = 0; iLon < nLon; iLon++ ) {
            double phi = iLon * dPhi;
            cosPhis[ iLon ] = Math.cos( phi );
            sinPhis[ iLon ] = Math.sin( phi );
        }

        /* Populate the arrays using symmetry; each circle is split into
         * four quadrants which can reuse the same trig calculations. */
        for ( int ip = 0; ip <= np; ip++ ) {
            double theta = ip * dTheta;
            double cosTheta = Math.cos( theta );
            double sinTheta = Math.sin( theta );
            for ( int il = 0; il < nLon; il++ ) {
                double x = dRadius * ( cosTheta * cosPhis[ il ] );
                double y = dRadius * ( cosTheta * sinPhis[ il ] );
                double z = dRadius * ( sinTheta );
                int ip0 = 0 * np + ip;
                int ip1 = 2 * np - ip - 1;
                int ip2 = 2 * np + ip;
                int ip3 = 4 * np - ip - 1;
                grid[ il ][ ip0 ] = new double[] { +x, +y, +z };
                grid[ il ][ ip1 ] = new double[] { -x, -y, +z };
                grid[ il ][ ip2 ] = new double[] { -x, -y, -z };
                grid[ il ][ ip3 ] = new double[] { +x, +y, -z };
            }
        }

        /* Return the result packaged as an array of line objects. */
        return toLines( grid );
    }

    /**
     * Returns an array of line specifications representing lines of
     * latitude.
     *
     * @param  dRadius  radius in data units
     * @param  nLat    number of equally spaced latitude lines in the north
     *                 (or south) hemisphere; excludes the equator
     * @return  <code>1+nLat*2</code>-element aray of parallel specifiers
     */
    public Line3d[] getLatitudeLines( double dRadius, int nLat ) {
        double dsize = 1.0 / Math.min( unitNorm( 0 ), unitNorm( 1 ) );
        double[][][] grid = new double[ 1 + nLat * 2 ][][];

        /* North/South latitude lines.  Calculate them using symmetry to
         * reduce the number of trig calculations required. */
        double dTheta = Math.PI * 0.5 / ( nLat + 1 );
        for ( int iLat = 0; iLat < nLat; iLat++ ) {
            double theta = ( iLat + 1 ) * dTheta;
            double cosTheta = Math.cos( theta );
            double sinTheta = Math.sin( theta );
            double r = dRadius * sinTheta;
            int np = getQuadrantPointCount( dsize, r * cosTheta );
            double dPhi = 2.0 * Math.PI / 4.0 / np;
            for ( int iup = 0; iup < 2; iup++ ) {
                int il = 1 + iLat * 2 + iup;
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
                    int il = 1 + iLat * 2 + iup;
                    double z = iup == 0 ? + dRadius * cosTheta
                                        : - dRadius * cosTheta;
                    grid[ il ][ ip0 ] = new double[] { +x, +y, +z };
                    grid[ il ][ ip1 ] = new double[] { -x, +y, +z };
                    grid[ il ][ ip2 ] = new double[] { -x, -y, +z };
                    grid[ il ][ ip3 ] = new double[] { +x, -y, +z };
                }
            }
        }

        /* Equator.  The calculation is the same as above, but with no
         * looping or reflection, and with theta set to zero. */
        int np = getQuadrantPointCount( dsize, dRadius );
        double dPhi = 2.0 * Math.PI / 4.0 / np;
        double[][] equator = new double[ np * 4 + 1 ][];
        for ( int ip = 0; ip < np; ip++ ) {
            double phi = ip * dPhi;
            double x = dRadius * Math.cos( phi );
            double y = dRadius * Math.sin( phi );
            int ip0 = 0 * np + ip;
            int ip1 = 2 * np - ip - 1;
            int ip2 = 2 * np + ip;
            int ip3 = 4 * np - ip - 1;
            equator[ ip0 ] = new double[] { +x, +y, 0 };
            equator[ ip1 ] = new double[] { -x, +y, 0 };
            equator[ ip2 ] = new double[] { -x, -y, 0 };
            equator[ ip3 ] = new double[] { +x, -y, 0 };
        }
        grid[ 0 ] = equator;

        /* Return the result packaged as an array of line objects. */
        return toLines( grid );
    }

    /**
     * Determines whether this object's CubeSurface contains the data origin.
     *
     * @return  true iff the data origin is within the cube region
     */
    private boolean containsOrigin() {
        for ( int i = 0; i < 3; i++ ) {
            if ( dlos_[ i ] >= 0 || dhis_[ i ] <= 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the number of points per circle quadrant that we
     * need to provide to get a good approximation to a real ellipse.
     * Good approximation here means deviation of half a pixel, which
     * should be fine at least in a bitmapped graphics context.
     *
     * @param  dataUnitSize  extent of one data unit in normalised cube units
     * @param  dataRadius    radius of circle to draw in data units
     */
    private int getQuadrantPointCount( double dataUnitSize,
                                       double dataRadius ) {

        /* Some fairly straightforward trig will show that the number
         * of straight line segments required to approximate a circle
         * of radius R in which no point should deviate more than E from
         * its ideal position is:
         *    2 * PI * sqrt( R / E )
         */
        double dPixel = dataUnitSize / gsize_;
        double tol = 0.5 * dPixel;
        double dnp = 2.0 * Math.PI / 4.0 * Math.sqrt( dataRadius / tol );

        /* Max out at a sensible number; otherwise for some plots you
         * could end up calculating a ludicrous number of points
         * (all or nearly all of which would never be seen). */
        return Math.min( 400, (int) Math.ceil( dnp ) );
    }

    /**
     * Package a 3-d array of doubles as a 1-d array of Line3ds.
     *
     * @param  grid  array of double[][3]s each representing a set of
     *               points in 3d; the last element should be null,
     *               and will be set by this routine to match the first
     * @return  array of lines
     */
    private static Line3d[] toLines( double[][][] grid ) {

        /* For each line, set the last point equal to the first. */
        for ( int il = 0; il < grid.length; il++ ) {
            double[][] line = grid[ il ];
            assert line[ line.length - 1 ] == null;
            line[ line.length - 1 ] = line[ 0 ];
        }

        Line3d[] lines = new Line3d[ grid.length ];
        for ( int il = 0; il < lines.length; il++ ) {
            lines[ il ] = new Line3d( grid[ il ] );
        }
        return lines;
    }

    /**
     * Returns the size of a data unit in normalised units for a given
     * axis of this object's cube surrface.  Only makes sense for a
     * non-logarithmic axis.
     *
     * @param  idim   dimension index (0..2)
     * @return   data unit size in normalised units
     */
    private double unitNorm( int idim ) {
        double[] dp0 = new double[ 3 ];
        double[] dp1 = new double[ 3 ];
        dp1[ idim ] = 1.0;
        return Math.abs( surf_.normalise( dp1, idim )
                       - surf_.normalise( dp0, idim ) );
    }

    /**
     * Represents a set of 3-d points that can be joined together to
     * make a grid line.  The first element is the same as the last.
     */
    public static class Line3d implements Iterable<double[]> {

        final double[][] points_;

        /**
         * Constructor.
         */
        Line3d( double[][] points ) {
            points_ = points;
        }

        /**
         * Returns an iterator over double[3]s representing 3d points.
         */
        public Iterator<double[]> iterator() {
            return Arrays.asList( points_ ).iterator();
        }
    }
}
