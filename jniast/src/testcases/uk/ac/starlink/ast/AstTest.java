package uk.ac.starlink.ast;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import junit.framework.Test;
import junit.framework.TestSuite;
import uk.ac.starlink.TestCase;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;

public class AstTest extends TestCase {

    public static String AST_FILE = "uk/ac/starlink/ast/alcyone.ast";
    private static FrameSet basicWcs;

    private Frame frm;
    private Frame grid;
    private SkyFrame sky;
    private FrameSet wcs;
    private Mapping map;

    public AstTest( String name ) {
        super( name );
    }

    protected void setUp() throws IOException {
        if ( basicWcs == null ) {
            InputStream strm = getClass()
                              .getClassLoader()
                              .getResourceAsStream( AST_FILE );
            assertNotNull( "Test file " + AST_FILE + " not present", strm );
            Channel chan = new Channel( strm, null );
            basicWcs = (FrameSet) chan.read();
            strm.close();
        }
        wcs = (FrameSet) basicWcs.copy();
        frm = new Frame( 3 );
        grid = wcs.getFrame( FrameSet.AST__BASE );
        map = wcs.getMapping( FrameSet.AST__BASE, FrameSet.AST__CURRENT );
        assertEquals( 2, grid.getNaxes() );
        assertEquals( "GRID", grid.getDomain() );
        sky = (SkyFrame) wcs.getFrame( FrameSet.AST__CURRENT );

        // Frame grid = new Frame( 2 );
        // Frame axes = new Frame( 2 );
        // Frame pixel = new Frame( 2 );
        // Frame sky = new SkyFrame();
        // Frame frame = grid;
        // grid.setDomain( "grid" );
        // axes.setDomain( "axes" );
        // pixel.setDomain( "pixel" );
        // FrameSet wcs = new FrameSet( grid );
        // Mapping gpmap = new WinMap( new double[] { 0.5, 0.5 },
        //                             new double[] { 1.5, 1.5 },
        //                             new double[] { 0.0, 0.0 },
        //                             new double[] { 1.0, 1.0 } );
        // Mapping umap = new UnitMap( 2 );
        // wcs.addFrame( FrameSet.AST__BASE, gpmap, axes );
        // wcs.addFrame( FrameSet.AST__CURRENT, umap, pixel );
 
    }

    public void testAstObject() {
        frm.setDomain( "custom" );
        assertTrue( frm.getDomain().equalsIgnoreCase( "custom" ) );
        assertTrue( frm.test( "domain" ) );
        assertTrue( frm.test( "DOMAIN" ) );
        frm.clear( "domain" );
        assertTrue( ! frm.test( "domain" ) );

        assertEquals( wcs.getC( "Class" ), "FrameSet" );
        try {
            new Frame( 1 ).getC( "Sir Not-appearing-in-this-class" );
            fail();
        }
        catch ( AstException e ) {}

        Frame ff = grid;
        ff.setIdent( "1-2-3-4" );
        ff.setID( "9-8-7-6" );

        Frame f = (Frame) grid.clone();
        Frame fff = (Frame) ff.copy();

        assertEquals( f.getIdent(), ff.getIdent() );
        assertEquals( f.getIdent(), fff.getIdent() );
        assertEquals( f.getID(), ff.getID() );
        assertTrue( ! f.getID().equals( fff.getID() ) );

        ff = null;
        System.gc();
        assertEquals( 2, f.getNaxes() );

        f.set( "Domain=COPY" );
        wcs.addFrame( FrameSet.AST__BASE, map, f );
        assertEquals( "COPY", wcs.getFrame( 5 ).getDomain() );

        assertTrue( wcs.getFrame( FrameSet.AST__CURRENT ).sameObject( f ) );

        f.clear( "ID" );
        f.clear( "Ident" );
        ff = (Frame) f.clone();
        fff = (Frame) f.copy();
        assertTrue( f.sameObject( ff ) );
        assertTrue( ! f.sameObject( fff ) );
        assertTrue( f.equals( ff ) );
        assertTrue( f.equals( fff ) );
        f.setTitle( "I'm not fff nosireebob" );
        assertTrue( f.equals( ff ) );
        assertTrue( ! f.equals( fff ) );

    }

