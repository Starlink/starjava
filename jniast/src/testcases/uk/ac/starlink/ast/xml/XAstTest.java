package uk.ac.starlink.ast.xml;

import java.io.IOException;
import javax.xml.transform.Source;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.PcdMap;
import uk.ac.starlink.ast.SkyFrame;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.TestCase;

import junit.framework.Test;
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

    public void testReadWriteElement() throws IOException {
        Element el = xw.makeElement( fset, "hdx:" );
        AstObject obj = xr.makeAst( el, "hdx:" );
        assertEquals( obj.getID(), confusingID );
        assertTrue( obj.equals( fset ) );
        obj.setID( "something else" );
        assertTrue( ! obj.equals( fset ) );

        Element el2 = xw.makeElement( fset, null );
        AstObject obj2 = xr.makeAst( el2, null );
        assertTrue( obj2.equals( fset ) );
    }

    public void testReadWriteSource() throws IOException {
        Source xsrc = xw.makeSource( fset, "eric:" );
        AstObject obj = xr.makeAst( xsrc, "eric:" );
        assertEquals( obj.getID(), confusingID );
        assertTrue( obj.equals( fset ) );
        obj.setID( "something else" );
        assertTrue( ! obj.equals( fset ) );
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

    public void testDefault() throws IOException {
        double discoVal = 23.0;
        Document doc = DOMUtils.newDocument();
        Element pcdEl = doc.createElement( "PcdMap" );
        Element discoEl = doc.createElement( XAstNames.ATTRIBUTE );
        discoEl.setAttribute( XAstNames.NAME, "Disco" );
        discoEl.setAttribute( XAstNames.VALUE, Double.toString( discoVal ) );
        pcdEl.appendChild( discoEl );
        doc.appendChild( pcdEl );

        XAstReader reader = new XAstReader();
        PcdMap pcd1 = (PcdMap) reader.makeAst( pcdEl, null );
        assertEquals( pcd1.getDisco(), discoVal );

        discoEl.setAttribute( XAstNames.DEFAULT, "true" );
        PcdMap pcd2 = (PcdMap) reader.makeAst( pcdEl, null );
        assertTrue( pcd2.getDisco() != discoVal );
    }

    public static Test suite() {
        return new TestSuite( XAstTest.class );
    }
}
