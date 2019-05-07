package uk.ac.starlink.topcat.plot2;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LabelledLine;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
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
        CIRCLE, POLYGON,
    };

    private static final String F_INSKYPOLYGON;
    private static final String F_SKYDISTANCE;

    /** JEL functions used when constructing expressions. */
    static final String[] JEL_FUNCTIONS = new String[] {
        F_INSKYPOLYGON = "inSkyPolygon",
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
     * @retrun  circle figure, or null
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
            String[] labels =
                TopcatJELUtils.getDataExpressions( tcModel, skyCont );
            if ( labels.length == 2 &&
                 labels[ 0 ] != null && labels[ 1 ] != null ) {
                DataGeom cloudGeom = cloud.getDataGeom();
                assert cloudGeom instanceof SkyDataGeom;
                SkyDataGeom varGeom = cloudGeom instanceof SkyDataGeom
                                    ? (SkyDataGeom) cloudGeom
                                    : SkyDataGeom.GENERIC;
                return createSkyExpression( labels[ 0 ], labels[ 1 ], varGeom );
            }
            else {
                return null;
            }
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
