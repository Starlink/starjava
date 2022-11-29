package uk.ac.starlink.topcat.plot2;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LabelledLine;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.geom.HealpixDataGeom;
import uk.ac.starlink.ttools.plot2.geom.Projection;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;
import uk.ac.starlink.ttools.plot2.geom.SkySys;

/**
 * FigureMode implementations for use with a SkySurface.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2019
 */
public abstract class SkyFigureMode implements FigureMode {

    private final String name_;

    /** SkySurface area within a circle on the sky. */
    public static final SkyFigureMode CIRCLE = new SkyFigureMode( "Circle" ) {
        public Figure createFigure( Surface surf, Point[] points ) {
            return surf instanceof SkySurface
                 ? createCircleFigure( (SkySurface) surf, points )
                 : null;
        }
    };

    /** SkySurface area within the ellipse defined by 2 or 3 points. */
    public static final SkyFigureMode ELLIPSE = new SkyFigureMode( "Ellipse" ) {
        public Figure createFigure( Surface surf, Point[] points ) {
            return surf instanceof SkySurface
                 ? createEllipseFigure( (SkySurface) surf, points )
                 : null;
        }
    };

    /** SkySurface area within the polygon bounded by (&gt;=3) points. */
    public static final SkyFigureMode POLYGON =
            new SkyFigureMode( "Polygon" ) {
        public Figure createFigure( Surface surf, Point[] points ) {
            return surf instanceof SkySurface
                 ? createPolygonFigure( (SkySurface) surf, points )
                 : null;
        }
    };

    /** Available polygon modes for use with sky surfaces. */
    public static final SkyFigureMode[] MODES = {
        CIRCLE, ELLIPSE, POLYGON,
    };

    private static final String F_INSKYPOLYGON;
    private static final String F_INSKYELLIPSE;
    private static final String F_SKYDISTANCE;

    /** JEL functions used when constructing expressions. */
    static final String[] JEL_FUNCTIONS = new String[] {
        F_INSKYPOLYGON = "inSkyPolygon",
        F_INSKYELLIPSE = "inSkyEllipse",
        F_SKYDISTANCE = "skyDistance",
    };

    /**
     * Constructor.
     *
     * @param  name  user-visible mode name
     */
    private SkyFigureMode( String name ) {
        name_ = name;
    }

    public String getName() {
        return name_;
    }

    /**
     * Returns a figure defined by the vertices of great circle minor arcs.
     *
     * @param  surf  plot surface
     * @param  points   graphics points defining polygon
     * @retrun  polygon figure, or null
     */
    private static Figure createPolygonFigure( SkySurface surf,
                                               Point[] points ) {
        int np = points.length;
        if ( np >= 2 ) {
            Point2D[] lineVertices = getLineVertices( surf, points );
            if ( lineVertices != null ) {
                Point[] points1 = new Point[ np + 1 ];
                System.arraycopy( points, 0, points1, 0, np );
                points1[ np ] = points[ 0 ];
                Point2D[] areaVertices = getLineVertices( surf, points1 );
                return new PolygonFigure( surf, points,
                                          lineVertices, areaVertices );
            }
        }
        return null;
    }

