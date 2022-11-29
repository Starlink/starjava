// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

/**
 * Functions useful for working with shapes on a sphere.
 * All angles are expressed in degrees.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2019
 */
public class Sky {

    private static final double D2R = Math.PI / 180.0;

    /**
     * Private constructor prevents instantiation.
     */
    private Sky() {
    }

    /**
     * Calculates the separation (distance around a great circle) of
     * two points on the sky in degrees.
     *
     * <p>This function is identical to <code>skyDistanceDegrees</code>
     * in class <code>CoordsDegrees</code>.
     *
     * @param  lon1   point 1 longitude in degrees
     * @param  lat1   point 1 latitude in degrees
     * @param  lon2   point 2 longitude in degrees
     * @param  lat2   point 2 latitude in degrees
     * @return  angular distance between point 1 and point 2 in degrees
     */
    public static double skyDistance( double lon1, double lat1,
                                      double lon2, double lat2 ) {
        return CoordsDegrees.skyDistanceDegrees( lon1, lat1, lon2, lat2 );
    }

    /**
     * Determines the longitude mid-way between two given longitudes.
     * In most cases this is just the mean of the two values,
     * but this function copes correctly with the case where the
     * given values straddle the lon=0 line.
     *
     * @example  <code>midLon(204.0, 203.5) = 203.75</code>
     * @example  <code>midLon(2, 359) = 0.5</code>
     *
     * @param  lon1  first longitude in degrees
     * @param  lon2  second longitude in degrees
     * @return   longitude midway between the given values
     */
    public static double midLon( double lon1, double lon2 ) {
        if ( Math.abs( lon2 - lon1 ) < 180 ) {
            return 0.5 * ( lon1 + lon2 );
        }
        else {
            double lon1a = ( ( lon1 + 180 ) % 360 ) - 180;
            double lon2a = ( ( lon2 + 180 ) % 360 ) - 180;
            assert Math.abs( lon2a - lon1a ) < 180;
            return 0.5 * ( lon1a + lon2a );
        }
    }

    /**
     * Determines the latitude midway between two given latitudes.
     * This simply returns the mean of the two values,
     * but is supplied for convenience to use alongside
     * the <code>midLon</code> function.
     *
     * @example  <code>midLat(23.5, 24.0) = 23.75</code>
     *
     * @param  lat1  first latitude in degrees
     * @param  lat2  second latitude in degrees
     * @return   latitude midway between the given values
     */
    public static double midLat( double lat1, double lat2 ) {
        return 0.5 * ( lat1 + lat2 );
    }

    /**
     * Tests whether a given sky position is inside a given ellipse.
     *
     * @param   lon0   test point longitude in degrees
     * @param   lat0   test point latitude in degrees
     * @param   lonCenter  ellipse center longitude in degrees
     * @param   latCenter  ellipse center latitude in degrees
     * @param   rA   ellipse first principal radius in degrees
     * @param   rB   ellipse second principal radius in degrees
     * @param   posAng  position angle in degrees from the North pole
     *                  to the primary axis of the ellipse in the direction
     *                  of increasing longitude
     * @return  true iff test point is inside, or on the border of, the ellipse
     */
    public static boolean inSkyEllipse( double lon0, double lat0,
                                        double lonCenter, double latCenter,
                                        double rA, double rB, double posAng ) {

        /* Return false for invalid inputs. */
        if ( ! ( lat0 >= -90 && lat0 <= 90 ) ||
             ! ( latCenter >= -90 && latCenter <= 90 ) ||
             ! ( rA >= 0 ) ||
             ! ( rB >= 0 ) ||
             Double.isNaN( lon0 ) ||
             Double.isNaN( lonCenter ) ||
             Double.isNaN( posAng ) ) {
            return false;
        }

        /* Deal with easy cases first. */
        if ( Math.min( rA, rB ) > 180 ) {
            return true;
        }
        double rMax = Math.max( rA, rB );
        if ( Math.abs( lat0 - latCenter ) > rMax ) {
            return false;
        }
        double dist = skyDistance( lon0, lat0, lonCenter, latCenter );
        if ( dist > rMax ) {
            return false;
        }
        if ( dist <= Math.min( rA, rB ) ) {
            return true;
        }

        /* Otherwise do the full calculation. */
        double theta =
              CoordsDegrees.posAngDegrees( lonCenter, latCenter, lon0, lat0 )
            - posAng;
        double cosTheta = Math.cos( D2R * theta );
        double sinTheta = Math.sin( D2R * theta );
        double rEllipse = rA * rB / Math.hypot( rB * cosTheta, rA * sinTheta );
        return dist <= rEllipse;
    }

