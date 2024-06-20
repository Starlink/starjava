package uk.ac.starlink.ttools.plot2;

import cds.healpix.CompassPoint;
import cds.healpix.FlatHashIterator;
import cds.healpix.HashComputer;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.VerticesAndPathComputer;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utilities for working with the cds-healpix-java library in sky plots.
 *
 * @author   Mark Taylor
 * @since    21 May 2020
 */
public class CdsHealpixUtil {

    private static final double HALF_PI = 0.5 * Math.PI;

    /** Default minimum interpolation for HEALPix tile edges. */
    public static final int DFLT_INTERPOLATE_DEPTH = 5;

    static {
        assert VerticesAndPathComputer.LON_INDEX == 0;
        assert VerticesAndPathComputer.LAT_INDEX == 1;
    }

    /**
     * Private constructor prevents instantiation.
     */
    private CdsHealpixUtil() {
    }

    /**
     * Converts a longitude, latitude pair to a unit vector
     * suitable for use as a sky surface data position,
     * writing the result to a supplied array.
     * No error checking is done.
     *
     * @param   lonRad  longitude in radians
     * @param   latRad  latitude in radians
     * @param   xyz    3-element vector into which (x,y,z) is written
     */
    public static void lonlatToVector( double lonRad, double latRad,
                                       double[] xyz ) {
        double theta = HALF_PI - latRad;
        double phi = lonRad;
        double sd = Math.sin( theta );
        double x = Math.cos( phi ) * sd;
        double y = Math.sin( phi ) * sd;
        double z = Math.cos( theta );
        xyz[ 0 ] = x;
        xyz[ 1 ] = y;
        xyz[ 2 ] = z;
    }

    /**
     * Converts a longitude, latitude 2-vector to a unit vector
     * suitable for use as a sky surface data position,
     * writing the result to a supplied array.
     * No error checking is done.
     *
     * @param   lonlatRad  2-element vector giving (longitude,latitude)
     *                     in radians
     * @param   xyz    3-element vector into which (x,y,z) is written
     */
    public static void lonlatToVector( double[] lonlatRad, double[] xyz ) {
        lonlatToVector( lonlatRad[ 0 ], lonlatRad[ 1 ], xyz );
    }

    /**
     * Converts a longitude, latitude 2-vector to a unit vector
     * suitable for use as a sky surface data position,
     * returning the result as a new array.
     * No error checking is done.
     *
     * @param   lonlatRad  2-element vector giving (longitude,latitude)
     *                     in radians
     * @return  3-element unit vector containing (x,y,z)
     */
    public static double[] lonlatToVector( double[] lonlatRad ) {
        double[] xyz = new double[ 3 ];
        lonlatToVector( lonlatRad[ 0 ], lonlatRad[ 1 ], xyz );
        return xyz;
    }

    /**
     * Calculates the tile index given a sky surface position unit vector.
     *
     * @param  xyz  3-element unit vector containing (x,y,z)
     */
    public static long vectorToHash( HashComputer hasher, double[] xyz ) {
        double x = xyz[ 0 ];
        double y = xyz[ 1 ];
        double z = xyz[ 2 ];
        double theta = Math.acos( z );
        double phi = Math.atan2( y, x );
        double lonRad = phi;
        double latRad = HALF_PI - theta;
        return hasher.hash( lonRad, latRad );
    }

    /**
     * Converts a sky surface data position unit vector
     * to a longitude, latitude 2-vector,
     * writing the result to a supplied array.
     * No error checking is done.
     *
     * @param  xyz  3-element array containing (x,y,z)
     * @param  lonlatRad  2-element array into which (longitude, latitude)
     *                    in radians is written
     */
    public static void vectorToLonlat( double[] xyz, double[] lonlatRad ) {
        double x = xyz[ 0 ];
        double y = xyz[ 1 ];
        double z = xyz[ 2 ];
        double theta = Math.acos( z );
        double phi = Math.atan2( y, x );
        double lonRad = phi;
        double latRad = HALF_PI - theta;
        lonlatRad[ 0 ] = lonRad;
        lonlatRad[ 1 ] = latRad;
    }

    /**
     * Converts a sky surface data position unit vector
     * to a longitude, latitude 2-vector,
     * returning the restult as a new array.
     * No error checking is done.
     *
     * @param  xyz  3-element unit vector containing (x,y,z)
     * @return   2-element array giving (longitude, latitude) in radians
     */
    public static double[] vectorToLonlat( double[] xyz ) {
        double[] lonlat = new double[ 2 ];
        vectorToLonlat( xyz, lonlat );
        return lonlat;
    }

    /**
     * Represents the tiles in a BMOC as a Set of Longs.
     * The returned object is a thin adapter on top of the input BMOC.
     *
     * @param   bmoc  result of healpix query
     * @return   set of hashes at BMOC depth
     */
    public static Set<Long> bmocSet( final HealpixNestedBMOC bmoc ) {
        return new AbstractSet<Long>() {
            private int size_ = -1;
            public int size() {
                if ( size_ < 0 ) {
                    size_ = (int) bmoc.computeDeepSize();
                }
                return size_;
            }
            public Iterator<Long> iterator() {
                final FlatHashIterator fhit = bmoc.flatHashIterator();
                return new Iterator<Long>() {
                    public boolean hasNext() {
                        return fhit.hasNext();
                    }
                    public Long next() {
                        return Long.valueOf( fhit.next() );
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Calculates vertices round the edge of a HEALPix tile,
     * writing the result in a supplied array.
     * This convenience method just simplifies the required parameters.
     *
     * @param   vpc  healpix object that does the work
     * @param   hash   tile index
     * @param   minDepth   the minimal depth at which vertices are calculated;
     *                     if the tile depth is greater or equal to this value
     *                     then 4 vertices will be calculated, 
     *                     otherwise interpolated points will be included
     * @param   lonlats    [N][2]-element array into which pairs of
     *                     (longitude,latitude) in radians will be written
     * @return  number of vertices written into lonlats
     */
    public static int lonlatVertices( VerticesAndPathComputer vpc,
                                      long hash, int minDepth,
                                      double[][] lonlats ) {
        int nseg = 1 << Math.max( 0, minDepth - vpc.depth() );
        vpc.pathAlongCellEdge( hash, CompassPoint.Cardinal.E, true,
                               nseg, lonlats );
        return 4 * nseg;
    }

    /**
     * Calculates vertices round the edge of a HEALPix tile,
     * returning the result in a new array.
     * This convenience method just simplifies the required parameters.
     *
     * @param   vpc  healpix object that does the work
     * @param   hash   tile index
     * @param   minDepth   the minimal depth at which vertices are calculated;
     *                     if the tile depth is greater or equal to this value
     *                     then 4 vertices will be calculated, 
     *                     otherwise interpolated points will be included
     * @return  array of (longitude,latitude) pairs in radians
     */
    public static double[][] lonlatVertices( VerticesAndPathComputer vpc,
                                             long hash, int minDepth ) {
        int nseg = 1 << Math.max( 0, minDepth - vpc.depth() );
        double[][] lonlats = new double[ 4 * nseg ][ 2 ];
        int nv = lonlatVertices( vpc, hash, minDepth, lonlats );
        assert nv == lonlats.length;
        return lonlats;
    }
}
