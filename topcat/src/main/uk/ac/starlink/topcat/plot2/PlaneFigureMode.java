package uk.ac.starlink.topcat.plot2;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.function.BinaryOperator;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.LabelledLine;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;

/**
 * FigureMode implementations for use with a PlanarSurface.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2019
 */
public abstract class PlaneFigureMode implements FigureMode {

    private final String name_;

    /** PlanarSurface area within the polygon bounded by (&gt;=3) points. */
    public static final FigureMode POLYGON = new PlaneFigureMode( "Polygon" ) {
        public Figure createFigure( Surface surf, Point[] points ) {
            return surf instanceof PlanarSurface && points.length > 1
                 ? new PolygonFigure( (PlanarSurface) surf, points )
                 : null;
        }
    };

    /** PlanarSurface area within a rectangle aligned with the axes. */
    public static final FigureMode BOX = createBoxMode( "Box" );

    /** PlanarSurface area within a graphics ellipse (center+radius). */
    public static final FigureMode ELLIPSE =
        createEllipseMode( "Aligned Ellipse" );

    /** PlanarSurface area within a rotated ellipse. */
    public static final FigureMode ROTATED_ELLIPSE =
        createRotatedEllipseMode( "Rotated Ellipse" );

    /** Inverse of polygon mode. */
    public static final FigureMode OUTSIDE_POLYGON = invertMode( POLYGON );

    /** PlanarSurface area below a line formed by joining points. */
    public static final FigureMode BELOW =
        createSideMode( "Below", true, true );

    /** PlanarSurface area above a line formed by joining points. */
    public static final FigureMode ABOVE = 
        createSideMode( "Above", true, false );

    /** PlanarSurface area to the left of a line formed by joining points. */
    public static final FigureMode LEFT =
        createSideMode( "Left", false, true );

    /** PlanarSurface area to the right of a line formed by joining points. */
    public static final FigureMode RIGHT =
        createSideMode( "Right", false, false );

