package uk.ac.starlink.topcat.plot2;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;

/**
 * Defines how an area is constructed from a user-supplied set of
 * vertices on a 2d graphics surface.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2018
 */
public enum PolygonMode {

    /** Area within the polygon bounded by (at least 3) points. */
    INSIDE() {
        public Area createArea( Rectangle bounds, Point[] points ) {
            return createInsideArea( points );
        }
        public String createExpression( PlanarSurface surf, Point[] points,
                                        String xvar, String yvar ) {
            return createInsideExpression( surf, points, xvar, yvar );
        }
    },

    /** Area outside the polygon bounded by (at least 3) points. */
    OUTSIDE() {
        public Area createArea( Rectangle bounds, Point[] points ) {
            return invertArea( bounds, createInsideArea( points ) );
        }
        public String createExpression( PlanarSurface surf, Point[] points,
                                        String xvar, String yvar ) {
            String inExpr = createInsideExpression( surf, points, xvar, yvar );
            return inExpr == null ? null
                                  : "!" + inExpr;
        }
    },

    /** Area below a line formed by joining points. */
    BELOW() {
        public Area createArea( Rectangle bounds, Point[] points ) {
            return invertArea( bounds,
                               createSideArea( true, bounds, points ) );
        }
        public String createExpression( PlanarSurface surf, Point[] points,
                                        String xvar, String yvar ) {
            return createSideExpression( surf, points, xvar, yvar,
                                         true, true );
        }
    },

    /** Area above a line formed by joining points. */
    ABOVE() {
        public Area createArea( Rectangle bounds, Point[] points ) {
            return createSideArea( true, bounds, points );
        }
        public String createExpression( PlanarSurface surf, Point[] points,
                                        String xvar, String yvar ) {
            return createSideExpression( surf, points, xvar, yvar,
                                         true, false );
        }
    },

    /** Area to the left of a line formed by joining points. */
    LEFT() {
        public Area createArea( Rectangle bounds, Point[] points ) {
            return createSideArea( false, bounds, points );
        }
        public String createExpression( PlanarSurface surf, Point[] points,
                                        String xvar, String yvar ) {
            return createSideExpression( surf, points, xvar, yvar,
                                         false, true );
        }
    },

    /** Area to the right of a line formed by joining points. */
    RIGHT() {
        public Area createArea( Rectangle bounds, Point[] points ) {
            return invertArea( bounds,
                               createSideArea( false, bounds, points ) );
        }
        public String createExpression( PlanarSurface surf, Point[] points,
                                        String xvar, String yvar ) {
            return createSideExpression( surf, points, xvar, yvar,
                                         false, false );
        }
    };

    /**
     * Coefficient tolerance in pixels.
     * This value is used to determine to what level of precision
     * positions and coefficients are reported when constructing
     * algebraic expressions to represent polygon areas.
     * The reported coefficients are good enough to reconstruct the
     * areas drawn to within PIXTOL pixels.
     */
    private static final double PIXTOL = 1.0;

    /** JEL functions used when constructing expressions. */
    private static final String F_ISINSIDE;
    private static final String F_POLYLINE;
    private static final String F_LOG10;
    static final String[] JEL_FUNCTIONS = new String[] {
        F_ISINSIDE = "isInside",
        F_POLYLINE = "polyLine",
        F_LOG10 = "log10",
    };

    /**
     * Returns a drawable shape representing the area defined by a
     * set of user-specified graphics points.  The shape does not
     * necessarily extend beyond the supplied bounding rectangle.
     * If the points are not appropriate or sufficient to define
     * a polygon of this type, null is returned.
     *
     * @param  bounds  bounds of graphics space on which the area will be drawn
     * @param  points  vertices in graphics space defining the polygon
     * @return   inclusion shape, or null
     */
    public abstract Area createArea( Rectangle bounds, Point[] points );

    /**
     * Returns an algebraic (JEL) expression defining the area in data space
     * defined by a set of user-specified graphics points.
     * If the points are not appropriate or sufficient to define
     * a polygon of this type, null is returned.
     *
     * @param  surface  plotting surface
     * @param  points  vertices in graphics space defining the polygon
     * @param  xvar   JEL-friendly expression naming the X coordinate
     * @param  yvar   JEL-friendly expression naming the Y coordinate
     * @return   boolean JEL inclusion expression, or null
     */
    public abstract String createExpression( PlanarSurface surface,
                                             Point[] points,
                                             String xvar, String yvar );

