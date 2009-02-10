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

public class TTest extends TestCase {

    public static String AST_FILE = "uk/ac/starlink/ast/alcyone.ast";
    private static FrameSet basicWcs;

    private Frame grid;
    private SkyFrame sky;
    private FrameSet wcs;
    private Mapping map;
    private int ic;

    public TTest( String name ) {
        super( name );
    }

    protected void setUp() throws IOException, InterruptedException {
        if ( basicWcs == null ) {
            Channel chan;
            InputStream strm = getClass()
                              .getClassLoader()
                              .getResourceAsStream( AST_FILE );
            assertNotNull( "Test file " + AST_FILE + " not present", strm );
            chan = new Channel( strm, null );
            basicWcs = (FrameSet) chan.read();
            strm.close();
        }
        basicWcs.setID( ic + "-bwcs" );
        for ( int i = 1; i <= basicWcs.getNframe(); i++ ) {
            basicWcs.getFrame( i ).setID( ic + "-b" + i );
        }
        wcs = (FrameSet) basicWcs.copy();
        wcs.setID( ic + "-wcs" );
        for ( int i = 1; i <= wcs.getNframe(); i++ ) {
            wcs.getFrame( i ).setID( ic + "-w" + i );
        }
        grid = wcs.getFrame( FrameSet.AST__BASE );
        map = wcs.getMapping( FrameSet.AST__BASE, FrameSet.AST__CURRENT );
        assertEquals( 2, grid.getNaxes() );
        assertEquals( "GRID", grid.getDomain() );
        sky = (SkyFrame) wcs.getFrame( FrameSet.AST__CURRENT );
    }

    public void testAstObject() {

        assertEquals( wcs.getC( "Class" ), "FrameSet" );
        try {
            Frame fx = new Frame( 1 );
            fx.setID( ic + "-fx" );
            fx.getC( "Sir Not-appearing-in-this-class" );
            fail();
        }
        catch ( AstException e ) {
            assertEquals( AstException.AST__BADAT, e.getStatus() );
        }

        Frame ff = grid;

        Frame fff = (Frame) ff.copy();
        fff.setID( ff.getID() + "-c" );

        assertEquals( ff.getIdent(), fff.getIdent() );
        assertTrue( ! ff.getID().equals( fff.getID() ) );

        assertTrue( wcs.getFrame( FrameSet.AST__BASE ).sameObject( ff ) );

        fff.set( "Domain=COPY" );
        fff.set( "Ident=f5" );
        wcs.addFrame( FrameSet.AST__BASE, map, fff );
        assertEquals( "COPY", wcs.getFrame( 5 ).getDomain() );

        ff = null;
        System.gc();
        assertEquals( 2, fff.getNaxes() );
    }


    public static void main( String[] args )
            throws IOException, InterruptedException {
        TTest t = new TTest( "test" );
        t.ic = 0;
        t.setUp();
        t.ic = 1;
        t.testAstObject();
        // t.setUp();
        t.ic = 2;
        t.testAstObject();
    }

}