    /**
     * Returns a figure defined by the center and a radial point of a
     * small circle.  The first and last supplied points are used,
     * any in between are ignored.
     *
     * @param  surf  plot surface
     * @param  points   graphics points defining circle
     * @return  circle figure, or null
     */
    private static Figure createCircleFigure( SkySurface surf,
                                              Point[] points ) {
        int np = points.length;
        if ( np >= 2 ) {
            Point p0 = points[ 0 ];
            Point p1 = points[ np - 1 ];
            Point2D[] circleVertices = getCircleVertices( surf, p0, p1 );
            return circleVertices == null
                 ? null
                 : new CircleFigure( surf, p0, p1, circleVertices );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a figure defined by the center and one or two radial points
     * of an ellipse.  The first two and, if applicable, last points are used,
     * any others are ignored.
     *
     * @param  surf  plot surface
     * @param  points   graphics points defining ellipse
     * @return  ellipse figure, or null
     */
    private static Figure createEllipseFigure( SkySurface surf,
                                               Point[] points ) {
        int np = points.length;
        if ( np >= 2 ) {
            Point p0 = points[ 0 ];
            Point pA = points[ 1 ];
            Point p2 = np > 2 ? points[ np - 1 ] : null;
            double[] vec0 = surf.graphicsToData( p0 );
            double[] vecA = surf.graphicsToData( pA );
            double posAng = vectorPositionAngle( vec0, vecA );
            double rA = surf.screenDistanceRadians( p0, pA );
            double perpDist = p2 == null ? Double.NaN
                                         : perpDistance( surf, p0, pA, p2 );
            double rB = perpDist > 0 ? perpDist : 0.5 * rA;
            Point2D[] ellipseVertices =
                getEllipseVertices( surf, p0, rA, rB, posAng );
            return ellipseVertices == null
                 ? null
                 : new EllipseFigure( surf, p0, rA, rB, posAng,
                                      ellipseVertices, p2 != null );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a list of graphics points that should be joined up in
     * graphics space to represent great circles joining a set of vertices.
     * The returned points will be close enough together that joining
     * them with straight lines in graphics space should look OK.
     * If no contiguous figure can be drawn, null is returned.
     * No steps are taken to close the polygon.
     *
     * @param  surf  plot surface
     * @param  points   graphics points defining polygon
     * @return  array of contiguous points, or null
     */
    private static Point2D[] getLineVertices( SkySurface surf,
                                              Point[] points ) {
        List<Point2D> vertices = new ArrayList<Point2D>();
        for ( int ip = 1; ip < points.length; ip++ ) {
            LabelledLine line =
                surf.createLine( points[ ip - 1 ], points[ ip ] );
            if ( line == null ) {
                return null;
            }
            for ( Point2D lp : line.getPoints() ) {
                if ( lp == null ) {
                    return null;
                }
                vertices.add( lp );
            }
        }
        return vertices.toArray( new Point2D[ 0 ] );
    }

    /**
     * Returns a set of graphics points that can be used to represent
     * a circle on the sky, given a center and radial graphics point.
     * The returned points will be close enough together that joining
     * them with straight lines in graphics space should look OK.
     * If no contiguous figure can be drawn, null is returned.
     *
     * @param   surf  sky surface
     * @param   p0    central point
     * @param   p1    radial point
     * @return  list of vertices defining a closed figure in graphics
     *          coordinates, or null if the arguments are not suitable
     */
    private static Point2D[] getCircleVertices( SkySurface surf,
                                                Point p0, Point p1 ) {

        /* Get the sky cone geometry. */
        double radius = surf.screenDistanceRadians( p0, p1 );
        Projection proj = surf.getProjection();
        if ( ! ( radius > 0 ) ) {
            return null;
        }

        /* Work out how many points will be required along the
         * circumference to get something reasonably circular
         * looking. */
        double gr = Math.hypot( p1.x - p0.x, p1.y - p0.y );
        double dTheta = 6. / gr;

        /* Take the original point on the circumference and rotate it
         * by a constant angle multiple times around the
         * central vector in 3d space.  This traces out a circle
         * on the unit sphere, which can be converted back into
         * graphics coordinates. */
        double[] vec0 = surf.graphicsToData( p0 );
        double[] vec1 = surf.graphicsToData( p1 );
        double[] axrot = axialRotationMatrix( vec0, dTheta );
        List<Point2D> vertices = new ArrayList<Point2D>();
        for ( double theta = 0; theta < 2 * Math.PI; theta += dTheta ) {
            Point2D.Double vertex = new Point2D.Double();
            if ( surf.dataToGraphics( vec1, false, vertex ) &&
                 proj.isContinuousLine( vec0, vec1 ) ) {
                vertices.add( vertex );
            }
            else {
                return null;
            }
            vec1 = Matrices.mvMult( axrot, vec1 );
        }
        return vertices.toArray( new Point2D[ 0 ] );
    }

    /**
     * Returns a set of graphics points that can be used to represent
     * an ellipse on the sky, given a center, semi-major axis lengths,
     * and position angle.
     * If no contiguous figure can be drawn, null is returned.
     *
     * @param  surf  sky surface
     * @param  p0    ellipse center in graphics coordinates
     * @param  rA    primary semi-major axis length in radians
     * @param  rB    secondary semi-major axis length in radians
     * @param  posAng  angle from North to primary semi-major axis
     *                 in direction of increasing longitude
     * @return   list of vertices defining a closed figure in graphics
     *           coordinates, or null if the arguments are not suitable
     */
    private static Point2D[] getEllipseVertices( SkySurface surf, Point p0,
                                                 double rA, double rB,
                                                 double posAng ) {

        /* Define functions to calculate radius and point on ellipse boundary
         * for angle parameter. */
        Projection proj = surf.getProjection();
        double[] vec0 = surf.graphicsToData( p0 );
        if ( vec0 == null ) {
            return null;
        }
        DoubleUnaryOperator radiusTheta = theta -> {
            double cosTheta = Math.cos( theta );
            double sinTheta = Math.sin( theta );
            return rA * rB / Math.hypot( rB * cosTheta, rA * sinTheta );
        };
        DoubleFunction<Point2D.Double> pointTheta = theta -> {
            double[] vec1 =
                distanceBearing( vec0, posAng + theta,
                                 radiusTheta.applyAsDouble( theta ) );
            Point2D.Double p1 = new Point2D.Double();
            return proj.isContinuousLine( vec0, vec1 )
                && surf.dataToGraphics( vec1, false, p1 ) ? p1 : null;
        };

        /* Work out how many points will be required along the boundary
         * to get a fairly smooth ellipse. */
        Point2D.Double pA = pointTheta.apply( 0. );
        Point2D.Double pB = pointTheta.apply( 0.5 * Math.PI );
        if ( pA == null || pB == null ) {
            return null;
        }
        double gr = Math.max( Math.hypot( pA.x - p0.x, pA.y - p0.y ),
                              Math.hypot( pB.x - p0.x, pB.y - p0.y ) );
        double dTheta = 6. / gr;
        double rScale = Math.max( Math.min( rA, rB ), ( rA + rB ) / 16 );

        /* Generate and return the points. */
        List<Point2D> vertices = new ArrayList<Point2D>();
        for ( double theta = 0; theta < 2 * Math.PI; ) {
            Point2D vertex = pointTheta.apply( theta );
            if ( vertex != null ) {
                vertices.add( vertex );
            }
            else {
                return null;
            }
            double r = radiusTheta.applyAsDouble( theta );
            theta += dTheta * rScale / r;
        }
        return vertices.toArray( new Point2D[ 0 ] );
    }

    /**
     * Calculates a rotation matrix which will rotate any vector by a
     * given angle around a given axial vector.
     *
     * @param  axvec  (x,y,z) unit vector defining the axial direction
     * @param  angle in radians defining the amount of rotation
     */
    private static double[] axialRotationMatrix( double[] axvec, double phi ) {

        /* The algebra here is taken from routine SLA_DAV2M in SLALIB. */
        double x = axvec[ 0 ];
        double y = axvec[ 1 ];
        double z = axvec[ 2 ];
        double s = Math.sin( phi );
        double c = Math.cos( phi );
        double w = 1.0 - c;
        return new double[] {
            x*x*w+c,   x*y*w+z*s, x*z*w-y*s,
            x*y*w-z*s, y*y*w+c,   y*z*w+x*s,
            x*z*w+y*s, y*z*w-x*s, z*z*w+c,
        };
    }

    /**
     * Calculates the bearing (position angle) of one celestial direction
     * with respect to another.
     *
     * <p>This is routine DPAV from SLALIB.
     *
     * @param   vec1  unit vector of starting point
     * @param   vec2  unit vector of other point
     * @return   bearing of vec2 from vec1 as an angle in radians
     *           from North in the direction of positive longitude
     */
    private static double vectorPositionAngle( double[] vec1, double[] vec2 ) {
        double x1 = vec1[ 0 ];
        double y1 = vec1[ 1 ];
        double z1 = vec1[ 2 ];
        double w = Math.sqrt( x1 * x1 + y1 * y1 + z1 * z1 );
        if ( w != 0 ) {
            x1 = x1 / w;
            y1 = y1 / w;
            z1 = z1 / w;
        }
        double x2 = vec2[ 0 ];
        double y2 = vec2[ 1 ];
        double z2 = vec2[ 2 ];
        double sq = y2 * x1 - x2 * y1;
        double cq = z2 * ( x1 * x1 + y1 * y1 ) - z1 * ( x2 * x1 + y2 * y1 );
        if ( sq == 0 && cq == 0 ) {
            cq = 1;
        }
        return Math.atan2( sq, cq );
    }

    /**
     * Calculates the position vector that results from following a
     * bearing from a starting position for a given distance.
     * Input and output positions are unit vectors (direction cosines).
     *
     * @param  vec0  starting position unit vector
     * @param  psi   bearing as angle clockwise from north in radians
     *               (like position angle)
     * @param  distRad   distance travelled in radians
     * @return  ending position unit vector
     */
    private static double[] distanceBearing( double[] vec0, double psi,
                                             double distRad ) {

        /* Convert from input unit vector to lat/long. */
        double lat0 = 0.5 * Math.PI - Math.acos( vec0[ 2 ] );
        double lon0 = Math.atan2( vec0[ 1 ], vec0[ 0 ] );

        /* These expressions were copied from
         * http://www.movable-type.co.uk/scripts/latlong.html.
         * I thought there should be something in SLALIB that does this,
         * but I couldn't find it. */
        double lat1 = Math.asin( Math.sin( lat0 ) * Math.cos( distRad )
                               + Math.cos( lat0 ) * Math.sin( distRad )
                                                  * Math.cos( psi ) );
        double lon1 = lon0
                    + Math.atan2( Math.sin( psi ) * Math.sin( distRad )
                                                  * Math.cos( lat0 ),
                                  Math.cos( distRad ) - Math.sin( lat0 )
                                                      * Math.sin( lat1 ) );

        /* Convert from output lat/long to unit vector. */
        double theta1 = 0.5 * Math.PI - lat1;
        return new double[] {
            Math.sin( theta1 ) * Math.cos( lon1 ),
            Math.sin( theta1 ) * Math.sin( lon1 ),
            Math.cos( theta1 ),
        };
    }

    /**
     * Calculate tangent plane coordinates from unit vectors.
     * This is DV2TP from SLALIB.
     *
     * @param  vec0  unit vector of tangent point
     * @param  vec1  unit vector of point to be transformed
     * @return  2-element (xi,eta) tangent point plane coordinates of vec1,
     *                    in radians, or null if there's a problem
     */
    private static double[] dv2tp( double[] vec0, double[] vec1 ) {
        double r2 = vec0[ 0 ] * vec0[ 0 ] + vec0[ 1 ] * vec0[ 1 ];
        double r = Math.sqrt( r2 );
        double w = vec1[ 0 ] * vec0[ 0 ] + vec1[ 1 ] * vec0[ 1 ];
        double d = w + vec1[ 2 ] * vec0[ 2 ];
        if ( d < 1e-6 ) {
            return null;
        }
        else {
            d = d * r;
            return new double[] {
                ( vec1[ 1 ] * vec0[ 0 ] - vec1[ 0 ] * vec0[ 1 ] ) / d,
                ( vec1[ 2 ] * r2 - vec0[ 2 ] * w ) / d,
            };
        }
    }

    /**
     * Calculates the perpendicular distance from a point p2
     * to the line between p0 and p1.
     *
     * @param  p0  graphics coordinates for one end of line
     * @param  p1  graphics coordinatse for other end of line
     * @param  p2  graphics coordinates for point
     * @return  length in radians of perpendicular dropped from
     *          sky position at p2 to great circle defined by p1-p0,
     *          or NaN if it can't be done
     */
    private static double perpDistance( SkySurface ssurf, Point p0, Point pA,
                                        Point p2 ) {

        /* Transform from graphics coordinates to unit vectors. */
        double[] vec0 = ssurf.graphicsToData( p0 );
        double[] vecA = ssurf.graphicsToData( pA );
        double[] vec2 = ssurf.graphicsToData( p2 );
        if ( vec0 == null || vecA == null || vec2 == null ) {
            return Double.NaN;
        }

        /* Project points A and 2 to tangent plane centered on p0. */
        double[] tpA = dv2tp( vec0, vecA );
        double[] tp2 = dv2tp( vec0, vec2 );
        if ( tpA == null || tp2 == null ) {
            return Double.NaN;
        }

        /* Determine length of perpendicular on tangent plane
         * dropped from p2 to 0A. */
        double v1x = tpA[ 0 ];
        double v1y = tpA[ 1 ];
        double r1 = Math.hypot( v1x, v1y );
        double u1x = v1x / r1;
        double u1y = v1y / r1;
        double v2x = tp2[ 0 ];
        double v2y = tp2[ 1 ];
        double p12 = u1x * v2x + u1y * v2y;
        double vpx = v2x - p12 * u1x;
        double vpy = v2y - p12 * u1y;
        return Math.hypot( vpx, vpy );
    }

    /**
     * Utility method to turn an array of points into an Area.
     *
     * @param  vertices  defines polygon in graphics space
     * @return   area surrounded by supplied vertices
     */
    private static Area createArea( Point2D[] vertices ) {
        if ( vertices != null ) {
            Polygon poly = new Polygon();
            for ( Point2D p : vertices ) {
                poly.addPoint( (int) p.getX(), (int) p.getY() );
            }
            return new Area( poly );
        }
        else {
            return null;
        }
    }

    /**
     * Utility method to turn an array of points into a path
     *
     * @param  vertices  defines path in graphics space
     * @param  isClosed  true to close the path, false to leave open
     * @return  path object
     */
    private static Path2D createPath( Point2D[] vertices, boolean isClosed ) {
        Path2D.Double path = new Path2D.Double();
        if ( vertices != null ) {
            for ( int ip = 0; ip < vertices.length; ip++ ) {
                Point2D p = vertices[ ip ];
                if ( ip == 0 ) {
                    path.moveTo( p.getX(), p.getY() );
                }
                else {
                    path.lineTo( p.getX(), p.getY() );
                }
            }
            if ( isClosed ) {
                path.lineTo( vertices[ 0 ].getX(), vertices[ 0 ].getY() );
            }
        }
        return path;
    }

    /**
     * Returns a formatted list of lon, lat coordinates corresponding
     * to a list of points in graphics space.
     * For N submitted points, the output is of the form
     * ", lon1, lat1, lon2, lat2... ,lonN, latN".
     *
     * @param  surf    plotting surface
     * @param  gps     array of N points in graphics space
     * @param  varGeom  geometry of the lon and lat variable values
     * @return  string giving a comma-separated list of sky coordinates
     */
    private static String referencePoints( SkySurface surf, Point[] gps,
                                           SkyDataGeom varGeom ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Point point : gps ) {
            sbuf.append( ", " );
            double[] dpos = surf.graphicsToData( point );
            if ( varGeom != null ) {
                varGeom.unrotate( dpos );
            }
            double pixRad = Math.sqrt( surf.pixelAreaSteradians( point ) );
            if ( dpos == null || ! ( pixRad > 0 ) ) {
                return null;
            }
            double x = dpos[ 0 ];
            double y = dpos[ 1 ];
            double z = dpos[ 2 ];
            double latRad = Math.PI * 0.5 - Math.acos( z );
            double lonRad = Math.atan2( y, x );
            while ( lonRad < 0 ) {
                lonRad += 2 * Math.PI;
            }
            sbuf.append( SkySurface
                        .formatPositionDec( lonRad, latRad, pixRad )
                        .replaceAll( "\\+", "" )
                        .replaceAll( " ", "" ) );
        }
        return sbuf.toString();
    }

    /**
     * Assembles an ADQL conditional expression representing inclusion
     * in a closed shape.
     *
     * @param  lonVar  ADQL-friendly name of longitude variable
     * @param  latVar  ADQL-friendly name of latitude variable
     * @param  adqlShape   ADQL geometry expression definining shape
     * @return  conditional ADQL expression testing inclusion of
     *          <code>(lonVar,latVar)</code> in <code>adqlShape</code>
     */
    private static String adqlContains( String lonVar, String latVar,
                                        String adqlShape ) {
        return new StringBuffer()
            .append( "1=CONTAINS(" )
            .append( "POINT('', " )
            .append( lonVar )
            .append( ", " )
            .append( latVar )
            .append( "), " )
            .append( adqlShape )
            .append( ")" )
            .toString();
    }

    /**
     * Partial Figure implementation for use with SkyFigureMode.
     */
    private static abstract class SkyFigure implements Figure {
        final SkySurface surf_;

        /**
         * Constructor.
         *
         * @param  surf   plot surface
         */
        SkyFigure( SkySurface surf ) {
            surf_ = surf;
        }

        public String createExpression( TableCloud cloud ) {
            TopcatModel tcModel = cloud.getTopcatModel();
            GuiCoordContent skyCont = cloud.getGuiCoordContent( 0 );
            DataGeom cloudGeom = cloud.getDataGeom();
            String[] labels =
                TopcatJELUtils.getDataExpressions( tcModel, skyCont );
            if ( labels.length == 2 &&
                 labels[ 0 ] != null && labels[ 1 ] != null ) {
                assert cloudGeom instanceof SkyDataGeom;
                SkyDataGeom varGeom = cloudGeom instanceof SkyDataGeom
                                    ? (SkyDataGeom) cloudGeom
                                    : SkyDataGeom.GENERIC;
                return createSkyExpression( labels[ 0 ], labels[ 1 ], varGeom );
            }
            else if ( labels.length == 1 && labels[ 0 ] != null &&
                      cloudGeom instanceof HealpixDataGeom ) {
                return createHealpixExpression( labels[ 0 ],
                                                (HealpixDataGeom) cloudGeom );
            }
            else {
                return null;
            }
        }

        /**
         * Returns a JEL expression defining the area in data space
         * defined by a set of graphics points, given the HEALPix index
         * variable expression.
         *
         * <p>Currently returns null.
         *
         * @param   hpxVar  JEL-friendly expression naming HEALPix index
         * @param   varGeom  HEALPix geometry applying to the variable
         * @return   boolean JEL inclusion expression, or null
         */
        public String createHealpixExpression( String hpxVar,
                                               HealpixDataGeom varGeom ) {

            /* Placeholder implementation.  This could construct an expression
             * based on converting healpix index (hpxVar) to lon/lat and
             * then call createSkyExpression, or use healpix-specific
             * expressions based on the hpxVar itself.  Other classes
             * currently are missing various methods required to get either
             * of these working, but it wouldn't really be hard to do. */
            return null;
        }

        public String getExpression() {
            String[] names = getViewCoordNames();
            return createSkyExpression( names[ 0 ], names[ 1 ],
                                        SkyDataGeom.GENERIC );
        }

        public String getAdql() {
            String[] names = getViewCoordNames();
            return createSkyAdql( names[ 0 ], names[ 1 ],
                                  SkyDataGeom.GENERIC );
        }

        /**
         * Returns a JEL expression defining the area in data space
         * defined by a set of graphics points, given the latitude and longitude
         * variable expresssions.
         *
         * @param  lonVar  JEL-friendly expression naming longitude coordinate
         * @param  latVar  JEL-friendly expression naming latitude coordinate
         * @param  varGeom  geometry of the lon and lat variable values
         * @return   boolean JEL inclusion expression, or null
         */
        abstract String createSkyExpression( String lonVar, String latVar,
                                             SkyDataGeom varGeom );

        /**
         * Returns an ADQL expression representing the area in data space
         * defined by a set of graphics points, given the latitude and longitude
         * variable expressions.
         *
         * @param  lonVar  ADQL-friendly expression naming longitude coordinate
         * @param  latVar  ADQL-friendly expression naming latitude coordinate
         * @param  varGeom  geometry of the lon and lat variable values
         * @return   boolean ADQL inclusion expression, or null
         */
        abstract String createSkyAdql( String lonVar, String latVar,
                                       SkyDataGeom varGeom );

        /**
         * Returns a pair of coordinate names suitable for this figure's
         * view coordinate system.
         *
         * @return   2-element array giving suitable names for lon,lat coords
         */
        private String[] getViewCoordNames() {
            SkySys sys = surf_.getViewSystem();
            if ( SkySys.EQUATORIAL.equals( sys ) ) {
                return new String[] { "ra", "dec" };
            }
            else if ( SkySys.GALACTIC.equals( sys ) ) {
                return new String[] { "l", "b" };
            }
            else if ( SkySys.ECLIPTIC2000.equals( sys ) ) {
                return new String[] { "ecl_lon", "ecl_lat" };
            }
            else {
                return new String[] { "lon", "lat" };
            }
        }
    }

    /**
     * Figure implementation representing a small circle on the sky.
     */
    private static class CircleFigure extends SkyFigure {
        final Point p0_;
        final Point p1_;
        final Point2D[] circleVertices_;

        /**
         * Constructor.
         *
         * @param  surf  plotting surface
         * @param  p0    graphics position of circle center
         * @param  p1    graphics position of a point on the radius
         * @param  circleVertices  list of graphics positions outlining the
         *                         circle
         */
        CircleFigure( SkySurface surf, Point p0, Point p1,
                      Point2D[] circleVertices ) {
            super( surf );
            p0_ = p0;
            p1_ = p1;
            circleVertices_ = circleVertices;
        }

        public Area getArea() {
            return createArea( circleVertices_ );
        }

        public void paintPath( Graphics2D g ) {
            g.draw( createPath( circleVertices_, true ) );
            LabelledLine radial = surf_.createLine( p0_, p1_ );
            radial.drawLine( g );
            radial.drawLabel( g, null );
        }

        public Point[] getVertices() {
            return new Point[] { p0_, p1_ };
        }

        public String createSkyExpression( String lonVar, String latVar,
                                           SkyDataGeom varGeom ) {
            return new StringBuffer()
                  .append( F_SKYDISTANCE )
                  .append( "(" )
                  .append( lonVar )
                  .append( ", " )
                  .append( latVar )
                  .append( referencePoints( surf_, new Point[] { p0_ },
                                            varGeom ) )
                  .append( ")" )
                  .append( " < " )
                  .append( formatRadius() )
                  .toString();
        }

        public String createSkyAdql( String lonVar, String latVar,
                                     SkyDataGeom varGeom ) {
            return adqlContains( lonVar, latVar, new StringBuffer()
                .append( "CIRCLE(''" )
                .append( referencePoints( surf_, new Point[] { p0_ }, varGeom ))
                .append( ", " )
                .append( formatRadius() )
                .append( ")" )
                .toString()
            );
        }

        /**
         * Returns a formatted string giving the radius in degrees
         * for this circular figure.
         *
         * @return   radius string ready for insertion into expression
         */
        private String formatRadius() {
            double radius =
                Math.toDegrees( surf_.screenDistanceRadians( p0_, p1_ ) );
            double minPixArea = 4 * Math.PI;
            for ( Point2D v : circleVertices_ ) {
                minPixArea = Math.min( minPixArea,
                                       surf_.pixelAreaSteradians( v ) );
            }
            double radiusEps = Math.toDegrees( Math.sqrt( minPixArea ) );
            return PlotUtil.formatNumber( radius, radiusEps );
        }
    }

    /**
     * Figure implementation representing an ellipse on the sky.
     */
    private static class EllipseFigure extends SkyFigure {
        final Point p0_;
        final double rA_;
        final double rB_;
        final double posAng_;
        final Point2D[] ellipseVertices_;
        final boolean hasP2_;
        final Point pA_;
        final Point pB_;
        static final double rb0_ = 0.5;

        /**
         * Constructor.
         *
         * @param  surf  sky surface
         * @param  p0    ellipse center in graphics coordinates
         * @param  rA    primary semi-major axis length in radians
         * @param  rB    secondary semi-major axis length in radians
         * @param  posAng  angle from North to primary semi-major axis
         *                 in direction of increasing longitude
         * @param  ellipseVertices  list of vertices in graphics coordinates
         *                          representing the ellipse
         * @param  hasP2   whether the point defining the secondary
         *                 semi-major axis has been explicitly supplied
         *                 by the user
         */
        EllipseFigure( SkySurface surf, Point p0, double rA, double rB,
                       double posAng, Point2D[] ellipseVertices,
                       boolean hasP2 ) {
            super( surf );
            p0_ = p0;
            rA_ = rA;
            rB_ = rB;
            posAng_ = posAng;
            ellipseVertices_ = ellipseVertices;
            hasP2_ = hasP2;
            double[] vec0 = surf.graphicsToData( p0 );
            double[] vecA = distanceBearing( vec0, posAng, rA );
            double[] vecB = distanceBearing( vec0, posAng + 0.5 * Math.PI, rB );
            Point2D.Double pA = new Point2D.Double();
            Point2D.Double pB = new Point2D.Double();
            surf.dataToGraphics( vecA, false, pA );
            surf.dataToGraphics( vecB, false, pB );
            pA_ = new Point( (int) Math.round(pA.x), (int) Math.round(pA.y) );
            pB_ = new Point( (int) Math.round(pB.x), (int) Math.round(pB.y) );
        }

        public Area getArea() {
            return createArea( ellipseVertices_ );
        }

        public void paintPath( Graphics2D g ) {
            g.draw( createPath( ellipseVertices_, true ) );
            LabelledLine axisA = surf_.createLine( p0_, pA_ );
            axisA.drawLine( g );
            axisA.drawLabel( g, null );
            LabelledLine axisB = surf_.createLine( p0_, pB_ );
            axisB.drawLine( g );
            axisB.drawLabel( g, null );
        }

        public Point[] getVertices() {
            return hasP2_ ? new Point[] { p0_, pA_, pB_ }
                          : new Point[] { p0_, pA_ };
        }

        public String createSkyExpression( String lonVar, String latVar,
                                           SkyDataGeom varGeom ) {
            return new StringBuffer()
               .append( F_INSKYELLIPSE )
               .append( "(" )
               .append( lonVar )
               .append( ", " )
               .append( latVar )
               .append( referencePoints( surf_, new Point[] { p0_ }, varGeom ) )
               .append( ", " )
               .append( formatRadius( rA_ ) )
               .append( ", " )
               .append( formatRadius( rB_ ) )
               .append( ", " )
               .append( (int) Math.toDegrees( posAng_ ) )
               .append( "." )
               .append( ")" )
               .toString();
        }

        public String createSkyAdql( String lonVar, String latVar,
                                     SkyDataGeom varGeom ) {
            // It's not impossible to come up with a closed form ADQL
            // expression defining this ellipse, but it would be rather
            // long-winded.
            return null;
        }

        /**
         * Returns a formatted string giving a length value in degrees,
         * to a sensible precision, for a semi-major radius of this ellipse.
         *
         * @param  r  length value in radians
         * @return   formatted text in degrees
         */
        private String formatRadius( double r ) {
            double minPixArea = 4 * Math.PI;
            for ( Point2D v : ellipseVertices_ ) {
                minPixArea = Math.min( minPixArea,
                                       surf_.pixelAreaSteradians( v ) );
            }
            double radius = Math.toDegrees( r );
            double radiusEps = Math.toDegrees( Math.sqrt( minPixArea ) );
            return PlotUtil.formatNumber( radius, radiusEps );
        }
    }

    /**
     * Figure implementation representing a polygon on the sky.
     */
    private static class PolygonFigure extends SkyFigure {
        final Point[] points_;
        final Point2D[] lineVertices_;
        final Point2D[] areaVertices_;

        /**
         * Constructor.
         *
         * @param  surf  plotting surface
         * @param  points  graphics points defining the polygon
         * @param  lineVertices   closely separated points giving the path
         *                        in graphics space
         * @param  areaVertices   closely separated points defining the
         *                        closed polygon in graphics space
         */
        PolygonFigure( SkySurface surf, Point[] points,
                       Point2D[] lineVertices, Point2D[] areaVertices ) {
            super( surf );
            points_ = points;
            lineVertices_ = lineVertices;
            areaVertices_ = areaVertices;
        }

        public Area getArea() {
            return createArea( areaVertices_ );
        }

        public void paintPath( Graphics2D g ) {
            g.draw( createPath( lineVertices_, false ) );
        }

        public Point[] getVertices() {
            return points_.clone();
        }

        public String createSkyExpression( String lonVar, String latVar,
                                           SkyDataGeom varGeom ) {
            return points_.length > 2 && areaVertices_ != null
                 ? new StringBuffer()
                      .append( F_INSKYPOLYGON )
                      .append( "(" )
                      .append( lonVar )
                      .append( ", " )
                      .append( latVar )
                      .append( referencePoints( surf_, points_, varGeom ) )
                      .append( ")" )
                      .toString()
                 : null;
        }

        public String createSkyAdql( String lonVar, String latVar,
                                     SkyDataGeom varGeom ) {
            return adqlContains( lonVar, latVar, new StringBuffer()
                .append( "POLYGON(" )
                .append( "''" )
                .append( referencePoints( surf_, points_, varGeom ) )
                .append( ")" )
                .toString()
            );
        }
    }
}