    public void testChannel() throws IOException {
        Channel chan = new TestChannel();
        chan.write( new FrameSet( frm ) );
        FrameSet f2 = (FrameSet) chan.read();
        assertEquals( f2.getNframe(), 1 );
        assertEquals( frm.getNaxes(), f2.getFrame( 1 ).getNaxes() );

        chan.setI( "Full", -1 );
        assertEquals( -1, chan.getI( "Full" ) );
        chan.setFull( 10 );
        assertEquals( 1, chan.getI( "Full" ) );
    }

    public void testFitsChan() throws IOException {
        FitsChan fchan = new FitsChan();
        fchan.write( wcs );
        int nc = fchan.getNcard();
        assertTrue( "Enough cards", nc > 10 );
        fchan.setCard( 10 );
        while ( fchan.getNcard() > 10 ) {
            fchan.delFits();
        }
    }

    public void testFrame() {

        // format
        String s1v = sky.format( 1, 0.5 );
        String s2v = sky.format( 2, 1.5 );
        assertTrue( s1v.indexOf( ':' ) > 0 );
        assertTrue( s2v.indexOf( ':' ) > 0 );

        // unformat
        assertEquals( 0.5, sky.unformat( 1, s1v ), 0.0001 );
        assertEquals( 1.5, sky.unformat( 2, s2v ), 0.0001 );

        // pickAxes
        Frame yks = (SkyFrame) sky.pickAxes( 2, new int[] { 2, 1 }, null );
        assertEquals( sky.format( 1, 2.5 ), yks.format( 2, 2.5 ) );
        assertEquals( sky.format( 2, 3.5 ), yks.format( 1, 3.5 ) );
        Mapping[] skmap = new Mapping[ 1 ];
        Frame sk = sky.pickAxes( 1, new int[] { 1 }, skmap );
        assertEquals( 1, sk.getNaxes() );
        assertTrue( skmap[ 0 ] instanceof PermMap );
        assertEquals( 2, skmap[ 0 ].getNin() );
        assertEquals( 1, skmap[ 0 ].getNout() );

        // permAxes
        yks.permAxes( new int[] { 2, 1 } );
        assertEquals( yks.format( 1, 0.5 ), sky.format( 1, 0.5 ) );
        assertEquals( yks.format( 2, 1.5 ), sky.format( 2, 1.5 ) );

        // norm
        double[] pnorm = new double[] { 0.25, 0.75 };
        double[] pn1 = (double[]) pnorm.clone();
        sky.norm( pnorm );
        assertArrayEquals( pnorm, pn1 );
        pnorm[ 0 ] += 2 * Math.PI;
        pnorm[ 1 ] += 2 * Math.PI;
        sky.norm( pnorm );
        assertArrayEquals( pnorm, pn1 );
    }

    public void testFrameSet() {
        Frame pixel = wcs.getFrame( 2 );
        wcs.removeFrame( 3 );
        SkyFrame sfrm = (SkyFrame) wcs.getFrame( 3 );

        assertEquals( "GRID", grid.getDomain() );
        assertEquals( "PIXEL", pixel.getDomain() );

        SkyFrame sfrm2 = (SkyFrame) wcs.findFrame( new SkyFrame(), "" )
                                       .getFrame( FrameSet.AST__CURRENT );
        assertTrue( sky.equals( sfrm2 ) );

        assertEquals( "SKY", sky.getDomain() );
        assertEquals( "Right ascension", sky.getLabel( 1 ) );
        assertEquals( "Declination", sky.getLabel( 2 ) );

        sky.setLabel( 1, "RA" );
        sky.setLabel( 2, "Dec" );
        assertEquals( "RA", sky.getLabel( 1 ) ); 
        assertEquals( "Dec", sky.getLabel( 2 ) );
     
    }

