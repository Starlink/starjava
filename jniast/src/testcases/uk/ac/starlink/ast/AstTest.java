package uk.ac.starlink.ast;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import junit.framework.Test;
import junit.framework.TestSuite;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfMarker;
import uk.ac.starlink.ast.grf.DefaultGrfFontManager;
import uk.ac.starlink.ast.grf.GrfEscape;

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
        assertTrue( AstPackage.isAvailable() );
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
        catch ( AstException e ) {
            assertEquals( AstException.AST__BADAT, e.getStatus() );
        }

        Frame ff = grid;
        ff.setIdent( "1-2-3-4" );
        ff.setID( "9-8-7-6" );
        assertTrue( ff.getObjSize() > 1 );

        Frame f = grid;
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

        // AST now stores copies of frames in the FrameSet, so we can no
        // longer test that sameObject(f) returns true for the current frame.
        // In order to do the test with equals(f) we need also to set the
        // same ID attribute.
        Frame wcs_current = wcs.getFrame( FrameSet.AST__CURRENT );
        wcs_current.setID( "9-8-7-6" );
        assertTrue( wcs_current.equals( f ) );

        f.clear( "ID" );
        f.clear( "Ident" );
        ff = f;
        fff = (Frame) f.copy();
        assertTrue( f.sameObject( ff ) );
        assertTrue( ! f.sameObject( fff ) );
        assertTrue( f.equals( ff ) );
        assertTrue( f.equals( fff ) );
        f.setTitle( "I'm not fff nosireebob" );
        assertTrue( f.equals( ff ) );
        assertTrue( ! f.equals( fff ) );

    }

    public void testTuning() {
        assertEquals( 0, AstObject.tune( "ObjectCaching", 2 ) );
        assertEquals( 2, AstObject.tune( "ObjectCaching", 0 ) );
        assertEquals( 0, AstObject.tune( "ObjectCaching",
                                         AstObject.AST__TUNULL ) );
        assertEquals( 0, AstObject.tune( "ObjectCaching", 0 ) );
    }

    private void exerciseChannel( Channel chan ) throws IOException {
        assertEquals( 1, chan.write( new FrameSet( frm ) ) );
        FrameSet f2 = (FrameSet) chan.read();
        assertEquals( f2.getNframe(), 1 );
        assertEquals( frm.getNaxes(), f2.getFrame( 1 ).getNaxes() );

        chan.setI( "Full", -1 );
        assertEquals( -1, chan.getI( "Full" ) );
        chan.setFull( 10 );
        assertEquals( 1, chan.getI( "Full" ) );
    }

    public void testChannel() throws IOException {
        exerciseChannel( new TestChannel() );
        Channel chan = new TestChannel();
        boolean strict = chan.getStrict();
        chan.setStrict( ! strict );
        assertTrue( strict ^ chan.getStrict() );
        chan.setStrict( strict );
        KeyMap warnings = chan.warnings();
        assertNull( warnings );
    }

    public void testXmlChan() throws IOException {
        XmlChan xchan = new MemoryXmlChan();
        exerciseChannel( xchan );
    }

    public void testFitsChan() throws IOException {
        FitsChan fchan = new FitsChan();
        assertEquals( 1, fchan.write( wcs ) );
        int nc = fchan.getNcard();
        assertTrue( "Enough cards", nc > 10 );
        fchan.setCard( 11 );
        while ( fchan.getNcard() >= 11 ) {
            fchan.delFits();
        }
        int ncomm = 0;
        int nline = 0;
        for ( Iterator it = fchan.iterator(); it.hasNext(); ) {
            String line = (String) it.next();
            assertEquals( 80, line.length() );
            assertTrue( line.trim().length() == 0 ||
                        line.charAt( 8 ) == '=' ||
                        line.startsWith( "COMMENT" ) );
            if ( line.startsWith( "COMMENT" ) ) {
                ncomm++;
            }
            nline++;
        }
        assertEquals( 10, nline );
        assertTrue( ncomm > 0 );
        for ( Iterator it = fchan.iterator(); it.hasNext(); ) {
            String line = (String) it.next();
            if ( line.startsWith( "COMMENT" ) ) {
                it.remove();
            }
        }
        assertEquals( 10 - ncomm, fchan.getNcard() );

        assertTrue( ! fchan.getCarLin() );
        fchan.setCarLin( true );
        assertTrue( fchan.getCarLin() );
        assertTrue( fchan.getDefB1950() );
        fchan.setDefB1950( false );
        assertTrue( ! fchan.getDefB1950() );

        String[] cards = new String[] {
            "SIMPLE  =                    T / Standard FITS format",
            "BITPIX  =                    8 / Character data",
            "NAXIS   =                    0 / No image, just extensions",
        };
        StringBuffer cardbuf = new StringBuffer();
        for ( int i = 0; i < cards.length; i++ ) {
            String card = cards[ i ];
            cardbuf.append( card );
            for ( int j = card.length(); j < 80; j++ ) {
                cardbuf.append( ' ' );
            }
        }
        fchan.putCards( cardbuf.toString() );
        assertEquals( cards.length, fchan.getNcard() );
        assertTrue( fchan.findFits( "BITPIX", false )
                         .startsWith( cards[ 1 ] ) );
        assertEquals( 2, fchan.getCard() );
    }

    public void testFrame() {

        // format
        String s1v = sky.format( 1, 0.5 );
        String s2v = sky.format( 2, 1.5 );
        assertTrue( s1v.indexOf( ':' ) > 0 );
        assertTrue( s2v.indexOf( ':' ) > 0 );
        assertEquals( "<bad>", new Frame( 1 ).format( 1, AstObject.AST__BAD ) );
        assertTrue( new Frame( 1 ).format( 1, Double.NaN ).toUpperCase()
                                  .indexOf( "NAN" ) > -1 );

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

        // atts
        Frame ff = new Frame( 2 );
        try {
            assertTrue( ! ff.test( "top(2)" ) );
            ff.setTop( 2, -100.0 );
            assertTrue( ff.test( "top(2)" ) );
            assertEquals( -100.0, ff.getTop( 2 ) );
        }
        catch ( AstException e ) {
            if ( e.getMessage().indexOf( "is invalid for a Axis" ) > 0 ) {
                e.printStackTrace( System.out );
                System.out.println( AstObject.reportVersions() );
                assertTrue( "You probably have the wrong AST version", false );
            }
            else {
                throw e;
            }
        }

        // activeUnit
        assertTrue( ! ff.getActiveUnit() );
        ff.setActiveUnit( true );
        assertTrue( ff.getActiveUnit() );
        ff.setActiveUnit( false );
        assertTrue( ! ff.getActiveUnit() );
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

    public void testSkyRef() {
        SkyFrame sk8 = new SkyFrame();

        // Might be either "origin" or "ignore" depending on version.
        // Skip the test as the easiest way round this.
        // assertEquals( "origin", sk8.getSkyRefIs().toLowerCase() );

        sk8.setSkyRefIs( "pole" );
        assertEquals( "pole", sk8.getSkyRefIs().toLowerCase() );
        try {
            sk8.setSkyRefIs( "Bristol" );
            fail();
        }
        catch( AstException e ) {
            assertEquals( AstException.AST__OPT, e.getStatus() );
        }

        double ref1 = Math.PI / 8.;
        double ref2 = Math.PI / 16.;
        String ref = "1:30:00.0, 11:15:00";

        sk8.setSkyRef( 1, ref1 );
        sk8.setSkyRef( 2, ref2 );
        assertEquals( ref1, sk8.getSkyRef( 1 ), 1e-10 );
        assertEquals( ref2, sk8.getSkyRef( 2 ), 1e-10 );
        assertEquals( ref, sk8.getSkyRef() );
        sk8.setSkyRef( ref );
        assertEquals( ref1, sk8.getSkyRef( 1 ), 1e-10 );
        assertEquals( ref2, sk8.getSkyRef( 2 ), 1e-10 );
        assertEquals( ref, sk8.getSkyRef() );

        sk8.setSkyRefP( 1, ref1 );
        sk8.setSkyRefP( 2, ref2 );
        assertEquals( ref1, sk8.getSkyRefP( 1 ), 1e-10 );
        assertEquals( ref2, sk8.getSkyRefP( 2 ), 1e-10 );
        assertEquals( ref, sk8.getSkyRefP() );
        sk8.setSkyRefP( ref );
        assertEquals( ref1, sk8.getSkyRefP( 1 ), 1e-10 );
        assertEquals( ref2, sk8.getSkyRefP( 2 ), 1e-10 );
        assertEquals( ref, sk8.getSkyRefP() );

        sk8.setAlignOffset( true );
        assertTrue( sk8.getAlignOffset() );
        sk8.setAlignOffset( false );
        assertTrue( ! sk8.getAlignOffset() );
    }

    public void testFluxFrame() {
        FluxFrame ff0 = new FluxFrame();
        SpecFrame sf = new SpecFrame();
        FluxFrame ff1 = new FluxFrame( 1., sf );
        FluxFrame ff2 = new FluxFrame( 1., sf );
        ff1.setSystem( "FLXDN" );
        ff2.setSystem( "FLXDNW" );
        assertEquals( "FLXDN", ff1.getSystem() );
        assertEquals( "FLXDNW", ff2.getSystem() );
        FrameSet cfs = ff1.convert( ff2, "" );
        double[] ans = cfs.tran1( 1, new double[] { 1. }, true );

        // Not 100% certain this is what it should be, but looks pretty
        // plausible.
        assertEquals( 1.0, 3e8 * 1e10 / ans[ 0 ], 0.001 );

        assertEquals( "W/m^2/Hz", ff1.getUnit( 1 ) );

        assertEquals( 1.0, ff2.getSpecVal() );
        ff2.setSpecVal( 109. );
        assertEquals( 109., ff2.getSpecVal() );
    }

    public void testSpecFluxFrame() {
        SpecFrame sf = new SpecFrame();
        FluxFrame ff = new FluxFrame();
        SpecFluxFrame sff = new SpecFluxFrame( sf, ff );
        Frame[] fs = sff.decompose();
        assertEquals( sf, fs[ 0 ] );
        assertEquals( ff, fs[ 1 ] );
        assertTrue( sf.sameObject( fs[ 0 ] ) );
        assertTrue( ff.sameObject( fs[ 1 ] ) );
    }

    public void testDSBSpecFrame() {
        DSBSpecFrame dsb = new DSBSpecFrame();

        dsb.setDsbCentre( 109. );
        assertEquals( 109., dsb.getDsbCentre() );

        assertEquals( 4.0, dsb.getIf() );
        dsb.setIf( 23. );
        assertEquals( 23., dsb.getIf() );

        assertEquals( "lsb", dsb.getSideBand().toLowerCase() );
        dsb.setSideBand( "usb" );
        assertEquals( "usb", dsb.getSideBand().toLowerCase() );
        dsb.setSideBand( "lsb" );
        assertEquals( "lsb", dsb.getSideBand().toLowerCase() );
        try {
            dsb.setSideBand( "sideways" );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( e.getStatus(), AstException.AST__ATTIN );
        }

        dsb.setAlignSideBand( false );
        assertTrue( ! dsb.getAlignSideBand() );
        dsb.setAlignSideBand( true );
        assertTrue( dsb.getAlignSideBand() );
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

        assertArrayEquals(
            new double[] { 1, 2 },
            new Frame( 2 )
           .intersect( new double[] { 1, 0 }, new double[] { 1, 4 },
                       new double[] { 0, 2 }, new double[] { 2, 2 } ) );

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

        double[] linCoeffs = zoomer.linearApprox( new double[] { 0, 0, },
                                                  new double[] { 1, 1, },
                                                  0.01 );
        assertArrayEquals( new double[] { 0, 0, 2, 0, 0, 2 }, linCoeffs );

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

        // rate
        assertEquals( 2.0, zoomer.rate( new double[ 2 ], 1, 1 ) );
        assertEquals( 0.0, zoomer.rate( new double[ 2 ], 1, 2 ) );
        try {
            zoomer.rate( new double[ 1 ], 1, 2 );
            fail();
        }
        catch ( IllegalArgumentException e ) {
            // ok
        }

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

        // mapSplit
        ZoomMap zmap = new ZoomMap( 3, 5.0 );
        int[] zouts = new int[ 3 ];
        Mapping zmap1 = zmap.mapSplit( new int[] { 3, }, zouts );
        assertArrayEquals( new int[] { 3, 0, 0 }, zouts );
        assertArrayEquals( new double[] { 20.0, },
                           zmap1.tran1( 1, new double[] { 4.0 }, true ) );
        try {
            zmap.mapSplit( new int[] { 3, }, new int[ 2 ] );
            fail();
        }
        catch ( IllegalArgumentException e ) {
            // too few elements in out array
        }

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
        assertEquals( 1, mc.write( imap ) );
        Mapping jmap = (Mapping) mc.read();
        double[][] resultb = jmap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resultb[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resultb[ 1 ] );

        FitsChan fc = new FitsChan();
        assertEquals( 1, fc.write( jmap ) );
        fc.setCard( 1 );
        Mapping kmap = (Mapping) fc.read();
        double[][] resultc = kmap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resultc[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resultc[ 1 ] );

        XmlChan xc = new MemoryXmlChan();
        assertEquals( 1, xc.write( kmap ) );
        Mapping lmap = (Mapping) xc.read();
        double[][] resultd = lmap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resultd[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resultd[ 1 ] );

        double[][] resulte = imap.tran2( 3, xina, yina, true );
        assertArrayEquals( resulta[ 0 ], resulte[ 0 ] );
        assertArrayEquals( resulta[ 1 ], resulte[ 1 ] );
    }

    public void testTranGrid() {
        Mapping map = new CmpMap( new ZoomMap( 2, 5.0 ),
                                  new ShiftMap( new double[] { 100.0, 0.0 } ),
                                  true );
        double[][] grid =
            map.tranGrid( 2, new int[] { 2, -1 }, new int[] { 3, 1 },
                          0.1, 10, true, 2 );
        assertArrayEquals( new double[] { 110, 115, 110, 115, 110, 115, },
                           grid[ 0 ] );
        assertArrayEquals( new double[] {  -5,  -5,   0,   0,   5,   5, },
                           grid[ 1 ] );
    }

    public void testPolyMap() {
        // y = 3*x*x.
        Mapping poly = new PolyMap( 1, 1,
                                    1, new double[] { 3.0, 1., 2. },
                                    0, null );
        double x = 29.;
        assertEquals( 3 * x * x,
                      poly.tran1( 1, new double[] { x }, true )[ 0 ] );

        double[] lbnd = new double[] { 0 };
        double[] ubnd = new double[] { 1 };
        assertNull( poly.linearApprox( lbnd, ubnd, 1e-5 ) );
        assertNotNull( poly.linearApprox( lbnd, ubnd, 1e+5 ) );
    }

    public void testGrismMap() {
        GrismMap gmap = new GrismMap();
        assertEquals( 0.0, gmap.getGrismEps() );
        gmap.setGrismEps( 0.25 );
        assertEquals( 0.25, gmap.getGrismEps() );
        // ?? don't know enough about grisms to come up with a sensible
        // transformation test.
    }

    public void testShiftMap() {
        double xoff = 23.;
        double yoff = 5.;
        Mapping shifty = new ShiftMap( new double[] { xoff, yoff } );
        double[][] out = shifty.tran2( 1, new double[ 1 ],
                                          new double[ 1 ], true );
        assertEquals( xoff, out[ 0 ][ 0 ] );
        assertEquals( yoff, out[ 1 ][ 0 ] );
        Mapping unshift = new WinMap( 2, new double[] { 0, 0 },
                                         new double[] { 1, 1 },
                                         new double[] { 0-xoff, 0-yoff },
                                         new double[] { 1-xoff, 1-yoff } );
        Mapping unit = new CmpMap( shifty, unshift, true );
        out = unit.tran2( 1, new double[ 1 ], new double[ 1 ], true );
        assertEquals( 0.0, out[ 0 ][ 0 ] );
        assertEquals( 0.0, out[ 1 ][ 0 ] );
        assertTrue( ! UnitMap.class.equals( unit.getClass() ) );
        assertEquals( UnitMap.class, unit.simplify().getClass() );
    }

    public void testRateMap() {
        RateMap rmap = new RateMap( new UnitMap( 1 ), 1, 1 );
        assertEquals( 1.0, rmap.tran1( 1, new double[] { 109. }, true )[ 0 ] );
    }

    public void testTimeMap() {
        TimeMap tmap = new TimeMap();
        assertEquals( tmap, new TimeMap( 0 ) );
        TimeMap tmap2 = new TimeMap();
        tmap2.timeAdd( "MJDTOMJD", new double[] { 101., 100., } );
        assertArrayEquals( new double[] { 100., 200. },
                           tmap.tran1( 2, new double[] { 100., 200. }, true ) );
        assertArrayEquals( new double[] { 101., 201. },
                           tmap2.tran1( 2, new double[] { 100., 200. }, true ));
        try {
            tmap.timeAdd( "NOTACONVERSION", null );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__TIMIN, e.getStatus() );
        }

        /* What happens if we supply an array that's too short? */
        boolean raised = false;
        try {
            tmap.timeAdd( "BEPTOMJD", null );
        }
        catch ( AstException e ) {
            raised = true;
            assertEquals( AstException.AST__TIMIN, e.getStatus() );
        }
        assertTrue(raised);

        raised = false;
        try {
            tmap.timeAdd( "MJDTOJD", new double[] { 0. } );
        }
        catch ( AstException e ) {
            raised = true;
            assertEquals( AstException.AST__TIMIN, e.getStatus() );
        }
        assertTrue(raised);
    }

    public void testTimeFrame() {
        TimeFrame tfrm = new TimeFrame();
        assertEquals( "TAI", tfrm.getTimeScale() );
        tfrm.setTimeScale( "LAST" );
        assertEquals( "LAST", tfrm.getTimeScale() );
        try {
            tfrm.setTimeScale( "TeaTime" );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__ATTIN, e.getStatus() );
        }
        tfrm = new TimeFrame();
        assertEquals( "MJD", tfrm.getSystem() );
        assertEquals( "d", tfrm.getUnit( 1 ) );
        tfrm.setSystem( "JEPOCH" );
        assertEquals( "JEPOCH", tfrm.getSystem() );
        assertEquals( "yr", tfrm.getUnit( 1 ) );
        tfrm.setTimeOrigin( "1970-JAN-01T00:00:00" );
        assertEquals( 1970.0, tfrm.getTimeOrigin() );
        tfrm.setUnit( 1, "s" );
        assertEquals( "s", tfrm.getUnit( 1 ) );
        assertEquals( (double) System.currentTimeMillis() * 1e-3,
                      tfrm.currentTime(), 60.0 );
    }

    public void testTranMap() {
        MathMap fmap = new MathMap( 1, 1, new String[] { "f=i*2.0" },
                                          new String[] { "i" } );
        MathMap imap = new MathMap( 1, 1, new String[] { "f" },
                                          new String[] { "i=f*0.5" } );
        TranMap tmap = new TranMap( fmap, imap );
        double num = 44.;
        double[] numarr = new double[] { num };
        assertEquals( num * 2.0, tmap.tran1( 1, numarr, true )[ 0 ] );
        assertEquals( num * 0.5, tmap.tran1( 1, numarr, false )[ 0 ] );
        assertEquals( num * 2.0, fmap.tran1( 1, numarr, true )[ 0 ] );
        assertEquals( num * 0.5, imap.tran1( 1, numarr, false )[ 0 ] );
        try {
            fmap.tran1( 1, numarr, false );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__TRNND, e.getStatus() );
        }
        try {
            imap.tran1( 1, numarr, true );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__TRNND, e.getStatus() );
        }
    }

    public void testSwitchMap() {
        Frame f = new Frame( 1 );
        Region lReg =
            new Box( f, 1, new double[] { -100 }, new double[] { 0 }, null );
        Region rReg =
            new Box( f, 1, new double[] { 0 }, new double[] { +100 }, null );
        SelectorMap fsMap = new SelectorMap( new Region[] { lReg, rReg }, -99 );
        assertEquals( 1, fsMap.getNout() );
        assertArrayEquals(
            new double[] { 0, 1, 2, -99 },
            fsMap.tran1( 4, new double[] { 9999, -1, +1, AstObject.AST__BAD },
                         true ) );
        Mapping lMap = new ShiftMap( new double[] { -4 } );
        Mapping rMap = new ShiftMap( new double[] { +4 } );
        SwitchMap swMap =
            new SwitchMap( fsMap, null, new Mapping[] { lMap, rMap } );
        assertArrayEquals(
            new double[] { AstObject.AST__BAD, -5, +5, AstObject.AST__BAD },
            swMap.tran1( 4, new double[] { -1000, -1, +1, +1000 }, true ) );
    }

    public void testPlot() {

        /* Check we have graphics capability. */
        if ( isHeadless() ) {
            System.out.println( "Headless environment - no Plot testing" );
            return;
        }

        double[] basebox = new double[] { 0, 0, 50000, 50000 };
        JFrame toplev = new JFrame();
        toplev.setAutoRequestFocus( false );

        TestPlotHolder pan = new TestPlotHolder();
        pan.setPreferredSize( new Dimension( 400, 400 ) );
        toplev.getContentPane().setLayout( new FlowLayout() );
        toplev.getContentPane().add( pan );
        toplev.pack();

        Plot plot = new Plot( wcs, pan.getVisibleRect(), basebox, 40, 20, 40, 20 );
        pan.plot = plot;

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

        // line with no style
        plot.curve( new double[] { 1.5e4, 3.5e4 },
                    new double[] { 3.5e4, 1.5e4 } );

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

        plot.setSize( "textlab", 12 );
        plot.setColour( "strings", Color.red.getRGB() );

        int textFont = DefaultGrfFontManager.getReference().
            add( new Font( "SansSerif", Font.BOLD, 24 ) );
        plot.setFont( "strings", textFont );
        plot.setSize( "strings", 2.0 );

        plot.text( "JNI", new double[] { 1e4, 1e4 },
                   new float[] { -1, 1 }, "BL" );
        plot.text( "AST", new double[] { 4e4, 4e4 },
                   new float[] { 1, -1 }, "BL" );

        // boundingBox, invisible
        plot.setInvisible( true );
        plot.curve( new double[] { 1000., 1000. },
                    new double[] { 2000., 2000. } );
        Rectangle2D linbox = plot.boundingBox();
        assertTrue( pan.getBounds().contains( linbox ) );
        assertTrue( linbox.getHeight() > 5. && linbox.getWidth() > 5. );
        plot.setInvisible( false );

        plot = null;

        // log grid
        TestPlotHolder pan2 = new TestPlotHolder();
        pan2.setPreferredSize( new Dimension( 400, 400 ) );
        toplev.getContentPane().add( pan2 );
        toplev.pack();
        Plot plot2 = new Plot( new Frame( 2 ), pan2.getVisibleRect(),
                               new double[] { 5.5e0, 5e-3, 4.5e1, 5e3 },
                               60, 60, 60, 60 );
        plot2.setLogTicks( 1, false );
        plot2.setLogTicks( 2, true );
        pan2.plot = plot2;
        plot2.setColour( Color.black.getRGB() );
        plot2.setGrid( true );
        plot2.setMinTickLen( plot2.getMinTickLen( 1 ) * 2.0 );
        plot2.curve( new double[] { 0, 0 }, new double[] { 5e1, 5e3 } );
        plot2.setLogPlot( true );
        plot2.setNumLab( true );
        plot2.setDrawTitle( true );
        plot2.setTitle("Log Coords (10%^50+%s70+%c1000+n%c+%s+%^+ notation)");
        plot2.setTextLab( true );
        plot2.set("label(1)=Axis %^50+ %s50+ one %s+ %^+" );
        plot2.setLabel( 2, "Axis %^50+ %s50+ two %s+ %^+");

        plot2.grid();
        plot2.border();

        toplev.setVisible( true );

        try {
            Thread.currentThread().sleep( 1000 );
        }
        catch ( InterruptedException e ) {
            // no action
        }
    }

    public void testAstConstants() {
        int ast__air = WcsMap.AST__AIR;
        AstObject.getAstConstantD( "AST__BAD" );

        Matcher matcher = Pattern.compile( "AST V([2-9])\\.([0-9]+)-([0-9]+); "+
                                           "JNIAST native V5\\.1-0; " +
                                           "JNIAST java V5\\.1-0" +
                                           "\\b.*" )
                                 .matcher( AstObject.reportVersions() );
        assertTrue( AstObject.reportVersions(), matcher.matches() );
        int astMajor = Integer.parseInt( matcher.group( 1 ) );
        int astMinor = Integer.parseInt( matcher.group( 2 ) );
        int astRelease = Integer.parseInt( matcher.group( 3 ) );
        System.out.println( AstObject.reportVersions() );
        assertTrue( "Checking AST version: " + AstObject.reportVersions(),
                    ( astMajor > 3 ) ||
                    ( astMajor == 3 && astMinor > 7 ) ||
                    ( astMajor == 3 && astMinor == 7 && astRelease >= 0 ) );

        String absentConstName = "ABSENT_CONSTANT";
        try {
            AstObject.getAstConstantI( absentConstName );
            assertTrue( false );
        }
        catch ( IllegalArgumentException e ) {
            assertTrue( e.getMessage().indexOf( absentConstName ) > 0 );
        }
        try {
            AstObject.getAstConstantD( absentConstName );
            assertTrue( false );
        }
        catch ( IllegalArgumentException e ) {
            assertTrue( e.getMessage().indexOf( absentConstName ) > 0 );
        }
        try {
            AstObject.getAstConstantC( absentConstName );
            assertTrue( false );
        }
        catch ( IllegalArgumentException e ) {
            assertTrue( e.getMessage().indexOf( absentConstName ) > 0 );
        }
    }

    public void testSpecMap() {
        SpecMap smap = new SpecMap( 1, 0 );

        // freq -> lambda
        smap.specAdd( "FRtoWV", null );
        double nu = 3e8;
        double lambda = smap.tran1( 1, new double[] { nu }, true )[ 0 ];
        assertEquals( 1.0, lambda, 0.01 );

        // unit
        smap.specAdd( "WVtoFR", new double[ 0 ] );
        double a1 = 23.;
        double a2 = smap.tran1( 1, new double[] { a1 }, true )[ 0 ];
        assertEquals( a1, a2, 1e-8 );

        // lambda -> freq
        smap.specAdd( "FRtoWV", new double[ 0 ] );
        double nu2 = smap.tran1( 1, new double[] { lambda }, true )[ 0 ];
        assertEquals( nu, nu2, 1e-8 );
    }

    public void testSpecFrame() {
        SpecFrame sf = new SpecFrame();
        assertEquals( 1e5, sf.getRestFreq() );
        sf.setRestFreq( "1 Hz" );
        assertEquals( 1e-9, sf.getRestFreq() );

        SkyFrame sky = new SkyFrame();
        sky.setSystem( "FK5" );
        double lon = 0.5;
        double lat = 1.23;
        sf.setRefPos( sky, lon, lat );
        double[] ll = sf.getRefPos( sky );
        assertEquals( lon, ll[ 0 ] );
        assertEquals( lat, ll[ 1 ] );
    }

    public void testException() {
        AstException e1 = new AstException( "not an error",
                                            AstException.AST__UK1ER );
        AstException e2 = new AstException( "also not an error",
                                            AstException.AST__BADUN );
        assertEquals( AstException.AST__UK1ER, e1.getStatus() );
        assertEquals( "AST__UK1ER", e1.getStatusName() );
        assertEquals( AstException.AST__BADUN, e2.getStatus() );
        assertEquals( "AST__BADUN", e2.getStatusName() );
        assertTrue( e1.getMessage().startsWith( "not an error" ) );
        assertTrue( e2.getMessage().startsWith( "also not an error" ) );

        AstException e3 = new AstException( "Mary had a little lamb, "+
                                            "whoose fleece was white "+
                                            "as snow. And every where" +
                                            "that Mary went the lamb "+
                                            "was sure to go.",
                                            AstException.AST__BADUN );
        assertTrue( e3.getMessage().indexOf( "sure to go." ) > 0 );
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
    { setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) ); }
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

