package uk.ac.starlink.ast.xml;

import java.io.IOException;
import org.w3c.dom.Element;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.PcdMap;
import uk.ac.starlink.ast.SkyFrame;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class XAstTest extends TestCase {

    private FrameSet fset;
    private String confusingID;
    private XAstReader xr;
    private XAstWriter xw;

    public XAstTest( String name ) {
        super( name );
    }

    protected void setUp() {
        xr = new XAstReader();
        xw = new XAstWriter();
        Frame frm = new Frame( 2 );
        fset = new FrameSet( frm );
        fset.addFrame( 1, new PcdMap( 1e-5, new double[ 2 ] ), new SkyFrame() );
        frm.setDomain( "INITIAL" );
        confusingID = "<test>test &lt; &quotone&quot <![CDATA[&&<<>>]]>"
                    + " test 'two' </test>";
        fset.setID( confusingID );
    }

    public void testReadWrite() throws IOException {
        Element el = xw.makeElement( fset, "hdx:" );
        AstObject obj = xr.makeAst( el, "hdx:" );
        assertEquals( obj.getID(), confusingID );
        assertTrue( obj.equals( fset ) );

        Element el2 = xw.makeElement( fset, null );
        AstObject obj2 = xr.makeAst( el2, null );
        assertTrue( obj2.equals( fset ) );
    }

    public void testExceptions() {
        Element el = xw.makeElement( fset, "fish" );
        Element impostor = el.getOwnerDocument().createElement( "impostor" );
        try {
            AstObject obj = xr.makeAst( el, "fowl" );
            fail();
        }
        catch ( IOException e ) {}
        try {
            AstObject obj = xr.makeAst( el, "fish" );
        }
        catch ( IOException e ) {
            fail( e.getMessage() );
        }
        el.appendChild( impostor );
        try {
            AstObject obj2 = xr.makeAst( el, "fish" );
            fail();
        }
        catch ( IOException e ) {}
    }

    public static Test suite() {
        return new TestSuite( XAstTest.class );
    }
}