    public void testGeometry() {
        double[] point0 = new double[] { 100, 200 };
        double[] point1 = new double[] { 103, 200 };
        double[] point2 = new double[] { 100, 204 };

        assertEquals( -Math.PI / 2, grid.angle( point1, point0, point2 ) );
        assertEquals( 5.0, grid.distance( point1, point2 ) );
        assertArrayEquals( point1, grid.offset( point0, point1, 3.0 ) );

        // The presence of the offset2 invocation in the following test
        // was causing a (load-time?) error on the OSF1_alpha JDK1.3.
        // If it was present, then at the start of this method, before
        // any of it has been executed, the following message is
        // emitted:
        //
        //    Assertion failed: loc2mode(dst) == loc2mode(src),
        //       file /sys/alpha/compiler.c, line 5277
        //
        // and the JVM bails out with error status.  This has to be a
        // JVM bug?  It is not particularly easy to reproduce if you
        // remove lots of the rest of the code in this method.
        // I don't have any better ideas than simply commenting out the test.
        // Haven't tried it at 1.4 - leave it in and see if it causes trouble.
        double[] po2i = new double[] { AstObject.AST__BAD, 0.0 };
        double[] po2o = new double[ 2 ];
        grid.offset2( po2i, 0.25, 10, po2o );
        assertEquals( AstObject.AST__BAD, po2o[ 0 ] );
        assertEquals( AstObject.AST__BAD, po2o[ 1 ] );

        assertEquals( 0.0, grid.axAngle( new double[] { 0, 0 },
                                         new double[] { 0, 20 }, 2 ) );

        double[] point4 = new double[ 2 ];
        double[] rslvd = grid.resolve( new double[] { 0, 0 },
                                       new double[] { 0, -1 },
                                       new double[] { 2, 2 }, point4 );
        assertEquals( point4[ 0 ], 0.0 );
        assertEquals( point4[ 1 ], 2.0 );
        assertEquals( rslvd[ 0 ], -2.0 );
        assertEquals( rslvd[ 1 ], 2.0 );

        assertEquals( grid.axOffset( 2, 99., 2. ), 101. );

        assertEquals( grid.axDistance( 1, 4., 9. ), 5. );
    }