    /**
     * Tests whether a given sky position is inside the polygon defined
     * by a given set of vertices.  The bounding lines of the polygon
     * are the minor arcs of great circles between adjacent vertices,
     * with an extra line assumed between the first and last supplied vertex.
     * The interior of the polygon is defined to be the smaller of the
     * two regions separated by the boundary.
     * The vertices are specified as a sequence of
     * lon<sub>i</sub>, lat<sub>i</sub> pairs.
     *
     * <p>The implementation of this point-in-polygon function
     * is mostly correct, but may not be bulletproof.
     * It ought to work for relatively small regions anywhere
     * on the sky, but for instance it may get the sense wrong for
     * regions that extend to cover both poles.
     *
     * @param  lon0  test point latitude in degrees
     * @param  lat0  test point longitude in degrees
     * @param  lonLats  2N arguments (<code>lon1</code>, <code>lat1</code>,
     *                                <code>lon2</code>, <code>lat2</code>, ...,
     *                                <code>lonN</code>, <code>latN</code>)
     *                  giving (longitude, latitude) vertices of
     *                  an N-sided polygon in degrees
     * @return  true iff test point is inside, or on the border of,
     *               the polygon
     */
    public static boolean inSkyPolygon( double lon0, double lat0,
                                        double... lonLats ) {

        /* This method uses a version of the crossing-number algorithm
         * for point-in-polygon determination, adapted for use on a sphere.
         * It was written with reference to
         * http://geomalgorithms.com/a03-_inclusion.html.
         * The crossing-number algorithm works by counting edge crossings
         * between the test point and a point known to be outside the region.  
         * In the plane, it's easy to identify an outside point as one at
         * infinite distance, but there is no obious equivalent on the sphere;
         * to put it another way there is no obvious general criterion given
         * a set of vertices on the sphere for which side counts as inside.
         * Many GIS implementations just assume that the south pole is outside,
         * but that's not suitable on the sky.  Our intention is to define
         * the smaller of the two regions as inside, however the polygon
         * area is not very easy/fast to determine, so we hack it.
         * We make two counts of edge crossings: (a) to the north pole and
         * (b) to the south pole.  If their parity is the same, we assume
         * they are both outside.  If one is odd and one is even, then
         * one pole is inside the region and we have to decide which.
         * We hack this: whichever one is furthest from most of the vertices
         * is assumed to be outside. */
 
        /* No doubt there is some room for efficiency improvements here.
         * However, having played around a bit with transforming into
         * vector space and caching values and so on, in practice it
         * doesn't seem to make a huge amount of difference, since
         * the number of test point longitude edge intersections is
         * usually quite low: often the test region covers a small
         * fraction of the dataset longitude range, and in most cases
         * the test meridian will not intersect more than 2 edges. */

        /* Check there is a sensible number of points. */
        int np2 = lonLats.length;
        if ( np2 % 2 != 0 || np2 < 6 ) {
            return false;
        }

        /* Initialise variables. */
        int ncN = 0;
        int ncS = 0;
        lon0 = Arithmetic.mod( lon0, 360 );
        double latSum = 0;

        /* Loop over each edge (pair of vertices), including the edge formed by
         * the first and last array element. */
        double lonA = Arithmetic.mod( lonLats[ np2 - 2 ], 360 );
        double latA = lonLats[ np2 - 1 ];
        for ( int ip2 = 0; ip2 < np2; ip2 += 2 ) {
            double lonB = Arithmetic.mod( lonLats[ ip2 + 0 ], 360 );
            double latB = lonLats[ ip2 + 1 ];

            /* Determine whether the edge crosses the same longitude as
             * the test point.  We assume that each edge corresponds to
             * the minor arc (shortest distance along a great circle)
             * between the points, so we have to treat separately cases
             * that do and don't cross the antimeridian.
             * Careful treatment is required for edge and corner cases
             * to avoid miscounting where a vertex is exactly on the
             * test longitude: for each edge, the western endpoint is
             * included in the line and the eastern endpoint is excluded.
             * Vertical edges (meridians) are ignored, since they don't
             * take you across the polygon boundary. */
            double dlonAB = lonB - lonA;
            if ( dlonAB != 0 &&
                 Math.abs( dlonAB ) < 180 ? ( lonA <= lon0 && lon0 < lonB ||
                                              lonB <= lon0 && lon0 < lonA )
                                          : ( lon0 < lonA && lon0 < lonB ||
                                              lon0 >= lonA && lon0 >= lonB ) ) {

                /* For each edge that crosses the test longitude, count
                 * whether it does so above or below the test latitude. */
                double latAB = arcLat( lonA, latA, lonB, latB, lon0 );
                if ( latAB > lat0 ) {
                    ncN++;
                }
                else if ( latAB < lat0 ) {
                    ncS++;
                }

                /* If the point is actually on an edge, consider the point
                 * to be contained in the polygon. */
                else {
                    return true;
                }
            }
            latSum += latA;
            lonA = lonB;
            latA = latB;
        }

        /* Now we have a count of how many edges are crossed between the
         * test point and each pole.  Use the count from the one furthest
         * from the mean latitude of the polygon vertices.  Inclusion
         * is determined from its parity. */
        int nc = latSum > 0 ? ncS : ncN;
        return nc % 2 != 0;
    }