    /**
     * Returns the complement of an area.
     *
     * @param  bounds  bounds of graphics space on which the area will be drawn
     * @param  complement   area to be excluded, may be null
     * @return  inclusion shape, or null
     */
    private static Area invertArea( Rectangle bounds, Area complement ) {
        if ( bounds == null || complement == null ) {
            return null;
        }
        else {
            Area result = new Area( bounds );
            result.subtract( complement );
            return result;
        }
    }

    /**
     * Returns a shape corresponding to the inside of a polygon.
     *
     * @param  points  vertices in graphics space defining the polygon
     * @return  inclusion shape, or null if underspecified
     */
    private static Area createInsideArea( Point[] points ) {
        int np = points.length;
        if ( np > 2 ) {
            int[] xs = new int[ np ];
            int[] ys = new int[ np ];
            for ( int ip = 0; ip < np; ip++ ) {
                Point p = points[ ip ];
                xs[ ip ] = p.x;
                ys[ ip ] = p.y;
            }
            return new Area( new Polygon( xs, ys, np ) );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a shape corresponding to the values on the lower side of
     * (graphics coordinate less than) a given line.
     *
     * @param  isY   true for a line like a function Y(x),
     *               false for a line like a function X(y)
     * @param  bounds  bounds of graphics space on which the area will be drawn
     * @param  points  vertices defining the line
     * @return  inclusion shape or null
     */
    private static Area createSideArea( boolean isY, Rectangle bounds,
                                        Point[] points ) {
        int np = points.length;

        /* No points, no area. */
        if ( np == 0 ) {
            return null;
        }

        /* One point, take the whole region to one side of it. */
        else if ( np == 1 ) {
            Point p0 = points[ 0 ];
            Rectangle result = new Rectangle( bounds );
            if ( isY ) {
                result.height = Math.max( 0, p0.y - bounds.y );
            }
            else {
                result.width = Math.max( 0, p0.x - bounds.x );
            }
            return new Area( result );
        }
        else {

            /* Check abcissa is strictly monotonic; if not, return null. */
            for ( int ip = 0; ip < np - 2; ip++ ) {
                int[] cs = new int[ 3 ];
                for ( int j = 0; j < 3; j++ ) {
                    Point p = points[ ip + j ];
                    cs[ j ] = isY ? p.x : p.y;
                }
                if ( ( cs[ 2 ] - cs[ 1 ] ) * ( cs[ 1 ] - cs[ 0 ] ) <= 0 ) {
                    return null;
                }
            }

            /* Build up the shape from a trapezium going to the edge of
             * the bounding rectangle at either end, and then a trapezium
             * for each section for the non-end points. */
            Area result = new Area();
            result.add( createEdgeTrapezium( isY, bounds,
                                             points[ 0 ], points[ 1 ] ) );
            for ( int ip = 0; ip < np - 2; ip++ ) {
                result.add( createBoundedTrapezium( isY, bounds,
                                                    points[ ip + 1 ],
                                                    points[ ip + 2 ] ) );
            }
            result.add( createEdgeTrapezium( isY, bounds,
                                             points[ np - 1 ],
                                             points[ np - 2 ] ) );
            return result;
        }
    }

    /**
     * Creates a trapezium corresponding to a line infinitely extended
     * in one direction.
     *
     * @param  isY   true for trapezium extending to vertical axis
     *               false for trapezium extending to horizontal axis,
     * @param  bounds  bounds of graphics space
     * @param  p0  the point in the direction of extension to the edge
     * @param  p1  the point at a vertex
     * @return  shape
     */
    private static Area createEdgeTrapezium( boolean isY, Rectangle bounds,
                                             Point p0, Point p1 ) {
        final int x2;
        final int y2;
        if ( isY ) {
            x2 = p0.x < p1.x ? bounds.x : bounds.x + bounds.width;
            y2 = (int) ( ( p1.y - p0.y ) / (double) ( p1.x - p0.x )
                         * ( x2 - p0.x ) )
               + p0.y;
        }
        else {
            y2 = p0.y < p1.y ? bounds.y : bounds.y + bounds.height;
            x2 = (int) ( ( p1.x - p0.x ) / (double) ( p1.y - p0.y )
                         * ( y2 - p0.y ) )
               + p0.x;
        }
        return createBoundedTrapezium( isY, bounds, p1, new Point( x2, y2 ) );
    }

    /**
     * Creates a trapezium with sides dropping to one edge of the bounding
     * rectangle and a sloping section between two given points.
     *
     * @param  isY  true for trapezium extending to vertical axis,
     *              false for trapezium extending to horizontal axis
     * @param  bounds  bounds of graphics space
     * @param  p1  one vertex
     * @param  p2  other vertex
     * @return  shape
     */
    private static Area createBoundedTrapezium( boolean isY, Rectangle bounds,
                                                Point p1, Point p2 ) {
        int[] xs = isY ? new int[] { p1.x, p1.x, p2.x, p2.x }
                       : new int[] { bounds.x, p1.x, p2.x, bounds.x };
        int[] ys = isY ? new int[] { bounds.y, p1.y, p2.y, bounds.y }
                       : new int[] { p1.y, p1.y, p2.y, p2.y };
        return new Area( new Polygon( xs, ys, 4 ) );
    }

    /**
     * Returns a JEL expression representing inclusion in a closed polygon.
     *
     * @param  surf  plotting surface
     * @param  points  graphics points defining polygon vertices
     * @param  xvar  name of X coordinate
     * @param  yvar  name of Y coordinate
     * @return  JEL inequality expression, or null if not enough points
     */
    private static String createInsideExpression( PlanarSurface surf,
                                                  Point[] points,
                                                  String xvar, String yvar ) {
        return points.length > 2
             ? new StringBuffer()
                  .append( F_ISINSIDE )
                  .append( "(" )
                  .append( referenceName( surf, xvar, 0 ) )
                  .append( ", " )
                  .append( referenceName( surf, yvar, 1 ) )
                  .append( referencePoints( surf, points, true ) )
                  .append( ")" )
                  .toString()
             : null;
    }

    /**
     * Returns a JEL expression representing inclusion in the area to one side
     * of a line defined by a set of user-supplied points.
     *
     * @param  surf  plotting surface
     * @param  points  graphics points defining line vertices
     * @param  xvar  name of X coordinate
     * @param  yvar  name of Y coordinate
     * @param  isYfunc  true for a line that corresponds to a function
     *                  defining y as a function of x;
     *                  false for the other way round
     * @param  isLess   true for graphics coordinates that are lower than
     *                  the given line; false for greater than the given line
     * @return  JEL inequality expression, or null if points are not suitable
     */
    private static String createSideExpression( PlanarSurface surf,
                                                Point[] points,
                                                String xvar, String yvar,
                                                boolean isYfunc,
                                                boolean isLess ) {
        int np = points.length;
        boolean[] logFlags = surf.getLogFlags();
        Axis[] axes = surf.getAxes();
        String operator = surf.getFlipFlags()[ isYfunc ? 1 : 0 ]
                        ? ( isLess ? ">" : "<=" )
                        : ( isLess ? "<" : ">=" );

        /* No points, no region. */
        if ( np == 0 ) {
            return null;
        }

        /* One point, take the whole region to one side of it. */
        else if ( np == 1 ) {
            Point p0 = points[ 0 ];
            Axis axis = axes[ isYfunc ? 1 : 0 ];
            double gval = isYfunc ? p0.y : p0.x;
            double dval = axis.graphicsToData( gval );
            double dval1 = axis.graphicsToData( gval - 0.5 / PIXTOL );
            double dval2 = axis.graphicsToData( gval + 0.5 / PIXTOL );
            double epsilon = Math.abs( dval2 - dval1 );
            String value = PlotUtil.formatNumber( dval, epsilon );
            return new StringBuffer()
                  .append( isYfunc ? yvar : xvar )
                  .append( " " )
                  .append( operator )
                  .append( " " )
                  .append( value )
                  .toString();
        }

        /* Two points, construct a linear inequality.
         * We could just use the special shape functions here
         * (same case as np>2), but doing it this way gives an
         * expression which is more comprehensible for users and easier
         * to transfer to other contexts (such as a research paper).
         * The functional form of the line is (e.g.) "y < M * (x-x1) + y1",
         * where (x1,y1) is one of the supplied points.
         * This is preferred to the more conventional "y < m * x + c"
         * because it's less sensitive to the precision of the gradient
         * (if the visible range of x is far from zero), which means that
         * the gradient m can be reported with fewer significant figures. */
        else if ( np == 2 ) {
            Point p1 = points[ 0 ];
            Point p2 = points[ 1 ];
            boolean xlog = logFlags[ 0 ];
            boolean ylog = logFlags[ 1 ];
            Axis xaxis = axes[ 0 ];
            Axis yaxis = axes[ 1 ];
            String xref = referenceName( surf, xvar, 0 );
            String yref = referenceName( surf, yvar, 1 );
            double dx1 = xaxis.graphicsToData( p1.x );
            double dy1 = yaxis.graphicsToData( p1.y );
            double dx2 = xaxis.graphicsToData( p2.x );
            double dy2 = yaxis.graphicsToData( p2.y );
            double rx1 = xlog ? Math.log10( dx1 ) : dx1;
            double ry1 = ylog ? Math.log10( dy1 ) : dy1;
            double rx2 = xlog ? Math.log10( dx2 ) : dx2;
            double ry2 = ylog ? Math.log10( dy2 ) : dy2;
            double m = isYfunc ? ( ry2 - ry1 ) / ( rx2 - rx1 )
                               : ( rx2 - rx1 ) / ( ry2 - ry1 );
            double c = isYfunc ? ry1 - m * rx1
                               : rx1 - m * ry1;
            double xEpsilon = getPixelEpsilon( surf, 0, p1.x );
            double yEpsilon = getPixelEpsilon( surf, 1, p1.y );

            /* Calculate the tolerance on the gradient by identifying
             * the value that will make a difference of at most PIXTOL 
             * pixels on either edge of the visible plot bounds. */
            double mEpsilon = 0;
            for ( double dlim : surf.getDataLimits()[ isYfunc ? 0 : 1 ] ) {
                double rlim = ( isYfunc ? xlog : ylog ) ? Math.log10( dlim )
                                                        : dlim;
                double dm = Math.abs( ( isYfunc ? yEpsilon : xEpsilon )
                                    / ( rlim - ( isYfunc ? rx1 : ry1 ) ) );
                mEpsilon = mEpsilon > 0 ? Math.min( mEpsilon, dm ) : dm;
            }
            return new StringBuffer()
                  .append( isYfunc ? yref : xref )
                  .append( " " )
                  .append( operator )
                  .append( " " )
                  .append( PlotUtil.formatNumber( m, mEpsilon ) )
                  .append( " * " )
                  .append( "(" )
                  .append( isYfunc ? xref : yref )
                  .append( isYfunc ? addFormattedValue( -rx1, xEpsilon )
                                   : addFormattedValue( -ry1, yEpsilon ) )
                  .append( ")" )
                  .append( isYfunc ? addFormattedValue( +ry1, yEpsilon )
                                   : addFormattedValue( +rx1, xEpsilon ) )
                  .toString();
        }

        /* Use the special functions from uk.ac.starlink.ttools.func.Shapes. */
        else {
            String xref = referenceName( surf, xvar, 0 );
            String yref = referenceName( surf, yvar, 1 );
            return new StringBuffer()
                  .append( isYfunc ? yref : xref )
                  .append( " " )
                  .append( operator )
                  .append( " " )
                  .append( F_POLYLINE )
                  .append( "(" )
                  .append( isYfunc ? xref : yref )
                  .append( referencePoints( surf, points, isYfunc ) )
                  .append( ")" )
                  .toString();
        }
    }

    /**
     * Returns a list of x, y coordinates in data space corresponding
     * to a list of points in graphics space.
     * For N submitted points, the output is of the form
     * ", x1, y1, x2, y2... ,xN, yN" or
     * ", y1, x1, y2, x2... ,yN, xN".
     *
     * @param  surf  plotting surface
     * @param  gps   array of N points in graphics space
     * @param  isXy  true for X,Y output sequence,
     *              false for Y,X output sequence
     * @return  string giving a comma-separated list of data coordinates,
     *          or log(data) coordinates if the axis is logarithmic
     */
    private static String referencePoints( PlanarSurface surf, Point[] gps,
                                           boolean isXy ) {
         StringBuffer sbuf = new StringBuffer();
         for ( Point point : gps ) {
             sbuf.append( ", " )
                 .append( referenceValue( surf, point, isXy ? 0 : 1 ) )
                 .append( "," )
                 .append( referenceValue( surf, point, isXy ? 1 : 0 ) );
         }
         return sbuf.toString();
    }

    /**
     * Provides a string by which a linear coordinate can be referred to.
     * This is either the supplied variable name itself, or an expression
     * calculating its (base 10) logarithm.
     *
     * @param  surf  plotting surface
     * @param  varname  JEL-friendly data space variable name
     * @param  icoord   coordinate index; 0 for X, 1 for Y
     * @return  JEL-friendly linear expression referencing <code>varname</code>
     */
    private static String referenceName( PlanarSurface surf, String varname,
                                         int icoord ) {
        return surf.getLogFlags()[ icoord ]
             ? new StringBuffer()
                  .append( F_LOG10 )
                  .append( "(" )
                  .append( varname )
                  .append( ")" )
                  .toString()
             : varname;
    }

    /**
     * Returns a string suitable for appending to an expression that
     * adds a given value to it.
     *
     * @param  value  numeric value
     * @param  epsilon   precision level for formatting
     * @return JEL-friendly string "+ <value>" or "- (-<value>)"
     */
    private static String addFormattedValue( double value, double epsilon ) {
        return new StringBuffer()
              .append( ' ' )
              .append( value >= 0 ? '+' : '-' )
              .append( ' ' )
              .append( PlotUtil.formatNumber( Math.abs( value ), epsilon ) )
              .toString();
    }

    /**
     * Provides an expression referring to the data coordinate
     * of a graphics point suitable for use in linear expressions;
     * this may be a logarithm.  The value is formatted with a precision
     * corresponding to the size of a pixel.
     *
     * @param  surf   plotting surface
     * @param  gp    point in graphics coordinates
     * @param  icoord   coordinate index; 0 for X, 1 for Y
     * @return  JEL-friendly number with suitable precision
     */
    private static String referenceValue( PlanarSurface surf, Point gp,
                                          int icoord ) {
        Axis axis = surf.getAxes()[ icoord ];
        boolean isLog = surf.getLogFlags()[ icoord ];
        double gval = new int[] { gp.x, gp.y }[ icoord ];
        double dval = axis.graphicsToData( gval );
        double rval = isLog ? Math.log10( dval ) : dval;
        double epsilon = getPixelEpsilon( surf, icoord, gval );
        return PlotUtil.formatNumber( rval, epsilon );
    }

    /**
     * Returns a value in data (or log-data) coordinates corresponding
     * to a suitable accuracy for numeric reporting.  The returned value
     * is scaled by the size of a screen pixel in the relevant dimension.
     *
     * @param  surf   plotting surface
     * @param  icoord   coordinate index; 0 for X, 1 for Y
     * @param  gval   value in graphics coordinates
     * @param  precision size
     */
    private static double getPixelEpsilon( PlanarSurface surf, int icoord,
                                           double gval ) {
        boolean isLog = surf.getLogFlags()[ icoord ];
        Axis axis = surf.getAxes()[ icoord ];
        double dval = axis.graphicsToData( gval );
        double dval1 = axis.graphicsToData( gval - 0.5 / PIXTOL );
        double dval2 = axis.graphicsToData( gval + 0.5 / PIXTOL );
        double rval = isLog ? Math.log10( dval ) : dval;
        double rval1 = isLog ? Math.log10( dval1 ) : dval1;
        double rval2 = isLog ? Math.log10( dval2 ) : dval2;
        return Math.abs( rval2 - rval1 );
    }
}