    public void testMapping() throws IOException {
        ZoomMap zoomer = new ZoomMap( 2, 3.0 );
        assertTrue( zoomer.getTranForward() );
        assertTrue( zoomer.getTranInverse() );
        assertTrue( ! zoomer.getInvert() );
        assertEquals( 2, zoomer.getNin() );
        assertEquals( 2, zoomer.getNout() );

        double[] xin = new double[] { 10, 20, 40 };
        double[] yin = new double[] { 2, 4, 8 };
        zoomer.setZoom( 2 );
        double[][] result2 = zoomer.tran2( 3, xin, yin, true );
        assertEquals( 2, result2.length );
        assertArrayEquals( new double[] { 20, 40, 80 }, result2[ 0 ] );
        assertArrayEquals( new double[] { 4, 8, 16 }, result2[ 1 ] );

        double[] resultN = 
            zoomer.tranN( 3, 2, new double[] { 10, 20, 40, 2, 4, 8 }, true, 2 );
        assertArrayEquals( new double[] { 20, 40, 80, 4, 8, 16 }, resultN );

        PermMap permer = new PermMap( 2, new int[] { 2, 1 },
                                      3, new int[] { 2, 1, -1 },
                                      new double[] { 23 } );

        double[][] resultP = 
            permer.tranP( 3, 2, new double[][] { xin, yin }, true, 3 );
        assertEquals( 3, resultP.length );
        assertArrayEquals( yin, resultP[ 0 ] );
        assertArrayEquals( xin, resultP[ 1 ] );
        assertArrayEquals( new double[] { 23, 23, 23 }, resultP[ 2 ] );
        resultP = 
            permer.tranP( 3, 3, 
                          new double[][] { xin, yin, new double[] { 0, 0, 0 } },
                          false, 2 );
        assertEquals( 2, resultP.length );
        assertArrayEquals( yin, resultP[ 0 ] );
        assertArrayEquals( xin, resultP[ 1 ] );

        MathMap skidoo = new MathMap( 1, 1, new String[] { "a = x + 23" },
                                      new String[] { "x = a - 23" } );
        skidoo.setSimpFI( true );
        skidoo.setSimpIF( true );
        MathMap skidoo2 = (MathMap) skidoo.copy();
        skidoo2.invert();
        double[] box23 = 
            skidoo.mapBox( new double[] { 0 }, new double[] { 100 }, 
                           true, 1, null, null );
        assertArrayEquals( new double[] { 23, 123 }, box23 );

        double[] result1 = skidoo.tran1( 2, new double[] { 100, 200 }, true );
        assertEquals( 2, result1.length );
        assertArrayEquals( result1, new double[] { 123, 223 } );

        double[]  small = new double[] { 999 };
        CmpMap skidoodiks = new CmpMap( skidoo, skidoo2, true );
        assertArrayEquals( small, skidoodiks.tran1( 1, small, true ) );
        assertArrayEquals( small, skidoodiks.tran1( 1, small, false ) );

        // simplify
        assertTrue( skidoodiks.getClass().equals( CmpMap.class ) );
        assertTrue( skidoodiks.simplify().getClass().equals( UnitMap.class ) );

        // decompose
        boolean[] series = new boolean[ 1 ];
        boolean[] inverts = new boolean[ 2 ];
        Mapping[] maps = skidoodiks.decompose( series, inverts );
        assertTrue( inverts[ 0 ] != inverts[ 1 ] );
        assertTrue( series[ 0 ] );                // series
        assertTrue( maps[ 0 ] instanceof MathMap );
        assertTrue( maps[ 1 ] instanceof MathMap );

        CmpFrame cfrm = new CmpFrame( new Frame( 2 ), new Frame( 3 ) );
        Frame[] frms = cfrm.decompose();
        assertEquals( 3, frms[ 1 ].getNaxes() );
        assertEquals( "Frame", frms[ 0 ].getC( "Class" ) );
        assertEquals( 1, frms[ 0 ].decompose( null, null ).length );

        // MatrixMap
        assertEquals( UnitMap.class, 
                      new MatrixMap( 10, 10 ).simplify().getClass() );
        assertEquals( UnitMap.class,
                      new MatrixMap( 4, 4, new double[] { 1, 1, 1, 1 } )
                     .simplify().getClass() );
        assertEquals( UnitMap.class,
                      new MatrixMap( 2, 2, 
                                     new double[][] { new double[] { 1, 0 },
                                                      new double[] { 0, 1 }, } )
                     .simplify().getClass() );

        // IntraMap
        Mapping imap = new IntraMap( new TestTransformer2( 2.0, 4.0 ) );
        double[] xina = new double[] { 1, 2, 3 };
        double[] yina = new double[] { 100, 200, 300 };
        double[][] resulta = imap.tran2( 3, xina, yina, true );
        assertArrayEquals( new double[] { 2, 4, 6 }, resulta[ 0 ] );
        assertArrayEquals( new double[] { 400, 800, 1200 }, resulta[ 1 ] );

        Channel mc = new TestChannel();
        mc.write( imap );
        Mapping jmap = (Mapping) mc.read();
        double[][] resultb = jmap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resultb[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resultb[ 1 ] );

        FitsChan fc = new FitsChan();
        fc.write( jmap );
        fc.setCard( 1 );
        Mapping kmap = (Mapping) fc.read();
        double[][] resultc = kmap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resultc[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resultc[ 1 ] );

        double[][] resultd = imap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resultd[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resultd[ 1 ] );
    }