    /**
     * Determine the latitude for a given longitude of a great circle
     * minor arc defined by two points.
     *
     * @param   lonA   point A longitude in degrees
     * @param   latA   point A latitude in degrees
     * @param   lonB   point B longitude in degrees
     * @param   latB   point B latitude in degrees
     * @param   lon0  longitude to query in degrees
     * @return  latitude for lon0 in degrees
     */
    private static double arcLat( double lonA, double latA,
                                  double lonB, double latB,
                                  double lon0 ) {

        /* This works by linearly interpolating between the unit vector
         * representations of A and B; this interpolation corresponds
         * to a minor arc on the surface of the unit sphere.
         * So we just need to determine the fraction k of the distance
         * between A and B we have to travel to get to the query point.
         * We have to get to the point where y/x = tan(lon0).
         * The interpolation line is r = rA + k(rB-rA), so just solve for k
         * tan(lon0) = y/x = yA+k(yB-yA) / xA+k(xB-xA). */
        double[] vecA = toVector( lonA, latA );
        double[] vecB = toVector( lonB, latB );
        double xA = vecA[ 0 ];
        double yA = vecA[ 1 ];
        double zA = vecA[ 2 ];
        double xAB = vecB[ 0 ] - vecA[ 0 ];
        double yAB = vecB[ 1 ] - vecA[ 1 ];
        double zAB = vecB[ 2 ] - vecA[ 2 ];
        double phi = Math.toRadians( lon0 );

        /* Calculate the interpolation factor.
         * Be a bit careful here to avoid divide by zero issues. */
        double sinPhi = Math.sin( phi );
        double cosPhi = Math.cos( phi );
        final double k;
        if ( Math.abs( sinPhi ) < Math.abs( cosPhi ) ) {
            double tanPhi = sinPhi / cosPhi;
            k = ( yA - xA * tanPhi ) / ( xAB * tanPhi - yAB );
        }
        else {
            double cotPhi = cosPhi / sinPhi;
            k = ( xA - yA * cotPhi ) / ( yAB * cotPhi - xAB );
        }

        /* Interpolate, renormalise, and extract the latitude from
         * the resulting vector. */
        double x0 = xA + k * xAB;
        double y0 = yA + k * yAB;
        double z0 = zA + k * zAB;
        double r0 = Math.sqrt( x0 * x0 + y0 * y0 + z0 * z0 );
        return TrigDegrees.asinDeg( z0 / r0 );
    }

    /**
     * Converts longitude and latitude into a unit vector.
     *
     * @param  lon  longitude in degrees
     * @param  lat  latitutde in degrees
     * @return   3-element X,Y,Z unit vector
     */
    private static double[] toVector( double lon, double lat ) {
        double cosLat = TrigDegrees.cosDeg( lat );
        double sinLat = TrigDegrees.sinDeg( lat );
        double cosLon = TrigDegrees.cosDeg( lon );
        double sinLon = TrigDegrees.sinDeg( lon );
        return new double[] {
            cosLat * cosLon,
            cosLat * sinLon,
            sinLat,
        };
    }
}