    /** Available polygon modes for use with planar surfaces. */
    public static final FigureMode[] MODES = {
        POLYGON, BOX, ELLIPSE, ROTATED_ELLIPSE, BELOW, ABOVE, LEFT, RIGHT,
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

    private static final String F_ISINSIDE;
    private static final String F_POLYLINE;
    private static final String F_LOG10;
    private static final String F_SQUARE;

    /** JEL functions used when constructing expressions. */
    static final String[] JEL_FUNCTIONS = new String[] {
        F_ISINSIDE = "isInside",
        F_POLYLINE = "polyLine",
        F_LOG10 = "log10",
        F_SQUARE = "square",
    };

    /**
     * Constructor.
     *
     * @param  name  user-visible mode name
     */
    private PlaneFigureMode( String name ) {
        name_ = name;
    }

    public String getName() {
        return name_;
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
     * Provides a string by which a linear coordinate can be referred to
     * in ADQL.  This is either the supplied variable name itself, or
     * an ADQL expression calculating its (base 10) logarithm.
     *
     * @param  surf  plotting surface
     * @param  varname  ADQL-friendly data space variable name
     * @param  icoord   coordinate index; 0 for X, 1 for Y
     * @return  ADQL-friendly linear expression referencing <code>varname</code>
     */
    private static String referenceAdqlName( PlanarSurface surf, String varname,
                                             int icoord ) {
        return surf.getLogFlags()[ icoord ]
             ? new StringBuffer()
                  .append( "LOG10(" )
                  .append( varname )
                  .append( ")" )
                  .toString()
             : varname;
    }

    /**
     * Returns a string suitable for appending to an expression that
     * adds a given value to it, with a given level of precision.
     *
     * @param  value  numeric value
     * @param  epsilon   precision level for formatting
     * @return JEL-friendly string "+ value" or "- (-value)"
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
     * Returns a string suitable for appending to an expression that
     * adds a given value to it, with a given number of significant figures.
     *
     * @param  value  numeric value
     * @param  nsf  guideline number of significant figures
     * @return JEL-friendly string "+ value" or "- (-value)"
     */
    private static String addFormattedValueSf( double value, int nsf ) {
        return new StringBuffer()
              .append( ' ' )
              .append( value >= 0 ? '+' : '-' )
              .append( ' ' )
              .append( PlotUtil.formatNumberSf( Math.abs( value ), nsf ) )
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

    /**
     * Returns a formatted string representing the numeric value in
     * data coordinates corresponding to a given graphics coordinate value
     * on a given axis.
     * 
     * @param  axis  axis
     * @param  gval  position in graphics coordinates along axis
     * @return   value in data coordinates along axis,
     *           formatted to an appropriate precision
     */
    private static String formatGraphicsCoordinate( Axis axis, double gval ) {
        double dval = axis.graphicsToData( gval );
        double dval1 = axis.graphicsToData( gval - 0.5 / PIXTOL );
        double dval2 = axis.graphicsToData( gval + 0.5 / PIXTOL );
        double epsilon = Math.abs( dval2 - dval1 );
        return PlotUtil.formatNumber( dval, epsilon );
    }

    /**
     * Returns a figure mode which includes the opposite of a given mode.
     *
     * @param  inMode  mode to invert
     * @return  inverted mode
     */
    private static FigureMode invertMode( final FigureMode inMode ) {
        return new FigureMode() {
            public String getName() {
                return "NOT_" + inMode.getName();
            }
            public Figure createFigure( final Surface surf, Point[] points ) {
                final PlaneFigure inFig =
                    (PlaneFigure) inMode.createFigure( surf, points );
                return new PlaneFigure( (PlanarSurface) surf, points ) {
                    public Area getArea() {
                        return invertArea( inFig.getArea() );
                    }
                    public void paintPath( Graphics2D g ) {
                        inFig.paintPath( g );
                    }
                    public Point[] getVertices() {
                        return inFig.getVertices();
                    }
                    public String createPlaneExpression( String xvar,
                                                         String yvar ) {
                        return "!(" + inFig.createPlaneExpression( xvar, yvar )
                             + ")";
                    }
                    public String createPlaneAdql( String xvar, String yvar ) {
                        String opp = inFig.createPlaneAdql( xvar, yvar );

                        /* Currently only used to invert INSIDE_POLYGON
                         * which is anyway not supported in ADQL, so no
                         * point doing much work here. */
                        if ( opp == null ) {
                            return null;
                        }
                        else {
                            assert false;
                            return null;
                        }
                    }
                };
            }
        };
    }

    /**
     * Defines a shape corresponding to the values to one side of a line
     * given by supplied points.
     *
     * @param  name   mode name
     * @param  isYfunc true for a line that corresponds to a function
     *                 defining y as a function of x;
     *                 false for the other way round
     * @param  isLess  true for graphics coordinates that are lower than
     *                 the given line; false for greater than the given line
     */
    private static PlaneFigureMode createSideMode( String name,
                                                   final boolean isYfunc,
                                                   final boolean isLess ) {
        return new PlaneFigureMode( name ) {
            public Figure createFigure( Surface surf, Point[] points ) {
                if ( surf instanceof PlanarSurface ) {
                    PlanarSurface psurf = (PlanarSurface) surf;
                    int np = points.length;

                    /* No points, no figure. */
                    if ( np == 0 ) {
                        return null;
                    }

                    /* One point, area on one side of point. */
                    else if ( np == 1 ) {
                        return new LineSideFigure( psurf, points[ 0 ],
                                                   isYfunc, isLess );
                    }

                    /* Otherwise, if check the abcissa is strictly monontonic,
                     * return a polyside figure, else return null. */
                    else {
                        for ( int ip = 0; ip < np - 2; ip++ ) {
                            int[] cs = new int[ 3 ];
                            for ( int j = 0; j < 3; j++ ) {
                                Point p = points[ ip + j ];
                                cs[ j ] = isYfunc ? p.x : p.y;
                            }
                            if ( ( cs[2] - cs[1] ) * ( cs[1] - cs[0] ) <= 0 ) {
                                return null;
                            }
                        }
                        return new PolySideFigure( psurf, points,
                                                   isYfunc, isLess );
                    }
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a mode for drawing boxes.
     *
     * @param  name  mode name
     * @return  new instance
     */
    private static FigureMode createBoxMode( String name ) {
        return new PlaneFigureMode( name ) {
            public Figure createFigure( Surface surf, Point[] points ) {
                int np = points.length;
                return surf instanceof PlanarSurface && np >= 2
                     ? new BoxFigure( (PlanarSurface) surf,
                                      points[ np - 2 ], points[ np - 1 ] )
                     : null;
            }
        };
    }

    /**
     * Returns a mode for drawing ellipses.
     *
     * @param  name  mode name
     * @return  new instance
     */
    private static FigureMode createEllipseMode( String name ) {
        return new PlaneFigureMode( name ) {
            public Figure createFigure( Surface surf, Point[] points ) {
                int np = points.length;
                return surf instanceof PlanarSurface && np >= 2
                     ? new EllipseFigure( (PlanarSurface) surf,
                                          points[ 0 ], points[ np - 1 ] )
                     : null;
            }
        };
    }

    /**
     * Returns a mode for drawing rotatable ellipses.
     *
     * @param  name  mode name
     * @return  new instance
     */
    private static FigureMode createRotatedEllipseMode( String name ) {
        return new PlaneFigureMode( name ) {
            public Figure createFigure( Surface surf, Point[] points ) {
                int np = points.length;
                if ( surf instanceof PlanarSurface ) {
                    PlanarSurface psurf = (PlanarSurface) surf;
                    if ( np >= 3 ) {
                        return new RotatedEllipseFigure( psurf, points[ 0 ],
                                                         points[ 1 ],
                                                         points[ np - 1 ] );
                    }
                    else if ( np == 2 ) {
                        return new RotatedEllipseFigure( psurf, points[ 0 ],
                                                         points[ 1 ], null );
                    }
                }
                return null;
            }
        };
    }

    /**
     * Partial Figure implementation for use with PlaneFigureMode.
     */
    static abstract class PlaneFigure implements Figure {
        final PlanarSurface surf_;
        final Point[] points_;
        final Rectangle bounds_;

        /**
         * Constructor.
         *
         * @param  surf  plot surface
         * @param  points  vertices defining figure
         */
        PlaneFigure( PlanarSurface surf, Point[] points ) {
            surf_ = surf;
            points_ = points;
            bounds_ = new Rectangle( surf.getPlotBounds() );
        }

        public String createExpression( TableCloud cloud ) {
            GuiCoordContent xContent = cloud.getGuiCoordContent( 0 );
            GuiCoordContent yContent = cloud.getGuiCoordContent( 1 );
            TopcatModel tcModel = cloud.getTopcatModel();
            String xvar = TopcatJELUtils.getDataExpression( tcModel, xContent );
            String yvar = TopcatJELUtils.getDataExpression( tcModel, yContent );
            return xvar != null && yvar != null
                 ? createPlaneExpression( xvar, yvar )
                 : null;
        }

        /**
         * Returns a JEL expression defining the area in data space
         * defined by a set of graphics points, given the X and Y variable
         * expressions.
         *
         * @param  xvar   JEL-friendly expression naming the X coordinate
         * @param  yvar   JEL-friendly expression naming the Y coordinate
         * @return   boolean JEL inclusion expression, or null
         */
        abstract String createPlaneExpression( String xvar, String yvar );

        /**
         * Returns an ADQL expression representing the area in data space
         * defined by a set of graphics points, given the X and Y variable
         * expressions.
         *
         * @param  xvar   ADQL-friendly expression naming the X coordinate
         * @param  yvar   ADQL-friendly expression naming the Y coordinate
         * @return   boolean ADQL inclusion expression, or null
         */
        abstract String createPlaneAdql( String xvar, String yvar );

        public String getExpression() {
            return createPlaneExpression( "X", "Y" );
        }

        public String getAdql() {
            return createPlaneAdql( "X", "Y" );
        }

        /**
         * Paints an unclosed path joining all this figure's points.
         *
         * @param  g  graphics context
         */
        void paintPolyPath( Graphics2D g ) {
            Path2D path = new Path2D.Double();
            Point p0 = points_[ 0 ];
            path.moveTo( p0.getX(), p0.getY() );
            for ( int ip = 1; ip < points_.length; ip++ ) {
                Point p = points_[ ip ];
                path.lineTo( p.getX(), p.getY() );
            }
            g.draw( path );
        }

        /**
         * Returns an area which includes all of the visible plotting surface
         * except for that included in a supplied area.
         *
         * @param  area   area to invert
         * @return   complement of area
         */
        Area invertArea( Area area ) {
            Area result = new Area( bounds_ );
            result.subtract( area );
            return result;
        }
    }

    /**
     * Figure implementation for an enclosing polygon.
     */
    private static class PolygonFigure extends PlaneFigure {

        /**
         * Constructor.
         *
         * @param  surf  plotting surface
         * @param  points   points defining polygon
         */
        PolygonFigure( PlanarSurface surf, Point[] points ) {
            super( surf, points );
        }

        public Area getArea() {
            int np = points_.length;
            int[] xs = new int[ np ];
            int[] ys = new int[ np ];
            for ( int ip = 0; ip < np; ip++ ) {
                Point p = points_[ ip ];
                xs[ ip ] = p.x;
                ys[ ip ] = p.y;
            }
            return new Area( new Polygon( xs, ys, np ) );
        }

        public void paintPath( Graphics2D g ) {
            paintPolyPath( g );
        }

        public Point[] getVertices() {
            return points_.clone();
        }

        public String createPlaneExpression( String xvar, String yvar ) {
            return new StringBuffer()
                  .append( F_ISINSIDE )
                  .append( "(" )
                  .append( referenceName( surf_, xvar, 0 ) )
                  .append( ", " )
                  .append( referenceName( surf_, yvar, 1 ) )
                  .append( referencePoints( surf_, points_, true ) )
                  .append( ")" )
                  .toString();
        }

        public String createPlaneAdql( String xvar, String yvar ) {

            /* ADQL POLYGON function is specific to spherical geometry,
             * so there's no straightforward way to write this. */
            return null;
        }
    }

    /**
     * Figure implementation for a rectangle aligned with the axes.
     */
    private static class BoxFigure extends PlaneFigure {

        private final Point p0_;
        private final Point p1_;
        private final Rectangle rect_;

        /**
         * Constructor.
         *
         * @param  surf  plot surface
         * @param  p0    one corner
         * @param  p1    opposite corner
         */
        BoxFigure( PlanarSurface surf, Point p0, Point p1 ) {
            super( surf, new Point[] { p0, p1 } );
            p0_ = p0;
            p1_ = p1;
            rect_ = new Rectangle( Math.min( p0.x, p1.x ),
                                   Math.min( p0.y, p1.y ),
                                   Math.abs( p1.x - p0.x ),
                                   Math.abs( p1.y - p0.y ) );
        }

        public Area getArea() {
            return new Area( rect_ );
        }

        public void paintPath( Graphics2D g ) {
            g.draw( rect_ );
        }

        public Point[] getVertices() {
            return new Point[] { p0_, p1_ };
        }

        public String createPlaneExpression( String xvar, String yvar ) {
            String[] xLimits = getFormattedLimits( false );
            String[] yLimits = getFormattedLimits( true );
            return new StringBuffer()
                .append( xvar )
                .append( " > " )
                .append( xLimits[ 0 ] )
                .append( " && " )
                .append( xvar )
                .append( " < " )
                .append( xLimits[ 1 ] )
                .append( " && " )
                .append( yvar )
                .append( " > " )
                .append( yLimits[ 0 ] )
                .append( " && " )
                .append( yvar )
                .append( " < " )
                .append( yLimits[ 1 ] )
                .toString();
        }

        public String createPlaneAdql( String xvar, String yvar ) {
            String[] xLimits = getFormattedLimits( false );
            String[] yLimits = getFormattedLimits( true );
            return new StringBuffer()
               .append( xvar )
               .append( " BETWEEN " )
               .append( xLimits[ 0 ] )
               .append( " AND " )
               .append( xLimits[ 1 ] )
               .append( " AND " )
               .append( yvar )
               .append( " BETWEEN " )
               .append( yLimits[ 0 ] )
               .append( " AND " )
               .append( yLimits[ 1 ] )
               .toString();
        }

        /**
         * Returns a pair of formatted strings corresponding to data space
         * coordinate values giving the bounds of this box on one of the axes.
         *
         * @param  isY  true for Y axis, false for X axis
         * @return  2-element array giving (lower,upper) bound
         */
        private String[] getFormattedLimits( boolean isY ) {
            Axis axis = surf_.getAxes()[ isY ? 1 : 0 ];
            int g0 = isY ? p0_.y : p0_.x;
            int g1 = isY ? p1_.y : p1_.x;
            double d0 = axis.graphicsToData( g0 );
            double d1 = axis.graphicsToData( g1 );
            int[] gs = d0 < d1 ? new int[] { g0, g1 } : new int[] { g1, g0 };
            return new String[] {
                formatGraphicsCoordinate( axis, gs[ 0 ] ),
                formatGraphicsCoordinate( axis, gs[ 1 ] ),
            };
        }
    }

    /**
     * Figure implementation for an ellipse.
     */
    private static class EllipseFigure extends PlaneFigure {
        final Point p0_;
        final Point p1_;
        final Ellipse2D ellipse_;
        final double dx_;
        final double dy_;
        final double cx_;
        final double cy_;
        final double xEps_;
        final double yEps_;

        /**
         * Constructor.
         *
         * @param  surf  plot surface
         * @param  p0    center
         * @param  p1    point on radius
         */
        EllipseFigure( PlanarSurface surf, Point p0, Point p1 ) {
            super( surf, new Point[] { p0, p1 } );
            p0_ = p0;
            p1_ = p1;
            double scale = Math.sqrt( 2.0 );
            double rx = Math.abs( p1.x - p0.x ) * scale;
            double ry = Math.abs( p1.y - p0.y ) * scale;
            ellipse_ =
                new Ellipse2D.Double( p0.x - rx, p0.y - ry, 2 * rx, 2 * ry );
            Axis[] axes = surf_.getAxes();
            Axis xAxis = axes[ 0 ];
            Axis yAxis = axes[ 1 ];
            boolean[] logFlags = surf_.getLogFlags();
            boolean xlog = logFlags[ 0 ];
            boolean ylog = logFlags[ 1 ];
            double x0 = xAxis.graphicsToData( p0_.x );
            double y0 = yAxis.graphicsToData( p0_.y );
            double x1 = xAxis.graphicsToData( p0_.x + rx );
            double y1 = yAxis.graphicsToData( p0_.y + ry );
            dx_ = Math.abs( xlog ? Math.log10( x1 / x0 ) : x1 - x0 );
            dy_ = Math.abs( ylog ? Math.log10( y1 / y0 ) : y1 - y0 );
            cx_ = xlog ? Math.log10( x0 ) : x0;
            cy_ = ylog ? Math.log10( y0 ) : y0;
            double xa = xAxis.graphicsToData( p0_.x + 1 );
            double ya = yAxis.graphicsToData( p0_.y + 1 );
            xEps_ = Math.abs( xlog ? Math.log10( xa / x0 ) : xa - x0 );
            yEps_ = Math.abs( ylog ? Math.log10( ya / y0 ) : ya - y0 );
        }

        public Area getArea() {
            return new Area( ellipse_ );
        }

        public void paintPath( Graphics2D g ) {
            g.draw( ellipse_ );
            int gw = (int) ( ellipse_.getWidth() / 2 );
            int gh = (int) ( ellipse_.getHeight() / 2 );
            LabelledLine xline =
                new LabelledLine( p0_, new Point( p0_.x + gw, p0_.y ),
                                  PlotUtil.formatNumber( Math.abs( dx_ ),
                                                         xEps_ ) );
            LabelledLine yline =
                new LabelledLine( p0_, new Point( p0_.x, p0_.y - gh ),
                                  PlotUtil.formatNumber( Math.abs( dy_ ),
                                                         yEps_ ) );
            xline.drawLine( g );
            yline.drawLine( g );
            boolean[] logFlags = surf_.getLogFlags();
            if ( ! logFlags[ 0 ] ) {
                xline.drawLabel( g, null );
            }
            if ( ! logFlags[ 1 ] ) {
                yline.drawLabel( g, null );
            }
        }

        public Point[] getVertices() {
            return new Point[] { p0_, p1_ };
        }

        public String createPlaneExpression( String xvar, String yvar ) {
            return new StringBuffer()
                 .append( F_SQUARE )
                 .append( "(" )
                 .append( "(" )
                 .append( referenceName( surf_, xvar, 0 ) )
                 .append( addFormattedValue( -cx_, xEps_ ) )
                 .append( ")" )
                 .append( "/" )
                 .append( PlotUtil.formatNumber( dx_, xEps_ ) )
                 .append( ")" )
                 .append( " + " )
                 .append( F_SQUARE )
                 .append( "(" )
                 .append( "(" )
                 .append( referenceName( surf_, yvar, 1 ) )
                 .append( addFormattedValue( -cy_, yEps_ ) )
                 .append( ")" )
                 .append( "/" )
                 .append( PlotUtil.formatNumber( dy_, yEps_ ) )
                 .append( ")" )
                 .append( " < " )
                 .append( "1" )
                 .toString();
        }

        public String createPlaneAdql( String xvar, String yvar ) {
            return new StringBuffer()
                .append( "POWER(" )
                .append( "(" )
                .append( referenceAdqlName( surf_, xvar, 0 ) )
                .append( addFormattedValue( -cx_, xEps_ ) )
                .append( ")" )
                .append( "/" )
                .append( PlotUtil.formatNumber( dx_, xEps_ ) )
                .append( ", 2)" )
                .append( " + " )
                .append( "POWER(" )
                .append( "(" )
                .append( referenceAdqlName( surf_, yvar, 1 ) )
                .append( addFormattedValue( -cy_, yEps_ ) )
                .append( ")" )
                .append( "/" )
                .append( PlotUtil.formatNumber( dy_, yEps_ ) )
                .append( ", 2)" )
                .append( " < " )
                .append( "1" )
                .toString();
        }
    }

    /**
     * Figure implementation for a rotatable ellipse.
     */
    private static class RotatedEllipseFigure extends PlaneFigure {
        final Point p0_;
        final Point pa_;
        final Point pb_;
        final boolean hasP2_;
        final Shape ellipse_;
        final BinaryOperator<String> fracA_;
        final BinaryOperator<String> fracB_;
        static final double rb0_ = 0.5;

        /**
         * Constructor.
         *
         * @param  surf  plot surface
         * @param  p0    center
         * @param  p1    point at end of primary radius
         * @param  p2    point used to choose secondary radius,
         *               or null if not chosen yet
         */
        RotatedEllipseFigure( PlanarSurface surf,
                              Point p0, Point p1, Point p2 ) {
            super( surf, new Point[] { p0, p1, p2 } );
            p0_ = p0;
            pa_ = p1;
            hasP2_ = p2 != null;
            double ra = Math.hypot( p1.x - p0.x, p1.y - p0.y );

            /* Come up with a value for the secondary radius.
             * If we have a user-chosen p2, take it to be the distance
             * along a perpendicular dropped from there to the primary radius.
             * If not, just call it some default fraction of the primary. */
            double rb = p2 == null ? rb0_ * ra
                                   : perpDistance( p0, p1, p2 );

            /* Work out the vector corresponding to the end of the secondary
             * radius. */
            double theta = Math.atan2( pa_.y - p0_.y, pa_.x - p0_.x );
            pb_ =
                new Point( (int) Math.round( p0_.x + rb * Math.sin( theta ) ),
                           (int) Math.round( p0_.y - rb * Math.cos( theta ) ) );

            /* Prepare a shape to draw on the graphics surface. */
            Ellipse2D ellipse0 =
                new Ellipse2D.Double( p0_.x - ra, p0_.y - rb, 2 * ra, 2 * rb );
            ellipse_ = AffineTransform
                      .getRotateInstance( theta, p0_.x, p0_.y )
                      .createTransformedShape( ellipse0 );

            /* Prepare axis information we will need. */
            Axis[] axes = surf_.getAxes();
            Axis xAxis = axes[ 0 ];
            Axis yAxis = axes[ 1 ];
            boolean[] logFlags = surf_.getLogFlags();
            boolean xlog = logFlags[ 0 ];
            boolean ylog = logFlags[ 1 ];
            int[] xbounds = xAxis.getGraphicsLimits();
            int[] ybounds = yAxis.getGraphicsLimits();
            int xdim = xbounds[ 1 ] - xbounds[ 0 ];
            int ydim = ybounds[ 1 ] - ybounds[ 0 ];

            /* Now determine the algebraic expression in data coordinates
             * defining the same shape.  Note that the vectors we're using
             * for primary and secondary radius, though orthogonal in
             * graphics space, are not in general orthogonal in data space,
             * so it's not as simple as rotating the coordinate space
             * and using (x/a)^2+(y/b)^2<1.
             * We need to decompose the offset position vector into
             * multiples of the non-orthogonal basis vectors
             * (ra and rb in data space), which requires a matrix inversion.
             * It took me an embarrassingly long time to work this out.
             * Calculate the coefficients here. */
            double x0 = xAxis.graphicsToData( p0_.x );
            double y0 = yAxis.graphicsToData( p0_.y );
            double xa = xAxis.graphicsToData( pa_.x );
            double ya = yAxis.graphicsToData( pa_.y );
            double xb = xAxis.graphicsToData( pb_.x );
            double yb = yAxis.graphicsToData( pb_.y );
            double cx = xlog ? Math.log10( x0 ) : x0;
            double cy = ylog ? Math.log10( y0 ) : y0;
            double vax = xlog ? Math.log10( xa / x0 ) : xa - x0;
            double vay = ylog ? Math.log10( ya / y0 ) : ya - y0;
            double vbx = xlog ? Math.log10( xb / x0 ) : xb - x0;
            double vby = ylog ? Math.log10( yb / y0 ) : yb - y0;
            double det1 = 1. / ( vax * vby - vbx * vay );
            double kxa =  vby * det1;
            double kya = -vbx * det1;
            double kxb = -vay * det1;
            double kyb =  vax * det1;

            /* Finally prepare the expressions for the components of
             * the vector that must be within the unit circle for inclusion;
             * these are used to generate the syntax-specific expression
             * strings. */
            double x1 = xAxis.graphicsToData( p0_.x + 1 );
            double y1 = yAxis.graphicsToData( p0_.y + 1 );
            double xEps = Math.abs( xlog ? Math.log10( x1 / x0 ) : x1 - x0 );
            double yEps = Math.abs( ylog ? Math.log10( y1 / y0 ) : y1 - y0 );
            int nsf = (int) Math.ceil( Math.log10( Math.max( xdim, ydim ) ) );
            fracA_ = (xref, yref) -> new StringBuffer()
                .append( PlotUtil.formatNumberSf( kxa, nsf ) )
                .append( timesOffset( xref, cx, xEps ) )
                .append( addFormattedValueSf( kya, nsf ) )
                .append( timesOffset( yref, cy, yEps ) )
                .toString();
            fracB_ = (xref, yref) -> new StringBuffer()
                .append( PlotUtil.formatNumberSf( kxb, nsf ) )
                .append( timesOffset( xref, cx, xEps ) )
                .append( addFormattedValueSf( kyb, nsf ) )
                .append( timesOffset( yref, cy, yEps ) )
                .toString();
        }

        public Area getArea() {
            return new Area( ellipse_ );
        }

        public void paintPath( Graphics2D g ) {
            g.draw( ellipse_ );
            g.drawLine( p0_.x, p0_.y, pa_.x, pa_.y );
            g.drawLine( p0_.x, p0_.y, pb_.x, pb_.y );
        }

        public Point[] getVertices() {
            return hasP2_ ? new Point[] { p0_, pa_, pb_ }
                          : new Point[] { p0_, pa_ };
        }

        public String createPlaneExpression( String xvar, String yvar ) {
            String x = referenceName( surf_, xvar, 0 );
            String y = referenceName( surf_, yvar, 1 );
            return String.join( "",
                F_SQUARE, "(", fracA_.apply( x, y ), ")",
                " + ",
                F_SQUARE, "(", fracB_.apply( x, y ), ")",
                " < 1",
            "" );
        }

        public String createPlaneAdql( String xvar, String yvar ) {
            String x = referenceAdqlName( surf_, xvar, 0 );
            String y = referenceAdqlName( surf_, yvar, 1 );
            return String.join( "",
                "POWER(", fracA_.apply( x, y ), ", 2)",
                " + ",
                "POWER(", fracB_.apply( x, y ), ", 2)",
                " < 1",
            "" );
        }

        /**
         * Utility function to prepare an expression string giving a
         * multiplier by an offset coordinate.
         *
         * @param  xref  textual representation of coordinate
         * @param  x0    coordinate origin (negative offset)
         * @param  eps   precision for offset
         * @return  expression equivalent to "<code>*(xref-x0)</code>"
         */
        private static String timesOffset( String xref, double x0,
                                           double eps ) {
            return new StringBuffer()
               .append( "*(" )
               .append( xref )
               .append( addFormattedValue( -x0, eps ) )
               .append( ")" )
               .toString();
        }

        /**
         * Calculates the perpendicular distance from a point p2
         * to the line between p0 and p1.
         *
         * @param  p0  one end of line
         * @param  p1  other end of line
         * @param  p2  point
         * @return  length of perpendicular dropped from <code>p2</code>
         *          to <code>p1-p0</code>
         */
        private static double perpDistance( Point p0, Point p1, Point p2 ) {
            double v1x = p1.x - p0.x;
            double v1y = p1.y - p0.y;
            double r1 = Math.hypot( v1x, v1y );
            double u1x = v1x / r1;
            double u1y = v1y / r1;
            double v2x = p2.x - p0.x;
            double v2y = p2.y - p0.y;
            double p12 = u1x * v2x + u1y * v2y;
            double vpx = v2x - p12 * u1x;
            double vpy = v2y - p12 * u1y;
            return Math.hypot( vpx, vpy );
        }
    }

    /**
     * Figure implementation including all points to one side of a given point.
     */
    private static class LineSideFigure extends PlaneFigure {
        final Point point_;
        final boolean isYfunc_;
        final boolean isLess_;

        /**
         * Constructor.
         *
         * @param  surf  plotting surface
         * @param  point   point defining boundary
         * @param  isYfunc true for a line that corresponds to a function
         *                 defining y as a function of x (horizontal line)
         *                 false for the other way round
         * @param  isLess  true for graphics coordinates that are lower than
         *                 the given line; false for greater than the given line
         */
        LineSideFigure( PlanarSurface surf, Point point,
                        boolean isYfunc, boolean isLess ) {
            super( surf, new Point[] { point } );
            point_ = point;
            isYfunc_ = isYfunc;
            isLess_ = isLess;
        }

        public Area getArea() {
            Rectangle rect = new Rectangle( bounds_ );
            if ( isYfunc_ ) {
                rect.height = Math.max( 0, point_.y - bounds_.y );
            }
            else {
                rect.width = Math.max( 0, point_.x - bounds_.x );
            }
            Area rectArea = new Area( rect );
            return isLess_ == isYfunc_ ? invertArea( rectArea ) : rectArea;
        }

        public void paintPath( Graphics2D g ) {
            return;
        }

        public Point[] getVertices() {
            return new Point[] { point_ };
        }

        public String createPlaneExpression( String xvar, String yvar ) {
            String operator = surf_.getFlipFlags()[ isYfunc_ ? 1 : 0 ]
                            ? ( isLess_ ? ">" : "<=" )
                            : ( isLess_ ? "<" : ">=" );
            Axis axis = surf_.getAxes()[ isYfunc_ ? 1 : 0 ];
            double gval = isYfunc_ ? point_.y : point_.x;
            return new StringBuffer()
                  .append( isYfunc_ ? yvar : xvar )
                  .append( " " )
                  .append( operator )
                  .append( " " )
                  .append( formatGraphicsCoordinate( axis, gval ) )
                  .toString();
        }

        public String createPlaneAdql( String xvar, String yvar ) {

            /* No special functions; same as JEL expression. */
            return createPlaneExpression( xvar, yvar );
        }
    }

    /**
     * Defines a shape corresponding to the values on one side of a line
     * given by supplied points.
     */
    private static class PolySideFigure extends PlaneFigure {
        final boolean isYfunc_;
        final boolean isLess_;

        /**
         * Constructor.
         *
         * @param  surf   plotting surface
         * @param  points  points defining the border
         * @param  isYfunc true for a line that corresponds to a function
         *                 defining y as a function of x;
         *                 false for the other way round
         * @param  isLess  true for graphics coordinates that are lower than
         *                 the given line; false for greater than the given line
        */
        PolySideFigure( PlanarSurface surf, Point[] points,
                        boolean isYfunc, boolean isLess ) {
            super( surf, points );
            isYfunc_ = isYfunc;
            isLess_ = isLess;
        }

        public Area getArea() {

            /* Build up the shape from a trapezium going to the edge of
             * the bounding rectangle at either end, and then a trapezium
             * for each section for the non-end points. */
            Area poly = new Area();
            poly.add( createEdgeTrapezium( points_[ 0 ], points_[ 1 ] ) );
            int np = points_.length;
            for ( int ip = 0; ip < np - 2; ip++ ) {
                poly.add( createBoundedTrapezium( points_[ ip + 1 ],
                                                  points_[ ip + 2 ] ) );
            }
            poly.add( createEdgeTrapezium( points_[ np - 1 ],
                                           points_[ np - 2 ] ) );
            return isLess_ == isYfunc_ ? invertArea( poly ) : poly;
        }

        public void paintPath( Graphics2D g ) {
            paintPolyPath( g );
        }

        public Point[] getVertices() {
            return points_.clone();
        }

        public String createPlaneExpression( String xvar, String yvar ) {
            Axis[] axes = surf_.getAxes();
            boolean[] logFlags = surf_.getLogFlags();
            String operator = surf_.getFlipFlags()[ isYfunc_ ? 1 : 0 ]
                            ? ( isLess_ ? ">" : "<=" )
                            : ( isLess_ ? "<" : ">=" );

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
            if ( points_.length == 2 ) {
                Point p1 = points_[ 0 ];
                Point p2 = points_[ 1 ];
                boolean xlog = logFlags[ 0 ];
                boolean ylog = logFlags[ 1 ];
                Axis xaxis = axes[ 0 ];
                Axis yaxis = axes[ 1 ];
                String xref = referenceName( surf_, xvar, 0 );
                String yref = referenceName( surf_, yvar, 1 );
                double dx1 = xaxis.graphicsToData( p1.x );
                double dy1 = yaxis.graphicsToData( p1.y );
                double dx2 = xaxis.graphicsToData( p2.x );
                double dy2 = yaxis.graphicsToData( p2.y );
                double rx1 = xlog ? Math.log10( dx1 ) : dx1;
                double ry1 = ylog ? Math.log10( dy1 ) : dy1;
                double rx2 = xlog ? Math.log10( dx2 ) : dx2;
                double ry2 = ylog ? Math.log10( dy2 ) : dy2;
                double m = isYfunc_ ? ( ry2 - ry1 ) / ( rx2 - rx1 )
                                    : ( rx2 - rx1 ) / ( ry2 - ry1 );
                double c = isYfunc_ ? ry1 - m * rx1
                                    : rx1 - m * ry1;
                double xEpsilon = getPixelEpsilon( surf_, 0, p1.x );
                double yEpsilon = getPixelEpsilon( surf_, 1, p1.y );

                /* Calculate the tolerance on the gradient by identifying
                 * the value that will make a difference of at most PIXTOL
                 * pixels on either edge of the visible plot bounds. */
                double mEpsilon = 0;
                for ( double dlim : surf_.getDataLimits()[ isYfunc_ ? 0 : 1 ] ){
                    double rlim = ( isYfunc_ ? xlog : ylog )
                                ? Math.log10( dlim )
                                : dlim;
                    double dm = Math.abs( ( isYfunc_ ? yEpsilon : xEpsilon )
                                        / ( rlim - ( isYfunc_ ? rx1 : ry1 ) ) );
                    mEpsilon = mEpsilon > 0 ? Math.min( mEpsilon, dm ) : dm;
                }
                return new StringBuffer()
                      .append( isYfunc_ ? yref : xref )
                      .append( " " )
                      .append( operator )
                      .append( " " )
                      .append( PlotUtil.formatNumber( m, mEpsilon ) )
                      .append( " * " )
                      .append( "(" )
                      .append( isYfunc_ ? xref : yref )
                      .append( isYfunc_ ? addFormattedValue( -rx1, xEpsilon )
                                        : addFormattedValue( -ry1, yEpsilon ) )
                      .append( ")" )
                      .append( isYfunc_ ? addFormattedValue( +ry1, yEpsilon )
                                        : addFormattedValue( +rx1, xEpsilon ) )
                      .toString();
            }

            /* Use the special function from
             * uk.ac.starlink.ttools.func.Shapes. */
            else {
                String xref = referenceName( surf_, xvar, 0 );
                String yref = referenceName( surf_, yvar, 1 );
                return new StringBuffer()
                      .append( isYfunc_ ? yref : xref )
                      .append( " " )
                      .append( operator )
                      .append( " " )
                      .append( F_POLYLINE )
                      .append( "(" )
                      .append( isYfunc_ ? xref : yref )
                      .append( referencePoints( surf_, points_, isYfunc_ ) )
                      .append( ")" )
                      .toString();
            }
        }

        public String createPlaneAdql( String xvar, String yvar ) {
            if ( points_.length <= 2 ) {
                String adql = createPlaneExpression( xvar, yvar );
                assert adql.indexOf( F_POLYLINE ) < 0;
                return adql;
            }
            else {
                return null;
            }
        }
        
        /**
         * Creates a trapezium corresponding to a line infinitely extended
         * in one direction.
         *
         * @param  p0  the point in the direction of extension to the edge
         * @param  p1  the point at a vertex
         * @return  shape
         */
        private Area createEdgeTrapezium( Point p0, Point p1 ) {
            final int x2;
            final int y2;
            if ( isYfunc_ ) {
                x2 = p0.x < p1.x ? bounds_.x : bounds_.x + bounds_.width;
                y2 = (int) ( ( p1.y - p0.y ) / (double) ( p1.x - p0.x )
                             * ( x2 - p0.x ) )
                   + p0.y;
            }
            else {
                y2 = p0.y < p1.y ? bounds_.y : bounds_.y + bounds_.height;
                x2 = (int) ( ( p1.x - p0.x ) / (double) ( p1.y - p0.y )
                             * ( y2 - p0.y ) )
                   + p0.x;
            }
            return createBoundedTrapezium( p1, new Point( x2, y2 ) );
        }

        /**
         * Creates a trapezium with sides dropping to one edge of the bounding
         * rectangle and a sloping section between two given points.
         *
         * @param  p1  one vertex
         * @param  p2  other vertex
         * @return  shape
         */
        private Area createBoundedTrapezium( Point p1, Point p2 ) {
            int[] xs = isYfunc_ ? new int[] { p1.x, p1.x, p2.x, p2.x }
                                : new int[] { bounds_.x, p1.x, p2.x, bounds_.x};
            int[] ys = isYfunc_ ? new int[] { bounds_.y, p1.y, p2.y, bounds_.y }
                                : new int[] { p1.y, p1.y, p2.y, p2.y };
            return new Area( new Polygon( xs, ys, 4 ) );
        }
    }
}