    public void testPlot() {
        double[] basebox = new double[] { 0, 0, 50000, 50000 };
        JFrame toplev = new JFrame();
        TestPlotHolder pan = new TestPlotHolder();
        pan.setPreferredSize( new Dimension( 400, 400 ) );
        toplev.getContentPane().add( pan );
        toplev.pack();

        Plot plot = new Plot( wcs, pan.getBounds(), basebox, 40, 20, 40, 20 );
        pan.plot = plot;

        double gap1 = plot.getGap( 1 );
        gap1 *= 0.5;
        plot.setGap( 1, gap1 );
        assertEquals( gap1, plot.getGap( 1 ) );

        plot.setGrid( true );
        plot.setColour( "grid", Color.green.getRGB() );
        assertEquals( 1.0, plot.getSize( "NumLab" ) );
        plot.setSize( "NumLab", 0.8 );

        // grid
        plot.grid();

        // border
        plot.setStyle( "border", DefaultGrf.DOTDASH );
        plot.setColour( "border", Color.red.getRGB() );
        plot.border();

        // clip
        plot.clip( Plot.AST__NOFRAME, null, null );
        plot.clip( 2, new double[] { 1e4, 1e4 }, new double[] { 4e4, 4e4 } );
        plot.setCurrent( 2 );

        // curve
        plot.setWidth( "curves", 10 );
        plot.setColour( "curves", 0xff0000ff );
        plot.curve( new double[] { 0e4,0e4 }, new double[] { 5e4, 5e4 } );
        plot.clip( 2, new double[] { AstObject.AST__BAD, AstObject.AST__BAD },
                   new double[] { AstObject.AST__BAD, AstObject.AST__BAD } );

        // gridLine
        plot.setColour( "grid", Color.red.brighter().getRGB() );
        plot.setStyle( "grid", DefaultGrf.DASH );
        plot.setWidth( "grid", 2 );
        plot.gridLine( 1, new double[] { 0e4, 2e4 }, 5e4 );

        // genCurve
        Mapping pm = new MathMap( 1, 2,
            new String[] { "x = 5e4 * theta",
                           "y = 2e4 + 2e4 * sin(" + Math.PI + " * 2 * theta)" },
            new String[] { "theta" }
        );
        plot.setWidth( "curves", 6 );
        plot.setColour( "curves", Color.orange.darker().getRGB() & 0x60ffffff );
        plot.genCurve( pm );

        // polyCurve
        double[][] points = new double[][] {
            new double[] { 1e4, 1e4, 4e4, 4e4, 1e4 },
            new double[] { 1e4, 4e4, 4e4, 1e4, 1e4 },
        };
        plot.set( "color(curves)=" + Color.black.getRGB() + "," +
                  "width(curves)=2" );
        plot.polyCurve( 5, 2, points );

        // mark
        plot.setColour( "markers", Color.yellow.getRGB() );
        plot.setSize( "markers", plot.getSize( "markers" ) * 20 );
        plot.mark( 5, 2, points, DefaultGrfMarker.FILLEDDIAMOND );

        plot.setSize( "text", 12 );
        plot.text( "JNI", new double[] { 1e4, 1e4 },
                   new float[] { -1, 1 }, "BL" );
        plot.text( "AST", new double[] { 4e4, 4e4 },
                   new float[] { 1, -1 }, "BL" );

        toplev.setVisible( true );

        try {
            Thread.currentThread().sleep( 4000 );
        }
        catch ( InterruptedException e ) {
            // no action
        }
    }


    public static Test suite() {
        return new TestSuite( AstTest.class );
    }


}

class TestChannel extends Channel {
    List buf = new ArrayList();
    protected void sink( String line ) {
        buf.add( line );
    }
    protected String source() {
        return buf.isEmpty() ? null : (String) buf.remove( 0 );
    }
}

class TestPlotHolder extends JPanel {
    Plot plot;
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        plot.paint( g );
    }
}

class TestTransformer2 extends Transformer2 implements Serializable {
    private double xfactor;
    private double yfactor;
    public TestTransformer2( double x, double y ) {
        xfactor = x;
        yfactor = y;
    }
    public double[][] tran2( int npoint, double[] xin, double[] yin,
                             boolean forward ) {
        double[] xout = new double[ npoint ];
        double[] yout = new double[ npoint ];
        double xf = forward ? xfactor : ( 1.0 / xfactor );
        double yf = forward ? yfactor : ( 1.0 / yfactor );
        for ( int i = 0; i < npoint; i++ ) {
            xout[ i ] = xf * xin[ i ];
            yout[ i ] = yf * yin[ i ];
        }
        return new double[][] { xout, yout };
    }
    public boolean simpFI() {
        return true;
    }
    public boolean simpIF() {  
        return true;
    }                          
    public boolean hasInverse() {
        return ( xfactor != 0 ) && ( yfactor != 0 );
    }
    public String getAuthor() {
        return "Mark Taylor (Starlink)";
    }
    public String getContact() {
        return "Desolation Row";
    }
    public String getPurpose() {
        return "Two-dimensional stretch";
    }
}

