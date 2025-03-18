package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.lang.reflect.Method;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurface;
import uk.ac.starlink.ttools.plot2.geom.SideFlags;
import uk.ac.starlink.util.LogUtils;

public class FigureModeTest extends TestCase {

    public FigureModeTest() {
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
    }

    public void testModes() throws Throwable {

        // These tests are quite stringent, since there are several
        // distinct code paths through the FigureMode's construction
        // of JEL expressions and inclusion shapes, and no compile-time
        // or run-time checking (other than user eyeballs) that it's
        // got them right.
        for ( int i = 0; i < 16; i++ ) {
            boolean xflip = ( ( i >> 0 ) & 1 ) != 0;
            boolean yflip = ( ( i >> 1 ) & 1 ) != 0;
            boolean xlog = ( ( i >> 2 ) & 1 ) != 0;
            boolean ylog = ( ( i >> 3 ) & 1 ) != 0;
            Scale xscale = xlog ? Scale.LOG : Scale.LINEAR;
            Scale yscale = ylog ? Scale.LOG : Scale.LINEAR;
            PlanarSurface surf =
                new PlaneSurface( 0, 100, 0, 100,        // graphics coords
                                  0.1, 10000, 0.2, 20,   // data coords
                                  xscale, yscale, xflip, yflip,
                                  new Tick[ 0 ], new Tick[ 0 ],
                                  Orientation.X, Orientation.Y, "X", "Y",
                                  new Tick[ 0 ], new Tick[ 0 ],
                                  Orientation.ANTI_X, Orientation.ANTI_Y,
                                  (String) null, (String) null,
                                  (Captioner) null, SideFlags.ALL,
                                  Color.GRAY, Color.BLACK, false );
            checkPlaneMode( PlaneFigureMode.POLYGON,
                            PlaneFigureMode.OUTSIDE_POLYGON,
                            surf,
                       new Point[] {
                           new Point( 20, 20 ),
                           new Point( 80, 20 ),
                           new Point( 80, 80 ),
                           new Point( 20, 80 ),
                       }, new Point[] {
                           new Point( 50, 50 ),
                           new Point( 30, 60 ),
                       }, new Point[] {
                           new Point( 10, 10 ),
                           new Point( 10, 90 ),
                       } );
            checkPlaneMode( PlaneFigureMode.BELOW, PlaneFigureMode.ABOVE,
                            surf,
                       new Point[] {
                           new Point( 5, 10 ),
                       },
                       new Point[] {
                           new Point( 28, 11 ),
                           new Point( 66, 99 ),
                       },
                       new Point[] {
                           new Point( 28, 9 ),
                           new Point( 66, 4 ),
                       } );
            checkPlaneMode( PlaneFigureMode.BELOW, PlaneFigureMode.ABOVE,
                            surf,
                       new Point[] {
                           new Point( 0, 0 ),
                           new Point( 50, 50 ),
                       }, new Point[] {
                           new Point( 10, 20 ),
                       }, new Point[] {
                           new Point( 20, 10 ),
                       } );
            checkPlaneMode( PlaneFigureMode.LEFT, PlaneFigureMode.RIGHT,
                            surf,
                       new Point[] {
                           new Point( 10, 10 ),
                           new Point( 20, 90 ),
                       },
                       new Point[] {
                           new Point( 9, 10 ),
                           new Point( 19, 90 ),
                       },
                       new Point[] {
                           new Point( 11, 10 ),
                           new Point( 21, 90 ),
                       } );
            checkPlaneMode( PlaneFigureMode.BELOW, PlaneFigureMode.ABOVE,
                            surf,
                       new Point[] {
                           new Point( 10, 10 ),
                           new Point( 20, 20 ),
                           new Point( 60, 10 ),
                           new Point( 80, 30 ),
                       },
                       new Point[] {
                           new Point( 2, 3 ),
                           new Point( 10, 11 ),
                           new Point( 15, 16 ),
                           new Point( 20, 21 ),
                           new Point( 21, 20 ),
                           new Point( 60, 11 ),
                           new Point( 61, 12 ),
                           new Point( 80, 31 ),
                           new Point( 99, 50 ),
                       },
                       new Point[] {
                           new Point( 2, 1 ),
                           new Point( 10, 9 ),
                           new Point( 15, 14 ),
                           new Point( 20, 19 ),
                           new Point( 21, 19 ),
                           new Point( 60, 9 ),
                           new Point( 61, 10 ),
                           new Point( 80, 29 ),
                           new Point( 99, 48 ),
                       } );
        }
    }

    private void checkPlaneMode( FigureMode mode, FigureMode antiMode,
                                 PlanarSurface surf, Point[] vertices,
                                 Point[] insides, Point[] outsides )
            throws Throwable {
        PlaneFigureMode.PlaneFigure fig =
            (PlaneFigureMode.PlaneFigure) mode.createFigure( surf, vertices );
        PlaneFigureMode.PlaneFigure antiFig =
            (PlaneFigureMode.PlaneFigure) antiMode.createFigure( surf,vertices);
        Area area = fig.getArea();
        Area antiArea = antiFig.getArea();
        for ( Point p : insides ) {
            assertTrue( area.contains( p ) );
            assertFalse( antiArea.contains( p ) );
            assertTrue( evaluatePlaneExpr( fig, p ) );
            assertFalse( evaluatePlaneExpr( antiFig, p ) );
        }
        for ( Point p : outsides ) {
            assertFalse( area.contains( p ) );
            assertTrue( antiArea.contains( p ) );
            assertFalse( evaluatePlaneExpr( fig, p ) );
            assertTrue( evaluatePlaneExpr( antiFig, p ) );
        }
    }

    private boolean evaluatePlaneExpr( PlaneFigureMode.PlaneFigure fig,
                                       Point point )
            throws Throwable {
        PlanarSurface surf = fig.surf_;
        double dx = surf.getAxes()[ 0 ].graphicsToData( point.x );
        double dy = surf.getAxes()[ 1 ].graphicsToData( point.y );
        StarTable table = new EmptyStarTable();
        ValueInfo xInfo = new DefaultValueInfo( "x", Double.class, null );
        ValueInfo yInfo = new DefaultValueInfo( "y", Double.class, null );
        table.setParameter( new DescribedValue( xInfo, Double.valueOf( dx ) ) );
        table.setParameter( new DescribedValue( yInfo, Double.valueOf( dy ) ) );
        String expr = fig.createPlaneExpression( "param$x", "param$y" );
        JELRowReader rdr = JELUtils.createDatalessRowReader( table );
        Library lib = JELUtils.getLibrary( rdr );
        CompiledExpression compEx = JELUtils.compile( lib, table, expr );
        return ((Boolean) rdr.evaluate( compEx )).booleanValue();
    }

    public void testFunctions() {
        for ( String fname : PlaneFigureMode.JEL_FUNCTIONS ) {
            assertNotNull( getLibraryMethod( fname ) );
        }
        for ( String fname : SkyFigureMode.JEL_FUNCTIONS ) {
            assertNotNull( getLibraryMethod( fname ) );
        }
    }

    private static Method getLibraryMethod( String fname ) {
        for ( Class clazz : JELUtils.getStaticClasses() ) {
            for ( Method method : clazz.getDeclaredMethods() ) {
                if ( method.getName().equals( fname ) ) {
                    return method;
                }
            }
        }
        return null;
    }
}
